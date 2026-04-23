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
import androidx.appcompat.widget.PopupMenu
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
import com.forge.vdesign.ui.workspace.WorkspaceBottomSheet
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
        val btnWorkspace: ImageButton = findViewById(R.id.btnWorkspace)
        val btnChatMenu: ImageButton = findViewById(R.id.btnChatMenu)
        val chatEmptyState: View = findViewById(R.id.chatEmptyState)
        chatRecyclerView = findViewById(R.id.chatRecyclerView)

        // Setup Adapters
        setupHistoryAdapter(rvConversations, drawerLayout)
        setupChatAdapter()

        // Prompt chips — tap to pre-fill input and send
        val chipPrompts = mapOf(
            R.id.chip1 to "Design a modern SaaS analytics dashboard with charts and KPI cards",
            R.id.chip2 to "Design a premium e-commerce product detail page with a clean, minimal layout",
            R.id.chip3 to "Design a fitness tracker home screen with workout stats and progress rings"
        )
        chipPrompts.forEach { (id, prompt) ->
            findViewById<TextView>(id).setOnClickListener {
                inputField.setText(prompt)
                inputField.setSelection(prompt.length)
                inputField.requestFocus()
            }
        }

        // Button Listeners
        btnMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
        
        btnWorkspace.setOnClickListener {
            val state = chatViewModel.uiState.value
            if (state.conversationId.isEmpty()) {
                Toast.makeText(this, "Start a chat first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val screens = state.persistedMessages.filter { it.isScreenCard }
            WorkspaceBottomSheet().apply {
                // Must show first before calling bind(), or hack a bundle. Actually better to just do it in onViewCreated.
                // But passing data directly here requires the view to exist. Let's pass via companion object or setter before show?
                // Calling show() attaches it, but view might not be created synchronously. We can pass via bundle in a real app,
                // but let's just use a setter that caches the data if view is null! Wait, let's fix the bottom sheet bind to handle it.
            }.apply {
                this.pendingScreens = screens
                this.pendingProjectTitle = state.conversationTitle
            }.show(supportFragmentManager, WorkspaceBottomSheet.TAG)
        }

        btnChatMenu.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.menu_chat_context, popup.menu)
            
            // Re-find the active conversation to know its starred state if we want to toggle the title, but wait, DB takes care of that.
            val activeConvoId = chatViewModel.uiState.value.conversationId
            val title = chatViewModel.uiState.value.conversationTitle
            
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_new_chat -> {
                        chatViewModel.switchConversation(null)
                        drawerLayout.closeDrawers()
                        true
                    }
                    R.id.action_rename -> {
                        if (activeConvoId.isNotEmpty()) showRenameDialog(activeConvoId, title)
                        true
                    }
                    R.id.action_star -> {
                        if (activeConvoId.isNotEmpty()) {
                            // We don't have the exact current starred state easily without searching the history list, 
                            // so we'll just search it from the conversationListViewModel state!
                            val isCurrentlyStarred = conversationListViewModel.state.value.conversations.find { it.id == activeConvoId }?.isStarred ?: false
                            conversationListViewModel.starConversation(activeConvoId, !isCurrentlyStarred)
                            Toast.makeText(this, if (isCurrentlyStarred) "Un-starred" else "Starred", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                    R.id.action_delete -> {
                        if (activeConvoId.isNotEmpty()) {
                            android.app.AlertDialog.Builder(this)
                                .setTitle("Delete Chat?")
                                .setMessage("This will wipe all messages and generated screens.")
                                .setPositiveButton("Delete") { _, _ ->
                                    conversationListViewModel.deleteConversation(activeConvoId)
                                    chatViewModel.switchConversation(null)
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                        }
                        true
                    }
                    else -> false
                }
            }
            popup.show()
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
        observeActiveChat(tvConversationTitle, sendBtn, inputField, chatEmptyState)
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

    private fun showRenameDialog(conversationId: String, currentTitle: String) {
        val input = EditText(this).apply {
            setText(currentTitle)
            setSelection(currentTitle.length)
            setSingleLine()
        }
        
        android.app.AlertDialog.Builder(this)
            .setTitle("Rename Chat")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newTitle = input.text.toString().trim()
                if (newTitle.isNotBlank()) {
                    conversationListViewModel.renameConversation(conversationId, newTitle)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeActiveChat(tvTitle: TextView, sendBtn: ImageButton, input: EditText, emptyState: View) {
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

                    val allMessages = state.persistedMessages + liveSuffix
                    val isEmpty = allMessages.isEmpty() && !state.isSending && !state.isStreaming

                    // Toggle empty state welcome screen vs live chat
                    emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
                    chatRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE

                    // Only enable input if not busy loading/streaming
                    val busy = state.isSending || state.isStreaming
                    sendBtn.isEnabled = !busy
                    sendBtn.alpha = if (busy) 0.4f else 1.0f
                    
                    chatAdapter.submitList(allMessages, streamingId)

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