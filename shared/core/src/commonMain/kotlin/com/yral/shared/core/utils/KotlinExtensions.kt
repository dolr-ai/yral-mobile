package com.yral.shared.core.utils

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlin.time.Duration

private const val MAX_CONCURRENCY = 64

class InsufficientItemsException(
    message: String,
) : Exception(message)

fun <T> Iterable<T>.filterFirstNSuspendFlow(
    n: Int,
    throwOnInsufficient: Boolean = false, // Whether to throw exception when insufficient items are found
    maxConcurrency: Int = MAX_CONCURRENCY, // Optional cap to prevent excessive parallelism
    predicate: suspend (T) -> Boolean,
): Flow<T> {
    // Handle edge case where n is 0
    if (n <= 0) {
        return emptyFlow()
    }

    return channelFlow {
        val items = this@filterFirstNSuspendFlow.toList()

        // Handle edge case where input is empty
        if (items.isEmpty()) {
            if (throwOnInsufficient) {
                throw InsufficientItemsException("Found only 0 items out of required $n")
            }
            return@channelFlow
        }

        val semaphore = Semaphore(maxConcurrency)
        val resultChannel = Channel<T>(Channel.UNLIMITED)
        val completedJobsMutex = Mutex()
        var completedJobs = 0
        val totalJobs = items.size
        val jobs =
            items.map { item ->
                launch {
                    semaphore.withPermit {
                        if (predicate(item)) {
                            resultChannel.send(item)
                        }
                    }
                    // Track completion
                    completedJobsMutex.withLock {
                        completedJobs++
                        if (completedJobs == totalJobs) {
                            resultChannel.close()
                        }
                    }
                }
            }
        launch {
            var count = 0
            for (item in resultChannel) {
                send(item)
                count++
                if (count >= n) {
                    jobs.forEach { it.cancel() }
                    resultChannel.close()
                    break
                }
            }
            // If we exit the loop because channel was closed but didn't get enough items
            if (count < n && throwOnInsufficient) {
                throw InsufficientItemsException("Found only $count items out of required $n")
            }
        }
    }
}

fun <P, R> Iterable<P>.processFirstNSuspendFlow(
    n: Int,
    maxConcurrency: Int = MAX_CONCURRENCY, // Optional cap to prevent excessive parallelism
    process: suspend (P) -> R,
): Flow<R> {
    if (n <= 0) {
        return emptyFlow()
    }

    return channelFlow {
        val items = this@processFirstNSuspendFlow.toList()

        // Handle edge case where input is empty
        if (items.isEmpty()) {
            return@channelFlow
        }

        val semaphore = Semaphore(maxConcurrency)
        val resultChannel = Channel<R>(Channel.UNLIMITED)
        val completedJobsMutex = Mutex()
        var completedJobs = 0
        val totalJobs = items.size
        val jobs =
            items.map { item ->
                launch {
                    semaphore.withPermit {
                        resultChannel.send(process(item))
                    }
                    // Track completion
                    completedJobsMutex.withLock {
                        completedJobs++
                        if (completedJobs == totalJobs) {
                            resultChannel.close()
                        }
                    }
                }
            }
        launch {
            var count = 0
            for (item in resultChannel) {
                send(item)
                count++
                if (count >= n) {
                    jobs.forEach { it.cancel() }
                    resultChannel.close()
                    break
                }
            }
        }
    }
}

inline fun <reified T : Enum<T>> safeValueOf(name: String?): T? = enumValues<T>().find { it.name == name }

inline fun <T> List<T>.update(
    index: Int,
    transform: (T) -> T,
): List<T> = toMutableList().also { it[index] = transform(it[index]) }

@Suppress("MagicNumber")
fun formatRemainingDuration(duration: Duration): String {
    val totalSeconds = duration.inWholeSeconds.coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    fun Long.twoDigits(): String = this.toString().padStart(2, '0')

    return when {
        hours > 0 -> "${hours.twoDigits()}:${minutes.twoDigits()}:${seconds.twoDigits()}"
        minutes > 0 -> "${minutes.twoDigits()}:${seconds.twoDigits()}"
        else -> "${minutes.twoDigits()}:${seconds.twoDigits()}"
    }
}
