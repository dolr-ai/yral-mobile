@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.yral.featureflag.providers

import cocoapods.FirebaseRemoteConfig.FIRRemoteConfig
import cocoapods.FirebaseRemoteConfig.FIRRemoteConfigSource
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration

internal actual fun createFirebaseRemoteConfigPlatform(): FirebaseRemoteConfigPlatform =
    IosFirebaseRemoteConfigPlatform(
        remoteConfig = FIRRemoteConfig.remoteConfig(),
    )

private class IosFirebaseRemoteConfigPlatform(
    private val remoteConfig: FIRRemoteConfig,
) : FirebaseRemoteConfigPlatform {
    override fun getStringOrNull(key: String): String? {
        val value = remoteConfig.configValueForKey(key)
        return if (value.source != FIRRemoteConfigSource.FIRRemoteConfigSourceStatic) {
            value.stringValue
        } else {
            null
        }
    }

    override suspend fun fetchAndActivate(minimumFetchInterval: Duration?) {
        if (minimumFetchInterval != null) {
            fetch(minimumFetchInterval)
            activate()
        } else {
            fetchAndActivate()
        }
    }

    private suspend fun fetchAndActivate() {
        suspendCancellableCoroutine { continuation ->
            remoteConfig.fetchAndActivateWithCompletionHandler { _, error ->
                if (error != null) {
                    continuation.resumeWithException(
                        error.asException("Firebase Remote Config fetch and activate failed"),
                    )
                } else {
                    continuation.resume(Unit)
                }
            }
        }
    }

    private suspend fun fetch(minimumFetchInterval: Duration) {
        suspendCancellableCoroutine { continuation ->
            remoteConfig.fetchWithExpirationDuration(
                expirationDuration = minimumFetchInterval.inWholeSeconds.toDouble(),
                completionHandler = { _, error ->
                    if (error != null) {
                        continuation.resumeWithException(
                            error.asException("Firebase Remote Config fetch failed"),
                        )
                    } else {
                        continuation.resume(Unit)
                    }
                },
            )
        }
    }

    private suspend fun activate() {
        suspendCancellableCoroutine { continuation ->
            remoteConfig.activateWithCompletion { _, error ->
                if (error != null) {
                    continuation.resumeWithException(
                        error.asException("Firebase Remote Config activation failed"),
                    )
                } else {
                    continuation.resume(Unit)
                }
            }
        }
    }
}

private fun NSError.asException(fallbackMessage: String): Exception = Exception(localizedDescription ?: fallbackMessage)
