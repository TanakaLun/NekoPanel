# NekoPanel — Agent Guide

## Project
Clash API client panel for Android. Jetpack Compose + Miuix (HyperOS) UI.

## Build & Dependencies
- AGP 9.3.1, Kotlin 2.4.10, Compose BOM 2026.06.01
- **NO Material3** — Miuix is the only design system
- Miuix: `miuix-ui`, `miuix-preference`, `miuix-icons` via Maven
- Blur: snapshot AAR at `app/libs/miuix-blur-android-0.9.3-9686db2b-*.aar`
- Nav: snapshot AAR at `app/libs/miuix-nav-android-0.9.3-9686db2b-*.aar`
- Snapshot AARs committed with `git add -f` (`.gitignore` has `*.aar`)
- **No local compilation** — commit directly; CI compiles on `origin/miuix`

## Navigation (miuix-nav)
- Uses `top.yukonga.miuix.kmp.nav.core.*` package (NOT `androidx.navigation3.*`)
- `Route` sealed interface extends `NavKey`, annotated with `@Serializable`
- `Navigator` class wraps `NavBackStack` (not `SnapshotStateList`)
- `rememberNavBackStack<Route>(Route.Main)` creates the back stack
- `NavDisplay(backStack, onBack, transition, effects) { entry<Route.X>(swipeDismiss) { } }` DSL
- `NavDisplayEffects` for corner clip, dim, backdrop color
- `NavTransitions.MiuixDefault` is the standard transition
- `NavSwipeDirection.LeftToRight` for swipe-back (per-entry configurable)
- `rememberNavSystemCornerRadius()` for screen corner radius
- `NavCornerClipMode.Leading` for slide transitions
- Navigation state (`selectedTab`, etc.) managed via `AppState` + `LocalAppState`/`LocalUpdateAppState`

## Blur
- `rememberBlurBackdrop(): LayerBackdrop?` — returns `null` when shader unsupported
- `BlurredBar(backdrop, blurActive, scrollBehavior, isProgressive, content)` — wraps bars with `textureBlur` (Gaussian) or `progressiveTextureBlur` (Progressive)
- Both helpers in `ui/BlurExt.kt`
- **No outer `Box(Modifier.background(...))`** around Scaffold — breaks backdrop capture
- `barColor = Color.Transparent` when blur active, `surfaceColor` when not
- Progressive: `gradient = ProgressiveBlur.Top.copy(curve = 2.2f)`, `blurRadius = 10f`
- Gaussian: `blurRadius = 25f`, `BlendColorEntry(surfaceColor.copy(0.8f))`

## Architecture
- `MainActivity.kt` → `NekoPanelApp` (theme setup) → `NekoPanelMain` (NavDisplay host)
- `AppState.kt`: theme/UI state data class + CompositionLocals (Miuix example pattern)
- `WebSocketState.kt`: StateFlow-like holder for memory/traffic/logs/connections data
- Screens: ProxiesScreen, RulesScreen, TrafficScreen, FullSettingsScreen, UiSettingsScreen, BackupScreen
- API client: `ApiClient` (singleton, OkHttp WebSocket)
- Settings: `SettingsManager` (Room-backed, cached in `ConcurrentHashMap`)

## Key Conventions
- Follow Miuix example app (`../miuix/example`) patterns — not custom implementations
- `rememberLayerBackdrop` + `textureBlur` for NavigationBar and TopAppBar blur
- `rememberLayerBackdrop` created per-screen (inside Scaffold's scope, NOT wrapped in outer Box)
- `WindowSpinnerPreference`/`WindowDropdownPreference` for dropdowns
- Semantic colors: `primary`/`error`/`secondaryVariant` for severity
- `TopAppBar` with `scrollBehavior` + `nestedScroll` for scroll-aware toolbar
- `ConfigToggle` updates: use `JSONObject(config.toString()).also { it.put(...) }` to trigger recomposition
