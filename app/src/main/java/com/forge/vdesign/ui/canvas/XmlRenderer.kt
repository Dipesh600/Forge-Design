package com.forge.vdesign.ui.canvas

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/**
 * XmlRenderer
 *
 * Renders a raw Android XML string into a live View hierarchy at runtime.
 *
 * Strategy:
 *   - Parse the XML string into a View tree
 *   - Map XML element names to concrete Android Views using a safe allowlist
 *   - Apply basic attributes (layout_width, layout_height, padding, text, etc.)
 *   - Resolve @color, @dimen, @string refs from FORGE resource stubs
 *   - Return Result<View> — never throw, never crash the host Activity
 *
 * Supported root views: LinearLayout, FrameLayout, ScrollView
 * Supported leaves: TextView, MaterialButton, ImageView, MaterialCardView, View
 */
object XmlRenderer {

    private const val TAG = "XmlRenderer"

    /**
     * Parse and render [xml] string into a View.
     * Returns null on any error (caller should show error state).
     */
    fun render(context: Context, xml: String?): View? {
        if (xml.isNullOrBlank()) return null
        return try {
            val factory = android.view.LayoutInflater.from(context)

            // Attempt 1: Try Android LayoutInflater directly.
            // This works when the XML is properly formed with namespace declarations.
            val cleanXml = xml.trim()
            val stream = cleanXml.byteInputStream(Charsets.UTF_8)
            val parser = android.util.Xml.newPullParser()
            parser.setInput(stream, "UTF-8")
            factory.inflate(parser, null, false)

        } catch (e: Exception) {
            Log.w(TAG, "LayoutInflater failed (${e.message}), falling back to manual renderer")
            try {
                renderManual(context, xml)
            } catch (e2: Exception) {
                Log.e(TAG, "Manual renderer also failed", e2)
                createErrorPlaceholder(context, e2.message ?: "Render error")
            }
        }
    }

