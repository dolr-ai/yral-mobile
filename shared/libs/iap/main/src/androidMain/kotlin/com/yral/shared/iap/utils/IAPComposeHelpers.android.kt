package com.yral.shared.iap.utils

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable

@Composable
actual fun getPurchaseContext(): Any? = LocalActivity.current
