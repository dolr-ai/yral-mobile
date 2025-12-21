@file:Suppress("MagicNumber")

package com.yral.shared.libs.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import yral_mobile.shared.libs.designsystem.generated.resources.Res
import yral_mobile.shared.libs.designsystem.generated.resources.kumbh_sans_black
import yral_mobile.shared.libs.designsystem.generated.resources.kumbh_sans_bold
import yral_mobile.shared.libs.designsystem.generated.resources.kumbh_sans_extra_bold
import yral_mobile.shared.libs.designsystem.generated.resources.kumbh_sans_extra_light
import yral_mobile.shared.libs.designsystem.generated.resources.kumbh_sans_light
import yral_mobile.shared.libs.designsystem.generated.resources.kumbh_sans_medium
import yral_mobile.shared.libs.designsystem.generated.resources.kumbh_sans_regular
import yral_mobile.shared.libs.designsystem.generated.resources.kumbh_sans_semi_bold
import yral_mobile.shared.libs.designsystem.generated.resources.kumbh_sans_thin

@Composable
fun kumbhSansFontFamily() =
    FontFamily(
        Font(Res.font.kumbh_sans_black, FontWeight.Black),
        Font(Res.font.kumbh_sans_light, FontWeight.Light),
        Font(Res.font.kumbh_sans_regular, FontWeight.Normal),
        Font(Res.font.kumbh_sans_thin, FontWeight.Thin),
        Font(Res.font.kumbh_sans_bold, FontWeight.Bold),
        Font(Res.font.kumbh_sans_semi_bold, FontWeight.SemiBold),
        Font(Res.font.kumbh_sans_extra_bold, FontWeight.ExtraBold),
        Font(Res.font.kumbh_sans_extra_light, FontWeight.ExtraLight),
        Font(Res.font.kumbh_sans_medium, FontWeight.Medium),
    )

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
    val regMedium: TextStyle,
    val baseBold: TextStyle,
    val xsBold: TextStyle,
    val lgMedium: TextStyle,
    val baseSemiBold: TextStyle,
    val mdSemiBold: TextStyle,
    val regBold: TextStyle,
    val regSemiBold: TextStyle,
    val xxlBold: TextStyle,
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
            regMedium = TextStyle.Default,
            baseBold = TextStyle.Default,
            xsBold = TextStyle.Default,
            lgMedium = TextStyle.Default,
            baseSemiBold = TextStyle.Default,
            mdSemiBold = TextStyle.Default,
            regBold = TextStyle.Default,
            regSemiBold = TextStyle.Default,
            xxlBold = TextStyle.Default,
        )
    }

@Suppress("LongMethod")
@Composable
fun appTypoGraphy(): AppTopography {
    val fontFamily = kumbhSansFontFamily()
    return AppTopography(
        xlBold =
            TextStyle(
                fontSize = 20.sp,
                lineHeight = 30.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
            ),
        baseMedium =
            TextStyle(
                fontSize = 14.sp,
                lineHeight = 19.6.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Medium,
            ),
        mdBold =
            TextStyle(
                fontSize = 16.sp,
                lineHeight = 22.4.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
            ),
        mdRegular =
            TextStyle(
                fontSize = 16.sp,
                lineHeight = 22.4.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Normal,
            ),
        regRegular =
            TextStyle(
                fontSize = 12.sp,
                lineHeight = 16.8.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Normal,
            ),
        xlSemiBold =
            TextStyle(
                fontSize = 20.sp,
                lineHeight = 28.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight.SemiBold,
            ),
        baseRegular =
            TextStyle(
                fontSize = 14.sp,
                lineHeight = 19.6.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Normal,
            ),
        feedCanisterId =
            TextStyle(
                fontSize = 15.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight.SemiBold,
            ),
        feedDescription =
            TextStyle(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Medium,
            ),
        lgBold =
            TextStyle(
                fontSize = 18.sp,
                lineHeight = 25.2.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight(700),
            ),
        mdMedium =
            TextStyle(
                fontSize = 16.sp,
                lineHeight = 22.4.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight(500),
            ),
        regMedium =
            TextStyle(
                fontSize = 12.sp,
                lineHeight = 16.8.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight(500),
            ),
        baseBold =
            TextStyle(
                fontSize = 14.sp,
                lineHeight = 21.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight(700),
            ),
        xsBold =
            TextStyle(
                fontSize = 8.sp,
                lineHeight = 11.2.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight(700),
            ),
        lgMedium =
            TextStyle(
                fontSize = 18.sp,
                lineHeight = 25.2.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight(500),
            ),
        baseSemiBold =
            TextStyle(
                fontSize = 14.sp,
                lineHeight = 19.6.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight(600),
            ),
        mdSemiBold =
            TextStyle(
                fontSize = 16.sp,
                lineHeight = 22.4.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight.SemiBold,
            ),
        regBold =
            TextStyle(
                fontSize = 12.sp,
                lineHeight = 16.8.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
            ),
        regSemiBold =
            TextStyle(
                fontSize = 12.sp,
                lineHeight = 16.8.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight.SemiBold,
            ),
        xxlBold =
            TextStyle(
                fontSize = 24.sp,
                lineHeight = 33.6.sp,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
            ),
    )
}
