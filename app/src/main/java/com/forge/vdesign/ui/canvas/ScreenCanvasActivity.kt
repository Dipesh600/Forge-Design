package com.forge.vdesign.ui.canvas

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.forge.vdesign.R
import com.forge.vdesign.databinding.ActivityScreenCanvasBinding
import com.forge.vdesign.domain.model.DesignBrief
import com.forge.vdesign.domain.model.GeneratedScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * ScreenCanvasActivity — FORGE's clean screen canvas.
 *
 * Shows all generated Stitch screens as horizontal scrollable thumbnails.
 * Tap any thumbnail → opens full-screen preview (WebView or screenshot).
 * Nothing else. No skill cards, no critic panels, no clutter.
 */
@AndroidEntryPoint
class ScreenCanvasActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_GENERATED_SCREEN = "EXTRA_GENERATED_SCREEN"
        const val EXTRA_DESIGN_BRIEF     = "EXTRA_DESIGN_BRIEF"
        const val EXTRA_PROMPT           = "EXTRA_DESIGN_BRIEF_PROMPT"
    }

    private lateinit var binding: ActivityScreenCanvasBinding
    private val viewModel: CanvasViewModel by viewModels()
    private lateinit var screenAdapter: ScreenThumbnailAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScreenCanvasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Shared element transition name
        ViewCompat.setTransitionName(binding.canvasRoot, "canvas_handoff")

        setupEdgeToEdge()
        setupToolbar()
        setupRecyclerView()
        setupActions()
        observeViewModel()
        startGeneration()
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.canvasRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerView() {
        screenAdapter = ScreenThumbnailAdapter { screen, _ ->
            // Tap → open in-app WebView preview (never leaves FORGE)
            ScreenPreviewActivity.launch(this, screen)
        }
        binding.rvScreens.apply {
            adapter = screenAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
                this@ScreenCanvasActivity,
                RecyclerView.HORIZONTAL,
                false
            )
        }
    }

    private fun setupActions() {
        binding.btnRegenerate.setOnClickListener { viewModel.regenerateActive() }
        binding.btnExploreVariants.setOnClickListener { viewModel.exploreVariants() }
        // "Open in Stitch" → show first screen in-app full screen
        binding.btnOpenInStitch.setOnClickListener {
            val first = viewModel.screens.value.firstOrNull()
            if (first != null) ScreenPreviewActivity.launch(this, first)
        }
    }

    private fun startGeneration() {
        val preGenerated: GeneratedScreen? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_GENERATED_SCREEN, GeneratedScreen::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getParcelableExtra(EXTRA_GENERATED_SCREEN)
        }

        val brief: DesignBrief? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_DESIGN_BRIEF, DesignBrief::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getParcelableExtra(EXTRA_DESIGN_BRIEF)
        }

        when {
            preGenerated != null -> viewModel.displayScreen(preGenerated)
            brief != null        -> viewModel.generateDesignSet(brief)
            else -> {
                val prompt = intent.getStringExtra(EXTRA_PROMPT) ?: ""
                if (prompt.isNotBlank()) viewModel.generateScreen(prompt)
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is CanvasUiState.Idle       -> showLoading()   // Idle = about to generate
                    is CanvasUiState.Generating -> showLoading()
                    is CanvasUiState.Success    -> showSuccess()
                    is CanvasUiState.Error      -> showError(state.message)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.screens.collect { screens ->
                screenAdapter.submitList(screens.toList())
                if (screens.isNotEmpty()) {
                    binding.tvProjectName.text = screens.first().screenName
                        .split(" ").dropLast(1).joinToString(" ")
                        .ifBlank { "FORGE Canvas" }
                    binding.tvScreenCount.text = "${screens.size} screen${if (screens.size != 1) "s" else ""}"
                }
                // As each screen comes in, switch to success if still loading
                if (screens.isNotEmpty() && viewModel.uiState.value is CanvasUiState.Generating) {
                    showSuccess()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.agentLog.collect { log ->
                val last = log.lastOrNull() ?: return@collect
                binding.tvGeneratingStatus.text = "${last.agent}: ${last.action}"
            }
        }
    }

    private fun showLoading() {
        binding.loadingState.visibility = View.VISIBLE
        binding.successState.visibility = View.GONE
        binding.errorState.visibility   = View.GONE
        binding.progressBar.visibility  = View.VISIBLE
    }

    private fun showSuccess() {
        binding.loadingState.visibility = View.GONE
        binding.successState.visibility = View.VISIBLE
        binding.errorState.visibility   = View.GONE
        binding.progressBar.visibility  = View.GONE
    }

    private fun showError(message: String) {
        binding.loadingState.visibility = View.GONE
        binding.successState.visibility = View.GONE
        binding.errorState.visibility   = View.VISIBLE
        binding.progressBar.visibility  = View.GONE
        binding.tvErrorMessage.text     = message
        binding.btnRetry.setOnClickListener { viewModel.retry() }
    }
}

// ── Screen thumbnail adapter ──────────────────────────────────────────────────

class ScreenThumbnailAdapter(
    private val onClick: (GeneratedScreen, Int) -> Unit
) : RecyclerView.Adapter<ScreenThumbnailAdapter.VH>() {

    private val items = mutableListOf<GeneratedScreen>()

    fun submitList(list: List<GeneratedScreen>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_screen_thumbnail, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], position + 1)
        holder.itemView.setOnClickListener { onClick(items[position], position) }
    }

    override fun getItemCount() = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val img: ImageView   = v.findViewById(R.id.imgScreenshot)
        private val shimmer: View    = v.findViewById(R.id.shimmerThumb)
        private val tvName: TextView  = v.findViewById(R.id.tvScreenName)
        private val tvStatus: TextView = v.findViewById(R.id.tvScreenStatus)
        private val tvNum: TextView   = v.findViewById(R.id.tvScreenNumber)

        fun bind(screen: GeneratedScreen, number: Int) {
            tvNum.text   = number.toString()
            tvName.text  = screen.screenName
            tvStatus.text = when {
                screen.screenshotUrl != null || screen.htmlUrl != null -> "Generated ✓"
                else -> "Generating…"
            }

            val imageUrl = screen.screenshotUrl ?: screen.htmlUrl
            if (imageUrl != null) {
                shimmer.visibility = View.GONE
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.color.shimmer_base)
                    .into(img)
            } else {
                shimmer.visibility = View.VISIBLE
            }
        }
    }
}
