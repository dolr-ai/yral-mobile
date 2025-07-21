package com.yral.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.yral.android.R
import com.yral.android.ui.design.LocalAppTopography
import com.yral.android.ui.design.YralColors

sealed class ToastType {
    abstract val message: String

    data class Big(
        val heading: String,
        override val message: String,
    ) : ToastType()

    data class Small(
        override val message: String,
    ) : ToastType()
}

enum class ToastStatus {
    Success,
    Warning,
    Error,
    Info,
}

data class ToastCTA(
    val text: String,
    val onClick: () -> Unit,
)

private const val ANIMATION_DURATION_MS = 300

@Composable
fun Toast(
    visible: Boolean,
    type: ToastType,
    modifier: Modifier = Modifier,
    status: ToastStatus = ToastStatus.Info,
    cta: ToastCTA? = null,
    onDismiss: () -> Unit = {},
) {
    AnimatedVisibility(
        visible = visible,
        enter =
            slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(ANIMATION_DURATION_MS),
            ) + fadeIn(animationSpec = tween(ANIMATION_DURATION_MS)),
        exit =
            slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(ANIMATION_DURATION_MS),
            ) + fadeOut(animationSpec = tween(ANIMATION_DURATION_MS)),
        modifier = modifier,
    ) {
        val statusColor = iconColor(status)
        ToastContainer(statusColor) {
            when (type) {
                is ToastType.Big -> ToastContentBig(type, status, cta, onDismiss)
                is ToastType.Small -> ToastContentSmall(type, status, cta, onDismiss)
            }
        }
    }
}

@Composable
private fun ToastContainer(
    statusColor: Color,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(size = 6.dp),
        color = YralColors.Neutral800,
        shadowElevation = 10.dp,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(size = 6.dp)),
    ) {
        val offsetX = with(LocalDensity.current) { 22.dp.toPx() }
        val offsetY = with(LocalDensity.current) { 24.dp.toPx() }
        val radialGradientBrush =
            Brush.radialGradient(
                colors =
                    listOf(
                        statusColor.copy(alpha = 0.25f),
                        statusColor.copy(alpha = 0.08f),
                        YralColors.Neutral800,
                    ),
                center = Offset(x = offsetX, y = offsetY),
                radius = with(LocalDensity.current) { 106.dp.toPx() },
                tileMode = TileMode.Clamp,
            )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(radialGradientBrush),
        )

        content()
    }
}

@Composable
private fun ToastContentBig(
    toast: ToastType.Big,
    status: ToastStatus,
    cta: ToastCTA?,
    onDismiss: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        StatusIcon(status, modifier = Modifier.align(if (cta == null) Alignment.CenterVertically else Alignment.Top))

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = toast.heading,
                    style = LocalAppTopography.current.baseMedium,
                    color = YralColors.NeutralTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                CrossIcon(onDismiss)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = toast.message,
                style = LocalAppTopography.current.baseRegular,
                color = YralColors.Neutral300,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (cta != null) {
                Spacer(modifier = Modifier.height(10.dp))
                CtaText(cta, status)
            }
        }
    }
}

@Composable
private fun ToastContentSmall(
    toast: ToastType.Small,
    status: ToastStatus,
    cta: ToastCTA?,
    onDismiss: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatusIcon(status)

        Text(
            text = toast.message,
            style = LocalAppTopography.current.baseRegular,
            color = YralColors.NeutralTextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (cta != null) {
                CtaText(cta, status)
            }
            CrossIcon(onDismiss)
        }
    }
}

@Composable
private fun StatusIcon(
    status: ToastStatus,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(iconResource(status)),
        contentDescription = status.name,
        tint = iconColor(status),
        modifier =
            modifier
                .size(32.dp)
                .background(color = YralColors.NeutralBackgroundCardBackground, shape = CircleShape)
                .padding(4.dp),
    )
}

@Composable
private fun CtaText(
    cta: ToastCTA,
    status: ToastStatus,
) {
    Text(
        text = cta.text,
        style = LocalAppTopography.current.baseRegular,
        color = ctaColor(status),
        maxLines = 1,
        modifier = Modifier.clickable { cta.onClick() },
    )
}

@Composable
private fun CrossIcon(onClick: () -> Unit) {
    Icon(
        painter = painterResource(R.drawable.cross),
        contentDescription = "Dismiss",
        tint = Color.White,
        modifier =
            Modifier
                .size(16.dp)
                .clickable { onClick() },
    )
}

@Composable
private fun iconColor(status: ToastStatus): Color =
    when (status) {
        ToastStatus.Success -> YralColors.SuccessGreen
        ToastStatus.Warning -> YralColors.PrimaryYellow
        ToastStatus.Error -> YralColors.ErrorRed
        ToastStatus.Info -> YralColors.Pink300
    }

private fun iconResource(status: ToastStatus): Int =
    when (status) {
        ToastStatus.Success -> R.drawable.ic_tick_circle
        ToastStatus.Warning -> R.drawable.ic_warning_circle
        ToastStatus.Error -> R.drawable.ic_cross_circle
        ToastStatus.Info -> R.drawable.ic_information_circle
    }

private fun ctaColor(status: ToastStatus): Color =
    when (status) {
        ToastStatus.Success -> YralColors.Green200
        ToastStatus.Warning -> YralColors.Yellow100
        ToastStatus.Error -> YralColors.Red400
        ToastStatus.Info -> YralColors.Pink200
    }

@Suppress("UnusedPrivateMember")
@Preview
@Composable
private fun ToastPreview(
    @PreviewParameter(ToastPreviewParameterProvider::class) parameter: ToastPreviewParameter,
) {
    Box(
        modifier =
            Modifier
                .background(YralColors.OnPrimaryContainer)
                .padding(16.dp),
    ) {
        Toast(
            visible = true,
            type = parameter.type,
            status = parameter.status,
            cta = parameter.cta,
        )
    }
}

private data class ToastPreviewParameter(
    val type: ToastType,
    val status: ToastStatus,
    val cta: ToastCTA?,
)

private class ToastPreviewParameterProvider : PreviewParameterProvider<ToastPreviewParameter> {
    override val values =
        listOf(
            ToastType.Big("Big Toast", "Long description of the big toast that is somewhat long"),
            ToastType.Small("Small Toast with long message that is somewhat long"),
        ).flatMap { type ->
            ToastStatus.entries.flatMap { status ->
                listOf(null, ToastCTA("View Now") { }).map { cta ->
                    ToastPreviewParameter(type, status, cta)
                }
            }
        }.asSequence()
}
