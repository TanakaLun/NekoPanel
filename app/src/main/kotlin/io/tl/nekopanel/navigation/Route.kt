package io.tl.nekopanel.navigation

import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavKey

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Main : Route

    @Serializable
    data object UiSettings : Route

    @Serializable
    data object Backup : Route
}
