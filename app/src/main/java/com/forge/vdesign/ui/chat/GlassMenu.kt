package com.forge.vdesign.ui.chat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.forge.vdesign.R

class GlassMenu(private val context: Context) {

    private val popupWindow: PopupWindow
    private val container: LinearLayout

    init {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.layout_glass_menu_container, null)
        container = view.findViewById(R.id.menuItemsContainer)
        
        popupWindow = PopupWindow(context).apply {
            contentView = view
            width = ViewGroup.LayoutParams.WRAP_CONTENT
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            isFocusable = true
            isOutsideTouchable = true
            setBackgroundDrawable(null) // Handled by our layout
            elevation = 24f
        }
    }

    fun addItem(title: String, iconRes: Int, onClick: () -> Unit): GlassMenu {
        val inflater = LayoutInflater.from(context)
        val itemView = inflater.inflate(R.layout.item_glass_menu, container, false)
        
        itemView.findViewById<TextView>(R.id.tvItemTitle).text = title
        itemView.findViewById<ImageView>(R.id.ivItemIcon).setImageResource(iconRes)
        
        itemView.setOnClickListener {
            onClick()
            dismiss()
        }
        
        container.addView(itemView)
        return this
    }

    fun show(anchor: View) {
        // Adjust position if needed, or just show as dropdown
        popupWindow.showAsDropDown(anchor, 0, 8)
    }

    fun dismiss() {
        popupWindow.dismiss()
    }
}
