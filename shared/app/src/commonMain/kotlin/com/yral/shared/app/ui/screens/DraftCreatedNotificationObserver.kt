package com.yral.shared.app.ui.screens

import androidx.compose.runtime.Composable
import com.yral.shared.features.uploadvideo.presentation.VideoDraftPollingManager

@Composable
internal expect fun ObserveDraftCreatedNotifications(videoDraftPollingManager: VideoDraftPollingManager)
