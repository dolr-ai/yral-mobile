package com.yral.shared.features.chat.ui.wall

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yral.shared.features.chat.domain.models.DiscoverySearchResult
import com.yral.shared.libs.designsystem.component.YralGridImage
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.chat.generated.resources.Res
import yral_mobile.shared.features.chat.generated.resources.discovery_search_empty_subtitle
import yral_mobile.shared.features.chat.generated.resources.discovery_search_empty_title
import yral_mobile.shared.features.chat.generated.resources.discovery_search_failed

/**
 * Renders the search results overlay. Replaces the influencer grid in
 * the Discover tab whenever the user has typed something into the
 * search bar. Owns three terminal states: loading (no spinner — keeps
 * the area calm, just shows nothing), empty, and error. Tapping a row
 * delegates to [onResultClick] which the parent wires to the same
 * conversation-open path the influencer cards use.
 */
@Composable
fun DiscoverySearchResults(
    query: String,
    results: List<DiscoverySearchResult>,
    isLoading: Boolean,
    error: String?,
    onResultClick: (DiscoverySearchResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            error != null && results.isEmpty() ->
                CenteredMessage(
                    title = stringResource(Res.string.discovery_search_failed),
                    subtitle = error,
                )

            !isLoading && results.isEmpty() && query.isNotBlank() ->
                CenteredMessage(
                    title = stringResource(Res.string.discovery_search_empty_title, query),
                    subtitle = stringResource(Res.string.discovery_search_empty_subtitle),
                )

            else ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(results, key = { it.id }) { result ->
                        DiscoverySearchRow(
                            result = result,
                            onClick = { onResultClick(result) },
                        )
                    }
                }
        }
    }
}

@Composable
private fun DiscoverySearchRow(
    result: DiscoverySearchResult,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(48.dp).background(YralColors.Neutral800, CircleShape)) {
            YralGridImage(
                imageUrl = result.avatarUrl,
                shape = CircleShape,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
            Text(
                text = result.displayName,
                style = LocalAppTopography.current.baseSemiBold,
                color = YralColors.NeutralTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            result.subtitle?.let { subtitle ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = LocalAppTopography.current.smRegular,
                    color = YralColors.NeutralTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CenteredMessage(
    title: String,
    subtitle: String? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = LocalAppTopography.current.baseSemiBold,
            color = YralColors.NeutralTextPrimary,
            textAlign = TextAlign.Center,
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = LocalAppTopography.current.baseRegular,
                color = YralColors.NeutralTextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
