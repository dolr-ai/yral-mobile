package com.yral.shared.firebaseStore

import cocoapods.FirebaseAppCheck.FIRAppCheck
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalForeignApi::class)
actual suspend fun firebaseAppCheckToken(): String =
    suspendCancellableCoroutine { continuation ->
        FIRAppCheck.appCheck().tokenForcingRefresh(false) { appCheckToken, error ->
            if (error == null) {
                checkNotNull(appCheckToken)
                continuation.resume(appCheckToken.token)
            } else {
                continuation.resumeWithException(Exception(error.toString()))
            }
        }
    }
