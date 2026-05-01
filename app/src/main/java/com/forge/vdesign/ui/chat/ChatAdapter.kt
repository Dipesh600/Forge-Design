package com.forge.vdesign.ui.chat

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
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
    private val onOpenCanvas: (view: View, brief: DesignBrief, prompt: String) -> Unit = { _, _, _ -> },
    private val onCopyMessage: (String) -> Unit = {},
    private val onRetryMessage: (ChatMessage) -> Unit = {},
    private val onRejectScreen: (String) -> Unit = {}
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    // Built once per adapter instance — Markwon is thread-safe for reads.
    private var markwon: Markwon? = null

    private fun getOrCreateMarkwon(context: Context): Markwon {
        return markwon ?: Markwon.builder(context)
            .usePlugin(GlideImagesPlugin.create(context))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(object : io.noties.markwon.AbstractMarkwonPlugin() {
                override fun configureTheme(builder: io.noties.markwon.core.MarkwonTheme.Builder) {
                    val codeBgColor = android.graphics.Color.parseColor("#1AFFFFFF")
                    val codeTextColor = android.graphics.Color.parseColor("#E8E8F0")
                    builder.codeBackgroundColor(codeBgColor)
                        .codeTextColor(codeTextColor)
                        .codeBlockBackgroundColor(codeBgColor)
                        .codeBlockTextColor(codeTextColor)
                }
            })
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
        const val VIEW_TYPE_ACTIVITY_TIMELINE = 7
        const val VIEW_TYPE_STATUS         = 8
    }
    
    // Track expanded thinking message IDs locally in the adapter
    private val expandedThinkingIds = mutableSetOf<String>()

    /** ID of the message currently being streamed — shows blinking cursor */
    private var streamingMessageId: String? = null
    private var isBusy: Boolean = false

    /** Update the list AND which message is actively streaming */
    fun submitList(messages: List<ChatMessage>, streamingId: String?, isBusy: Boolean) {
        this.streamingMessageId = streamingId
        this.isBusy = isBusy
        submitList(messages)
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return when {
            message.id.startsWith("timeline_") -> VIEW_TYPE_ACTIVITY_TIMELINE
            message.id == "forge_thinking"    -> VIEW_TYPE_THINKING
            message.id == "forge_status"      -> VIEW_TYPE_STATUS
            message.isScreenCard              -> VIEW_TYPE_SCREEN_CARD
            message.isAgentLog                -> VIEW_TYPE_AGENT_ACTIVITY
            message.isCanvasCard              -> VIEW_TYPE_CANVAS_CARD
            message.role == MessageRole.USER  -> VIEW_TYPE_USER
            else                              -> VIEW_TYPE_AI
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_USER           -> TextViewHolder(inflater.inflate(R.layout.item_message_user, parent, false))
            VIEW_TYPE_CANVAS_CARD    -> CanvasCardViewHolder(inflater.inflate(R.layout.item_message_canvas_card, parent, false), onOpenCanvas)
            VIEW_TYPE_AGENT_ACTIVITY -> AgentActivityViewHolder(inflater.inflate(R.layout.item_message_agent_activity, parent, false))
            VIEW_TYPE_SCREEN_CARD    -> ScreenCardViewHolder(inflater.inflate(R.layout.item_screen_result_card, parent, false), onRejectScreen)
            VIEW_TYPE_THINKING       -> ThinkingViewHolder(inflater.inflate(R.layout.item_message_thinking, parent, false))
            VIEW_TYPE_ACTIVITY_TIMELINE -> TimelineViewHolder(inflater.inflate(R.layout.item_message_activity_timeline, parent, false))
            VIEW_TYPE_STATUS         -> StatusViewHolder(inflater.inflate(R.layout.item_message_status, parent, false))
            else                     -> AiViewHolder(
                inflater.inflate(R.layout.item_message_ai, parent, false),
                ::getOrCreateMarkwon,
                onCopyMessage,
                onRetryMessage
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is TextViewHolder          -> holder.bind(message)
            is AiViewHolder            -> holder.bind(message, message.id == streamingMessageId, isBusy)
            is CanvasCardViewHolder    -> holder.bind(message)
            is AgentActivityViewHolder -> holder.bind(message, expandedThinkingIds, ::notifyItemChanged)
            is ScreenCardViewHolder    -> holder.bind(message)
            is ThinkingViewHolder      -> holder.bind(message, expandedThinkingIds.contains(message.conversationId))
            is TimelineViewHolder      -> holder.bind(message, expandedThinkingIds)
            is StatusViewHolder        -> holder.bind(message)
        }
    }

    // ── ViewHolders ───────────────────────────────────────────────────────────

    class TextViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        fun bind(message: ChatMessage) { messageText.text = message.content }
    }

    class AiViewHolder(
        itemView: View,
        private val markwonProvider: (Context) -> Markwon,
        private val onCopyMessage: (String) -> Unit,
        private val onRetryMessage: (ChatMessage) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val cursorView: TextView? = itemView.findViewById(R.id.tvStreamingCursor)
        private val actionsGroup: View? = itemView.findViewById(R.id.aiActionsGroup)
        private val btnCopy: View? = itemView.findViewById(R.id.btnCopy)
        private val btnRetry: View? = itemView.findViewById(R.id.btnRetry)

        fun bind(message: ChatMessage, isStreaming: Boolean, isGlobalBusy: Boolean) {
            if (message.isLoading && message.content.isEmpty()) {
                // Typing indicator — just dots, no markdown needed
                messageText.text = "✴ "
                cursorView?.visibility = View.VISIBLE
                actionsGroup?.visibility = View.GONE
            } else {
                // Render markdown: ###, **bold**, lists, inline images, etc.
                val markwon = markwonProvider(itemView.context)
                markwon.setMarkdown(messageText, message.content)
                cursorView?.visibility = if (isStreaming) View.VISIBLE else View.GONE
                
                if (isStreaming || isGlobalBusy) {
                    actionsGroup?.visibility = View.GONE
                } else {
                    actionsGroup?.visibility = View.VISIBLE
                    btnCopy?.setOnClickListener { onCopyMessage(message.content) }
                    btnRetry?.setOnClickListener { onRetryMessage(message) }
                }
            }
        }
    }

    /**
     * Renders a persisted agent activity log (e.g. tool call or tool result).
     * Rendered as a collapsible UI similar to Thinking, but persists in history.
     */
    class AgentActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val llHeader: View = itemView.findViewById(R.id.llActivityHeader)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvActivityTitle)
        private val tvContent: TextView = itemView.findViewById(R.id.tvActivityContent)
        private val ivChevron: ImageView = itemView.findViewById(R.id.ivActivityChevron)

        fun bind(message: ChatMessage, expandedIds: MutableSet<String>, onChange: (Int) -> Unit) {
            tvTitle.text = message.agentLogTitle
            tvContent.text = message.agentLogContent

            val isExpanded = expandedIds.contains(message.id)
            tvContent.visibility = if (isExpanded) View.VISIBLE else View.GONE
            val chevronRot = if (isExpanded) 180f else 0f
            ivChevron.rotation = chevronRot

            // Pulsating animation for the activity orb
            itemView.findViewById<View>(R.id.viewActivityOrb)?.let { orb ->
                val anim = ObjectAnimator.ofFloat(orb, "alpha", 0.3f, 1.0f)
                anim.duration = 800
                anim.repeatMode = ValueAnimator.REVERSE
                anim.repeatCount = ValueAnimator.INFINITE
                anim.start()
            }

            llHeader.setOnClickListener {
                if (expandedIds.contains(message.id)) {
                    expandedIds.remove(message.id)
                } else {
                    expandedIds.add(message.id)
                }
                onChange(bindingAdapterPosition)
            }
        }
    }

    class CanvasCardViewHolder(
        itemView: View,
        private val onOpenCanvas: (View, DesignBrief, String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvProjectName: TextView = itemView.findViewById(R.id.tvProjectName)
        private val tvScreensList: TextView = itemView.findViewById(R.id.tvScreensList)
        private val tvMoodChip: TextView    = itemView.findViewById(R.id.tvCanvasPrompt)
        private val btnOpen: MaterialButton = itemView.findViewById(R.id.btnOpenCanvas)
        private val canvasCard: View        = itemView.findViewById(R.id.canvasCard)

        fun bind(message: ChatMessage) {
            val brief = message.embeddedBrief

            if (brief != null) {
                // Rich display from the compiled brief
                tvProjectName.text = brief.projectName
                tvScreensList.text = brief.plannedScreens.joinToString("  ·  ")
                tvMoodChip.text    = brief.mood

                // Show screen count on button
                val count = brief.plannedScreens.size
                btnOpen.text = "Open in Studio →  ($count screens)"
            } else {
                // Legacy canvas card — no brief embedded
                tvProjectName.text = "FORGE Studio"
                tvScreensList.text = message.content.take(80)
                tvMoodChip.text    = "custom"
                btnOpen.text       = "Open in Studio →"
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
                onOpenCanvas(canvasCard, fallbackBrief, fallbackBrief.rawPrompt)
            }
        }
    }

    /**
     * Screen result card — shown inline in chat when FORGE generates a screen.
     * Loads screenshotUrl via Glide. Taps open ScreenPreviewActivity (in-app WebView).
     */
    class ScreenCardViewHolder(
        itemView: View,
        private val onRejectScreen: (String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val imgScreenshot: ImageView = itemView.findViewById(R.id.imgScreenshot)
        private val tvScreenName: TextView   = itemView.findViewById(R.id.tvScreenName)
        private val cardRoot: View           = itemView.findViewById(R.id.cardScreen)
        private val btnPreview: TextView     = itemView.findViewById(R.id.btnPreview)
        private val btnRejectScreen: TextView = itemView.findViewById(R.id.btnRejectScreen)

        fun bind(message: ChatMessage) {
            tvScreenName.text = message.content  // content = screen name
            
            if (message.isRejected) {
                cardRoot.alpha = 0.5f
                btnRejectScreen.text = "Rejected"
                btnRejectScreen.textSize = 10f
                btnRejectScreen.isEnabled = false
                btnRejectScreen.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                btnRejectScreen.setPadding(12, 0, 12, 0)
            } else {
                cardRoot.alpha = 1.0f
                btnRejectScreen.text = "✖"
                btnRejectScreen.textSize = 18f
                btnRejectScreen.isEnabled = true
                btnRejectScreen.layoutParams.width = itemView.context.resources.displayMetrics.density.toInt() * 36
                btnRejectScreen.setPadding(0, 0, 0, 0)
                btnRejectScreen.setOnClickListener { onRejectScreen(message.id) }
            }

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
                    .placeholder(R.drawable.bg_image_placeholder)
                    .transition(DrawableTransitionOptions.withCrossFade(300))
                    .into(imgScreenshot)
            } else {
                imgScreenshot.setImageResource(R.drawable.bg_image_placeholder)
            }

            val transitionName = "canvas_transition_${message.id}"
            androidx.core.view.ViewCompat.setTransitionName(imgScreenshot, transitionName)

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
                    ),
                    designReasoning = message.designReasoning.map { 
                        com.forge.vdesign.domain.model.DesignReasoning(
                            decision = it.decision,
                            principle = it.principle,
                            sourceBook = it.sourceBook,
                            skillId = it.skillId
                        )
                    }
                )
                
                val activity = ctx as? android.app.Activity
                val options = if (activity != null) {
                    androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(
                        activity, imgScreenshot, transitionName
                    ).toBundle()
                } else null
                
                com.forge.vdesign.ui.canvas.ScreenPreviewActivity.launch(ctx, screen, "", options)
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

            // Pulsating animation for the thinking orb
            itemView.findViewById<View>(R.id.viewThinkingOrb)?.let { orb ->
                val anim = ObjectAnimator.ofFloat(orb, "alpha", 0.3f, 1.0f)
                anim.duration = 1000
                anim.repeatMode = ValueAnimator.REVERSE
                anim.repeatCount = ValueAnimator.INFINITE
                anim.start()
            }

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

    class TimelineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val llHeader: View = itemView.findViewById(R.id.llTimelineHeader)
        private val ivChevron: ImageView = itemView.findViewById(R.id.ivTimelineChevron)
        private val llStepsContainer: LinearLayout = itemView.findViewById(R.id.llStepsContainer)

        fun bind(message: ChatMessage, expandedIds: MutableSet<String>) {
            val isExpanded = expandedIds.contains(message.id)
            ivChevron.rotation = if (isExpanded) 180f else 0f
            llStepsContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE

            llStepsContainer.removeAllViews()
            val inflater = LayoutInflater.from(itemView.context)

            message.agentTasks.forEachIndexed { index, task ->
                val stepView = inflater.inflate(R.layout.item_timeline_step, llStepsContainer, false)
                val tvTitle: TextView = stepView.findViewById(R.id.tvStepTitle)
                val tvContent: TextView = stepView.findViewById(R.id.tvStepContent)
                val viewLine: View = stepView.findViewById(R.id.viewTimelineLine)
                val orb: View = stepView.findViewById(R.id.viewStepOrb)

                tvTitle.text = task.label
                tvContent.text = task.detail ?: ""
                tvContent.visibility = if (task.detail.isNullOrEmpty()) View.GONE else View.VISIBLE
                
                // Hide line for the last item
                viewLine.visibility = if (index == message.agentTasks.size - 1) View.GONE else View.VISIBLE

                // Animate orb if it's the latest (and agent is busy)
                if (index == message.agentTasks.size - 1) {
                    val anim = ObjectAnimator.ofFloat(orb, "alpha", 0.4f, 1.0f)
                    anim.duration = 800
                    anim.repeatMode = ValueAnimator.REVERSE
                    anim.repeatCount = ValueAnimator.INFINITE
                    anim.start()
                } else {
                    orb.alpha = 0.5f // Dim previous steps
                }

                llStepsContainer.addView(stepView)
            }

            llHeader.setOnClickListener {
                if (expandedIds.contains(message.id)) {
                    expandedIds.remove(message.id)
                } else {
                    expandedIds.add(message.id)
                }
                (bindingAdapter as? ChatAdapter)?.notifyItemChanged(bindingAdapterPosition)
            }
        }
    }

    class StatusViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvStatusText: TextView = itemView.findViewById(R.id.tvStatusText)
        private val ivStatusSpinner: ImageView = itemView.findViewById(R.id.ivStatusSpinner)

        fun bind(message: ChatMessage) {
            tvStatusText.text = message.content

            // Rotation animation for the refresh icon
            val anim = ObjectAnimator.ofFloat(ivStatusSpinner, "rotation", 0f, 360f)
            anim.duration = 1000
            anim.repeatMode = ValueAnimator.RESTART
            anim.repeatCount = ValueAnimator.INFINITE
            anim.interpolator = android.view.animation.LinearInterpolator()
            anim.start()
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
