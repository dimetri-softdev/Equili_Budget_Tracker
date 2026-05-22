package com.example.equili.data.model

import java.io.Serializable

/**
 * ExpenseModel represents an individual spending record.
 * Updated for Firebase Firestore compatibility.
 */
data class ExpenseModel(
    /** Unique ID for the expense. */
    var id: String = "",

    /** The UID of the user who owns this record. */
    var userId: String = "",

    /** A brief name or description for the expense. */
    val title: String = "",

    /** The monetary value of the expense. */
    val amount: Double = 0.0,

    /** The category assigned to this expense. */
    val category: String = "",

    /** The date of the expense stored as a timestamp (Long). */
    val date: Long = 0,

    /** The starting time of the expense activity. */
    val startTime: String = "",

    /** The ending time of the expense activity. */
    val endTime: String = "",

    /** Local or remote file path to the attached receipt image, if any. */
    val imagePath: String? = null
) : Serializable {
    // Firestore requires a no-argument constructor
    constructor() : this("", "", "", 0.0, "", 0, "", "", null)
}
