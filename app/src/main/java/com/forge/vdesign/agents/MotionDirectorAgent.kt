package com.forge.vdesign.agents

import android.util.Log
import com.forge.vdesign.brain.MiniMaxClient
import com.forge.vdesign.brain.MiniMaxMessage
import com.forge.vdesign.brain.MiniMaxRequest
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MotionDirectorAgent
 *
 * Identifies transition opportunities in a generated screen and writes
 * Android Animator XML and MotionLayout constraint suggestions.
 *
 * Three categories of motion it handles:
 *   1. Screen entry   — how the screen appears (slide, fade, expand)
 *   2. Element reveal — staggered content animation (cards, list items)
 *   3. State changes  — micro-interactions (button press, loading, success)
 *
 * Output: MotionSpec containing:
 *   - Named transitions with their suggested Animator XML
 *   - MotionLayout constraint hints (startConstraintSet → endConstraintSet)
 *   - Timing values in ms with easing type
 *
 * Based on Material Motion system (container transform, shared axis, fade through, fade).
 * Called when user enables the Motion Layer in the canvas toolbar.
 */
@Singleton
class MotionDirectorAgent @Inject constructor(
    private val miniMaxClient: MiniMaxClient,
    private val gson: Gson
) {

    companion object {
        private const val TAG = "MotionDirectorAgent"
    }

    data class TransitionSpec(
        val name: String,           // "screen_entry", "card_reveal_0", "fab_press"
        val category: TransitionCategory,
        val easingType: EasingType,
        val durationMs: Int,
        val delayMs: Int,           // for stagger
        val description: String,    // human-readable explanation
        val animatorXmlHint: String // Animator XML snippet (not full file, just the key anim tag)
    )

    enum class TransitionCategory {
        SCREEN_ENTRY,       // Activity/Fragment enter transition
        ELEMENT_REVEAL,     // Staggered content appear
        STATE_CHANGE,       // Button press, toggle, loading
        NAVIGATION          // Back press, bottom nav switch
    }

    enum class EasingType(val interpolator: String) {
        SPRING("@interpolator/motion_spring"),
        STANDARD("@interpolator/motion_easing_standard"),
        EMPHASIZED("@interpolator/motion_easing_emphasized"),
        DECELERATE("@interpolator/motion_easing_emphasized_decelerate"),
        ACCELERATE("@interpolator/motion_easing_emphasized_accelerate")
    }

    data class MotionSpec(
        val screenName: String,
        val materialMotionPattern: MaterialMotionPattern,
        val transitions: List<TransitionSpec>,
        val motionLayoutHint: String,  // High-level MotionLayout suggestion as comment
        val totalDurationMs: Int
    )

    enum class MaterialMotionPattern {
        CONTAINER_TRANSFORM,    // For shared-element transitions
        SHARED_AXIS,            // For navigational transitions (Z axis = forward/back)
        FADE_THROUGH,           // For unrelated content swaps (bottom nav)
        FADE                    // For subtle elements appearing/disappearing
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Analyse a screen description and generate motion specifications.
     *
     * @param screenName Name of the screen
     * @param description Stitch's description of the screen
     * @param isNavigationTarget True if this screen is navigated TO from another
     */
    suspend fun generateMotionSpec(
        screenName: String,
        description: String?,
        isNavigationTarget: Boolean = true
    ): MotionSpec {
        if (description.isNullOrBlank()) return buildMinimalSpec(screenName)

        return try {
            val rawSpec = callMiniMaxForMotion(screenName, description, isNavigationTarget)
            parseMotionSpec(screenName, rawSpec)
        } catch (e: Exception) {
            Log.w(TAG, "MotionDirector failed for $screenName: ${e.message}")
            buildMinimalSpec(screenName)
        }
    }

    /**
     * Format the MotionSpec as a developer-readable summary for display in the canvas.
     */
    fun formatForDisplay(spec: MotionSpec): String = buildString {
        appendLine("✦ Motion Layer — ${spec.screenName}")
        appendLine("Pattern: ${spec.materialMotionPattern.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }}")
        appendLine("Total duration: ${spec.totalDurationMs}ms")
        appendLine()
        spec.transitions.forEach { t ->
            appendLine("▸ ${t.name} (${t.category.name.lowercase().replace('_', ' ')})")
            appendLine("  ${t.description}")
            appendLine("  ${t.durationMs}ms — ${t.easingType.name.lowercase()}")
            if (t.delayMs > 0) appendLine("  delay: ${t.delayMs}ms")
        }
        appendLine()
        appendLine(spec.motionLayoutHint)
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private suspend fun callMiniMaxForMotion(
        screenName: String,
        description: String,
        isNavigationTarget: Boolean
    ): MotionResponse {
        val systemPrompt = """
You are FORGE's Motion Director — a senior Android motion designer specialising in Material Motion.

Analyse the screen description and design animation specifications following Material Design 3 motion guidelines.
Output ONLY valid JSON matching this schema exactly:

{
  "materialMotionPattern": "CONTAINER_TRANSFORM|SHARED_AXIS|FADE_THROUGH|FADE",
  "transitions": [
    {
      "name": "short_snake_case_name",
      "category": "SCREEN_ENTRY|ELEMENT_REVEAL|STATE_CHANGE|NAVIGATION",
      "easingType": "SPRING|STANDARD|EMPHASIZED|DECELERATE|ACCELERATE",
      "durationMs": 300,
      "delayMs": 0,
      "description": "human-readable description",
      "animatorXmlHint": "<objectAnimator android:propertyName=\"alpha\" android:valueFrom=\"0\" android:valueTo=\"1\" android:duration=\"300\"/>"
    }
  ],
  "motionLayoutHint": "Short note on how to use MotionLayout for the main interaction",
  "totalDurationMs": 500
}

Rules:
- Max 5 transitions. Focus on the most impactful ones.
- Screen entry: ALWAYS DECELERATE easing, 300-500ms
- Stagger element reveals: 50ms delay between items, STANDARD easing
- Button state changes: SPRING easing, 150ms
- Choose materialMotionPattern based on whether this is a navigation target (SHARED_AXIS) or content swap (FADE_THROUGH)
- Write functional animatorXmlHint snippets — not pseudocode, actual XML attributes
        """.trimIndent()

        val userMessage = buildString {
            appendLine("Screen: $screenName")
            appendLine("Navigation target: $isNavigationTarget")
            appendLine("Description: $description")
        }

        val response = miniMaxClient.chatCompletion(
            MiniMaxRequest(messages = listOf(
                MiniMaxMessage("system", systemPrompt),
                MiniMaxMessage("user", userMessage)
            ))
        )
        val raw = response.choices?.firstOrNull()?.message?.content ?: "{}"
        val json = raw.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return gson.fromJson(json, MotionResponse::class.java)
    }

    private fun parseMotionSpec(screenName: String, response: MotionResponse): MotionSpec {
        val transitions = response.transitions?.map { raw ->
            TransitionSpec(
                name        = raw.name ?: "transition",
                category    = try {
                    TransitionCategory.valueOf(raw.category ?: "SCREEN_ENTRY")
                } catch (e: Exception) { TransitionCategory.SCREEN_ENTRY },
                easingType  = try {
                    EasingType.valueOf(raw.easingType ?: "STANDARD")
                } catch (e: Exception) { EasingType.STANDARD },
                durationMs  = raw.durationMs ?: 300,
                delayMs     = raw.delayMs ?: 0,
                description = raw.description ?: "Transition",
                animatorXmlHint = raw.animatorXmlHint ?: ""
            )
        } ?: buildDefaultTransitions(screenName)

        val pattern = try {
            MaterialMotionPattern.valueOf(response.materialMotionPattern ?: "SHARED_AXIS")
        } catch (e: Exception) { MaterialMotionPattern.SHARED_AXIS }

        return MotionSpec(
            screenName           = screenName,
            materialMotionPattern = pattern,
            transitions          = transitions,
            motionLayoutHint     = response.motionLayoutHint ?: "Use MotionLayout with constraints for complex animations",
            totalDurationMs      = response.totalDurationMs ?: transitions.maxOfOrNull { it.durationMs + it.delayMs } ?: 500
        )
    }

    private fun buildMinimalSpec(screenName: String) = MotionSpec(
        screenName            = screenName,
        materialMotionPattern = MaterialMotionPattern.SHARED_AXIS,
        transitions           = buildDefaultTransitions(screenName),
        motionLayoutHint      = "Implement with Transition XML in res/transition/ and apply to window",
        totalDurationMs       = 450
    )

    private fun buildDefaultTransitions(screenName: String) = listOf(
        TransitionSpec(
            name            = "screen_enter",
            category        = TransitionCategory.SCREEN_ENTRY,
            easingType      = EasingType.DECELERATE,
            durationMs      = 350,
            delayMs         = 0,
            description     = "$screenName slides up and fades in on entry",
            animatorXmlHint = """<objectAnimator android:propertyName="translationY"
    android:valueFrom="80dp" android:valueTo="0dp" android:duration="350"
    android:interpolator="@interpolator/motion_easing_emphasized_decelerate"/>"""
        ),
        TransitionSpec(
            name            = "content_reveal",
            category        = TransitionCategory.ELEMENT_REVEAL,
            easingType      = EasingType.STANDARD,
            durationMs      = 200,
            delayMs         = 100,
            description     = "Content elements fade in with 50ms stagger between items",
            animatorXmlHint = """<objectAnimator android:propertyName="alpha"
    android:valueFrom="0" android:valueTo="1" android:duration="200"
    android:startOffset="100"/>"""
        ),
        TransitionSpec(
            name            = "primary_action",
            category        = TransitionCategory.STATE_CHANGE,
            easingType      = EasingType.SPRING,
            durationMs      = 150,
            delayMs         = 0,
            description     = "Primary button scales slightly on press with spring physics",
            animatorXmlHint = """<objectAnimator android:propertyName="scaleX"
    android:valueFrom="1.0" android:valueTo="0.95" android:duration="150"/>"""
        )
    )

    // Internal deserialization models
    private data class MotionResponse(
        val materialMotionPattern: String?,
        val transitions: List<RawTransition>?,
        val motionLayoutHint: String?,
        val totalDurationMs: Int?
    )

    private data class RawTransition(
        val name: String?,
        val category: String?,
        val easingType: String?,
        val durationMs: Int?,
        val delayMs: Int?,
        val description: String?,
        val animatorXmlHint: String?
    )
}
