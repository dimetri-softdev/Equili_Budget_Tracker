package com.example.equili

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.biometric.BiometricManager
import com.example.equili.ui.viewModel.ExpenseViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

/**
 * ProfileActivity displays the user's profile information.
 */
class ProfileActivity : AppCompatActivity() {

    private val viewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val tvUsername = findViewById<TextView>(R.id.tvUsername)
        val tvUserEmail = findViewById<TextView>(R.id.tvUserEmail)
        val tvGender = findViewById<TextView>(R.id.tvGender)
        val tvDOB = findViewById<TextView>(R.id.tvDOB)
        val tvAddress = findViewById<TextView>(R.id.tvAddress)
        val tvEmployment = findViewById<TextView>(R.id.tvEmploymentStatus)
        val tvCreationDate = findViewById<TextView>(R.id.tvCreationDate)
        val switchBiometric = findViewById<SwitchCompat>(R.id.switchBiometric)
        val btnBack = findViewById<Button>(R.id.btnBack)

        val prefs = getSharedPreferences("EquiliPrefs", MODE_PRIVATE)
        val isBiometricEnabled = prefs.getBoolean("BIOMETRIC_ENABLED", false)
        switchBiometric.isChecked = isBiometricEnabled

        // Observe the user profile from ViewModel (Realtime Database/Firestore)
        viewModel.currentUser.observe(this) { userModel ->
            if (userModel != null) {
                tvUsername.text = userModel.username
                tvGender.text = userModel.gender
                tvDOB.text = userModel.dob
                tvAddress.text = userModel.address
                tvEmployment.text = if (userModel.isEmployed) "Employed" else "Unemployed"
            }
        }

        // Fetch current user from Firebase Auth for email and metadata
        val firebaseUser = FirebaseAuth.getInstance().currentUser

        if (firebaseUser != null) {
            tvUserEmail.text = firebaseUser.email

            val creationTimestamp = firebaseUser.metadata?.creationTimestamp
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
                val biometricManager = BiometricManager.from(this)
                when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
                    BiometricManager.BIOMETRIC_SUCCESS -> {
                        prefs.edit().putBoolean("BIOMETRIC_ENABLED", true).apply()
                        firebaseUser?.email?.let {
                            prefs.edit().putString("LAST_LOGGED_IN_EMAIL", it).apply()
                        }
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
