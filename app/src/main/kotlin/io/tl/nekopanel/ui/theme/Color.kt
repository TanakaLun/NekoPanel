package io.tl.nekopanel.ui.theme

import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

data class ThemeScheme(
    val name: String, val key: String,
    val lightPrimary: Color, val lightOnPrimary: Color, val lightPrimaryContainer: Color,
    val darkPrimary: Color, val darkOnPrimary: Color, val darkPrimaryContainer: Color,
) {
    fun lightScheme() = lightColorScheme(
        primary = lightPrimary, onPrimary = lightOnPrimary, primaryContainer = lightPrimaryContainer,
    )
    fun darkScheme() = darkColorScheme(
        primary = darkPrimary, onPrimary = darkOnPrimary, primaryContainer = darkPrimaryContainer,
    )
}

val JapaneseThemeSchemes = listOf(
    ThemeScheme("绯红", "akabeni",
        Color(0xFFD43B3B), Color.White, Color(0xFFFFDAD6),
        Color(0xFFFFB4A7), Color(0xFF68000A), Color(0xFF93000F)),
    ThemeScheme("群青", "gunjou",
        Color(0xFF1E3A8A), Color.White, Color(0xFFDAE2FF),
        Color(0xFFAEC7FF), Color(0xFF001F5E), Color(0xFF002E7E)),
    ThemeScheme("萌黄", "moegi",
        Color(0xFF2E7D32), Color.White, Color(0xFFC8E6C9),
        Color(0xFF81C784), Color(0xFF003300), Color(0xFF1B5E20)),
    ThemeScheme("山吹", "yamabuki",
        Color(0xFFDAA520), Color.White, Color(0xFFF5E6C8),
        Color(0xFFFFD54F), Color(0xFF3E2D00), Color(0xFF5A4300)),
    ThemeScheme("藤紫", "fujimurasaki",
        Color(0xFF7C3AED), Color.White, Color(0xFFEDE9FE),
        Color(0xFFC4B5FD), Color(0xFF2E1065), Color(0xFF4C1D95)),
    ThemeScheme("空色", "sorairo",
        Color(0xFF0288D1), Color.White, Color(0xFFB3E5FC),
        Color(0xFF81D4FA), Color(0xFF00344E), Color(0xFF01579B)),
    ThemeScheme("若竹", "wakatake",
        Color(0xFF2E7D32), Color.White, Color(0xFFC8E6C9),
        Color(0xFF81C784), Color(0xFF00330A), Color(0xFF1B5E20)),
    ThemeScheme("胭脂", "enji",
        Color(0xFF9B1B30), Color.White, Color(0xFFFFDAD6),
        Color(0xFFFFB4A7), Color(0xFF5E0000), Color(0xFF93000F)),
)

fun resolveThemeColors(customKey: String, dark: Boolean): Colors? {
    val scheme = JapaneseThemeSchemes.firstOrNull { it.key == customKey } ?: return null
    return if (dark) scheme.darkScheme() else scheme.lightScheme()
}