package io.tl.nekopanel.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

@Composable
fun NekoPanelTheme(
    themeMode: String = "follow_system",
    dynamicColor: Boolean = true,
    customSeedColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val isDark = themeMode == "dark" || (themeMode == "follow_system" && isSystemInDarkTheme())

    val colorSchemeMode = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isDark) ColorSchemeMode.MonetDark else ColorSchemeMode.MonetLight
    } else if (customSeedColor != null) {
        if (isDark) ColorSchemeMode.MonetDark else ColorSchemeMode.MonetLight
    } else if (isDark) ColorSchemeMode.Dark else ColorSchemeMode.Light

    val controller = remember(colorSchemeMode, customSeedColor) {
        ThemeController(
            colorSchemeMode = colorSchemeMode,
            keyColor = customSeedColor,
            paletteStyle = ThemePaletteStyle.TonalSpot,
            colorSpec = ThemeColorSpec.Spec2021,
        )
    }

    MiuixTheme(controller = controller) {
        content()
    }
}