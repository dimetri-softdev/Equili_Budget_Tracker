package com.example.equili.data.model

import java.io.Serializable

/**
 * UserModel represents a registered user in the system.
 * Updated for Firebase Realtime Database compatibility.
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
    var lastActionDate: Long = 0
) : Serializable {
    // Required for Firebase Realtime Database
    constructor() : this("", "", 0, 1, 0, 0)
}
