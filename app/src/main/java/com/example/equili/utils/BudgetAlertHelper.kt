package com.example.equili.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.equili.DashboardActivity
import com.example.equili.R

/**
 * BudgetAlertHelper centralises all budget-alert notification logic for Equili.
 *
 * Responsibilities:
 *  1. Creates the Android notification channel (required on API 26+).
 *  2. Fire a system notification when the user crosses the 80% budget threshold.
 *  3. Provide reusable percentage-tier helpers consumed by both the ViewModel and UI.

 */
object BudgetAlertHelper {

    private const val CHANNEL_ID   = "equili_budget_alerts"
    private const val CHANNEL_NAME = "Budget Alerts"
    private const val NOTIF_ID_BUDGET = 1001

    // ------------------------------------------------------------------
    // Channel Setup
    // ------------------------------------------------------------------

    /**
     * Creates the notification channel. Safe to call multiple times; the system
     * ignores duplicate registrations.
     * Must be called before posting any notification (call from Application or Activity.onCreate).
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Warns you when spending approaches or exceeds your monthly goal."
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    // ------------------------------------------------------------------
    // Notification Delivery
    // ------------------------------------------------------------------

    fun postBudgetWarningNotification(context: Context, percent: Int) {
        val tapIntent = Intent(context, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (title, body) = when {
            percent >= 100 -> "🚨 Budget Exceeded!" to "You've spent over 100% of your monthly maximum."
            percent >= 90  -> "⛔ Critical: ${percent}% Used" to "You're very close to your spending limit. Be careful!"
            else           -> "⚠️ Budget Warning: ${percent}% Used" to "You've used ${percent}% of your monthly maximum goal."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_BUDGET, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted (Android 13+); silent fallback.
            // The in-app dialog still fires via ExpenseViewModel.budgetWarningEvent.
        }
    }

    // ------------------------------------------------------------------
    // Shared Helpers
    // ------------------------------------------------------------------

    /**
     * Calculates the spend percentage relative to a max goal.
     *
     * @return An integer 0–200+ capped only by actual spend (NOT coerced to 100).
     */
    fun spendPercent(spent: Double, maxGoal: Double): Int {
        if (maxGoal <= 0) return 0
        return ((spent / maxGoal) * 100).toInt()
    }

    /**
     * Returns true if [spent] has crossed the 80% warning threshold for [maxGoal].
     * Used as a lightweight check before posting notifications to avoid spam.
     */
    fun isOverWarningThreshold(spent: Double, maxGoal: Double): Boolean =
        spendPercent(spent, maxGoal) >= 80
}
