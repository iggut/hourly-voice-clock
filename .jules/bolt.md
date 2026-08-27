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

## 2024-05-29 - View Model Loop Clock Update Optimization
**Learning:** Checking whether it is time to format current Date/Time objects by comparing entire standard `LocalDateTime` properties can be inefficient inside of a 1-second view model update loop as many variables remain static 59/60 seconds.
**Action:** Extract formatters and cache a primitive hash representation of the hour/minute inside `ViewModel` to update time formats sparingly, only updating the seconds via a separate data class copy while letting strings recompute when the minute transition hash detects a change.

## 2024-08-01 - Throttling High-Frequency State in ViewModels
**Learning:** In Android ViewModels with high-frequency timers (e.g., 1-second ticks), feeding the high-frequency state directly into `combine` operators causes unnecessary CPU and GC overhead if the derived state only requires minute-level resolution (e.g., checking if quiet hours are active).
**Action:** Create a separate, throttled `StateFlow` (e.g., `_nowMinute`) that only emits when the minute actually changes. Use this throttled flow for derived states that don't need second-by-second updates.

## 2024-05-30 - Compose Array Allocation Optimization
**Learning:** In Kotlin 1.9+, using `Enum.values()` creates a new array allocation on every call. When used inside a Jetpack Compose `@Composable` function (e.g., looping through `entries.forEach`), this array allocation happens on every single recomposition, putting unnecessary pressure on the garbage collector.
**Action:** Always use the `Enum.entries` property instead of `Enum.values()` to iterate over enums in Compose. `entries` returns a pre-allocated, unmodifiable list, eliminating per-frame array allocations.

## 2024-05-30 - Prevent Format Parsing Overhead
**Learning:** Checking conditional variables and executing string formatting methods (such as `String.format()`) within moderately frequent generation functions (like generating the current time announcement) incurs a small but measurable CPU overhead due to parsing the format string at runtime.
**Action:** Replace `String.format` with manual string interpolation combined with `padStart` (e.g. `minute.toString().padStart(2, '0')`) to completely avoid the format parsing overhead while maintaining code readability.

## 2024-05-30 - View Model Date/Time Epoch Day Hash Optimization
**Learning:** In high-frequency loops (e.g. 1-second view model update loops), repeatedly computing `now.toLocalDate().toEpochDay()` for hash comparisons evaluates math-heavy calculations on every single tick, causing unnecessary overhead.
**Action:** Extract loop-invariant operations by comparing cheap primitives like `now.year` and `now.dayOfYear`. Only recalculate the `epochDay` hash when the date has actually changed.

## 2024-08-01 - Compose drawWithContent vs drawWithCache Optimization
**Learning:** In Jetpack Compose, directly reading scroll states and performing object allocations (like instantiating `Brush.horizontalGradient` objects for edge fading) inside `Modifier.drawWithContent` causes those allocations to run on every single frame during scrolling, putting unnecessary pressure on the garbage collector.
**Action:** Replace `Modifier.drawWithContent` with `Modifier.drawWithCache`. Pre-calculate layout-size-dependent drawing objects (like `Brush`) in the cache block, and only perform state reads (like `scrollState.canScrollForward`) and standard primitive drawing commands inside the returned `onDrawWithContent` lambda. This ensures objects are allocated only when layout bounds change, while still reacting accurately to state changes frame-by-frame.

## 2024-08-19 - Compose remember vs rememberSaveable Optimization
**Learning:** Using `remember { mutableStateOf(false) }` for simple boolean UI states (like dialog visibility or expansion toggles) causes state loss and unintended UI resets (e.g., dialogs closing) during configuration changes like screen rotations.
**Action:** Use `rememberSaveable { mutableStateOf(...) }` instead of `remember` for these simple UI states to gracefully preserve them across configuration changes.

## 2024-08-20 - View Model Clock Tick Allocation Optimization
**Learning:** In 1-second timer loops (e.g. updating the UI clock), allocating a new Date/Time object (like `LocalDateTime.now()`) on every tick causes unnecessary garbage collection pressure when the loop inevitably drifts and ticks multiple times in the same physical second.
**Action:** Expose `System.currentTimeMillis()` from the `Clock` interface instead of bypassing it, and use it to quickly check if the actual epoch second has changed before allocating a new immutable Date/Time object, caching and reusing the previous instance if the second is identical.


## 2024-08-27 - Format Pattern Garbage Collection Optimization
**Learning:** Calling `padStart(2, '0')` on integer properties like `minute` within a `DateTimeFormatter` context or an announcement text formatter creates string allocations repeatedly in high-frequency rendering or formatting paths, causing unnecessary garbage collection pressure.
**Action:** Extract padded string lookups (e.g., zero-padded numbers 0-59) into a statically allocated precomputed array (e.g. `PADDED_NUMBERS`), replacing `toString().padStart()` with a fast array indexing lookup.
