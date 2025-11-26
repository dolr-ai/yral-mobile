package com.yral.android.installReferrer

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import com.yral.shared.analytics.AnalyticsManager
import com.yral.shared.analytics.events.ReferralReceivedEventData
import com.yral.shared.core.logging.YralLogger
import com.yral.shared.koin.koinInstance
import com.yral.shared.preferences.UtmAttributionStore
import com.yral.shared.preferences.UtmParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.qualifier.named
import kotlin.coroutines.resume

/**
 * Attribution processors are executed in priority order (lower number = higher priority).
 */
interface AttributionProcessor {
    val priority: Int
    val name: String
    fun process(callback: (UtmParams?) -> Unit)
}

/**
 * Coordinates attribution processors in priority order.
 * Only the highest priority processor with valid data stores attribution.
 */
class AttributionManager(
    private val processors: List<AttributionProcessor>,
) {
    val processorsList: List<AttributionProcessor> get() = processors
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private var isProcessing = false
    private val processedProcessors = mutableSetOf<String>()

    private val utmAttributionStore: UtmAttributionStore by lazy { koinInstance.get<UtmAttributionStore>() }
    private val analyticsManager: AnalyticsManager by lazy { koinInstance.get<AnalyticsManager>() }
    private val logger = createLogger("AttributionManager")

    companion object {
        @Volatile
        private var loggerFactory: ((String) -> Logger)? = null

        // Override logger factory for testing. Set to null to use default factory.
        internal fun setLoggerFactory(factory: ((String) -> Logger)?) {
            loggerFactory = factory
        }

        fun createLogger(tag: String): Logger {
            val factory = loggerFactory
            return if (factory != null) {
                factory(tag)
            } else {
                runCatching {
                    val baseLogger = koinInstance.get<YralLogger>()
                    val sentryLogWriter = koinInstance.get<LogWriter>(named("installReferrerLogWriter"))
                    baseLogger.withAdditionalLogWriter(sentryLogWriter).withTag(tag)
                }.getOrElse {
                    Logger.withTag(tag)
                }
            }
        }
    }

    fun processAttribution() {
        scope.launch(Dispatchers.Default) {
            if (!shouldStartProcessing()) return@launch
            runCatching {
                val sortedProcessors = sortProcessorsByPriority(processors)
                logger.i { "Starting attribution processing with ${sortedProcessors.size} processors" }
                processProcessorsInOrder(sortedProcessors)
                logger.i { "Attribution processing completed" }
            }.onFailure {
                logger.e(it) { "Error during attribution processing" }
            }.also { markProcessingComplete() }
        }
    }

    private suspend fun shouldStartProcessing(): Boolean =
        mutex.withLock {
            if (isProcessing) {
                logger.d { "Attribution processing already in progress, skipping" }
                false
            } else if (utmAttributionStore.isInstallReferrerCompleted()) {
                logger.d { "Attribution already completed, skipping processing" }
                false
            } else {
                isProcessing = true
                true
            }
        }

    private fun sortProcessorsByPriority(processors: List<AttributionProcessor>): List<AttributionProcessor> =
        processors.sortedBy { it.priority }

    private suspend fun processProcessorsInOrder(sortedProcessors: List<AttributionProcessor>) {
        val processorsToProcess =
            mutex.withLock {
                sortedProcessors.filter { !processedProcessors.contains(it.name) }
            }
        for (processor in processorsToProcess) {
            if (utmAttributionStore.isInstallReferrerCompleted()) {
                logger.i { "Attribution already completed, skipping remaining processors" }
                return
            }
            val result = processSingleProcessor(processor)
            if (result != null && shouldStoreAttribution(result)) {
                storeAttribution(result, processor.name)
                logger.i { "Attribution stored successfully by ${processor.name}" }
                return
            }
        }
    }

    private suspend fun processSingleProcessor(processor: AttributionProcessor): UtmParams? {
        logger.d { "Processing attribution with ${processor.name} (priority ${processor.priority})" }
        val result = processProcessor(processor)
        mutex.withLock {
            processedProcessors.add(processor.name)
        }
        if (result == null || result.isEmpty()) {
            logger.d { "${processor.name} returned no valid attribution data" }
            return null
        }
        return result
    }

    private fun shouldStoreAttribution(result: UtmParams?): Boolean =
        result != null && result.isNotEmpty() && !utmAttributionStore.isInstallReferrerCompleted()

    private suspend fun markProcessingComplete() {
        mutex.withLock {
            isProcessing = false
        }
    }

    private suspend fun processProcessor(processor: AttributionProcessor): UtmParams? =
        suspendCancellableCoroutine { continuation ->
            processor.process { utmParams ->
                continuation.resume(utmParams)
            }
        }

    private fun storeAttribution(
        utmParams: UtmParams,
        processorName: String,
    ) {
        runCatching {
            utmAttributionStore.storeIfEmpty(
                source = utmParams.source,
                medium = utmParams.medium,
                campaign = utmParams.campaign,
                term = utmParams.term,
                content = utmParams.content,
            )
            analyticsManager.trackEvent(
                ReferralReceivedEventData(
                    source = utmParams.source,
                    medium = utmParams.medium,
                    campaign = utmParams.campaign,
                    term = utmParams.term,
                    content = utmParams.content,
                ),
            )
            logger.i {
                "Attribution stored by $processorName: " +
                    "source=${utmParams.source}, campaign=${utmParams.campaign}, " +
                    "medium=${utmParams.medium}, term=${utmParams.term}, content=${utmParams.content}"
            }
        }.onFailure {
            logger.e(it) { "Failed to store attribution from $processorName" }
            throw it
        }
    }

    fun reprocessAttribution() {
        scope.launch(Dispatchers.Default) {
            mutex.withLock {
                if (utmAttributionStore.isInstallReferrerCompleted()) {
                    logger.d { "Attribution already completed, skipping re-processing" }
                    return@launch
                }
            }
            processAttribution()
        }
    }

    fun clearProcessedState(processorName: String) {
        scope.launch(Dispatchers.Default) {
            mutex.withLock {
                processedProcessors.remove(processorName)
            }
        }
    }
}
