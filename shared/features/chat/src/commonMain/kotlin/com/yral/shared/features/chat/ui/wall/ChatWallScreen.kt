package com.yral.shared.features.chat.ui.wall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.collectAsLazyPagingItems
import com.yral.shared.features.chat.domain.models.Influencer
import com.yral.shared.features.chat.nav.wall.ChatWallComponent
import com.yral.shared.features.chat.viewmodel.ChatWallViewModel
import com.yral.shared.libs.designsystem.component.YralAsyncImage
import com.yral.shared.libs.designsystem.component.YralButton
import com.yral.shared.libs.designsystem.component.getSVGImageModel
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors

@Composable
fun ChatWallScreen(
    component: ChatWallComponent,
    viewModel: ChatWallViewModel,
    modifier: Modifier = Modifier,
) {
    val influencers = viewModel.influencers.collectAsLazyPagingItems()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = "Chat with Influencers",
            style = LocalAppTopography.current.xlBold,
            color = YralColors.Grey50,
            modifier = Modifier.padding(bottom = 22.dp),
        )
        Text(
            text = "Chat, learn, and vibe with your favorite creators.",
            style = LocalAppTopography.current.baseRegular,
            color = YralColors.Grey0,
            modifier = Modifier.padding(bottom = 20.dp),
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(influencers.itemCount) { index ->
                influencers[index]?.let { influencer ->
                    InfluencerCard(
                        influencer = influencer,
                        onClick = { component.openConversation(influencer.id) },
                    )
                }
            }
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun InfluencerCard(
    influencer: Influencer,
    onClick: () -> Unit,
) {
    val cardShape = MaterialTheme.shapes.medium

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(ChatWallScreenConstants.CARD_ASPECT_RATIO)
                .clip(cardShape)
                .background(
                    color = Color.Transparent,
                    shape = cardShape,
                ),
    ) {
        YralAsyncImage(
            imageUrl = influencer.avatarUrl,
            contentScale = ContentScale.Crop,
            shape = cardShape,
            backgroundColor = Color.DarkGray,
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(cardShape),
        )
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(10.dp),
        ) {
            Text(
                text = influencer.displayName.ifBlank { influencer.name },
                style = LocalAppTopography.current.mdSemiBold,
                color = YralColors.Grey0,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "@${influencer.name}",
                style = LocalAppTopography.current.smRegular,
                color = YralColors.Grey0,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            Text(
                text = influencer.description,
                style = LocalAppTopography.current.smMedium,
                color = YralColors.Grey0,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (influencer.isActive == "coming_soon") {
                Text(
                    text = "coming soon...",
                    style = LocalAppTopography.current.smMedium,
                    color = YralColors.Grey0,
                    maxLines = 1,
                )
            } else {
                YralButton(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                    text = "Talk To Me",
                    backgroundColor = YralColors.Grey50,
                    textStyle =
                        LocalAppTopography
                            .current
                            .smSemiBold
                            .copy(
                                color = YralColors.Pink300,
                            ),
                    paddingValues = PaddingValues(vertical = 6.dp),
                    buttonHeight = 26.dp,
                    onClick = onClick,
                )
            }
        }
    }
}

@Suppress("MagicNumber")
object ChatWallScreenConstants {
    const val CARD_ASPECT_RATIO = 0.75f
}
