package com.yral.shared.libs.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yral.shared.libs.designsystem.component.YralContextMenuConstants.DEFAULT_MENU_ICON_SIZE
import com.yral.shared.libs.designsystem.component.YralContextMenuConstants.DEFAULT_MENU_ITEM_MAX_HEIGHT
import com.yral.shared.libs.designsystem.component.YralContextMenuConstants.DEFAULT_TRIGGER_SIZE
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.YralColors
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * A reusable context menu component with contextual positioning.
 *
 * @param items List of menu items to display
 * @param triggerIcon The icon to display as the menu trigger
 * @param triggerSize Size of the trigger icon (default: 20.dp)
 * @param menuIconSize Size of the menu item icons (default: 20.dp)
 * @param modifier Modifier for the trigger icon
 */
@Composable
fun YralContextMenu(
    items: List<YralContextMenuItem>,
    triggerIcon: DrawableResource,
    triggerSize: Dp = DEFAULT_TRIGGER_SIZE.dp,
    menuIconSize: Dp = DEFAULT_MENU_ICON_SIZE.dp,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    var showMenu by remember { mutableStateOf(false) }
    Box {
        Image(
            painter = painterResource(triggerIcon),
            contentDescription = "Menu",
            modifier =
                modifier
                    .size(triggerSize)
                    .clickable { showMenu = true },
        )
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            containerColor = Color.Transparent,
            modifier =
                Modifier
                    .border(
                        width = 1.dp,
                        color = YralColors.Neutral700,
                        shape = RoundedCornerShape(8.dp),
                    ).background(
                        color = YralColors.Neutral800,
                        shape = RoundedCornerShape(8.dp),
                    ),
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = item.text,
                            style = LocalAppTopography.current.baseRegular,
                            color = YralColors.NeutralTextPrimary,
                        )
                    },
                    leadingIcon =
                        item.icon?.let { icon ->
                            {
                                Image(
                                    painter = painterResource(icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(menuIconSize),
                                    contentScale = ContentScale.FillBounds,
                                )
                            }
                        },
                    onClick = {
                        showMenu = false
                        item.onClick()
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.heightIn(max = DEFAULT_MENU_ITEM_MAX_HEIGHT.dp),
                )
            }
        }
    }
}

/**
 * Data class representing a menu item in the context menu
 */
data class YralContextMenuItem(
    val text: String,
    val icon: DrawableResource? = null,
    val onClick: () -> Unit,
)

private object YralContextMenuConstants {
    const val DEFAULT_TRIGGER_SIZE = 20f
    const val DEFAULT_MENU_ICON_SIZE = 20f
    const val DEFAULT_MENU_ITEM_MAX_HEIGHT = 40f
}
