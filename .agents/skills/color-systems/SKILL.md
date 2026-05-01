---
name: color-systems
description: Governs all decisions regarding UI color palettes, dark mode implementations, semantic colors (error/success), interactive states (hover/active), elevation, and contrast. Use this skill whenever the user asks about color choices, brand palettes, dark mode, component states, shadows, or when evaluating an interface that feels visually overwhelming, muddy, or inaccessible due to color.
---

# Color Systems

Color in UI design is functional first, aesthetic second. Based on Josef Albers' theory of color relativity and modern platform guidelines (Material Design, Apple HIG), color must guide attention, communicate state, and establish hierarchy. Color is never perceived in isolation; it is entirely dependent on its surrounding context.

## Core Principles
1. **Color is relative.** A color's appearance is defined by its background (simultaneous contrast). You cannot evaluate a hex code in isolation.
2. **Color communicates meaning.** Semantic colors (red, green, yellow) carry hardcoded human associations. Never override them for the sake of branding.
3. **Hierarchy through distribution.** The 60-30-10 rule dictates that color must be distributed unevenly to establish a clear visual hierarchy.
4. **Dark mode is not an inversion.** Dark mode requires a complete re-evaluation of saturation, contrast, and elevation mechanics, not just a mathematical color flip.
5. **Never rely on color alone.** Color blindness affects 8% of men. Always pair color changes with text, icons, or structural changes to communicate state.

## Rules

### R1: The 60-30-10 Rule for Distribution
**Source**: Universal UI Law / Platform Guidelines
**Rule**: Distribute UI colors according to the 60-30-10 ratio: 60% dominant background color, 30% secondary surface color (cards, sidebars), and 10% accent color (primary actions, highlights).
**Why**: Equal distribution of colors creates visual chaos. A strict ratio ensures the interface feels unified while allowing the 10% accent color to clearly draw the eye to primary actions.
**Check**: Is the accent color restricted exclusively to the most important interactive elements?
**Bad example**: A screen where the background, the cards, and the buttons all use the primary brand blue in roughly equal proportions.
**Good example**: A screen with a 60% off-white background, 30% light gray cards, and 10% vivid blue primary buttons.

### R2: Reserve Semantic Colors for System States
**Source**: Apple HIG / Material Design 3
**Rule**: Strictly reserve Red for destructive actions/errors, Green for success/completion, and Yellow/Orange for warnings.
**Why**: These are deeply ingrained psychological associations. Using them for non-semantic branding causes severe cognitive dissonance.
**Check**: Are there any red, green, or yellow elements on the screen that do NOT communicate error, success, or warning?
**Bad example**: Using a bright red button for "Save Draft" simply because red is the company's brand color.
**Good example**: Using the brand's primary blue for "Save Draft" and reserving red exclusively for "Delete Account."

### R3: Prevent The Vibrating Edge
**Source**: Interaction of Color (Albers)
**Rule**: Never place highly saturated, complementary colors directly adjacent to one another (e.g., pure red on pure blue, or bright green on magenta).
**Why**: The human eye cannot focus on opposite wavelengths simultaneously. When highly saturated complements touch, the edge between them appears to vibrate or blur, causing immediate eye strain.
**Check**: Do the adjacent colors hurt your eyes to look at closely?
**Bad example**: Bright cyan text on a pure red button.
**Good example**: White text on a pure red button, or navy blue text on a pale cyan button.

### R4: Desaturate Colors for Dark Mode
**Source**: Material Design 3 / Apple HIG
**Rule**: Light mode brand colors must be desaturated (lowered chroma) and often lightened before being used in dark mode. Never use fully saturated light-mode colors against dark backgrounds.
**Why**: Fully saturated colors on a black background create visual vibration and bleed, reducing legibility and causing eye fatigue. Desaturated "pastel" versions maintain readability without losing the hue.
**Check**: If you switch from light to dark mode, does the accent color become less saturated?
**Bad example**: Using the exact same neon blue (`#0000FF`) for both light mode and dark mode buttons.
**Good example**: Using `#0000FF` in light mode, but softening it to `#6666FF` in dark mode.

