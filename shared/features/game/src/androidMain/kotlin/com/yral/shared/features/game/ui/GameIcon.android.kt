package com.yral.shared.features.game.ui

import androidx.compose.ui.text.EmojiSupportMatch
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle

internal actual fun emojiTextStyle(): TextStyle =
    TextStyle(
        platformStyle =
            PlatformTextStyle(
                emojiSupportMatch = EmojiSupportMatch.Default,
            ),
    )
