
## 2024-05-27 - Expanding touch targets for compose settings

**Learning:** When building settings screens using Jetpack Compose components like `RadioButton` and `Switch`, the touch target of the component itself is quite small and requires precise interaction, which is bad for accessibility and UX.

**Action:** Wrap the component and its text label in a `Row` with `Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { ... }`, and set the inner component's click/change handler to `null` so the entire row serves as a large, accessible touch target.

## 2024-05-24 - Fix Expand/Collapse Affordance
**Learning:** Found an accordion element ("OEM Specific Instructions") using a `Warning` icon when expanded, instead of a standard downward arrow. This breaks user expectations for interaction patterns where expanded sections should use `KeyboardArrowDown` to indicate state.
**Action:** Always ensure expand/collapse toggles use standard icon pairs (e.g., `KeyboardArrowRight` for collapsed, `KeyboardArrowDown` or `KeyboardArrowUp` for expanded) to provide correct affordances.

## 2026-05-30 - Fix AlertDialog Switch touch target
**Learning:** Found a `Switch` element in an AlertDialog that wasn't correctly wrapped in a clickable `Row` like the other settings, highlighting an accessibility issue pattern specific to this app's components where setting list items get proper accessible touch targets but modal/dialog settings sometimes get overlooked.
**Action:** Always verify that interactive components inside dialogs and modals receive the same accessible touch target wrapping (`Row` + `.clickable`) as the main screen components.
