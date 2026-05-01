---
name: android-platform-fluency
description: Governs all platform-specific design decisions for modern Android applications. Use this skill whenever the user asks about Material Design 3 (M3) components, dynamic color, tonal elevation, typography scales, Android navigation patterns (Bottom Navigation vs Rail), edge-to-edge layouts (WindowInsets), adaptive UI (WindowSizeClass), predictive back gestures, or Android accessibility standards like minimum touch targets.
---

# Android Platform Fluency

Designing for Android requires fluency in Material Design 3 (M3) and modern Android architecture guidelines. This skill ensures that generated interfaces feel native to the Android ecosystem, correctly utilizing the M3 token system, adaptive layouts, and platform-specific behaviors like edge-to-edge rendering and predictive back navigation. A design that fights the platform guidelines will feel alien, broken, or outdated to an Android user.

## Core Principles

1. **Tokens over Hex Codes.** M3 relies on a dynamic, relational color system. Never hardcode absolute hex values; always map UI elements to semantic color roles (Primary, Surface, Error) to support Dynamic Color and Dark Mode natively.
2. **Elevation is Tonal, not just Spatial.** M3 reduces reliance on drop shadows. Elevation is primarily communicated through subtle shifts in surface color lightness (Tonal Elevation).
3. **Embrace Edge-to-Edge.** Modern Android apps must draw behind the system navigation and status bars, handling `WindowInsets` to pad content appropriately.
4. **Adaptive by Default.** An Android app is not just a phone app. Layouts must adapt gracefully across phones, foldables, and tablets using standard `WindowSizeClass` breakpoints.
5. **Respect the Back Stack.** Android users rely heavily on the system back button/gesture. The UI must support predictive back and maintain a logical, predictable back stack.

## Rules

### R1: The M3 Color Token System
**Source**: Material Design 3 (Design Tokens)
**Rule**: Map all interface colors to the M3 token roles rather than absolute values. Use `Primary` for the most prominent actions, `Secondary` for less prominent elements, `Tertiary` for contrasting accents, `Error` for destructive actions, and `Surface` for backgrounds.
**Why**: Hardcoding hex colors breaks user-generated Dynamic Color (Material You) and makes dark mode implementation rigid and brittle.
**Check**: Are you defining a color by its hex code instead of its semantic M3 role?
**Bad example**: `background-color: #6200EE`
**Good example**: `background-color: colorPrimary` or `MaterialTheme.colorScheme.primary`

### R2: Surface and On-Surface Pairing
**Source**: Material Design 3 (Color Roles)
**Rule**: Every container color must be paired with its specific "On" color token for text and iconography (e.g., `Primary` container must use `On Primary` text; `Surface` must use `On Surface`).
**Why**: The "On" color tokens are mathematically calculated by the M3 palette generator to guarantee accessible contrast ratios against their parent containers in both light and dark modes.
**Check**: Is the text color inside a `Primary` button explicitly set to `On Primary`?
**Bad example**: Setting the text color of a `Primary` button to `#FFFFFF` manually.
**Good example**: Setting the text color of a `Primary` button to `MaterialTheme.colorScheme.onPrimary`.

### R3: Tonal Elevation Over Shadows
**Source**: Material Design 3 (Elevation)
**Rule**: Use M3 Tonal Elevation (shifting the background color slightly lighter or darker) to differentiate overlapping surfaces, reserving heavy drop shadows exclusively for transient elements like Modals, FABs, and Dialogs.
**Why**: M3 modernizes the UI by flattening the visual hierarchy. Overusing drop shadows creates a cluttered, outdated "Material Design 1" look.
**Check**: Are you using a drop shadow to separate a permanent list item from its background?
**Bad example**: Adding an 8dp drop shadow to every card in a scrolling feed.
**Good example**: Applying Elevation Level 1 (a subtle tonal shift) to the cards, and keeping them shadowless until dragged.

