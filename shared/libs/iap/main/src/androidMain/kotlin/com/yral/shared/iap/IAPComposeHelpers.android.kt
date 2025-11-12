package com.yral.shared.iap

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable

@Composable
internal actual fun getPurchaseContext(): Any? = LocalActivity
