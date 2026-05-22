package com.example.equili.data.model

import java.io.Serializable

/**
 * GoalModel represents the user's monthly spending limits.
 * Updated for Firebase Firestore compatibility.
 */
data class GoalModel(
    /** The UID of the user who owns this goal. */
    var userId: String = "",

    /** The minimum spending threshold the user aims for. */
    val minGoal: Double = 0.0,

    /** The maximum spending limit for the month. */
    val maxGoal: Double = 1000.0
) : Serializable {
    // Firestore requires a no-argument constructor
    constructor() : this("", 0.0, 1000.0)
}
