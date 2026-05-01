package com.forge.vdesign.ui.canvas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forge.vdesign.agents.MotionDirectorAgent
import com.forge.vdesign.agents.VariantExplorerAgent
import com.forge.vdesign.brain.AgentLogEntry
import com.forge.vdesign.brain.BrainOrchestrator
import com.forge.vdesign.brain.OrchestrationResult
import com.forge.vdesign.domain.model.DesignBrief
import com.forge.vdesign.domain.model.GeneratedScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * CanvasViewModel
 *
 * Owns UI state for [ScreenCanvasActivity].
 * Supports both single-screen and multi-screen (generateDesignSet) flows.
 *
 * Design set flow:
 *   generateDesignSet(brief) → collects generateScreenSet() Flow
 *   Each screen emitted adds to [screens] and the tab bar updates live.
 *
 * Agent log:
 *   BrainOrchestrator emits AgentLogEntry callbacks → [agentLog] StateFlow
 *   Canvas shows them as a live scrolling panel.
 */
@HiltViewModel
class CanvasViewModel @Inject constructor(
    private val brainOrchestrator: BrainOrchestrator,
    private val variantExplorerAgent: VariantExplorerAgent,
    private val motionDirectorAgent: MotionDirectorAgent
) : ViewModel() {

    private val _uiState = MutableStateFlow<CanvasUiState>(CanvasUiState.Idle)
    val uiState: StateFlow<CanvasUiState> = _uiState

    private val _agentLog = MutableStateFlow<List<AgentLogEntry>>(emptyList())
    val agentLog: StateFlow<List<AgentLogEntry>> = _agentLog

    private val _screens = MutableStateFlow<List<GeneratedScreen>>(emptyList())
    val screens: StateFlow<List<GeneratedScreen>> = _screens

    private val _activeScreenIndex = MutableStateFlow(0)
    val activeScreenIndex: StateFlow<Int> = _activeScreenIndex

    // Variant exploration results
    private val _variantSet = MutableStateFlow<VariantExplorerAgent.VariantSet?>(null)
    val variantSet: StateFlow<VariantExplorerAgent.VariantSet?> = _variantSet

    // Motion spec for the active screen
    private val _motionSpec = MutableStateFlow<MotionDirectorAgent.MotionSpec?>(null)
    val motionSpec: StateFlow<MotionDirectorAgent.MotionSpec?> = _motionSpec

    // Kept for single-screen re-generation
    private var lastPrompt = ""
    private var lastBrief: DesignBrief? = null

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Primary entry point: generate the full set of screens from a DesignBrief.
     * Called when the canvas opens from a completed consultative chat session.
     */
    fun generateDesignSet(brief: DesignBrief) {
        lastBrief = brief
        _uiState.value = CanvasUiState.Generating
        _screens.value = emptyList()
        _agentLog.value = emptyList()
        _activeScreenIndex.value = 0

        viewModelScope.launch {
            brainOrchestrator.generateScreenSet(brief, onLog = ::appendLog)
                .catch { e ->
                    appendLog(AgentLogEntry("BrainOrchestrator", "Error: ${e.message}", com.forge.vdesign.brain.LogStatus.FAILED))
                    if (_screens.value.isEmpty()) {
                        _uiState.value = CanvasUiState.Error(e.message ?: "Generation failed")
                    }
                }
                .collect { result ->
                    when (result) {
                        is OrchestrationResult.Success -> {
                            val screen = result.screen
                            val updated = _screens.value + screen
                            _screens.value = updated
                            // Switch active tab to the newest screen
                            _activeScreenIndex.value = updated.size - 1
                            // Transition to Success after first screen arrives
                            _uiState.value = CanvasUiState.Success
                        }
                        is OrchestrationResult.Failure -> {
                            appendLog(AgentLogEntry("BrainOrchestrator", "Failed: ${result.error}", com.forge.vdesign.brain.LogStatus.FAILED))
                            if (_screens.value.isEmpty()) {
                                _uiState.value = CanvasUiState.Error(result.error)
                            }
                        }
                    }
                }
        }
    }

    /** Single-screen generation from a raw prompt (fallback / simple mode) */
    fun generateScreen(prompt: String) {
        lastPrompt = prompt
        _uiState.value = CanvasUiState.Generating
        _screens.value = emptyList()
        _agentLog.value = emptyList()

        viewModelScope.launch {
            when (val result = brainOrchestrator.generateScreen(prompt, onLog = ::appendLog)) {
                is OrchestrationResult.Success -> {
                    _screens.value = listOf(result.screen)
                    lastBrief = result.screen.brief
                    _uiState.value = CanvasUiState.Success
                }
                is OrchestrationResult.Failure -> {
                    _uiState.value = CanvasUiState.Error(result.error)
                }
            }
        }
    }

    /** Display an already-generated screen (passed via Intent) */
    fun displayScreen(screen: GeneratedScreen) {
        _screens.value = listOf(screen)
        lastBrief = screen.brief
        _uiState.value = CanvasUiState.Success
    }

    /** Switch active screen tab */
    fun selectScreen(index: Int) {
        if (index in _screens.value.indices) {
            _activeScreenIndex.value = index
        }
    }

    /** Regenerate the currently active screen */
    fun regenerateActive() {
        val screen = _screens.value.getOrNull(_activeScreenIndex.value) ?: return
        val brief = lastBrief ?: return
        _uiState.value = CanvasUiState.Generating

        viewModelScope.launch {
            when (val result = brainOrchestrator.regenerate(
                screenId = screen.screenId,
                brief = brief,
                screenName = screen.screenName,
                onLog = ::appendLog
            )) {
                is OrchestrationResult.Success -> {
                    val updated = _screens.value.toMutableList()
                    updated[_activeScreenIndex.value] = result.screen
                    _screens.value = updated
                    _uiState.value = CanvasUiState.Success
                }
                is OrchestrationResult.Failure -> {
                    _uiState.value = CanvasUiState.Error(result.error)
                }
            }
        }
    }

    fun retry() {
        val brief = lastBrief
        if (brief != null) generateDesignSet(brief) else generateScreen(lastPrompt)
    }

    // ── Phase 4 agents ────────────────────────────────────────────────────────

    fun exploreVariants() {
        val screen = _screens.value.getOrNull(_activeScreenIndex.value) ?: return
        val brief = lastBrief ?: return

        viewModelScope.launch {
            appendLog(AgentLogEntry("VariantExplorer", "Generating 3 variants for ${screen.screenName}…", com.forge.vdesign.brain.LogStatus.RUNNING))
            val set = variantExplorerAgent.exploreVariants(
                brief       = brief,
                screenId    = screen.screenId,
                screenName  = screen.screenName,
                baseDescription = screen.description
            )
            _variantSet.value = set
            val newScreens = _screens.value.toMutableList()
            newScreens.addAll(set.variants.mapNotNull { v ->
                val res = v.stitchResult ?: return@mapNotNull null
                com.forge.vdesign.domain.model.GeneratedScreen(
                    screenId = res.screenId ?: "",
                    screenName = v.displayName,
                    htmlUrl = res.htmlUrl,
                    screenshotUrl = res.screenshotUrl,
                    description = v.tradeoffExplanation,
                    brief = brief
                )
            })
            _screens.value = newScreens
            appendLog(AgentLogEntry("VariantExplorer", "${set.variants.size} variants ready ✓", com.forge.vdesign.brain.LogStatus.DONE))
        }
    }

    fun addMotion() {
        val screen = _screens.value.getOrNull(_activeScreenIndex.value) ?: return
        viewModelScope.launch {
            appendLog(AgentLogEntry("MotionDirector", "Generating motion spec for ${screen.screenName}…", com.forge.vdesign.brain.LogStatus.RUNNING))
            val spec = motionDirectorAgent.generateMotionSpec(
                screenName  = screen.screenName,
                description = screen.description
            )
            _motionSpec.value = spec
            appendLog(AgentLogEntry("MotionDirector", "${spec.transitions.size} transitions ready ✓", com.forge.vdesign.brain.LogStatus.DONE))
        }
    }

    fun exportScreen() {
        val screen = _screens.value.getOrNull(_activeScreenIndex.value)
        android.util.Log.d("CanvasVM", "Export ${screen?.screenName} — Phase 5")
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun appendLog(entry: AgentLogEntry) {
        _agentLog.value = _agentLog.value + entry
    }
}

// ─── UI State ────────────────────────────────────────────────────────────────

sealed class CanvasUiState {
    object Idle       : CanvasUiState()
    object Generating : CanvasUiState()
    object Success    : CanvasUiState()
    data class Error(val message: String) : CanvasUiState()
}
