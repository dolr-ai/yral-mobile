package com.yral.android.ui.components.hashtagInput

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors

@Composable
fun HashtagChip(
    tag: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .padding(start = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(YralColors.Neutral700)
                .clickable { onClick() },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
        ) {
            Text(
                text = "#$tag",
                style = LocalAppTopography.current.regRegular,
                color = YralColors.Neutral50,
            )
        }
    }
}

@Composable
fun HashtagEditChip(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    setFocus: (Boolean) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    ChipInputField(
        value = value,
        onValueChange = onValueChange,
        onDone = onDone,
        placeholder = stringResource(R.string.add_hashtags),
        showHash = true,
        focusRequester = focusRequester,
        setFocus = setFocus,
    )
}