    /**
     * Manual rendering fallback — builds a simplified View tree from XML tags.
     * Used when the XML references unknown views or resources that LayoutInflater rejects.
     */
    private fun renderManual(context: Context, xml: String): View {
        val parser = android.util.Xml.newPullParser()
        parser.setInput(xml.reader())

        val root = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        var eventType = parser.eventType
        val stack = ArrayDeque<ViewGroup>()
        stack.addLast(root)

        while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                org.xmlpull.v1.XmlPullParser.START_TAG -> {
                    val tagName = parser.name ?: ""
                    val view = createView(context, tagName, parser)
                    val parent = stack.lastOrNull()
                    if (view != null && parent != null) {
                        parent.addView(view)
                        if (view is ViewGroup) {
                            stack.addLast(view)
                        }
                    }
                }
                org.xmlpull.v1.XmlPullParser.END_TAG -> {
                    val tagName = parser.name ?: ""
                    if (isLayoutTag(tagName) && stack.size > 1) {
                        stack.removeLast()
                    }
                }
            }
            eventType = parser.next()
        }

        return root
    }

    private fun createView(context: Context, tagName: String, parser: org.xmlpull.v1.XmlPullParser): View? {
        val attrs = extractAttributes(parser)
        val width = resolveSize(attrs["android:layout_width"], context, ViewGroup.LayoutParams.MATCH_PARENT)
        val height = resolveSize(attrs["android:layout_height"], context, ViewGroup.LayoutParams.WRAP_CONTENT)

        return when {
            tagName.contains("LinearLayout") -> LinearLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(width, height)
                orientation = if (attrs["android:orientation"] == "horizontal")
                    LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
                applyPadding(this, attrs, context)
            }
            tagName.contains("FrameLayout") || tagName.contains("RelativeLayout") -> FrameLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(width, height)
                applyPadding(this, attrs, context)
            }
            tagName.contains("ScrollView") || tagName.contains("NestedScrollView") -> ScrollView(context).apply {
                layoutParams = ViewGroup.LayoutParams(width, height)
            }
            tagName.contains("CardView") -> MaterialCardView(context).apply {
                layoutParams = ViewGroup.LayoutParams(width, height)
                val cornerRadiusPx = dpToPx(context, 12).toFloat()
                radius = cornerRadiusPx
                cardElevation = dpToPx(context, 2).toFloat()
                applyPadding(this, attrs, context)
            }
            tagName.contains("Button") -> MaterialButton(context).apply {
                layoutParams = ViewGroup.LayoutParams(width, height)
                val textVal = resolveText(attrs["android:text"], context)
                if (textVal.isNotBlank()) text = textVal
            }
            tagName.contains("TextView") -> TextView(context).apply {
                layoutParams = ViewGroup.LayoutParams(width, height)
                val textVal = resolveText(attrs["android:text"], context)
                if (textVal.isNotBlank()) text = textVal
                attrs["android:textSize"]?.let { sizeStr ->
                    val sp = sizeStr.replace("sp", "").toFloatOrNull()
                    if (sp != null) textSize = sp
                }
                applyPadding(this, attrs, context)
            }
            tagName.contains("ImageView") || tagName.contains("Image") -> ImageView(context).apply {
                layoutParams = ViewGroup.LayoutParams(width, height)
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(android.graphics.Color.parseColor("#E8EAF6"))
            }
            tagName == "View" -> View(context).apply {
                layoutParams = ViewGroup.LayoutParams(width, height)
                val bgColor = attrs["android:background"]
                if (bgColor != null && bgColor.startsWith("#")) {
                    try { setBackgroundColor(android.graphics.Color.parseColor(bgColor)) } catch (_: Exception) {}
                }
            }
            // Skip unknown XML tags (xml declaration, includes, etc.)
            else -> null
        }
    }

    private fun extractAttributes(parser: org.xmlpull.v1.XmlPullParser): Map<String, String> {
        val attrs = mutableMapOf<String, String>()
        for (i in 0 until parser.attributeCount) {
            attrs["${parser.getAttributePrefix(i)}:${parser.getAttributeName(i)}"] = parser.getAttributeValue(i)
        }
        return attrs
    }

    private fun resolveSize(value: String?, context: Context, default: Int): Int = when (value) {
        "match_parent", "fill_parent" -> ViewGroup.LayoutParams.MATCH_PARENT
        "wrap_content" -> ViewGroup.LayoutParams.WRAP_CONTENT
        null -> default
        else -> value.replace("dp", "").toIntOrNull()?.let { dpToPx(context, it) } ?: default
    }

    private fun resolveText(value: String?, context: Context): String {
        if (value == null) return ""
        if (value.startsWith("@string/")) {
            val name = value.removePrefix("@string/")
            val id = context.resources.getIdentifier(name, "string", context.packageName)
            if (id != 0) return context.getString(id)
        }
        return value
    }

    private fun applyPadding(view: View, attrs: Map<String, String>, context: Context) {
        val all = attrs["android:padding"]?.replace("dp", "")?.toIntOrNull()?.let { dpToPx(context, it) } ?: 0
        val start = attrs["android:paddingStart"]?.replace("dp", "")?.toIntOrNull()?.let { dpToPx(context, it) } ?: all
        val end = attrs["android:paddingEnd"]?.replace("dp", "")?.toIntOrNull()?.let { dpToPx(context, it) } ?: all
        val top = attrs["android:paddingTop"]?.replace("dp", "")?.toIntOrNull()?.let { dpToPx(context, it) } ?: all
        val bottom = attrs["android:paddingBottom"]?.replace("dp", "")?.toIntOrNull()?.let { dpToPx(context, it) } ?: all
        view.setPaddingRelative(start, top, end, bottom)
    }

    private fun isLayoutTag(tag: String) = tag.contains("Layout") || tag.contains("ScrollView") ||
            tag.contains("CardView") || tag.contains("AppBar")

    private fun dpToPx(context: Context, dp: Int): Int =
        (dp * context.resources.displayMetrics.density).toInt()

    private fun createErrorPlaceholder(context: Context, message: String): View {
        return TextView(context).apply {
            text = "⚠ Layout render error\n$message"
            textSize = 12f
            setPadding(16, 16, 16, 16)
            setTextColor(android.graphics.Color.parseColor("#B00020"))
        }
    }
}
