package com.yral.shared.features.chat.ui.wall

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import yral_mobile.shared.features.chat.generated.resources.Res
import yral_mobile.shared.features.chat.generated.resources.discovery_search_clear_action
import yral_mobile.shared.features.chat.generated.resources.discovery_search_create_action
import yral_mobile.shared.libs.designsystem.generated.resources.ic_plus_circle
import yral_mobile.shared.libs.designsystem.generated.resources.ic_search
import yral_mobile.shared.libs.designsystem.generated.resources.ic_x_circle
import yral_mobile.shared.libs.designsystem.generated.resources.Res as DesignRes

/**
 * Shared chat-home search bar. Renders identically on both Discover and
 * Inbox tabs (Rishi: ADHD-friendly symmetry). Trailing icon swaps with
 * the input state: "+" when the field is empty (opens Create) and "✕"
 * when the field has text (clears the input + dismisses the keyboard).
 *
 * Placeholder + the action behind the leading-magnifier-icon submit are
 * tab-aware — supplied by the caller via [placeholder] / [onQueryChange].
 */
@Composable
fun DiscoverySearchBar(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    onCreateClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(YralColors.Neutral800, RoundedCornerShape(24.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { focusRequester.requestFocus() },
                ).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(DesignRes.drawable.ic_search),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                cursorBrush = SolidColor(YralColors.Pink300),
                textStyle =
                    LocalAppTopography.current.baseRegular.copy(
                        color = YralColors.NeutralTextPrimary,
                    ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
            )
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    style = LocalAppTopography.current.baseRegular,
                    color = YralColors.NeutralTextSecondary,
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        if (query.isEmpty()) {
            Image(
                painter = painterResource(DesignRes.drawable.ic_plus_circle),
                contentDescription = stringResource(Res.string.discovery_search_create_action),
                modifier =
                    Modifier
                        .size(28.dp)
                        .clickable(onClick = onCreateClick),
            )
        } else {
            Image(
                painter = painterResource(DesignRes.drawable.ic_x_circle),
                contentDescription = stringResource(Res.string.discovery_search_clear_action),
                modifier =
                    Modifier
                        .size(24.dp)
                        .clickable {
                            onClearClick()
                            keyboardController?.hide()
                        },
            )
        }
    }
}
