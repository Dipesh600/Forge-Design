package com.forge.vdesign.agents

import android.util.Log
import com.forge.vdesign.brain.MiniMaxClient
import com.forge.vdesign.brain.MiniMaxMessage
import com.forge.vdesign.brain.MiniMaxRequest
import com.forge.vdesign.domain.model.DesignBrief
import com.forge.vdesign.domain.model.DesignDna
import com.forge.vdesign.domain.model.GeneratedScreen
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MemoryKeeperAgent
 *
 * Extracts and maintains three persistent stores:
 *   1. Working Memory  — current session context (in-memory)
 *   2. Style Profile   — user's design preferences inferred over time (Firestore)
 *   3. Design DNA      — visual language of the current project (Firestore)
 *
 * After each screen is generated, the MemoryKeeper:
 *   - Calls MiniMax to extract colour palette, typography scale, spacing unit, component patterns
 *   - Merges these into the project's DesignDna
 *   - Writes to Firestore so the next screen can check coherence
 *
 * This is what makes FORGE cross-screen coherent — every new screen generation
 * receives the project's DNA as context, so colors, fonts, and components stay consistent.
 */
@Singleton
class MemoryKeeperAgent @Inject constructor(
    private val miniMaxClient: MiniMaxClient,
    private val firestore: FirebaseFirestore,
    private val gson: Gson
) {

    companion object {
        private const val TAG = "MemoryKeeperAgent"
        private const val COLLECTION_DNA  = "design_dna"
        private const val COLLECTION_STYLE = "style_profiles"
    }

    // ── Working Memory (in-memory, resets per session) ────────────────────────

    private val workingMemory = mutableListOf<String>() // rolling context snippets

    fun addToWorkingMemory(context: String) {
        workingMemory.add(context)
        if (workingMemory.size > 20) workingMemory.removeFirst()
    }

    fun getWorkingMemoryContext(): String = workingMemory.joinToString("\n")

    // ── Design DNA Extraction ─────────────────────────────────────────────────

    /**
     * After a screen is generated, extract its visual DNA and merge into project DNA.
     *
     * @param screen The freshly generated screen
     * @param existingDna The project's current DNA (null on first screen)
     * @return Updated DesignDna
     */
    suspend fun extractAndMergeDna(
        screen: GeneratedScreen,
        existingDna: DesignDna?
    ): DesignDna {
        val extractedDna = extractDnaFromScreen(screen, existingDna)
        saveDnaToFirestore(extractedDna)
        addToWorkingMemory("Generated ${screen.screenName}: score=${screen.criticScore}, mood=${screen.brief.mood}")
        return extractedDna
    }

    /**
     * Load the latest DNA for a project from Firestore.
     * Returns null if no DNA exists yet (first screen of a new project).
     */
    suspend fun loadProjectDna(projectId: String): DesignDna? {
        return try {
            val doc = firestore.collection(COLLECTION_DNA).document(projectId).get().await()
            if (doc.exists()) {
                @Suppress("UNCHECKED_CAST")
                DesignDna(
                    projectId       = projectId,
                    primaryColor    = doc.getString("primaryColor") ?: "#6750A4",
                    secondaryColor  = doc.getString("secondaryColor") ?: "#625B71",
                    neutralPalette  = (doc.get("neutralPalette") as? List<String>) ?: emptyList(),
                    typescaleHeadline = (doc.getLong("typescaleHeadline") ?: 24).toInt(),
                    typescaleBody     = (doc.getLong("typescaleBody") ?: 16).toInt(),
                    typescaleCaption  = (doc.getLong("typescaleCaption") ?: 12).toInt(),
                    cornerRadius    = (doc.getLong("cornerRadius") ?: 12).toInt(),
                    spacingUnit     = (doc.getLong("spacingUnit") ?: 8).toInt(),
                    componentPatterns = (doc.get("componentPatterns") as? List<String>) ?: emptyList(),
                    brandVoice      = doc.getString("brandVoice") ?: "friendly, clear, confident",
                    screenCount     = (doc.getLong("screenCount") ?: 0).toInt()
                )
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "loadProjectDna failed: ${e.message}")
            null
        }
    }

    /**
     * Build a context string from DNA to inject into new screen generation prompts.
     * This is the key mechanism for cross-screen coherence.
     */
    fun buildDnaContext(dna: DesignDna?): String {
        dna ?: return ""
        return buildString {
            appendLine("DESIGN DNA (maintain consistency with existing screens):")
            appendLine("  Brand voice: ${dna.brandVoice}")
            appendLine("  Primary colour: ${dna.primaryColor}")
            appendLine("  Secondary colour: ${dna.secondaryColor}")
            appendLine("  Typography: ${dna.typescaleHeadline}sp headline, ${dna.typescaleBody}sp body, ${dna.typescaleCaption}sp caption")
            appendLine("  Corner radius: ${dna.cornerRadius}dp")
            appendLine("  Spacing unit: ${dna.spacingUnit}dp grid")
            if (dna.componentPatterns.isNotEmpty()) {
                appendLine("  Components used: ${dna.componentPatterns.joinToString(", ")}")
            }
            appendLine("  Screens generated: ${dna.screenCount}")
        }
    }

    // ── Style Profile ─────────────────────────────────────────────────────────

    /**
     * Update the user's style profile based on their design choices.
     * Called when the user picks a variant, regenerates, or gives explicit feedback.
     *
     * @param userId Firebase Auth UID
     * @param signal "liked_minimal" | "liked_expressive" | "liked_structured" | "regenerated"
     */
    suspend fun updateStyleProfile(userId: String, signal: String) {
        try {
            val ref = firestore.collection(COLLECTION_STYLE).document(userId)
            val doc = ref.get().await()
            val counts = (doc.get("signals") as? Map<String, Long>)?.toMutableMap()
                ?: mutableMapOf()
            counts[signal] = (counts[signal] ?: 0L) + 1L
            ref.set(mapOf(
                "signals" to counts,
                "lastUpdated" to Timestamp.now(),
                "preferredStyle" to dominantStyle(counts)
            )).await()
        } catch (e: Exception) {
            Log.w(TAG, "updateStyleProfile failed: ${e.message}")
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private suspend fun extractDnaFromScreen(
        screen: GeneratedScreen,
        existingDna: DesignDna?
    ): DesignDna {
        val existingContext = if (existingDna != null) {
            "Existing DNA: primary=${existingDna.primaryColor}, body=${existingDna.typescaleBody}sp, radius=${existingDna.cornerRadius}dp"
        } else "No existing DNA (this is the first screen)"

        val systemPrompt = """
You are the Memory Keeper for FORGE. Extract design DNA from this screen description.
Output ONLY valid JSON — no markdown, no explanation.

Schema:
{
  "primaryColor": "#hexcode",
  "secondaryColor": "#hexcode",
  "neutralPalette": ["#hex1", "#hex2", "#hex3"],
  "typescaleHeadline": 24,
  "typescaleBody": 16,
  "typescaleCaption": 12,
  "cornerRadius": 12,
  "spacingUnit": 8,
  "componentPatterns": ["bottom-nav", "card-list", "fab"],
  "brandVoice": "3 adjectives e.g. friendly, clear, confident"
}

Rules:
- If the description doesn't mention a colour, infer from the mood: ${screen.brief.mood}
- cornerRadius: 4=sharp, 8=balanced, 16=rounded, 24=pill
- componentPatterns: list the Material 3 components you see referenced
- brandVoice: infer from app name + mood
- $existingContext — merge intelligently, don't discard existing values
        """.trimIndent()

        val userMessage = """
Screen: ${screen.screenName}
App: ${screen.brief.projectName}
Mood: ${screen.brief.mood}
Stitch description: ${screen.description ?: "No description available"}
Stitch suggestions: ${screen.stitchSuggestions.take(3).joinToString("; ")}
        """.trimIndent()

        return try {
            val response = miniMaxClient.chatCompletion(
                MiniMaxRequest(messages = listOf(
                    MiniMaxMessage("system", systemPrompt),
                    MiniMaxMessage("user", userMessage)
                ))
            )
            val raw = response.choices?.firstOrNull()?.message?.content ?: ""
            val json = raw.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val extracted = gson.fromJson(json, DnaResponse::class.java)

            DesignDna(
                projectId       = screen.brief.stitchProjectId ?: screen.brief.projectName,
                primaryColor    = extracted.primaryColor ?: existingDna?.primaryColor ?: "#6750A4",
                secondaryColor  = extracted.secondaryColor ?: existingDna?.secondaryColor ?: "#625B71",
                neutralPalette  = extracted.neutralPalette ?: existingDna?.neutralPalette ?: emptyList(),
                typescaleHeadline = extracted.typescaleHeadline ?: existingDna?.typescaleHeadline ?: 24,
                typescaleBody   = extracted.typescaleBody ?: existingDna?.typescaleBody ?: 16,
                typescaleCaption = extracted.typescaleCaption ?: existingDna?.typescaleCaption ?: 12,
                cornerRadius    = extracted.cornerRadius ?: existingDna?.cornerRadius ?: 12,
                spacingUnit     = extracted.spacingUnit ?: existingDna?.spacingUnit ?: 8,
                componentPatterns = extracted.componentPatterns ?: existingDna?.componentPatterns ?: emptyList(),
                brandVoice      = extracted.brandVoice ?: existingDna?.brandVoice ?: "friendly, clear, confident",
                screenCount     = (existingDna?.screenCount ?: 0) + 1
            )
        } catch (e: Exception) {
            Log.w(TAG, "DNA extraction failed, using fallback: ${e.message}")
            existingDna?.copy(screenCount = (existingDna.screenCount) + 1) ?: DesignDna(
                projectId       = screen.brief.stitchProjectId ?: screen.brief.projectName,
                primaryColor    = "#6750A4",
                secondaryColor  = "#625B71",
                neutralPalette  = listOf("#FFFBFE", "#E6E1E5", "#49454F"),
                typescaleHeadline = 24,
                typescaleBody   = 16,
                typescaleCaption = 12,
                cornerRadius    = 12,
                spacingUnit     = 8,
                componentPatterns = listOf("bottom-nav", "card"),
                brandVoice      = "${screen.brief.mood}, clear, confident",
                screenCount     = 1
            )
        }
    }

    private suspend fun saveDnaToFirestore(dna: DesignDna) {
        try {
            firestore.collection(COLLECTION_DNA).document(dna.projectId).set(mapOf(
                "primaryColor"      to dna.primaryColor,
                "secondaryColor"    to dna.secondaryColor,
                "neutralPalette"    to dna.neutralPalette,
                "typescaleHeadline" to dna.typescaleHeadline,
                "typescaleBody"     to dna.typescaleBody,
                "typescaleCaption"  to dna.typescaleCaption,
                "cornerRadius"      to dna.cornerRadius,
                "spacingUnit"       to dna.spacingUnit,
                "componentPatterns" to dna.componentPatterns,
                "brandVoice"        to dna.brandVoice,
                "screenCount"       to dna.screenCount,
                "lastUpdated"       to Timestamp.now()
            )).await()
        } catch (e: Exception) {
            Log.w(TAG, "saveDna failed: ${e.message}")
        }
    }

    private fun dominantStyle(signals: Map<String, Long>): String {
        return signals.entries.maxByOrNull { it.value }?.key ?: "balanced"
    }

    // Internal deserialization
    private data class DnaResponse(
        val primaryColor: String?,
        val secondaryColor: String?,
        val neutralPalette: List<String>?,
        val typescaleHeadline: Int?,
        val typescaleBody: Int?,
        val typescaleCaption: Int?,
        val cornerRadius: Int?,
        val spacingUnit: Int?,
        val componentPatterns: List<String>?,
        val brandVoice: String?
    )
}
