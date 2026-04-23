package com.forge.vdesign.ui.canvas

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.forge.vdesign.databinding.ActivityScreenPreviewBinding
import com.forge.vdesign.domain.model.GeneratedScreen
import kotlinx.coroutines.*

/**
 * ScreenPreviewActivity — full-screen in-app viewer for a Stitch-generated screen.
 *
 * Prefers HTML (WebView) and falls back to screenshotUrl (ImageView).
 * User never leaves FORGE.
 */
class ScreenPreviewActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_SCREEN = "EXTRA_SCREEN"
        private const val EXTRA_SIMPLE_NAME = "EXTRA_SIMPLE_NAME"
        private const val EXTRA_SIMPLE_HTML = "EXTRA_SIMPLE_HTML"
        private const val EXTRA_SIMPLE_IMG = "EXTRA_SIMPLE_IMG"

        fun launch(context: Context, screen: GeneratedScreen) {
            val intent = Intent(context, ScreenPreviewActivity::class.java).apply {
                putExtra(EXTRA_SCREEN, screen)
            }
            context.startActivity(intent)
        }

        fun launchSimple(context: Context, name: String, htmlUrl: String?, screenshotUrl: String?) {
            val intent = Intent(context, ScreenPreviewActivity::class.java).apply {
                putExtra(EXTRA_SIMPLE_NAME, name)
                putExtra(EXTRA_SIMPLE_HTML, htmlUrl)
                putExtra(EXTRA_SIMPLE_IMG, screenshotUrl)
            }
            context.startActivity(intent)
        }
    }

    private lateinit var binding: ActivityScreenPreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScreenPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.previewToolbar.setNavigationOnClickListener { finish() }

        val screen: GeneratedScreen? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_SCREEN, GeneratedScreen::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getParcelableExtra(EXTRA_SCREEN)
        }

        val name = screen?.screenName ?: intent.getStringExtra(EXTRA_SIMPLE_NAME)
        val htmlUrl = screen?.htmlUrl ?: intent.getStringExtra(EXTRA_SIMPLE_HTML)
        val screenshotUrl = screen?.screenshotUrl ?: intent.getStringExtra(EXTRA_SIMPLE_IMG)
        
        if (name == null) { finish(); return }

        binding.previewToolbar.title = name

        when {
            !htmlUrl.isNullOrBlank() -> {
                // If we also have a screenshot, keep it around as a fallback just in case
                if (!screenshotUrl.isNullOrBlank()) {
                    binding.btnOpenInteractive?.visibility = View.VISIBLE
                    binding.btnOpenInteractive?.text = "Show Screenshot Instead"
                    binding.btnOpenInteractive?.setOnClickListener {
                        binding.previewWebView.visibility = View.GONE
                        binding.btnOpenInteractive?.visibility = View.GONE
                        loadImage(screenshotUrl)
                    }
                }
                loadWebView(htmlUrl)
            }
            !screenshotUrl.isNullOrBlank() -> loadImage(screenshotUrl)
            else -> {
                binding.previewEmpty.visibility = View.VISIBLE
                Toast.makeText(this, "No preview URL yet — still generating", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadWebView(url: String) {
        binding.previewWebView.apply {
            visibility = View.VISIBLE
            webViewClient = WebViewClient()
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = true
                displayZoomControls = false
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
        }
        
        // The HTML URL returned by Stitch is often a direct file payload or attachment. 
        // We must fetch the string directly and render it so it doesn't trigger Android's DownloadManager blank screen.
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val connection = java.net.URL(url).openConnection()
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                val stream = connection.getInputStream()
                val htmlContent = stream.bufferedReader().use { it.readText() }
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    binding.previewWebView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                }
            } catch (e: Exception) {
                android.util.Log.e("WebView", "Failed to fetch HTML string", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    binding.previewWebView.loadUrl(url) // Fallback if fetch fails
                }
            }
        }
    }

    private fun loadImage(url: String) {
        var highResUrl = url
        // Google Cloud Storage / Google User Content handles dynamic resizing flags natively
        if (highResUrl.contains("googleusercontent.com")) {
            if (highResUrl.lastIndexOf('=') > highResUrl.lastIndexOf('/')) {
                highResUrl = highResUrl.substring(0, highResUrl.lastIndexOf('=')) + "=s1080"
            } else if (!highResUrl.contains("=")) {
                highResUrl += "=s1080"
            }
        }
        
        binding.previewImage.visibility = View.VISIBLE
        Glide.with(this).load(highResUrl).into(binding.previewImage)
    }

    override fun onDestroy() {
        binding.previewWebView.destroy()
        super.onDestroy()
    }
}
