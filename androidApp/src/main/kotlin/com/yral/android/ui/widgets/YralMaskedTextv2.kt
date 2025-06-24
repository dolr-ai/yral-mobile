package com.yral.android.ui.widgets

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.TextPaint
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.core.graphics.createBitmap
import android.graphics.Canvas as AndroidCanvas

@Composable
fun YralMaskedVectorTextV2(
    modifier: Modifier = Modifier, // width need to specified according to useCase
    text: String,
    vectorRes: Int,
    textStyle: TextStyle,
    maxLines: Int = Int.MAX_VALUE,
    textOverflow: TextOverflow = TextOverflow.Clip,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val drawable =
        remember(vectorRes) {
            AppCompatResources.getDrawable(context, vectorRes)?.mutate()
        } ?: return

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
                        textStyle,
                        textOverflow,
                        density,
                    )
                val vectorBitmap = createVectorBitmap(drawable, canvasWidth, canvasHeight)
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
    textStyle: TextStyle,
    textOverflow: TextOverflow,
    density: Density,
): Bitmap {
    val textBitmap = createBitmap(textWidth, textHeight)
    val textCanvas = AndroidCanvas(textBitmap)
    val androidTextPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = with(density) { textStyle.fontSize.toPx() }
            typeface =
                when (textStyle.fontWeight) {
                    FontWeight.Bold -> Typeface.DEFAULT_BOLD
                    else -> Typeface.DEFAULT
                }
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

private fun createVectorBitmap(
    drawable: Drawable,
    textWidth: Int,
    textHeight: Int,
): Bitmap {
    val vectorBitmap = createBitmap(textWidth, textHeight)
    val vectorCanvas = AndroidCanvas(vectorBitmap)
    drawable.setBounds(0, 0, textWidth, textHeight)
    drawable.draw(vectorCanvas)
    return vectorBitmap
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