### R4: The M3 Type Scale
**Source**: Material Design 3 (Typography)
**Rule**: Strictly utilize the 5 standard M3 typography categories: `Display` (massive, short text), `Headline` (primary page titles), `Title` (medium emphasis, app bars), `Body` (long-form reading), and `Label` (buttons, tiny metadata).
**Why**: Deviating from the scale breaks typographic rhythm and accessibility scaling.
**Check**: Can every text element on the screen be mapped cleanly to one of the 5 M3 type roles?
**Bad example**: Creating a custom "Subtext_Mini_Bold" style at 11sp.
**Good example**: Using `Label Small` for the timestamp on a message.

### R5: Minimum Touch Targets
**Source**: Material Design 3 (Accessibility)
**Rule**: Every interactive element must have a minimum clickable area of 48x48dp, regardless of the visual size of the component itself.
**Why**: 48dp translates to roughly 9mm, the minimum physical size required for a human finger to reliably tap without error.
**Check**: Is the bounding box of the click listener at least 48x48dp, even if the icon inside is only 24x24dp?
**Bad example**: A 24x24dp close icon with exactly a 24x24dp touch target.
**Good example**: A 24x24dp close icon with 12dp of invisible padding on all sides, resulting in a 48x48dp touch target.

### R6: Button Hierarchy Selection
**Source**: Material Design 3 (Buttons)
**Rule**: Use `Filled Buttons` for the single highest priority action on screen, `Tonal Buttons` for high-priority secondary actions, `Outlined Buttons` for medium priority, and `Text Buttons` for the lowest priority actions.
**Why**: Providing multiple `Filled Buttons` confuses the user about what the primary goal of the screen is.
**Check**: Is there more than one `Filled Button` visible in the immediate viewport?
**Bad example**: A dialog with a Filled "Cancel" button and a Filled "Submit" button.
**Good example**: A dialog with a Text "Cancel" button and a Filled "Submit" button.

### R7: Button Anatomy Constraints
**Source**: Material Design 3 (Buttons)
**Rule**: M3 Buttons must use fully rounded pill-shapes (Corner size: 100%) by default, contain label text using the `Label Large` type token, and optionally include an 18x18dp icon placed before the text.
**Why**: Standardizing button anatomy ensures users instantly recognize interactive elements across the OS.
**Check**: Are the buttons fully rounded, and is the text capitalized normally (M3) rather than ALL CAPS (M2)?
**Bad example**: A rectangular button with ALL CAPS text.
**Good example**: A pill-shaped button with Sentence case text.

### R8: Floating Action Button (FAB) Placement
**Source**: Material Design 3 (FABs)
**Rule**: Use a FAB exclusively for the single most common, primary action in the current screen context. Place it in the bottom-right corner, 16dp from the edges.
**Why**: The FAB is the most visually prominent element on the screen. Using it for a secondary action wastes its visual weight.
**Check**: Does the action the FAB performs represent the main reason the user is on this screen?
**Bad example**: Using a FAB to open the settings menu on a social feed.
**Good example**: Using a FAB to "Compose New Tweet" on a social feed.

### R9: TextField Usage
**Source**: Material Design 3 (TextFields)
**Rule**: Use `Filled TextFields` for forms with multiple inputs to establish a clear visual baseline, and use `Outlined TextFields` in isolated contexts or where a filled background conflicts with the surface.
**Why**: M3 explicitly defines these two styles to solve different spatial problems. Filled is for dense forms; Outlined is for focus.
**Check**: Are you mixing Filled and Outlined text fields in the same form?
**Bad example**: A login form where "Email" is an Outlined field and "Password" is a Filled field.
**Good example**: A login form where both fields are Filled, providing a consistent vertical rhythm.

