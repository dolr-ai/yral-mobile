package com.yral.featureflag.sample

import com.yral.featureflag.core.FeatureFlag
import com.yral.featureflag.core.FlagAudience
import com.yral.featureflag.core.FlagGroup

object AppFeatureFlags {
    object Onboarding : FlagGroup(keyPrefix = "onboarding", defaultAudience = FlagAudience.INTERNAL_QA) {
        val UseNewFlow: FeatureFlag<Boolean> =
            boolean(
                keySuffix = "new_flow",
                name = "Enable New Onboarding",
                description = "Switch to the experimental onboarding flow.",
                defaultValue = false,
            )

        val WelcomeText: FeatureFlag<String> =
            string(
                keySuffix = "welcome_text",
                name = "Welcome Text",
                description = "Text shown on the first screen of onboarding.",
                defaultValue = "Welcome to Yral",
            )
    }

    object Profile : FlagGroup(keyPrefix = "profile", defaultAudience = FlagAudience.INTERNAL_QA) {
        val EnableAvatarEditor: FeatureFlag<Boolean> =
            boolean(
                keySuffix = "avatar_editor_enabled",
                name = "Enable Avatar Editor",
                description = "Turns on the new avatar editor UI.",
                defaultValue = false,
            )

        val MaxBioLength: FeatureFlag<Int> =
            int(
                keySuffix = "max_bio_length",
                name = "Max Bio Length",
                description = "Maximum number of characters allowed in bio.",
                defaultValue = 160,
            )
    }
}
