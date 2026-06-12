package com.example.equili

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.equili.data.model.ExpenseModel
import com.example.equili.ui.adapter.ExpenseAdapter
import com.example.equili.ui.viewModel.ExpenseViewModel
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * HistoryActivity displays a list of past expenses within a chosen date range.
 * Users can search, sort, and export records.
 */
class HistoryActivity : AppCompatActivity() {

    private val viewModel: ExpenseViewModel by viewModels()
    private lateinit var adapter: ExpenseAdapter

    // Default filter: Start of the current month
    private var startDate: Calendar = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }

    // Default filter: End of the current day
    private var endDate: Calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure user is truly logged into Firebase
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_history)

        // UI Initialization
        val tvStart = findViewById<TextView>(R.id.tvStartDate)
        val tvEnd = findViewById<TextView>(R.id.tvEndDate)
        val etSearch = findViewById<EditText>(R.id.etSearch)
        val spinnerSort = findViewById<Spinner>(R.id.spinnerSort)
        val btnExport = findViewById<ImageButton>(R.id.btnExport)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        // Initialize ExpenseAdapter
        adapter = ExpenseAdapter(
            onItemClick = { expense ->
                val intent = Intent(this, ExpenseActivity::class.java)
                intent.putExtra("EXPENSE", expense)
                startActivity(intent)
            },
            onDeleteClick = { expense ->
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Delete Expense")
                    .setMessage("Are you sure you want to delete this expense?")
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteExpense(expense)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        updateDateLabels(tvStart, tvEnd)

        // Fetch initial data based on default range
        viewModel.setDateRange(startDate.timeInMillis, endDate.timeInMillis)

        // Observe and display filtered expenses
        viewModel.filteredExpenses.observe(this) { expenses ->
            adapter.setExpenses(expenses)
        }

        // Search logic: Filter by title
        etSearch.addTextChangedListener { text ->
            viewModel.setSearchQuery(text.toString())
        }

        // Sorting logic: Initialize Spinner and listener
        val sortOptions = arrayOf("Newest", "Oldest", "Highest Amount", "Lowest Amount", "Category (A-Z)")
        val sortAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sortOptions)
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSort.adapter = sortAdapter

        spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val option = when (position) {
                    0 -> ExpenseViewModel.SortOption.DATE_DESC
                    1 -> ExpenseViewModel.SortOption.DATE_ASC
                    2 -> ExpenseViewModel.SortOption.AMOUNT_DESC
                    3 -> ExpenseViewModel.SortOption.AMOUNT_ASC
                    4 -> ExpenseViewModel.SortOption.CATEGORY_ASC
                    else -> ExpenseViewModel.SortOption.DATE_DESC
                }
                viewModel.setSortOption(option)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Export Logic
        btnExport.setOnClickListener {
            val expenses = viewModel.filteredExpenses.value
            if (!expenses.isNullOrEmpty()) {
                exportExpensesToCsv(expenses)
            } else {
                Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show()
            }
        }

        // Filter: Select start date
        tvStart.setOnClickListener {
            showDatePicker { date ->
                startDate = date.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
                updateDateLabels(tvStart, tvEnd)
                viewModel.setDateRange(startDate.timeInMillis, endDate.timeInMillis)
            }
        }

        // Filter: Select end date
        tvEnd.setOnClickListener {
            showDatePicker { date ->
                endDate = date.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }
                updateDateLabels(tvStart, tvEnd)
                viewModel.setDateRange(startDate.timeInMillis, endDate.timeInMillis)
            }
        }

        // Bottom Navigation Logic
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_history
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    false
                }
                R.id.nav_history -> true
                R.id.nav_analytics -> {
                    startActivity(Intent(this, AnalyticsActivity::class.java))
                    false
                }
                R.id.nav_rewards -> {
                    startActivity(Intent(this, RewardsActivity::class.java))
                    false
                }
                R.id.nav_categories -> {
                    startActivity(Intent(this, CategoryActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }

    private fun updateDateLabels(tvStart: TextView, tvEnd: TextView) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        tvStart.text = "From: ${sdf.format(startDate.time)}"
        tvEnd.text = "To: ${sdf.format(endDate.time)}"
    }

    private fun showDatePicker(onDateSelected: (Calendar) -> Unit) {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val selected = Calendar.getInstance()
            selected.set(y, m, d)
            onDateSelected(selected)
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun exportExpensesToCsv(expenses: List<ExpenseModel>) {
        val csvHeader = "Title,Amount,Category,Date\n"
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val csvData = StringBuilder(csvHeader)

        for (expense in expenses) {
            csvData.append("${expense.title},${expense.amount},${expense.category},${sdf.format(Date(expense.date))}\n")
        }

        try {
            val filename = "Equili_Expenses_${System.currentTimeMillis()}.csv"
            val file = File(cacheDir, filename)
            FileOutputStream(file).use { it.write(csvData.toString().toByteArray()) }

            val contentUri: Uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share Report"))
        } catch (e: Exception) {
            Toast.makeText(this, "Error exporting data", Toast.LENGTH_SHORT).show()
        }
    }
}
