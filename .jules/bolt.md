## 2024-05-27 - DateTimeFormatter Optimization
**Learning:** In Android apps using Compose, formatting logic within `ViewModel` loops or UI components shouldn't repeatedly initialize `DateTimeFormatter` as parsing format patterns creates unnecessary overhead.
**Action:** Extract standard formats into companion objects or static properties, preventing unnecessary CPU usage and object allocation across recompositions and frequent logic ticks.

## 2026-05-28 - Compose Animation Recomposition Optimization
**Learning:** In Jetpack Compose, directly reading a rapidly changing state (like an infinite transition animation value such as `pulseAlpha`) in a standard modifier like `Modifier.background()` causes the entire composable to recompose on every single frame.
**Action:** Prevent unnecessary recompositions by reading animated state values inside lambda-based modifiers that defer the state read to the drawing phase, such as `Modifier.graphicsLayer { alpha = pulseAlpha }`.
## 2024-05-29 - View Model Loop String Formatting Optimization
**Learning:** In Android, executing string formatting methods (such as `DateTimeFormatter.format()`) on variables that change infrequently (like dates or the target of the next hour) inside high-frequency intervals (like a 1-second view model UI tick loop) generates unnecessary string allocations and CPU overhead, putting pressure on garbage collection.
**Action:** Prevent redundant string re-allocations and recalculations by caching the last processed target variable (like `java.time.LocalDate`) and only executing formatting logic when the target state actually changes.
## 2024-06-03 - Avoid `String.split` in UI on Frame Update
**Learning:** In Jetpack Compose, passing pre-formatted complex strings (like a full time with seconds) to the UI that must then be split (`String.split`) to be rendered generates unnecessary garbage collection pressure and CPU overhead on every recomposition.
**Action:** Extract formatters and perform formatting/splitting in the `ViewModel` (or equivalent), passing individual, pre-processed parts in a data class (e.g., `TimeDisplayState`) to the UI, minimizing recomposition overhead.

## 2024-08-01 - Prevent GC pressure from `padStart` in 1-second ticks
**Learning:** In high-frequency ViewModel loops (like updating seconds every tick), using `toString().padStart(2, '0')` continually allocates new String objects, putting pressure on garbage collection.
**Action:** Use a precomputed static array of formatted strings (e.g., `Array(60) { it.toString().padStart(2, '0') }`) and fetch values using primitive indices (like `now.second`) to eliminate string allocations in the update loop.
