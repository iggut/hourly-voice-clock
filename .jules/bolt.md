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

## 2026-06-11 - Avoid String Allocations in High-Frequency Loops
**Learning:** In a 1-second `ViewModel` UI tick loop, operations like `now.second.toString().padStart(2, '0')` generate an unnecessary string allocation and put pressure on garbage collection on every single frame.
**Action:** Cache string conversions and use precomputed arrays (e.g., an array of strings for seconds `00` to `59`) to eliminate garbage collection overhead in high-frequency tick loops.
