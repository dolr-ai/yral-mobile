package com.yral.shared.features.chat.ui.conversation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.yral.shared.features.chat.domain.models.ConversationInfluencer
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.component.YralContextMenu
import com.yral.shared.libs.designsystem.component.YralContextMenuItem
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.chat.generated.resources.Res
import yral_mobile.shared.features.chat.generated.resources.access_expires_in
import yral_mobile.shared.features.chat.generated.resources.back
import yral_mobile.shared.features.chat.generated.resources.clear_chat
import yral_mobile.shared.features.chat.generated.resources.share_profile
import yral_mobile.shared.features.chat.generated.resources.view_profile
import yral_mobile.shared.libs.designsystem.generated.resources.arrow_left
import yral_mobile.shared.libs.designsystem.generated.resources.ic_more
import yral_mobile.shared.libs.designsystem.generated.resources.ic_share
import yral_mobile.shared.libs.designsystem.generated.resources.ic_x_circle
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Composable
internal fun ChatHeader(
    influencer: ConversationInfluencer?,
    onBackClick: () -> Unit,
    onProfileClick: (ConversationInfluencer) -> Unit,
    onClearChat: () -> Unit,
    onShareProfile: () -> Unit,
    accessExpiresInText: String? = null,
    isAccessExpiringSoon: Boolean = false,
    isBotAccount: Boolean,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(YralColors.PrimaryContainer)
                .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left side: Back arrow + Profile info
            LeftPart(
                influencer = influencer,
                onBackClick = onBackClick,
                onProfileClick = onProfileClick,
                accessExpiresInText = accessExpiresInText,
                isAccessExpiringSoon = isAccessExpiringSoon,
            )

            // Right side: More icon with context menu
            RightPart(onClearChat = onClearChat, onShareProfile = onShareProfile, isBotAccount = isBotAccount)
        }
    }
}

@Composable
private fun LeftPart(
    influencer: ConversationInfluencer?,
    onBackClick: () -> Unit,
    onProfileClick: (ConversationInfluencer) -> Unit,
    accessExpiresInText: String?,
    isAccessExpiringSoon: Boolean,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(DesignRes.drawable.arrow_left),
                contentDescription = stringResource(Res.string.back),
                modifier =
                    Modifier
                        .size(24.dp)
                        .clickable(onClick = onBackClick),
                contentScale = ContentScale.None,
            )
            influencer?.let { inf ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    YralAsyncImage(
                        imageUrl = inf.avatarUrl,
                        modifier = Modifier.size(40.dp).align(Alignment.Top),
                        contentScale = ContentScale.Crop,
                        shape = CircleShape,
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = inf.displayName,
                            style = LocalAppTopography.current.mdSemiBold,
                            color = YralColors.NeutralIconsActive,
                        )
                        HeaderSubtitle(
                            onProfileClick = { onProfileClick(inf) },
                        )
                    }
                }
            }
        }
        if (accessExpiresInText != null) {
            AccessExpiry(accessExpiresInText, isAccessExpiringSoon)
        }
    }
}

@Composable
private fun AccessExpiry(
    accessExpiresInText: String?,
    isAccessExpiringSoon: Boolean,
) {
    val prefix = stringResource(Res.string.access_expires_in, "")
    Text(
        text =
            buildAnnotatedString {
                append(prefix)
                withStyle(
                    SpanStyle(
                        color = if (isAccessExpiringSoon) YralColors.ErrorRed else YralColors.Neutral500,
                    ),
                ) {
                    append(accessExpiresInText)
                }
            },
        style = LocalAppTopography.current.regRegular,
        color = YralColors.Neutral500,
        modifier = Modifier.padding(start = 24.dp),
    )
}

@Composable
private fun HeaderSubtitle(onProfileClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(onClick = onProfileClick),
    ) {
        Text(
            text = stringResource(Res.string.view_profile),
            style = LocalAppTopography.current.baseRegular,
            color = YralColors.BlueTextPrimary,
        )
        Image(
            painter = painterResource(DesignRes.drawable.arrow_left),
            contentDescription = null,
            modifier =
                Modifier
                    .size(10.dp)
                    .graphicsLayer { rotationZ = ARROW_ROTATION },
            colorFilter = ColorFilter.tint(YralColors.BlueTextPrimary),
            contentScale = ContentScale.None,
        )
    }
}

@Composable
private fun RightPart(
    onClearChat: () -> Unit,
    onShareProfile: () -> Unit,
    isBotAccount: Boolean,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val menuItems =
            buildList {
                add(
                    YralContextMenuItem(
                        text = stringResource(Res.string.share_profile),
                        icon = DesignRes.drawable.ic_share,
                        onClick = onShareProfile,
                    ),
                )
                if (!isBotAccount) {
                    add(
                        YralContextMenuItem(
                            text = stringResource(Res.string.clear_chat),
                            icon = DesignRes.drawable.ic_x_circle,
                            onClick = onClearChat,
                        ),
                    )
                }
            }
        YralContextMenu(
            items = menuItems,
            triggerIcon = DesignRes.drawable.ic_more,
            triggerSize = 24.dp,
            menuIconSize = 20.dp,
        )
    }
}
