---
name: visual-hierarchy
description: Governs all visual hierarchy decisions in UI — size, weight, colour contrast, emphasis, scan paths (F-pattern/Z-pattern), proximity, and grouping. Use this skill whenever the user asks about visual weight, hierarchy, emphasizing elements, de-emphasizing text, whitespace, Gestalt principles like figure/ground, or arranging elements by importance. Also trigger for any UI review task — evaluating what draws the eye is mandatory.
---

# Visual Hierarchy

Visual hierarchy is the deliberate arrangement of elements to guide the user's attention in order of importance. It relies on the interplay of size, font weight, color, and whitespace to create a clear scan path, ensuring that primary actions stand out, secondary information supports, and tertiary details recede.

## Core Principles
1. **Size isn't everything.** Relying solely on size for hierarchy makes interfaces look clumsy; use font weight and color contrast to create distinct levels of importance.
2. **De-emphasize rather than emphasize.** Making secondary content softer (lighter color, lower weight) is often more effective than making primary content louder.
3. **Weight defines importance.** Heavier elements naturally draw the eye; use bolder font weights and saturated colors to signal primary actions.
4. **Proximity implies relationship.** Elements placed close together are perceived as a group; use whitespace to separate distinct conceptual blocks.
5. **Establish clear scan paths.** Users don't read; they scan in F-patterns or Z-patterns. Place the most critical information along these natural eye paths.
6. **Figure vs. Ground.** The relationship between an element (figure) and its background (ground) must be distinct; ambiguous relationships cause cognitive strain.
7. **Similarity groups elements.** Elements sharing visual characteristics (color, shape, size) are perceived as related.

## Rules

### R1: Do Not Rely Solely on Font Size
**Source**: Refactoring UI
**Rule**: Combine font weight and color contrast alongside size to establish hierarchy.
**Why**: Relying purely on size requires massive scale jumps to show distinction, which wastes space and creates clumsy proportions.
**Check**: Is the primary element distinct from the secondary element even if they were the same size?
**Bad example**: A page title is 48px, a section title is 36px, and body text is 24px, all using the same regular font weight and black color.
**Good example**: A page title is 32px (bold, black), section title is 20px (semibold, dark gray), and body text is 16px (regular, medium gray).

### R2: De-emphasize Secondary Content
**Source**: Refactoring UI
**Rule**: Soften secondary information by reducing its contrast or weight instead of making the primary information larger or bolder.
**Why**: When everything shouts, nothing is heard. Lowering the volume on less important elements makes the important ones stand out naturally.
**Check**: Did you try making the supporting text lighter before you tried making the main text bolder?
**Bad example**: Making a heading bold and uppercase just because the body text below it feels too prominent.
**Good example**: Changing the color of a timestamp to a lighter gray so the primary comment text stands out more.

### R3: Use Font Weight to Signal Primacy
**Source**: Refactoring UI
**Rule**: Apply heavier font weights (600, 700) to data or labels you want the user to process first.
**Why**: Bold text has more visual weight and creates stronger contrast against the background, drawing the eye immediately.
**Check**: Does the most critical piece of text in this component have the heaviest font weight?
**Bad example**: A dashboard stat card where the label "Total Revenue" and the number "$10,000" both use regular font weight.
**Good example**: A dashboard stat card where the number "$10,000" is bold and the label "Total Revenue" is regular and gray.

### R4: Use Color to De-emphasize
**Source**: Refactoring UI
**Rule**: Apply lighter colors (e.g., grays on a light background) to reduce the visual weight of tertiary information.
**Why**: Lower contrast reduces the element's "pull" on the user's attention, pushing it back in the visual hierarchy.
**Check**: Are less critical elements (like timestamps or footnotes) lower in contrast than the main content?
**Bad example**: Body text and less important metadata are both rendered in `#111111` black.
**Good example**: Body text is `#111111`, while the author name and date are `#666666`.

### R5: Separate Visual Hierarchy from Document Hierarchy
**Source**: Refactoring UI
**Rule**: Style text based on its visual importance in the UI, not its semantic HTML tag (h1, h2, h3).
**Why**: A secondary label might semantically be an `h2`, but visually it might need to look smaller than body text to avoid overwhelming the layout.
**Check**: Are you styling this element based on how important it looks rather than its underlying HTML tag?
**Bad example**: Forcing an `h3` category label to be 24px bold because "headings should be big", making it overpower the 18px article title below it.
**Good example**: Styling the `h3` category label as 12px uppercase and gray, allowing the article title to dominate.

