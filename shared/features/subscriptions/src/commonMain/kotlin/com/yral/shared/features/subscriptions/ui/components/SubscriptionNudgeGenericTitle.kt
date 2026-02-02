package com.yral.shared.features.subscriptions.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.component.features.proBrush
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.subscriptions.generated.resources.Res
import yral_mobile.shared.features.subscriptions.generated.resources.create_without_limits
import yral_mobile.shared.features.subscriptions.generated.resources.go_pro
import yral_mobile.shared.libs.designsystem.generated.resources.ic_thunder
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

@Composable
fun SubscriptionNudgeGenericTitle(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.go_pro),
                style = LocalAppTopography.current.xxlBold.copy(brush = proBrush()),
            )
            Image(
                painter = painterResource(DesignRes.drawable.ic_thunder),
                contentDescription = null,
                contentScale = ContentScale.Inside,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = stringResource(Res.string.create_without_limits),
            style = LocalAppTopography.current.xxlBold,
            color = Color.White,
        )
    }
}
