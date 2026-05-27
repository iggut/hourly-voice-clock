
## 2024-05-27 - Expanding touch targets for compose settings

**Learning:** When building settings screens using Jetpack Compose components like `RadioButton` and `Switch`, the touch target of the component itself is quite small and requires precise interaction, which is bad for accessibility and UX.

**Action:** Wrap the component and its text label in a `Row` with `Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { ... }`, and set the inner component's click/change handler to `null` so the entire row serves as a large, accessible touch target.
