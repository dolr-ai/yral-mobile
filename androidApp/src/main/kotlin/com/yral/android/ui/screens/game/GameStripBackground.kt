package com.yral.android.ui.screens.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yral.android.ui.design.YralColors.SmileyGameCardBackground

@Composable
fun GameStripBackground(
    modifier: Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.SpaceBetween,
    content: @Composable () -> Unit,
) {
    Row(
        modifier =
            modifier
                .padding(
                    horizontal = 18.dp,
                    vertical = 16.dp,
                ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        color = SmileyGameCardBackground,
                        shape = RoundedCornerShape(size = 49.dp),
                    ).padding(
                        horizontal = 12.dp,
                        vertical = 9.dp,
                    ),
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
        }
    }
}
