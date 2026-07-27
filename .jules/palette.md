## 2024-05-27 - Expanding touch targets for compose settings

**Learning:** When building settings screens using Jetpack Compose components like `RadioButton` and `Switch`, the touch target of the component itself is quite small and requires precise interaction, which is bad for accessibility and UX.

**Action:** Wrap the component and its text label in a `Row` with `Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { ... }`, and set the inner component's click/change handler to `null` so the entire row serves as a large, accessible touch target.

## 2024-05-24 - Fix Expand/Collapse Affordance
**Learning:** Found an accordion element ("OEM Specific Instructions") using a `Warning` icon when expanded, instead of a standard downward arrow. This breaks user expectations for interaction patterns where expanded sections should use `KeyboardArrowDown` to indicate state.
**Action:** Always ensure expand/collapse toggles use standard icon pairs (e.g., `KeyboardArrowRight` for collapsed, `KeyboardArrowDown` or `KeyboardArrowUp` for expanded) to provide correct affordances.

## 2026-05-30 - Fix AlertDialog Switch touch target
**Learning:** Found a `Switch` element in an AlertDialog that wasn't correctly wrapped in a clickable `Row` like the other settings, highlighting an accessibility issue pattern specific to this app's components where setting list items get proper accessible touch targets but modal/dialog settings sometimes get overlooked.
**Action:** Always verify that interactive components inside dialogs and modals receive the same accessible touch target wrapping (`Row` + `.clickable`) as the main screen components.

## 2024-05-30 - Disabled primary button feedback
**Learning:** Disabled action buttons (like 'Announce Now' during TTS initialization) without visual explanations cause user confusion, as the user doesn't know why they cannot interact with a primary feature.
**Action:** Always provide contextual feedback for disabled primary buttons, either by morphing the button text (e.g., 'Initializing Voice...') or adding an inline loading indicator (e.g. `CircularProgressIndicator`), rather than leaving it inexplicably unresponsive.

## 2024-05-31 - Split long text field labels into label and placeholder
**Learning:** Found `OutlinedTextField` instances with very long, instructional labels like "Prefix (e.g., 'Hello, it is now ')". This creates visual clutter and text truncation when the label floats up upon focus or input.
**Action:** Always improve form field labels by splitting long instructional labels into a concise, floating `label` (e.g., "Prefix") and moving the example text into the `placeholder` property (e.g., "e.g., 'Hello, it is now '") to guide users cleanly.

## 2026-06-10 - Screen reader semantics for Compose Sliders
**Learning:** Found that `Slider` components in Jetpack Compose do not automatically inherit semantic descriptions from adjacent `Text` elements used as visual labels, leading to screen readers simply announcing "Slider, 50%" without context. This is an accessibility issue pattern specific to custom control layouts.
**Action:** Always ensure interactive Compose components like `Slider` explicitly define a `contentDescription` using `Modifier.semantics { contentDescription = "[Label]" }` when their visual label is implemented as a separate `Text` component.

## 2024-06-11 - Screen reader fallback descriptions
**Learning:** Found an IconButton that used an optional description string in its contentDescription without a fallback, which could result in screen readers reading out an incomplete sentence if the data was missing.
**Action:** Always ensure dynamic string templates used for `contentDescription` implement fallback logic (e.g. `.ifBlank { fallback }`) to guarantee complete and meaningful announcements.

## 2024-06-12 - Confirmation dialogs for destructive actions
**Learning:** Deleting large downloaded voice models without confirmation causes accidental data loss and forces users to re-download 100MB+ files. This is a critical UX/accessibility failure for destructive actions.
**Action:** Always trigger a confirmation `AlertDialog` before executing destructive actions (e.g., deleting downloaded local voice models) to prevent accidental data loss.
## 2026-06-26 - Bounded Touch Ripples for Interactive Rows\n**Learning:** When applying the `.clickable` modifier to a `Row` or container in Jetpack Compose, failing to add a clipping modifier (e.g., `.clip(RoundedCornerShape(...))`) immediately *before* `.clickable` causes the ripple effect to awkwardly spill outside the container's visual bounds. This lacks visual polish.\n**Action:** Always ensure the ripple effect is properly bounded by applying a `.clip(...)` modifier immediately *before* `.clickable` for Jetpack Compose list items or rows.
## 2024-07-08 - Use toggleable/selectable for Compose settings
**Learning:** Found that when wrapping Jetpack Compose `Switch` or `RadioButton` settings elements in a clickable row, using `Modifier.clickable` fails to correctly merge screen reader semantics and roles. Screen readers won't announce the state change intuitively.
**Action:** Always use `Modifier.toggleable` (with `Role.Switch`) or `Modifier.selectable` (with `Role.RadioButton`) instead of `Modifier.clickable` on the parent container when building interactive setting items.
## 2024-10-25 - Screen reader semantics for Compose expand/collapse accordions
**Learning:** Found an accordion element ("OEM Specific Instructions") using a `Modifier.clickable` modifier without a `role` or semantic state, meaning screen readers cannot announce its expanded/collapsed state to users. This is an accessibility gap for standard expand/collapse toggle rows.
**Action:** Always ensure accordion-style rows use explicit roles and states via `Modifier.clickable(role = Role.Button)` and `Modifier.semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }` with localized strings.

## 2024-10-26 - Use localized string resources for screen reader semantics
**Learning:** Found a hardcoded "Back" string for `contentDescription` in an `IconButton` used for navigation. Hardcoding accessibility descriptions ignores non-English users, rendering screen reader announcements unhelpful for them.
**Action:** Always use `stringResource(R.string.[name])` for `contentDescription` or semantic tags to ensure i18n support.
