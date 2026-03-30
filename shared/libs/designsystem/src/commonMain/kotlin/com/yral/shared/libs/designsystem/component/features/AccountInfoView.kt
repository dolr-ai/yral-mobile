package com.yral.shared.libs.designsystem.component.features

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.shared.core.session.AccountInfo
import com.yral.shared.libs.designsystem.component.CreateInfluencerButton
import com.yral.shared.libs.designsystem.component.YralButton
import com.yral.shared.libs.designsystem.component.YralButtonState
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.component.formatAbbreviation
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.designsystem.theme.YralDimens
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.libs.designsystem.generated.resources.Res
import yral_mobile.shared.libs.designsystem.generated.resources.anonymous_account_setup
import yral_mobile.shared.libs.designsystem.generated.resources.edit_profile
import yral_mobile.shared.libs.designsystem.generated.resources.follow
import yral_mobile.shared.libs.designsystem.generated.resources.followers
import yral_mobile.shared.libs.designsystem.generated.resources.following
import yral_mobile.shared.libs.designsystem.generated.resources.ic_bot_username_star
import yral_mobile.shared.libs.designsystem.generated.resources.ic_thunder
import yral_mobile.shared.libs.designsystem.generated.resources.login
import yral_mobile.shared.libs.designsystem.generated.resources.pro
import yral_mobile.shared.libs.designsystem.generated.resources.share_profile
import yral_mobile.shared.libs.designsystem.generated.resources.subscribe
import yral_mobile.shared.libs.designsystem.generated.resources.subscribed
import yral_mobile.shared.libs.designsystem.generated.resources.talk_to_me

