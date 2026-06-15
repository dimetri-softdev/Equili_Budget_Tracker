package com.example.equili.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.equili.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await

/**
 * ExpenseRepository handles data operations between the application and Firebase Realtime Database.
 * It follows the Repository Pattern to abstract data sources and provide a clean API to the ViewModel.
 *
 * Data Structure in Firebase:
 * users/
 *   {uid}/
 *     profile/   -> User details, level, XP, badges
 *     expenses/  -> List of logged expenses
 *     categories/ -> User-defined categories
 *     goals/     -> Monthly budget goals
 */
class ExpenseRepository {

    // Reference to the root of the Realtime Database
    private val db = FirebaseDatabase.getInstance().reference

    // Instance of Firebase Auth to retrieve user identity
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()

    /** Returns the unique identifier (UID) of the currently authenticated user. */
    private fun getUid(): String = auth.currentUser?.uid ?: ""

    // =========================================================================
    // EXPENSE OPERATIONS
    // =========================================================================

    /**
     * Inserts a new expense or updates an existing one in the cloud.
     * Uses [push()] for new entries to generate a unique ID.
     */
    suspend fun insertExpense(expense: ExpenseModel) {
        val uid = getUid()
        if (uid.isEmpty()) return

        expense.userId = uid
        val userExpensesRef = db.child("users").child(uid).child("expenses")

        if (expense.id.isEmpty()) {
            val newRef = userExpensesRef.push()
            expense.id = newRef.key ?: ""
            newRef.setValue(expense).await()
        } else {
            userExpensesRef.child(expense.id).setValue(expense).await()
        }
    }

    /**
     * Deletes an expense by its unique ID from the user's expense list.
     */
    suspend fun deleteExpense(expense: ExpenseModel) {
        val uid = getUid()
        if (uid.isEmpty() || expense.id.isEmpty()) return
        db.child("users").child(uid).child("expenses").child(expense.id).removeValue().await()
    }

    /**
     * Retrieves all user expenses and listens for real-time updates.
     * Results are sorted by date in descending order (newest first).
     */
    fun getAllExpenses(): LiveData<List<ExpenseModel>> {
        val liveData = MutableLiveData<List<ExpenseModel>>()
        val uid = getUid()
        if (uid.isEmpty()) {
            liveData.value = emptyList()
            return liveData
        }

        db.child("users").child(uid).child("expenses")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val items = mutableListOf<ExpenseModel>()
                    snapshot.children.forEach { child ->
                        child.getValue(ExpenseModel::class.java)?.let { items.add(it) }
                    }
                    liveData.value = items.sortedByDescending { it.date }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        return liveData
    }

    /**
     * Retrieves a list of expenses filtered within a specific timestamp range.
     * Useful for monthly dashboard views and analytics.
     */
    fun getExpensesInRange(start: Long, end: Long): LiveData<List<ExpenseModel>> {
        val liveData = MutableLiveData<List<ExpenseModel>>()
        val uid = getUid()
        if (uid.isEmpty()) return liveData

        db.child("users").child(uid).child("expenses")
            .orderByChild("date")
            .startAt(start.toDouble())
            .endAt(end.toDouble())
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val items = mutableListOf<ExpenseModel>()
                    snapshot.children.forEach { child ->
                        child.getValue(ExpenseModel::class.java)?.let { items.add(it) }
                    }
                    liveData.value = items.sortedByDescending { it.date }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        return liveData
    }

    /**
     * Aggregates spending totals by category for a given date range.
     * Returns a list of [CategoryTotal] objects.
     */
    fun getCategoryTotalsInRange(start: Long, end: Long): LiveData<List<CategoryTotal>> {
        val result = MutableLiveData<List<CategoryTotal>>()
        val uid = getUid()
        if (uid.isEmpty()) return result

        db.child("users").child(uid).child("expenses")
            .orderByChild("date")
            .startAt(start.toDouble())
            .endAt(end.toDouble())
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<ExpenseModel>()
                    snapshot.children.forEach { child ->
                        child.getValue(ExpenseModel::class.java)?.let { list.add(it) }
                    }
                    val totals = list.groupBy { it.category }
                        .map { (name, items) -> CategoryTotal(name, items.sumOf { it.amount }) }
                    result.value = totals
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        return result
    }

    // =========================================================================
    // CATEGORY OPERATIONS
    // =========================================================================

    /**
     * Retrieves all spending categories available for the user.
     * If no categories exist (e.g., for a new user), it automatically populates defaults.
     */
    fun getAllCategories(): LiveData<List<CategoryModel>> {
        val liveData = MutableLiveData<List<CategoryModel>>()
        val uid = getUid()
        if (uid.isEmpty()) return liveData

        db.child("users").child(uid).child("categories")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val items = mutableListOf<CategoryModel>()
                    if (snapshot.exists()) {
                        snapshot.children.forEach { child ->
                            child.getValue(CategoryModel::class.java)?.let { items.add(it) }
                        }
                    } else {
                        populateDefaultCategories(uid)
                    }
                    liveData.value = items.sortedBy { it.name }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        return liveData
    }

