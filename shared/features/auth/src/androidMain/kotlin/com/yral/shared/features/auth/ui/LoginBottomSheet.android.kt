package com.yral.shared.features.auth.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun getContext(): Any = LocalContext.current
