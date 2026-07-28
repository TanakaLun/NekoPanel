# NekoPanel — Agent Guide

## Project
Clash API client panel for Android. Jetpack Compose + Miuix (HyperOS) UI.

## Build & Dependencies
- AGP 9.3.1, Kotlin 2.4.10, Compose BOM 2026.06.01
- **NO Material3** — Miuix is the only design system
- Miuix artifacts: `top.yukonga.miuix.kmp:miuix-{module}-android:0.9.3`
  - `miuix-ui-android`, `miuix-preference-android`, `miuix-icons-android`
  - `miuix-navigation3-ui-android` (AndroidX Nav3 wrapper), `miuix-blur-android` (blur)
- Navigation3: `androidx.navigation3:navigation3-runtime`, `androidx.navigationevent:navigationevent-compose`
- Key deps: OkHttp 5.4, Coil 2.7, Room 2.8
- **No local compilation** — commit directly; CI compiles

## Navigation Pattern
- Uses `androidx.navigation3.ui.NavDisplay` with `entryProvider` DSL (KernelSU pattern)
- `Route` sealed interface extending `NavKey` (Main/UiSettings/Backup)
- `Navigator` class with `SnapshotStateList<NavKey>` back stack
- `LocalNavigator` CompositionLocal for screen access
- `rememberDecoratedNavEntries()` + `rememberSaveableStateHolderNavEntryDecorator()` for lifecycle management
- NavDisplay handles predictive back gesture and transitions automatically
- **NO** `AnimatedContent`, `BackHandler`, or manual `currentPage` state
- Example references: `../KernelSU` (full Navigation3 pattern), `../miuix/example` (component usage)

## Theme
- `NekoPanelTheme` uses `MiuixTheme` with `ThemeController` (seed-color Monet palette)
- 18 Japanese/Chinese seed colors in `AllThemeSchemes` (Color.kt)
- Dark mode via `themeMode` param ("follow_system"/"light"/"dark")
- Dynamic color (wallpaper) beats seed color: `effectiveSeedColor = null` when `dynamicColor = true`
- No custom `lightScheme`/`darkScheme` — ThemeController generates from keyColor

## Architecture
- `MainActivity.kt` → `NekoPanelApp` → `NekoPanelMain` (NavDisplay host)
- Screens: ProxiesScreen, RulesScreen, TrafficScreen, FullSettingsScreen, UiSettingsScreen, BackupScreen
- WebSocket streams (memory, traffic, logs, connections) collected in `NekoPanelMain`
- API client: `ApiClient` (singleton, OkHttp WebSocket)
- Settings: `SettingsManager` (Room-backed), persisted via `SettingsDao`
- Service: `DataDaemonService` (foreground, background WebSocket)

## Key Conventions
- Always follow Miuix example app patterns — not custom implementations
- Use `rememberLayerBackdrop` + `textureBlur` for NavigationBar and TopAppBar blur effects
- `WindowSpinnerPreference`/`WindowDropdownPreference` for dropdowns (no Scaffold ancestor needed)
- `rememberSaveable` for state that must survive page switches (NavDisplay keeps entries alive)
- Semantic colors: `primary`/`error`/`secondaryVariant` for severity, `onSurfaceVariant`/`onSurfaceVariantSummary` for labels
- `TopAppBar` with `scrollBehavior` + `nestedScroll` for scroll-aware toolbar
- `ConfigToggle` updates: use `JSONObject(config.toString()).also { it.put(...) }` to trigger recomposition
