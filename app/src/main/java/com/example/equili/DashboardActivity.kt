package com.example.equili

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.equili.ui.viewModel.ExpenseViewModel
import com.google.firebase.auth.FirebaseAuth
import java.util.*

/**
 * DashboardActivity is the main hub of the application.
 * It displays current spending, user level, streaks, and provides navigation to all features.
 */
class DashboardActivity : AppCompatActivity() {

    private val viewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- SECURITY CHECK START ---
        // Ensure user is truly logged into Firebase before showing data
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            // No authenticated user, force redirect to Login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        // Sync the session for legacy parts of the app
        viewModel.setCurrentUser(user.email ?: "")
        // --- SECURITY CHECK END ---

        setContentView(R.layout.activity_dashboard)

        // UI Initialization
        val budgetAmount = findViewById<TextView>(R.id.budgetAmount)
        val tvGoals = findViewById<TextView>(R.id.tvGoals)
        val tvLevel = findViewById<TextView>(R.id.tvLevel)
        val tvStreak = findViewById<TextView>(R.id.tvStreak)
        val xpProgress = findViewById<ProgressBar>(R.id.xpProgress)

        val addBtn = findViewById<Button>(R.id.addBtn)
        val categoryBtn = findViewById<Button>(R.id.categoryBtn)
        val historyBtn = findViewById<Button>(R.id.historyBtn)
        val analyticsBtn = findViewById<Button>(R.id.analyticsBtn)
        val goalBtn = findViewById<Button>(R.id.goalBtn)
        val logoutBtn = findViewById<ImageButton>(R.id.btnLogout)
        val themeBtn = findViewById<ImageButton>(R.id.btnTheme)

        // Theme Toggle Logic: Switches between Light and Dark mode
        val isNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
        themeBtn.setImageResource(if (isNightMode) android.R.drawable.ic_menu_day else android.R.drawable.ic_menu_recent_history)

        themeBtn.setOnClickListener {
            val isCurrentlyNight = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
            if (isCurrentlyNight) {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
            } else {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
            }
        }

        // Set the default date range for dashboard stats (current calendar month)
        val start = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val end = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }
        viewModel.setDateRange(start.timeInMillis, end.timeInMillis)

        // Observe and update total monthly spending
        viewModel.expensesInDateRange.observe(this) { expenses ->
            val spent = expenses?.sumOf { it.amount } ?: 0.0
            budgetAmount.text = String.format(Locale.getDefault(), "R%.2f Spent this month", spent)
            updateProgressBar(spent)
        }

        // Observe user profile data (XP, Level, Streak) for gamification features
        viewModel.currentUser.observe(this) { user ->
            if (user != null) {
                tvLevel.text = "Level ${user.level}"
                tvStreak.text = user.streak.toString()

                val xpNeeded = user.level * 100
                xpProgress.max = xpNeeded
                xpProgress.progress = user.xp
            }
        }

        // Observe and display current monthly spending goals
        viewModel.monthlyGoal.observe(this) { goal ->
            if (goal != null) {
                tvGoals.text = String.format(Locale.getDefault(), "Goal: Min R%.0f - Max R%.0f", goal.minGoal, goal.maxGoal)
                val spent = viewModel.expensesInDateRange.value?.sumOf { it.amount } ?: 0.0
                updateProgressBar(spent)
            } else {
                tvGoals.text = "Goal: Not set"
            }
        }

        // Navigation button listeners
        addBtn.setOnClickListener { startActivity(Intent(this, ExpenseActivity::class.java)) }
        categoryBtn.setOnClickListener { startActivity(Intent(this, CategoryActivity::class.java)) }
        historyBtn.setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }
        analyticsBtn.setOnClickListener { startActivity(Intent(this, AnalyticsActivity::class.java)) }

        goalBtn.setOnClickListener { showGoalDialog() }

        // Logout: Clear session and return to Landing Screen
        logoutBtn.setOnClickListener {
            FirebaseAuth.getInstance().signOut() // Sign out from Firebase
            getSharedPreferences("EquiliPrefs", MODE_PRIVATE).edit().clear().apply()
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }

        // Bottom Navigation Interaction
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    false
                }
                R.id.nav_analytics -> {
                    startActivity(Intent(this, AnalyticsActivity::class.java))
                    false
                }
                R.id.nav_categories -> {
                    startActivity(Intent(this, CategoryActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }

    /**
     * Updates the UI progress bar relative to the user's maximum goal.
     */
    private fun updateProgressBar(spent: Double) {
        val goal = viewModel.monthlyGoal.value
        val max = goal?.maxGoal ?: 1000.0 // Fallback max goal
        val progressPercent = if (max > 0) ((spent / max) * 100).toInt() else 0
        findViewById<ProgressBar>(R.id.progress).progress = progressPercent.coerceIn(0, 100)
    }

    /**
     * Shows an AlertDialog for the user to input their minimum and maximum monthly goals.
     */
    private fun showGoalDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_goals, null)
        val etMin = view.findViewById<EditText>(R.id.etMinGoal)
        val etMax = view.findViewById<EditText>(R.id.etMaxGoal)

        AlertDialog.Builder(this)
            .setTitle("Set Monthly Goals")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val min = etMin.text.toString().toDoubleOrNull() ?: 0.0
                val max = etMax.text.toString().toDoubleOrNull() ?: 1000.0
                viewModel.updateGoal(min, max)
                Toast.makeText(this, "Goals Updated", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
