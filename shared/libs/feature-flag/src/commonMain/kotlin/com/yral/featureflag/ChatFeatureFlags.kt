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
                description =
                    "When ON, exposes the Take Over toggle inside a creator's inbox conversation view " +
                        "and renders user-side join/leave system banners. When OFF, the creator sees the legacy " +
                        "'switch to your human profile' prompt and system banners are not rendered.",
                defaultValue = false,
            )
        val SseStreamingEnabled: FeatureFlag<Boolean> =
            boolean(
                keySuffix = "sseStreamingEnabled",
                name = "SSE token streaming",
                description =
                    "When ON, text-only AI replies stream token-by-token via " +
                        "POST .../messages/stream (Phase 2.7). When OFF, sends use the existing " +
                        "POST .../messages endpoint. Carve-outs (media attachments, active takeover, " +
                        "backend 404) always fall back to non-streaming regardless of this flag.",
                // LOCAL-ONLY OVERRIDE for SSE testing: flip to `true` for dev only — revert
                // to `false` before any commit. The PR on origin must keep defaultValue = false
                // until backend cutover + GA.
                defaultValue = false,
            )
        val H2hChatEnabled: FeatureFlag<Boolean> =
            boolean(
                keySuffix = "h2hChatEnabled",
                name = "Human-to-Human chat",
                description = "When ON, exposes the Send Message button on other-user profile screens and " +
                    "lets users open 1:1 H2H conversations via POST /api/v1/chat/human/conversations. When " +
                    "OFF, the button is hidden and any H2H conversations already in the user's inbox are " +
                    "filtered out at render time so the feature is fully dormant. PR keeps defaultValue = " +
                    "false until backend cutover + GA.",
                defaultValue = false,
            )
        val SoulFileCoachEnabled: FeatureFlag<Boolean> =
            boolean(
                keySuffix = "soulFileCoachEnabled",
                name = "Soul File Coach (Phase 7.5)",
                description =
                    "When ON, exposes the 'Make your AI Influencer better' button on the creator's " +
                        "own bot profile. Tap → starts a coach session at " +
                        "POST /api/v1/creator/coach/conversations/{bot_id} and navigates into a coach chat where " +
                        "the creator iterates on the bot's system instructions. Coach replies can include a " +
                        "structured proposal which the creator can Apply with one tap (POST .../apply) to update " +
                        "the bot. When OFF, the button is hidden and the feature is dormant. PR keeps " +
                        "defaultValue = false until backend cutover + GA — matches the H2H/Audio/SSE/" +
                        "Chat-as-Human/Video Ideas dormant-default pattern.",
                defaultValue = false,
            )
        val AudioRecordingEnabled: FeatureFlag<Boolean> =
            boolean(
                keySuffix = "audioRecordingEnabled",
                name = "In-chat voice-message recording",
                description = "When ON, exposes the mic button in the chat input action row so users can " +
                    "record + send voice messages (Phase 1.7b). Recording is .m4a / audio/mp4 written to " +
                    "platform cache, uploaded via POST /api/v1/media/upload type=audio, sent as a chat " +
                    "message with audio_url + message_type=audio. Backend transcribes via Gemini and the " +
                    "AI replies based on the transcription. When OFF, the mic button is hidden and the " +
                    "feature is fully dormant. PR keeps defaultValue = false until backend cutover + GA.",
                defaultValue = false,
            )
        val VideoIdeasEnabled: FeatureFlag<Boolean> =
            boolean(
                keySuffix = "videoIdeasEnabled",
                name = "Daily 5 Video Ideas tab (Phase 22.3)",
                description = "When ON, exposes the third \"Ideas\" tab (lightbulb icon) on a creator's own " +
                    "AI-influencer profile so they can see 5 fresh AI-generated video ideas daily and " +
                    "one-tap Create headlessly into Drafts via the existing video-gen pipeline. The data " +
                    "calls hit GET/POST /api/v1/influencers/{id}/video-ideas on agent.rishi.yral.com — " +
                    "endpoints that do not exist on the production chat-ai backend. When OFF, the tab is " +
                    "hidden entirely and the bot profile shows the legacy 2-tab UX (Published + Drafts); " +
                    "no calls are made to the missing endpoints. PR keeps defaultValue = false until " +
                    "backend cutover + GA — same shape as H2H/Audio/SSE/Chat-as-Human/Coach gates.",
                defaultValue = false,
            )
        val DiscoveryFeedV2Enabled: FeatureFlag<Boolean> =
            boolean(
                keySuffix = "discoveryFeedV2Enabled",
                name = "Discovery feed v2 (agent.rishi.yral.com)",
                description = "When ON, the influencer discovery screen fetches its list from " +
                    "GET https://agent.rishi.yral.com/api/v2/discovery/influencer-feed (with JWT " +
                    "when the user is logged in). When OFF, it calls the existing Anshuman recsys " +
                    "host (https://recsys-influencer-feed.ansuman.yral.com/api/v1/influencer-feed) " +
                    "exactly as today. The response envelope is byte-compatible with Anshuman's so " +
                    "no parsing changes. Rollback is a server-side flag flip; the OFF path is " +
                    "completely unchanged. PR keeps defaultValue = false until backend cutover + GA " +
                    "— same shape as the other agent-API feature gates.",
                defaultValue = false,
            )
    }
}
// Initiate action
