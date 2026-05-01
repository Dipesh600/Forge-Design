package com.forge.vdesign.mcp

import com.forge.vdesign.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * StitchMcpClient
 *
 * Calls the FORGE Stitch server on Render directly via OkHttp.
 * - 3-minute read timeout (Stitch screen generation takes 60-90 seconds)
 * - No Firebase Functions overhead
 * - Simple POST /stitch with X-Forge-Key auth header
 *
 * Set STITCH_SERVER_URL and STITCH_API_KEY in BuildConfig or replace the constants below.
 */
@Singleton
class StitchMcpClient @Inject constructor(
    private val gson: Gson
) {
    companion object {
        // ── Set this to your Render URL after deployment ────────────────────
        // Example: "https://forge-stitch.onrender.com"
        private const val SERVER_URL = BuildConfig.STITCH_SERVER_URL

        // ── This must match FORGE_API_KEY on your Render server ─────────────
        private const val API_KEY = BuildConfig.STITCH_API_KEY
    }

    // 3-minute timeout — Stitch screen generation takes 60-90 seconds
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.MINUTES)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    /** Create a new Stitch project, returns the projectId */
    suspend fun createProject(title: String): StitchProjectResult = withContext(Dispatchers.IO) {
        val body = mapOf("tool" to "create_project", "title" to title)
        val json = post(body)
        android.util.Log.d("StitchMcpClient", "createProject: $json")
        gson.fromJson(json, StitchProjectResult::class.java)
    }

    /** Generate a brand new screen from a text prompt */
    suspend fun generateScreen(
        projectId: String,
        prompt: String,
        deviceType: String = "MOBILE"
    ): StitchScreenResult = withContext(Dispatchers.IO) {
        val body = mapOf(
            "tool"       to "generate_screen_from_text",
            "projectId"  to projectId,
            "prompt"     to prompt,
            "deviceType" to deviceType
        )
        val json = post(body)
        android.util.Log.d("StitchMcpClient", "generate_screen_from_text: $json")
        gson.fromJson(json, StitchScreenResult::class.java)
    }

    /** Edit one or more existing screens with a targeted instruction */
    suspend fun editScreens(
        projectId: String,
        screenIds: List<String>,
        editInstruction: String
    ): StitchScreenResult = withContext(Dispatchers.IO) {
        val body = mapOf(
            "tool"              to "edit_screens",
            "projectId"         to projectId,
            "selectedScreenIds" to screenIds,
            "prompt"            to editInstruction
        )
        val json = post(body)
        gson.fromJson(json, StitchScreenResult::class.java)
    }

    /** List all screens in a project */
    suspend fun listScreens(projectId: String): List<StitchScreenResult> =
        withContext(Dispatchers.IO) {
            val body = mapOf("tool" to "list_screens", "projectId" to projectId)
            val json = post(body)
            val wrapper = gson.fromJson(json, ListScreensWrapper::class.java)
            wrapper.screens ?: emptyList()
        }

    /** Create a design system */
    suspend fun createDesignSystem(
        projectId: String,
        designSystem: Map<String, Any>
    ): StitchDesignSystemResult = withContext(Dispatchers.IO) {
        val body = mapOf(
            "tool" to "create_design_system",
            "projectId" to projectId,
            "designSystem" to designSystem
        )
        val json = post(body)
        gson.fromJson(json, StitchDesignSystemResult::class.java)
    }

    /** Apply a design system */
    suspend fun applyDesignSystem(
        projectId: String,
        screenIds: List<String>,
        assetId: String
    ): StitchScreenResult = withContext(Dispatchers.IO) {
        val body = mapOf(
            "tool" to "apply_design_system",
            "projectId" to projectId,
            "selectedScreenInstances" to screenIds,
            "assetId" to assetId
        )
        val json = post(body)
        gson.fromJson(json, StitchScreenResult::class.java)
    }

    /** Generate variants */
    suspend fun generateVariants(
        projectId: String,
        screenIds: List<String>,
        prompt: String,
        variantOptions: Map<String, Any>,
        deviceType: String = "MOBILE"
    ): StitchScreenResult = withContext(Dispatchers.IO) {
        val body = mapOf(
            "tool" to "generate_variants",
            "projectId" to projectId,
            "selectedScreenIds" to screenIds,
            "prompt" to prompt,
            "variantOptions" to variantOptions,
            "deviceType" to deviceType
        )
        val json = post(body)
        gson.fromJson(json, StitchScreenResult::class.java)
    }

    /** List projects */
    suspend fun listProjects(filter: String? = null): List<StitchProjectResult> =
        withContext(Dispatchers.IO) {
            val body = mutableMapOf<String, Any?>("tool" to "list_projects")
            if (filter != null) body["filter"] = filter
            val json = post(body)
            val wrapper = gson.fromJson(json, ListProjectsWrapper::class.java)
            wrapper.projects ?: emptyList()
        }

    /** Get screen metadata */
    suspend fun getScreen(name: String, projectId: String, screenId: String): StitchScreenResult =
        withContext(Dispatchers.IO) {
            val body = mapOf(
                "tool" to "get_screen",
                "name" to name,
                "projectId" to projectId,
                "screenId" to screenId
            )
            val json = post(body)
            gson.fromJson(json, StitchScreenResult::class.java)
        }

    // ── HTTP helper ───────────────────────────────────────────────────────────

    private fun post(body: Map<String, Any?>): String {
        val requestBody = gson.toJson(body).toRequestBody(JSON_MEDIA)
        val request = Request.Builder()
            .url("$SERVER_URL/stitch")
            .post(requestBody)
            .addHeader("X-Forge-Key", API_KEY)
            .addHeader("Content-Type", "application/json")
            .build()

        return try {
            http.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: "{}"
                android.util.Log.d("StitchMcpClient", "HTTP ${response.code}: ${responseBody.take(200)}")
                if (!response.isSuccessful) {
                    // Return error JSON so Gson can parse it into the result model
                    """{"error":"Stitch server ${response.code}: ${responseBody.take(100)}"}"""
                } else {
                    responseBody
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("StitchMcpClient", "HTTP call failed: ${e.message}", e)
            """{"error":"${e.message?.replace("\"", "'")}"}"""
        }
    }

    private data class ListScreensWrapper(val screens: List<StitchScreenResult>? = null)
    private data class ListProjectsWrapper(val projects: List<StitchProjectResult>? = null)
}

// ─── Result models ────────────────────────────────────────────────────────────

data class StitchScreenResult(
    val screenId: String? = null,
    val projectId: String? = null,
    val htmlUrl: String? = null,
    val screenshotUrl: String? = null,
    val description: String? = null,
    val suggestions: List<String>? = null,
    val error: String? = null
) {
    val isSuccess: Boolean get() = error == null && (htmlUrl != null || screenshotUrl != null)
}

data class StitchProjectResult(
    val projectId: String? = null,
    val title: String? = null,
    val error: String? = null
) {
    val isSuccess: Boolean get() = error == null && projectId != null
}

data class StitchDesignSystemResult(
    val assetId: String? = null,
    val projectId: String? = null,
    val error: String? = null
) {
    val isSuccess: Boolean get() = error == null && assetId != null
}
