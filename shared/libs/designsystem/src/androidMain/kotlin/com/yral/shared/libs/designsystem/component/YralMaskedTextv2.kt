package com.yral.shared.libs.designsystem.component

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Typeface
import android.text.TextPaint
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.resolveAsTypeface
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.graphics.createBitmap
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import android.graphics.Canvas as AndroidCanvas

@Suppress("LongMethod")
@Composable
actual fun YralMaskedVectorTextV2(
    text: String,
    drawableRes: DrawableResource,
    textStyle: TextStyle,
    modifier: Modifier, // width need to specified according to useCase
    maxLines: Int,
    textOverflow: TextOverflow,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val painter = painterResource(drawableRes)
    val resolvedTypeface =
        LocalFontFamilyResolver.current
            .resolveAsTypeface(
                fontFamily = textStyle.fontFamily ?: FontFamily.Default,
                fontWeight = textStyle.fontWeight ?: FontWeight.Normal,
                fontStyle = textStyle.fontStyle ?: FontStyle.Normal,
            ).value
    Layout(
        content = {
            Canvas(modifier = Modifier) {
                // The actual drawing will use the size provided by the Layout
                val canvasWidth = size.width.toInt().coerceAtLeast(1)
                val canvasHeight = size.height.toInt().coerceAtLeast(1)
                val textBitmap =
                    createTextBitmap(
                        text,
                        canvasWidth,
                        canvasHeight,
                        textStyle.fontSize.toPx(),
                        resolvedTypeface,
                        textOverflow,
                        density,
                    )
                val vectorBitmap = painterToBitmap(painter, canvasWidth, canvasHeight, density)
                val resultBitmap =
                    createMaskedBitmap(textBitmap, vectorBitmap, canvasWidth, canvasHeight)
                drawResultBitmap(resultBitmap)
            }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        // Use the maxWidth from constraints, but at least 1
        val availableWidth = constraints.maxWidth.coerceAtLeast(1)
        // Measure the text with the available width
        val measured =
            textMeasurer.measure(
                text,
                style = textStyle,
                maxLines = maxLines,
                overflow = textOverflow,
                constraints = constraints,
            )
        val textWidth = measured.size.width.coerceIn(constraints.minWidth, availableWidth)
        val textHeight =
            measured.size.height.coerceIn(
                constraints.minHeight,
                constraints.maxHeight.coerceAtLeast(1),
            )

        val placeable =
            measurables.first().measure(
                constraints.copy(
                    minWidth = textWidth,
                    maxWidth = textWidth,
                    minHeight = textHeight,
                    maxHeight = textHeight,
                ),
            )
        layout(textWidth, textHeight) {
            placeable.place(0, 0)
        }
    }
}

private fun createTextBitmap(
    text: String,
    textWidth: Int,
    textHeight: Int,
    fontSize: Float,
    resolvedTypeface: Typeface,
    textOverflow: TextOverflow,
    density: Density,
): Bitmap {
    val textBitmap = createBitmap(textWidth, textHeight)
    val textCanvas = AndroidCanvas(textBitmap)
    val androidTextPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = with(density) { fontSize }
            typeface = resolvedTypeface
            isAntiAlias = true
            isSubpixelText = true
        }

    val displayText = getDisplayText(text, textWidth, androidTextPaint, textOverflow)
    val baselineY = textHeight - androidTextPaint.descent()
    val textX = 0f
    textCanvas.drawText(displayText, textX, baselineY, androidTextPaint)
    return textBitmap
}

private fun getDisplayText(
    text: String,
    textWidth: Int,
    paint: TextPaint,
    overflow: TextOverflow,
): String =
    when (overflow) {
        TextOverflow.Ellipsis -> ellipsisText(text, textWidth, paint)
        TextOverflow.MiddleEllipsis -> middleEllipsisText(text, textWidth, paint)
        TextOverflow.StartEllipsis -> startEllipsisText(text, textWidth, paint)
        TextOverflow.Clip -> clipText(text, textWidth, paint)
        else -> text
    }

private fun ellipsisText(
    text: String,
    textWidth: Int,
    paint: TextPaint,
): String {
    val ellipsis = "..."
    var displayText = text
    if (paint.measureText(text) > textWidth) {
        var endIndex = text.length
        while (endIndex > 0 && paint.measureText(displayText) > textWidth) {
            endIndex--
            displayText = text.substring(0, endIndex) + ellipsis
        }
    }
    return displayText
}

private fun clipText(
    text: String,
    textWidth: Int,
    paint: TextPaint,
) = text.takeWhile { paint.measureText(it.toString()) <= textWidth }

private fun middleEllipsisText(
    text: String,
    textWidth: Int,
    paint: TextPaint,
): String {
    val ellipsis = "..."
    var displayText = text
    if (paint.measureText(text) > textWidth) {
        var startIndex = 0
        var endIndex = text.length
        while (startIndex < endIndex &&
            paint.measureText(
                text.substring(0, startIndex) + ellipsis + text.substring(endIndex),
            ) < textWidth
        ) {
            startIndex++
            endIndex--
        }
        displayText = text.substring(0, startIndex) + ellipsis + text.substring(endIndex)
    }
    return displayText
}

private fun startEllipsisText(
    text: String,
    textWidth: Int,
    paint: TextPaint,
): String {
    val ellipsis = "..."
    var displayText = text
    if (paint.measureText(text) > textWidth) {
        var startIndex = 0
        while (startIndex < text.length && paint.measureText(displayText) > textWidth) {
            startIndex++
            displayText = ellipsis + text.substring(startIndex)
        }
    }
    return displayText
}

fun painterToBitmap(
    painter: Painter,
    textWidth: Int,
    textHeight: Int,
    density: Density,
): Bitmap {
    val imageBitmap = ImageBitmap(textWidth, textHeight)
    val canvas = Canvas(imageBitmap)

    val drawScope = CanvasDrawScope()
    val floatSize = Size(textWidth.toFloat(), textHeight.toFloat())
    drawScope.draw(
        density = density,
        layoutDirection = LayoutDirection.Ltr,
        canvas = canvas,
        size = floatSize,
    ) {
        with(painter) {
            draw(floatSize)
        }
    }

    return imageBitmap.asAndroidBitmap()
}

private fun createMaskedBitmap(
    textBitmap: Bitmap,
    vectorBitmap: Bitmap,
    textWidth: Int,
    textHeight: Int,
): Bitmap {
    val resultBitmap = createBitmap(textWidth, textHeight)
    val resultCanvas = AndroidCanvas(resultBitmap)
    resultCanvas.drawBitmap(textBitmap, 0f, 0f, null)
    val maskPaint =
        Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }
    resultCanvas.drawBitmap(vectorBitmap, 0f, 0f, maskPaint)
    return resultBitmap
}

private fun DrawScope.drawResultBitmap(resultBitmap: Bitmap) {
    val xOffset = (size.width - resultBitmap.width) / 2f
    val yOffset = (size.height - resultBitmap.height) / 2f
    drawIntoCanvas {
        it.nativeCanvas.drawBitmap(resultBitmap, xOffset, yOffset, null)
    }
}
