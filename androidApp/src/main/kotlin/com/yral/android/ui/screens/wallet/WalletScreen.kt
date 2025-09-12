package com.yral.android.ui.screens.wallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.screens.profile.AccountInfoSection
import com.yral.android.ui.screens.wallet.nav.WalletComponent
import com.yral.shared.core.session.SessionManager
import com.yral.shared.features.auth.utils.getAccountInfo
import org.koin.compose.koinInject

@Composable
fun WalletScreen(
    @Suppress("UnusedParameter")
    component: WalletComponent,
    modifier: Modifier = Modifier,
    sessionManager: SessionManager = koinInject(),
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WalletHeader()
        Spacer(modifier = Modifier.height(8.dp))
        sessionManager.getAccountInfo()?.let { info ->
            AccountInfoSection(accountInfo = info)
        }
    }
}

@Composable
private fun WalletHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.my_wallet),
            style = LocalAppTopography.current.xlBold,
            color = YralColors.NeutralTextPrimary,
        )
    }
}
