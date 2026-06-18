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
import com.yral.shared.features.chat.domain.models.InboxSearchResult
import com.yral.shared.libs.designsystem.component.YralGridImage
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.chat.generated.resources.Res
import yral_mobile.shared.features.chat.generated.resources.inbox_search_empty_subtitle
import yral_mobile.shared.features.chat.generated.resources.inbox_search_empty_title
import yral_mobile.shared.features.chat.generated.resources.inbox_search_failed

/**
 * Renders inbox search results — conversation cards (avatar + bot name
 * + last_message_at + subtitle). Mirrors the [DiscoverySearchResults]
 * three-state pattern (error / empty / list) so the two tabs feel
 * identical under the same search bar.
 */
@Composable
fun InboxSearchResults(
    query: String,
    results: List<InboxSearchResult>,
    isLoading: Boolean,
    error: String?,
    onResultClick: (InboxSearchResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            error != null && results.isEmpty() ->
                CenteredInboxMessage(
                    title = stringResource(Res.string.inbox_search_failed),
                    subtitle = error,
                )

            !isLoading && results.isEmpty() && query.isNotBlank() ->
                CenteredInboxMessage(
                    title = stringResource(Res.string.inbox_search_empty_title, query),
                    subtitle = stringResource(Res.string.inbox_search_empty_subtitle),
                )

            else ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(results, key = { it.conversationId }) { result ->
                        InboxSearchRow(
                            result = result,
                            onClick = { onResultClick(result) },
                        )
                    }
                }
        }
    }
}

@Composable
private fun InboxSearchRow(
    result: InboxSearchResult,
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
        Column(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.Center) {
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
        result.lastMessageAt?.let { ts ->
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = ts.shortFormForRow(),
                style = LocalAppTopography.current.smRegular,
                color = YralColors.NeutralTextSecondary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CenteredInboxMessage(
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

/**
 * Cheap human-readable date shortener for the last-message-at column.
 * Renders the ISO8601 date portion ([ISO_DATE_PREFIX_LENGTH] chars,
 * "YYYY-MM-DD") if present; the inbox endpoint may evolve to ship a
 * pre-formatted relative string, in which case this is a no-op fallback.
 */
private fun String.shortFormForRow(): String = take(ISO_DATE_PREFIX_LENGTH)

/** Length of the "YYYY-MM-DD" prefix on an ISO-8601 timestamp. */
private const val ISO_DATE_PREFIX_LENGTH = 10
