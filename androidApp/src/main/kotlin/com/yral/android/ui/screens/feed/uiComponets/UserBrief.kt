package com.yral.android.ui.screens.feed.uiComponets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors
import com.yral.android.ui.screens.feed.uiComponets.UserBriefConstants.MAX_LINES_FOR_POST_DESCRIPTION
import com.yral.android.ui.widgets.YralAsyncImage
import io.ktor.http.Url

@Composable
fun UserBrief(
    profileImageUrl: Url?,
    principalId: String,
    postDescription: String,
    isPostDescriptionExpanded: Boolean,
    setPostDescriptionExpanded: (isExpanded: Boolean) -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier =
            modifier
                .padding(top = 22.dp, start = 16.dp, bottom = 22.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp, start = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            UserBriefProfileImage(profileImageUrl)
            UserBriefDetails(
                modifier = Modifier.weight(1f),
                principalId = principalId,
                postDescription = postDescription,
                isPostDescriptionExpanded = isPostDescriptionExpanded,
                setPostDescriptionExpanded = setPostDescriptionExpanded,
            )
        }
    }
}

@Composable
private fun UserBriefProfileImage(profileImageUrl: Url?) {
    YralAsyncImage(
        imageUrl = profileImageUrl.toString(),
        modifier = Modifier.size(40.dp),
        border = 2.dp,
        borderColor = YralColors.Pink300,
        backgroundColor = YralColors.ProfilePicBackground,
    )
}

@Composable
private fun UserBriefDetails(
    modifier: Modifier,
    principalId: String,
    postDescription: String,
    isPostDescriptionExpanded: Boolean,
    setPostDescriptionExpanded: (isExpanded: Boolean) -> Unit,
) {
    Column(
        modifier =
            modifier
                .clickable {
                    // setPostDescriptionExpanded(!isPostDescriptionExpanded)
                    setPostDescriptionExpanded(false)
                },
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = principalId,
            style = LocalAppTopography.current.feedCanisterId,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (postDescription.trim().isNotEmpty()) {
            if (isPostDescriptionExpanded) {
                val scrollState = rememberScrollState()
                val maxHeight =
                    LocalAppTopography
                        .current
                        .feedDescription
                        .lineHeight
                        .value * MAX_LINES_FOR_POST_DESCRIPTION
                Text(
                    modifier =
                        Modifier
                            .heightIn(max = maxHeight.dp)
                            .verticalScroll(scrollState),
                    text = postDescription,
                    style = LocalAppTopography.current.feedDescription,
                    color = Color.White,
                )
            } else {
                Text(
                    text = postDescription,
                    style = LocalAppTopography.current.feedDescription,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

object UserBriefConstants {
    const val MAX_LINES_FOR_POST_DESCRIPTION = 5
}
