package com.yral.shared.features.coach.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.features.chat.domain.models.SystemPromptPreview
import com.yral.shared.features.chat.domain.usecases.GetSystemPromptPreviewUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Coach pivot Bucket 2 (pivoted 2026-06-12) — drives the read-only "View
 * full prompt" page. Always fetches fresh on entry; never caches. There
 * is no edit state — all changes to the bot personality happen via Coach.
 */
class SoulFileViewModel(
    private val getSystemPromptPreviewUseCase: GetSystemPromptPreviewUseCase,
) : ViewModel() {
    private val _viewState = MutableStateFlow(SoulFileViewState())
    val viewState: StateFlow<SoulFileViewState> = _viewState.asStateFlow()

    fun openForBot(botId: String) {
        _viewState.update {
            it.copy(
                botId = botId,
                isLoading = true,
                preview = null,
                error = null,
            )
        }
        load(botId)
    }

    fun retry() {
        val botId = _viewState.value.botId ?: return
        _viewState.update { it.copy(isLoading = true, error = null) }
        load(botId)
    }

    private fun load(botId: String) {
        viewModelScope.launch {
            getSystemPromptPreviewUseCase(GetSystemPromptPreviewUseCase.Params(botId = botId))
                .onSuccess { preview ->
                    _viewState.update {
                        if (it.botId != botId) {
                            it
                        } else {
                            it.copy(preview = preview, isLoading = false, error = null)
                        }
                    }
                }.onFailure { error ->
                    Logger.e(error) { "SystemPromptPreview load failed botId=$botId" }
                    _viewState.update {
                        if (it.botId != botId) {
                            it
                        } else {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Could not load the full prompt",
                            )
                        }
                    }
                }
        }
    }

    fun clearError() {
        _viewState.update { it.copy(error = null) }
    }
}

data class SoulFileViewState(
    val botId: String? = null,
    val isLoading: Boolean = false,
    val preview: SystemPromptPreview? = null,
    val error: String? = null,
)
