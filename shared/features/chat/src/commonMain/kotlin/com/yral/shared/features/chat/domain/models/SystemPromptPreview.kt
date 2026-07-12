package com.yral.shared.features.chat.domain.models

/**
 * Coach pivot Bucket 2 — what the mobile-side "View full prompt" page renders.
 *
 * This is the complete system prompt the LLM sees when a user chats with this
 * bot, broken down by composition layer. The page is purely a transparency
 * window — read-only. All actual edits to the bot's personality happen via
 * Coach (`/apply`), not from this page.
 *
 * Backend endpoint: GET /api/v1/influencers/{bot_id}/system-prompt-preview.
 */
data class SystemPromptPreview(
    val botId: String,
    val botName: String?,
    val archetype: String?,
    /**
     * ISO-8601 timestamp of when the preview was composed server-side. The
     * page always fetches fresh on entry; this surfaces the freshness to the
     * creator so they know an override applied 10 seconds ago is reflected.
     */
    val asOf: String,
    val layers: SystemPromptLayers,
    /**
     * Empty list when the bot has no enabled skills. Never null. Empty
     * suppresses the Skills section entirely in the UI.
     */
    val skillsEnabled: List<EnabledSkill>,
    /**
     * Overrides applied via Coach (`/apply` with proposed_global_rule_override
     * or proposed_section_change). Each entry is "rule_key" -> "applied_value"
     * (e.g. "response_length" -> "shorter"). Empty when no overrides applied.
     */
    val appliedOverrides: Map<String, String>,
    /**
     * The fully-composed final prompt text the LLM actually receives. Kept on
     * the model for completeness even though the v1 UI renders structured-only
     * (no "view raw" toggle per Rishi).
     */
    val composedPreviewText: String,
    /**
     * Coach pivot Bucket 2 follow-up — when the backend wires engagement
     * schedule into the preview response, this carries the proactive-
     * messaging / check-in cadence knobs. Null when the backend response
     * predates the engagement_schedule top-level field (older deploy),
     * so the UI section stays hidden until the new endpoint lands.
     */
    val engagementSchedule: EngagementSchedule? = null,
)

data class EngagementSchedule(
    val inactivityProactive: InactivityProactive?,
    val skillCheckins: SkillCheckins?,
    val firstTurnNudge: FirstTurnNudge?,
)

data class InactivityProactive(
    val enabledByDefault: Boolean?,
    val thresholdHours: Int?,
    val perConversationOverrides: List<String>,
    val source: String?,
    val note: String?,
)

data class SkillCheckins(
    val skillSlug: String?,
    val displayName: String?,
    val defaultCadenceHours: Int?,
    /**
     * Backend ships this as a Boolean despite the plural field name —
     * true = bot honours per-user preferred-time scheduling.
     */
    val perUserPreferredTimes: Boolean?,
    val source: String?,
    val note: String?,
)

data class FirstTurnNudge(
    val enabled: Boolean?,
    val initialIdleMinutes: Int?,
    val source: String?,
    val note: String?,
)

data class SystemPromptLayers(
    val l1GlobalRules: String,
    val l2ArchetypeBlock: String,
    /**
     * Empty list when the bot is on the flat-fallback legacy representation.
     * In that case [l3FlatFallback] is populated instead.
     */
    val l3PersonalitySections: List<SystemPromptSection>,
    /**
     * Populated only for legacy bots that haven't been migrated to sections.
     * UI renders "Layer 3 — Personality (flat)" with this body when present;
     * otherwise renders "Layer 3 — Personality sections" from [l3PersonalitySections].
     */
    val l3FlatFallback: String?,
    val l4UserSegmentTemplate: String,
)

data class SystemPromptSection(
    val id: String,
    val heading: String,
    val body: String,
)

data class EnabledSkill(
    val id: String,
    val name: String,
    val description: String,
    /**
     * The exact prompt block the backend injects for this skill when it's
     * enabled — what the LLM literally sees. Rendered verbatim in the
     * collapsible skill row body so the creator can audit it.
     */
    val promptBlock: String,
)
