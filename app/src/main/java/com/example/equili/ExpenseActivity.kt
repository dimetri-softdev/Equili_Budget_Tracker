package com.example.equili

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.equili.data.model.ExpenseModel
import com.example.equili.ui.viewModel.ExpenseViewModel
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * ExpenseActivity handles the creation and editing of expense records.
 * It allows users to input details like amount, title, category, date, time,
 * and attach a receipt image from the camera or gallery.
 */
class ExpenseActivity : AppCompatActivity() {

    private val viewModel: ExpenseViewModel by viewModels()
    private var selectedDate: Calendar = Calendar.getInstance()
    private var startTime: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    private var endTime: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    private var imagePath: String? = null // Stores the path to the attached receipt image
    private var currentPhotoPath: String? = null // Temporary path for newly captured camera image

    private var existingExpense: ExpenseModel? = null // Holds the expense being edited, if any

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_IMAGE_PICK = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure user is truly logged into Firebase before showing data
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_expense)

        // Initialize UI components
        val etAmount = findViewById<EditText>(R.id.etAmount)
        val etTitle = findViewById<EditText>(R.id.etTitle)
        val tvDate = findViewById<TextView>(R.id.tvDate)
        val tvStart = findViewById<TextView>(R.id.tvStartTime)
        val tvEnd = findViewById<TextView>(R.id.tvEndTime)
        val spinner = findViewById<Spinner>(R.id.spinnerCategory)
        val ivPreview = findViewById<ImageView>(R.id.ivPreview)
        val btnCapture = findViewById<Button>(R.id.btnCapture)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val tvHeader = findViewById<TextView>(R.id.tvHeader)

        // Set default values for date and time displays
        tvDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate.time)
        tvStart.text = startTime
        tvEnd.text = endTime

        // Determine if we are in "Edit" mode by checking for passed ExpenseModel
        existingExpense = intent.getSerializableExtra("EXPENSE") as? ExpenseModel
        existingExpense?.let {
            tvHeader.text = "Edit Expense"
            etAmount.setText(it.amount.toString())
            etTitle.setText(it.title)
            selectedDate.timeInMillis = it.date
            startTime = it.startTime
            endTime = it.endTime
            imagePath = it.imagePath

            tvDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate.time)
            tvStart.text = startTime
            tvEnd.text = endTime

            // Load and display existing receipt image if available
            if (imagePath != null) {
                ivPreview.visibility = View.VISIBLE
                try {
                    ivPreview.setImageURI(Uri.fromFile(File(imagePath!!)))
                } catch (e: Exception) {
                    ivPreview.visibility = View.GONE
                }
            }

            btnSave.text = "Update Expense"
        }

        // Observe categories from ViewModel to populate the category Spinner
        viewModel.allCategories.observe(this) { categories ->
            val names = categories.map { it.name }.toMutableList()
            if (names.isEmpty()) names.add("General") // Fallback if no categories exist

            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
            spinner.adapter = adapter

            // If editing, select the current category in the spinner
            existingExpense?.let {
                val pos = names.indexOf(it.category)
                if (pos >= 0) spinner.setSelection(pos)
            }
        }

        // Click listener for date selection
        tvDate.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                selectedDate.set(y, m, d)
                tvDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate.time)
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Click listener for start time selection
        tvStart.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                startTime = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                tvStart.text = startTime
            }, 12, 0, true).show()
        }

        // Click listener for end time selection
        tvEnd.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                endTime = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                tvEnd.text = endTime
            }, 13, 0, true).show()
        }

        // Open dialog to choose image source (Camera or Gallery)
        btnCapture.setOnClickListener { showImageSourceDialog() }

        // Click listener for saving or updating the expense
        btnSave.setOnClickListener {
            val amountStr = etAmount.text.toString()
            val title = etTitle.text.toString()
            val category = spinner.selectedItem?.toString() ?: "General"

            if (amountStr.isNotEmpty() && title.isNotEmpty()) {
                try {
                    val amount = amountStr.toDouble()
                    // Create an ExpenseModel object with current inputs
                    val expense = ExpenseModel(
                        id = existingExpense?.id ?: "", // Empty for new record, existing ID for update
                        userId = "", // Handled by repository
                        title = title,
                        amount = amount,
                        category = category,
                        date = selectedDate.timeInMillis,
                        startTime = startTime,
                        endTime = endTime,
                        imagePath = imagePath
                    )
                    // Persist the expense via ViewModel
                    viewModel.insertExpense(expense)
                    Toast.makeText(this, if (existingExpense == null) "Expense Saved" else "Expense Updated", Toast.LENGTH_SHORT).show()
                    finish() // Return to the previous screen
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill Title and Amount", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Shows a dialog to select between taking a new photo or choosing an existing one from the gallery.
     */
    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        AlertDialog.Builder(this)
            .setTitle("Add Receipt")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> dispatchTakePictureIntent()
                    1 -> dispatchPickGalleryIntent()
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    /**
     * Launches the system camera app to capture a photo.
     */
    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            try {
                // Create a temporary file where the photo should go
                val photoFile: File? = createImageFile()
                photoFile?.also {
                    // Get a content URI for the file using FileProvider for secure sharing
                    val photoURI: Uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", it)
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            } catch (ex: Exception) {
                Toast.makeText(this, "Camera error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Launches the system gallery/media picker.
     */
    private fun dispatchPickGalleryIntent() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    /**
     * Creates a unique temporary file in the app's external pictures directory.
     */
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath // Keep track of the file path
        }
    }

    /**
     * Handles the results from the camera or gallery intent.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            val ivPreview = findViewById<ImageView>(R.id.ivPreview)
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    // Image was saved to currentPhotoPath by the camera
                    imagePath = currentPhotoPath
                    ivPreview.visibility = View.VISIBLE
                    ivPreview.setImageURI(Uri.fromFile(File(imagePath!!)))
                }
                REQUEST_IMAGE_PICK -> {
                    // Image was selected from gallery, copy it to app's internal storage
                    val selectedImage: Uri? = data?.data
                    selectedImage?.let { uri ->
                        imagePath = copyUriToInternalStorage(uri)
                        ivPreview.visibility = View.VISIBLE
                        ivPreview.setImageURI(Uri.fromFile(File(imagePath!!)))
                    }
                }
            }
        }
    }

    /**
     * Copies a file from a content URI (e.g., from Gallery) to the app's private storage.
     * This ensures the app maintains access to the file even if the original is moved or deleted.
     */
    private fun copyUriToInternalStorage(uri: Uri): String? {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val file = createImageFile()
        val outputStream = FileOutputStream(file)
        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        return file.absolutePath
    }
}
