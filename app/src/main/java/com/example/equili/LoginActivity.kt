package com.example.equili

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.equili.ui.viewModel.ExpenseViewModel
import com.google.firebase.auth.FirebaseAuth

/**
 * LoginActivity handles user authentication.
 * Verified against Firebase Authentication to allow access to the cloud-synced dashboard.
 */
class LoginActivity : AppCompatActivity() {

    private val TAG = "LoginActivity"
    private val viewModel: ExpenseViewModel by viewModels()

    // Firebase Authentication instance
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // UI component initialization
        val etUser = findViewById<EditText>(R.id.etUsername)
        val etPass = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        // Login button click logic
        btnLogin.setOnClickListener {
            val email = etUser.text.toString().trim()
            val pass = etPass.text.toString()

            // Validate input fields are not empty
            if (email.isNotEmpty() && pass.isNotEmpty()) {

                Log.d(TAG, "Attempting Firebase login for user: $email")

                // Firebase Login Logic Start
                auth.signInWithEmailAndPassword(email, pass)
                    .addOnSuccessListener { result ->
                        // Successful login!
                        val user = result.user
                        Log.i(TAG, "Successfully authenticated user: ${user?.email} (UID: ${user?.uid})")

                        // Persist session locally using SharedPreferences
                        // This allows other parts of the app to know who is logged in
                        getSharedPreferences("EquiliPrefs", MODE_PRIVATE).edit()
                            .putString("CURRENT_USER", email)
                            .apply()

                        // Navigate to the dashboard
                        val intent = Intent(this@LoginActivity, DashboardActivity::class.java)
                        startActivity(intent)
                        finish() // Prevent returning to login via back button
                    }
                    .addOnFailureListener { e ->
                        // Authentication failed (wrong password, user doesn't exist, etc.)
                        Log.w(TAG, "Login failed for $email: ${e.message}")
                        Toast.makeText(this@LoginActivity, "Invalid email or password", Toast.LENGTH_SHORT).show()
                    }
                // --- Firebase Login Logic End ---

            } else {
                Toast.makeText(this, "Please enter login details", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
