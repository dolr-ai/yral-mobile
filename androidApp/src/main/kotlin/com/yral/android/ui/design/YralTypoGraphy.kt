@file:Suppress("MagicNumber")

package com.yral.android.ui.design

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.yral.android.R

object YralTypoGraphy {
    val KumbhSans =
        FontFamily(
            Font(R.font.kumbh_sans_black, FontWeight.Black),
            Font(R.font.kumbh_sans_light, FontWeight.Light),
            Font(R.font.kumbh_sans_regular, FontWeight.Normal),
            Font(R.font.kumbh_sans_thin, FontWeight.Thin),
            Font(R.font.kumbh_sans_bold, FontWeight.Bold),
            Font(R.font.kumbh_sans_semi_bold, FontWeight.SemiBold),
            Font(R.font.kumbh_sans_extra_bold, FontWeight.ExtraBold),
            Font(R.font.kumbh_sans_extra_light, FontWeight.ExtraLight),
            Font(R.font.kumbh_sans_medium, FontWeight.Medium),
        )
}

data class AppTopography(
    val xlBold: TextStyle,
)

val LocalAppTopography =
    staticCompositionLocalOf {
        AppTopography(
            xlBold = TextStyle.Default,
        )
    }

fun appTypoGraphy() =
    AppTopography(
        xlBold =
            TextStyle(
                fontSize = 20.sp,
                lineHeight = 30.sp,
                fontFamily = YralTypoGraphy.KumbhSans,
                fontWeight = FontWeight(700),
                color = YralColors.neutralTextPrimary,
                textAlign = TextAlign.Center,
            ),
    )
