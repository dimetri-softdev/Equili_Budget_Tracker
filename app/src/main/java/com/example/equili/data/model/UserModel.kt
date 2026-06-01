package com.example.equili.data.model

import java.io.Serializable

/**
 * UserModel represents a registered user in the system.
 * Extended with badge/achievement tracking for gamification.
 */
data class UserModel(
    /** Unique user ID from Firebase Auth. */
    var uid: String = "",

    /** User's email address. */
    var email: String = "",

    /** Accumulated Experience Points (XP). */
    var xp: Int = 0,

    /** Current user level based on XP. */
    var level: Int = 1,

    /** Number of consecutive days of engagement. */
    var streak: Int = 0,

    /** Timestamp of the last action for streak calculation. */
    var lastActionDate: Long = 0,

    /**
     * Number of consecutive days the user stayed under their daily budget.
     * Used to unlock the SAVER_7 badge after 7 consecutive under-budget days.
     */
    var underBudgetDays: Int = 0,

    /**
     * Map of badge IDs to BadgeModel objects the user has earned.
     * Stored as a Firebase map: { "first_expense": { id, name, description, earned }, ... }
     */
    var badges: Map<String, BadgeModel> = emptyMap()
) : Serializable {
    // Required for Firebase Realtime Database
    constructor() : this("", "", 0, 1, 0, 0, 0, emptyMap())

    /** Returns true if the user already holds a given badge. */
    fun hasBadge(type: BadgeType): Boolean = badges[type.id]?.earned == true
}
