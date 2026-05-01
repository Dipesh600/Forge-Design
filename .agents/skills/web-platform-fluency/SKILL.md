---
name: web-platform-fluency
description: Governs all platform-specific design decisions for responsive Web applications. Use this skill whenever the user asks about responsive design, CSS Grid/Flexbox layouts, web navigation (megamenus, breadcrumbs), hover states, keyboard accessibility (focus rings), semantic HTML structure, SEO-friendly design, or adapting layouts across mobile, tablet, and ultra-wide desktop breakpoints.
---

# Web Platform Fluency

Designing for the Web requires a fundamentally different mindset than designing for native mobile. The Web is infinitely fluid; a user's viewport can be 320px wide or 4000px wide, and interaction modalities span mice, trackpads, keyboards, and touchscreens. This skill ensures that generated web designs embrace fluidity, respect web navigation conventions, and adhere to strict accessibility and semantic standards required by browsers and search engines.

## Core Principles

1. **Infinite Fluidity.** Web design is not drawing fixed screens; it is defining rules for how content reflows. Layouts must be intrinsically responsive, expanding and contracting gracefully before hitting hard breakpoints.
2. **Modality Agnostic.** The Web is used by mice (hover), keyboards (tabbing), and fingers (touch). Interfaces must explicitly design states for all three interaction methods.
3. **Semantic Structure.** The visual hierarchy must map 1:1 with semantic HTML tags (`<nav>`, `<main>`, `<h1>`, `<ul>`) for screen readers and SEO.
4. **The F-Pattern and Scanning.** Desktop web users do not read; they scan in an F-pattern. Layouts must front-load critical information and utilize ample whitespace to guide the eye.
5. **Progressive Enhancement.** Core content and functionality must be accessible even if complex scripts fail or viewport conditions are suboptimal.

## Rules

### R1: Fluid Before Breakpoints
**Source**: Web Standards (Responsive Design)
**Rule**: Utilize fluid measurement units (%, vw, vh, fr) and CSS Grid/Flexbox properties (like `flex-wrap` and `auto-fit`) to allow content to naturally reflow, relying on explicit media queries (breakpoints) only for major structural shifts.
**Why**: Hardcoding pixel widths for 3 specific breakpoints leaves users with awkward, stretched, or broken layouts in the countless viewport sizes in between.
**Check**: If you slowly drag the browser window from 1920px down to 375px, does the layout break at any point?
**Bad example**: Setting a card's width to exactly `300px` and changing it to `100%` only at a mobile breakpoint.
**Good example**: Setting a card grid to `grid-template-columns: repeat(auto-fit, minmax(300px, 1fr))`, allowing it to wrap automatically.

### R2: Hover is for Discovery, Not Necessity
**Source**: W3C / Nielsen Norman Group
**Rule**: Never hide critical information, actions, or navigation paths solely behind a `hover` interaction. Everything essential must be visible by default or accessible via a click/tap.
**Why**: Hover states do not exist on mobile devices (touchscreens). If an action is only revealed on hover, mobile users cannot use your site.
**Check**: If you disable the mouse entirely, can you still accomplish all tasks on the page?
**Bad example**: An e-commerce grid where the "Add to Cart" button only appears when the user's mouse hovers over the product image.
**Good example**: The "Add to Cart" button is always visible below the product, but hovering over the image reveals a secondary "Quick View" preview.

### R3: Explicit Keyboard Focus States
**Source**: WCAG 2.1
**Rule**: Every interactive element (links, buttons, inputs) must have a highly visible `:focus` state (usually a thick outline) for users navigating via the `Tab` key.
**Why**: Keyboard-only users and screen reader users rely entirely on focus rings to know where they are on the page. Disabling them is a severe accessibility violation.
**Check**: If you navigate the page using only the `Tab` key, is your current location always unmistakably obvious?
**Bad example**: Using `outline: none;` in CSS without providing a high-contrast alternative focus state.
**Good example**: Applying a 3px solid blue `:focus-visible` ring with a 2px `outline-offset` to all buttons.

