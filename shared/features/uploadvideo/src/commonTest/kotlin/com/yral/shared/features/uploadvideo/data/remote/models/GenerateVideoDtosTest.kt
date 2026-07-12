package com.yral.shared.features.uploadvideo.data.remote.models

import com.yral.shared.core.rust.KotlinDelegatedIdentityWire
import com.yral.shared.core.rust.KotlinJwkEcKey
import com.yral.shared.features.uploadvideo.domain.models.GenerateVideoParams
import com.yral.shared.features.uploadvideo.domain.models.ImageData
import com.yral.shared.features.uploadvideo.domain.models.ImageInput
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GenerateVideoDtosTest {
    @Test
    fun `text generation request omits image`() {
        val dto =
            GenerateVideoParams(
                providerId = "provider",
                prompt = "prompt",
            ).toRequestDto(TEST_DELEGATED_IDENTITY)

        assertNull(dto.request.image)
    }

    @Test
    fun `image generation request includes base64 image payload`() {
        val dto =
            GenerateVideoParams(
                providerId = "provider",
                prompt = "prompt",
                image =
                    ImageData.Base64(
                        ImageInput(
                            data = "base64-data",
                            mimeType = "image/png",
                        ),
                    ),
            ).toRequestDto(TEST_DELEGATED_IDENTITY)

        val image = dto.request.image?.jsonObject
        val value = image?.get("value")?.jsonObject
        assertEquals("Base64", image?.get("type")?.jsonPrimitive?.content)
        assertEquals("base64-data", value?.get("data")?.jsonPrimitive?.content)
        assertEquals("image/png", value?.get("mime_type")?.jsonPrimitive?.content)
    }

    private companion object {
        val TEST_DELEGATED_IDENTITY =
            KotlinDelegatedIdentityWire(
                fromKey = emptyList(),
                toSecret =
                    KotlinJwkEcKey(
                        kty = "EC",
                        crv = "P-256",
                        x = "x",
                        y = "y",
                        d = null,
                    ),
                delegationChain = emptyList(),
            )
    }
}
