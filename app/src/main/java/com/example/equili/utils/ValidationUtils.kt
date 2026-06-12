package com.example.equili.utils

/**
 * Utility object for data validation across the app.
 */
object ValidationUtils {

    /**
     * Validates password complexity.
     * Rules: 6+ chars, starts with uppercase, contains a digit, contains a symbol.
     */
    fun isValidPassword(password: String): Boolean {
        if (password.length < 6) return false
        if (password.isEmpty()) return false
        if (!password[0].isUpperCase()) return false

        val hasNumber = password.any { it.isDigit() }
        val hasSymbol = password.any { !it.isLetterOrDigit() }

        return hasNumber && hasSymbol
    }

    /**
     * Basic email format validation using regex for unit test compatibility.
     */
    fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()
        return email.matches(emailRegex)
    }

    /**
     * Validates username.
     * Rules: 3-20 characters, alphanumeric or common symbols.
     */
    fun isValidUsername(username: String): Boolean {
        if (username.length < 3 || username.length > 20) return false
        val usernameRegex = "^[a-zA-Z0-9._ ]+\$".toRegex()
        return username.matches(usernameRegex)
    }
}
