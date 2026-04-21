package com.yral.featureflag.providers

import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration

internal actual fun createFirebaseRemoteConfigPlatform(): FirebaseRemoteConfigPlatform =
    AndroidFirebaseRemoteConfigPlatform(
        remoteConfig = FirebaseRemoteConfig.getInstance(),
    )

private class AndroidFirebaseRemoteConfigPlatform(
    private val remoteConfig: FirebaseRemoteConfig,
) : FirebaseRemoteConfigPlatform {
    override fun getStringOrNull(key: String): String? {
        val value = remoteConfig.getValue(key)
        return if (value.source != FirebaseRemoteConfig.VALUE_SOURCE_STATIC) {
            value.asString()
        } else {
            null
        }
    }

    override suspend fun fetchAndActivate(minimumFetchInterval: Duration?) {
        if (minimumFetchInterval != null) {
            remoteConfig.fetch(minimumFetchInterval.inWholeSeconds).await()
            remoteConfig.activate().await()
        } else {
            remoteConfig.fetchAndActivate().await()
        }
    }
}

private suspend fun <T> Task<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(task.exception ?: IllegalStateException("Firebase task failed"))
            }
        }
    }
