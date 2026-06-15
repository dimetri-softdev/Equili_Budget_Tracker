package com.example.equili

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.biometric.BiometricManager
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

/**
 * ProfileActivity displays the user's profile information.
 * Feature 2: Display user email and account creation date from Firebase Auth.
 * Feature 7: Biometric Security (Fingerprint/Face ID) toggle.
 */
class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val tvUserEmail = findViewById<TextView>(R.id.tvUserEmail)
        val tvCreationDate = findViewById<TextView>(R.id.tvCreationDate)
        val switchBiometric = findViewById<SwitchCompat>(R.id.switchBiometric)
        val btnBack = findViewById<Button>(R.id.btnBack)

        val prefs = getSharedPreferences("EquiliPrefs", MODE_PRIVATE)
        val isBiometricEnabled = prefs.getBoolean("BIOMETRIC_ENABLED", false)
        switchBiometric.isChecked = isBiometricEnabled

        // Fetch current user from Firebase Auth
        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            // Display user's email address
            tvUserEmail.text = user.email

            // Fetch and format account "Creation Date"
            val creationTimestamp = user.metadata?.creationTimestamp
            if (creationTimestamp != null && creationTimestamp > 0) {
                val date = Date(creationTimestamp)
                val format = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                tvCreationDate.text = format.format(date)
            } else {
                tvCreationDate.text = "Not Available"
            }
        } else {
            tvUserEmail.text = "Guest User"
            tvCreationDate.text = "N/A"
        }

        switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Check if device supports biometrics before enabling
                val biometricManager = BiometricManager.from(this)
                when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
                    BiometricManager.BIOMETRIC_SUCCESS -> {
                        prefs.edit().putBoolean("BIOMETRIC_ENABLED", true).apply()
                        Toast.makeText(this, "Fingerprint Login Enabled", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        switchBiometric.isChecked = false
                        Toast.makeText(this, "Biometric authentication is not available on this device", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                prefs.edit().putBoolean("BIOMETRIC_ENABLED", false).apply()
                Toast.makeText(this, "Fingerprint Login Disabled", Toast.LENGTH_SHORT).show()
            }
        }

        btnBack.setOnClickListener {
            finish()
        }
    }
}
