package com.example.equili.data.model

import java.io.Serializable

/**
 * GoalModel represents the user's monthly spending limits.
 * Updated for Firebase Realtime Database compatibility.
 */
data class GoalModel(
    /** The UID of the user who owns this goal. */
    var userId: String = "",

    /** The minimum spending threshold the user aims for. */
    var minGoal: Double = 0.0,

    /** The maximum spending limit for the month. */
    var maxGoal: Double = 1000.0
) : Serializable
