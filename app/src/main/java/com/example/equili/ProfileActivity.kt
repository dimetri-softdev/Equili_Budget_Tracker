package com.example.equili

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

/**
 * ProfileActivity displays the user's profile information.
 * Feature 2: Display user email and account creation date from Firebase Auth.
 */
class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val tvUserEmail = findViewById<TextView>(R.id.tvUserEmail)
        val tvCreationDate = findViewById<TextView>(R.id.tvCreationDate)
        val btnBack = findViewById<Button>(R.id.btnBack)

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

        btnBack.setOnClickListener {
            finish()
        }
    }
}
