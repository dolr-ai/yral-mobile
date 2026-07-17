package com.yral.featureflag

import kotlin.test.Test
import kotlin.test.assertFalse

class ChatFeatureFlagsTest {
    @Test
    fun soulFileCoach_remainsDisabledByDefault() {
        // PR #1185 flipped this to false for production safety — the feature
        // calls agent.rishi.yral.com endpoints that don't exist on the chat-ai
        // backend yet. Stays dormant (like H2H/Audio/SSE/Chat-as-Human/Video Ideas)
        // until backend cutover + GA, then flipped via Firebase Remote Config.
        assertFalse(ChatFeatureFlags.Chat.SoulFileCoachEnabled.defaultValue)
    }

    @Test
    fun chatAsHumanCreator_remainsDisabledByDefault() {
        assertFalse(ChatFeatureFlags.Chat.ChatAsHumanCreatorEnabled.defaultValue)
    }
}
