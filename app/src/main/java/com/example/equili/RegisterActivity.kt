package com.example.equili

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.equili.data.model.UserModel
import com.example.equili.ui.viewModel.ExpenseViewModel
import com.example.equili.utils.ValidationUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * RegisterActivity handles the creation of new user accounts.
 * Now updated to use Firebase Authentication for cloud-based identity management.
 */
class RegisterActivity : AppCompatActivity() {

    private val TAG = "RegisterActivity"
    private val viewModel: ExpenseViewModel by viewModels()

    // Initialize Firebase Auth
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // --- Firebase Integration Start ---
        auth = FirebaseAuth.getInstance()
        // --- Firebase Integration End ---

        // UI component initialization
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        // Register button logic
        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            // Step 1: Basic validation - check if fields are empty
            if (email.isEmpty() || password.isEmpty()) {
                Log.w(TAG, "Registration failed: Empty fields")
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Step 2: Security check - validate password complexity
            if (!ValidationUtils.isValidPassword(password)) {
                Log.w(TAG, "Registration failed: Password complexity not met for user $email")
                Toast.makeText(this, "Password must start with a capital, be 6+ characters, and contain a number and symbol", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // --- Firebase Registration Logic Start ---
            Log.d(TAG, "Attempting to create Firebase user for: $email")

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    // User created successfully in Firebase Auth!
                    val uid = result.user?.uid ?: ""
                    Log.i(TAG, "Successfully registered user with Firebase UID: $uid")

                    // Create user profile in Firestore
                    lifecycleScope.launch {
                        viewModel.registerUser(UserModel(uid = uid, email = email))

                        // Save the user email to SharedPreferences to maintain the session
                        getSharedPreferences("EquiliPrefs", MODE_PRIVATE).edit()
                            .putString("CURRENT_USER", email)
                            .apply()

                        Toast.makeText(this@RegisterActivity, "Registration Successful", Toast.LENGTH_SHORT).show()

                        // Proceed directly to the Dashboard
                        val intent = Intent(this@RegisterActivity, DashboardActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
                .addOnFailureListener { e ->
                    // Handle common errors (e.g., email already in use, network issues)
                    Log.e(TAG, "Firebase registration failed: ${e.message}")
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            // --- Firebase Registration Logic End ---
        }
    }
}
