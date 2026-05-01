---
name: ios-platform-fluency
description: Governs all platform-specific design decisions for modern iOS/iPadOS applications. Use this skill whenever the user asks about Apple Human Interface Guidelines (HIG), SF Symbols, iOS navigation (Tab Bars vs Navigation Bars), modal sheets, iOS touch targets (44x44pt), iOS corner radii (continuous curves/squircles), haptics, safe areas, or making an interface feel "native" to Apple platforms.
---

# iOS Platform Fluency

Designing for iOS requires strict adherence to the Apple Human Interface Guidelines (HIG). iOS users expect a highly specific visual and interactive language defined by clarity, deference, and depth. This skill ensures that generated interfaces utilize native iOS metaphors—such as blur/vibrancy for depth, continuous curve corner radii, SF Symbols, and standard navigation controllers—preventing designs from looking like ported Android apps or generic web views.

## Core Principles

1. **Clarity over Decoration.** Text must be legible at every size, icons must be precise, and negative space is preferred over heavy borders and dividers to structure content.
2. **Deference to Content.** The UI should fade into the background. Content is the primary interface.
3. **Depth via Translucency.** iOS establishes hierarchy not through heavy drop shadows (like Material), but through translucent materials, blur, and vibrancy.
4. **Fluidity and Gestures.** iOS relies heavily on physical gestures (swipe to go back, pull to dismiss modals). The design must account for gesture zones and natural physics.
5. **Dynamic Type.** All text must scale automatically based on the user's system-level accessibility settings.

## Rules

### R1: The 44pt Minimum Touch Target
**Source**: Apple HIG (Accessibility)
**Rule**: Every interactive element must have a minimum clickable area of 44x44pt (points), regardless of the visual size of the icon or text.
**Why**: This is the minimum physical size (approx. 7-9mm) required for an average finger to tap a screen without making errors.
**Check**: Is the invisible hit box for this icon exactly 44x44pt, even if the visual icon is 24x24pt?
**Bad example**: A 20x20pt "X" button placed flush in the corner with no extra padding.
**Good example**: A 20x20pt "X" button with 12pt of invisible padding on all sides to create a 44x44pt target.

### R2: Safe Area Insets
**Source**: Apple HIG (Layout)
**Rule**: Never place interactive elements or critical content outside the Safe Area (the space unaffected by the Dynamic Island, camera notch, rounded screen corners, and the home indicator).
**Why**: Content outside the Safe Area will be physically obscured by hardware elements or will conflict with system-level gestures (like swiping the home bar).
**Check**: Does the content gracefully pad itself away from the bottom home indicator and top notch/Dynamic Island?
**Bad example**: A "Checkout" button resting flush against the very bottom pixels of an iPhone screen.
**Good example**: A "Checkout" button anchored securely above the bottom Safe Area inset.

### R3: iOS Corner Radii (Continuous Curves)
**Source**: Apple HIG (Visual Design)
**Rule**: Use "continuous curves" (squircles) for corner radii rather than standard CSS/Android rounded rectangles. 
**Why**: Apple hardware and software strictly use continuous curves to eliminate the abrupt transition from a straight line to a curve, creating a smoother, softer aesthetic native to iOS.
**Check**: Does the corner rounding style match the shape of the physical iOS device screen and native app icons?
**Bad example**: Using a standard geometric `border-radius: 12px`.
**Good example**: Using the `curve: continuous` property (or `corner-curve: continuous`) to match native iOS squircles.

### R4: Tab Bar Navigation
**Source**: Apple HIG (Navigation)
**Rule**: Use a bottom Tab Bar exclusively for top-level app navigation (3 to 5 items). Never hide primary navigation in a hamburger menu on iOS.
**Why**: Tab Bars provide instant access and visibility to core features, which is the expected iOS mental model.
**Check**: Are all core destinations visible at the bottom of the screen at all times?
**Bad example**: Using a "hamburger" drawer menu to house the main sections of the app.
**Good example**: A bottom Tab Bar with 4 clearly labeled destinations.

