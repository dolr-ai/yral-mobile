package com.yral.shared.core.utils

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

private const val MAX_CONCURRENCY = 64

class InsufficientItemsException(
    message: String,
) : Exception(message)

fun <T> Iterable<T>.filterFirstNSuspendFlow(
    n: Int,
    maxConcurrency: Int = MAX_CONCURRENCY, // Optional cap to prevent excessive parallelism
    predicate: suspend (T) -> Boolean,
): Flow<T> =
    channelFlow {
        val semaphore = Semaphore(maxConcurrency)
        val resultChannel = Channel<T>(Channel.UNLIMITED)
        val completedJobsMutex = Mutex()
        var completedJobs = 0
        val totalJobs = count()
        val jobs =
            map { item ->
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
            if (count < n) {
                throw InsufficientItemsException("Found only $count items out of required $n")
            }
        }
    }
