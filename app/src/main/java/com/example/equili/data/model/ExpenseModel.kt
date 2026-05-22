package com.example.equili.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * ExpenseModel represents an individual spending record in the database.
 * Implements Serializable to allow passing objects between activities via Intents.
 */
//@Entity(tableName = "expense_table")
data class ExpenseModel(

    /** Unique ID for the expense, automatically generated. */
    //@PrimaryKey(autoGenerate = true)
    val id: String = "",

    /** The owner of the expense record. */
    val userEmail: String = "",

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

    /** Local file path to the attached receipt image, if any. */
    val imagePath: String? = null
) : Serializable{
    // Firestore needs an empty constructor
    constructor() : this("","","",0.0,"",0,"","",null)

}