### R4: Desktop Navigation Scaling (Megamenus)
**Source**: Nielsen Norman Group (Navigation)
**Rule**: On desktop breakpoints, utilize the horizontal space to expose navigation structure (e.g., horizontal header links or megamenus). Do not hide main navigation behind a hamburger menu on wide screens.
**Why**: Forcing desktop users to click a hamburger menu to discover navigation hides the "scent of information" and drastically reduces feature discovery.
**Check**: Is the primary navigation immediately readable upon loading the homepage on a laptop?
**Bad example**: A 1920px wide desktop layout where the top bar is empty except for a hamburger menu icon in the far right corner.
**Good example**: A desktop header displaying the top 5 product categories explicitly, with dropdowns for sub-categories.

### R5: Breadcrumbs for Deep Hierarchies
**Source**: Nielsen Norman Group (Navigation)
**Rule**: Use breadcrumb trails (e.g., Home > Electronics > Audio > Headphones) on desktop layouts for e-commerce or deeply nested content to orient the user and provide rapid upward navigation.
**Why**: Web users rarely arrive at a page via the homepage (they arrive via Google search). Breadcrumbs instantly explain where they are in the site's architecture.
**Check**: Can a user who lands directly on a product page via a search engine immediately understand the site structure?
**Bad example**: A product page with no context of its parent category.
**Good example**: A sticky breadcrumb trail placed just below the main header.

### R6: Strict Heading Hierarchy
**Source**: WCAG 2.1 / SEO Best Practices
**Rule**: Every page must have exactly one `<h1>` that describes the core topic, followed by sequentially nested `<h2>`, `<h3>`, etc. Never skip heading levels purely for visual styling.
**Why**: Screen readers use heading tags to build a table of contents for blind users. Search engines use them to understand page relevance. Skipping levels breaks the document outline.
**Check**: If you stripped away all CSS, does the document read like a perfectly formatted academic outline?
**Bad example**: Using an `<h4>` directly under an `<h1>` because the visual font size of `<h2>` was too large.
**Good example**: Using an `<h2>` for the section, but applying a CSS utility class like `.text-sm` if it needs to look smaller.

### R7: Max-Width for Readability (Measure)
**Source**: Web Typography Standards
**Rule**: Constrain blocks of text to a maximum measure of 65–75 characters (approx. 600px - 700px).
**Why**: If a text block stretches across an entire 1920px monitor, the user's eye loses its place when tracking from the end of one line back to the beginning of the next.
**Check**: Does the text naturally wrap before becoming exhaustingly long to read?
**Bad example**: A blog post article where the `<p>` tags stretch 100% of the screen width on a 4K monitor.
**Good example**: A blog post wrapped in a `<main class="max-w-2xl mx-auto">` container.

### R8: Cursor Changes for Affordances
**Source**: Web UX Standards
**Rule**: Always change the mouse cursor to a `pointer` (the hand icon) when hovering over buttons, links, and interactive elements, and use `not-allowed` for disabled elements.
**Why**: The cursor is the primary feedback mechanism on desktop web. If it doesn't change, the user assumes the element is static.
**Check**: Does the cursor explicitly tell the user whether the element beneath it can be clicked?
**Bad example**: A custom interactive `<div class="card">` that acts as a link but uses the default arrow cursor on hover.
**Good example**: Adding `cursor: pointer;` to the card, instantly signaling its interactivity.

### R9: Target Areas for Touch (Mobile Web)
**Source**: WCAG 2.1 / Apple HIG
**Rule**: When the layout drops to a mobile breakpoint (e.g., max-width 768px), all interactive links, buttons, and navigation items must physically expand or pad to a minimum of 44x44px.
**Why**: A text link in a paragraph is easy to click with a mouse, but nearly impossible to tap accurately with a thumb on a mobile screen.
**Check**: Are inline links spaced far enough apart on mobile that a user won't accidentally tap the wrong one?
**Bad example**: A dense footer with 20 tiny text links packed tightly together.
**Good example**: On mobile, the footer links convert to large, block-level elements with 12px of vertical padding.

