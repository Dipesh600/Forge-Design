---
name: spatial-systems
description: Governs all structural, spatial, and compositional decisions in UI design. Use this skill whenever the user asks about grids, layout, spacing, margins, columns, alignment, responsiveness, white space, structural rhythm, compositional balance, padding, icon sizing, baseline grids, component heights, or predictable measurement systems. Also trigger for any UI review task — layout forms the structural skeleton of all interfaces and must always be evaluated for coherence.
---

# Spatial Systems

A grid is not a constraint; it is a grammatical system for space. It provides a logical, mathematical foundation that brings order, rhythm, and predictability to a layout. This skill ensures that interfaces are not arbitrarily placed on a canvas, but rather constructed using proportional modules, intentional margins, and disciplined vertical and horizontal rhythms. It incorporates the industry-standard 8pt grid methodology to eliminate arbitrary guesswork, ensure crisp pixel rendering on diverse screen resolutions, and create perfectly consistent visual rhythm across complex applications.

## Core Principles

1. **Order creates trust.** A mathematically sound grid projects authority, clarity, and professionalism; arbitrary placement creates subconscious unease.
2. **The content dictates the grid.** Never force content into a pre-made grid. Derive the module and column width from the nature of the primary content.
3. **Margins are spatial statements.** Margins are not just leftover space or "padding"; they are the frame that defines the active area and creates focus.
4. **White space is an active element.** Empty space is not a void to be filled; it is a structural block used to group, separate, and emphasize content.
5. **Violate the grid dominantly.** If you must break the grid to create emphasis, break it massively and intentionally. Subtle deviations look like mistakes.
6. **Eliminate guesswork.** Using a strict multiple-of-8 system means you never have to debate whether a margin should be 15px, 16px, or 17px. It is always 16px.
7. **Scale flawlessly.** Screen resolutions (1x, 2x, 3x) scale perfectly when base numbers are even and divisible by 8, preventing half-pixel rendering blurs.
8. **Decouple typography.** While containers and spacing use multiples of 8, typography baselines use a finer 4pt grid for necessary optical control.

## Rules

### R1: The 12-Column Standard
**Source**: *Grid Systems in Graphic Design* (The Grid and Design Philosophy)
**Rule**: Default to a 12-column grid for digital interfaces due to its superior divisibility (by 2, 3, 4, and 6) compared to 10- or 16-column grids.
**Why**: 12 columns provide the maximum structural flexibility for spanning different content types (halves, thirds, quarters) without fractional pixels.
**Check**: Can the active layout grid be evenly divided into 2, 3, 4, and 6 equal parts?
**Bad example**: Using a 10-column grid and trying to divide the layout into thirds.
**Good example**: Using a 12-column grid with a main content area spanning 8 columns and a sidebar spanning 4 columns.

### R2: Module Derivation
**Source**: *Grid Systems in Graphic Design* (The Typographic Grid)
**Rule**: Determine the size of the base grid module by first typesetting the primary body text at its optimal measure (45–75 characters).
**Why**: The grid must serve the content, not the other way around. The optimal reading width dictates the structural unit.
**Check**: Was the column width determined by the ideal line length of the primary text?
**Bad example**: Choosing an arbitrary 300px column width, forcing the text to wrap at uncomfortable 35-character intervals.
**Good example**: Typesetting the text to 65 characters, noting it takes up 600px, and using 600px as the foundational spatial unit.

### R3: Gutter-to-Column Ratio
**Source**: *Grid Systems in Graphic Design* (Width of Column)
**Rule**: Ensure gutter widths are visibly narrower than column widths, typically mapping to the width of 1 to 2 ems of the base typography.
**Why**: If gutters are too wide, the columns disconnect and lose their relationship; if too narrow, the text runs together.
**Check**: Is the gutter width strictly smaller than the width of a single column, but large enough to clearly separate text blocks?
**Bad example**: A 100px column separated by a 100px gutter.
**Good example**: A 60px column separated by a 24px gutter.

### R4: [DIAGRAM] The Margin Frame
**Source**: *Grid Systems in Graphic Design* (Margin Proportions)
**Diagram**: An illustration showing a page with progressively larger margins from top, to inner, to outer, to bottom.
**Rule**: Treat margins as an active, proportional frame that protects the content from the edge of the screen, ensuring the bottom margin is the largest to anchor the layout visually.
**Check**: Are the outer margins of the container distinct from the internal gutters, and is the bottom margin visually anchoring the content?
**Bad example**: Using 16px padding on all sides of a container, making it look top-heavy due to optical weight.
**Good example**: Setting a top margin of 48px and a bottom margin of 64px to give the layout optical balance.

