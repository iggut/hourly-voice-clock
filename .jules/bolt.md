## 2024-05-27 - DateTimeFormatter Optimization
**Learning:** In Android apps using Compose, formatting logic within `ViewModel` loops or UI components shouldn't repeatedly initialize `DateTimeFormatter` as parsing format patterns creates unnecessary overhead.
**Action:** Extract standard formats into companion objects or static properties, preventing unnecessary CPU usage and object allocation across recompositions and frequent logic ticks.

## 2026-05-28 - Compose Animation Recomposition Optimization
**Learning:** In Jetpack Compose, directly reading a rapidly changing state (like an infinite transition animation value such as `pulseAlpha`) in a standard modifier like `Modifier.background()` causes the entire composable to recompose on every single frame.
**Action:** Prevent unnecessary recompositions by reading animated state values inside lambda-based modifiers that defer the state read to the drawing phase, such as `Modifier.graphicsLayer { alpha = pulseAlpha }`.

## 2024-06-05 - Avoid repetitive string formatting in 1-second UI loops
**Learning:** Formatting strings (like `DateTimeFormatter.format()`) every second in ViewModel coroutines creates unnecessary string allocations and puts pressure on the garbage collector, even if the result is identical to the previous second (e.g. for dates or "next hour" announcements).
**Action:** Cache the formatted string and only update it when the underlying unit of time (date or hour) actually changes, reducing redundant formatting from 86,400 times/day to just a few times.
