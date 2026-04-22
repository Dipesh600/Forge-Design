package com.forge.vdesign.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.forge.vdesign.R
import com.forge.vdesign.domain.model.ChatMessage
import com.forge.vdesign.domain.model.MessageRole
import com.forge.vdesign.ui.auth.AuthActivity
import com.forge.vdesign.ui.canvas.ScreenCanvasActivity
import com.forge.vdesign.ui.chat.ChatAdapter
import com.forge.vdesign.ui.chat.ChatViewModel
import com.forge.vdesign.ui.conversations.ConversationAdapter
import com.forge.vdesign.ui.conversations.ConversationListViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * MainActivity — FORGE Unified Interface
 *
 * Implements a ChatGPT-style layout.
 * - The primary root view is a Chat interface managed by [ChatViewModel].
 * - The sliding side drawer is the conversation history managed by [ConversationListViewModel].
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val chatViewModel: ChatViewModel by viewModels()
    private val conversationListViewModel: ConversationListViewModel by viewModels()

    private lateinit var chatAdapter: ChatAdapter
    private lateinit var historyAdapter: ConversationAdapter
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatLayoutManager: LinearLayoutManager

    private val STREAMING_ID = "forge_streaming_live"

    private fun scrollToBottom() {
        val lastPosition = chatAdapter.itemCount - 1
        if (lastPosition < 0) return
        chatRecyclerView.scrollToPosition(lastPosition)
        chatRecyclerView.post {
            val lastView = chatLayoutManager.findViewByPosition(lastPosition) ?: return@post
            val overscroll = lastView.bottom - (chatRecyclerView.height - chatRecyclerView.paddingBottom)
            if (overscroll > 0) chatRecyclerView.scrollBy(0, overscroll)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Edge-to-Edge with Keyboard support
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawerLayout)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            // Only apply padding to the chatRoot child so the drawer spans full height
            findViewById<View>(R.id.chatRoot).setPadding(bars.left, bars.top, bars.right, bars.bottom)
            findViewById<View>(R.id.navDrawer).setPadding(0, bars.top, 0, bars.bottom)
            insets
        }

        // Layout Views
        val drawerLayout: DrawerLayout = findViewById(R.id.drawerLayout)
        val btnMenu: ImageButton = findViewById(R.id.btnMenu)
        val tvConversationTitle: TextView = findViewById(R.id.tvConversationTitle)
        val tvUserDisplayName: TextView = findViewById(R.id.tvUserDisplayName)
        val rvConversations: RecyclerView = findViewById(R.id.rvConversations)
        val inputField: EditText = findViewById(R.id.messageInput)
        val sendBtn: ImageButton = findViewById(R.id.sendButton)
        val btnSignOutDrawer: View = findViewById(R.id.btnSignOutDrawer)
        val btnNewChatTop: ImageButton = findViewById(R.id.btnNewChatTop)
        chatRecyclerView = findViewById(R.id.chatRecyclerView)

        // Setup Adapters
        setupHistoryAdapter(rvConversations, drawerLayout)
        setupChatAdapter()

        // Button Listeners
        btnMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
        
        btnNewChatTop.setOnClickListener {
            chatViewModel.switchConversation(null)
            drawerLayout.closeDrawers()
        }

        sendBtn.setOnClickListener {
            val text = inputField.text.toString().trim()
            if (text.isNotBlank()) {
                chatViewModel.sendMessage(text)
                inputField.text.clear()
            }
        }

        btnSignOutDrawer.setOnClickListener {
            conversationListViewModel.signOut()
        }

        // Observer loops
        observeConversationList(tvUserDisplayName)
        observeActiveChat(tvConversationTitle, sendBtn, inputField)
    }

    private fun setupHistoryAdapter(rv: RecyclerView, drawerLayout: DrawerLayout) {
        historyAdapter = ConversationAdapter(
            onOpen = { convo ->
                chatViewModel.switchConversation(convo.id)
                drawerLayout.closeDrawer(GravityCompat.START)
            },
            onDelete = { convo ->
                android.app.AlertDialog.Builder(this)
                    .setTitle("Delete conversation?")
                    .setMessage("\"${convo.title}\" will be permanently deleted.")
                    .setPositiveButton("Delete") { _, _ ->
                        conversationListViewModel.deleteConversation(convo.id)
                        if (chatViewModel.uiState.value.conversationId == convo.id) {
                            chatViewModel.switchConversation(null)
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = historyAdapter
    }

    private fun setupChatAdapter() {
        chatAdapter = ChatAdapter(
            onOpenCanvas = { brief, prompt ->
                val intent = Intent(this, ScreenCanvasActivity::class.java).apply {
                    putExtra(ScreenCanvasActivity.EXTRA_DESIGN_BRIEF, brief)
                    putExtra(ScreenCanvasActivity.EXTRA_PROMPT, prompt)
                }
                startActivity(intent)
            },
            onCopyMessage = { text ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("FORGE Response", text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            onRetryMessage = { aiMsg ->
                val allMsgs = chatViewModel.uiState.value.persistedMessages
                val idx = allMsgs.indexOfFirst { it.id == aiMsg.id }
                if (idx > 0) {
                    val fallbackMsg = allMsgs.subList(0, idx).lastOrNull { it.role == MessageRole.USER }
                    if (fallbackMsg != null) {
                        chatViewModel.sendMessage(fallbackMsg.content)
                        return@ChatAdapter
                    }
                }
                Toast.makeText(this, "No previous prompt to retry.", Toast.LENGTH_SHORT).show()
            }
        )
        chatLayoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        chatRecyclerView.layoutManager = chatLayoutManager
        chatRecyclerView.adapter = chatAdapter
        chatRecyclerView.itemAnimator = null // prevent flicker

        chatAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = scrollToBottom()
            override fun onChanged() = scrollToBottom()
        })
    }

    private fun observeConversationList(tvUser: TextView) {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                conversationListViewModel.state.collect { state ->
                    if (state.isSignedOut) { goToAuth(); return@collect }
                    tvUser.text = "Sign out (${state.userDisplayName})"
                    historyAdapter.submitList(state.conversations)
                }
            }
        }
    }

    private fun observeActiveChat(tvTitle: TextView, sendBtn: ImageButton, input: EditText) {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                chatViewModel.uiState.collect { state ->
                    if (state.isSignedOut) { goToAuth(); return@collect }
                    
                    tvTitle.text = state.conversationTitle

                    val liveSuffix = mutableListOf<ChatMessage>()

                    if (state.thinkingContent != null) {
                        liveSuffix += ChatMessage(
                            id = "forge_thinking",
                            conversationId = state.conversationId,
                            role = MessageRole.ASSISTANT,
                            content = state.thinkingContent
                        )
                    }

                    if (state.statusLine != null) {
                        liveSuffix += ChatMessage(
                            id = "forge_status",
                            conversationId = state.conversationId,
                            role = MessageRole.ASSISTANT,
                            content = "⟳  ${state.statusLine}",
                            isLoading = true
                        )
                    }

                    val streamingId: String?
                    if ((state.isStreaming || state.isSending) && state.streamingContent.isNotEmpty()) {
                        liveSuffix += ChatMessage(
                            id = STREAMING_ID,
                            conversationId = state.conversationId,
                            role = MessageRole.ASSISTANT,
                            content = state.streamingContent,
                            isLoading = false
                        )
                        streamingId = STREAMING_ID
                    } else {
                        streamingId = null
                    }

                    // Only enable input if not busy loading/streaming
                    val busy = state.isSending || state.isStreaming
                    sendBtn.isEnabled = !busy
                    sendBtn.alpha = if (busy) 0.4f else 1.0f
                    
                    chatAdapter.submitList(state.persistedMessages + liveSuffix, streamingId)

                    if (state.error != null) {
                        Toast.makeText(this@MainActivity, state.error, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun goToAuth() {
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }
}