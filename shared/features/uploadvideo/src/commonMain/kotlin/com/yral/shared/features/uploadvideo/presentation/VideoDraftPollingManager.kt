package com.yral.shared.features.uploadvideo.presentation

import com.yral.shared.core.logging.YralLogger
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.videostate.VideoGenerationTracker
import com.yral.shared.features.uploadvideo.domain.UploadRepository
import com.yral.shared.features.uploadvideo.domain.models.InProgressDraft
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VideoDraftPollingManager(
    private val repository: UploadRepository,
    private val sessionManager: SessionManager,
    appDispatchers: AppDispatchers,
    logger: YralLogger,
) {
    private val logger = logger.withTag("VideoDraftPollingManager")
    private val scope = CoroutineScope(SupervisorJob() + appDispatchers.network)
    private var pollingJob: Job? = null
    private var activeUserId: String? = null

    fun onGenerationSubmitted(userId: String) {
        activeUserId = userId
        startPolling(userId = userId, waitBeforeFirstPoll = true)
    }

    fun onAppForegrounded() {
        val userId = sessionManager.userPrincipal ?: return
        activeUserId = userId
        startPolling(userId = userId, waitBeforeFirstPoll = false)
    }

    fun onAppBackgrounded() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun startPolling(
        userId: String,
        waitBeforeFirstPoll: Boolean,
    ) {
        pollingJob?.cancel()
        pollingJob =
            scope.launch {
                if (waitBeforeFirstPoll) {
                    delay(POLL_INTERVAL_MS)
                }
                while (currentCoroutineContext().isActive) {
                    val items = pollOnce(userId)
                    if (items == null) {
                        delay(POLL_INTERVAL_MS)
                    } else if (items.isEmpty()) {
                        if (activeUserId == userId) {
                            activeUserId = null
                        }
                        pollingJob = null
                        break
                    } else {
                        delay(POLL_INTERVAL_MS)
                    }
                }
            }
    }

    private suspend fun pollOnce(userId: String): List<InProgressDraft>? =
        try {
            val items = repository.getInProgressDrafts(userId)
            VideoGenerationTracker.setPendingGenerationCount(items.size)
            VideoGenerationTracker.requestDraftsRefresh()
            logger.d { "in_progress_drafts userId=$userId count=${items.size}" }
            items
        } catch (
            @Suppress("TooGenericExceptionCaught") error: Exception,
        ) {
            logger.e(error) { "Failed to poll in-progress video drafts for $userId" }
            null
        }

    private companion object {
        const val POLL_INTERVAL_MS = 30_000L
    }
}