### R5: The Baseline Grid
**Source**: *Grid Systems in Graphic Design* (Leading)
**Rule**: Lock all text elements, regardless of font size, to a strict vertical baseline grid (typically a multiple of the base line-height).
**Why**: A baseline grid creates an invisible, harmonious vertical rhythm that guides the eye smoothly down the page.
**Check**: If you draw horizontal lines across the screen at consistent intervals, does the bottom of every text element sit exactly on a line?
**Bad example**: Text blocks with varying, arbitrary line-heights that drift out of vertical alignment with adjacent columns.
**Good example**: All text elements snapping to a strict 4px or 8px vertical baseline grid.

### R6: [DIAGRAM] Image Spanning
**Source**: *Grid Systems in Graphic Design* (Photographs and the Grid)
**Diagram**: Images of various sizes aligning perfectly to the column edges and gutters.
**Rule**: Size all images and graphical elements to span an exact number of columns, starting and ending precisely on column edges, never inside a gutter.
**Check**: Do the left and right edges of every image align perfectly with the defined column tracks?
**Bad example**: An image that spans 3.5 columns, bleeding arbitrarily into a gutter.
**Good example**: An image sized to span exactly 4 columns, snapping edge-to-edge.

### R7: The Dominant Violation
**Source**: *Grid Systems in Graphic Design* (Breaking the Grid)
**Rule**: If an element must break the grid for emphasis, it must span multiple columns or break the margin drastically.
**Why**: A minor misalignment looks like an error. A massive, intentional violation signals to the user that this element is fundamentally different or important.
**Check**: If an element breaks the grid, does it do so aggressively enough to look intentional rather than mistaken?
**Bad example**: An element shifted 10px outside the grid line.
**Good example**: A hero image that intentionally breaks the container and bleeds entirely off the edge of the screen.

### R8: [DIAGRAM] Spatial Rhythm and Repetition
**Source**: *Grid Systems in Graphic Design* (Spatial Rhythm)
**Diagram**: A series of recurring spatial intervals separating different groups of content.
**Rule**: Use a limited set of recurring spatial values (e.g., small, medium, large intervals) consistently throughout the entire interface to separate components.
**Check**: Are you using a strict, limited scale of spacing variables rather than bespoke padding for every element?
**Bad example**: A page with gaps of 12px, 15px, 22px, 28px, and 40px between various elements.
**Good example**: A strict spacing scale using only 16px, 32px, and 64px intervals.

### R9: Active White Space
**Source**: *Grid Systems in Graphic Design* (Design Philosophy)
**Rule**: Use empty grid modules to group related elements together by pushing unrelated elements further away.
**Why**: Proximity is the strongest visual signal of relationship. White space is the tool that creates proximity.
**Check**: Is the white space separating two distinct sections noticeably larger than the white space separating elements within a section?
**Bad example**: Equal 20px spacing between a headline and its paragraph, AND between that paragraph and the next distinct section.
**Good example**: 16px spacing between a headline and its paragraph, and 64px spacing before the next section begins.

### R10: Responsive Proportion Translation
**Source**: *Grid Systems in Graphic Design* (Adaptation)
**Rule**: When translating a grid to a smaller screen, maintain the proportional relationships (the module) while reducing the number of columns (e.g., 12 to 8 to 4).
**Why**: The mathematical harmony of the layout must survive screen resizing; shrinking everything destroys legibility, while maintaining columns on mobile destroys the module.
**Check**: On mobile breakpoints, are there fewer columns rather than microscopically thin columns?
**Bad example**: Squeezing a 12-column grid onto a mobile screen, resulting in 10px wide columns.
**Good example**: Reflowing a 12-column desktop grid into a 4-column mobile grid.

### R11: The Soft Grid (Spacing)
**Source**: *8pt Grid System Conventions* (Spacing)
**Rule**: All margins, padding, and layout gaps between elements must be a strict multiple of 8 (8, 16, 24, 32, 40, 48, 64, 80, 96, 128).
**Why**: Consistent spacing intervals train the user's eye to recognize structural patterns and hierarchy instantly.
**Check**: Are all margin and padding values in the CSS/design file divisible by 8?
**Bad example**: Using padding values of 10px, 15px, or 20px.
**Good example**: Standardizing padding values to 8px, 16px, or 24px.

### R12: The Hard Grid (Component Sizing)
**Source**: *8pt Grid System Conventions* (Sizing)
**Rule**: The height (and ideally width, when fixed) of all UI components like buttons, inputs, and cards must be a multiple of 8.
**Why**: When components have heights like 32px, 40px, or 48px, they stack and align perfectly against each other on the spatial grid without awkward remainders.
**Check**: Is the overall height of the button or text input exactly divisible by 8?
**Bad example**: A primary button with a height of 45px.
**Good example**: A primary button with a height of 48px (which also satisfies mobile touch target requirements).

