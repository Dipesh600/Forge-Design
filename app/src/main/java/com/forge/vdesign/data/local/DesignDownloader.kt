package com.forge.vdesign.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DesignDownloader
 *
 * Downloads Stitch-generated HTML files to the local `.stitch/designs/` directory.
 * Stored HTML is the design reference — viewable offline and shareable.
 *
 * Storage path: [filesDir]/.stitch/designs/<screenName>.html
 */
@Singleton
class DesignDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {

    private val designsDir: File by lazy {
        File(context.filesDir, ".stitch/designs").also { it.mkdirs() }
    }

    /**
     * Download HTML from [url] and save as [screenName].html
     * @return local file path on success, null on failure
     */
    suspend fun downloadHtml(url: String, screenName: String): String? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null

        val safeFileName = screenName.replace(Regex("[^a-zA-Z0-9_-]"), "_") + ".html"
        val targetFile = File(designsDir, safeFileName)

        return@withContext try {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                android.util.Log.w("DesignDownloader", "Download failed for $screenName: ${response.code}")
                return@withContext null
            }

            response.body?.byteStream()?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            android.util.Log.d("DesignDownloader", "Saved: ${targetFile.absolutePath}")
            targetFile.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("DesignDownloader", "Failed to download $screenName", e)
            null
        }
    }

    /** List all downloaded design HTML files */
    fun listDownloaded(): List<File> = designsDir.listFiles()
        ?.filter { it.extension == "html" }
        ?.sortedByDescending { it.lastModified() }
        ?: emptyList()

    fun designsDirectory(): File = designsDir
}
