package com.yral.shared.features.coach.nav

import kotlinx.serialization.Serializable

@Serializable
data class OpenCoachParams(
    val botId: String,
    val botName: String? = null,
    val avatarUrl: String? = null,
    /**
     * Coach pivot Bucket 2 — when Coach was opened by tapping a
     * specific section card on the Soul File page, this carries the
     * section's stable backend slug. Wired into the
     * `POST /conversations/{bot_id}` body's `section_hint` so the
     * backend defaults proposals to that section. Null when Coach was
     * opened from the generic entry point.
     */
    val sectionHint: String? = null,
)
