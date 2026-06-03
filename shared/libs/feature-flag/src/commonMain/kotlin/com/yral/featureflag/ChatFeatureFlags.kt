package com.yral.featureflag

import com.yral.featureflag.core.FeatureFlag
import com.yral.featureflag.core.FlagAudience
import com.yral.featureflag.core.FlagGroup

object ChatFeatureFlags {
    object Chat :
        FlagGroup(keyPrefix = "chat", defaultAudience = FlagAudience.INTERNAL_QA) {
        val Enabled: FeatureFlag<Boolean> =
            boolean(
                keySuffix = "enabled",
                name = "Enable or disable chat",
                description = "Enable or disable chat",
                defaultValue = true,
            )
        val LoginPromptMessageThreshold: FeatureFlag<Int> =
            int(
                keySuffix = "loginPromptMessageThreshold",
                name = "Login prompt message limit",
                description = "Total message count at which to show login prompt in chat",
                defaultValue = 10,
            )
        val SubscriptionMandatoryThreshold: FeatureFlag<Int> =
            int(
                keySuffix = "subscriptionMandatoryThreshold",
                name = "Subscription mandatory threshold",
                description = "Total message count at which to show subscription nudge in chat",
                defaultValue = 70,
            )
        val MaxBotCountForCta: FeatureFlag<Int> =
            int(
                keySuffix = "maxBotCountForCta",
                name = "Max bot count for CTA",
                description = "Maximum number of bots before hiding the create influencer CTA",
                defaultValue = 3,
            )
        val MaxVisibleBotUsernames: FeatureFlag<Int> =
            int(
                keySuffix = "maxVisibleBotUsernames",
                name = "Max visible bot usernames",
                description = "Maximum number of bot usernames to show before '+N More'",
                defaultValue = 2,
            )
        val ChatAsHumanCreatorEnabled: FeatureFlag<Boolean> =
            boolean(
                keySuffix = "chatAsHumanCreatorEnabled",
                name = "Chat as Human Creator (takeover)",
                description = "When ON, exposes the Take Over toggle inside a creator's inbox conversation view " +
                    "and renders user-side join/leave system banners. When OFF, the creator sees the legacy " +
                    "'switch to your human profile' prompt and system banners are not rendered.",
                defaultValue = false,
            )
        val SseStreamingEnabled: FeatureFlag<Boolean> =
            boolean(
                keySuffix = "sseStreamingEnabled",
                name = "SSE token streaming",
                description = "When ON, text-only AI replies stream token-by-token via " +
                    "POST .../messages/stream (Phase 2.7). When OFF, sends use the existing " +
                    "POST .../messages endpoint. Carve-outs (media attachments, active takeover, " +
                    "backend 404) always fall back to non-streaming regardless of this flag.",
                // LOCAL-ONLY OVERRIDE for SSE testing: flip to `true` for dev only — revert
                // to `false` before any commit. The PR on origin must keep defaultValue = false
                // until backend cutover + GA.
                defaultValue = false,
            )
        val SoulFileCoachEnabled: FeatureFlag<Boolean> =
            boolean(
                keySuffix = "soulFileCoachEnabled",
                name = "Soul File Coach (Phase 7.5)",
                description = "When ON, exposes the 'Make your AI Influencer better' button on the creator's " +
                    "own bot profile. Tap → starts a coach session at POST /api/v1/creator/coach/conversations/{bot_id} " +
                    "and navigates into a coach chat where the creator iterates on the bot's system instructions. " +
                    "Coach replies can include a structured proposal which the creator can Apply with one tap " +
                    "(POST .../apply) to update the bot. When OFF, the button is hidden and the feature is dormant. " +
                    "PR keeps defaultValue = false until backend cutover + GA.",
                defaultValue = false,
            )
    }
}
