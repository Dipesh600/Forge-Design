package com.forge.vdesign.ui.chat

import android.app.Activity
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.forge.vdesign.R

class GlassToast(private val activity: Activity) {

    fun show(message: String, iconRes: Int? = null) {
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        val toastView = LayoutInflater.from(activity).inflate(R.layout.layout_glass_toast, rootView, false)
        
        toastView.findViewById<TextView>(R.id.toastMessage).text = message
        val icon = toastView.findViewById<ImageView>(R.id.toastIcon)
        if (iconRes != null) {
            icon.setImageResource(iconRes)
            icon.visibility = View.VISIBLE
        } else {
            icon.visibility = View.GONE
            toastView.findViewById<TextView>(R.id.toastMessage).apply {
                val params = layoutParams as ViewGroup.MarginLayoutParams
                params.marginStart = 0
                layoutParams = params
            }
        }

        rootView.addView(toastView)

        // Position at top
        val params = toastView.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.topMargin = 100 // Safe area / top bar offset
        toastView.layoutParams = params

        // Animation
        toastView.translationY = -200f
        toastView.alpha = 0f
        
        toastView.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(400)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                toastView.postDelayed({
                    toastView.animate()
                        .translationY(-200f)
                        .alpha(0f)
                        .setDuration(400)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .withEndAction {
                            rootView.removeView(toastView)
                        }
                        .start()
                }, 2500)
            }
            .start()
    }

    companion object {
        fun show(activity: Activity, message: String, iconRes: Int? = null) {
            GlassToast(activity).show(message, iconRes)
        }
    }
}
