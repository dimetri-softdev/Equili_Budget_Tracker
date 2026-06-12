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

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ExpenseRepository()

    private val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

    private val dateRange   = MutableLiveData<Pair<Long, Long>>()
    private val searchQuery = MutableLiveData("")
    private val sortOption  = MutableLiveData(SortOption.DATE_DESC)

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
     * The UI should show a warning dialog and call [consumeBudgetWarning] after displaying.
     */
    private val _budgetWarningEvent = MutableLiveData<Int?>(null)
    val budgetWarningEvent: LiveData<Int?> get() = _budgetWarningEvent

    /**
     * Emits the new level number when the user levels up.
     * The UI should trigger the Level Up animation and call [consumeLevelUpEvent] after displaying.
     */
    private val _levelUpEvent = MutableLiveData<Int?>(null)
    val levelUpEvent: LiveData<Int?> get() = _levelUpEvent

    /**
     * Emits a [BadgeModel] when a badge is newly awarded.
     * The UI should show the badge dialog and call [consumeBadgeEvent] after displaying.
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

    val expensesInDateRange: LiveData<List<ExpenseModel>> = dateRange.switchMap { range ->
        repository.getExpensesInRange(range.first, range.second)
    }

    /**
     * Expenses filtered by search query and sorted by selected option.
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

    fun setDateRange(start: Long, end: Long) { dateRange.value = start to end }
    fun setSearchQuery(query: String)        { searchQuery.value = query }
    fun setSortOption(option: SortOption)    { sortOption.value = option }

    /** Not needed now because Firebase Auth manages the session. Kept for legacy compatibility. */
    fun setCurrentUser(email: String) {}

    fun consumeBudgetWarning() { _budgetWarningEvent.value = null }
    fun consumeLevelUpEvent()  { _levelUpEvent.value = null }
    fun consumeBadgeEvent()    { _badgeUnlockedEvent.value = null }
    fun consumeNewMonthEvent() { _newMonthEvent.value = null }

    // -----------------------------------------------------------------------------------------
    // MONTH TRANSITION
    // -----------------------------------------------------------------------------------------

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

                // 3. Update user's last processed month
                repository.updateUser(user.copy(lastProcessedMonth = currentMonth))
            }

            // 4. Notify UI of new month (can be used to show a toast or dialog)
            _newMonthEvent.postValue(currentMonth)
        }
    }

    // -----------------------------------------------------------------------------------------
    // CRUD
    // -----------------------------------------------------------------------------------------

    fun insertExpense(expense: ExpenseModel) = viewModelScope.launch {
        // Snapshot user on Main thread before switching to IO
        val userSnapshot = currentUser.value
        withContext(Dispatchers.IO) {
            repository.insertExpense(expense)
        }
        addXp(15, userSnapshot)
        updateStreak(userSnapshot)
        checkFirstExpenseBadge(userSnapshot)
    }

    fun deleteExpense(expense: ExpenseModel) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteExpense(expense)
    }

    fun insertCategory(category: CategoryModel) = viewModelScope.launch {
        val userSnapshot = currentUser.value
        withContext(Dispatchers.IO) { repository.insertCategory(category) }
        addXp(25, userSnapshot)
    }

    fun updateGoal(min: Double, max: Double) = viewModelScope.launch {
        val userSnapshot = currentUser.value
        withContext(Dispatchers.IO) {
            repository.updateGoal(GoalModel(minGoal = min, maxGoal = max, month = currentMonth))
        }
        addXp(20, userSnapshot)
    }

    fun getCategoryTotalsInRange(start: Long, end: Long): LiveData<List<CategoryTotal>> =
        repository.getCategoryTotalsInRange(start, end)

    suspend fun registerUser(user: UserModel) = repository.registerUser(user)

    suspend fun getUserByEmail(email: String): UserModel? = repository.getUserByEmail(email)

    // -----------------------------------------------------------------------------------------
    // BUDGET ALERT
    // -----------------------------------------------------------------------------------------

    fun checkBudgetAlert(spent: Double, maxGoal: Double) {
        if (maxGoal <= 0) return
        val percent = ((spent / maxGoal) * 100).toInt()
        if (percent >= 80 && _budgetWarningEvent.value == null) {
            _budgetWarningEvent.postValue(percent)
        }
    }

    fun checkUnderBudgetBadge(spent: Double, maxGoal: Double) = viewModelScope.launch {
        val user = currentUser.value ?: return@launch
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        if (maxGoal > 0 && spent < maxGoal) {
            // 1. Award one-time "Budget Hero" badge
            if (!user.hasBadge(BadgeType.UNDER_BUDGET)) {
                val badge = BadgeType.UNDER_BUDGET.toEarnedBadge()
                val awarded = withContext(Dispatchers.IO) { repository.awardBadge(badge) }
                if (awarded) _badgeUnlockedEvent.postValue(badge)
            }

            // 2. Increment "Savvy Saver" counter only once per day
            if (user.lastBudgetCheckDate < today) {
                val newDays = user.underBudgetDays + 1
                val updatedUser = user.copy(underBudgetDays = newDays, lastBudgetCheckDate = today)
                withContext(Dispatchers.IO) { repository.updateUser(updatedUser) }

                // Award SAVER_7 after 7 consecutive under-budget days
                if (newDays >= 7 && !user.hasBadge(BadgeType.SAVER_7)) {
                    val saverBadge = BadgeType.SAVER_7.toEarnedBadge()
                    val awarded = withContext(Dispatchers.IO) { repository.awardBadge(saverBadge) }
                    if (awarded) _badgeUnlockedEvent.postValue(saverBadge)
                }
            }
        } else if (maxGoal > 0 && spent >= maxGoal) {
            // Reset saver streak if they go over budget
            if (user.underBudgetDays > 0) {
                withContext(Dispatchers.IO) {
                    repository.updateUser(user.copy(underBudgetDays = 0, lastBudgetCheckDate = today))
                }
            }
        }
    }

    // -----------------------------------------------------------------------------------------
    // XP & LEVELLING (thread-safe: user snapshot passed in from Main)
    // -----------------------------------------------------------------------------------------

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

    // -----------------------------------------------------------------------------------------
    // STREAK
    // -----------------------------------------------------------------------------------------

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
    // BADGE CHECKS
    // -----------------------------------------------------------------------------------------

    /** Awards the "First Step" badge on the very first expense logged. */
    private fun checkFirstExpenseBadge(userSnapshot: UserModel? = null) = viewModelScope.launch {
        val user = userSnapshot ?: currentUser.value ?: return@launch
        if (!user.hasBadge(BadgeType.FIRST_EXPENSE)) {
            val badge   = BadgeType.FIRST_EXPENSE.toEarnedBadge()
            val awarded = withContext(Dispatchers.IO) { repository.awardBadge(badge) }
            if (awarded) _badgeUnlockedEvent.postValue(badge)
        }
    }

    /** Awards the "On Fire 🔥" badge when the user hits a 7-day streak. */
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


    /** Awards the "Rising Star" badge when the user reaches level 5. */
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
