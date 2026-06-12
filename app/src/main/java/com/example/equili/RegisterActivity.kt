package com.example.equili

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
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
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val cbShowPassword = findViewById<CheckBox>(R.id.cbShowPassword)
        val spinnerGender = findViewById<Spinner>(R.id.spinnerGender)
        val etDOB = findViewById<EditText>(R.id.etDOB)
        val etAddress = findViewById<EditText>(R.id.etAddress)
        val switchEmployed = findViewById<Switch>(R.id.switchEmployed)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        // Setup Gender Spinner
        val genders = arrayOf("Select Gender", "Male", "Female", "Other", "Prefer not to say")
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genders)
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGender.adapter = genderAdapter

        // Setup DOB DatePicker
        etDOB.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            android.app.DatePickerDialog(this, { _, year, month, day ->
                etDOB.setText(String.format("%02d/%02d/%d", day, month + 1, year))
            }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show()
        }

        // Password Show/Hide toggle
        cbShowPassword.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                etPassword.inputType = android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                etPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            etPassword.setSelection(etPassword.text.length)
        }

        // Navigation: Back to landing screen
        btnBack.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // Register button logic
        btnRegister.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val gender = if (spinnerGender.selectedItemPosition > 0) spinnerGender.selectedItem.toString() else ""
            val dob = etDOB.text.toString()
            val address = etAddress.text.toString().trim()
            val isEmployed = switchEmployed.isChecked

            // Step 1: Username validation
            if (!ValidationUtils.isValidUsername(username)) {
                etUsername.error = "Please enter a valid username (3-20 characters, letters/numbers/spaces)"
                return@setOnClickListener
            }

            // Step 2: Email validation
            if (!ValidationUtils.isValidEmail(email)) {
                etEmail.error = "Please enter a valid email address"
                return@setOnClickListener
            }

            // Step 3: Password validation
            if (!ValidationUtils.isValidPassword(password)) {
                etPassword.error = "Password must start with a capital, be 6+ chars, have 1 number and 1 symbol"
                return@setOnClickListener
            }

            // Step 4: Personal data validation
            if (gender.isEmpty()) {
                Toast.makeText(this, "Please select your gender", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (dob.isEmpty()) {
                etDOB.error = "Please select your date of birth"
                return@setOnClickListener
            }
            if (address.isEmpty()) {
                etAddress.error = "Please enter your physical address"
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
                        val newUser = UserModel(
                            uid = uid,
                            email = email,
                            username = username,
                            gender = gender,
                            address = address,
                            dob = dob,
                            isEmployed = isEmployed
                        )
                        viewModel.registerUser(newUser)

                        // Save the user email to SharedPreferences to maintain the session
                        getSharedPreferences("EquiliPrefs", MODE_PRIVATE).edit()
                            .putString("CURRENT_USER", email)
                            .apply()

                        Toast.makeText(this@RegisterActivity, "Welcome, $username! Registration Successful", Toast.LENGTH_SHORT).show()

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
