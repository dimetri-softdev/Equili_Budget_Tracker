package com.example.equili

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
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
                // Auto-assign icon based on name keywords
                val lowerName = name.lowercase()
                val icon = when {
                    lowerName.contains("food") || lowerName.contains("eat") || lowerName.contains("drink") -> "ic_food"
                    lowerName.contains("transport") || lowerName.contains("car") || lowerName.contains("bus") || lowerName.contains("taxi") -> "ic_transport"
                    lowerName.contains("game") || lowerName.contains("playstation") || lowerName.contains("xbox") || lowerName.contains("nintendo") -> "ic_games"
                    lowerName.contains("sport") || lowerName.contains("gym") || lowerName.contains("football") || lowerName.contains("soccer") -> "ic_sports"
                    lowerName.contains("health") || lowerName.contains("med") || lowerName.contains("doctor") || lowerName.contains("hospital") -> "ic_health"
                    lowerName.contains("shop") || lowerName.contains("buy") || lowerName.contains("grocery") -> "ic_shopping"
                    lowerName.contains("utility") || lowerName.contains("bill") || lowerName.contains("electricity") || lowerName.contains("water") -> "ic_utilities"
                    lowerName.contains("entertainment") || lowerName.contains("movie") || lowerName.contains("music") || lowerName.contains("cinema") -> "ic_entertainment"
                    else -> "ic_categories"
                }

                // Save the new category
                viewModel.insertCategory(CategoryModel(name = name, icon = icon))
                etName.setText("") // Clear input field
                Toast.makeText(this, "Category '$name' Added with icon", Toast.LENGTH_SHORT).show()
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
                R.id.nav_rewards -> {
                    startActivity(Intent(this, RewardsActivity::class.java))
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
            val layout = LinearLayout(parent.context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(16, 16, 16, 16)
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            val iv = ImageView(parent.context).apply {
                id = View.generateViewId()
                layoutParams = LinearLayout.LayoutParams(60, 60).apply { marginEnd = 16 }
            }
            val tv = TextView(parent.context).apply {
                id = View.generateViewId()
                textSize = 18f
                setTextColor(0xFFFFFFFF.toInt())
            }
            layout.addView(iv)
            layout.addView(tv)
            return VH(layout)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            val container = holder.itemView as LinearLayout
            val iv = container.getChildAt(0) as ImageView
            val tv = container.getChildAt(1) as TextView

            tv.text = item.name

            // Resolve icon resource ID
            val resId = holder.itemView.context.resources.getIdentifier(
                item.icon, "drawable", holder.itemView.context.packageName
            )
            iv.setImageResource(if (resId != 0) resId else R.drawable.ic_categories)
            iv.setColorFilter(0xFF00FFFF.toInt()) // Equili Cyan
        }

        override fun getItemCount() = list.size

        /**
         * ViewHolder class for the adapter.
         */
        inner class VH(v: View) : RecyclerView.ViewHolder(v)
    }
}
