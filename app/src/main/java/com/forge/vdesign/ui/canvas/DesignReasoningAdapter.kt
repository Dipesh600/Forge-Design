package com.forge.vdesign.ui.canvas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forge.vdesign.R
import com.forge.vdesign.domain.model.DesignReasoning

/**
 * Adapter for the Design Reasoning panel in [ScreenCanvasActivity].
 * Shows each design decision with its principle and source book.
 */
class DesignReasoningAdapter :
    ListAdapter<DesignReasoning, DesignReasoningAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDecision: TextView = view.findViewById(R.id.tvDecision)
        val tvPrinciple: TextView = view.findViewById(R.id.tvPrinciple)
        val tvSourceBook: TextView = view.findViewById(R.id.tvSourceBook)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_design_reasoning, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.tvDecision.text = item.decision
        holder.tvPrinciple.text = "📐 ${item.principle}"
        holder.tvSourceBook.text = "📖 ${item.sourceBook}"
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<DesignReasoning>() {
            override fun areItemsTheSame(a: DesignReasoning, b: DesignReasoning) = a.decision == b.decision
            override fun areContentsTheSame(a: DesignReasoning, b: DesignReasoning) = a == b
        }
    }
}
