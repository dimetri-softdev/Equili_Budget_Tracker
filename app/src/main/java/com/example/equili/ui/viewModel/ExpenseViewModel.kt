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
 * Migrated to Firebase for cloud-synced budget tracking.
 */
class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ExpenseRepository()

    private val dateRange = MutableLiveData<Pair<Long, Long>>()

    /** Observes the current user profile from Firebase. */
    val currentUser: LiveData<UserModel?> = repository.getCurrentUserLiveData()

    /** List of all categories for the current user from cloud. */
    val allCategories: LiveData<List<CategoryModel>> = repository.getAllCategories()

    /** Current monthly spending goal from cloud. */
    val monthlyGoal: LiveData<GoalModel?> = repository.getMonthlyGoal()

    /** List of expenses filtered by the selected date range. */
    val expensesInDateRange: LiveData<List<ExpenseModel>> = dateRange.switchMap { range ->
        repository.getExpensesInRange(range.first, range.second)
    }

    /** Sum of all expense amounts in the current filtered range. */
    val totalAmountInDateRange: LiveData<Double?> = expensesInDateRange.map { list ->
        list.sumOf { it.amount }
    }

    init {
        // Set default date range to current month
        val start = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }
        val end = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59) }
        dateRange.value = start.timeInMillis to end.timeInMillis
    }

    /** No longer needed as Firebase Auth manages the session, but kept for legacy UI compatibility if any. */
    fun setCurrentUser(email: String) {
        // Firebase handles this internally
    }

    /** Updates the filter range for expenses. */
    fun setDateRange(start: Long, end: Long) {
        dateRange.value = start to end
    }

    /** Inserts a new expense and rewards the user with XP and streak updates. */
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
        val userValue = repository.getCurrentUserLiveData().value ?: return@launch

        var newXp = userValue.xp + amount
        var newLevel = userValue.level

        val xpNeeded = newLevel * 100
        if (newXp >= xpNeeded) {
            newXp -= xpNeeded
            newLevel++
        }

        repository.updateUser(userValue.copy(xp = newXp, level = newLevel))
    }

    private fun updateStreak() = viewModelScope.launch(Dispatchers.IO) {
        val userValue = repository.getCurrentUserLiveData().value ?: return@launch

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val lastDate = userValue.lastActionDate
        val yesterday = today - (24 * 60 * 60 * 1000)

        var newStreak = userValue.streak
        if (lastDate < today) {
            if (lastDate >= yesterday) {
                newStreak++
            } else {
                newStreak = 1
            }
            repository.updateUser(userValue.copy(streak = newStreak, lastActionDate = today))
            addXp(50)
        }
    }
}
