@file:Suppress("MagicNumber")

package com.yral.android.ui.design

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
    val baseMedium: TextStyle,
    val mdBold: TextStyle,
    val mdRegular: TextStyle,
    val regRegular: TextStyle,
    val xlSemiBold: TextStyle,
    val baseRegular: TextStyle,
    val feedCanisterId: TextStyle,
    val feedDescription: TextStyle,
    val lgBold: TextStyle,
    val mdMedium: TextStyle,
)

val LocalAppTopography =
    staticCompositionLocalOf {
        AppTopography(
            xlBold = TextStyle.Default,
            baseMedium = TextStyle.Default,
            mdBold = TextStyle.Default,
            mdRegular = TextStyle.Default,
            regRegular = TextStyle.Default,
            xlSemiBold = TextStyle.Default,
            baseRegular = TextStyle.Default,
            feedCanisterId = TextStyle.Default,
            feedDescription = TextStyle.Default,
            lgBold = TextStyle.Default,
            mdMedium = TextStyle.Default,
        )
    }

@Suppress("LongMethod")
fun appTypoGraphy() =
    AppTopography(
        xlBold =
            TextStyle(
                fontSize = 20.sp,
                lineHeight = 30.sp,
                fontFamily = YralTypoGraphy.KumbhSans,
                fontWeight = FontWeight.Bold,
            ),
        baseMedium =
            TextStyle(
                fontSize = 14.sp,
                lineHeight = 19.6.sp,
                fontFamily = YralTypoGraphy.KumbhSans,
                fontWeight = FontWeight.Medium,
            ),
        mdBold =
            TextStyle(
                fontSize = 16.sp,
                lineHeight = 22.4.sp,
                fontFamily = YralTypoGraphy.KumbhSans,
                fontWeight = FontWeight.Bold,
            ),
        mdRegular =
            TextStyle(
                fontSize = 16.sp,
                lineHeight = 22.4.sp,
                fontFamily = YralTypoGraphy.KumbhSans,
                fontWeight = FontWeight.Normal,
            ),
        regRegular =
            TextStyle(
                fontSize = 12.sp,
                lineHeight = 16.8.sp,
                fontFamily = YralTypoGraphy.KumbhSans,
                fontWeight = FontWeight.Normal,
            ),
        xlSemiBold =
            TextStyle(
                fontSize = 20.sp,
                lineHeight = 28.sp,
                fontFamily = YralTypoGraphy.KumbhSans,
                fontWeight = FontWeight.SemiBold,
            ),
        baseRegular =
            TextStyle(
                fontSize = 14.sp,
                lineHeight = 19.6.sp,
                fontFamily = YralTypoGraphy.KumbhSans,
                fontWeight = FontWeight.Normal,
            ),
        feedCanisterId =
            TextStyle(
                fontSize = 15.sp,
                fontFamily = YralTypoGraphy.KumbhSans,
                fontWeight = FontWeight.SemiBold,
            ),
        feedDescription =
            TextStyle(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontFamily = YralTypoGraphy.KumbhSans,
                fontWeight = FontWeight.Medium,
            ),
        lgBold =
            TextStyle(
                fontSize = 18.sp,
                lineHeight = 25.2.sp,
                fontFamily = YralTypoGraphy.KumbhSans,
                fontWeight = FontWeight(700),
            ),
        mdMedium =
            TextStyle(
                fontSize = 16.sp,
                lineHeight = 22.4.sp,
                fontFamily = YralTypoGraphy.KumbhSans,
                fontWeight = FontWeight(500),
            ),
    )
