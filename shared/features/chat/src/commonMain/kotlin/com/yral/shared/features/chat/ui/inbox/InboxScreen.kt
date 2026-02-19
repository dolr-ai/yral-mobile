package com.yral.shared.features.chat.ui.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.yral.shared.features.chat.nav.inbox.InboxComponent
import com.yral.shared.features.chat.viewmodel.InboxViewModel
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors

@Suppress("UnusedParameter")
@Composable
fun InboxScreen(
    component: InboxComponent,
    viewModel: InboxViewModel,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Coming Soon",
            style = LocalAppTopography.current.regMedium,
            color = YralColors.NeutralTextSecondary,
        )
    }
}
