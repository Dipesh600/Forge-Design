package com.forge.vdesign.ui.chat

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * ChatActivity
 *
 * The FORGE chat screen. Receives [EXTRA_CONVERSATION_ID] via Intent.
 * ChatViewModel is initialized with that ID via SavedStateHandle (Hilt handles this).
 *
 * Architecture:
 *  - [RecyclerView] shows persisted messages from Room (never modified directly)
 *  - [streamingContainer] shows the growing AI response while streaming
 *  - When stream ends: Room inserts the message → RecyclerView updates automatically
 *  - No optimistic messages = no deletion bug
 */
@AndroidEntryPoint
class ChatActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CONVERSATION_ID = "extra_conversation_id"

        fun launch(from: android.content.Context, conversationId: String) {
            from.startActivity(
                Intent(from, ChatActivity::class.java).apply {
                    putExtra(EXTRA_CONVERSATION_ID, conversationId)
                }
            )
        }
    }

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutManager: LinearLayoutManager

    /**
     * The stable ID used for the live-streaming message item while the AI is generating.
     * DiffUtil sees the same ID on every token update → dispatches a CHANGE (no flicker),
     * not a REMOVE + INSERT. When streaming ends, this ID disappears and Room's real
     * message (with its own UUID) takes its place seamlessly.
     */
    private val STREAMING_ID = "forge_streaming_live"

    /** Scroll to the absolute bottom of the chat list (two-pass to handle tall items). */
    private fun scrollToBottom() {
        val lastPosition = chatAdapter.itemCount - 1
        if (lastPosition < 0) return
        recyclerView.scrollToPosition(lastPosition)
        recyclerView.post {
            val lastView = layoutManager.findViewByPosition(lastPosition) ?: return@post
            val overscroll = lastView.bottom - (recyclerView.height - recyclerView.paddingBottom)
            if (overscroll > 0) recyclerView.scrollBy(0, overscroll)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.chatRoot)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // Streaming container is no longer used — streaming happens inline in the RecyclerView.
        // Keep the view in the layout for backwards compatibility but always hide it.
        findViewById<View>(R.id.streamingContainer).visibility = View.GONE

        // Views
        val inputField: EditText    = findViewById(R.id.messageInput)
        val sendBtn:    ImageButton = findViewById(R.id.sendButton)
        val backBtn:    ImageButton = findViewById(R.id.btnBack)
        val signOutBtn: ImageButton = findViewById(R.id.btnSignOut)
        val tvTitle:    TextView    = findViewById(R.id.tvConversationTitle)

        // Adapter — persisted messages only (no streaming messages in this list)
        chatAdapter = ChatAdapter(
            onOpenCanvas = { brief, prompt ->
                val intent = Intent(this, ScreenCanvasActivity::class.java).apply {
                    putExtra(ScreenCanvasActivity.EXTRA_DESIGN_BRIEF, brief)
                    putExtra(ScreenCanvasActivity.EXTRA_PROMPT, prompt)
                }
                startActivity(intent)
            }
        )

        recyclerView = findViewById(R.id.chatRecyclerView)

        // ── CRITICAL: stackFromEnd=true is the standard chat-app pattern.
        //   It pins new messages to the bottom of the viewport and ensures that
        //   tall messages (long AI responses) show their BOTTOM edge first —
        //   exactly where the streaming cursor was — preventing the "scrolled up" jump.
        layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = chatAdapter
        recyclerView.itemAnimator = null // prevent flicker on list updates

        // Auto-scroll observer — handles all DiffUtil dispatch callbacks:
        //   • onItemRangeInserted : new message arrived
        //   • onChanged           : full list replaced (first load)
        //   • onItemRangeChanged  : ignored — content edits shouldn't hijack scroll
        chatAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = scrollToBottom()
            override fun onChanged() = scrollToBottom()
        })

        // Send button
        sendBtn.setOnClickListener {
            val text = inputField.text.toString().trim()
            if (text.isNotBlank()) {
                viewModel.sendMessage(text)
                inputField.text.clear()
            }
        }

        backBtn.setOnClickListener { finish() }
        signOutBtn.setOnClickListener { viewModel.signOut() }

        // ── Observe state ─────────────────────────────────────────────────
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.isSignedOut) { goToAuth(); return@collect }

                    tvTitle.text = state.conversationTitle

                    // Build display list — all live state appears as transient chat items
                    val liveSuffix = mutableListOf<ChatMessage>()

                    // Thinking bubble: collapsible reasoning from M2.7
                    if (state.thinkingContent != null) {
                        liveSuffix += ChatMessage(
                            id             = "forge_thinking",
                            conversationId = state.conversationId,
                            role           = MessageRole.ASSISTANT,
                            content        = state.thinkingContent
                        )
                    }

                    // Status line (one-liner)
                    if (state.statusLine != null) {
                        liveSuffix += ChatMessage(
                            id             = "forge_status",
                            conversationId = state.conversationId,
                            role           = MessageRole.ASSISTANT,
                            content        = "⟳  ${state.statusLine}",
                            isLoading      = true
                        )
                    }

                    // Streaming content bubble
                    val streamingId: String?
                    if ((state.isStreaming || state.isSending) && state.streamingContent.isNotEmpty()) {
                        val liveMessage = ChatMessage(
                            id             = STREAMING_ID,
                            conversationId = state.conversationId,
                            role           = MessageRole.ASSISTANT,
                            content        = state.streamingContent,
                            isLoading      = false
                        )
                        liveSuffix += liveMessage
                        streamingId = STREAMING_ID
                    } else {
                        streamingId = null
                    }

                    chatAdapter.submitList(state.persistedMessages + liveSuffix, streamingId)

                    val busy = state.isSending || state.isStreaming
                    sendBtn.isEnabled = !busy
                    sendBtn.alpha = if (busy) 0.4f else 1.0f

                    if (state.error != null) {
                        android.widget.Toast.makeText(
                            this@ChatActivity, state.error, android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    } // end setupObservers

    private fun goToAuth() {
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }
}

