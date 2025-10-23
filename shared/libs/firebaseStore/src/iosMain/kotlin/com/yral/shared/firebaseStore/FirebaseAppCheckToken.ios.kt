package com.yral.shared.firebaseStore

import cocoapods.FirebaseAppCheck.FIRAppCheck
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalForeignApi::class)
actual suspend fun firebaseAppCheckToken(): String =
    suspendCoroutine {
        FIRAppCheck.appCheck().tokenForcingRefresh(false) { appCheckToken, error ->
            if (error == null) {
                checkNotNull(appCheckToken)
                it.resume(appCheckToken.token)
            } else {
                it.resumeWithException(Exception(error.toString()))
            }
        }
    }
