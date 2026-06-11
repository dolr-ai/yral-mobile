package com.yral.shared.features.chat.domain.models

/**
 * Coach pivot Bucket 2 — sectioned representation of a bot's personality.
 *
 * The bot's identity used to live as one opaque blob in
 * `system_instructions`. With sections, each part (Core personality,
 * Voice and tone, etc.) becomes a separately editable unit. Mobile
 * renders the Soul File page as a list of cards, one per [sections]
 * entry; tap-to-edit + tap-to-coach happen per-section, not against
 * the whole blob.
 *
 * Backwards-compat: backend may return [fallbackToFlat] = true for
 * bots still on the flat representation. In that case, [sections]
 * still contains exactly one synthetic section (heading
 * `"Core personality"`, id `"core_personality"`) so the screen has
 * no special-case branch.
 */
data class SoulFile(
    val botId: String,
    val displayName: String?,
    val sections: List<SoulFileSection>,
    /**
     * Opportunistic-concurrency handle from the backend. Mobile passes
     * this back unchanged on PUT; backend returns 409 if the sections
     * changed in the meantime. Treat as opaque.
     */
    val sectionsVersionSha256: String,
    val fallbackToFlat: Boolean,
)

data class SoulFileSection(
    /**
     * Stable backend slug (lowercase snake_case). Coach proposals
     * reference sections by this id. Never localized; never shown
     * directly to the user.
     */
    val id: String,
    /**
     * Human-readable section title rendered as the card heading.
     * Editable on PUT.
     */
    val heading: String,
    /**
     * The instruction body fed into the LLM at chat time. THE field
     * the creator edits when they tap Edit, and the field Coach
     * proposes new values for.
     */
    val body: String,
    /**
     * When false, the section is read-only on mobile (greyed card,
     * no Edit affordance) and Coach refuses proposals against it.
     * Used for platform-rules sections.
     */
    val editable: Boolean,
)