### R6: Group Related Elements with Proximity
**Source**: Universal Principles of Design (Gestalt)
**Rule**: Place related elements closer together than unrelated elements.
**Why**: Proximity is the strongest visual cue for relationship; the brain assumes things that are close together belong together.
**Check**: Is the space between related items demonstrably smaller than the space between unrelated groups?
**Bad example**: A form label is equidistant from the input field it belongs to and the input field above it.
**Good example**: A form label is 4px above its input field, and there is 24px of space before the next label.

### R7: Use Whitespace to Isolate and Elevate
**Source**: Refactoring UI
**Rule**: Surround high-priority elements with generous whitespace to increase their perceived importance.
**Why**: Empty space removes visual competition, allowing the isolated element to become the focal point.
**Check**: Does the primary action or message have the most "breathing room" in the layout?
**Bad example**: A primary "Buy Now" button is crammed immediately next to a dense paragraph of terms and conditions.
**Good example**: A pricing tier has ample padding around its price and a distinct "Sign Up" button separated by generous whitespace.

### R8: Engineer the F-Pattern Scan Path
**Source**: Universal Principles of Design
**Rule**: Place critical information along the top and left edges for text-heavy interfaces.
**Why**: Users from left-to-right reading cultures naturally scan across the top, then down the left side, looking for keywords.
**Check**: Are the most important headers and keywords anchored to the left edge of the content area?
**Bad example**: Important status indicators are buried in the middle of a dense paragraph on a dashboard.
**Good example**: A list of articles aligns titles to the left edge, with the most important metadata at the start of the line.

### R9: Engineer the Z-Pattern Scan Path
**Source**: Universal Principles of Design
**Rule**: Place the primary call-to-action at the bottom right of a low-density, image-heavy layout.
**Why**: The eye sweeps horizontally across the top, diagonally down to the bottom left, and horizontally across the bottom right.
**Check**: Does the scan path terminate at the primary action button?
**Bad example**: A hero section places the primary call-to-action button in the bottom left, contrary to the natural exit point.
**Good example**: A landing page hero has the logo top-left, navigation top-right, headline center, and "Get Started" button bottom-right.

### R10: Establish Clear Figure-Ground Contrast
**Source**: Universal Principles of Design
**Rule**: Ensure the primary element (figure) is clearly differentiated from its background (ground).
**Why**: Ambiguous figure-ground relationships cause perceptual confusion and slow down cognitive processing.
**Check**: Can you instantly tell what is the foreground object and what is the background?
**Bad example**: Placing white text over a complex, light-colored photograph without an overlay.
**Good example**: Using a semi-transparent dark gradient overlay on an image to ensure white text remains readable and distinct.

### R11: Utilize the Principle of Closure
**Source**: Universal Principles of Design
**Rule**: Provide just enough visual information for the user to perceive a complete pattern, reducing visual clutter.
**Why**: The human brain prefers to perceive complete shapes; leveraging this allows you to remove unnecessary borders or lines.
**Check**: Could you remove a border or line and still have the user understand the grouping?
**Bad example**: Drawing a heavy box around every single item in a list to separate them.
**Good example**: Using only whitespace and alignment to separate list items, relying on the user's mind to close the implicit rows.

### R12: Leverage Similarity for Categorization
**Source**: Universal Principles of Design
**Rule**: Apply consistent visual treatments (color, shape, size) to elements that share the same function or category.
**Why**: Elements that look similar are perceived to be part of the same group or have the same behavior.
**Check**: Do all elements that perform the same type of action look visually identical?
**Bad example**: Primary action buttons on different pages have different colors and border radii.
**Good example**: All destructive actions (Delete, Remove) across the app use the same red text style.

### R13: Apply Continuation for Flow
**Source**: Universal Principles of Design
**Rule**: Align elements linearly to lead the eye smoothly from one point of interest to another.
**Why**: Elements arranged in a line or curve are perceived as more related than elements not on the line or curve.
**Check**: Do the elements form a continuous visual path that guides the user to the next logical step?
**Bad example**: A multi-step form where the "Next" button jumps between the left and right sides of the screen depending on the step.
**Good example**: A timeline UI where a continuous vertical line connects chronological events, guiding the eye downward.

