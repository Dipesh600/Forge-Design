package com.forge.vdesign.skills

import android.content.Context
import com.forge.vdesign.skills.models.Skill
import com.forge.vdesign.skills.models.SkillRule
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SkillAssetLoader
 *
 * Loads bundled design skills from the assets/skills directory at startup.
 * These are the baseline skill set — no Firestore or network dependency.
 *
 * Asset skills are always available. Firestore skills (when ingested) override
 * them by ID via SkillRepository's merge logic.
 *
 * Each json file in assets/skills has the structure:
 *   id, name, category, sourceBooks, embedding (empty for assets), rules[]
 */

@Singleton
class SkillAssetLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var cachedSkills: List<Skill>? = null

    /**
     * Load all bundled skills from assets/skills/.
     * Results are cached in memory after first load.
     */
    fun loadAll(): List<Skill> {
        cachedSkills?.let { return it }

        val skills = mutableListOf<Skill>()
        try {
            val fileNames = context.assets.list("skills") ?: emptyArray()
            for (fileName in fileNames) {
                if (!fileName.endsWith(".json")) continue
                runCatching {
                    val json = context.assets.open("skills/$fileName")
                        .bufferedReader().use { it.readText() }
                    skills.add(parseSkill(json))
                }.onFailure {
                    android.util.Log.w("SkillAssetLoader", "Failed to load skill: $fileName — ${it.message}")
                }
            }
            android.util.Log.d("SkillAssetLoader", "Loaded ${skills.size} bundled skills: ${skills.map { it.id }}")
        } catch (e: Exception) {
            android.util.Log.e("SkillAssetLoader", "Failed to list skills assets", e)
        }

        cachedSkills = skills
        return skills
    }

    private fun parseSkill(json: String): Skill {
        val obj = JSONObject(json)

        val sourceBooks = mutableListOf<String>()
        obj.optJSONArray("sourceBooks")?.let { arr ->
            for (i in 0 until arr.length()) sourceBooks.add(arr.getString(i))
        }

        // Parse keywords — used by SkillRouter for smarter text-based routing
        val keywords = mutableListOf<String>()
        obj.optJSONArray("keywords")?.let { arr ->
            for (i in 0 until arr.length()) keywords.add(arr.getString(i))
        }

        val rules = mutableListOf<SkillRule>()
        obj.optJSONArray("rules")?.let { arr ->
            for (i in 0 until arr.length()) {
                val ruleObj = arr.getJSONObject(i)
                rules.add(
                    SkillRule(
                        id     = ruleObj.optString("id"),
                        rule   = ruleObj.optString("rule"),
                        weight = ruleObj.optDouble("weight", 1.0).toFloat(),
                        why    = ruleObj.optString("why", ""),
                        check  = ruleObj.optString("check", ""),
                        bad    = ruleObj.optString("bad", ""),
                        good   = ruleObj.optString("good", "")
                    )
                )
            }
        }

        return Skill(
            id          = obj.optString("id"),
            name        = obj.optString("name"),
            version     = obj.optString("version", "1.0"),
            category    = obj.optString("category"),
            keywords    = keywords,
            sourceBooks = sourceBooks,
            rules       = rules,
            embedding   = emptyList() // Asset skills use keyword routing, not embeddings
        )
    }
}
