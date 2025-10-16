package com.yral.shared.analytics.di

import org.koin.core.qualifier.named

val MIXPANEL_TOKEN = named("mixpanelToken")
val IS_DEBUG = named("isDEBUG")
val ONESIGNAL_APP_ID = named("oneSignalAppId")
