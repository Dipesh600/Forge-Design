package com.forge.vdesign.skills

import com.forge.vdesign.skills.models.Skill
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SkillRepository
 *
 * Single source of truth for design skills.
 *
 * Merge strategy: ASSET SKILLS FIRST (always available), then Firestore overrides by ID.
 * This means:
 *   - App works offline with 9 bundled skills from day one
 *   - When Firestore skills are ingested (via structure_skill.py), they are returned
 *     alongside+overriding asset skills for richer rule sets
 *   - Zero blocking dependency on cloud for the base skill set
 */
@Singleton
class SkillRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val assetLoader: SkillAssetLoader
) {

    /**
     * Observe the skills collection for real-time updates (Firestore + assets merged).
     */
    fun observeSkills(): Flow<List<Skill>> = callbackFlow {
        val subscription = firestore.collection("skills")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.w("SkillRepository", "Firestore observe error: ${error.message}")
                    // Fall back to assets — don't close the flow
                    trySend(assetLoader.loadAll())
                    return@addSnapshotListener
                }

                val firestoreSkills = snapshot?.documents
                    ?.mapNotNull { it.toObject(Skill::class.java) }
                    ?: emptyList()

                trySend(merge(assetLoader.loadAll(), firestoreSkills))
            }

        awaitClose { subscription.remove() }
    }

    /**
     * Fetch all skills synchronously (used by SkillRouter in the generation pipeline).
     * Returns asset skills immediately + Firestore skills if reachable.
     */
    suspend fun getAllSkills(): List<Skill> {
        val assetSkills = assetLoader.loadAll()

        val firestoreSkills = try {
            val snapshot = firestore.collection("skills").get().await()
            snapshot.documents.mapNotNull { it.toObject(Skill::class.java) }
        } catch (e: Exception) {
            android.util.Log.w("SkillRepository", "Firestore unavailable, using assets only: ${e.message}")
            emptyList()
        }

        val merged = merge(assetSkills, firestoreSkills)
        android.util.Log.d(
            "SkillRepository",
            "getAllSkills: ${merged.size} total (${assetSkills.size} asset, ${firestoreSkills.size} firestore)"
        )
        return merged
    }

    /**
     * Merge asset skills with Firestore skills.
     * Firestore skills take precedence over asset skills with the same ID.
     * Firestore-only skills are appended after.
     */
    private fun merge(assetSkills: List<Skill>, firestoreSkills: List<Skill>): List<Skill> {
        if (firestoreSkills.isEmpty()) return assetSkills

        val firestoreById = firestoreSkills.associateBy { it.id }
        val merged = assetSkills.map { asset ->
            firestoreById[asset.id] ?: asset // Firestore overrides asset by ID
        }
        // Add any Firestore skills not in assets
        val assetIds = assetSkills.map { it.id }.toSet()
        val extraFirestore = firestoreSkills.filter { it.id !in assetIds }
        return merged + extraFirestore
    }
}
