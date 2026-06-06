## 2024-05-27 - DateTimeFormatter Optimization
**Learning:** In Android apps using Compose, formatting logic within `ViewModel` loops or UI components shouldn't repeatedly initialize `DateTimeFormatter` as parsing format patterns creates unnecessary overhead.
**Action:** Extract standard formats into companion objects or static properties, preventing unnecessary CPU usage and object allocation across recompositions and frequent logic ticks.

## 2026-05-28 - Compose Animation Recomposition Optimization
**Learning:** In Jetpack Compose, directly reading a rapidly changing state (like an infinite transition animation value such as `pulseAlpha`) in a standard modifier like `Modifier.background()` causes the entire composable to recompose on every single frame.
**Action:** Prevent unnecessary recompositions by reading animated state values inside lambda-based modifiers that defer the state read to the drawing phase, such as `Modifier.graphicsLayer { alpha = pulseAlpha }`.

## 2026-06-06 - ViewModel Loop Formatting Optimization
**Learning:** In Android apps, doing continuous string formatting (like formatting a date string or calculating the next hour string) inside a high-frequency (e.g., 1-second) `ViewModel` loop creates unnecessary CPU overhead and memory allocations when the underlying value only changes once a day or once an hour.
**Action:** Always track the underlying unit of time (e.g., `dayOfYear` or `hour`) and only re-format strings when the time unit has actually changed.
