package com.yral.shared.firebaseStore

import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import kotlinx.coroutines.tasks.await

actual suspend fun firebaseAppCheckToken(): String =
    Firebase.appCheck
        .getToken(false)
        .await()
        .token
