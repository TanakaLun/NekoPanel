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

// 这里的 Purple80 等颜色确保你在 Color.kt 中有定义，或者直接换成 Color(0xFF...)
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

@Composable
fun ComposeEmptyActivityTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    pureBlackMode: Boolean = false, // 新增参数
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    val colorScheme = when {
        // 动态配色逻辑
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val dynamicScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (darkTheme && pureBlackMode) {
                // 如果是暗色+纯黑模式，在动态配色的基础上修改背景
                dynamicScheme.copy(
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceVariant = Color(0xFF121212), // 稍微浅一点的黑色用于区分卡片
                    surfaceContainer = Color.Black,     // 适配新版本 M3 参数
                    surfaceContainerLow = Color.Black,
                    surfaceContainerLowest = Color.Black
                )
            } else {
                dynamicScheme
            }
        }
        // 普通暗色模式
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
        // 亮色模式
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
