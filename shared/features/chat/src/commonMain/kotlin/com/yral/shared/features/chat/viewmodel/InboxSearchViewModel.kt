package com.yral.shared.features.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.features.chat.domain.models.InboxSearchResult
import com.yral.shared.features.chat.domain.usecases.SearchInboxUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Inbox search ViewModel. Mirrors [DiscoverySearchViewModel] — same
 * debounce/cancel-in-flight/LRU cache UX — but targets the
 * `/api/v2/chat/conversations/search` endpoint and emits
 * [InboxSearchResult] rows. The two VMs are kept separate so each owns
 * its own cache and result type; the shared search bar in
 * `ChatHomeScreen` dispatches keystrokes to whichever VM matches the
 * active tab.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class InboxSearchViewModel(
    private val searchInboxUseCase: SearchInboxUseCase,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val cache = object : LinkedHashMap<String, List<InboxSearchResult>>(CACHE_SIZE, LOAD_FACTOR, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, List<InboxSearchResult>>): Boolean =
            size > CACHE_SIZE
    }

    val state: StateFlow<InboxSearchState> =
        _query
            .map { it.trim() }
            .distinctUntilChanged()
            .debounce { if (cache.containsKey(it.cacheKey())) 0L else DEBOUNCE_MS }
            .flatMapLatest { trimmed ->
                when {
                    trimmed.isEmpty() -> flowOf(InboxSearchState(query = trimmed))
                    cache.containsKey(trimmed.cacheKey()) ->
                        flowOf(
                            InboxSearchState(
                                query = trimmed,
                                results = cache[trimmed.cacheKey()].orEmpty(),
                            ),
                        )
                    else -> searchFlow(trimmed)
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = InboxSearchState(),
            )

    fun setQuery(value: String) {
        _query.value = value
    }

    fun clearQuery() {
        _query.value = ""
    }

    private fun searchFlow(query: String) =
        flow {
            emit(InboxSearchState(query = query, isLoading = true))
            searchInboxUseCase(SearchInboxUseCase.Params(query = query))
                .onSuccess { results ->
                    cache[query.cacheKey()] = results
                    emit(InboxSearchState(query = query, results = results))
                }.onFailure { error ->
                    Logger.e(error) { "InboxSearch failed query=$query" }
                    emit(
                        InboxSearchState(
                            query = query,
                            error = error.message ?: "Search failed",
                        ),
                    )
                }
        }

    private fun String.cacheKey(): String = lowercase()

    private companion object {
        const val DEBOUNCE_MS = 150L
        const val CACHE_SIZE = 10
        const val LOAD_FACTOR = 0.75f
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

data class InboxSearchState(
    val query: String = "",
    val results: List<InboxSearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val isActive: Boolean get() = query.isNotEmpty()
    val isEmptyResult: Boolean
        get() = isActive && !isLoading && error == null && results.isEmpty()
}
