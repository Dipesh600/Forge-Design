package com.forge.vdesign.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
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
import com.forge.vdesign.ui.auth.AuthActivity
import com.forge.vdesign.ui.chat.ChatActivity
import com.forge.vdesign.ui.conversations.ConversationAdapter
import com.forge.vdesign.ui.conversations.ConversationListViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * MainActivity — FORGE Conversation List
 *
 * Entry point after auth. Shows all past conversations (ChatGPT-style sidebar).
 * Tapping a conversation opens ChatActivity with that conversationId.
 * New Chat FAB creates a fresh UUID conversation and opens ChatActivity.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: ConversationListViewModel by viewModels()
    private lateinit var adapter: ConversationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val toolbar:      MaterialToolbar               = findViewById(R.id.toolbar)
        val rvConvos:     RecyclerView                  = findViewById(R.id.rvConversations)
        val emptyState:   View                          = findViewById(R.id.emptyState)
        val fabNewChat:   ExtendedFloatingActionButton  = findViewById(R.id.fabNewChat)

        // Toolbar — sign out in menu
        setSupportActionBar(toolbar)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_sign_out -> { viewModel.signOut(); true }
                else -> false
            }
        }

        // Adapter
        adapter = ConversationAdapter(
            onOpen   = { convo -> openChat(convo.id) },
            onDelete = { convo ->
                // Confirm before deleting
                android.app.AlertDialog.Builder(this)
                    .setTitle("Delete conversation?")
                    .setMessage("\"${convo.title}\" will be permanently deleted.")
                    .setPositiveButton("Delete") { _, _ -> viewModel.deleteConversation(convo.id) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        rvConvos.adapter = adapter
        rvConvos.layoutManager = LinearLayoutManager(this)

        // New chat FAB
        fabNewChat.setOnClickListener {
            val newId = viewModel.createNewConversation()
            openChat(newId)
        }

        // Observe conversation list
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    if (state.isSignedOut) { goToAuth(); return@collect }

                    adapter.submitList(state.conversations)

                    val isEmpty = state.conversations.isEmpty() && !state.isLoading
                    emptyState.visibility  = if (isEmpty) View.VISIBLE else View.GONE
                    rvConvos.visibility    = if (isEmpty) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun openChat(conversationId: String) {
        ChatActivity.launch(this, conversationId)
    }

    private fun goToAuth() {
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }
}