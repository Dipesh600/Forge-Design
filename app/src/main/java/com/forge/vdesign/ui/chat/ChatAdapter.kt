package com.forge.vdesign.ui.chat

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.forge.vdesign.R
import com.forge.vdesign.domain.model.AgentTask
import com.forge.vdesign.domain.model.ChatMessage
import com.forge.vdesign.domain.model.DesignBrief
import com.forge.vdesign.domain.model.MessageRole
import com.forge.vdesign.domain.model.TaskStatus
import com.google.android.material.button.MaterialButton
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.image.glide.GlideImagesPlugin

/**
 * ChatAdapter — three view types:
 *   VIEW_TYPE_USER         → right-aligned bubble (user messages)
 *   VIEW_TYPE_AI           → left-aligned bubble, markdown-rendered, supports live streaming cursor ◌
 *   VIEW_TYPE_CANVAS_CARD  → FORGE Canvas action card (design ready to launch)
 *
 * AI messages are rendered with [Markwon] so that the LLM’s markdown output
 * (headers, bold, lists, inline images) displays as formatted content rather than
 * raw syntax like "**bold**" or "### heading".
 */
class ChatAdapter(
    private val onOpenCanvas: (brief: DesignBrief, prompt: String) -> Unit = { _, _ -> }
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    // Built once per adapter instance — Markwon is thread-safe for reads.
    private var markwon: Markwon? = null

    private fun getOrCreateMarkwon(context: Context): Markwon {
        return markwon ?: Markwon.builder(context)
            .usePlugin(GlideImagesPlugin.create(context))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .build()
            .also { markwon = it }
    }

    companion object {
        const val VIEW_TYPE_USER           = 1
        const val VIEW_TYPE_AI             = 2
        const val VIEW_TYPE_CANVAS_CARD    = 3
        const val VIEW_TYPE_AGENT_ACTIVITY = 4
        const val VIEW_TYPE_SCREEN_CARD    = 5
        const val VIEW_TYPE_THINKING       = 6
    }
    
    // Track expanded thinking message IDs locally in the adapter
    private val expandedThinkingIds = mutableSetOf<String>()

    /** ID of the message currently being streamed — shows blinking cursor */
    private var streamingMessageId: String? = null

    /** Update the list AND which message is actively streaming */
    fun submitList(messages: List<ChatMessage>, streamingId: String?) {
        streamingMessageId = streamingId
        submitList(messages)
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return when {
            message.id == "forge_thinking"    -> VIEW_TYPE_THINKING
            message.isScreenCard              -> VIEW_TYPE_SCREEN_CARD
            message.isAgentActivity           -> VIEW_TYPE_AGENT_ACTIVITY
            message.isCanvasCard              -> VIEW_TYPE_CANVAS_CARD
            message.role == MessageRole.USER  -> VIEW_TYPE_USER
            else                              -> VIEW_TYPE_AI
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_USER           -> TextViewHolder(inflater.inflate(R.layout.item_message_user, parent, false))
            VIEW_TYPE_CANVAS_CARD   -> CanvasCardViewHolder(inflater.inflate(R.layout.item_message_canvas_card, parent, false), onOpenCanvas)
            VIEW_TYPE_AGENT_ACTIVITY -> AgentActivityViewHolder(inflater.inflate(R.layout.item_agent_activity, parent, false))
            VIEW_TYPE_SCREEN_CARD   -> ScreenCardViewHolder(inflater.inflate(R.layout.item_screen_result_card, parent, false))
            VIEW_TYPE_THINKING       -> ThinkingViewHolder(inflater.inflate(R.layout.item_message_thinking, parent, false))
            else                     -> AiViewHolder(
                inflater.inflate(R.layout.item_message_ai, parent, false),
                ::getOrCreateMarkwon
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is TextViewHolder          -> holder.bind(message)
            is AiViewHolder            -> holder.bind(message, message.id == streamingMessageId)
            is CanvasCardViewHolder    -> holder.bind(message)
            is AgentActivityViewHolder -> holder.bind(message.agentTasks)
            is ScreenCardViewHolder    -> holder.bind(message)
            is ThinkingViewHolder      -> holder.bind(message, expandedThinkingIds.contains(message.conversationId) /* using convId as unique layout switch since thought is per-chat */)
        }
    }

    // ── ViewHolders ───────────────────────────────────────────────────────────

    class TextViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        fun bind(message: ChatMessage) { messageText.text = message.content }
    }

    class AiViewHolder(
        itemView: View,
        private val markwonProvider: (Context) -> Markwon
    ) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val cursorView: TextView? = itemView.findViewById(R.id.tvStreamingCursor)

        fun bind(message: ChatMessage, isStreaming: Boolean) {
            if (message.isLoading && message.content.isEmpty()) {
                // Typing indicator — just dots, no markdown needed
                messageText.text = "✴ "
                cursorView?.visibility = View.VISIBLE
            } else {
                // Render markdown: ###, **bold**, lists, inline images, etc.
                val markwon = markwonProvider(itemView.context)
                markwon.setMarkdown(messageText, message.content)
                cursorView?.visibility = if (isStreaming) View.VISIBLE else View.GONE
            }
        }
    }

    /**
     * Renders the live agent task log — a terminal-style panel distinct from chat bubbles.
     * Each AgentTask appears on its own line with ✓ / ⟳ / · / ✗ icon.
     */
    class AgentActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAgentLog: TextView    = itemView.findViewById(R.id.tvAgentLog)

        private val tvAgentStatus: TextView = itemView.findViewById(R.id.tvAgentStatus)
        private var pulseAnimator: ObjectAnimator? = null

        fun bind(tasks: List<AgentTask>) {
            if (tasks.isEmpty()) {
                tvAgentLog.text = "Starting..."
                tvAgentStatus.text = "Initializing"
                return
            }

            val activeTask = tasks.lastOrNull { it.status == TaskStatus.ACTIVE }
            tvAgentStatus.text = if (activeTask != null) "Working" else "Done"

            // Build the log text line by line
            tvAgentLog.text = tasks.joinToString("\n") { task ->
                buildString {
                    // Color coding via unicode trick — kept simple with monospace icons
                    when (task.status) {
                        TaskStatus.DONE    -> append("✓  ")
                        TaskStatus.ACTIVE  -> append("⟳  ")
                        TaskStatus.ERROR   -> append("✗  ")
                        TaskStatus.PENDING -> append("·  ")
                    }
                    append(task.label)
                    if (task.detail != null) append(" — ${task.detail}")
                }
            }

            // Pulse the "Working" label while an active task is in flight
            pulseAnimator?.cancel()
            if (activeTask != null) {
                pulseAnimator = ObjectAnimator.ofFloat(tvAgentStatus, "alpha", 1f, 0.3f).apply {
                    duration = 700
                    repeatCount = ObjectAnimator.INFINITE
                    repeatMode = ObjectAnimator.REVERSE
                    start()
                }
            } else {
                tvAgentStatus.alpha = 1f
            }
        }
    }

    class CanvasCardViewHolder(
        itemView: View,
        private val onOpenCanvas: (DesignBrief, String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvProjectName: TextView = itemView.findViewById(R.id.tvProjectName)
        private val tvScreensList: TextView = itemView.findViewById(R.id.tvScreensList)
        private val tvMoodChip: TextView    = itemView.findViewById(R.id.tvCanvasPrompt)
        private val btnOpen: MaterialButton = itemView.findViewById(R.id.btnOpenCanvas)

        fun bind(message: ChatMessage) {
            val brief = message.embeddedBrief

            if (brief != null) {
                // Rich display from the compiled brief
                tvProjectName.text = brief.projectName
                tvScreensList.text = brief.plannedScreens.joinToString("  ·  ")
                tvMoodChip.text    = brief.mood

                // Show screen count on button
                val count = brief.plannedScreens.size
                btnOpen.text = "Open in Canvas →  ($count screens)"
            } else {
                // Legacy canvas card — no brief embedded
                tvProjectName.text = "FORGE Canvas"
                tvScreensList.text = message.content.take(80)
                tvMoodChip.text    = "custom"
                btnOpen.text       = "Open in Canvas →"
            }

            btnOpen.setOnClickListener {
                val fallbackBrief = brief ?: DesignBrief(
                    screenType     = "custom",
                    userGoal       = message.content,
                    constraints    = emptyList(),
                    mood           = "minimal",
                    projectName    = "FORGE Project",
                    plannedScreens = listOf("Main Screen"),
                    rawPrompt      = message.content
                )
                onOpenCanvas(fallbackBrief, fallbackBrief.rawPrompt)
            }
        }
    }

    /**
     * Screen result card — shown inline in chat when FORGE generates a screen.
     * Loads screenshotUrl via Glide. Taps open ScreenPreviewActivity (in-app WebView).
     */
    class ScreenCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgScreenshot: ImageView = itemView.findViewById(R.id.imgScreenshot)
        private val tvScreenName: TextView   = itemView.findViewById(R.id.tvScreenName)
        private val cardRoot: View           = itemView.findViewById(R.id.cardScreen)
        private val btnPreview: TextView     = itemView.findViewById(R.id.btnPreview)

        fun bind(message: ChatMessage) {
            tvScreenName.text = message.content  // content = screen name

            var url = message.screenshotUrl
            if (!url.isNullOrBlank()) {
                // Stitch backend often returns 144px thumbnail URLs from Google servers (e.g. ...=s144-c).
                // We forcefully upgrade the Google display flag to =s1080 for high resolution.
                if (url.contains("googleusercontent.com")) {
                    if (url.lastIndexOf('=') > url.lastIndexOf('/')) {
                        url = url.substring(0, url.lastIndexOf('=')) + "=s1080"
                    } else if (!url.contains("=")) {
                        url += "=s1080"
                    }
                }
                
                Glide.with(itemView.context)
                    .load(url)
                    .placeholder(R.drawable.bg_screen_badge)
                    .transition(DrawableTransitionOptions.withCrossFade(300))
                    .into(imgScreenshot)
            } else {
                imgScreenshot.setImageResource(R.drawable.bg_screen_badge)
            }

            val openPreview = View.OnClickListener {
                val ctx = itemView.context
                val screen = com.forge.vdesign.domain.model.GeneratedScreen(
                    screenId      = message.id,
                    screenName    = message.content,
                    screenshotUrl = message.screenshotUrl,
                    htmlUrl       = message.htmlUrl,
                    brief         = com.forge.vdesign.domain.model.DesignBrief(
                        screenType     = "custom",
                        userGoal       = message.content,
                        constraints    = emptyList(),
                        mood           = "forge",
                        projectName    = message.content,
                        plannedScreens = listOf(message.content),
                        rawPrompt      = message.content
                    )
                )
                com.forge.vdesign.ui.canvas.ScreenPreviewActivity.launch(ctx, screen)
            }
            cardRoot.setOnClickListener(openPreview)
            btnPreview.setOnClickListener(openPreview)
        }
    }
    
    class ThinkingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val llHeader: View = itemView.findViewById(R.id.llThinkingHeader)
        private val tvContent: TextView = itemView.findViewById(R.id.tvThinkingContent)
        private val ivChevron: ImageView = itemView.findViewById(R.id.ivThinkingChevron)

        fun bind(message: ChatMessage, isExpanded: Boolean) {
            tvContent.text = message.content
            
            // Apply expanded state
            tvContent.visibility = if (isExpanded) View.VISIBLE else View.GONE
            ivChevron.rotation = if (isExpanded) 180f else 0f

            llHeader.setOnClickListener {
                val adapter = bindingAdapter as? ChatAdapter ?: return@setOnClickListener
                val expanded = adapter.expandedThinkingIds.contains(message.conversationId)
                if (expanded) {
                    adapter.expandedThinkingIds.remove(message.conversationId)
                } else {
                    adapter.expandedThinkingIds.add(message.conversationId)
                }
                adapter.notifyItemChanged(bindingAdapterPosition)
            }
        }
    }

    // ── Diff ──────────────────────────────────────────────────────────────────

    class MessageDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage) =
            oldItem == newItem
    }
}
