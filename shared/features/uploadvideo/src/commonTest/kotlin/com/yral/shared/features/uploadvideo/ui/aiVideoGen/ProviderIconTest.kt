package com.yral.shared.features.uploadvideo.ui.aiVideoGen

import com.yral.shared.features.uploadvideo.domain.models.Provider
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProviderIconTest {
    @Test
    fun `ltx id uses bundled provider icon`() {
        assertTrue(provider(id = "ltx", name = "Model").usesLtxProviderIcon())
    }

    @Test
    fun `ltx prefixed id uses bundled provider icon`() {
        assertTrue(provider(id = "ltx-video", name = "Model").usesLtxProviderIcon())
    }

    @Test
    fun `ltx name uses bundled provider icon when id differs`() {
        assertTrue(provider(id = "provider-id", name = "LTX Video").usesLtxProviderIcon())
    }

    @Test
    fun `unrelated provider does not use bundled provider icon`() {
        assertFalse(provider(id = "veo", name = "Google Veo").usesLtxProviderIcon())
    }

    private fun provider(
        id: String,
        name: String,
    ): Provider =
        Provider(
            id = id,
            name = name,
            description = null,
            cost = null,
            supportsImage = null,
            supportsNegativePrompt = null,
            supportsAudio = null,
            supportsSeed = null,
            allowedAspectRatios = emptyList(),
            allowedResolutions = emptyList(),
            allowedDurations = emptyList(),
            defaultAspectRatio = null,
            defaultResolution = null,
            defaultDuration = null,
            isAvailable = null,
            isInternal = null,
            modelIcon = "https://example.com/icon.svg",
            extraInfo = null,
        )
}
