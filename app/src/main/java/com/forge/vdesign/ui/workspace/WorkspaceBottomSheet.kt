package com.forge.vdesign.ui.workspace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.forge.vdesign.R
import com.forge.vdesign.domain.model.ChatMessage
import com.forge.vdesign.ui.canvas.ScreenPreviewActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.concurrent.TimeUnit

/**
 * WorkspaceBottomSheet — a BottomSheet that surfaces all screens generated for the
 * active conversation. Each screen card is tappable → opens ScreenPreviewActivity.
 *
 * Data is injected directly via [bind] from MainActivity, which already holds the
 * filtered screen_card messages from the active chat state.
 */
class WorkspaceBottomSheet : BottomSheetDialogFragment() {

    private lateinit var adapter: WorkspaceScreenAdapter
    
    override fun getTheme(): Int = R.style.TransparentBottomSheetDialog

    var pendingScreens: List<ChatMessage>? = null
    var pendingProjectTitle: String? = null

    var onEditScreenClicked: ((ChatMessage) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_workspace, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = WorkspaceScreenAdapter(
            onPreviewClick = { screen ->
                ScreenPreviewActivity.launchSimple(requireContext(), screen.content, screen.htmlUrl, screen.screenshotUrl)
                dismiss()
            },
            onEditClick = { screen ->
                onEditScreenClicked?.invoke(screen)
                dismiss()
            }
        )

        view.findViewById<RecyclerView>(R.id.rvWorkspaceScreens).apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            this.adapter = this@WorkspaceBottomSheet.adapter
        }

        if (pendingScreens != null && pendingProjectTitle != null) {
            bind(pendingScreens!!, pendingProjectTitle!!)
        }
    }

    /** Called from MainActivity with the current screen list for the active chat */
    fun bind(screens: List<ChatMessage>, projectTitle: String) {
        val isEmpty = screens.isEmpty()
        view?.findViewById<View>(R.id.workspaceEmpty)?.visibility = if (isEmpty) View.VISIBLE else View.GONE
        view?.findViewById<RecyclerView>(R.id.rvWorkspaceScreens)?.visibility = if (isEmpty) View.GONE else View.VISIBLE
        view?.findViewById<TextView>(R.id.tvWorkspaceTitle)?.text = "$projectTitle · Workspace"
        view?.findViewById<TextView>(R.id.tvScreenCount)?.text = "${screens.size} screen${if (screens.size != 1) "s" else ""}"
        adapter.submitList(screens)
    }

    class WorkspaceScreenAdapter(
        private val onPreviewClick: (ChatMessage) -> Unit,
        private val onEditClick: (ChatMessage) -> Unit
    ) : ListAdapter<ChatMessage, WorkspaceScreenAdapter.VH>(DiffCB()) {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val thumb: ImageView = view.findViewById(R.id.imgScreenThumb)
            val name: TextView = view.findViewById(R.id.tvScreenName)
            val id: TextView = view.findViewById(R.id.tvScreenId)
            val time: TextView = view.findViewById(R.id.tvScreenTime)
            val btnEdit: View = view.findViewById(R.id.btnEditScreen)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_workspace_screen, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val msg = getItem(position)
            holder.name.text = msg.content.ifBlank { "Screen ${position + 1}" }
            holder.id.text = "ID: ${msg.screenCardProjectId ?: msg.id.take(8)}"
            holder.time.text = formatRelativeTime(msg.timestamp)
            if (msg.screenshotUrl != null) {
                Glide.with(holder.thumb).load(msg.screenshotUrl).centerCrop().into(holder.thumb)
            } else {
                holder.thumb.setImageResource(android.R.drawable.ic_menu_gallery)
            }
            holder.thumb.setOnClickListener { onPreviewClick(msg) }
            holder.btnEdit.setOnClickListener { onEditClick(msg) }
        }

        private fun formatRelativeTime(ts: Long): String {
            val delta = System.currentTimeMillis() - ts
            return when {
                delta < TimeUnit.MINUTES.toMillis(1) -> "just now"
                delta < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(delta)}m ago"
                delta < TimeUnit.HOURS.toMillis(24) -> "${TimeUnit.MILLISECONDS.toHours(delta)}h ago"
                else -> "${TimeUnit.MILLISECONDS.toDays(delta)}d ago"
            }
        }

        class DiffCB : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(o: ChatMessage, n: ChatMessage) = o.id == n.id
            override fun areContentsTheSame(o: ChatMessage, n: ChatMessage) = o == n
        }
    }

    companion object {
        const val TAG = "WorkspaceBottomSheet"
    }
}
