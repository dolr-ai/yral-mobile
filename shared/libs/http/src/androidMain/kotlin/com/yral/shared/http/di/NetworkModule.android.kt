package com.yral.shared.http.di

import org.koin.android.ext.koin.androidApplication
import org.koin.core.scope.Scope

actual fun Scope.platformContext(): Any = androidApplication()
