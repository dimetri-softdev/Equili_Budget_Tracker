package com.example.equili

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.equili.ui.viewModel.ExpenseViewModel
import java.util.*

/**
 * AiChatActivity handles the conversational interface between the user and the Equili AI Assistant.
 * It uses a rule-based heuristic engine to analyze real-time budget data and provide financial advice.
 */
class AiChatActivity : AppCompatActivity() {

    // ViewModel for accessing live spending and goal data
    private val viewModel: ExpenseViewModel by viewModels()
    // Adapter for managing chat bubbles in the RecyclerView
    private lateinit var adapter: ChatAdapter
    // Local list to track messages during the current session
    private val messages = mutableListOf<ChatMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the UI layout for this activity
        setContentView(R.layout.activity_ai_chat)

        // Initialize UI components by ID
        val rvChat = findViewById<RecyclerView>(R.id.rvChat)
        val etMessage = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val tvTyping = findViewById<TextView>(R.id.tvTyping)

        // Configure RecyclerView with a linear vertical layout
        adapter = ChatAdapter(messages)
        rvChat.layoutManager = LinearLayoutManager(this)
        rvChat.adapter = adapter

        // Observe ViewModel data to ensure it stays fresh and loaded from Firebase
        viewModel.expensesInDateRange.observe(this) { /* Triggers load */ }
        viewModel.monthlyGoal.observe(this) { /* Triggers load */ }
        viewModel.currentUser.observe(this) { /* Triggers load */ }

        // Send an initial greeting from the AI when the chat opens
        addMessage("Hello! I'm your Equili AI Advisor. Ask me anything about your spending or for tips on saving money!", false)

        // Click listener for the Send button
        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                // 1. Add user's message to the list
                addMessage(text, true)
                // 2. Clear the input field for next message
                etMessage.setText("")
                // 3. Scroll to the bottom of the chat
                rvChat.scrollToPosition(messages.size - 1)

