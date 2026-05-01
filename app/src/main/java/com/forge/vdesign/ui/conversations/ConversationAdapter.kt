package com.forge.vdesign.ui.conversations

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.forge.vdesign.R
import com.forge.vdesign.domain.model.Conversation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ConversationAdapter(
    private val onOpen: (Conversation) -> Unit,
    private val onDelete: (Conversation) -> Unit
) : ListAdapter<Conversation, ConversationAdapter.ViewHolder>(DiffCallback()) {

    private var activeConversationId: String? = null

    fun submitList(list: List<Conversation>, activeId: String?) {
        activeConversationId = activeId
        submitList(list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val root: View = itemView.findViewById(R.id.convoItemRoot)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val btnDelete: View = itemView.findViewById(R.id.btnDelete)
        private val tvScreenCount: TextView = itemView.findViewById(R.id.tvScreenCount)
        private val imgThumbnail: ImageView = itemView.findViewById(R.id.imgThumbnail)
        private val tvGlyph: TextView = itemView.findViewById(R.id.tvGlyph)

        fun bind(convo: Conversation) {
            root.isSelected = (convo.id == activeConversationId)
            
            tvTitle.text = convo.title.ifBlank { "New Project" }
            tvTime.text = formatTime(convo.updatedAt)
            
            val countText = if (convo.screenCount == 1) "1 SCREEN CREATED" else "${convo.screenCount} SCREENS CREATED"
            tvScreenCount.text = countText
            tvScreenCount.visibility = if (convo.screenCount > 0) View.VISIBLE else View.GONE

            if (!convo.thumbnailUrl.isNullOrBlank()) {
                tvGlyph.visibility = View.GONE
                imgThumbnail.visibility = View.VISIBLE
                Glide.with(imgThumbnail)
                    .load(convo.thumbnailUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(imgThumbnail)
            } else {
                tvGlyph.visibility = View.VISIBLE
                imgThumbnail.visibility = View.GONE
            }

            itemView.setOnClickListener { onOpen(convo) }
            btnDelete.setOnClickListener { onDelete(convo) }
        }

        private fun formatTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val delta = now - timestamp
            return when {
                delta < TimeUnit.MINUTES.toMillis(1)  -> "just now"
                delta < TimeUnit.HOURS.toMillis(1)    -> "${TimeUnit.MILLISECONDS.toMinutes(delta)}m ago"
                delta < TimeUnit.HOURS.toMillis(24)   -> "${TimeUnit.MILLISECONDS.toHours(delta)}h ago"
                delta < TimeUnit.DAYS.toMillis(7)     -> "${TimeUnit.MILLISECONDS.toDays(delta)}d ago"
                else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Conversation>() {
        override fun areItemsTheSame(o: Conversation, n: Conversation) = o.id == n.id
        override fun areContentsTheSame(o: Conversation, n: Conversation) = o == n
    }
}
