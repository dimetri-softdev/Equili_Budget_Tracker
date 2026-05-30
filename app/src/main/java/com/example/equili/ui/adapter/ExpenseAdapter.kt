package com.example.equili.ui.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.equili.R
import com.example.equili.data.model.ExpenseModel
import com.example.equili.databinding.ExpenseItemBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * ExpenseAdapter binds a list of ExpenseModel data to the RecyclerView in HistoryActivity.
 * It uses ViewBinding for efficient UI updates and handles item clicks/long-clicks.
 */
class ExpenseAdapter(
    private val onItemClick: (ExpenseModel) -> Unit,
    private val onDeleteClick: (ExpenseModel) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    private var expenses = listOf<ExpenseModel>()

    /**
     * ViewHolder holds reference to the generated ViewBinding for each list item.
     */
    inner class ExpenseViewHolder(val binding: ExpenseItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ExpenseItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // Binding data to views
        holder.binding.apply {
            tvTitle.text = expense.title
            tvCategory.text = "${expense.category} | ${expense.startTime} - ${expense.endTime}"
            tvAmount.text = String.format(Locale.getDefault(), "R%.2f", expense.amount)
            tvDate.text = sdf.format(Date(expense.date))

            // Resolve and set Category Icon
            val resId = holder.itemView.context.resources.getIdentifier(
                expense.categoryIcon, "drawable", holder.itemView.context.packageName
            )
            ivCategoryIcon.setImageResource(if (resId != 0) resId else R.drawable.ic_categories)

            // Display a thumbnail if a receipt image is attached
            if (expense.imagePath != null) {
                ivSmallPhoto.visibility = View.VISIBLE
                ivSmallPhoto.setImageURI(Uri.fromFile(File(expense.imagePath)))
            } else {
                ivSmallPhoto.visibility = View.GONE
            }
        }

        // Handle simple click for editing
        holder.itemView.setOnClickListener {
            onItemClick(expense)
        }

        // Handle long click for deletion
        holder.itemView.setOnLongClickListener {
            onDeleteClick(expense)
            true
        }
    }

    override fun getItemCount() = expenses.size

    /**
     * Updates the adapter's data set and refreshes the list display.
     */
    fun setExpenses(list: List<ExpenseModel>) {
        expenses = list
        notifyDataSetChanged()
    }
}
