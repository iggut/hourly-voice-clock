## 2026-06-20 - Redundant UI Generation Loops
**Learning:** Checking conditional variables (`if (showFullText)`) inside high-frequency loops (like rendering `DayOfWeek.entries` on every recomposition) or creating unneeded allocations (like calculating colors manually per iteration) incurs measurable CPU overhead over time, even with a small number of items.
**Action:** Extract loop-invariant conditions and shared object instantiations outside of loops. Pre-allocate variables (such as common `Modifier.fillMaxWidth()` modifiers) and branch the entire loop instead of branching within it.

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

## 2024-08-01 - View Model Date/Time Reallocation Optimization
**Learning:** In high-frequency loops, repeatedly instantiating `LocalDateTime.now()` multiple times per tick, and subsequently calling object allocating methods like `.truncatedTo()` and `.plusHours()`, puts unnecessary pressure on garbage collection.
**Action:** Prevent redundant allocations in loops by instantiating `LocalDateTime.now()` only once per tick, sharing the reference, and performing integer comparisons against cached primitive variables (e.g. `now.hour`, `now.minute`) rather than creating and comparing temporary Date objects for unchanged values.

## 2024-06-21 - [Fast JSON Array Traversal]
**Learning:** Using JSON Array index-based traversal repeatedly (e.g. `optJSONObject(i)`) can be slower than primitive string index checks (`indexOf`) or avoiding `.length()` calls when searching for a specific substring within the raw JSON string or extracting the array length once.
**Action:** When extracting a specific value out of a potentially large JSON array (like an assets list), use `indexOf` on the stringified array first to find the target substring, then extract around it, and fallback to strict JSON parsing only if necessary.

## 2024-08-01 - Blocking I/O in Async Context Fix (Thread.sleep to Coroutines)
**Learning:** In Android, creating raw `Thread { ... }.start()` on demand is expensive and resource-intensive. Furthermore, using `Thread.sleep` to wait for I/O operations (like audio playback) blocks the entire underlying thread, leading to thread starvation and excessive memory overhead.
**Action:** Replace raw threads with Kotlin Coroutines using `Dispatchers.IO`. Use `coroutineScope.launch { ... }` for async work, and replace blocking `Thread.sleep` calls with non-blocking, suspending `delay()` functions to free up the thread pool for other tasks during wait times.

## 2024-05-30 - Compose Modifier Hoisting vs Locale Memoization
**Learning:** While Jetpack Compose guidelines occasionally advise against using `remember` for extremely lightweight modifier chains (like `.size(40.dp).clip(CircleShape)`) because the state-tracking overhead might exceed the allocation savings, utilizing `remember(locale) { ... }` to cache heavy `java.time` string formatting (e.g. `day.getDisplayName(...)`) outside of recomposition loops yields substantial and recommended performance gains.
**Action:** Prioritize memoizing heavy operations (like string formatting or Date/Time parsing) outside of high-frequency UI generation loops. If extracting modifiers, consider simply pulling them out as standard loop-invariant constants rather than forcing them into `remember` blocks unless they contain computationally expensive derivations.
