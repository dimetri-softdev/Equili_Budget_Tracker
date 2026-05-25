package com.example.equili.data.model

import java.io.Serializable

/**
 * ExpenseModel represents an individual spending record.
 * Updated for Firebase Realtime Database compatibility.
 */
data class ExpenseModel(
    /** Unique ID for the expense. */
    var id: String = "",

    /** The UID of the user who owns this record. */
    var userId: String = "",

    /** A brief name or description for the expense. */
    var title: String = "",

    /** The monetary value of the expense. */
    var amount: Double = 0.0,

    /** The category assigned to this expense. */
    var category: String = "",

    /** The date of the expense stored as a timestamp (Long). */
    var date: Long = 0,

    /** The starting time of the expense activity. */
    var startTime: String = "",

    /** The ending time of the expense activity. */
    var endTime: String = "",

    /** Local or remote file path to the attached receipt image, if any. */
    var imagePath: String? = null
) : Serializable
