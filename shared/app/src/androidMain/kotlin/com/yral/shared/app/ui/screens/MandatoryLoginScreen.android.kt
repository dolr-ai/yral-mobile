package com.yral.shared.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun getContext(): Any = LocalContext.current
