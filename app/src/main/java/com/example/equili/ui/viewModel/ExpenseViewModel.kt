package com.example.equili.ui.viewModel

import android.app.Application
import androidx.lifecycle.*
import com.example.equili.data.database.ExpenseDatabase
import com.example.equili.data.model.*
import com.example.equili.repository.ExpenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

/**
 * ExpenseViewModel manages UI-related data for the app.
 * It handles logic for data filtering, gamification (XP, levels, streaks),
 * and interacts with the Repository for database operations.
 */
class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository

    // Reactive triggers for data fetching
    private val currentUserEmail = MutableLiveData<String>()
    private val dateRange = MutableLiveData<Pair<Long, Long>>()
    private val searchQuery = MutableLiveData<String>("")
    private val sortOption = MutableLiveData<SortOption>(SortOption.DATE_DESC)

    enum class SortOption {
        DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC, CATEGORY_ASC
    }

    /** Observes the current user profile based on the active session email. */
    val currentUser: LiveData<UserModel?> = currentUserEmail.switchMap { email ->
        repository.getUserByEmailLiveData(email)
    }

    /**
     * MediatorLiveData that combines user email and date range.
     * Whenever either changes, it triggers dependent LiveData (like expensesInDateRange).
     */
    private val userAndRange = MediatorLiveData<Pair<String, Pair<Long, Long>>>().apply {
        addSource(currentUserEmail) { email -> value = email to (dateRange.value ?: (0L to 0L)) }
        addSource(dateRange) { range -> value = (currentUserEmail.value ?: "") to range }
    }

    /** List of all categories for the current user. */
    val allCategories: LiveData<List<CategoryModel>> = currentUserEmail.switchMap { email ->
        repository.getAllCategories(email)
    }

    /** Current monthly spending goal for the user. */
    val monthlyGoal: LiveData<GoalModel?> = currentUserEmail.switchMap { email ->
        repository.getMonthlyGoal(email)
    }

    /** List of expenses filtered by the current user and selected date range. */
    val expensesInDateRange: LiveData<List<ExpenseModel>> = userAndRange.switchMap { pair ->
        repository.getExpensesInRange(pair.first, pair.second.first, pair.second.second)
    }

    /**
     * Expenses filtered by search query and sorted by selected option.
     */
    val filteredExpenses = MediatorLiveData<List<ExpenseModel>>().apply {
        val update = {
            val list = expensesInDateRange.value ?: emptyList()
            val query = searchQuery.value ?: ""
            val option = sortOption.value ?: SortOption.DATE_DESC

            val filtered = if (query.isEmpty()) list else list.filter { it.title.contains(query, ignoreCase = true) }
            
            value = when (option) {
                SortOption.DATE_DESC -> filtered.sortedByDescending { it.date }
                SortOption.DATE_ASC -> filtered.sortedBy { it.date }
                SortOption.AMOUNT_DESC -> filtered.sortedByDescending { it.amount }
                SortOption.AMOUNT_ASC -> filtered.sortedBy { it.amount }
                SortOption.CATEGORY_ASC -> filtered.sortedBy { it.category }
            }
        }
        addSource(expensesInDateRange) { update() }
        addSource(searchQuery) { update() }
        addSource(sortOption) { update() }
    }

    /** Sum of all expense amounts in the current filtered range. */
    val totalAmountInDateRange: LiveData<Double?> = userAndRange.switchMap { pair ->
        repository.getExpensesInRange(pair.first, pair.second.first, pair.second.second).map { list ->
            list.sumOf { it.amount }
        }
    }

    init {
        // Initialize repository with database instance
        val db = ExpenseDatabase.getDatabase(application)
        repository = ExpenseRepository(db)

        // Set default date range to current month
        val start = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }
        val end = Calendar.getInstance()
        dateRange.value = start.timeInMillis to end.timeInMillis
    }

    /** Updates the active user session email. */
    fun setCurrentUser(email: String) {
        currentUserEmail.value = email
    }

    fun getCurrentUserEmail(): String? = currentUserEmail.value

    /** Updates the filter range for expenses. */
    fun setDateRange(start: Long, end: Long) {
        dateRange.value = start to end
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setSortOption(option: SortOption) {
        sortOption.value = option
    }

    /** Inserts a new expense and rewards the user with XP and streak updates. */
    fun insertExpense(expense: ExpenseModel) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertExpense(expense)
        addXp(15) // Reward for logging data
        updateStreak()
    }

    fun deleteExpense(expense: ExpenseModel) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteExpense(expense)
    }

    fun insertCategory(category: CategoryModel) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertCategory(category)
        addXp(25) // Reward for customization
    }

    fun updateGoal(min: Double, max: Double) = viewModelScope.launch(Dispatchers.IO) {
        val email = currentUserEmail.value ?: return@launch
        repository.updateGoal(GoalModel(userEmail = email, minGoal = min, maxGoal = max))
        addXp(20) // Reward for financial planning
    }

    fun getCategoryTotalsInRange(start: Long, end: Long): LiveData<List<CategoryTotal>> {
        val email = currentUserEmail.value ?: return MutableLiveData(emptyList())
        return repository.getCategoryTotalsInRange(email, start, end)
    }

    suspend fun registerUser(user: UserModel): Long = repository.registerUser(user)
    suspend fun getUserByEmail(email: String): UserModel? = repository.getUserByEmail(email)

    /**
     * Internal logic for gamification XP.
     * Handles level-ups when XP exceeds the requirement (100 XP per level).
     */
    private fun addXp(amount: Int) = viewModelScope.launch(Dispatchers.IO) {
        val email = currentUserEmail.value ?: return@launch
        val user = repository.getUserByEmail(email) ?: return@launch

        var newXp = user.xp + amount
        var newLevel = user.level

        val xpNeeded = newLevel * 100
        if (newXp >= xpNeeded) {
            newXp -= xpNeeded
            newLevel++
        }

        repository.updateUser(user.copy(xp = newXp, level = newLevel))
    }

    /**
     * Logic for tracking daily usage streaks.
     * If user logs an expense on a consecutive day, the streak increases.
     */
    private fun updateStreak() = viewModelScope.launch(Dispatchers.IO) {
        val email = currentUserEmail.value ?: return@launch
        val user = repository.getUserByEmail(email) ?: return@launch

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val lastDate = user.lastActionDate
        val yesterday = today - (24 * 60 * 60 * 1000)

        var newStreak = user.streak
        if (lastDate < today) {
            if (lastDate >= yesterday) {
                newStreak++ // Consecutive day
            } else {
                newStreak = 1 // Reset streak if a day was missed
            }
            repository.updateUser(user.copy(streak = newStreak, lastActionDate = today))
            addXp(50) // Bonus for maintaining/starting a streak
        }
    }
}
