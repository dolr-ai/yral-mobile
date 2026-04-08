package com.yral.shared.crashlytics.core

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
import dev.gitlive.firebase.crashlytics.crashlytics

internal actual fun createCrashlyticsProvider(): CrashlyticsProvider =
    FirebaseCrashlyticsProvider(
        crashlytics = Firebase.crashlytics(Firebase.app),
    )
