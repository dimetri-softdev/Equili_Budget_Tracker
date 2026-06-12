package com.example.equili

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.equili.data.model.BadgeModel
import com.example.equili.data.model.BadgeType
import com.example.equili.ui.viewModel.ExpenseViewModel
import com.google.firebase.auth.FirebaseAuth

class RewardsActivity : AppCompatActivity() {

    private val viewModel: ExpenseViewModel by viewModels()
    private lateinit var adapter: BadgesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_rewards)

        val tvLevel = findViewById<TextView>(R.id.tvRewardsLevel)
        val tvStreak = findViewById<TextView>(R.id.tvRewardsStreak)
        val pbXp = findViewById<ProgressBar>(R.id.pbRewardsXp)
        val rv = findViewById<RecyclerView>(R.id.rvBadges)

        rv.layoutManager = LinearLayoutManager(this)
        adapter = BadgesAdapter()
        rv.adapter = adapter

        viewModel.currentUser.observe(this) { user ->
            if (user != null) {
                tvLevel.text = "Level ${user.level}"
                tvStreak.text = "Daily Streak: ${user.streak} 🔥"

                val xpNeeded = user.level * 100
                pbXp.max = xpNeeded
                pbXp.progress = user.xp

                // Update badges list
                val earnedBadges = user.badges.values.toList()
                adapter.submitList(earnedBadges)
            }
        }

        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_rewards
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { startActivity(Intent(this, DashboardActivity::class.java)); false }
                R.id.nav_history -> { startActivity(Intent(this, HistoryActivity::class.java)); false }
                R.id.nav_analytics -> { startActivity(Intent(this, AnalyticsActivity::class.java)); false }
                R.id.nav_rewards -> true
                R.id.nav_categories -> { startActivity(Intent(this, CategoryActivity::class.java)); false }
                else -> false
            }
        }
    }

    inner class BadgesAdapter : RecyclerView.Adapter<BadgesAdapter.VH>() {
        private var list = listOf<BadgeModel>()

        fun submitList(l: List<BadgeModel>) {
            list = l
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
            LayoutInflater.from(parent.context).inflate(R.layout.item_badge, parent, false)
        )

        override fun onBindViewHolder(holder: VH, position: Int) {
            val badge = list[position]
            holder.name.text = badge.name
            holder.desc.text = badge.description

            val iconRes = when (badge.id) {
                BadgeType.FIRST_EXPENSE.id -> R.drawable.ic_badge_first_expense
                BadgeType.STREAK_7.id      -> R.drawable.ic_badge_streak
                BadgeType.UNDER_BUDGET.id  -> R.drawable.ic_badge_under_budget
                BadgeType.LEVEL_5.id       -> R.drawable.ic_badge_level_up
                BadgeType.SAVER_7.id       -> R.drawable.ic_badge_saver
                else                       -> R.drawable.ic_badge_first_expense
            }
            holder.icon.setImageResource(iconRes)
        }

        override fun getItemCount() = list.size

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val icon: ImageView = v.findViewById(R.id.ivBadgeIcon)
            val name: TextView = v.findViewById(R.id.tvBadgeName)
            val desc: TextView = v.findViewById(R.id.tvBadgeDesc)
        }
    }
}
