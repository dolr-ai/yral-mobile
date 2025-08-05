@file:Suppress("MagicNumber")

package com.yral.android.ui.design

import androidx.compose.ui.graphics.Color

object YralColors {
    val PrimaryContainer: Color = Color(0xFF0A0A0A)
    val OnPrimaryContainer: Color = Color(0xFFFAFAFA)

    val NeutralTextPrimary: Color = Color(0xFFFAFAFA)
    val NeutralTextSecondary: Color = Color(0xFFA3A3A3)

    val NeutralBackgroundCardBackground: Color = Color(0xFF171717)
    val NeutralIconsActive: Color = Color(0xFFFAFAFA)

    val Neutral50: Color = Color(0xFFFAFAFA)
    val Neutral300: Color = Color(0xFFD4D4D4)
    val Neutral500: Color = Color(0xFFA3A3A3)
    val Neutral600: Color = Color(0xFF525252)
    val Neutral700: Color = Color(0xFF404040)
    val Neutral800: Color = Color(0xFF212121)
    val Neutral900: Color = Color(0xFF171717)
    val Neutral950: Color = Color(0xFF0A0A0A)
    val NeutralBlack: Color = Color(0xFF1D1C2B)

    val Pink50: Color = Color(0xFFFCE6F2)
    val Pink100: Color = Color(0xFFF6B0D6)
    val Pink200: Color = Color(0xFFEC55A7)
    val Pink300: Color = Color(0xFFE2017B)
    val Pink400: Color = Color(0xFFA00157)

    val Divider: Color = Color(0xFF232323)

    val ShadowSpotColor: Color = Color(0x1C8377C6)
    val ShadowAmbientColor: Color = Color(0x1C8377C6)
    val ScrimColor: Color = Color(0xE5000000)
    val ScrimColorLight: Color = Color(0xCC000000)

    val ButtonBorderColor: Color = Color(0xFFE0E0E9)

    val ProfilePicBackground: Color = Color(0xFFC4C4C4)

    val Red300: Color = Color(0xFFF14331)
    val Red400: Color = Color(0xFFAB3023)
    val ErrorRed: Color = Color(0xFFEF4444)

    val Green50: Color = Color(0xFFE9FAF2)
    val Green200: Color = Color(0xFF68DBAB)
    val Green300: Color = Color(0xFF1EC981)
    val Green400: Color = Color(0xFF158F5C)
    val SuccessGreen: Color = Color(0xFF10B981)

    val Grey50: Color = Neutral50

    val Yellow400: Color = Color(0xFF2C2310)
    val Yellow100: Color = Color(0xFFFFDC8D)
    val PrimaryYellow: Color = Color(0xFFF9EA0E)

    val CoinBalanceBGStart = Color(0xFFFFCC00)
    val CoinBalanceBGEnd = Color(0xFFDA8100)
    val SmileyGameCardBackground = Color(0x66000000)
    val GameToggleBackground = Color(0x66212121)
    val HowToPlayBackground = Color(0x80000000)

    fun getColorByName(name: String): Color =
        try {
            val titleCaseName =
                name
                    .split(Regex("(?<=.)(?=\\d)"))
                    .joinToString("") {
                        it.replaceFirstChar { char -> char.uppercase() }
                    }

            val field = this::class.java.getDeclaredField(titleCaseName)
            field.isAccessible = true
            when (val value = field.get(this)) {
                is Long -> Color(value.toULong())
                is Color -> value
                else -> {
                    Grey50
                }
            }
        } catch (_: Exception) {
            Grey50
        }

    fun getColorFromHex(hex: String): Color {
        val cleanHex = hex.removePrefix("#")
        return try {
            val colorLong =
                when (cleanHex.length) {
                    6 -> 0xFF000000 or cleanHex.toLong(16) // RRGGBB -> add full alpha
                    8 -> cleanHex.toLong(16) // AARRGGBB
                    else -> return Color.White
                }
            Color(colorLong)
        } catch (e: NumberFormatException) {
            Color.White
        }
    }
}
