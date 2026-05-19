package io.tl.nekopanel.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

fun resolveThemeScheme(customKey: String, dark: Boolean, pureBlack: Boolean) =
    JapaneseThemeSchemes.firstOrNull { it.key == customKey }?.let { scheme ->
        val base = if (dark) scheme.darkScheme() else scheme.lightScheme()
        if (dark && pureBlack) base.copy(
            background = Color.Black, surface = Color.Black,
            surfaceVariant = Color(0xFF121212),
            surfaceContainer = Color.Black, surfaceContainerLow = Color.Black, surfaceContainerLowest = Color.Black,
        ) else base
    }

@Composable
fun ComposeEmptyActivityTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    pureBlackMode: Boolean = false,
    customPrimaryKey: String = "",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val dynamicScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (darkTheme && pureBlackMode) {
                dynamicScheme.copy(
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceVariant = Color(0xFF121212),
                    surfaceContainer = Color.Black,
                    surfaceContainerLow = Color.Black,
                    surfaceContainerLowest = Color.Black
                )
            } else {
                dynamicScheme
            }
        }
        customPrimaryKey.isNotBlank() ->
            resolveThemeScheme(customPrimaryKey, darkTheme, pureBlackMode) ?: if (darkTheme) DarkColorScheme else LightColorScheme
        darkTheme -> {
            if (pureBlackMode) {
                DarkColorScheme.copy(
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceVariant = Color(0xFF121212)
                )
            } else {
                DarkColorScheme
            }
        }
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
