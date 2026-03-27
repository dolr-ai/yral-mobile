package com.yral.shared.core.videostate

import androidx.compose.animation.core.LinearOutSlowInEasing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Suppress("TooManyFunctions")
object VideoGenerationTracker {
    const val ANIMATION_DURATION_MILLIS = 60_000L
    const val ANIMATION_MAX_PROGRESS = 0.95f
    private const val INVERSE_PROGRESS_BINARY_SEARCH_ITERATIONS = 24

    data class PendingGeneration(
        val id: Long,
        val progress: Float = 0f,
    )

    data class State(
        val pendingGenerations: List<PendingGeneration> = emptyList(),
    ) {
        val isGenerating: Boolean
            get() = pendingGenerations.isNotEmpty()
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _selectDraftsTab = MutableStateFlow(false)
    val selectDraftsTab: StateFlow<Boolean> = _selectDraftsTab.asStateFlow()
    private var nextPendingGenerationId = 0L

    fun requestDraftsTab() {
        _selectDraftsTab.value = true
    }

    fun onDraftCreatedAndRequestDraftsTab() {
        onDraftCreated()
        requestDraftsTab()
    }

    fun consumeDraftsTabRequest() {
        _selectDraftsTab.value = false
    }

    fun startGenerating() {
        val pendingGenerationId = nextPendingGenerationId++
        _state.update {
            it.copy(
                pendingGenerations =
                    it.pendingGenerations +
                        PendingGeneration(
                            id = pendingGenerationId,
                        ),
            )
        }
    }

    fun updateProgress(
        pendingGenerationId: Long,
        progress: Float,
    ) {
        val clampedProgress = progress.coerceIn(0f, 1f)
        _state.update { state ->
            val pendingGenerationIndex =
                state.pendingGenerations.indexOfFirst { it.id == pendingGenerationId }
            if (pendingGenerationIndex < 0) {
                state
            } else {
                state.copy(
                    pendingGenerations =
                        state.pendingGenerations.toMutableList().apply {
                            this[pendingGenerationIndex] =
                                this[pendingGenerationIndex].copy(progress = clampedProgress)
                        },
                )
            }
        }
    }

    fun stopGenerating() {
        _state.update { state ->
            state.copy(
                pendingGenerations = state.pendingGenerations.dropLast(1),
            )
        }
    }

    fun onDraftCreated() {
        _state.update { state ->
            state.copy(
                pendingGenerations = state.pendingGenerations.drop(1),
            )
        }
    }

    fun clearPendingGenerations() {
        _state.value = State()
    }

    fun reset() {
        clearPendingGenerations()
        consumeDraftsTabRequest()
    }

    fun generatingProgressTargetForElapsed(elapsedMillis: Long): Float {
        val fraction = (elapsedMillis.toFloat() / ANIMATION_DURATION_MILLIS).coerceIn(0f, 1f)
        return LinearOutSlowInEasing.transform(fraction) * ANIMATION_MAX_PROGRESS
    }

    fun displayedGeneratingProgress(
        animatedProgress: Float,
        reportedProgress: Float,
    ): Float = animatedProgress.coerceAtLeast(reportedProgress).coerceIn(0f, ANIMATION_MAX_PROGRESS)

    fun elapsedMillisForProgress(progress: Float): Long {
        val clampedProgress = progress.coerceIn(0f, ANIMATION_MAX_PROGRESS)
        return when {
            clampedProgress <= 0f -> 0L
            clampedProgress >= ANIMATION_MAX_PROGRESS -> ANIMATION_DURATION_MILLIS
            else -> {
                var low = 0f
                var high = 1f
                repeat(INVERSE_PROGRESS_BINARY_SEARCH_ITERATIONS) {
                    val mid = (low + high) / 2f
                    val midProgress = LinearOutSlowInEasing.transform(mid) * ANIMATION_MAX_PROGRESS
                    if (midProgress < clampedProgress) {
                        low = mid
                    } else {
                        high = mid
                    }
                }
                (((low + high) / 2f) * ANIMATION_DURATION_MILLIS).toLong()
            }
        }
    }
}
