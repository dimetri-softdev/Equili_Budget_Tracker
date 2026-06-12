package com.example.equili

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.equili.data.model.BadgeModel
import com.example.equili.data.model.BadgeType
import com.example.equili.ui.viewModel.ExpenseViewModel
import com.example.equili.utils.BudgetAlertHelper
import com.google.firebase.auth.FirebaseAuth
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private val viewModel: ExpenseViewModel by viewModels()

    // Track last warned percentage to avoid spamming the warning dialog
    private var lastWarnedPercent: Int = -1

    // -------------------------------------------------------------------------
    // Runtime permission launcher for POST_NOTIFICATIONS (Android 13+)
    // -------------------------------------------------------------------------
    private val notifPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- SECURITY CHECK ---
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_dashboard)

        // -----------------------------------------------------------------------
        // Budget notification channel (safe to call multiple times)
        // -----------------------------------------------------------------------
        BudgetAlertHelper.createNotificationChannel(this)

        // Request POST_NOTIFICATIONS on Android 13+ so the system notification can appear
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // -----------------------------------------------------------------------
        // UI references
        // -----------------------------------------------------------------------
        val budgetAmount = findViewById<TextView>(R.id.budgetAmount)
        val tvGoals      = findViewById<TextView>(R.id.tvGoals)
        val tvLevel      = findViewById<TextView>(R.id.tvLevel)
        val tvStreak     = findViewById<TextView>(R.id.tvStreak)
        val xpProgress   = findViewById<ProgressBar>(R.id.xpProgress)
        val progressBar  = findViewById<ProgressBar>(R.id.progress)

        val addBtn       = findViewById<Button>(R.id.addBtn)
        val categoryBtn  = findViewById<Button>(R.id.categoryBtn)
        val historyBtn   = findViewById<Button>(R.id.historyBtn)
        val analyticsBtn = findViewById<Button>(R.id.analyticsBtn)
        val goalBtn      = findViewById<Button>(R.id.goalBtn)
        val logoutBtn    = findViewById<ImageButton>(R.id.btnLogout)
        val profileBtn   = findViewById<ImageButton>(R.id.btnProfile)

        // Navigation for User Profile
        profileBtn.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // -----------------------------------------------------------------------
        // Date Range (current month)
        // -----------------------------------------------------------------------
        val start = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }
        val end = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
        }
        viewModel.setDateRange(start.timeInMillis, end.timeInMillis)

        // -----------------------------------------------------------------------
        // Observe spending + trigger Budget Alert
        // -----------------------------------------------------------------------
        viewModel.expensesInDateRange.observe(this) { expenses ->
            val spent = expenses?.sumOf { it.amount } ?: 0.0
            budgetAmount.text = String.format(Locale.getDefault(), "R%.2f Spent this month", spent)
            updateProgressBar(spent, progressBar)

            val maxGoal = viewModel.monthlyGoal.value?.maxGoal ?: 0.0
            if (maxGoal > 0) {
                viewModel.checkBudgetAlert(spent, maxGoal)
                // Check under-budget badge every time spending data refreshes
                viewModel.checkUnderBudgetBadge(spent, maxGoal)
            }
        }

        // -----------------------------------------------------------------------
        // Observe user profile (XP / Level / Streak)
        // -----------------------------------------------------------------------
        viewModel.currentUser.observe(this) { userProfile ->
            if (userProfile != null) {
                tvLevel.text  = "Level ${userProfile.level}"
                tvStreak.text = userProfile.streak.toString()
                val xpNeeded  = userProfile.level * 100
                xpProgress.max      = xpNeeded
                xpProgress.progress = userProfile.xp
            }
        }

        // -----------------------------------------------------------------------
        // Observe monthly goal
        // -----------------------------------------------------------------------
        viewModel.monthlyGoal.observe(this) { goal ->
            if (goal != null) {
                tvGoals.text = String.format(
                    Locale.getDefault(),
                    "Goal: Min R%.0f - Max R%.0f",
                    goal.minGoal, goal.maxGoal
                )
                val spent = viewModel.expensesInDateRange.value?.sumOf { it.amount } ?: 0.0
                updateProgressBar(spent, progressBar)
                viewModel.checkBudgetAlert(spent, goal.maxGoal)
            } else {
                tvGoals.text = "Goal: Not set"
            }
        }

        // -----------------------------------------------------------------------
        // ALERT SYSTEM: Budget Warning (≥ 80%)
        //   → Shows in-app dialog AND posts a system notification
        // -----------------------------------------------------------------------
        viewModel.budgetWarningEvent.observe(this) { percent ->
            if (percent != null && percent != lastWarnedPercent) {
                lastWarnedPercent = percent
                // In-app dialog
                showBudgetWarningDialog(percent)
                // System tray notification (fires even when app is backgrounded next time)
                BudgetAlertHelper.postBudgetWarningNotification(this, percent)
                viewModel.consumeBudgetWarning()
            }
        }

        // -----------------------------------------------------------------------
        // ANIMATIONS: Level-Up overlay
        // -----------------------------------------------------------------------
        viewModel.levelUpEvent.observe(this) { newLevel ->
            if (newLevel != null) {
                showLevelUpOverlay(newLevel)
                viewModel.consumeLevelUpEvent()
            }
        }

        // -----------------------------------------------------------------------
        // GAMIFICATION: Badge Unlock dialog
        // -----------------------------------------------------------------------
        viewModel.badgeUnlockedEvent.observe(this) { badge ->
            if (badge != null) {
                showBadgeUnlockDialog(badge)
                viewModel.consumeBadgeEvent()
            }
        }

        // -----------------------------------------------------------------------
        // Navigation
        // -----------------------------------------------------------------------
        addBtn.setOnClickListener       { startActivity(Intent(this, ExpenseActivity::class.java)) }
        categoryBtn.setOnClickListener  { startActivity(Intent(this, CategoryActivity::class.java)) }
        historyBtn.setOnClickListener   { startActivity(Intent(this, HistoryActivity::class.java)) }
        analyticsBtn.setOnClickListener { startActivity(Intent(this, AnalyticsActivity::class.java)) }

        goalBtn.setOnClickListener { showGoalDialog() }

        logoutBtn.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            getSharedPreferences("EquiliPrefs", MODE_PRIVATE).edit().clear().apply()
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }

        // Bottom Navigation
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
            R.id.bottomNavigation
        )
        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home       -> true
                R.id.nav_history    -> { startActivity(Intent(this, HistoryActivity::class.java));   false }
                R.id.nav_analytics  -> { startActivity(Intent(this, AnalyticsActivity::class.java)); false }
                R.id.nav_categories -> { startActivity(Intent(this, CategoryActivity::class.java));  false }
                else -> false
            }
        }
    }

    // ===========================================================================
    // BUDGET ALERT DIALOG
    // ===========================================================================


    private fun showBudgetWarningDialog(percent: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_budget_warning, null)
        val tvPercent  = dialogView.findViewById<TextView>(R.id.tvWarningPercent)
        val pbWarning  = dialogView.findViewById<ProgressBar>(R.id.pbWarning)

        tvPercent.text = "$percent% of your budget used!"
        pbWarning.progress = percent.coerceIn(0, 100)

        // Bar color to match severity
        val barColor = when {
            percent >= 100 -> android.graphics.Color.parseColor("#FF1744")
            percent >= 90  -> android.graphics.Color.parseColor("#FF3D00")
            else           -> android.graphics.Color.parseColor("#FF6D00")
        }
        pbWarning.progressTintList = android.content.res.ColorStateList.valueOf(barColor)

        // Shake animation on the progress bar to draw attention
        val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
        pbWarning.startAnimation(shake)

        val title = when {
            percent >= 100 -> "🚨 Budget Exceeded!"
            percent >= 90  -> "⛔ Critical Budget Alert"
            else           -> "⚠️ Budget Warning"
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(dialogView)
            .setMessage(
                "You've used $percent% of your maximum monthly budget.\n\n" +
                "Review your recent expenses or consider adjusting your goal."
            )
            .setPositiveButton("Review Expenses") { _, _ ->
                startActivity(Intent(this, HistoryActivity::class.java))
            }
            .setNeutralButton("Adjust Goal") { _, _ ->
                showGoalDialog()
            }
            .setNegativeButton("Dismiss", null)
            .show()
    }

    // ===========================================================================
    // LEVEL UP OVERLAY
    // ===========================================================================

    private fun showLevelUpOverlay(newLevel: Int) {
        val root = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(
            R.id.dashboardRoot
        ) ?: return

        val overlay    = LayoutInflater.from(this).inflate(R.layout.layout_level_up_overlay, root, false)
        val ivIcon     = overlay.findViewById<ImageView>(R.id.ivLevelUpIcon)
        val tvSubtitle = overlay.findViewById<TextView>(R.id.tvLevelUpSubtitle)
        tvSubtitle.text = "You reached Level $newLevel!"

        root.addView(overlay)
        overlay.visibility = View.VISIBLE

        // Confetti pop on the icon first (offset slightly inside the level_up anim)
        val confetti = AnimationUtils.loadAnimation(this, R.anim.confetti_pop)
        ivIcon.startAnimation(confetti)

        // Overall overlay fade / zoom
        val anim = AnimationUtils.loadAnimation(this, R.anim.level_up)
        anim.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(a: android.view.animation.Animation?) {}
            override fun onAnimationRepeat(a: android.view.animation.Animation?) {}
            override fun onAnimationEnd(a: android.view.animation.Animation?) {
                root.removeView(overlay)
            }
        })
        overlay.startAnimation(anim)

        // Toast for accessibility / users who miss the animation
        Toast.makeText(this, "🎉 Level Up! You are now Level $newLevel!", Toast.LENGTH_SHORT).show()
    }

    // ===========================================================================
    // BADGE UNLOCK DIALOG
    // ===========================================================================

    private fun showBadgeUnlockDialog(badge: BadgeModel) {
        val view   = LayoutInflater.from(this).inflate(R.layout.dialog_badge_unlock, null)
        val ivIcon = view.findViewById<ImageView>(R.id.ivBadgeIcon)
        val tvName = view.findViewById<TextView>(R.id.tvBadgeName)
        val tvDesc = view.findViewById<TextView>(R.id.tvBadgeDesc)

        tvName.text = badge.name
        tvDesc.text = badge.description

        val iconRes = when (badge.id) {
            BadgeType.FIRST_EXPENSE.id -> R.drawable.ic_badge_first_expense
            BadgeType.STREAK_7.id      -> R.drawable.ic_badge_streak
            BadgeType.UNDER_BUDGET.id  -> R.drawable.ic_badge_under_budget
            BadgeType.LEVEL_5.id       -> R.drawable.ic_badge_level_up
            BadgeType.SAVER_7.id       -> R.drawable.ic_badge_saver
            else                       -> R.drawable.ic_badge_first_expense
        }
        ivIcon.setImageResource(iconRes)

        val slideIn = AnimationUtils.loadAnimation(this, R.anim.badge_unlock)
        ivIcon.startAnimation(slideIn)

        AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Awesome! 🎉", null)
            .show()
    }

    // ===========================================================================
    // GOAL DIALOG (with save pulse animation)
    // ===========================================================================

    private fun showGoalDialog() {
        val view  = LayoutInflater.from(this).inflate(R.layout.dialog_goals, null)
        val etMin = view.findViewById<EditText>(R.id.etMinGoal)
        val etMax = view.findViewById<EditText>(R.id.etMaxGoal)

        // Pre-fill with current goal values if available
        viewModel.monthlyGoal.value?.let { goal ->
            if (goal.minGoal > 0) etMin.setText(goal.minGoal.toInt().toString())
            if (goal.maxGoal > 0) etMax.setText(goal.maxGoal.toInt().toString())
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Set Monthly Goals")
            .setView(view)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val saveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            saveBtn.setOnClickListener {
                val pulse = AnimationUtils.loadAnimation(this, R.anim.save_pulse)
                saveBtn.startAnimation(pulse)

                val min = etMin.text.toString().toDoubleOrNull() ?: 0.0
                val max = etMax.text.toString().toDoubleOrNull() ?: 1000.0

                if (max <= 0) {
                    Toast.makeText(this, "Maximum goal must be greater than zero", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (min > max) {
                    Toast.makeText(this, "Minimum goal cannot exceed maximum goal", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                viewModel.updateGoal(min, max)
                Toast.makeText(this, "✅ Goals Updated! R%.0f – R%.0f".format(min, max), Toast.LENGTH_SHORT).show()

                // Reset warning state so the alert can re-fire with the new goal
                lastWarnedPercent = -1

                dialog.dismiss()
            }
        }
        dialog.show()
    }

    // ===========================================================================
    // PROGRESS BAR HELPER
    // ===========================================================================

    private fun updateProgressBar(spent: Double, progressBar: ProgressBar) {
        val goal = viewModel.monthlyGoal.value
        val max  = goal?.maxGoal ?: 1000.0
        val progressPercent = if (max > 0) ((spent / max) * 100).toInt() else 0
        progressBar.progress = progressPercent.coerceIn(0, 100)

        val tintColor = when {
            progressPercent >= 100 -> android.graphics.Color.parseColor("#FF1744") // Red
            progressPercent >= 80  -> android.graphics.Color.parseColor("#FF6D00") // Orange-Red
            progressPercent >= 50  -> android.graphics.Color.parseColor("#FFA000") // Amber
            else                   -> android.graphics.Color.parseColor("#00FFFF") // Cyan (brand)
        }
        progressBar.progressTintList = android.content.res.ColorStateList.valueOf(tintColor)
    }
}
