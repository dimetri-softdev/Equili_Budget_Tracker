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
 * ExpenseRepository now uses Firebase Realtime Database for cloud data storage.
 * It manages expenses, categories, goals, and user profiles securely using the user's UID.
 */
class ExpenseRepository {

    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    /** Returns the current logged-in user's UID or an empty string. */
    private fun getUid(): String = auth.currentUser?.uid ?: ""

    // --- Expense Operations ---

    /**
     * Saves or updates an expense in the cloud.
     * Path: users/{uid}/expenses/{expenseId}
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

    suspend fun deleteExpense(expense: ExpenseModel) {
        val uid = getUid()
        if (uid.isEmpty() || expense.id.isEmpty()) return
        db.child("users").child(uid).child("expenses").child(expense.id).removeValue().await()
    }

    /** Retrieves all user expenses as LiveData (Real-time). */
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

    /** Retrieves expenses filtered by date range. */
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

    /** Aggregates category totals from Realtime DB. */
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

    // --- Category Operations ---

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
                        // Populate default categories if none exist in the cloud
                        populateDefaultCategories(uid)
                    }
                    liveData.value = items.sortedBy { it.name }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        return liveData
    }

    /** Helper to populate initial categories for a new user. */
    private fun populateDefaultCategories(uid: String) {
        val defaults = mapOf(
            "Food" to "ic_food",
            "Transport" to "ic_transport",
            "Shopping" to "ic_categories",
            "Entertainment" to "ic_categories",
            "Health" to "ic_categories",
            "Utilities" to "ic_categories"
        )
        val ref = db.child("users").child(uid).child("categories")
        defaults.forEach { (name, icon) ->
            val newRef = ref.push()
            newRef.setValue(CategoryModel(id = newRef.key ?: "", userId = uid, name = name, icon = icon))
        }
    }

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

    // --- Goal Operations ---

    /**
     * Retrieves the goal for a specific month.
     * Path: users/{uid}/goals/{month}
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

    /** One-shot fetch of a goal for transition logic. */
    suspend fun getGoalSnapshot(month: String): GoalModel? {
        val uid = getUid()
        if (uid.isEmpty()) return null
        return db.child("users").child(uid).child("goals").child(month).get().await()
            .getValue(GoalModel::class.java)
    }

    /**
     * Saves or updates a goal for its specific month.
     */
    suspend fun updateGoal(goal: GoalModel) {
        val uid = getUid()
        if (uid.isEmpty() || goal.month.isEmpty()) return
        goal.userId = uid
        db.child("users").child(uid).child("goals").child(goal.month).setValue(goal).await()
    }

    // --- User Profile Operations ---

    suspend fun registerUser(user: UserModel) {
        if (user.uid.isNotEmpty()) {
            db.child("users").child(user.uid).child("profile").setValue(user).await()
        }
    }

    suspend fun getUserByEmail(email: String): UserModel? {
        val snapshot = db.child("users").get().await()
        for (userSnapshot in snapshot.children) {
            val profile = userSnapshot.child("profile").getValue(UserModel::class.java)
            if (profile?.email == email) return profile
        }
        return null
    }

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

    suspend fun updateUser(user: UserModel) {
        val uid = getUid()
        if (uid.isEmpty()) return
        db.child("users").child(uid).child("profile").setValue(user).await()
    }

    /**
     * Awards a badge to the current user if they don't already have it.
     * Writes a single badge entry into the user's badges map in Firebase.
     * Returns true if the badge was newly awarded, false if already owned.
     */
    suspend fun awardBadge(badge: com.example.equili.data.model.BadgeModel): Boolean {
        val uid = getUid()
        if (uid.isEmpty()) return false

        // Only write the badge if not already earned (avoid duplicate award events)
        val snapshot = db.child("users").child(uid).child("profile")
            .child("badges").child(badge.id).get().await()
        val existing = snapshot.getValue(com.example.equili.data.model.BadgeModel::class.java)
        if (existing?.earned == true) return false

        db.child("users").child(uid).child("profile")
            .child("badges").child(badge.id).setValue(badge).await()
        return true
    }
}
