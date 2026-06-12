package com.example.equili

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.equili.data.model.CategoryTotal
import com.example.equili.ui.viewModel.ExpenseViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

/**
 * AnalyticsActivity displays spending analysis using a PieChart and a list of category totals.
 * Now synced with Dimetri's Firebase session management.
 */
class AnalyticsActivity : AppCompatActivity() {

    private val viewModel: ExpenseViewModel by viewModels()

    // Default start date set to the beginning of the current month
    private var startDate: Calendar = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }

    // Default end date set to the end of the current day
    private var endDate: Calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
    }

    private lateinit var adapter: AnalyticsAdapter
    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure user is truly logged into Firebase
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_analytics)

        // Initialize UI components
        val tvStart = findViewById<TextView>(R.id.tvStartAnalytic)
        val tvEnd = findViewById<TextView>(R.id.tvEndAnalytic)
        val rv = findViewById<RecyclerView>(R.id.rvAnalytics)
        pieChart = findViewById(R.id.pieChart)
        barChart = findViewById(R.id.barChart)

        setupPieChart()
        setupBarChart()

        // Set up RecyclerView for textual breakdown
        rv.layoutManager = LinearLayoutManager(this)
        adapter = AnalyticsAdapter()
        rv.adapter = adapter

        updateLabels(tvStart, tvEnd)
        observeAnalytics()

        // Date range selection listeners
        tvStart.setOnClickListener {
            showDatePicker {
                startDate = it.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
                updateLabels(tvStart, tvEnd)
                observeAnalytics()
            }
        }

        tvEnd.setOnClickListener {
            showDatePicker {
                endDate = it.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }
                updateLabels(tvStart, tvEnd)
                observeAnalytics()
            }
        }

        // Bottom Navigation setup
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_analytics
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
                R.id.nav_analytics -> true
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

    private fun setupPieChart() {
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.setExtraOffsets(5f, 10f, 5f, 5f)
        pieChart.dragDecelerationFrictionCoef = 0.95f
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.setTransparentCircleColor(Color.WHITE)
        pieChart.setTransparentCircleAlpha(110)
        pieChart.holeRadius = 58f
        pieChart.transparentCircleRadius = 61f
        pieChart.setDrawCenterText(true)
        pieChart.centerText = "Spending\nBreakdown"
        pieChart.setCenterTextColor(Color.CYAN)
        pieChart.setCenterTextSize(14f)
        pieChart.rotationAngle = 0f
        pieChart.isRotationEnabled = true
        pieChart.isHighlightPerTapEnabled = true
        pieChart.legend.isEnabled = false
        pieChart.setEntryLabelColor(Color.WHITE)
        pieChart.setEntryLabelTextSize(12f)
    }

    private fun updatePieChart(dataList: List<CategoryTotal>) {
        val entries = ArrayList<PieEntry>()
        dataList.forEach {
            if (it.total > 0) {
                entries.add(PieEntry(it.total.toFloat(), it.name))
            }
        }

        if (entries.isEmpty()) {
            pieChart.clear()
            return
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.setDrawIcons(false)
        dataSet.sliceSpace = 3f
        dataSet.selectionShift = 8f

        // Custom color palette for the chart
        val colors = ArrayList<Int>()
        val palette = arrayOf(
            "#00FFFF", // Equili Cyan
            "#FF9F43", // Warm Orange
            "#00D2D3", // Teal
            "#54A0FF", // Bright Blue
            "#5F27CD", // Purple
            "#FF6B6B", // Soft Red
            "#1DD1A1", // Jade Green
            "#FECA57"  // Mellow Yellow
        )
        for (color in palette) {
            colors.add(Color.parseColor(color))
        }
        dataSet.colors = colors

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(pieChart))
        data.setValueTextSize(13f)
        data.setValueTextColor(Color.WHITE)
        data.setValueTypeface(android.graphics.Typeface.DEFAULT_BOLD)

        pieChart.data = data
        pieChart.highlightValues(null)
        pieChart.invalidate() // Refresh chart
    }

    private fun setupBarChart() {
        barChart.description.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.setDrawBarShadow(false)
        barChart.isHighlightFullBarEnabled = false

        barChart.xAxis.apply {
            position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            textColor = Color.WHITE
            granularity = 1f
        }

        barChart.axisLeft.apply {
            textColor = Color.WHITE
            setDrawGridLines(true)
            gridColor = Color.parseColor("#33FFFFFF")
        }

        barChart.axisRight.isEnabled = false
        barChart.legend.textColor = Color.WHITE
    }

    private fun updateBarChart(dataList: List<CategoryTotal>) {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        dataList.forEachIndexed { index, item ->
            entries.add(BarEntry(index.toFloat(), item.total.toFloat()))
            labels.add(item.name)
        }

        if (entries.isEmpty()) {
            barChart.clear()
            return
        }

        barChart.xAxis.valueFormatter = IndexValueFormatter(labels)

        val dataSet = BarDataSet(entries, "Spent per Category")
        dataSet.color = Color.parseColor("#00FFFF") // Equili Cyan
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 10f

        val data = BarData(dataSet)
        barChart.data = data

        // Add Goal Limit Lines (Min and Max)
        val yAxis = barChart.axisLeft
        yAxis.removeAllLimitLines()

        viewModel.monthlyGoal.value?.let { goal ->
            if (goal.minGoal > 0) {
                val minLine = LimitLine(goal.minGoal.toFloat(), "Min Goal")
                minLine.lineColor = Color.GREEN
                minLine.lineWidth = 2f
                minLine.textColor = Color.GREEN
                minLine.textSize = 10f
                yAxis.addLimitLine(minLine)
            }
            if (goal.maxGoal > 0) {
                val maxLine = LimitLine(goal.maxGoal.toFloat(), "Max Goal")
                maxLine.lineColor = Color.RED
                maxLine.lineWidth = 2f
                maxLine.textColor = Color.RED
                maxLine.textSize = 10f
                yAxis.addLimitLine(maxLine)
            }
        }

        barChart.invalidate()
    }

    private fun observeAnalytics() {
        viewModel.getCategoryTotalsInRange(startDate.timeInMillis, endDate.timeInMillis).observe(this) {
            val list = it ?: emptyList()
            adapter.submitList(list)
            updatePieChart(list)
            updateBarChart(list)
        }

        // Also observe goals to update limit lines if goals change
        viewModel.monthlyGoal.observe(this) {
            val list = adapter.getCurrentList()
            updateBarChart(list)
        }
    }

    private fun updateLabels(tvS: TextView, tvE: TextView) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        tvS.text = sdf.format(startDate.time)
        tvE.text = sdf.format(endDate.time)
    }

    private fun showDatePicker(onDate: (Calendar) -> Unit) {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val sel = Calendar.getInstance()
            sel.set(y, m, d)
            onDate(sel)
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    inner class AnalyticsAdapter : RecyclerView.Adapter<AnalyticsAdapter.VH>() {
        private var items = listOf<CategoryTotal>()

        fun submitList(l: List<CategoryTotal>) {
            items = l.sortedByDescending { it.total }
            notifyDataSetChanged()
        }

        fun getCurrentList() = items

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
            LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        )

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.itemView.findViewById<TextView>(android.R.id.text1).apply {
                text = items[position].name
                setTextColor(0xFF00FFFF.toInt())
            }
            holder.itemView.findViewById<TextView>(android.R.id.text2).apply {
                text = String.format(Locale.getDefault(), "Total Spent: R%.2f", items[position].total)
                setTextColor(0xFFFFFFFF.toInt())
            }
        }

        override fun getItemCount() = items.size

        inner class VH(v: View) : RecyclerView.ViewHolder(v)
    }
}
