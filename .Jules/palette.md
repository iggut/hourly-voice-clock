## 2023-10-27 - Redundant accessibility descriptions in merged semantic trees
**Learning:** When using `Modifier.semantics(mergeDescendants = true)` on a parent container in Jetpack Compose (such as a custom `DashboardCard`), any inner text elements are grouped and announced together. If purely visual or structural icons (like a navigation chevron) inside that group also have a `contentDescription` set, they will be announced as well, causing redundancy (e.g. reading the title, then "Navigate forward").
**Action:** Always set `contentDescription = null` for purely decorative or structural icons inside `mergeDescendants = true` containers, especially when the parent's semantic `onClick(label="...")` already conveys the full action context.

## 2023-11-20 - Redundant screen reader announcements in Buttons containing Icons
**Learning:** In Jetpack Compose, when an `Icon` is placed inside a `Button` that also contains a `Text` node, providing a `contentDescription` for the `Icon` causes a redundant announcement by the screen reader if the `Text` node conveys the exact same semantic meaning (e.g., "Announce Now"). Screen readers will announce both the icon description and the text description sequentially.
**Action:** Always set `contentDescription = null` for an `Icon` if it is housed within a `Button` (or similar container) immediately adjacent to a `Text` component that provides the same or a sufficiently descriptive label.

## 2023-11-20 - Redundant screen reader announcements in Buttons containing Icons (Part 2)
**Learning:** Extending the previous learning, this redundancy applies broadly to any `Button` or interactive container that pairs an `Icon` with a descriptive `Text`. For instance, in `VoiceSettingsScreen` the "Preview Voice" button, and in `LocalVoiceSettingsScreen` the "Download" button, both paired icons with text that fully described the action. Similarly, the "Info" icon next to the "Single voice engine detected" text was redundant.
**Action:** When auditing codebase for accessibility, search for `Icon` usage inside `Button`s or `Row`s and verify whether adjacent text makes the icon's `contentDescription` redundant. If so, set it to `null`.