### R5: Tab Bar State Exclusivity
**Source**: Apple HIG (Navigation)
**Rule**: Tab Bars must only be used for navigation, never for executing actions (like "Create" or "Search").
**Why**: Conflating navigation with action creates unpredictable mental models.
**Check**: Does clicking an icon in the Tab Bar strictly switch the view, rather than triggering an action?
**Bad example**: A Tab Bar containing a "Camera" button that instantly opens the camera instead of switching to a camera view.
**Good example**: Placing the "Camera" action in the top Navigation Bar or as a distinct primary button within the view.

### R6: Navigation Bar Hierarchy
**Source**: Apple HIG (Navigation)
**Rule**: The top Navigation Bar must display the current view's title and provide a "Back" button on the left (if pushed onto the stack). Primary screen actions (like "Edit" or "Add") sit on the right side of the Navigation Bar.
**Why**: This is the universal standard for iOS hierarchical navigation.
**Check**: Does the top bar clearly state where the user is and provide a way back on the left?
**Bad example**: Centering actions in the Navigation Bar or placing the Back button on the right.
**Good example**: A Navigation Bar with "< Inbox" on the left, "Settings" in the center, and "Edit" on the right.

### R7: The Large Title Pattern
**Source**: Apple HIG (Navigation Bars)
**Rule**: Use Large Titles for top-level screens (like an Inbox or Library). The title must collapse into a standard, smaller centered inline title when the user scrolls down.
**Why**: Large Titles clearly orient the user upon entry, but collapse to save vertical space once the user begins consuming content.
**Check**: Does the title aggressively scale down into the nav bar upon scroll?
**Bad example**: A permanent, massive header that takes up 20% of the screen even when reading content.
**Good example**: A large "Inbox" header that slides up and shrinks into the top bar as the user scrolls the emails.

### R8: Modal Sheets for Contextual Tasks
**Source**: Apple HIG (Modals)
**Rule**: Use the iOS standard "Sheet" presentation (where the underlying view visually shrinks and recedes slightly into the background) for temporary, distinct tasks that don't belong in the main navigation flow.
**Why**: The receding background retains context, letting the user know exactly where they will return when the modal is dismissed.
**Check**: Does opening the modal push the previous screen slightly back in z-space rather than completely replacing it?
**Bad example**: Navigating to a full-screen, flat page just to select a filter option.
**Good example**: Sliding up a Modal Sheet for filters, with the main list visibly blurred in the background.

### R9: Swipe-to-Dismiss Gestures
**Source**: Apple HIG (Gestures)
**Rule**: Any view presented as a Modal Sheet must be dismissible by a downward swipe gesture. Any view pushed onto a navigation stack must be dismissible by an edge-swipe from the left.
**Why**: iOS users instinctively rely on these gestures before looking for a physical "Close" or "Back" button.
**Check**: Can the user escape the screen intuitively without tapping a specific button?
**Bad example**: A modal that can only be closed by hitting a 20x20pt "X" in the top corner.
**Good example**: A modal that includes a top handle and responds to a downward drag to close.

### R10: SF Symbols over Custom Iconography
**Source**: Apple HIG (Typography & Icons)
**Rule**: Use Apple's SF Symbols for standard UI icons instead of custom SVGs or third-party icon packs.
**Why**: SF Symbols are explicitly designed to align optically with the San Francisco system font and automatically scale with iOS Dynamic Type.
**Check**: Do the icons match the weight and scale of the adjacent text perfectly?
**Bad example**: Importing a heavy, custom SVG icon that looks misaligned and bulky next to standard SF Pro text.
**Good example**: Using `gearshape.fill` from SF Symbols that scales perfectly with the user's font size settings.

### R11: San Francisco System Font
**Source**: Apple HIG (Typography)
**Rule**: Use the system font (SF Pro for text, SF Compact for Apple Watch, SF Arabic, etc.) for UI text unless the app has a strictly distinct brand identity that requires a custom font.
**Why**: San Francisco optimizes for legibility at all scales and automatically tracks (adjusts letter spacing) based on point size.
**Check**: Does the app feel native and highly legible at small sizes?
**Bad example**: Forcing a decorative Google Font for tiny metadata text.
**Good example**: Using `SF Pro Text` for body copy and metadata.

