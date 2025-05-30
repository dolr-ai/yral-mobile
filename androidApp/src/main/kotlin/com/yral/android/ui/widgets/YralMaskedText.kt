package com.yral.android.ui.widgets

import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Typeface
import android.text.TextPaint
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.core.graphics.createBitmap
import android.graphics.Canvas as AndroidCanvas

@Composable
fun YralMaskedVectorText(
    text: String,
    vectorRes: Int,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val textMeasurer = rememberTextMeasurer()

    val drawable =
        remember(vectorRes) {
            AppCompatResources.getDrawable(context, vectorRes)?.mutate()
        } ?: return

    Box(
        modifier =
            modifier
                .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val measured = textMeasurer.measure(text, style = textStyle)
            val textWidth = measured.size.width
            val textHeight = measured.size.height

            // --- TEXT MASK BITMAP ---
            val textBitmap = createBitmap(textWidth, textHeight)
            val textCanvas = AndroidCanvas(textBitmap)
            val androidTextPaint =
                TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.BLACK
                    textSize = textStyle.fontSize.toPx()
                    typeface =
                        when (textStyle.fontWeight) {
                            FontWeight.Bold -> Typeface.DEFAULT_BOLD
                            else -> Typeface.DEFAULT
                        }
                }
            val baselineY = textHeight - androidTextPaint.descent()
            val textX = (textWidth - androidTextPaint.measureText(text)) / 2f
            textCanvas.drawText(text, textX, baselineY, androidTextPaint)

            // --- VECTOR FILL BITMAP ---
            val vectorBitmap = createBitmap(textWidth, textHeight)
            val vectorCanvas = AndroidCanvas(vectorBitmap)
            drawable.setBounds(0, 0, textWidth, textHeight)
            drawable.draw(vectorCanvas)

            // --- MASKED RESULT ---
            val resultBitmap = createBitmap(textWidth, textHeight)
            val resultCanvas = AndroidCanvas(resultBitmap)
            resultCanvas.drawBitmap(textBitmap, 0f, 0f, null)
            val maskPaint =
                Paint().apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                }
            resultCanvas.drawBitmap(vectorBitmap, 0f, 0f, maskPaint)

            // --- DRAW TO COMPOSE CANVAS, CENTERED ---
            val xOffset = (size.width - resultBitmap.width) / 2f
            val yOffset = (size.height - resultBitmap.height) / 2f
            drawIntoCanvas {
                it.nativeCanvas.drawBitmap(resultBitmap, xOffset, yOffset, null)
            }
        }
    }
}
