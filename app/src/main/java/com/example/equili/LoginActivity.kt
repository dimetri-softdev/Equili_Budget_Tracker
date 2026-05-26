package com.example.equili

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.equili.ui.viewModel.ExpenseViewModel
// import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * LoginActivity handles user authentication.
 * It verifies credentials against the local Room database and starts a session.
 */
class LoginActivity : AppCompatActivity() {

    private val TAG = "LoginActivity"
    // private lateinit var auth: FirebaseAuth
    private val viewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        // auth = FirebaseAuth.getInstance()

        // UI component initialization
        val etUser = findViewById<EditText>(R.id.etUsername)
        val etPass = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        // Login button click logic
        btnLogin.setOnClickListener {
            val email = etUser.text.toString().trim()
            val pass = etPass.text.toString()

            // Validate that input fields are not empty
            if (email.isNotEmpty() && pass.isNotEmpty()) {
                // Launch coroutine to perform database lookup off the main thread
                lifecycleScope.launch {
                    val user = viewModel.getUserByEmail(email)

                    // Simple password verification
                    if (user != null && user.password == pass) {
                        Log.d(TAG, "Login successful for user: $email")
                        // Persist session locally using SharedPreferences
                        getSharedPreferences("EquiliPrefs", MODE_PRIVATE).edit()
                            .putString("CURRENT_USER", email)
                            .apply()

                        // Navigate to the dashboard on successful login
                        val intent = Intent(this@LoginActivity, DashboardActivity::class.java)
                        startActivity(intent)
                        finish() // Prevent user from returning to login screen via 'Back'
                    } else {
                        Log.w(TAG, "Login failed for user: $email")
                        // Feedback for failed authentication
                        Toast.makeText(this@LoginActivity, "Invalid email or password", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Please enter login details", Toast.LENGTH_SHORT).show()
            }
        }

        // Forgot Password logic
        tvForgotPassword.setOnClickListener {
            val email = etUser.text.toString().trim()

            if (email.isNotEmpty()) {
                // Feature 1: Forgot Password Flow using Firebase Auth (Disabled for now)
                /*
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "Password reset email sent to $email")
                            Toast.makeText(this, "Reset link sent to your email", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.e(TAG, "Error sending reset email", task.exception)
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                */
                Toast.makeText(this, "Forgot password feature is currently disabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter your email in the Username field first", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