### R14: Emphasize Data over Labels
**Source**: Refactoring UI
**Rule**: In key-value pairs, emphasize the data and de-emphasize the label.
**Why**: Users usually know what the data represents from context; the data itself is the unique, useful information.
**Check**: Does the data stand out more than the label describing it?
**Bad example**: A profile card where "Email Address:" is bold and black, and `user@example.com` is regular and gray.
**Good example**: A profile card where `user@example.com` is dark and regular, and "Email Address" is small, uppercase, and light gray.

### R15: Emphasize Labels over Data When Comparing
**Source**: Refactoring UI
**Rule**: Emphasize labels instead of data only when the user is scanning a list of items looking for a specific data point.
**Why**: When comparing items (like specs on a pricing page), the user scans for the category first, then reads the data.
**Check**: Is the user looking for a specific field across multiple items, rather than reading a single item?
**Bad example**: On a tech specs comparison table, the feature name (label) is tiny and gray, while the checkmark (data) is huge and bold.
**Good example**: On a tech specs table, the feature name is prominent and dark, helping the user scan down the list to find "Battery Life".

### R16: Balance Weight with Contrast
**Source**: Refactoring UI
**Rule**: When you increase the weight or size of an element (like an icon), reduce its color contrast to maintain balance.
**Why**: An element that is both large/heavy AND high-contrast will overpower the layout.
**Check**: If an icon is placed next to text, does it steal all the attention from the text?
**Bad example**: A large, solid-fill icon placed next to a small paragraph, both using full black.
**Good example**: A large, solid-fill icon in a light, muted blue placed next to a dark gray paragraph, balancing the visual weight.

### R17: Increase Contrast on Thin Elements
**Source**: Refactoring UI
**Rule**: Use higher contrast colors for thin elements (borders, thin icons) to ensure they remain visible.
**Why**: Thin elements have less surface area, so they inherently carry less visual weight.
**Check**: Can you easily see the border or icon at a glance without squinting?
**Bad example**: A 1px border using a very light `#f3f4f6` gray that washes out on a white background.
**Good example**: A 1px border using a slightly darker `#e5e7eb` gray, or increasing the border to 2px if it must remain light.

### R18: Define the Invisible Primary
**Source**: Refactoring UI
**Rule**: Ensure every screen has one unambiguous primary element or action that holds the highest visual weight.
**Why**: Without a clear focal point, the user must expend cognitive effort figuring out what they are supposed to do or read first.
**Check**: If you blur your eyes, is there a single element that stands out above all others?
**Bad example**: A landing page where the headline, the hero image, and the primary button all share equal visual prominence.
**Good example**: A landing page where the "Start Free Trial" button uses a vibrant, saturated brand color while everything else is neutral.

## Anti-patterns

### The Shouting Page
Everything is bold, large, or brightly colored. Because everything is competing for attention, nothing is primary. The user's eye bounces around erratically, unable to settle on a starting point.

### The Invisible Primary
The most important action on the page (like the primary CTA) has the same visual weight as surrounding secondary content. It blends in, requiring the user to hunt for the next step.

### The Grey Fog
There is insufficient contrast between different levels of hierarchy. Headings, body text, and meta-information all look too similar, making the page feel like an undifferentiated wall of content.

### The Stranded Orphan
An element is placed indiscriminately, with its proximity failing to indicate its relationship. For example, a "Save" button floating halfway down the page, visually disconnected from the form it submits.

### The Tag-Driven Designer
Styling elements strictly by their HTML tags (e.g., forcing all `h2`s to be huge) regardless of their visual context, leading to components that are visually overpowered by their own labels.

## Quick Reference
- [ ] Combine size, weight, and color to establish hierarchy.
- [ ] Try de-emphasizing secondary content before making primary content larger.
- [ ] Use heavier font weights for primary data and actions.
- [ ] Group related items tightly; use generous whitespace to separate distinct groups.
- [ ] Style text based on visual importance, not HTML tags.
- [ ] Ensure the primary focal point is obvious and unambiguous.
- [ ] In key-value pairs, emphasize the data (usually) or the label (for scanning/comparing).
- [ ] Balance large/heavy elements by reducing their color contrast.
- [ ] Ensure a clear figure/ground distinction.
- [ ] Align content to natural scan paths (F-pattern for text, Z-pattern for sparse layouts).