### R10: Card Elevation Categories
**Source**: Material Design 3 (Cards)
**Rule**: Differentiate cards by choosing one of the three M3 types: `Elevated Card` (uses a drop shadow, best for distinct items), `Filled Card` (uses a surface variant color, best for grouping), or `Outlined Card` (uses a border, best for subtle boundaries).
**Why**: Mixing card styles inappropriately creates visual chaos.
**Check**: Does the card style match its level of interactivity and importance?
**Bad example**: Using an Outlined Card for a highly interactive, draggable item.
**Good example**: Using an Elevated Card for a draggable item, and Filled Cards for static dashboard widgets.

### R11: Bottom Navigation Bar Constraint
**Source**: Material Design 3 (Navigation Bar)
**Rule**: Use a Bottom Navigation Bar only for top-level destinations, containing exactly 3 to 5 items. Never use it for actions (like "Compose" or "Search").
**Why**: The Bottom Nav is for structural movement. Placing actions in the nav bar conflates navigation with execution.
**Check**: Does every icon in the Bottom Nav change the entire view of the application?
**Bad example**: A Bottom Nav with "Home", "Profile", and a "Create Post" action.
**Good example**: A Bottom Nav with "Home", "Search", "Notifications", and "Profile".

### R12: Navigation Rail for Tablets
**Source**: Material Design 3 (Navigation Rail)
**Rule**: Automatically convert Bottom Navigation Bars into a vertical Navigation Rail on the left edge of the screen when the layout expands to Tablet/Foldable sizes (Compact to Medium/Expanded WindowSizeClass).
**Why**: Bottom Navigation stretches uncomfortably on wide screens and is ergonomically difficult to reach on tablets. The left rail maps perfectly to where the user's left thumb rests holding a tablet.
**Check**: If the screen width exceeds 600dp, does the navigation move to the left edge?
**Bad example**: A 10-inch tablet layout where the Bottom Nav stretches across the entire bottom edge.
**Good example**: A tablet layout utilizing a Navigation Rail on the left, leaving the bottom edge clean.

### R13: Navigation Drawer for Deep Hierarchies
**Source**: Material Design 3 (Navigation Drawer)
**Rule**: Use a modal Navigation Drawer (hamburger menu) only when the app has more than 5 top-level destinations, or requires deep hierarchical navigation that cannot fit in a Bottom Nav.
**Why**: Items in a drawer are hidden (out of sight, out of mind). Hiding primary destinations reduces engagement.
**Check**: Could these destinations fit comfortably in a Bottom Nav or Rail?
**Bad example**: Hiding the 3 main features of the app inside a hamburger menu to make the UI look "clean".
**Good example**: Using a Bottom Nav for the 4 main features, and a Navigation Drawer for "Settings, Help, About, and Account Management".

### R14: Edge-to-Edge `WindowInsets`
**Source**: Android Developers (Edge-to-Edge)
**Rule**: The app's root layout must draw entirely under the transparent system status bar (top) and navigation bar (bottom). Use `WindowInsets` to apply internal padding so interactive content is not obscured.
**Why**: Modern Android requires edge-to-edge drawing to prevent ugly black bars at the top and bottom of the screen, providing an immersive experience.
**Check**: Are lists capable of scrolling *behind* the translucent bottom navigation handle?
**Bad example**: Setting the system navigation bar to solid black.
**Good example**: Drawing the UI behind a transparent navigation bar and applying `navigationBarsPadding()` to the bottom-most button.

### R15: Prevent Inset Overlap on Interactive Elements
**Source**: Android Developers (WindowInsets)
**Rule**: Never place interactive elements (like FABs or Bottom Navs) within the system gesture zones (bottom inset or side insets). Always add inset padding.
**Why**: Placing a button in the system navigation zone means the user will accidentally trigger the Android Home or Back gesture when trying to click the button.
**Check**: Is the FAB padded above the bottom WindowInset?
**Bad example**: A FAB sitting flush against the very bottom pixels of the physical screen.
**Good example**: A FAB sitting 16dp above the `systemGestureInsets.bottom`.

