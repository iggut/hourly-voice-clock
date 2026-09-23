## 2023-10-27 - Redundant accessibility descriptions in merged semantic trees
**Learning:** When using `Modifier.semantics(mergeDescendants = true)` on a parent container in Jetpack Compose (such as a custom `DashboardCard`), any inner text elements are grouped and announced together. If purely visual or structural icons (like a navigation chevron) inside that group also have a `contentDescription` set, they will be announced as well, causing redundancy (e.g. reading the title, then "Navigate forward").
**Action:** Always set `contentDescription = null` for purely decorative or structural icons inside `mergeDescendants = true` containers, especially when the parent's semantic `onClick(label="...")` already conveys the full action context.

## 2023-11-20 - Redundant screen reader announcements in Buttons containing Icons
**Learning:** In Jetpack Compose, when an `Icon` is placed inside a `Button` that also contains a `Text` node, providing a `contentDescription` for the `Icon` causes a redundant announcement by the screen reader if the `Text` node conveys the exact same semantic meaning (e.g., "Announce Now"). Screen readers will announce both the icon description and the text description sequentially.
**Action:** Always set `contentDescription = null` for an `Icon` if it is housed within a `Button` (or similar container) immediately adjacent to a `Text` component that provides the same or a sufficiently descriptive label.

## 2023-11-20 - Redundant screen reader announcements in Buttons containing Icons (Part 2)
**Learning:** Extending the previous learning, this redundancy applies broadly to any `Button` or interactive container that pairs an `Icon` with a descriptive `Text`. For instance, in `VoiceSettingsScreen` the "Preview Voice" button, and in `LocalVoiceSettingsScreen` the "Download" button, both paired icons with text that fully described the action. Similarly, the "Info" icon next to the "Single voice engine detected" text was redundant.
**Action:** When auditing codebase for accessibility, search for `Icon` usage inside `Button`s or `Row`s and verify whether adjacent text makes the icon's `contentDescription` redundant. If so, set it to `null`.

## 2023-11-20 - Missing accessibility semantics for `selectable` elements in `Row`s
**Learning:** When using `Modifier.selectable(role = Role.RadioButton)` or `Modifier.toggleable` on a parent `Row` containing a `RadioButton` (or `Switch`) and text, if `.semantics(mergeDescendants = true) {}` is missing before the `.selectable` modifier, screen readers may not correctly group the text label and the selectable state into a single coherent element for the user.
**Action:** Always verify that interactive `Row`s or `Card`s representing radio buttons or switches include `.semantics(mergeDescendants = true) {}` in their modifier chain before `.selectable` or `.toggleable` to ensure grouped announcements.

## 2023-11-21 - Lack of Role and full semantics for custom checkboxes
**Learning:** When building custom multi-select UI elements (like a day-of-week picker using circles) using `Modifier.selectable`, relying purely on a short label (like "M" for Monday) without assigning an appropriate Role leads to poor screen reader accessibility. Users hear "M" instead of "Monday" and are not informed they are interacting with a checkbox.
**Action:** Always use `.semantics(mergeDescendants = true) { contentDescription = fullContext }` to override narrow UI labels with descriptive text, and explicitly provide `role = Role.Checkbox` inside the `Modifier.selectable` to convey the component's interaction model to assistive technology.
## 2023-11-20 - Unlabelled loading indicators next to text
**Learning:** When displaying a visual loading indicator (like `CircularProgressIndicator`) right next to text describing the status (e.g. "Downloading...", "Checking for updates"), screen readers will read the text but may not associate the indicator with it, or might just announce a generic "progress bar" out of context. To create a cohesive announcement for assistive technologies, they should be grouped.
**Action:** Apply `Modifier.semantics(mergeDescendants = true) {}` to the parent `Row` container that holds both the `CircularProgressIndicator` and the `Text`. This ensures the elements are grouped and announced together as a single status update.

## 2023-11-20 - Redundant screen reader announcements in interactive rows
**Learning:** Similar to icons inside buttons, when interactive `Row` elements (e.g., those using `Modifier.toggleable` or `Modifier.selectable`) have `.semantics(mergeDescendants = true)` applied to group the text label and state for screen readers, any structural or purely visual icons (like alarms or locks) adjacent to the text will cause redundant double-announcements if they also have a localized `contentDescription` set (e.g. `stringResource(R.string.a11y_clock)`).
**Action:** Always set `contentDescription = null` for purely decorative or visual status icons that are housed within a merged interactive `Row` immediately adjacent to `Text` that conveys the same meaning.
