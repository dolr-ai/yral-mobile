package com.yral.shared.features.auth.ui.components

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable

@Composable
internal actual fun getContext(): Any? = LocalActivity.current
