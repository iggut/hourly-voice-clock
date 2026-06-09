## 2024-05-27 - DateTimeFormatter Optimization
**Learning:** In Android apps using Compose, formatting logic within `ViewModel` loops or UI components shouldn't repeatedly initialize `DateTimeFormatter` as parsing format patterns creates unnecessary overhead.
**Action:** Extract standard formats into companion objects or static properties, preventing unnecessary CPU usage and object allocation across recompositions and frequent logic ticks.

## 2026-05-28 - Compose Animation Recomposition Optimization
**Learning:** In Jetpack Compose, directly reading a rapidly changing state (like an infinite transition animation value such as `pulseAlpha`) in a standard modifier like `Modifier.background()` causes the entire composable to recompose on every single frame.
**Action:** Prevent unnecessary recompositions by reading animated state values inside lambda-based modifiers that defer the state read to the drawing phase, such as `Modifier.graphicsLayer { alpha = pulseAlpha }`.
## 2024-05-29 - View Model Loop String Formatting Optimization
**Learning:** In Android, executing string formatting methods (such as `DateTimeFormatter.format()`) on variables that change infrequently (like dates or the target of the next hour) inside high-frequency intervals (like a 1-second view model UI tick loop) generates unnecessary string allocations and CPU overhead, putting pressure on garbage collection.
**Action:** Prevent redundant string re-allocations and recalculations by caching the last processed target variable (like `java.time.LocalDate`) and only executing formatting logic when the target state actually changes.
## 2024-06-09 - View Model UI Fast Ticks Optimization
**Learning:** In Jetpack Compose, passing fully pre-formatted complex strings to the UI (e.g. `12:30:45 AM`) and requiring the UI to frequently parse and split it (`String.split()`) on every fast recomposition frame causes significant garbage collection pressure.
**Action:** Extract standard time components inside the `ViewModel` (e.g., using a `TimeComponents` data class to avoid redundant `DateTimeFormatter` parsing) and pass the explicit properties (hours, minutes, seconds) separately to the UI to bypass all UI-layer string manipulation logic during fast tick loops.