### R16: Predictive Back Support
**Source**: Android Developers (Predictive Back)
**Rule**: Ensure custom views, dialogs, and navigation frameworks support the Android Predictive Back gesture, allowing the user to peek at the previous screen before committing to the back action.
**Why**: Android 14+ makes the back gesture predictive. Apps that manually intercept the back button without providing the predictive animation feel broken and trap the user.
**Check**: Does swiping from the left edge slowly reveal the previous screen or home screen?
**Bad example**: Intercepting the hardware back button with `onBackPressed()` to show a custom modal without registering an `OnBackPressedCallback` for predictive back.
**Good example**: Using the standard AndroidX Navigation component which handles predictive back animations automatically.

### R17: Adaptive `WindowSizeClass`
**Source**: Android Developers (Adaptive Layouts)
**Rule**: Design layouts to react to three breakpoints: `Compact` (Phones, <600dp), `Medium` (Foldables/Small Tablets, 600-840dp), and `Expanded` (Large Tablets, >840dp).
**Why**: Hardcoding specific dp widths guarantees the UI will break on the diverse ecosystem of Android devices.
**Check**: Does the layout fundamentally restructure (not just stretch) when moved from Compact to Expanded?
**Bad example**: A single column list that stretches to 1000dp wide on a tablet.
**Good example**: A single column list on Compact that transforms into a two-pane List-Detail view on Expanded.

### R18: Two-Pane Layouts for Expanded Screens
**Source**: Android Developers (Adaptive Layouts)
**Rule**: When transitioning from Compact to Expanded width, convert deep list-to-detail navigation flows into a side-by-side Two-Pane layout (List on left, Detail on right).
**Why**: Forcing users to navigate back and forth on a large screen wastes space and requires excessive interaction cost.
**Check**: Does the app utilize the extra horizontal space to show hierarchy?
**Bad example**: A tablet app where clicking an email opens a new full-screen window, requiring the user to hit "Back" to see the inbox again.
**Good example**: A tablet app where clicking an email in the left pane updates the reading pane on the right.

### R19: Dialog Sizing Limits
**Source**: Material Design 3 (Dialogs)
**Rule**: Dialogs must never stretch to the edges of the screen. They must have a minimum margin of 24dp from the left and right edges, and a maximum width of 560dp.
**Why**: Full-width dialogs look like entirely new screens, confusing the user's mental model of the back stack.
**Check**: Is the dialog constrained to a readable maximum width on tablets?
**Bad example**: A dialog that stretches to 1000dp wide on a tablet, making the text impossible to track.
**Good example**: A dialog centered on the tablet screen capped at 560dp wide.

### R20: Snackbar Placement and Priority
**Source**: Material Design 3 (Snackbars)
**Rule**: Use Snackbars for brief, low-priority feedback about an operation. They must appear at the bottom of the screen, sit *above* bottom navigation bars, and disappear automatically (usually 4-10 seconds).
**Why**: Snackbars are transient and should not permanently obscure UI. Placing them under a FAB or Navigation Bar makes them illegible.
**Check**: If a Snackbar appears, does it push the FAB up or sit cleanly above the Bottom Nav?
**Bad example**: A Snackbar that appears at the very bottom edge of the screen, underneath the Bottom Navigation bar.
**Good example**: A Snackbar that appears immediately above the Bottom Navigation bar, temporarily shifting the FAB upward.

### R21: Standard Corner Radii (Shape Family)
**Source**: Material Design 3 (Shape)
**Rule**: Use the standard M3 shape scales: `None` (0dp), `Extra Small` (4dp), `Small` (8dp), `Medium` (12dp), `Large` (16dp), `Extra Large` (28dp), and `Full` (Pill). Do not use arbitrary radii like 7dp or 11dp.
**Why**: Consistent corner radii create a unified subconscious structural language across the app.
**Check**: Do the corner radii map to the official M3 shape scale?
**Bad example**: A card with an arbitrary 13dp border radius.
**Good example**: A card using the standard `Medium` (12dp) shape token.