    /**
     * Populates the database with a default set of expense categories for a new user.
     */
    private fun populateDefaultCategories(uid: String) {
        val defaults = mapOf(
            "Food" to "ic_food",
            "Transport" to "ic_transport",
            "Shopping" to "ic_shopping",
            "Entertainment" to "ic_entertainment",
            "Health" to "ic_health",
            "Utilities" to "ic_utilities",
            "Games" to "ic_games",
            "Sports" to "ic_sports"
        )
        val ref = db.child("users").child(uid).child("categories")
        defaults.forEach { (name, icon) ->
            val newRef = ref.push()
            newRef.setValue(CategoryModel(id = newRef.key ?: "", userId = uid, name = name, icon = icon))
        }
    }

    /**
     * Inserts a new custom category into the user's cloud profile.
     */
    suspend fun insertCategory(category: CategoryModel) {
        val uid = getUid()
        if (uid.isEmpty()) return
        category.userId = uid
        val userCategoriesRef = db.child("users").child(uid).child("categories")

        if (category.id.isEmpty()) {
            val newRef = userCategoriesRef.push()
            category.id = newRef.key ?: ""
            newRef.setValue(category).await()
        } else {
            userCategoriesRef.child(category.id).setValue(category).await()
        }
    }

    // =========================================================================
    // GOAL OPERATIONS
    // =========================================================================

    /**
     * Retrieves the budget goals (min/max) for a specific month.
     * Month format: YYYY-MM
     */
    fun getMonthlyGoal(month: String): LiveData<GoalModel?> {
        val liveData = MutableLiveData<GoalModel?>()
        val uid = getUid()
        if (uid.isEmpty()) return liveData

        db.child("users").child(uid).child("goals").child(month)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    liveData.value = snapshot.getValue(GoalModel::class.java)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        return liveData
    }

    /**
     * One-shot fetch of a specific month\u0027s goal.
     * Primarily used for month-to-month data migration/carry-over logic.
     */
    suspend fun getGoalSnapshot(month: String): GoalModel? {
        val uid = getUid()
        if (uid.isEmpty()) return null
        return db.child("users").child(uid).child("goals").child(month).get().await()
            .getValue(GoalModel::class.java)
    }

    /**
     * Saves or updates the budget goals for a specific month.
     */
    suspend fun updateGoal(goal: GoalModel) {
        val uid = getUid()
        if (uid.isEmpty() || goal.month.isEmpty()) return
        goal.userId = uid
        db.child("users").child(uid).child("goals").child(goal.month).setValue(goal).await()
    }

    // =========================================================================
    // USER PROFILE & GAMIFICATION
    // =========================================================================

    /**
     * Registers a new user profile in the database upon successful authentication.
     */
    suspend fun registerUser(user: UserModel) {
        if (user.uid.isNotEmpty()) {
            db.child("users").child(user.uid).child("profile").setValue(user).await()
        }
    }

    /**
     * Searches the database for a user profile matching the given email.
     */
    suspend fun getUserByEmail(email: String): UserModel? {
        val snapshot = db.child("users").get().await()
        for (userSnapshot in snapshot.children) {
            val profile = userSnapshot.child("profile").getValue(UserModel::class.java)
            if (profile?.email == email) return profile
        }
        return null
    }

    /**
     * Returns a real-time LiveData stream for the currently logged-in user\u0027s profile.
     */
    fun getCurrentUserLiveData(): LiveData<UserModel?> {
        val liveData = MutableLiveData<UserModel?>()
        val uid = getUid()
        if (uid.isEmpty()) return liveData

        db.child("users").child(uid).child("profile")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    liveData.value = snapshot.getValue(UserModel::class.java)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        return liveData
    }

    /**
     * Updates user profile fields (Level, XP, Address, etc.) in the cloud.
     */
    suspend fun updateUser(user: UserModel) {
        val uid = getUid()
        if (uid.isEmpty()) return
        db.child("users").child(uid).child("profile").setValue(user).await()
    }

    /**
     * Awards an achievement badge to the user.
     * Checks for prior ownership to prevent redundant event triggers in the UI.
     */
    suspend fun awardBadge(badge: BadgeModel): Boolean {
        val uid = getUid()
        if (uid.isEmpty()) return false

        // Atomically check if already earned
        val snapshot = db.child("users").child(uid).child("profile")
            .child("badges").child(badge.id).get().await()
        val existing = snapshot.getValue(BadgeModel::class.java)
        if (existing?.earned == true) return false

        db.child("users").child(uid).child("profile")
            .child("badges").child(badge.id).setValue(badge).await()
        return true
    }
}
