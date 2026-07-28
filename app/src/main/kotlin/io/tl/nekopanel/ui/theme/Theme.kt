package io.tl.nekopanel.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

@Composable
fun NekoPanelTheme(
    themeMode: String = "follow_system",
    dynamicColor: Boolean = true,
    customPrimaryKey: String = "",
    content: @Composable () -> Unit,
) {
    val isDark = themeMode == "dark" || (themeMode == "follow_system" && isSystemInDarkTheme())

    val colorSchemeMode = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isDark) ColorSchemeMode.MonetDark else ColorSchemeMode.MonetLight
    } else if (isDark) ColorSchemeMode.Dark else ColorSchemeMode.Light

    val customLight = if (!dynamicColor && customPrimaryKey.isNotBlank()) {
        resolveThemeColors(customPrimaryKey, dark = false)
    } else null

    val customDark = if (!dynamicColor && customPrimaryKey.isNotBlank()) {
        resolveThemeColors(customPrimaryKey, dark = true)
    } else null

    val controller = remember(colorSchemeMode, customLight, customDark) {
        ThemeController(
            colorSchemeMode = colorSchemeMode,
            lightColors = customLight ?: lightColorScheme(),
            darkColors = customDark ?: darkColorScheme(),
        )
    }

    MiuixTheme(controller = controller) {
        content()
    }
}