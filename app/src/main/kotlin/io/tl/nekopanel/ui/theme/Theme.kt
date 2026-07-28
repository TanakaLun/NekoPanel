package io.tl.nekopanel.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
import top.yukonga.miuix.kmp.theme.platformDynamicColors

@Composable
fun NekoPanelTheme(
    pureBlackMode: Boolean = false,
    themeMode: String = "follow_system",
    dynamicColor: Boolean = true,
    customPrimaryKey: String = "",
    content: @Composable () -> Unit,
) {
    val isDark = themeMode == "dark" || (themeMode == "follow_system" && isSystemInDarkTheme())
    val context = LocalContext.current

    val dynamicLight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        platformDynamicColors(false)
    } else null
    val dynamicDark = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        platformDynamicColors(true)
    } else null

    val colorSchemeMode = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dynamicLight != null && dynamicDark != null ->
            if (isDark) ColorSchemeMode.MonetDark else ColorSchemeMode.MonetLight
        customPrimaryKey.isNotBlank() -> ColorSchemeMode.Light
        else -> if (isDark) ColorSchemeMode.Dark else ColorSchemeMode.Light
    }

    val customLight = if (customPrimaryKey.isNotBlank()) {
        resolveThemeColors(customPrimaryKey, dark = false, pureBlack = false)
    } else null

    var customDark = if (customPrimaryKey.isNotBlank()) {
        resolveThemeColors(customPrimaryKey, dark = true, pureBlack = pureBlackMode)
    } else if (pureBlackMode) {
        darkColorScheme().copy(
            background = Color.Black, surface = Color.Black,
            surfaceVariant = Color(0xFF121212),
            surfaceContainer = Color.Black, surfaceContainerHigh = Color.Black, surfaceContainerHighest = Color.Black,
        )
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