### R5: Use Surface Lightness for Elevation in Dark Mode
**Source**: Material Design 3
**Rule**: In dark mode, convey depth (elevation) by making the surface color progressively lighter, not by adding drop shadows.
**Why**: Drop shadows rely on darkness to create the illusion of depth. On a dark background, shadows are invisible. Lighter surface colors simulate an object moving closer to a light source.
**Check**: Do elevated cards in dark mode have a lighter background color than the canvas beneath them?
**Bad example**: A dark gray card on a black background with a heavy black drop shadow.
**Good example**: A black background (`#121212`) with an elevated card using a slightly lighter gray (`#1E1E1E`).

### R6: Calculate States via HSL Lightness
**Source**: General UI Engineering
**Rule**: Generate hover and active component states by systematically adjusting the Lightness (L in HSL) or Opacity of the base color, rather than arbitrarily picking new hex codes.
**Why**: Mathematical adjustments (e.g., darken by 10% for hover, 20% for active) create a predictable, cohesive design system that scales across all colors.
**Check**: Is the hover state a direct mathematical derivative of the default state?
**Bad example**: A primary blue button (`#007BFF`) that turns a completely different hue of teal (`#20B2AA`) on hover.
**Good example**: A primary blue button (`hsl(211, 100%, 50%)`) that darkens to `hsl(211, 100%, 40%)` on hover.

### R7: Avoid Pure Black and Pure White
**Source**: Apple HIG / Material Design 3
**Rule**: Use off-blacks (e.g., `#1A1A1A`) for dark text/backgrounds and off-whites (e.g., `#FAFAFA`) for light backgrounds, rather than absolute `#000000` or `#FFFFFF`.
**Why**: Absolute contrast (pure black on pure white) can cause astigmatic halation (blurring) and eye strain on modern, high-brightness OLED screens.
**Check**: Is the primary background color `#FFFFFF` or `#000000`?
**Bad example**: Body text set to `#000000` on a `#FFFFFF` background.
**Good example**: Body text set to `#1C1C1C` on a `#F9F9F9` background.

### R8: Design in Grayscale First
**Source**: Interaction of Color (Albers)
**Rule**: Establish visual hierarchy using only shades of gray before applying any color.
**Why**: Color is deceptive. If a layout relies entirely on color to establish hierarchy, that hierarchy will fail for color-blind users or in poor lighting. If it works in grayscale, color will only enhance it.
**Check**: If you convert the screen to grayscale, can you still instantly identify the primary action and the visual hierarchy?
**Bad example**: A dashboard where the only difference between "Status: Online" and "Status: Offline" is the color of the text.
**Good example**: A dashboard where "Status: Online" is bold and accompanied by an icon, establishing hierarchy regardless of color.

### R9: Limit the Palette to One or Two Accents
**Source**: Material Design 3
**Rule**: Restrict the UI to one primary accent color and, if absolutely necessary, one secondary accent color.
**Why**: Every additional color introduced into a UI dilutes the impact of the primary accent. The more colors you use, the harder it is to direct the user's attention.
**Check**: Are there more than two distinct accent colors competing for attention on the screen?
**Bad example**: A settings page where every category icon uses a completely different, highly saturated brand color.
**Good example**: A settings page where all icons are neutral gray, and only the "Save Changes" button uses the primary blue accent.

### R10: Account for Simultaneous Contrast
**Source**: Interaction of Color (Albers)
**Rule**: Always evaluate text or icon color against the specific background it will sit on, never against a neutral canvas in a vacuum.
**Why**: A color's perceived lightness and hue shift drastically depending on its neighbor. A gray that looks perfectly legible on white may become invisible when placed on a blue background.
**Check**: Has this specific text color been tested for WCAG contrast compliance against this specific background?
**Bad example**: Designing a gray icon on a white canvas, then placing it on a dark blue header where it disappears.
**Good example**: Adjusting the icon to pure white specifically for the dark blue header to maintain contrast.

### R11: Tint Neutrals with the Primary Brand Color
**Source**: Material Design 3
**Rule**: Instead of using mathematically pure grays (which can look dead or dirty), tint your neutral grays slightly with the primary brand hue.
**Why**: Tinted neutrals create subtle, subconscious harmony across the entire interface, making the UI feel warmer and more cohesive.
**Check**: Do the gray backgrounds or text colors share a hint of the dominant brand hue?
**Bad example**: A brand using vibrant purple, but setting all backgrounds to a sterile, pure gray (`#EEEEEE`).
**Good example**: A brand using vibrant purple, setting its backgrounds to a cool, slightly purple-tinted gray (`#EBEAF0`).