                // 4. Show "thinking" indicator to simulate AI processing
                tvTyping.visibility = View.VISIBLE
                rvChat.postDelayed({
                    // 5. Hide typing indicator after delay
                    tvTyping.visibility = View.GONE
                    // 6. Generate and display the AI's intelligent response
                    val response = generateAiResponse(text)
                    addMessage(response, false)
                    // 7. Scroll to show the new AI message
                    rvChat.scrollToPosition(messages.size - 1)
                }, 1500) // 1.5 second delay for realism
            }
        }

        // Back button returns the user to the Dashboard
        btnBack.setOnClickListener { finish() }
    }

    /**
     * Appends a new message to the local list and notifies the adapter to refresh the UI.
     */
    private fun addMessage(text: String, isUser: Boolean) {
        messages.add(ChatMessage(text, isUser))
        adapter.notifyItemInserted(messages.size - 1)
    }

    /**
     * The AI Logical Engine.
     * Uses natural language processing (keyword-based) and real-time data analysis.
     * Evaluates user's spending against their defined goals to provide expert advice.
     */
    private fun generateAiResponse(userInput: String): String {
        val input = userInput.lowercase()

        // --- REAL-TIME DATA ACQUISITION ---
        val user = viewModel.currentUser.value
        val expenses = viewModel.expensesInDateRange.value ?: emptyList()
        val spent = expenses.sumOf { it.amount }
        val maxGoal = viewModel.monthlyGoal.value?.maxGoal ?: 0.0
        val remaining = maxGoal - spent

        // Calculate category-specific heavy spending
        val topCategory = expenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .maxByOrNull { it.value }

        // --- INTELLIGENT LOGIC ROUTING ---
        return when {
            // 1. GREETINGS & PERSONALITY
            input.contains("hello") || input.contains("hi") || input.contains("hey") -> {
                val namePart = if (user != null && user.username.isNotEmpty()) ", ${user.username}" else ""
                "Greetings$namePart! I'm your Equili Intelligence unit. I've analyzed your data and am ready to optimize your budget. What shall we look at first?"
            }

            // 2. SPENDING ANALYSIS
            (input.contains("how much") || input.contains("total")) && (input.contains("spent") || input.contains("expense")) ->
                "You have logged R${String.format("%.2f", spent)} in expenses this month. ${if (maxGoal > 0) "That's ${String.format("%.1f", (spent/maxGoal)*100)}% of your total budget." else "I recommend setting a budget goal to put this number into perspective."}"

            // 3. REMAINING BALANCE & GOALS
            input.contains("left") || input.contains("remaining") || input.contains("balance") || input.contains("can i spend") -> {
                if (maxGoal <= 0) "I can't calculate your balance because you haven't set a budget goal yet. Head to the 'Goals' section!"
                else if (remaining < 0) "Alert: You are currently R${String.format("%.2f", -remaining)} over your limit. We need to freeze non-essential spending immediately."
                else "You have R${String.format("%.2f", remaining)} left to spend until the end of the month. Use it wisely!"
            }

            // 4. CATEGORY INSIGHTS
            input.contains("category") || input.contains("most") || input.contains("where") || input.contains("breakdown") -> {
                if (topCategory != null) {
                    val percent = if (spent > 0) (topCategory.value / spent * 100).toInt() else 0
                    "My analysis shows you're spending most of your money on '${topCategory.key}' (R${String.format("%.2f", topCategory.value)}), which is $percent% of your total spending. Is this an area where you could cut back?"
                } else "I don't see any category data yet. Once you log some expenses, I can tell you where your money is going."
            }

            // 5. GAMIFICATION & STATUS
            input.contains("level") || input.contains("xp") || input.contains("streak") || input.contains("progress") -> {
                if (user != null) {
                    val xpNeeded = user.level * 100
                    "You are currently Level ${user.level} with ${user.xp} XP (you need ${xpNeeded - user.xp} more to level up). Your current streak is ${user.streak} days! Keep logging daily to earn more XP."
                } else "I'm still loading your profile data. Ask me again in a moment!"
            }

            // 6. BADGES
            input.contains("badge") || input.contains("achievement") -> {
                val badgeCount = user?.badges?.size ?: 0
                if (badgeCount > 0) "You have earned $badgeCount badges so far! Keep it up to become a financial master."
                else "You haven't earned any badges yet. Try logging your first expense or staying under budget for a week!"
            }

            // 7. FINANCIAL COACHING (Dynamic)
            input.contains("save") || input.contains("tip") || input.contains("advice") || input.contains("help") -> {
                when {
                    spent > (maxGoal * 0.9) && maxGoal > 0 -> "EMERGENCY: You've used 90% of your budget. Switch to 'Needs Only' mode. Cancel any subscriptions you don't use."
                    spent > (maxGoal * 0.7) && maxGoal > 0 -> "Coach Tip: You're at 70% of your budget. Try 'No-Spend Days' for the rest of the week."
                    topCategory?.key == "Food" || topCategory?.key == "Dining" -> "Coach Tip: Your food spending is high. Meal prepping on Sundays could save you up to R500 a week!"
                    topCategory?.key == "Entertainment" || topCategory?.key == "Shopping" -> "Coach Tip: That's a lot of fun spending! Try the 24-hour rule: wait a day before any non-essential purchase."
                    (user?.streak ?: 0) < 3 -> "Coach Tip: Try to log in daily. Users with a 7-day streak save 20% more on average because they stay aware of their balance."
                    else -> "Coach Tip: You're doing well! Consider moving any remaining balance at the end of the month into a high-interest savings account."
                }
            }

            // 8. HEALTH CHECK
            input.contains("status") || input.contains("how am i doing") || (input.contains("doing") && input.contains("how")) -> {
                val healthScore = if (maxGoal > 0) ((remaining / maxGoal) * 100).toInt() else 100
                when {
                    healthScore < 0 -> "Status: Critical. You've exceeded your targets by R${String.format("%.2f", -remaining)}. Let's try to minimize the damage for the rest of the month."
                    healthScore < 15 -> "Status: Warning. You're very close to your budget limit. Only buy essentials."
                    healthScore > 50 -> "Status: Healthy. You're managing your finances excellently! You have plenty of breathing room."
                    else -> "Status: Stable. You're on track to hit your goals. Stay disciplined!"
                }
            }

            // 9. WHO AM I
            input.contains("who are you") || input.contains("what can you do") || input.contains("about") ->
                "I am your Equili Financial Assistant. I analyze your spending patterns, track your budget goals, and monitor your gamification progress (Level, XP, Streaks). Ask me about your 'status', 'top category', or 'saving tips'!"

            // 10. SPECIFIC CATEGORY CHECK
            expenses.any { it.category.lowercase().contains(input) } -> {
                val cat = expenses.first { it.category.lowercase().contains(input) }.category
                val total = expenses.filter { it.category == cat }.sumOf { it.amount }
                "You've spent R${String.format("%.2f", total)} on $cat so far this month."
            }

            // 11. FALLBACK
            else -> "I understand you're interested in your finances. While I'm still learning, I can help with spending totals, budget balances, category analysis, and gamification status. Try asking 'How is my budget status?'"
        }
    }

    /**
     * Internal data class to hold individual message details.
     */
    data class ChatMessage(val text: String, val isUser: Boolean)

    /**
     * Custom RecyclerView Adapter for displaying chat history with alternating alignments.
     */
    inner class ChatAdapter(private val list: List<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.VH>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
            LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
        )

        override fun onBindViewHolder(holder: VH, position: Int) {
            val msg = list[position]
            // Toggle visibility of left/right containers based on message sender
            if (msg.isUser) {
                holder.llUser.visibility = View.VISIBLE
                holder.llAi.visibility = View.GONE
                holder.tvUser.text = msg.text
            } else {
                holder.llUser.visibility = View.GONE
                holder.llAi.visibility = View.VISIBLE
                holder.tvAi.text = msg.text
            }
        }

        override fun getItemCount() = list.size

        /**
         * ViewHolder class for individual chat bubble rows.
         */
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val llAi: View = v.findViewById(R.id.llAiMessage)
            val llUser: View = v.findViewById(R.id.llUserMessage)
            val tvAi: TextView = v.findViewById(R.id.tvAiText)
            val tvUser: TextView = v.findViewById(R.id.tvUserText)
        }
    }
}
