package com.example.equili.data.model

import java.io.Serializable

/**
 * CategoryModel represents a spending category.
 * Updated for Firebase Realtime Database compatibility with icons.
 */
data class CategoryModel(
    /** Unique ID for the category. */
    var id: String = "",

    /** The UID of the user who owns this category. */
    var userId: String = "",

    /** The display name of the category (e.g., "Food", "Transport"). */
    var name: String = "",

    /** The icon resource name or identifier. */
    var icon: String = "ic_categories"
) : Serializable {
    // Required for Firebase
    constructor() : this("", "", "", "ic_categories")
}
