package com.example.equili.data.model

import java.io.Serializable

/**
 * BadgeModel represents an achievement badge that can be unlocked by the user.
 * Badges are stored as a list on the UserModel in Firebase.
 *
 * Each badge has:
 * - [id]          → unique string identifier (matches BadgeType enum name)
 * - [name]        → display title shown in the badge dialog
 * - [description] → short description of how it was earned
 * - [iconRes]     → resource ID of the badge drawable (set at runtime, not stored in Firebase)
 * - [earned]      → whether the user has unlocked this badge
 */
data class BadgeModel(
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var earned: Boolean = false
) : Serializable {
    // No-arg constructor required for Firebase Realtime Database deserialization
    constructor() : this("", "", "", false)
}

/**
 * Enum of all badge types the app supports.
 * Used to check whether a badge should be awarded and to build [BadgeModel] instances.
 */
enum class BadgeType(
    val id: String,
    val displayName: String,
    val description: String
) {
    FIRST_EXPENSE(
        id          = "first_expense",
        displayName = "First Step",
        description = "You logged your very first expense! The journey to financial clarity begins."
    ),
    STREAK_7(
        id          = "streak_7",
        displayName = "On Fire 🔥",
        description = "You maintained a 7-day activity streak. Keep the momentum going!"
    ),
    UNDER_BUDGET(
        id          = "under_budget",
        displayName = "Budget Hero",
        description = "You stayed under your maximum budget goal this month. Well done!"
    ),
    LEVEL_5(
        id          = "level_5",
        displayName = "Rising Star",
        description = "You reached Level 5. You're becoming a budgeting pro!"
    ),
    SAVER_7(
        id          = "saver_7",
        displayName = "Savvy Saver 💰",
        description = "You stayed under your budget for 7 consecutive days. Outstanding discipline!"
    );

    /** Builds a [BadgeModel] instance for this badge type, pre-marked as earned. */
    fun toEarnedBadge() = BadgeModel(
        id          = id,
        name        = displayName,
        description = description,
        earned      = true
    )
}
