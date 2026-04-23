@file:Suppress("MagicNumber")

package com.yral.shared.app.ui.screens.profile

import androidx.compose.foundation.Image
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.yral.shared.app.ui.screens.profile.nav.ProfileComponent
import com.yral.shared.features.account.ui.AccountScreen
import com.yral.shared.features.account.viewmodel.AccountsViewModel
import com.yral.shared.libs.designsystem.component.YralGradientButton
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.app.generated.resources.Res
import yral_mobile.shared.app.generated.resources.ic_menu
import yral_mobile.shared.app.generated.resources.locked_profile
import yral_mobile.shared.app.generated.resources.locked_profile_subtitle
import yral_mobile.shared.app.generated.resources.locked_profile_title
import yral_mobile.shared.libs.designsystem.generated.resources.login
import yral_mobile.shared.libs.designsystem.generated.resources.my_profile
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Composable
internal fun LockedProfileStackScreen(
    component: ProfileComponent,
    modifier: Modifier = Modifier,
    accountsViewModel: AccountsViewModel,
    onAlertsToggleRequest: suspend (Boolean) -> Boolean,
    onLoginClick: () -> Unit,
) {
    Children(
        stack = component.stack,
        modifier = modifier,
    ) { child ->
        when (val instance = child.instance) {
            is ProfileComponent.Child.Main,
            is ProfileComponent.Child.EditProfile,
            -> {
                LockedProfileScreen(
                    onLoginClick = onLoginClick,
                    onMenuClick = component::openAccount,
                )
            }

            is ProfileComponent.Child.Account -> {
                AccountScreen(
                    component = instance.component,
                    viewModel = accountsViewModel,
                    onAlertsToggleRequest = onAlertsToggleRequest,
                )
            }
        }
    }
}

@Composable
private fun LockedProfileScreen(
    onLoginClick: () -> Unit,
    onMenuClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
        LockedProfileHeader(onMenuClick = onMenuClick)
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 31.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LockedProfileIllustration()
                Spacer(modifier = Modifier.height(23.dp))
                Text(
                    text = stringResource(Res.string.locked_profile_title),
                    style = LocalAppTopography.current.lgBold,
                    color = YralColors.NeutralTextPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.locked_profile_subtitle),
                    style = LocalAppTopography.current.baseRegular,
                    color = YralColors.NeutralTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(32.dp))
                YralGradientButton(
                    text = stringResource(DesignRes.string.login),
                    onClick = onLoginClick,
                    modifier = Modifier.fillMaxWidth(),
                    buttonHeight = 43.dp,
                )
            }
        }
    }
}

@Composable
private fun LockedProfileHeader(onMenuClick: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 14.dp, end = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(DesignRes.string.my_profile),
            style = LocalAppTopography.current.xlBold,
            color = YralColors.NeutralTextPrimary,
        )
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .clickable(onClick = onMenuClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_menu),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun LockedProfileIllustration() {
    Image(
        painter = painterResource(Res.drawable.locked_profile),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(128.dp),
    )
}
