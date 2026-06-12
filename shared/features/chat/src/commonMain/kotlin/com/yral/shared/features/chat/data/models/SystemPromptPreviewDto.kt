package com.yral.shared.features.chat.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Coach pivot Bucket 2 — `GET /api/v1/influencers/{bot_id}/system-prompt-preview`
 * response shape. Owner-gated. Returns the FULL composed system prompt the
 * LLM sees at chat time (L1–L4 layers + skills + applied overrides +
 * engagement schedule) so the creator can audit it from the read-only
 * "View full prompt" page in Coach.
 *
 * Defensive nullability: every field declares a default. Backend ships a
 * tight contract but mobile-side `MissingFieldException` is the bug class
 * we got bitten by on the override-apply flow last week — declaring
 * tolerant DTOs avoids the same trap when backend evolves.
 */
@Serializable
data class SystemPromptPreviewResponseDto(
    @SerialName("bot_id") val botId: String,
    @SerialName("bot_name") val botName: String? = null,
    @SerialName("archetype") val archetype: String? = null,
    @SerialName("as_of") val asOf: String,
    @SerialName("layers") val layers: SystemPromptLayersDto,
    @SerialName("skills_enabled") val skillsEnabled: List<EnabledSkillDto> = emptyList(),
    @SerialName("applied_overrides") val appliedOverrides: Map<String, String> = emptyMap(),
    @SerialName("composed_preview_text") val composedPreviewText: String,
    @SerialName("engagement_schedule") val engagementSchedule: EngagementScheduleDto? = null,
)

@Serializable
data class SystemPromptLayersDto(
    @SerialName("L1_global_rules") val l1GlobalRules: String = "",
    @SerialName("L2_archetype_block") val l2ArchetypeBlock: String = "",
    @SerialName("L3_personality_sections") val l3PersonalitySections: List<SystemPromptSectionDto> = emptyList(),
    @SerialName("L3_flat_fallback") val l3FlatFallback: String? = null,
    @SerialName("L4_user_segment_template") val l4UserSegmentTemplate: String = "",
)

@Serializable
data class SystemPromptSectionDto(
    @SerialName("id") val id: String,
    @SerialName("heading") val heading: String,
    @SerialName("body") val body: String,
)

@Serializable
data class EnabledSkillDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String,
    @SerialName("prompt_block") val promptBlock: String,
)

@Serializable
data class EngagementScheduleDto(
    @SerialName("inactivity_proactive") val inactivityProactive: InactivityProactiveDto? = null,
    @SerialName("skill_checkins") val skillCheckins: SkillCheckinsDto? = null,
    @SerialName("first_turn_nudge") val firstTurnNudge: FirstTurnNudgeDto? = null,
)

@Serializable
data class InactivityProactiveDto(
    @SerialName("enabled_by_default") val enabledByDefault: Boolean? = null,
    @SerialName("threshold_hours") val thresholdHours: Int? = null,
    @SerialName("per_conversation_overrides") val perConversationOverrides: List<String> = emptyList(),
    @SerialName("source") val source: String? = null,
    @SerialName("note") val note: String? = null,
)

@Serializable
data class SkillCheckinsDto(
    @SerialName("skill_slug") val skillSlug: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("default_cadence_hours") val defaultCadenceHours: Int? = null,
    /**
     * Backend ships this as a Boolean (not a list of times as the field
     * name might suggest). True = bot honours per-user preferred-time
     * scheduling at chat time; false = ignores user prefs.
     */
    @SerialName("per_user_preferred_times") val perUserPreferredTimes: Boolean? = null,
    @SerialName("source") val source: String? = null,
    @SerialName("note") val note: String? = null,
)

@Serializable
data class FirstTurnNudgeDto(
    @SerialName("enabled") val enabled: Boolean? = null,
    @SerialName("initial_idle_minutes") val initialIdleMinutes: Int? = null,
    @SerialName("source") val source: String? = null,
    @SerialName("note") val note: String? = null,
)