### R22: Switch vs Checkbox Usage
**Source**: Material Design 3 (Selection Controls)
**Rule**: Use a `Switch` to instantly activate/deactivate a standalone setting. Use a `Checkbox` when selecting multiple options from a list or when the selection requires a "Save" button to take effect.
**Why**: A switch implies an immediate system state change (like a light switch). A checkbox implies batching data for future submission.
**Check**: If the user toggles this control, does the setting apply immediately without a save button?
**Bad example**: Using checkboxes in a settings menu where toggling them immediately changes the app behavior.
**Good example**: Using switches in the settings menu, and checkboxes in a multi-select deletion list.

### R23: Divider Weight and Contrast
**Source**: Material Design 3 (Dividers)
**Rule**: Dividers must be 1dp thick and use the `Outline Variant` token color to ensure they are subtle and do not command visual hierarchy.
**Why**: Heavy or highly contrasted dividers create visual noise and segment the screen too aggressively.
**Check**: Are dividers barely visible, providing just enough structure to separate content?
**Bad example**: A 2dp thick, solid black divider between list items.
**Good example**: A 1dp thick divider using `MaterialTheme.colorScheme.outlineVariant`.

### R24: Scrim Color for Modals
**Source**: Material Design 3 (Scrim)
**Rule**: When a modal (Dialog, Bottom Sheet, or Navigation Drawer) is open, the background content must be obscured by a Scrim. The M3 standard scrim is the `Scrim` token color (usually pure black) set to 32% opacity.
**Why**: The scrim focuses the user's attention on the modal and disables interaction with the background surface.
**Check**: Does the background dim to exactly 32% opacity when the modal opens?
**Bad example**: Opening a dialog over a fully bright background, making it hard to read.
**Good example**: The background instantly dims using `colorScheme.scrim` at 32% alpha.

## Anti-patterns

### 1. The iOS Port
Designing an Android app by simply copying an iOS interface. This manifests as right-aligned carets in list items, bottom-up modal sheets for standard navigation, back text-labels in the app bar, and ignoring the Android hardware/gesture back button. It alienates Android users.

### 2. The Shadow Overload
Relying entirely on heavy M2-style drop shadows for every component on the screen instead of embracing M3's Tonal Elevation. This creates a cluttered, noisy interface that looks dated.

### 3. The Tiny Target
Ignoring the 48x48dp minimum touch target rule for icons and text links, leading to an inaccessible application that frustrates users with imprecise motor control or those using the app one-handed.

### 4. The Letterboxed Layout
Failing to implement `WindowInsets` correctly, resulting in solid black or colored bars at the top (status bar) and bottom (navigation bar) of the screen, destroying the modern edge-to-edge aesthetic.

## Quick Reference
- [ ] Are all colors mapped to M3 semantic tokens (Primary, Surface, Error) instead of hex codes?
- [ ] Is Tonal Elevation used instead of drop shadows for standard overlapping surfaces?
- [ ] Are all text elements mapped to the 5 M3 type scales (Display, Headline, Title, Body, Label)?
- [ ] Is every interactive element at least 48x48dp in touch area?
- [ ] Is the primary action a `Filled Button` and secondary actions `Tonal/Outlined/Text`?
- [ ] Does the Bottom Nav contain 3-5 items and strictly perform navigation (no actions)?
- [ ] Does the Bottom Nav convert to a Navigation Rail on tablet layouts (>600dp)?
- [ ] Is the app drawn edge-to-edge, padded correctly using `WindowInsets`?
- [ ] Are list-detail flows converted into Two-Pane layouts on expanded screens?
- [ ] Are dialogs constrained to a maximum width of 560dp?
- [ ] Are checkboxes used for deferred batch actions and switches used for instant state changes?
