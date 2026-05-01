package com.forge.vdesign.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.transition.TransitionManager
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.EditText
import android.widget.ImageButton
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import com.forge.vdesign.ui.chat.GlassDialog
import com.forge.vdesign.ui.chat.GlassMenu
import com.forge.vdesign.ui.chat.GlassToast
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
        val btnWorkspace: ImageButton = findViewById(R.id.btnWorkspace)
        val btnChatMenu: ImageButton = findViewById(R.id.btnChatMenu)
        val chatEmptyState: View = findViewById(R.id.chatEmptyState)
        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        
        val toolApprovalContainer: View = findViewById(R.id.toolApprovalContainer)
        val tvToolApprovalText: TextView = findViewById(R.id.tvToolApprovalText)
        val btnRejectTool: android.widget.Button = findViewById(R.id.btnRejectTool)
        val btnAllowTool: android.widget.Button = findViewById(R.id.btnAllowTool)
        val btnAllowAllTool: android.widget.Button = findViewById(R.id.btnAllowAllTool)
        val btnStopGeneration: View = findViewById(R.id.btnStopGeneration)

        btnAllowTool.setOnClickListener { chatViewModel.submitToolApproval(true) }
        btnAllowAllTool.setOnClickListener { chatViewModel.submitToolApprovalAll() }
        btnRejectTool.setOnClickListener { chatViewModel.submitToolApproval(false) }
        btnStopGeneration.setOnClickListener { chatViewModel.stopGeneration() }

        // Setup Adapters
        setupHistoryAdapter(rvConversations, drawerLayout)
        setupChatAdapter()

        // Starter Cards — tap to pre-fill input and send
        val cardPrompts = mapOf(
            R.id.card1 to "Design an E-commerce Kit: Multi-vendor marketplace with cart and checkout",
            R.id.card2 to "Design a Portfolio Studio: Minimalist dark-themed designer showcase",
            R.id.card3 to "Design a Foodie App: High-conversion delivery service UI"
        )
        cardPrompts.forEach { (id, prompt) ->
            findViewById<View>(id).setOnClickListener {
                inputField.setText(prompt)
                inputField.setSelection(prompt.length)
                inputField.requestFocus()
            }
        }

        // Button Listeners
        btnMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
        
        findViewById<View>(R.id.btnNewChatDrawer).setOnClickListener {
            chatViewModel.switchConversation(null)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        
        btnWorkspace.setOnClickListener {
            val state = chatViewModel.uiState.value
            if (state.conversationId.isEmpty()) {
                GlassToast.show(this, "Start a chat first.")
                return@setOnClickListener
            }
            val screens = state.persistedMessages.filter { it.isScreenCard && !it.isRejected }
            WorkspaceBottomSheet().apply {
                this.pendingScreens = screens
                this.pendingProjectTitle = state.conversationTitle
                this.onEditScreenClicked = { screen ->
                    val screenName = screen.content.ifBlank { "Screen" }
                    val prompt = "Edit the \"$screenName\" screen: "
                    inputField.setText(prompt)
                    inputField.setSelection(prompt.length)
                    inputField.requestFocus()
                    
                    // Show keyboard
                    val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.showSoftInput(inputField, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                }
            }.show(supportFragmentManager, WorkspaceBottomSheet.TAG)
        }

        btnChatMenu.setOnClickListener { view ->
            val activeConvoId = chatViewModel.uiState.value.conversationId
            val title = chatViewModel.uiState.value.conversationTitle
            val isStarred = conversationListViewModel.state.value.conversations.find { it.id == activeConvoId }?.isStarred ?: false

            GlassMenu(this)
                .addItem("New Chat", R.drawable.ic_add_chat) {
                    chatViewModel.switchConversation(null)
                    drawerLayout.closeDrawers()
                }
                .addItem("Rename", R.drawable.ic_edit) {
                    if (activeConvoId.isNotEmpty()) showRenameDialog(activeConvoId, title)
                }
                .addItem("Delete", R.drawable.ic_delete) {
                    if (activeConvoId.isNotEmpty()) {
                        GlassDialog.build()
                            .setContent("Delete Chat?", "This will wipe all messages and generated screens.")
                            .setButtons("Delete", "Cancel") {
                                conversationListViewModel.deleteConversation(activeConvoId)
                                chatViewModel.switchConversation(null)
                                GlassToast.show(this, "Project deleted")
                            }
                            .show(supportFragmentManager)
                    }
                }
                .show(view)
        }

        sendBtn.setOnClickListener {
            val text = inputField.text.toString().trim()
            if (text.isNotBlank()) {
                chatViewModel.sendMessage(text)
                inputField.text.clear()
            }
        }

        // Profile Entry (Bottom of Drawer)
        findViewById<View>(R.id.layoutProfileEntry).setOnClickListener {
            com.forge.vdesign.ui.profile.ProfileActivity.launch(this)
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // Observer loops
        observeConversationList(tvUserDisplayName)
        observeActiveChat(tvConversationTitle, sendBtn, inputField, chatEmptyState, toolApprovalContainer, tvToolApprovalText, btnStopGeneration)
    }

    private fun setupHistoryAdapter(rv: RecyclerView, drawerLayout: DrawerLayout) {
        historyAdapter = ConversationAdapter(
            onOpen = { convo ->
                chatViewModel.switchConversation(convo.id)
                drawerLayout.closeDrawer(GravityCompat.START)
            },
            onDelete = { convo ->
                GlassDialog.build()
                    .setContent("Delete Project?", "\"${convo.title}\" will be permanently deleted.")
                    .setButtons("Delete", "Cancel") {
                        conversationListViewModel.deleteConversation(convo.id)
                        if (chatViewModel.uiState.value.conversationId == convo.id) {
                            chatViewModel.switchConversation(null)
                        }
                        GlassToast.show(this, "Project deleted")
                    }
                    .show(supportFragmentManager)
            }
        )
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = historyAdapter
    }

    private fun setupChatAdapter() {
        chatAdapter = ChatAdapter(
            onOpenCanvas = { view, brief, prompt -> 
                val intent = Intent(this, ScreenCanvasActivity::class.java).apply {
                    putExtra(ScreenCanvasActivity.EXTRA_DESIGN_BRIEF, brief)
                    putExtra(ScreenCanvasActivity.EXTRA_PROMPT, prompt)
                }
                val options = androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this, view, "canvas_handoff"
                )
                startActivity(intent, options.toBundle())
            },
            onCopyMessage = { text ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("FORGE Response", text)
                clipboard.setPrimaryClip(clip)
                GlassToast.show(this, "Copied to clipboard")
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
                GlassToast.show(this, "No previous prompt to retry.")
            },
            onRejectScreen = { messageId ->
                chatViewModel.rejectScreenCard(messageId)
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
                // Observe the list itself
                launch {
                    conversationListViewModel.state.collect { state ->
                        if (state.isSignedOut) { goToAuth(); return@collect }
                        tvUser.text = state.userDisplayName.ifBlank { "Designer" }
                        historyAdapter.submitList(state.conversations, chatViewModel.uiState.value.conversationId)
                    }
                }
                // Observe the active ID changes from the chat state
                launch {
                    chatViewModel.uiState.collect { chatState ->
                        historyAdapter.submitList(conversationListViewModel.state.value.conversations, chatState.conversationId)
                    }
                }
            }
        }
    }

    private fun showRenameDialog(conversationId: String, currentTitle: String) {
        val dialogLayout = LayoutInflater.from(this).inflate(R.layout.layout_dialog_input, null)
        val input = dialogLayout.findViewById<EditText>(R.id.dialogEditText).apply {
            setText(currentTitle)
            setSelection(currentTitle.length)
            requestFocus()
        }
        
        GlassDialog.build()
            .setContent("Rename Project", "Enter a new name for your workspace.")
            .setCustomView(dialogLayout)
            .setButtons("Save", "Cancel") {
                val newTitle = input.text.toString().trim()
                if (newTitle.isNotBlank()) {
                    conversationListViewModel.renameConversation(conversationId, newTitle)
                    GlassToast.show(this, "Project renamed")
                }
            }
            .show(supportFragmentManager)
    }

    private fun observeActiveChat(
        tvTitle: TextView, 
        sendBtn: ImageButton, 
        input: EditText, 
        emptyState: View, 
        toolApprovalContainer: View, 
        tvToolApprovalText: TextView, 
        btnStopGeneration: View
    ) {
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
                            content = state.statusLine,
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

                    // Only enable input if not busy loading/streaming/approving
                    val busy = state.isSending || state.isStreaming || state.pendingToolApproval != null
                    sendBtn.isEnabled = !busy
                    sendBtn.alpha = if (busy) 0.4f else 1.0f
                    
                    chatAdapter.submitList(allMessages, streamingId, busy)

                    val showApproval = state.pendingToolApproval != null
                    if (toolApprovalContainer.visibility == View.VISIBLE && !showApproval || 
                        toolApprovalContainer.visibility == View.GONE && showApproval) {
                        TransitionManager.beginDelayedTransition(findViewById(R.id.chatRoot))
                    }

                    if (showApproval) {
                        toolApprovalContainer.visibility = View.VISIBLE
                        val toolName = state.pendingToolApproval.toolName
                        tvToolApprovalText.text = Html.fromHtml("Agent wants to run <b>$toolName</b>", Html.FROM_HTML_MODE_LEGACY)
                    } else {
                        toolApprovalContainer.visibility = View.GONE
                    }
                    
                    btnStopGeneration.visibility = if (state.isStreaming || state.isSending) View.VISIBLE else View.GONE

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