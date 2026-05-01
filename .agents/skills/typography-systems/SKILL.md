---
name: typography-systems
description: Governs all decisions regarding fonts, text sizes, line height (leading), line length (measure), paragraph alignment, typeface pairings, typographic hierarchy, and text legibility. Use this skill whenever the user asks about reading experiences, font choices, text layout, WCAG contrast for text, or when evaluating why a page of text looks overwhelming, messy, or hard to read.
---

# Typography Systems

Typography is the mechanical notation and arrangement of language. Good typography should be practically invisible, honoring the content without distracting from it. In UI design, robust typographic systems create hierarchy, guide the eye, and ensure legibility across all devices.

## Core Principles
1. **Typography is for reading.** Aesthetics never trump legibility. If the user struggles to read the text, the typography has failed.
2. **Contrast creates hierarchy.** Use stark contrasts in size, weight, and color to denote importance. Subtle variations create confusion.
3. **Respect the mathematical relationship.** Type size, line height, and line length are inherently linked. Changing one requires adjusting the others.
4. **Embrace negative space.** White space around text is what makes the text readable. Don't crowd your typography.

## Rules

### R1: Optimize the Measure (Line Length)
**Source**: The Elements of Typographic Style
**Rule**: Constrain the length of body text lines to an optimal measure of 45 to 75 characters (including spaces).
**Why**: If a line is too long, the eye loses its place when scanning back to the left margin. If it's too short, the eye has to jump too frequently, breaking the reading rhythm.
**Check**: Count the characters in a typical line of your body text. Is it between 45 and 75?
**Bad example**: A paragraph spanning the entire 1200px width of a desktop monitor (150+ characters).
**Good example**: A blog post article constrained to a maximum width of 680px (approx. 65 characters).

### R2: Scale Leading with Measure
**Source**: The Elements of Typographic Style
**Rule**: Set leading (line height) between 1.3x and 1.6x the font size for body text. Increase leading as the measure (line length) gets longer.
**Why**: Wider columns of text require more vertical space between lines to help the eye accurately track back to the beginning of the next line.
**Check**: Is the `line-height` of the body text explicitly set to a relative value between `1.3` and `1.6`?
**Bad example**: `font-size: 16px; line-height: 1;` (Lines are too cramped).
**Good example**: `font-size: 16px; line-height: 1.5;` for standard body text.

### R3: Never Justify Text on the Web
**Source**: Typographic Law
**Rule**: Use left-aligned (ragged right) text for all paragraphs of body copy. Never use `text-align: justify`.
**Why**: Web browsers lack the sophisticated hyphenation and justification engines of print software. Justified web text creates massive, distracting "rivers" of white space between words.
**Check**: Is the text alignment set to anything other than left-aligned for body copy?
**Bad example**: A multi-line paragraph block set to `text-align: justify`.
**Good example**: A multi-line paragraph block set to `text-align: left`.

### R4: Avoid Centering Long Text
**Source**: Typographic Law
**Rule**: Center alignment should be used exclusively for short headings or text blocks of 3 lines or fewer.
**Why**: Center-aligned text forces the user's eye to search for the unpredictable starting point of every new line, drastically slowing down reading speed.
**Check**: Is there a center-aligned block of text longer than 3 lines?
**Bad example**: A 6-line product description paragraph centered under an image.
**Good example**: A 2-line hero heading centered at the top of a landing page.

### R5: Establish a Modular Scale
**Source**: The Elements of Typographic Style
**Rule**: Base your typographic hierarchy (heading sizes) on a mathematical modular scale (e.g., Major Third: 1.250, Perfect Fourth: 1.333) rather than arbitrary values.
**Why**: A modular scale ensures a harmonious and predictable rhythm between all text elements on the screen.
**Check**: Is there a consistent mathematical multiplier between your H3, H2, and H1 sizes?
**Bad example**: H1: 32px, H2: 28px, H3: 26px (arbitrary sizes with low contrast).
**Good example**: Base: 16px. H3: 20px, H2: 25px, H1: 31px (using a 1.250 scale).

