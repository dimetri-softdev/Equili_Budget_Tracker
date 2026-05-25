package com.example.equili

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.equili.data.model.CategoryModel
import com.example.equili.ui.viewModel.ExpenseViewModel
import com.google.firebase.auth.FirebaseAuth

/**
 * CategoryActivity allows users to manage their custom expense categories.
 * Users can view existing categories and add new ones.
 * Synced with Dimetri's Firebase session management.
 */
class CategoryActivity : AppCompatActivity() {

    private val viewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure user is truly logged into Firebase
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_category)

        // UI Component Initialization
        val etName = findViewById<EditText>(R.id.etCategoryName)
        val btnAdd = findViewById<Button>(R.id.btnAddCategory)
        val rv = findViewById<RecyclerView>(R.id.rvCategories)

        // Setup RecyclerView for categories
        rv.layoutManager = LinearLayoutManager(this)
        val adapter = CategoryAdapter()
        rv.adapter = adapter

        // Observe changes in categories from the database
        viewModel.allCategories.observe(this) {
            adapter.submitList(it)
        }

        // Logic to add a new category
        btnAdd.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isNotEmpty()) {
                // Save the new category
                viewModel.insertCategory(CategoryModel(name = name))
                etName.setText("") // Clear input field
                Toast.makeText(this, "Category Added", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
            }
        }

        // Setup Bottom Navigation logic
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_categories
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    false
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    false
                }
                R.id.nav_analytics -> {
                    startActivity(Intent(this, AnalyticsActivity::class.java))
                    false
                }
                R.id.nav_categories -> true
                else -> false
            }
        }
    }

    /**
     * Internal Adapter class for the Category RecyclerView.
     */
    inner class CategoryAdapter : RecyclerView.Adapter<CategoryAdapter.VH>() {
        private var list = listOf<CategoryModel>()

        /**
         * Updates the internal data list and refreshes the UI.
         */
        fun submitList(newList: List<CategoryModel>) {
            list = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            // Programmatically creating a TextView for each category item for simplicity
            val tv = TextView(parent.context).apply {
                textSize = 18f
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(16, 16, 16, 16)
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            return VH(tv)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            (holder.itemView as TextView).text = list[position].name
        }

        override fun getItemCount() = list.size

        /**
         * ViewHolder class for the adapter.
         */
        inner class VH(v: View) : RecyclerView.ViewHolder(v)
    }
}
