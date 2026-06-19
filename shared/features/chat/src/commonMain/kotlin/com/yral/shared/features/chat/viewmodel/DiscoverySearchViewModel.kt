package com.yral.shared.features.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.shared.features.chat.domain.models.DiscoverySearchResult
import com.yral.shared.features.chat.domain.usecases.SearchDiscoveryUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * Discovery search ViewModel. Debounces user input by [DEBOUNCE_MS] ms,
 * cancels any in-flight request when a new keystroke comes in
 * (`flatMapLatest`), and caches the last [CACHE_SIZE] resolved queries
 * locally for instant repeat-typing or back-spacing into a prior query.
 *
 * Flag-gating is enforced by the UI: when DiscoverySearchEnabled is OFF,
 * the search bar is not rendered and this ViewModel is never collected.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class DiscoverySearchViewModel(
    private val searchDiscoveryUseCase: SearchDiscoveryUseCase,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    // Multiplatform-safe LRU. The `(capacity, loadFactor, accessOrder)`
    // LinkedHashMap constructor + removeEldestEntry override are
    // java.util-only — Kotlin/Native's LinkedHashMap is final and offers
    // neither, so we keep a plain insertion-ordered map and bump recency
    // manually by remove-then-reput on both read and write. Same pattern
    // as ConversationContentCache in this module.
    private val cache = LinkedHashMap<String, List<DiscoverySearchResult>>()

    val state: StateFlow<DiscoverySearchState> =
        _query
            .map { it.trim() }
            .distinctUntilChanged()
            .debounce { if (cache.containsKey(it.cacheKey())) 0L else DEBOUNCE_MS }
            .flatMapLatest { trimmed ->
                when {
                    trimmed.isEmpty() -> flowOf(DiscoverySearchState(query = trimmed))
                    else -> {
                        val cached = cacheGet(trimmed.cacheKey())
                        if (cached != null) {
                            flowOf(DiscoverySearchState(query = trimmed, results = cached))
                        } else {
                            searchFlow(trimmed)
                        }
                    }
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = DiscoverySearchState(),
            )

    fun setQuery(value: String) {
        _query.value = value
    }

    fun clearQuery() {
        _query.value = ""
    }

    private fun searchFlow(query: String) =
        flow {
            emit(DiscoverySearchState(query = query, isLoading = true))
            searchDiscoveryUseCase(SearchDiscoveryUseCase.Params(query = query))
                .onSuccess { results ->
                    cachePut(query.cacheKey(), results)
                    emit(DiscoverySearchState(query = query, results = results))
                }.onFailure { error ->
                    Logger.e(error) { "DiscoverySearch failed query=$query" }
                    emit(
                        DiscoverySearchState(
                            query = query,
                            error = error.message ?: "Search failed",
                        ),
                    )
                }
        }

    /** LRU read — bumps the touched key to the MRU end on hit. */
    private fun cacheGet(key: String): List<DiscoverySearchResult>? {
        val value = cache[key] ?: return null
        cache.remove(key)
        cache[key] = value
        return value
    }

    /** LRU write — re-puts as MRU and prunes from the head while oversize. */
    private fun cachePut(
        key: String,
        value: List<DiscoverySearchResult>,
    ) {
        cache.remove(key)
        cache[key] = value
        while (cache.size > CACHE_SIZE) {
            val eldest = cache.keys.iterator().next()
            cache.remove(eldest)
        }
    }

    private fun String.cacheKey(): String = lowercase()

    private companion object {
        const val DEBOUNCE_MS = 150L
        const val CACHE_SIZE = 10
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

data class DiscoverySearchState(
    val query: String = "",
    val results: List<DiscoverySearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val isActive: Boolean get() = query.isNotEmpty()
    val isEmptyResult: Boolean
        get() = isActive && !isLoading && error == null && results.isEmpty()
}
