package com.yral.shared.features.chat.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Coach pivot Bucket 2 — `GET /api/v1/influencers/{bot_id}/soul-file`
 * response shape. Owner-gated. Returns the bot's personality split
 * into editable sections (or a single synthetic section for bots
 * that haven't opted into the multi-section model — see
 * [fallbackToFlat]).
 */
@Serializable
data class SoulFileResponseDto(
    @SerialName("bot_id") val botId: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("sections") val sections: List<SoulFileSectionDto>,
    @SerialName("sections_version_sha256") val sectionsVersionSha256: String,
    @SerialName("fallback_to_flat") val fallbackToFlat: Boolean = false,
)

@Serializable
data class SoulFileSectionDto(
    @SerialName("id") val id: String,
    @SerialName("heading") val heading: String,
    @SerialName("body") val body: String,
    @SerialName("editable") val editable: Boolean,
)

/**
 * `PUT /api/v1/influencers/{bot_id}/soul-file` body. Optimistic
 * concurrency: backend returns 409 `stale_sections` if the supplied
 * [expectedSectionsVersionSha256] doesn't match current. Mobile must
 * re-GET on 409 (auto-merging plain-English text is lossy).
 */
@Serializable
data class UpdateSoulFileRequestDto(
    @SerialName("sections") val sections: List<SoulFileSectionDto>,
    @SerialName("expected_sections_version_sha256") val expectedSectionsVersionSha256: String,
)