### R6: Skip Weights to Create Contrast
**Source**: Typographic Law
**Rule**: When combining font weights within the same family to create hierarchy, skip at least one weight (e.g., pair Light with Bold, or Regular with Extra Bold).
**Why**: Pairing adjacent weights (like Regular and Medium) provides insufficient visual contrast; it looks like a mistake rather than an intentional hierarchy.
**Check**: Are the two paired weights noticeably distinct at a quick glance?
**Bad example**: A card title in `font-weight: 500` and the subtitle in `font-weight: 400`.
**Good example**: A card title in `font-weight: 700` (Bold) and the subtitle in `font-weight: 400` (Regular).

### R7: Enforce Contrast in Typeface Pairings
**Source**: The Elements of Typographic Style
**Rule**: When pairing two different typefaces, ensure they are distinctly different (e.g., a Serif with a Sans-Serif). Never pair two similar typefaces from the same classification.
**Why**: Typefaces from the same classification (e.g., Helvetica and Arial) will clash because they are too similar to provide contrast, but too different to look cohesive.
**Check**: Could a user easily mistake the two typefaces for being the exact same font?
**Bad example**: Pairing Open Sans for headings with Roboto for body text.
**Good example**: Pairing Merriweather (Serif) for headings with Open Sans (Sans-Serif) for body text.

### R8: Limit the Font Salad
**Source**: Typographic Law
**Rule**: Use a maximum of two font families per project (one for headings, one for body). One is often enough.
**Why**: Loading multiple font families harms page performance and creates visual chaos. A single font family with a variety of weights is usually sufficient for a complete hierarchy.
**Check**: Are there 3 or more distinct font families loaded in the UI?
**Bad example**: A landing page using Oswald for H1s, Playfair for H2s, and Inter for body text.
**Good example**: A landing page using Inter for all text, relying on size, weight, and color to establish hierarchy.

### R9: Track Out All-Caps Text
**Source**: The Elements of Typographic Style
**Rule**: When setting text in ALL CAPS, increase the letter-spacing (tracking) by 5% to 10% (e.g., `letter-spacing: 0.05em`).
**Why**: Capital letters were designed to sit next to lowercase letters. When placed next to each other, they appear too tightly packed and clump together.
**Check**: Does the ALL CAPS text have added letter-spacing?
**Bad example**: `text-transform: uppercase; letter-spacing: 0;`
**Good example**: `text-transform: uppercase; letter-spacing: 0.05em;`

### R10: Never Use All-Caps for Body Text
**Source**: Typographic Law
**Rule**: Restrict ALL CAPS exclusively to short labels, acronyms, or brief headings. Never set paragraphs or multi-line sentences in all-caps.
**Why**: Lowercase letters have ascenders and descenders that create unique word shapes, which the brain recognizes instantly. All-caps text forms identical rectangular blocks, forcing the brain to read letter-by-letter.
**Check**: Is there a sentence longer than 5 words set entirely in uppercase?
**Bad example**: A 3-line legal disclaimer written in ALL CAPS.
**Good example**: A small label "NEW ARRIVAL" set in ALL CAPS above a product image.

### R11: Tighten Leading on Large Headings
**Source**: Typographic Law
**Rule**: As font size increases, relative line height must decrease. Set large headings (H1, H2) to a tighter leading (1.1 to 1.2) than body text.
**Why**: At large sizes, standard body leading (1.5) creates massive gaps between lines, breaking the heading apart visually.
**Check**: Is the line-height of the H1 tighter than the line-height of the body paragraph?
**Bad example**: `h1 { font-size: 48px; line-height: 1.5; }` (72px line height creates a huge gap).
**Good example**: `h1 { font-size: 48px; line-height: 1.1; }` (52px line height keeps the heading cohesive).

