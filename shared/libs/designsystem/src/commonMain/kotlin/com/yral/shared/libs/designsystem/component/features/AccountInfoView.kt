package com.yral.shared.libs.designsystem.component.features

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.shared.core.session.AccountInfo
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.component.YralButton
import com.yral.shared.libs.designsystem.component.YralButtonState
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.component.formatAbbreviation
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.designsystem.theme.YralDimens
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.libs.designsystem.generated.resources.Res
import yral_mobile.shared.libs.designsystem.generated.resources.anonymous_account_setup
import yral_mobile.shared.libs.designsystem.generated.resources.edit_profile
import yral_mobile.shared.libs.designsystem.generated.resources.follow
import yral_mobile.shared.libs.designsystem.generated.resources.followers
import yral_mobile.shared.libs.designsystem.generated.resources.following
import yral_mobile.shared.libs.designsystem.generated.resources.login

@Suppress("LongMethod", "LongParameterList")
@Composable
fun AccountInfoView(
    accountInfo: AccountInfo,
    totalFollowers: Long? = null,
    totalFollowing: Long? = null,
    bio: String? = null,
    isSocialSignIn: Boolean,
    showLoginPrompt: Boolean = true,
    loginText: String = stringResource(Res.string.login),
    loginSubText: String = stringResource(Res.string.anonymous_account_setup),
    onLoginClicked: () -> Unit = {},
    showEditProfile: Boolean = false,
    onEditProfileClicked: () -> Unit = {},
    showFollow: Boolean = false,
    isFollowing: Boolean = false,
    isFollowInProgress: Boolean = false,
    onFollowClicked: () -> Unit = {},
    onFollowersClick: (() -> Unit)? = null,
    onFollowingClick: (() -> Unit)? = null,
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
            YralAsyncImage(
                imageUrl = accountInfo.profilePic,
                modifier = Modifier.size(76.dp),
                contentScale = ContentScale.Crop,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = accountInfo.displayName,
                    style = LocalAppTopography.current.baseMedium,
                    color = YralColors.NeutralTextSecondary,
                )
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
        when {
            !isSocialSignIn && showLoginPrompt -> {
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
                ProfileButton(
                    text = stringResource(Res.string.edit_profile),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onEditProfileClicked,
                )
            }
            showFollow -> {
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
                        modifier = Modifier.fillMaxWidth(),
                        text = followText,
                        borderColor = YralColors.Neutral700,
                        borderWidth = 1.dp,
                        backgroundColor = YralColors.Neutral800,
                        textStyle = TextStyle(color = YralColors.NeutralTextPrimary),
                        onClick = onFollowClicked,
                        buttonState = buttonState,
                    )
                } else {
                    YralGradientButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = followText,
                        onClick = onFollowClicked,
                        buttonState = buttonState,
                    )
                }
            }
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
