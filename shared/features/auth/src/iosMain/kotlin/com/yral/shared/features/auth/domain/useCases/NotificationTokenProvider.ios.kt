@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.yral.shared.features.auth.domain.useCases

import cocoapods.FirebaseMessaging.FIRMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal actual suspend fun getNotificationToken(): String =
    suspendCancellableCoroutine { continuation ->
        FIRMessaging.messaging().tokenWithCompletion { token, error ->
            when {
                error != null -> {
                    continuation.resumeWithException(
                        error.asException("Firebase Messaging token fetch failed"),
                    )
                }

                token != null -> {
                    continuation.resume(token)
                }

                else -> {
                    continuation.resumeWithException(IllegalStateException("Firebase Messaging returned no token"))
                }
            }
        }
    }

private fun NSError.asException(fallbackMessage: String): Exception = Exception(localizedDescription ?: fallbackMessage)
