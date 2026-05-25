package com.example.equili

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

/**
 * MainActivity serves as the entry point/landing screen of the application.
 * It checks for existing Firebase sessions and provides navigation to Login or Registration.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- SESSION CHECK START ---
        // Use Firebase Auth as the source of truth for logged-in users
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // User is already logged in, skip this screen and go straight to Dashboard
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }
        // --- SESSION CHECK END ---

        setContentView(R.layout.activity_main)

        // Initialize UI components
        val loginBtn = findViewById<Button>(R.id.btnLogin)
        val registerBtn = findViewById<Button>(R.id.btnRegister)

        // Navigate to the login screen
        loginBtn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // Navigate to the registration screen
        registerBtn.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
