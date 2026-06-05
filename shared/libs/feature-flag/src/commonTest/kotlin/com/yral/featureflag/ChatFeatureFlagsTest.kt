package com.yral.featureflag

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatFeatureFlagsTest {
    @Test
    fun soulFileCoach_isEnabledByDefault() {
        assertTrue(ChatFeatureFlags.Chat.SoulFileCoachEnabled.defaultValue)
    }

    @Test
    fun chatAsHumanCreator_remainsDisabledByDefault() {
        assertFalse(ChatFeatureFlags.Chat.ChatAsHumanCreatorEnabled.defaultValue)
    }
}
