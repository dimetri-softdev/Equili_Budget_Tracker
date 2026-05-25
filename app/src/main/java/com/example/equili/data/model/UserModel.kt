package com.example.equili.data.model

import java.io.Serializable

/**
 * UserModel represents a registered user in the system.
 * Updated for Firebase Firestore compatibility.
 */
data class UserModel(
    /** Unique user ID from Firebase Auth. */
    var uid: String = "",

    /** User's email address. */
    val email: String = "",

    /** Accumulated Experience Points (XP). */
    val xp: Int = 0,

    /** Current user level based on XP. */
    val level: Int = 1,

    /** Number of consecutive days of engagement. */
    val streak: Int = 0,

    /** Timestamp of the last action for streak calculation. */
    val lastActionDate: Long = 0
) : Serializable {
    // Firestore requires a no-argument constructor
    constructor() : this("", "", 0, 1, 0, 0)
}
