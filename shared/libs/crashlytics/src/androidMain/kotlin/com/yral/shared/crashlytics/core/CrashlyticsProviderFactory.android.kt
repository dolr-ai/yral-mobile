package com.yral.shared.crashlytics.core

import com.google.firebase.crashlytics.FirebaseCrashlytics

internal actual fun createCrashlyticsProvider(): CrashlyticsProvider =
    FirebaseCrashlyticsProvider(
        crashlytics = FirebaseCrashlytics.getInstance(),
    )
