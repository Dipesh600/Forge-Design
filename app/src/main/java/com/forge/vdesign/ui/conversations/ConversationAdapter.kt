package com.forge.vdesign.ui.conversations

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView   = itemView.findViewById(R.id.tvTitle)
        private val tvPreview: TextView = itemView.findViewById(R.id.tvPreview)
        private val tvTime: TextView    = itemView.findViewById(R.id.tvTime)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(convo: Conversation) {
            tvTitle.text   = convo.title.ifBlank { "New Chat" }
            tvPreview.text = convo.title.ifBlank { "Tap to continue…" }
            tvTime.text    = formatTime(convo.updatedAt)

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
