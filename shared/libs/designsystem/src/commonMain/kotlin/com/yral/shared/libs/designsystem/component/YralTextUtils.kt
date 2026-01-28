package com.yral.shared.libs.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.yral.shared.libs.designsystem.theme.YralBrushes

@Composable
fun buildHighlightedText(
    fullText: String,
    highlightedText: String = "",
    baseTextStyle: TextStyle,
    baseColor: Color = Color.White,
    highlightBrush: Brush = YralBrushes.GoldenTextBrush,
    highlightFontWeight: FontWeight = FontWeight.Bold,
): AnnotatedString =
    buildAnnotatedString {
        val highlightStart = fullText.indexOf(highlightedText)
        val highlightEnd = highlightStart + highlightedText.length

        val spanStyle =
            SpanStyle(
                fontSize = baseTextStyle.fontSize,
                fontFamily = baseTextStyle.fontFamily,
                fontWeight = baseTextStyle.fontWeight,
            )

        if (highlightStart >= 0 && highlightedText.isNotEmpty()) {
            // Text before highlighted portion
            if (highlightStart > 0) {
                withStyle(
                    style = spanStyle.copy(color = baseColor),
                ) {
                    append(fullText.take(highlightStart))
                }
            }

            // Highlighted text
            withStyle(
                style =
                    spanStyle.copy(
                        brush = highlightBrush,
                        fontWeight = highlightFontWeight,
                    ),
            ) {
                append(fullText.substring(highlightStart, highlightEnd))
            }

            // Text after highlighted portion
            if (highlightEnd < fullText.length) {
                withStyle(
                    style = spanStyle.copy(color = baseColor),
                ) {
                    append(fullText.substring(highlightEnd))
                }
            }
        } else {
            // No highlighting, just apply base style
            withStyle(
                style = spanStyle.copy(color = baseColor),
            ) {
                append(fullText)
            }
        }
    }
