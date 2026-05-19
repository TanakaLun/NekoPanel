package io.tl.nekopanel.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

data class ThemeScheme(
    val name: String, val key: String,
    val lightPrimary: Color, val lightOnPrimary: Color, val lightPrimaryContainer: Color, val lightSecondary: Color, val lightTertiary: Color,
    val darkPrimary: Color, val darkOnPrimary: Color, val darkPrimaryContainer: Color, val darkSecondary: Color, val darkTertiary: Color,
) {
    fun lightScheme() = lightColorScheme(
        primary = lightPrimary, onPrimary = lightOnPrimary, primaryContainer = lightPrimaryContainer,
        secondary = lightSecondary, tertiary = lightTertiary,
    )
    fun darkScheme() = darkColorScheme(
        primary = darkPrimary, onPrimary = darkOnPrimary, primaryContainer = darkPrimaryContainer,
        secondary = darkSecondary, tertiary = darkTertiary,
    )
}

val JapaneseThemeSchemes = listOf(
    ThemeScheme("绯红", "akabeni",
        Color(0xFFD43B3B), Color.White, Color(0xFFFFDAD6), Color(0xFF8C4B4B), Color(0xFFB35E5E),
        Color(0xFFFFB4A7), Color(0xFF68000A), Color(0xFF93000F), Color(0xFFD5BDBD), Color(0xFFE5BDBD)),
    ThemeScheme("群青", "gunjou",
        Color(0xFF1E3A8A), Color.White, Color(0xFFDAE2FF), Color(0xFF565F71), Color(0xFF6F5C8C),
        Color(0xFFAEC7FF), Color(0xFF001F5E), Color(0xFF002E7E), Color(0xFFBBC0D0), Color(0xFFD2BEEF)),
    ThemeScheme("萌黄", "moegi",
        Color(0xFF2E7D32), Color.White, Color(0xFFC8E6C9), Color(0xFF4B6E4B), Color(0xFF6B8E23),
        Color(0xFF81C784), Color(0xFF003300), Color(0xFF1B5E20), Color(0xFFB8D4B8), Color(0xFFC5E1A5)),
    ThemeScheme("山吹", "yamabuki",
        Color(0xFFDAA520), Color.White, Color(0xFFF5E6C8), Color(0xFF7A6E4C), Color(0xFF9B8E5C),
        Color(0xFFFFD54F), Color(0xFF3E2D00), Color(0xFF5A4300), Color(0xFFCBC0A2), Color(0xFFD4CAA6)),
    ThemeScheme("藤紫", "fujimurasaki",
        Color(0xFF7C3AED), Color.White, Color(0xFFEDE9FE), Color(0xFF5B4B7A), Color(0xFF8B5CF6),
        Color(0xFFC4B5FD), Color(0xFF2E1065), Color(0xFF4C1D95), Color(0xFFB4A6CE), Color(0xFFD4C4F6)),
    ThemeScheme("空色", "sorairo",
        Color(0xFF0288D1), Color.White, Color(0xFFB3E5FC), Color(0xFF4A6A7A), Color(0xFF38B0DE),
        Color(0xFF81D4FA), Color(0xFF00344E), Color(0xFF01579B), Color(0xFFB0D0DE), Color(0xFFA0D8EE)),
    ThemeScheme("若竹", "wakatake",
        Color(0xFF2E7D32), Color.White, Color(0xFFC8E6C9), Color(0xFF3D6B4D), Color(0xFF4CAF50),
        Color(0xFF81C784), Color(0xFF00330A), Color(0xFF1B5E20), Color(0xFFB0D0B4), Color(0xFFA5D6A7)),
    ThemeScheme("胭脂", "enji",
        Color(0xFF9B1B30), Color.White, Color(0xFFFFDAD6), Color(0xFF7A4B4B), Color(0xFFB35E5E),
        Color(0xFFFFB4A7), Color(0xFF5E0000), Color(0xFF93000F), Color(0xFFD5BDBD), Color(0xFFE5BDBD)),
)
