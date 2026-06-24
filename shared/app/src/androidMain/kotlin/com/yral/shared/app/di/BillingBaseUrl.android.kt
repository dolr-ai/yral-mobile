package com.yral.shared.app.di

import com.yral.shared.core.AppConfigurations

actual fun resolveBillingBaseUrl(): String = AppConfigurations.BILLING_BASE_URL