@Suppress("LongMethod", "LongParameterList", "CyclomaticComplexMethod")
@Composable
fun AccountInfoView(
    accountInfo: AccountInfo,
    totalFollowers: Long? = null,
    totalFollowing: Long? = null,
    bio: String? = null,
    isSocialSignIn: Boolean,
    showLogin: Boolean = true,
    loginText: String = stringResource(Res.string.login),
    loginSubText: String = stringResource(Res.string.anonymous_account_setup),
    onLoginClicked: () -> Unit = {},
    showEditProfile: Boolean = false,
    onEditProfileClicked: () -> Unit = {},
    showShareProfile: Boolean = false,
    onShareProfileClicked: () -> Unit = {},
    showFollow: Boolean = false,
    isFollowing: Boolean = false,
    isFollowInProgress: Boolean = false,
    isAiInfluencer: Boolean = false,
    isTalkToMeInProgress: Boolean = false,
    onFollowClicked: () -> Unit = {},
    onFollowersClick: (() -> Unit)? = null,
    onFollowingClick: (() -> Unit)? = null,
    onTalkToMeClicked: () -> Unit = {},
    showSubscribe: Boolean = false,
    onSubscribeClicked: () -> Unit = {},
    isProUser: Boolean = false,
    showCreateInfluencerCta: Boolean = false,
    onCreateInfluencerClick: () -> Unit = {},
    botUsernames: List<String> = emptyList(),
    createdByUsername: String? = null,
    maxVisibleBotUsernames: Int = 2,
    onUsernameClick: ((String) -> Unit)? = null,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    top = YralDimens.paddingLg,
                    end = 16.dp,
                ),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileImageView(
                imageUrl = accountInfo.profilePic,
                applyFrame = isProUser,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.Start),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = accountInfo.displayName,
                        style = LocalAppTopography.current.baseSemiBold,
                        color = YralColors.NeutralTextPrimary,
                    )
                    if (isProUser) {
                        ProChip()
                    }
                }
                Row {
                    totalFollowers?.let {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top),
                            horizontalAlignment = Alignment.Start,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .clickable(enabled = onFollowersClick != null) {
                                        onFollowersClick?.invoke()
                                    },
                        ) {
                            Text(
                                text = formatAbbreviation(totalFollowers, 0),
                                style = LocalAppTopography.current.mdSemiBold,
                                color = YralColors.NeutralTextPrimary,
                            )
                            Text(
                                text = stringResource(Res.string.followers),
                                style = LocalAppTopography.current.baseRegular,
                                color = YralColors.NeutralTextPrimary,
                            )
                        }
                    }
                    totalFollowing?.let {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top),
                            horizontalAlignment = Alignment.Start,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .clickable(enabled = onFollowingClick != null) {
                                        onFollowingClick?.invoke()
                                    },
                        ) {
                            Text(
                                text = formatAbbreviation(totalFollowing, 0),
                                style = LocalAppTopography.current.mdSemiBold,
                                color = YralColors.NeutralTextPrimary,
                            )
                            Text(
                                text = stringResource(Res.string.following),
                                style = LocalAppTopography.current.baseRegular,
                                color = YralColors.NeutralTextPrimary,
                            )
                        }
                    }
                }
                // Bio moved below the row to align with profile picture edge
            }
        }
        bio?.takeUnless { it.isBlank() }?.let { nonEmptyBio ->
            Text(
                text = nonEmptyBio,
                style = LocalAppTopography.current.regRegular,
                color = YralColors.NeutralTextPrimary,
            )
        }
        if (botUsernames.isNotEmpty()) {
            BotUsernamesRow(
                botUsernames = botUsernames,
                maxVisible = maxVisibleBotUsernames,
                onUsernameClick = onUsernameClick,
            )
        }
        createdByUsername
            ?.takeUnless { it.isBlank() }
            ?.let { username ->
                CreatedByRow(
                    username = username,
                    onClick = onUsernameClick?.let { cb -> { cb(username) } },
                )
            }
        when {
            !isSocialSignIn && showLogin -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
                ) {
                    YralGradientButton(
                        text = loginText,
                        onClick = onLoginClicked,
                    )
                    Text(
                        text = loginSubText,
                        style = LocalAppTopography.current.baseRegular,
                        color = YralColors.NeutralTextPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            showEditProfile -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ProfileButton(
                        text = stringResource(Res.string.edit_profile),
                        modifier = Modifier.weight(1f),
                        onClick = onEditProfileClicked,
                    )
                    if (showShareProfile) {
                        ProfileButton(
                            text = stringResource(Res.string.share_profile),
                            modifier = Modifier.weight(1f),
                            onClick = onShareProfileClicked,
                        )
                    }
                }
            }
            showFollow -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    val followText =
                        when {
                            isFollowInProgress -> ""
                            isFollowing -> stringResource(Res.string.following)
                            else -> stringResource(Res.string.follow)
                        }
                    val buttonState =
                        if (isFollowInProgress) YralButtonState.Loading else YralButtonState.Enabled
                    if (isFollowing) {
                        YralButton(
                            modifier = Modifier.weight(1f),
                            text = followText,
                            borderColor = YralColors.Neutral700,
                            borderWidth = 1.dp,
                            backgroundColor = YralColors.Neutral800,
                            textStyle =
                                LocalAppTopography
                                    .current
                                    .baseSemiBold
                                    .copy(
                                        color = YralColors.Grey50,
                                    ),
                            onClick = onFollowClicked,
                            buttonState = buttonState,
                            buttonHeight = 40.dp,
                        )
                    } else {
                        YralGradientButton(
                            modifier = Modifier.weight(1f),
                            text = followText,
                            onClick = onFollowClicked,
                            buttonState = buttonState,
                            buttonHeight = 40.dp,
                            textStyle =
                                LocalAppTopography
                                    .current
                                    .baseSemiBold
                                    .copy(
                                        color = YralColors.Grey50,
                                        textAlign = TextAlign.Center,
                                    ),
                        )
                    }
                    if (showSubscribe) {
                        SubscribeButton(
                            modifier = Modifier.weight(1f),
                            onClick = onSubscribeClicked,
                        )
                    }
                    if (isAiInfluencer) {
                        val talkButtonState =
                            if (isTalkToMeInProgress) YralButtonState.Loading else YralButtonState.Enabled
                        val talkText =
                            if (isTalkToMeInProgress) {
                                ""
                            } else {
                                stringResource(Res.string.talk_to_me)
                            }
                        YralButton(
                            modifier = Modifier.weight(1f),
                            text = talkText,
                            backgroundColor = YralColors.Grey50,
                            textStyle =
                                LocalAppTopography
                                    .current
                                    .baseSemiBold
                                    .copy(
                                        color = YralColors.Pink300,
                                    ),
                            onClick = onTalkToMeClicked,
                            buttonState = talkButtonState,
                            buttonHeight = 40.dp,
                        )
                    }
                }
            }
        }
        if (showCreateInfluencerCta) {
            CreateInfluencerButton(
                modifier = Modifier.fillMaxWidth().height(45.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                alignIconToEnd = true,
                onClick = onCreateInfluencerClick,
            )
        }
        Spacer(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(YralColors.Divider)
                    .padding(bottom = YralDimens.paddingLg),
        )
    }
}

