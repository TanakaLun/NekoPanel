package io.tl.nekopanel.navigation

import androidx.navigation3.runtime.NavKey

sealed interface Route : NavKey {
    data object Main : Route
    data object UiSettings : Route
    data object Backup : Route
}
