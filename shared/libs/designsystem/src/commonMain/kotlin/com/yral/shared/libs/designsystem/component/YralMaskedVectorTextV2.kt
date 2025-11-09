package com.yral.shared.libs.designsystem.component

import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.appTypoGraphy
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.vectorResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import yral_mobile.shared.libs.designsystem.generated.resources.Res
import yral_mobile.shared.libs.designsystem.generated.resources.golden_gradient

@Composable
fun YralMaskedVectorTextV2(
    text: String,
    drawableRes: DrawableResource,
    textStyle: TextStyle,
    modifier: Modifier = Modifier, // width need to specified according to useCase
    maxLines: Int = Int.MAX_VALUE,
    textOverflow: TextOverflow = TextOverflow.Clip,
) {
    val painter = rememberVectorPainter(image = vectorResource(drawableRes))

    Text(
        text = text,
        style = textStyle,
        maxLines = maxLines,
        overflow = textOverflow,
        modifier =
            modifier
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithCache {
                    onDrawWithContent {
                        // 1) Draw the text normally (as a mask)
                        drawContent()

                        // 2) Draw the vector "through" the text ink via SrcAtop
                        drawIntoCanvas { canvas ->
                            val layerPaint = Paint().apply { blendMode = BlendMode.SrcIn }
                            val bounds = Rect(Offset.Zero, size)
                            canvas.saveLayer(bounds, layerPaint)
                            // Painter respects the current DrawScope (this block)
                            with(painter) { with(this@onDrawWithContent) { draw(size = size) } }
                            canvas.restore()
                        }
                    }
                },
    )
}

@Preview
@Composable
fun YralMaskedVectorTextV2Preview() {
    CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
        YralMaskedVectorTextV2(
            text = "Hello World",
            drawableRes = Res.drawable.golden_gradient,
            textStyle = LocalAppTopography.current.baseMedium,
            modifier = Modifier.wrapContentSize(),
        )
    }
}
