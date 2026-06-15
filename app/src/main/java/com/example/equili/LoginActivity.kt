package com.example.equili

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import com.example.equili.ui.viewModel.ExpenseViewModel
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.Executor

/**
 * LoginActivity handles user authentication.
 * Verified against Firebase Authentication to allow access to the cloud-synced dashboard.
 */
class LoginActivity : AppCompatActivity() {

    private val TAG = "LoginActivity"
    private val viewModel: ExpenseViewModel by viewModels()

    // Firebase Authentication instance
    private lateinit var auth: FirebaseAuth
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // UI component initialization
        val etUser = findViewById<EditText>(R.id.etUsername)
        val etPass = findViewById<EditText>(R.id.etPassword)
        val cbShow = findViewById<CheckBox>(R.id.cbShowPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnBiometric = findViewById<ImageButton>(R.id.btnBiometric)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        // Check if biometric is enabled and supported
        val prefs = getSharedPreferences("EquiliPrefs", MODE_PRIVATE)
        val isBiometricEnabled = prefs.getBoolean("BIOMETRIC_ENABLED", false)
        val lastEmail = prefs.getString("LAST_LOGGED_IN_EMAIL", "")

        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

        if (isBiometricEnabled && canAuthenticate) {
            btnBiometric.visibility = android.view.View.VISIBLE
            // Pre-fill email if we have it
            if (!lastEmail.isNullOrEmpty()) {
                etUser.setText(lastEmail)
            }

            // Auto-trigger biometric prompt for a smoother experience
            btnBiometric.postDelayed({
                biometricPrompt.authenticate(promptInfo)
            }, 500)
        } else {
            btnBiometric.visibility = android.view.View.GONE
        }

        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val email = prefs.getString("LAST_LOGGED_IN_EMAIL", "") ?: prefs.getString("CURRENT_USER", "")

                    if (!email.isNullOrEmpty()) {
                        Log.i(TAG, "Biometric authentication succeeded for $email")
                        etUser.setText(email)
                        Toast.makeText(applicationContext, "Biometric Verified. Please enter your password.", Toast.LENGTH_LONG).show()
                        etPass.requestFocus()

                        // Note: To make this a TRUE "one-tap" login, you would need to store
                        // the user's password in the Android Keystore during their first login.
                        // For now, we use biometrics as a secure identity verification and auto-fill.
                    } else {
                        Toast.makeText(applicationContext, "Please login with password once to link biometrics", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Login")
            .setSubtitle("Log in using your fingerprint")
            .setNegativeButtonText("Use account password")
            .build()

        btnBiometric.setOnClickListener {
            biometricPrompt.authenticate(promptInfo)
        }

        // Show/Hide password toggle
        cbShow.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                etPass.inputType = android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                etPass.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            // Move cursor to end
            etPass.setSelection(etPass.text.length)
        }

        // Navigation: Back to landing screen
        btnBack.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

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
                            .putString("LAST_LOGGED_IN_EMAIL", email)
                            .apply()

                        // Navigate to the dashboard
                        val intent = Intent(this@LoginActivity, DashboardActivity::class.java)
                        startActivity(intent)
                        finish() // Prevent user from returning to login screen via 'Back'
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

        // Forgot Password logic
        tvForgotPassword.setOnClickListener {
            val email = etUser.text.toString().trim()

            if (email.isNotEmpty()) {
                // Feature 1: Forgot Password Flow using Firebase Auth
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
            } else {
                Toast.makeText(this, "Please enter your email in the Username field first", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
