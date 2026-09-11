## 2026-09-07 - ErrorChip touch target and semantic grouping
**Learning:** When making an interactive chip or error banner containing a message and a "Dismiss" action, making only the "Dismiss" text clickable creates an undersized touch target and causes screen readers to read the message and the action separately. Applying `.clickable` to the entire container along with `Modifier.semantics(mergeDescendants = true)` solves both issues.
**Action:** Always apply `.clickable` to the parent container of informational chips (and bound the ripple with `.clip`) instead of isolating interactivity to small text elements inside, and group their semantics.

## 2026-09-07 - Avoid redundant screen reader announcements on Buttons
**Learning:** When adding an `Icon` inside a `Button` that also contains a `Text` element describing the action, setting a `contentDescription` on the `Icon` causes the screen reader to read the action twice (e.g., "Download, Download").
**Action:** Always set `contentDescription = null` for `Icon` components when they are accompanied by a descriptive `Text` component within an interactive container like a `Button`.
