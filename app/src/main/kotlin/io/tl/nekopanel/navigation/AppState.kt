package io.tl.nekopanel.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

@Stable
data class AppState(
    val themeMode: String = "follow_system",
    val dynamicColor: Boolean = true,
    val customColorKey: String = "",
    val transitionStyle: Int = 1,
    val blurStyle: Int = 0,
    val selectedTab: Int = 0,
    val trafficTab: Int = 0,
    val currentMode: String = "rule",
    val currentLogLevel: String = "info",
)

val LocalAppState = compositionLocalOf<AppState> { error("No AppState provided") }
val LocalUpdateAppState = staticCompositionLocalOf<((AppState) -> AppState) -> Unit> { error("No AppState updater provided") }
