## 2024-05-27 - DateTimeFormatter Optimization
**Learning:** In Android apps using Compose, formatting logic within `ViewModel` loops or UI components shouldn't repeatedly initialize `DateTimeFormatter` as parsing format patterns creates unnecessary overhead.
**Action:** Extract standard formats into companion objects or static properties, preventing unnecessary CPU usage and object allocation across recompositions and frequent logic ticks.

## 2026-05-28 - Compose Animation Recomposition Optimization
**Learning:** In Jetpack Compose, directly reading a rapidly changing state (like an infinite transition animation value such as `pulseAlpha`) in a standard modifier like `Modifier.background()` causes the entire composable to recompose on every single frame.
**Action:** Prevent unnecessary recompositions by reading animated state values inside lambda-based modifiers that defer the state read to the drawing phase, such as `Modifier.graphicsLayer { alpha = pulseAlpha }`.

## 2024-06-05 - Time Formatting Recomposition Optimization
**Learning:** Formatting logic within 1-second `ViewModel` loops using `DateTimeFormatter.format()` generates excessive overhead as strings are parsed and formatted repeatedly every tick, even when the resulting output has not changed (e.g. date strings or upcoming hour announcements).
**Action:** Extract tracking variables (e.g., `dayOfYear` or `hour`) and cache formatted strings, only re-evaluating the `DateTimeFormatter` when the underlying value natively updates (e.g., `now.dayOfYear != lastDateDayOfYear`), significantly reducing CPU usage and GC thrashing.
