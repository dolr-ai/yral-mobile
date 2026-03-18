package com.yral.shared.core.videostate

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object VideoGenerationTracker {
    data class State(
        val isGenerating: Boolean = false,
        val progress: Float = 0f,
        val draftVideoIds: Set<String> = emptySet(),
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _selectDraftsTab = MutableStateFlow(false)
    val selectDraftsTab: StateFlow<Boolean> = _selectDraftsTab.asStateFlow()

    fun requestDraftsTab() {
        _selectDraftsTab.value = true
    }

    fun consumeDraftsTabRequest() {
        _selectDraftsTab.value = false
    }

    fun startGenerating() {
        _state.update { it.copy(isGenerating = true, progress = 0f) }
    }

    fun updateProgress(progress: Float) {
        _state.update { it.copy(progress = progress.coerceIn(0f, 1f)) }
    }

    fun stopGenerating() {
        _state.update { it.copy(isGenerating = false, progress = 0f) }
    }

    fun markAsDraft(videoId: String) {
        _state.update { it.copy(draftVideoIds = it.draftVideoIds + videoId) }
    }

    fun clearDraft(videoId: String) {
        _state.update { it.copy(draftVideoIds = it.draftVideoIds - videoId) }
    }
}
