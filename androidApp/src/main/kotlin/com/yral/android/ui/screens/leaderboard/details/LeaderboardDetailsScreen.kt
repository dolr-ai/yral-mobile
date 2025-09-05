package com.yral.android.ui.screens.leaderboard.details

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors

@Composable
fun LeaderboardDetailsScreen(component: LeaderboardDetailsComponent) {
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer),
    ) {
        Header { component.onBack() }
        Spacer(Modifier.height(22.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .background(YralColors.Neutral800)
                    .fillMaxWidth()
                    .height(38.dp)
                    .padding(start = 16.dp),
        ) {
            val chips = listOf("Aug 15", "Aug 16", "Aug 17", "Aug 18", "Aug 19", "Aug 20", "Aug 21")
            items(chips) {
                DateChip(date = it, isSelected = it == "Aug 15")
            }
        }
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .height(54.dp)
                .padding(vertical = 12.dp, horizontal = 12.dp),
    ) {
        Image(
            painter = painterResource(id = R.drawable.arrow_left),
            contentDescription = "image description",
            contentScale = ContentScale.None,
            modifier =
                Modifier
                    .size(24.dp)
                    .clickable { onBack() },
        )
        Text(
            text = stringResource(R.string.weekly_wins),
            style = LocalAppTopography.current.xlBold,
            modifier = Modifier.fillMaxWidth(),
            color = YralColors.NeutralIconsActive,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DateChip(
    date: String,
    isSelected: Boolean,
) {
    val chipBackground =
        if (isSelected) {
            YralColors.Neutral50
        } else {
            Color.Transparent
        }
    val chipTextColor =
        if (isSelected) {
            YralColors.Yellow400
        } else {
            YralColors.Yellow300
        }
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .height(30.dp)
                .background(color = chipBackground, shape = RoundedCornerShape(size = 32.dp))
                .padding(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 4.dp),
    ) {
        Text(
            text = date,
            style = LocalAppTopography.current.mdBold,
            color = chipTextColor,
        )
    }
}