### R13: The 4pt Baseline Exception
**Source**: *8pt Grid System Conventions* (Typography & The 8pt Grid)
**Rule**: Align typography to a 4pt baseline grid by ensuring that the CSS `line-height` value is a multiple of 4 (16, 20, 24, 28, 32).
**Why**: Text requires finer vertical control than containers. A strict 8pt scale pushes text too far apart at certain sizes; a 4pt baseline maintains rhythm while allowing optical comfort.
**Check**: Is the `line-height` of every text style a multiple of 4?
**Bad example**: Font size 15px with a line-height of 22px.
**Good example**: Font size 16px with a line-height of 24px.

### R14: Strict Icon Framing
**Source**: *8pt Grid System Conventions* (Icons & The 8pt Grid)
**Rule**: Place all vector icons inside a square bounding box (frame) whose dimensions are a multiple of 8 (16x16, 24x24, 32x32), regardless of the icon's internal shape.
**Why**: Icons vary wildly in internal shape. By standardizing their bounding boxes to the 8pt grid, they will always align perfectly with adjacent text and buttons.
**Check**: Is the container frame for the icon sized to a multiple of 8?
**Bad example**: Exporting a star icon exactly to its edges at 21x19px.
**Good example**: Placing that star icon inside a transparent 24x24px container frame before exporting or aligning it in code.

### R15: The 4pt Micro-adjust (Half-step)
**Source**: *8pt Grid System Conventions* (The 4pt Grid)
**Rule**: For extremely tight UI clusters (like an icon sitting next to its immediate text label), use a 4pt spacing interval.
**Why**: Sometimes 8pt is too loose to communicate strict grouping. The 4pt half-step provides an acceptable, mathematically sound exception for micro-UI relationships.
**Check**: If 8pt spacing feels too wide, did you step down specifically to 4pt rather than an arbitrary number?
**Bad example**: Adjusting an icon 5px away from its label because 8px looked too far.
**Good example**: Setting the gap between the icon and text to exactly 4px.

## Anti-patterns

### 1. The Arbitrary Margin
Spacing values chosen without relationship to a base unit or modular scale. The layout feels subtly "off" or messy, but the user cannot articulate why.

### 2. The Rogue Element
A single component (often a button, image, or card) that ignores the grid tracks entirely, floating misaligned and breaking the compositional coherence of the page.

### 3. The Suffocated Content
Gutters that are too narrow relative to the column width, or margins that are too tight to the screen edge. The content feels cramped, and columns bleed together illegibly.

### 4. The Micro-Grid
Creating a grid with so many columns (e.g., 24 or 36) that the grid itself ceases to provide structural discipline, essentially allowing elements to be placed anywhere.

### 5. The False Alignment
Aligning the bounding box of an element to the grid rather than its visual weight (e.g., aligning the edge of a circular icon to the grid line, making it optically appear indented compared to flat text).

### 6. The Magic Number
Hardcoding an arbitrary pixel value (like `margin-top: 13px`) just to make a specific layout "look right," completely breaking the systemic rhythm of the interface. 

### 7. The Fluid Icon
Dropping raw vector icons into a layout without placing them in standardized 24x24 or 32x32 bounding boxes, causing the entire layout to shift depending on which specific icon is loaded.

### 8. The Typographic Drift
Setting line-heights using relative percentages (like `1.5`) that resolve to sub-pixels or odd numbers (like `21px`), causing the text to drift completely off the 4pt/8pt vertical grid over multiple lines.

## Quick Reference

- [ ] Does the layout use a 12-column grid?
- [ ] Is the module width derived from comfortable reading line-lengths?
- [ ] Do all images and containers snap perfectly to column edges?
- [ ] Are gutter widths strictly maintained and smaller than columns?
- [ ] Do all text elements align to a predictable vertical baseline grid?
- [ ] Is the spacing between distinct sections visibly larger than internal spacing?
- [ ] If an element breaks the grid, does it do so obviously and intentionally?
- [ ] Does the grid reduce column count elegantly on mobile screens?
- [ ] Are all margins and padding values multiples of 8?
- [ ] Are the heights of all standard components (buttons, inputs) multiples of 8?
- [ ] Is every typographic `line-height` a multiple of 4?
- [ ] Are all icons wrapped in strict 16x16, 24x24, or 32x32 containers?
- [ ] Are there zero half-pixel or odd-number measurements in the inspector?