### R12: Depth via Translucency and Materials
**Source**: Apple HIG (Visual Design)
**Rule**: Establish visual hierarchy by using standard iOS background materials (Thick, Regular, Thin, Ultrathin) which apply real-time blur and vibrancy to content passing beneath them, rather than relying heavily on solid colors and drop shadows.
**Why**: Translucency provides a sense of depth and context that feels uniquely native to iOS.
**Check**: When content scrolls under the top Navigation Bar, does it blur beautifully behind the bar?
**Bad example**: A solid white Navigation Bar with a heavy, harsh black drop shadow.
**Good example**: A translucent Navigation Bar using `UIBlurEffectStyle.systemMaterial`.

### R13: Segmented Controls for Sub-views
**Source**: Apple HIG (Controls)
**Rule**: Use Segmented Controls (a horizontal pill-shaped toggle) to switch between closely related data views within the same context (e.g., Map vs. List).
**Why**: This is the native iOS paradigm for sub-navigation, whereas Android uses sliding Tabs.
**Check**: Does the control allow rapid, flat switching of content within a single screen?
**Bad example**: Using an Android-style Material Tab bar with an underline indicator in an iOS app.
**Good example**: Using the standard, pill-shaped iOS Segmented Control.

### R14: Context Menus over Long Press
**Source**: Apple HIG (Menus)
**Rule**: Provide an iOS Context Menu (which reveals a list of actions and optionally a preview) when the user long-presses an item, rather than triggering a custom popover or an Android-style multi-select mode.
**Why**: Context Menus are the system standard for revealing hidden item-level actions without leaving the current view.
**Check**: Does long-pressing reveal a native, blurred contextual menu?
**Bad example**: Long-pressing an item changes the entire screen's Navigation Bar to "Edit Mode" (Android pattern).
**Good example**: Long-pressing an item gently blurs the background and pops up a list of actions (Share, Delete, Favorite).

### R15: Avoid iOS System Color Hardcoding
**Source**: Apple HIG (Color)
**Rule**: Use iOS Semantic Colors (e.g., `systemBlue`, `label`, `secondarySystemBackground`) instead of hardcoding absolute hex/RGB values.
**Why**: Semantic colors automatically adapt to Light/Dark Mode and accessibility high-contrast settings seamlessly.
**Check**: Are colors defined by their role in the UI?
**Bad example**: Setting the background color to `#FFFFFF`.
**Good example**: Setting the background color to `systemBackground`.

## Anti-patterns

### 1. The Android Port
Designing an iOS app with Material Design components. This includes using Floating Action Buttons (FABs), Bottom Sheets (instead of iOS Modal Sheets), sliding Material Tabs with underlines, or placing the Back button in the wrong location. It violates iOS user expectations.

### 2. The Hamburger Hideaway
Placing the core navigation of an iOS app into a hidden side-drawer (hamburger menu) instead of using the standard, visible bottom Tab Bar.

### 3. The Custom Shadow Abuse
Attempting to create depth using heavy, highly-contrasted drop shadows instead of utilizing Apple's native blur, vibrancy, and material translucency APIs.

### 4. Ignoring Dynamic Type
Hardcoding font sizes so they do not respond to the user's system-level text size preferences. This instantly breaks accessibility for users who require larger text.

## Quick Reference
- [ ] Are all touch targets strictly 44x44pt or larger?
- [ ] Are the top notch and bottom home indicator respected via Safe Area insets?
- [ ] Are corners using "continuous curves" (squircles) rather than sharp circular radii?
- [ ] Is primary navigation handled via a bottom Tab Bar (3-5 items)?
- [ ] Is hierarchical navigation handled via a top Navigation Bar with a left-aligned Back button?
- [ ] Do large titles collapse into the Navigation Bar on scroll?
- [ ] Are temporary tasks presented using standard receding Modal Sheets?
- [ ] Can modals be dismissed with a downward swipe?
- [ ] Are SF Symbols used for standard iconography?
- [ ] Is depth created using blur/translucency rather than heavy drop shadows?
- [ ] Are system semantic colors used to automatically support Dark Mode?