@Composable
private fun BotUsernamesRow(
    botUsernames: List<String>,
    maxVisible: Int = 2,
    onUsernameClick: ((String) -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val visible = if (expanded) botUsernames else botUsernames.take(maxVisible)
    val remainingCount = (botUsernames.size - visible.size).coerceAtLeast(0)
    Row(
        modifier = Modifier.padding(top = 0.dp, bottom = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        visible.forEachIndexed { index, username ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier.clickable(enabled = onUsernameClick != null) {
                        onUsernameClick?.invoke(username)
                    },
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_bot_username_star),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "@$username",
                    style = LocalAppTopography.current.regBold,
                    color = YralColors.Pink200,
                )
            }
            if (index != visible.lastIndex || remainingCount > 0) {
                Spacer(modifier = Modifier.width(12.dp))
            }
        }
        if (remainingCount > 0) {
            Text(
                text = "+$remainingCount More",
                style = LocalAppTopography.current.regSemiBold,
                color = YralColors.BlueTextPrimary,
                modifier = Modifier.clickable { expanded = true },
            )
        }
    }
}

@Composable
private fun CreatedByRow(
    username: String,
    onClick: (() -> Unit)? = null,
) {
    val text =
        buildAnnotatedString {
            append("Created by ")
            pushStyle(SpanStyle(color = YralColors.BlueTextPrimary))
            append("@")
            append(username)
            pop()
        }
    Text(
        text = text,
        style = LocalAppTopography.current.baseRegular,
        color = YralColors.Grey50,
        modifier =
            Modifier
                .padding(top = 0.dp, bottom = 0.dp)
                .clickable(enabled = onClick != null) { onClick?.invoke() },
    )
}

@Composable
private fun ProfileButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(8.dp),
        color = YralColors.Neutral800,
        contentColor = YralColors.NeutralTextPrimary,
        border = BorderStroke(1.dp, YralColors.Neutral700),
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = LocalAppTopography.current.baseSemiBold,
                color = YralColors.NeutralTextPrimary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun SubscribeButton(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    buttonState: SubscribeButtonState = SubscribeButtonState.Subscribe,
    onClick: () -> Unit,
) {
    val isSubscribed = buttonState == SubscribeButtonState.Subscribed
    val showLoading = isLoading && !isSubscribed
    Surface(
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(4.dp),
        color = if (isSubscribed) YralColors.Neutral800 else YralColors.Yellow400,
        border =
            BorderStroke(
                width = 1.dp,
                color = if (isSubscribed) YralColors.Neutral700 else YralColors.Yellow200,
            ),
        onClick = if (isSubscribed || showLoading) ({}) else onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = YralColors.Yellow200,
                    strokeWidth = 2.dp,
                )
            } else if (isSubscribed) {
                Text(
                    text = stringResource(Res.string.subscribed),
                    style = LocalAppTopography.current.regSemiBold,
                    color = YralColors.Grey50,
                )
            } else {
                Text(
                    text = stringResource(Res.string.subscribe),
                    style = LocalAppTopography.current.regSemiBold,
                    color = YralColors.Yellow200,
                )
                Image(
                    painter = painterResource(Res.drawable.ic_thunder),
                    contentDescription = null,
                    contentScale = ContentScale.Inside,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

enum class SubscribeButtonState {
    Subscribe,
    Subscribed,
}

@Composable
private fun ProChip() {
    Row(
        horizontalArrangement =
            Arrangement.spacedBy(
                2.dp,
                Alignment.CenterHorizontally,
            ),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .width(56.dp)
                .height(22.dp)
                .border(
                    width = 1.dp,
                    brush = proBrush(),
                    shape = RoundedCornerShape(size = 100.dp),
                ),
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_thunder),
            contentDescription = "bolt",
            contentScale = ContentScale.Inside,
            modifier = Modifier.width(12.dp).height(12.dp),
        )
        Text(
            text = stringResource(Res.string.pro),
            style = LocalAppTopography.current.smMedium,
            color = YralColors.Yellow200,
        )
    }
}
