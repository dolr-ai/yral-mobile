package com.yral.shared.features.uploadvideo.presentation

import com.yral.shared.features.uploadvideo.domain.models.ImageData

interface BotVideoGenManager {
    fun enqueueGeneration(
        botPrincipal: String,
        prompt: String,
        imageData: ImageData,
    )
}