### R12: Ensure Text Contrast on Colored Buttons
**Source**: WCAG Accessibility Guidelines
**Rule**: Text placed on colored accent buttons must meet a minimum contrast ratio of 4.5:1 (AA standard).
**Why**: Buttons are the most critical interaction points. If the text is unreadable due to low contrast with the button color, the user cannot confidently take action.
**Check**: If you squint at the button, can you still read the label?
**Bad example**: White text on a bright yellow button (Ratio: 1.1:1).
**Good example**: Dark charcoal text on a bright yellow button (Ratio: 12.4:1).

### R13: Never Rely Solely on Color to Convey Information
**Source**: Apple HIG
**Rule**: Whenever color is used to indicate state or meaning, it must be accompanied by a secondary indicator (text, shape, icon, or pattern).
**Why**: To ensure accessibility for visually impaired and color-blind users who cannot perceive the color shift.
**Check**: If the screen were printed in black and white, would the user still understand the system state?
**Bad example**: An input field outline turning red to indicate a validation error, with no error text.
**Good example**: An input field outline turning red, accompanied by a warning icon and the text "This field is required."

### R14: Provide Sufficient Contrast Between UI Components
**Source**: WCAG Accessibility Guidelines
**Rule**: The boundaries of interactive components (like input fields or buttons) must have a contrast ratio of at least 3:1 against the background canvas.
**Why**: Users need to know where interactive hit areas begin and end. If a text input blends into the background, the user won't know where to click.
**Check**: Is the edge of the input field clearly visible against the page background?
**Bad example**: A white input field placed on an off-white `#F9F9F9` background with no border.
**Good example**: A white input field placed on an off-white `#F9F9F9` background with a `#CCCCCC` border.

### R15: Maintain Semantic Consistency Across the App
**Source**: General UI Law
**Rule**: Once a color is assigned a specific functional meaning (e.g., Blue = Clickable Link), it must not be used for non-functional decoration elsewhere in the app.
**Why**: Users build a mental model of your app's interaction language within seconds. Breaking that model causes frustration and mis-clicks.
**Check**: Is the primary link color being used anywhere just to "look nice"?
**Bad example**: Using blue for text links, but also using the exact same blue for static, unclickable section headings.
**Good example**: Using blue exclusively for interactive elements, and using dark gray or black for all static headings.

## Anti-patterns

### The Rainbow UI
An interface that fails to utilize the 60-30-10 rule, instead employing 3 or more highly saturated accent colors in equal proportions. The resulting design lacks hierarchy, confuses the user, and looks distinctly unprofessional.

### The Vibrating Edge
Placing highly saturated complementary colors directly next to or on top of one another (e.g., red text on a green background). This violates human optical limits, causing the edges to blur, vibrate, and induce headaches.

### The Inverted Dark Mode
A lazy dark mode implementation where the background is simply turned black, but the light-mode brand colors (highly saturated blues/purples/reds) are kept exactly the same. This creates aggressive contrast and eye strain.

### The Meaningless Red
Using red or green for primary branding or standard interactive elements in a way that conflicts with their deeply ingrained semantic meanings (Red = Stop/Danger, Green = Go/Success).

### The Contrast Illusion
Failing to recognize Albers' law of simultaneous contrast by assuming a hex code that passes accessibility checks on a white background will also be legible on a colored background.

### The Phantom Component
Designing interactive elements (like input fields or secondary buttons) with such low contrast against the background canvas that their boundaries disappear, leaving the user guessing where to click.

## Quick Reference
- [ ] Is color distributed using the 60-30-10 rule?
- [ ] Are semantic colors (red/green/yellow) reserved exclusively for states?
- [ ] Are highly saturated complementary colors prevented from touching?
- [ ] Are dark mode colors desaturated to prevent eye strain?
- [ ] Is elevation in dark mode handled by lighter surfaces rather than shadows?
- [ ] Are hover/active states calculated systematically (via lightness/opacity)?
- [ ] Is pure black (`#000000`) and pure white (`#FFFFFF`) avoided?
- [ ] Does the UI function perfectly if converted to grayscale?
- [ ] Does every color-coded state have a secondary visual indicator (icon/text)?
- [ ] Do button text and component borders meet WCAG contrast minimums?
