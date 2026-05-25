package com.example.equili.ui.viewModel

import android.app.Application
import androidx.lifecycle.*
import com.example.equili.data.model.*
import com.example.equili.repository.ExpenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

/**
 * ExpenseViewModel manages UI-related data for the app.
 * Upgraded by Nelson with Search/Sort features and synced with Dimetri's Firebase foundation.
 */
class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ExpenseRepository()

    private val dateRange = MutableLiveData<Pair<Long, Long>>()
    private val searchQuery = MutableLiveData("")
    private val sortOption = MutableLiveData(SortOption.DATE_DESC)

    enum class SortOption {
        DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC, CATEGORY_ASC
    }

    /** Observes the current user profile from Firebase. */
    val currentUser: LiveData<UserModel?> = repository.getCurrentUserLiveData()

    /** List of all categories for the current user from cloud. */
    val allCategories: LiveData<List<CategoryModel>> = repository.getAllCategories()

    /** Current monthly spending goal from cloud. */
    val monthlyGoal: LiveData<GoalModel?> = repository.getMonthlyGoal()

    /**
     * Core Data: Expenses filtered by date range from the cloud.
     */
    val expensesInDateRange: LiveData<List<ExpenseModel>> = dateRange.switchMap { range ->
        repository.getExpensesInRange(range.first, range.second)
    }

    /** Sum of all expense amounts in the current filtered range. */
    val totalAmountInDateRange: LiveData<Double?> = expensesInDateRange.map { list ->
        list.sumOf { it.amount }
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

    init {
        // Set default date range to current month
        val start = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }
        val end = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59) }
        dateRange.value = start.timeInMillis to end.timeInMillis
    }

    fun setDateRange(start: Long, end: Long) {
        dateRange.value = start to end
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setSortOption(option: SortOption) {
        sortOption.value = option
    }

    fun setCurrentUser(email: String) {
        // Kept for backward compatibility, Firebase handles session internally
    }

    fun insertExpense(expense: ExpenseModel) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertExpense(expense)
        addXp(15)
        updateStreak()
    }

    fun deleteExpense(expense: ExpenseModel) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteExpense(expense)
    }

    fun insertCategory(category: CategoryModel) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertCategory(category)
        addXp(25)
    }

    fun updateGoal(min: Double, max: Double) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateGoal(GoalModel(minGoal = min, maxGoal = max))
        addXp(20)
    }

    fun getCategoryTotalsInRange(start: Long, end: Long): LiveData<List<CategoryTotal>> {
        return repository.getCategoryTotalsInRange(start, end)
    }

    suspend fun registerUser(user: UserModel) = repository.registerUser(user)

    suspend fun getUserByEmail(email: String): UserModel? = repository.getUserByEmail(email)

    private fun addXp(amount: Int) = viewModelScope.launch(Dispatchers.IO) {
        val userValue = currentUser.value ?: return@launch
        var newXp = userValue.xp + amount
        var newLevel = userValue.level
        val xpNeeded = newLevel * 100
        if (newXp >= xpNeeded) {
            newXp -= xpNeeded
            newLevel++
        }

        // Manual copy since UserModel is a simple class now
        val updatedUser = UserModel(
            uid = userValue.uid,
            email = userValue.email,
            xp = newXp,
            level = newLevel,
            streak = userValue.streak,
            lastActionDate = userValue.lastActionDate
        )
        repository.updateUser(updatedUser)
    }

    private fun updateStreak() = viewModelScope.launch(Dispatchers.IO) {
        val userValue = currentUser.value ?: return@launch
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val lastDate = userValue.lastActionDate
        val yesterday = today - (24 * 60 * 60 * 1000)
        var newStreak = userValue.streak

        if (lastDate < today) {
            newStreak = if (lastDate >= yesterday) newStreak + 1 else 1

            val updatedUser = UserModel(
                uid = userValue.uid,
                email = userValue.email,
                xp = userValue.xp,
                level = userValue.level,
                streak = newStreak,
                lastActionDate = today
            )
            repository.updateUser(updatedUser)
            addXp(50)
        }
    }
}
