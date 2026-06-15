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

class AiChatActivity : AppCompatActivity() {

    private val viewModel: ExpenseViewModel by viewModels()
    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_chat)

        val rvChat = findViewById<RecyclerView>(R.id.rvChat)
        val etMessage = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val tvTyping = findViewById<TextView>(R.id.tvTyping)

        adapter = ChatAdapter(messages)
        rvChat.layoutManager = LinearLayoutManager(this)
        rvChat.adapter = adapter

        // Welcome message
        addMessage("Hello! I'm your Equili AI Advisor. Ask me anything about your spending or for tips on saving money!", false)

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                addMessage(text, true)
                etMessage.setText("")
                rvChat.scrollToPosition(messages.size - 1)

                // Simulate AI response
                tvTyping.visibility = View.VISIBLE
                rvChat.postDelayed({
                    tvTyping.visibility = View.GONE
                    val response = generateAiResponse(text)
                    addMessage(response, false)
                    rvChat.scrollToPosition(messages.size - 1)
                }, 1500)
            }
        }

        btnBack.setOnClickListener { finish() }
    }

    private fun addMessage(text: String, isUser: Boolean) {
        messages.add(ChatMessage(text, isUser))
        adapter.notifyItemInserted(messages.size - 1)
    }

    private fun generateAiResponse(userInput: String): String {
        val input = userInput.lowercase()
        val spent = viewModel.expensesInDateRange.value?.sumOf { it.amount } ?: 0.0
        val maxGoal = viewModel.monthlyGoal.value?.maxGoal ?: 0.0

        return when {
            input.contains("hello") || input.contains("hi") -> "Hi there! I'm ready to help you manage your budget. What's on your mind?"
            input.contains("how much") && input.contains("spent") -> "You have spent a total of R${String.format("%.2f", spent)} so far this month."
            input.contains("budget") || input.contains("goal") -> {
                if (maxGoal > 0) "Your monthly budget is R${String.format("%.2f", maxGoal)}. You have R${String.format("%.2f", maxGoal - spent)} remaining."
                else "You haven't set a budget yet! You should set one in the 'Goals' section."
            }
            input.contains("save") || input.contains("tip") -> {
                val tips = listOf(
                    "Try to track every small purchase, even coffee. It adds up!",
                    "Check your 'Entertainment' category. That's usually where most people can save the most.",
                    "Since you've spent R${String.format("%.2f", spent)}, try to keep your daily spending under R${String.format("%.2f", (maxGoal - spent) / 15)} for the rest of the month.",
                    "Did you know? Users who check their analytics daily spend 15% less on average!"
                )
                tips.random()
            }
            input.contains("who are you") -> "I am the Equili AI Assistant, designed to help you reach your financial goals through data analysis and smart tracking."
            input.contains("status") || input.contains("how am i doing") -> {
                when {
                    spent > maxGoal && maxGoal > 0 -> "To be honest, you are currently over budget. We should look at your history and see where to cut back."
                    spent > (maxGoal * 0.8) -> "You are approaching your limit (80% used). Be careful with big purchases this week!"
                    else -> "You are doing great! You are well within your limits. Keep up the discipline!"
                }
            }
            else -> "That's a great question! While I'm still learning, I recommend checking your Analytics tab for a visual breakdown of that data."
        }
    }

    data class ChatMessage(val text: String, val isUser: Boolean)

    inner class ChatAdapter(private val list: List<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
            LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
        )

        override fun onBindViewHolder(holder: VH, position: Int) {
            val msg = list[position]
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

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val llAi: View = v.findViewById(R.id.llAiMessage)
            val llUser: View = v.findViewById(R.id.llUserMessage)
            val tvAi: TextView = v.findViewById(R.id.tvAiText)
            val tvUser: TextView = v.findViewById(R.id.tvUserText)
        }
    }
}
