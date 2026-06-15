package com.example.equili.ui.viewModel

import android.app.Application
import androidx.lifecycle.*
import com.example.equili.data.model.*
import com.example.equili.repository.ExpenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * ExpenseViewModel manages the UI-related data in a lifecycle-conscious way.
 * It serves as a bridge between the [ExpenseRepository] and the UI (Activities/Fragments).
 */
class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    // Repository for data operations (Firebase / Local)
    private val repository = ExpenseRepository()

    // Current month identifier for goal tracking (Format: YYYY-MM)
    private val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

    // Observable states for filtering and sorting
    private val dateRange   = MutableLiveData<Pair<Long, Long>>()
    private val searchQuery = MutableLiveData("")
    private val sortOption  = MutableLiveData(SortOption.DATE_DESC)

    /**
     * Supported sorting criteria for expense lists.
     */
    enum class SortOption {
        DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC, CATEGORY_ASC
    }

    /** Observes the current user profile from Firebase. */
    val currentUser: LiveData<UserModel?> = repository.getCurrentUserLiveData()

    /** List of all categories for the current user from cloud. */
    val allCategories: LiveData<List<CategoryModel>> = repository.getAllCategories()

    /** Current monthly spending goal from cloud. */
    val monthlyGoal: LiveData<GoalModel?> = repository.getMonthlyGoal(currentMonth)

    // -----------------------------------------------------------------------------------------
    // ALERT SYSTEM: One-shot LiveData events consumed by the UI
    // -----------------------------------------------------------------------------------------

    /**
     * Emits a budget warning percentage (e.g. 82) when the user crosses the 80% threshold.
     */
    private val _budgetWarningEvent = MutableLiveData<Int?>(null)
    val budgetWarningEvent: LiveData<Int?> get() = _budgetWarningEvent

    /**
     * Emits the new level number when the user levels up.
     */
    private val _levelUpEvent = MutableLiveData<Int?>(null)
    val levelUpEvent: LiveData<Int?> get() = _levelUpEvent

    /**
     * Emits a [BadgeModel] when a badge is newly awarded.
     */
    private val _badgeUnlockedEvent = MutableLiveData<BadgeModel?>(null)
    val badgeUnlockedEvent: LiveData<BadgeModel?> get() = _badgeUnlockedEvent

    /**
     * Emits the month string (YYYY-MM) when a new month transition is detected.
     */
    private val _newMonthEvent = MutableLiveData<String?>(null)
    val newMonthEvent: LiveData<String?> get() = _newMonthEvent

    // -----------------------------------------------------------------------------------------
    // Core Data
    // -----------------------------------------------------------------------------------------

    /**
     * LiveData stream of expenses within the selected [dateRange].
     */
    val expensesInDateRange: LiveData<List<ExpenseModel>> = dateRange.switchMap { range ->
        repository.getExpensesInRange(range.first, range.second)
    }

    /**
     * Reactive stream that applies search filtering and sorting logic to the expenses list.
     */
    val filteredExpenses = MediatorLiveData<List<ExpenseModel>>().apply {
        val update = {
            val list   = expensesInDateRange.value ?: emptyList()
            val query  = searchQuery.value ?: ""
            val option = sortOption.value ?: SortOption.DATE_DESC

            val filtered = if (query.isEmpty()) list
                           else list.filter { it.title.contains(query, ignoreCase = true) }

            value = when (option) {
                SortOption.DATE_DESC     -> filtered.sortedByDescending { it.date }
                SortOption.DATE_ASC      -> filtered.sortedBy { it.date }
                SortOption.AMOUNT_DESC   -> filtered.sortedByDescending { it.amount }
                SortOption.AMOUNT_ASC    -> filtered.sortedBy { it.amount }
                SortOption.CATEGORY_ASC  -> filtered.sortedBy { it.category }
            }
        }
        addSource(expensesInDateRange) { update() }
        addSource(searchQuery) { update() }
        addSource(sortOption) { update() }
    }

    /** Sum of all expense amounts in the current filtered range. */
    val totalAmountInDateRange: LiveData<Double?> = expensesInDateRange.map { list ->
        list.sumOf { it.amount }
    }

    init {
        // Initialize date range to the current month by default
        val start = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        }
        val end = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
        }
        dateRange.value = start.timeInMillis to end.timeInMillis

        // Trigger month transition check when user profile is loaded
        currentUser.observeForever(object : androidx.lifecycle.Observer<UserModel?> {
            override fun onChanged(user: UserModel?) {
                if (user != null) {
                    checkAndCarryOverGoal(user)
                    currentUser.removeObserver(this)
                }
            }
        })
    }

    // Setters for UI controls
    fun setDateRange(start: Long, end: Long) { dateRange.value = start to end }
    fun setSearchQuery(query: String)        { searchQuery.value = query }
    fun setSortOption(option: SortOption)    { sortOption.value = option }

    /** Legacy placeholder for compatibility. */
    fun setCurrentUser(email: String) {}

    // Methods to reset one-shot events after consumption
    fun consumeBudgetWarning() { _budgetWarningEvent.value = null }
    fun consumeLevelUpEvent()  { _levelUpEvent.value = null }
    fun consumeBadgeEvent()    { _badgeUnlockedEvent.value = null }
    fun consumeNewMonthEvent() { _newMonthEvent.value = null }

    // -----------------------------------------------------------------------------------------
    // MONTH TRANSITION LOGIC
    // -----------------------------------------------------------------------------------------

    /**
     * Checks if the app has entered a new month and carries over the budget goal if necessary.
     */
    private fun checkAndCarryOverGoal(user: UserModel) = viewModelScope.launch {
        if (user.lastProcessedMonth != currentMonth) {
            val previousMonth = user.lastProcessedMonth

            withContext(Dispatchers.IO) {
                // 1. Check if current month goal exists
                val currentGoal = repository.getGoalSnapshot(currentMonth)

                if (currentGoal == null && previousMonth.isNotEmpty()) {
                    // 2. Carry over goal from last processed month
                    val oldGoal = repository.getGoalSnapshot(previousMonth)
                    if (oldGoal != null) {
                        repository.updateGoal(oldGoal.copy(month = currentMonth))
                    }
                }

                // 3. Update user's last processed month in Firebase
                repository.updateUser(user.copy(lastProcessedMonth = currentMonth))
            }

            // 4. Notify UI of new month
            _newMonthEvent.postValue(currentMonth)
        }
    }

    // -----------------------------------------------------------------------------------------
    // CRUD OPERATIONS
    // -----------------------------------------------------------------------------------------

    /**
     * Inserts a new expense and updates gamification stats.
     */
    fun insertExpense(expense: ExpenseModel) = viewModelScope.launch {
        val userSnapshot = currentUser.value
        withContext(Dispatchers.IO) {
            repository.insertExpense(expense)
        }
        addXp(15, userSnapshot)
        updateStreak(userSnapshot)
        checkFirstExpenseBadge(userSnapshot)
    }

    /**
     * Deletes an expense from the database.
     */
    fun deleteExpense(expense: ExpenseModel) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteExpense(expense)
    }

    /**
     * Adds a new custom category.
     */
    fun insertCategory(category: CategoryModel) = viewModelScope.launch {
        val userSnapshot = currentUser.value
        withContext(Dispatchers.IO) { repository.insertCategory(category) }
        addXp(25, userSnapshot)
    }

    /**
     * Updates the monthly budget goals.
     */
    fun updateGoal(min: Double, max: Double) = viewModelScope.launch {
        val userSnapshot = currentUser.value
        withContext(Dispatchers.IO) {
            repository.updateGoal(GoalModel(minGoal = min, maxGoal = max, month = currentMonth))
        }
        addXp(20, userSnapshot)
    }

    /**
     * Retrieves spending totals grouped by category.
     */
    fun getCategoryTotalsInRange(start: Long, end: Long): LiveData<List<CategoryTotal>> =
        repository.getCategoryTotalsInRange(start, end)

    suspend fun registerUser(user: UserModel) = repository.registerUser(user)

    suspend fun getUserByEmail(email: String): UserModel? = repository.getUserByEmail(email)

    // -----------------------------------------------------------------------------------------
    // BUDGET ALERT & GAMIFICATION
    // -----------------------------------------------------------------------------------------

    /**
     * Triggers a warning event if spending exceeds 80% of the max goal.
     */
    fun checkBudgetAlert(spent: Double, maxGoal: Double) {
        if (maxGoal <= 0) return
        val percent = ((spent / maxGoal) * 100).toInt()
        if (percent >= 80 && _budgetWarningEvent.value == null) {
            _budgetWarningEvent.postValue(percent)
        }
    }

    /**
     * Logic for awarding "Budget Hero" and "Savvy Saver" badges based on discipline.
     */
    fun checkUnderBudgetBadge(spent: Double, maxGoal: Double) = viewModelScope.launch {
        val user = currentUser.value ?: return@launch
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        if (maxGoal > 0 && spent < maxGoal) {
            // Award "Budget Hero" for staying under max budget
            if (!user.hasBadge(BadgeType.UNDER_BUDGET)) {
                val badge = BadgeType.UNDER_BUDGET.toEarnedBadge()
                val awarded = withContext(Dispatchers.IO) { repository.awardBadge(badge) }
                if (awarded) _badgeUnlockedEvent.postValue(badge)
            }

            // Increment under-budget streak
            if (user.lastBudgetCheckDate < today) {
                val newDays = user.underBudgetDays + 1
                val updatedUser = user.copy(underBudgetDays = newDays, lastBudgetCheckDate = today)
                withContext(Dispatchers.IO) { repository.updateUser(updatedUser) }

                // Award SAVER_7 badge
                if (newDays >= 7 && !user.hasBadge(BadgeType.SAVER_7)) {
                    val saverBadge = BadgeType.SAVER_7.toEarnedBadge()
                    val awarded = withContext(Dispatchers.IO) { repository.awardBadge(saverBadge) }
                    if (awarded) _badgeUnlockedEvent.postValue(saverBadge)
                }
            }
        } else if (maxGoal > 0 && spent >= maxGoal) {
            // Reset streak on overspending
            if (user.underBudgetDays > 0) {
                withContext(Dispatchers.IO) {
                    repository.updateUser(user.copy(underBudgetDays = 0, lastBudgetCheckDate = today))
                }
            }
        }
    }

    /**
     * Core progression logic: Increments XP and handles Level-Up events.
     */
    private fun addXp(amount: Int, userSnapshot: UserModel? = null) = viewModelScope.launch {
        val userValue = userSnapshot ?: currentUser.value ?: return@launch
        var newXp     = userValue.xp + amount
        var newLevel  = userValue.level
        val xpNeeded  = newLevel * 100

        if (newXp >= xpNeeded) {
            newXp -= xpNeeded
            newLevel++
            _levelUpEvent.postValue(newLevel)
            checkLevelBadge(newLevel, userValue)
        }

        val updatedUser = userValue.copy(xp = newXp, level = newLevel)
        withContext(Dispatchers.IO) { repository.updateUser(updatedUser) }
    }

    /**
     * Logic for maintaining the activity streak.
     */
    private fun updateStreak(userSnapshot: UserModel? = null) = viewModelScope.launch {
        val userValue = userSnapshot ?: currentUser.value ?: return@launch
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val lastDate  = userValue.lastActionDate
        val yesterday = today - (24 * 60 * 60 * 1000)

        if (lastDate < today) {
            val newStreak = if (lastDate >= yesterday) userValue.streak + 1 else 1
            val updatedUser = userValue.copy(streak = newStreak, lastActionDate = today)
            withContext(Dispatchers.IO) { repository.updateUser(updatedUser) }
            addXp(50, updatedUser)
            checkStreakBadge(newStreak, updatedUser)
        }
    }

    // -----------------------------------------------------------------------------------------
    // BADGE ELIGIBILITY CHECKS
    // -----------------------------------------------------------------------------------------

    private fun checkFirstExpenseBadge(userSnapshot: UserModel? = null) = viewModelScope.launch {
        val user = userSnapshot ?: currentUser.value ?: return@launch
        if (!user.hasBadge(BadgeType.FIRST_EXPENSE)) {
            val badge   = BadgeType.FIRST_EXPENSE.toEarnedBadge()
            val awarded = withContext(Dispatchers.IO) { repository.awardBadge(badge) }
            if (awarded) _badgeUnlockedEvent.postValue(badge)
        }
    }

    private fun checkStreakBadge(streak: Int, userSnapshot: UserModel? = null) = viewModelScope.launch {
        if (streak >= 7) {
            val user    = userSnapshot ?: currentUser.value ?: return@launch
            if (!user.hasBadge(BadgeType.STREAK_7)) {
                val badge   = BadgeType.STREAK_7.toEarnedBadge()
                val awarded = withContext(Dispatchers.IO) { repository.awardBadge(badge) }
                if (awarded) _badgeUnlockedEvent.postValue(badge)
            }
        }
    }

    private fun checkLevelBadge(level: Int, userSnapshot: UserModel? = null) = viewModelScope.launch {
        if (level >= 5) {
            val user    = userSnapshot ?: currentUser.value ?: return@launch
            if (!user.hasBadge(BadgeType.LEVEL_5)) {
                val badge   = BadgeType.LEVEL_5.toEarnedBadge()
                val awarded = withContext(Dispatchers.IO) { repository.awardBadge(badge) }
                if (awarded) _badgeUnlockedEvent.postValue(badge)
            }
        }
    }
}
