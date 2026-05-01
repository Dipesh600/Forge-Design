package com.forge.vdesign.ui.chat

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.button.MaterialButton
import com.forge.vdesign.R

class GlassDialog : DialogFragment() {

    private var title: String? = null
    private var message: String? = null
    private var positiveText: String = "Confirm"
    private var negativeText: String = "Cancel"
    private var onPositive: (() -> Unit)? = null
    private var onNegative: (() -> Unit)? = null
    private var customView: View? = null

    fun setContent(title: String?, message: String?) = apply {
        this.title = title
        this.message = message
    }

    fun setButtons(positive: String, negative: String = "Cancel", onPos: () -> Unit) = apply {
        this.positiveText = positive
        this.negativeText = negative
        this.onPositive = onPos
    }

    fun setCustomView(view: View) = apply {
        this.customView = view
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.layout_glass_dialog, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        view.findViewById<TextView>(R.id.dialogTitle).text = title
        view.findViewById<TextView>(R.id.dialogMessage).text = message
        
        if (customView != null) {
            val frame = view.findViewById<FrameLayout>(R.id.customViewContainer)
            frame.visibility = View.VISIBLE
            (customView?.parent as? ViewGroup)?.removeView(customView)
            frame.addView(customView)
        }

        view.findViewById<MaterialButton>(R.id.btnPositive).apply {
            text = positiveText
            setOnClickListener {
                onPositive?.invoke()
                dismiss()
            }
        }

        view.findViewById<MaterialButton>(R.id.btnNegative).apply {
            text = negativeText
            setOnClickListener {
                onNegative?.invoke()
                dismiss()
            }
        }

        return view
    }

    fun show(manager: FragmentManager) {
        show(manager, "GlassDialog")
    }

    companion object {
        fun build() = GlassDialog()
    }
}
