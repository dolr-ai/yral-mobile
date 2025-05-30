package com.yral.shared.core.utils

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private const val MAX_CONCURRENCY = 64

fun <T> Iterable<T>.filterFirstNSuspendFlow(
    n: Int,
    maxConcurrency: Int = MAX_CONCURRENCY, // Optional cap to prevent excessive parallelism
    predicate: suspend (T) -> Boolean,
): Flow<T> =
    channelFlow {
        val semaphore = Semaphore(maxConcurrency)
        val resultChannel = Channel<T>(Channel.UNLIMITED)
        val jobs =
            map { item ->
                launch {
                    semaphore.withPermit {
                        if (predicate(item)) {
                            resultChannel.send(item)
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
