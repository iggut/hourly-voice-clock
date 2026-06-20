## 2026-06-20 - Redundant UI Generation Loops
**Learning:** Checking conditional variables (`if (showFullText)`) inside high-frequency loops (like rendering `DayOfWeek.entries` on every recomposition) or creating unneeded allocations (like calculating colors manually per iteration) incurs measurable CPU overhead over time, even with a small number of items.
**Action:** Extract loop-invariant conditions and shared object instantiations outside of loops. Pre-allocate variables (such as common `Modifier.fillMaxWidth()` modifiers) and branch the entire loop instead of branching within it.