### R12: Adhere to WCAG Contrast Minimums
**Source**: WCAG Accessibility Guidelines
**Rule**: Body text must maintain a minimum contrast ratio of 4.5:1 against its background. Large text (18pt+ or 14pt+ bold) must maintain a 3.1:1 ratio.
**Why**: Low contrast text is completely unreadable for users with visual impairments, older users, or anyone using a low-quality monitor or sitting in bright sunlight.
**Check**: Run the text color and background color through a contrast checker. Does it pass AA standards?
**Bad example**: Light gray text (`#999999`) on a white background (`#FFFFFF`) — Ratio: 2.8:1.
**Good example**: Dark gray text (`#595959`) on a white background (`#FFFFFF`) — Ratio: 4.6:1.

### R13: Tie Headings to Their Subsequent Content
**Source**: Typographic Law (Proximity)
**Rule**: A heading must sit visually closer to the paragraph that follows it than the paragraph that precedes it.
**Why**: Gestalt law of proximity: elements placed close together are perceived as a group. A heading belongs to the text beneath it.
**Check**: Is the `margin-top` of the heading significantly larger than its `margin-bottom`?
**Bad example**: A heading with 24px margin on top and 24px margin on the bottom.
**Good example**: A heading with 48px margin on top and 16px margin on the bottom.

### R14: Set a Minimum Baseline Size for Web
**Source**: Typographic Law
**Rule**: The primary body text size for desktop and mobile screens should be no smaller than 16px.
**Why**: Mobile screens are held at roughly the same distance as a book; desktop screens are further away. 16px provides a safe, highly legible baseline for the vast majority of typefaces.
**Check**: Is the primary body font size set to less than 16px?
**Bad example**: A blog post where the main reading text is set to 12px.
**Good example**: A blog post where the main reading text is set to 18px, with 14px reserved strictly for secondary meta-data (dates, tags).

### R15: Use Tabular Figures for Data
**Source**: The Elements of Typographic Style
**Rule**: When displaying columns of numbers (pricing tables, dashboards, timers), use tabular figures (`font-variant-numeric: tabular-nums`).
**Why**: Standard proportional numbers vary in width (a '1' is narrower than an '8'), causing columns of numbers to misalign jaggedly. Tabular figures give every number the exact same width.
**Check**: If you stack the number 111 above the number 888, do they align perfectly?
**Bad example**: A financial data table where the decimal points don't align because the numbers are proportional.
**Good example**: A financial data table using `tabular-nums` so every digit perfectly aligns vertically.

## Anti-patterns

### The Wall of Text
A paragraph where the measure exceeds 80 characters, often coupled with tight leading (< 1.3). It looks visually intimidating and causes the reader's eye to get lost when tracking back to the start of the next line.

### The Typographic Whisper
Text that uses ultra-thin font weights or low-contrast colors (e.g., light gray on white) in a misguided attempt to look "clean" or "minimalist," resulting in a completely illegible interface.

### The Font Salad
An interface that loads 3, 4, or 5 different font families. It creates a chaotic, disorganized visual language that lacks cohesion and damages page load performance.

### The River of White Space
The result of applying `text-align: justify` to web copy. Because web browsers cannot hyphenate accurately, justification forces massive, irregular gaps of white space between words that form distracting visual "rivers" down the paragraph.

### The Floating Heading
A heading with equal top and bottom margins. It floats equidistantly between two paragraphs, breaking the principle of proximity and making it unclear which paragraph the heading actually belongs to.

### The Shouting Paragraph
A multi-line block of text set entirely in ALL CAPS. It strips away the unique word shapes created by ascenders and descenders, reducing reading speed and conveying an aggressive, shouting tone.

## Quick Reference
- [ ] Is the body text line length constrained to 45–75 characters?
- [ ] Is the body line height set between 1.3 and 1.6?
- [ ] Are headings tighter in line height (1.1-1.2) than body text?
- [ ] Is all body text left-aligned (never justified)?
- [ ] Are font sizes scaled using a consistent mathematical ratio?
- [ ] Are font weights skipped to create high contrast (e.g., Regular to Bold)?
- [ ] Is ALL CAPS restricted to short labels and tracked out (letter-spacing)?
- [ ] Does the text pass the 4.5:1 WCAG contrast minimum?
- [ ] Do headings sit closer to the text below them than the text above them?
- [ ] Are tabular numbers used for data columns?
