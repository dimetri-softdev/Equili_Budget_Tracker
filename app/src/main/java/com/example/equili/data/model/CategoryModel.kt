package com.example.equili.data.model

import java.io.Serializable

/**
 * CategoryModel represents a spending category.
 * Updated for Firebase Firestore compatibility.
 */
data class CategoryModel(
    /** Unique ID for the category. */
    var id: String = "",

    /** The UID of the user who owns this category. */
    var userId: String = "",

    /** The display name of the category (e.g., "Food", "Transport"). */
    val name: String = ""
) : Serializable {
    // Firestore requires a no-argument constructor
    constructor() : this("", "", "")
}