### R10: Semantic Buttons vs. Links
**Source**: W3C HTML5 Standards
**Rule**: Use an `<a>` tag strictly when navigating to a new URL or page section. Use a `<button>` tag strictly when triggering an action on the current page (opening a modal, submitting a form, toggling state).
**Why**: Browsers, screen readers, and SEO bots treat links and buttons entirely differently. Using a link to open a modal breaks accessibility.
**Check**: Does clicking the element change the URL? If yes, it's a link. If no, it's a button.
**Bad example**: `<a href="#" onClick="openModal()">Save Changes</a>`
**Good example**: `<button type="button" onClick="openModal()">Save Changes</button>`

### R11: Skeleton over Blocking Spiners
**Source**: Web Performance UX
**Rule**: Prioritize rendering the HTML shell of the page immediately with skeleton placeholders for dynamic data, rather than showing a blank white page with a single loading spinner.
**Why**: Web network latency is highly variable. A blank screen increases bounce rates; a skeleton screen lowers perceived load time by establishing immediate context.
**Check**: Does the layout structure render instantly, even before the API returns the data?
**Bad example**: A React app that mounts and returns `null` or a full-page spinner while fetching data.
**Good example**: A React app that renders the header, sidebar, and a gray wireframe of the content area while fetching data.

### R12: The 8-Point Grid System (Web Adaptation)
**Source**: Web Design Standards
**Rule**: Use a spacing system based on multiples of 8 (8px, 16px, 24px, 32px, 64px) for margins, padding, and layout gaps.
**Why**: The 8pt grid scales perfectly across standard display resolutions without creating sub-pixel rendering issues (blurry lines).
**Check**: Are you using arbitrary spacing values like 11px or 27px?
**Bad example**: `margin-top: 15px; padding: 25px;`
**Good example**: `margin-top: 16px; padding: 24px;`

## Anti-patterns

### 1. The Mobile App Clone
Designing a desktop web application exactly like a mobile app. This includes hiding the main navigation in a hamburger menu on a 1920px screen, stretching single-column lists across ultra-wide monitors, and failing to utilize dense data presentation capabilities of a mouse/keyboard interface.

### 2. The Unreachable Hover
Hiding critical tools, actions, or sub-navigation exclusively behind a CSS `:hover` state. When users on mobile phones try to access the feature, they cannot, effectively locking them out of the product.

### 3. The SEO Disaster
Prioritizing visual design over semantic HTML by removing `<h1>` tags, using `<div onClick>` instead of `<button>` or `<a>`, and failing to provide `alt` text for images. This creates a site that is invisible to search engines and unusable for screen readers.

### 4. The Mouse Trap
Removing the `:focus` ring outline because "it looks ugly," completely trapping and disabling keyboard-only users who can no longer tell where they are on the page.

## Quick Reference
- [ ] Does the layout naturally reflow using CSS Grid/Flexbox before hitting fixed media queries?
- [ ] Is critical navigation exposed horizontally on desktop, rather than hidden in a hamburger?
- [ ] Are hover states strictly used for enhancement, not for hiding critical actions from mobile?
- [ ] Does every interactive element possess a highly visible `:focus-visible` state?
- [ ] Are headings strictly nested (H1 -> H2 -> H3) without skipping levels?
- [ ] Is text measure constrained to a readable ~70 characters wide?
- [ ] Does the cursor change to a pointer over all clickable elements?
- [ ] Are `<a>` tags used exclusively for URLs, and `<button>` tags for page actions?
- [ ] Do interactive elements expand to at least 44x44px on mobile viewports?
- [ ] Is the spacing utilizing a strict 8-point grid?
