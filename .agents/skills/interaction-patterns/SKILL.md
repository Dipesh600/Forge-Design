---
name: interaction-patterns
description: Governs all interaction patterns, component states (hover, active, disabled, loading, empty, error), navigation models, gestural interactions, and cognitive-perceptual constraints like Fitts's Law and Hick's Law. Use this skill whenever the user asks about interactive states, loading screens, empty states, error handling, confirmations, modals, button sizes, touch targets, chunking, or animation. Trigger for any UI review focusing on how components behave when interacted with.
---

# Interaction Patterns

Interaction patterns govern how static visuals respond to human input. A robust interaction system respects the limits of human perception, memory, and motor control, while ensuring every interactive component explicitly communicates its current state to the user.

## Core Principles
1. **Design for human limits.** Respect short-term memory (4±1 items) and motor precision; interfaces must adapt to human biology, not the other way around.
2. **Every state must be designed.** An interactive element isn't just its default appearance; normal, hover, pressed, and disabled states are mandatory.
3. **Motion is a responsibility.** Animation instinctively hijacks peripheral vision; use it only to convey meaning or direct attention, never purely for decoration.
4. **Errors are inevitable; recovery must be cheap.** The UX cost of an error is the time and effort required to reverse it. Design for effortless reversibility.
5. **Respect the user's workflow.** Never interrupt the user with a modal dialog unless the system cannot proceed without their explicit input.

## Rules

### R1: Apply Fitts's Law to Primary Actions
**Source**: Designing with the Mind in Mind
**Rule**: Make primary interactive targets large and place them close to the user's natural starting cursor/thumb position.
**Why**: The time required to rapidly move to a target area is a function of the ratio between the distance to the target and the width of the target.
**Check**: Is the primary action the largest and most easily reachable target on the screen?
**Bad example**: A tiny 16x16px "Save" icon placed in the far top-right corner of a dense form.
**Good example**: A large, full-width "Submit" button placed immediately below the final input field.

### R2: Avoid Relying on Red/Green Distinctions
**Source**: Designing with the Mind in Mind
**Rule**: Never use the distinction between red and green as the sole indicator of system state.
**Why**: Red-green color blindness is the most common form of color vision deficiency. Blue and orange is a much safer oppositional pairing.
**Check**: If you render the screen in grayscale, can you still tell the success state from the error state?
**Bad example**: A system status dashboard where online servers are marked with a green dot and offline servers with a red dot, with no other text or iconography.
**Good example**: Online servers have a green checkmark icon; offline servers have a red warning triangle icon.

### R3: Limit Choices to Reduce Decision Paralysis
**Source**: Designing with the Mind in Mind
**Rule**: Limit the number of presented options at any given time to avoid overwhelming the user (Hick's Law).
**Why**: The time it takes to make a decision increases logarithmically with the number of choices. More options often lead to choice paralysis.
**Check**: Could this list of options be categorized, chunked, or reduced without losing core functionality?
**Bad example**: A main navigation menu with 40 flat links all exposed simultaneously.
**Good example**: A navigation menu with 5 top-level categories, each revealing its specific links only upon interaction.

### R4: Chunk Information for Short-Term Memory
**Source**: Designing with the Mind in Mind
**Rule**: Group related items into chunks of 4±1 to accommodate the strict limits of human working memory.
**Why**: The brain can reliably hold about 4 items in active memory at once; chunking allows the brain to process larger amounts of information by remembering the chunks instead of the individual pieces.
**Check**: Are long lists or numbers broken into smaller, easily digestible groups?
**Bad example**: Displaying a 16-digit credit card number as a single continuous string: `4111222233334444`.
**Good example**: Displaying a credit card number in chunks of four: `4111 2222 3333 4444`.

### R5: Use Motion Exclusively for Meaning
**Source**: Designing with the Mind in Mind
**Rule**: Use animation only to draw attention to critical state changes, establish spatial relationships, or provide feedback—never for mere decoration.
**Why**: The human visual system is biologically wired to immediately focus on motion in the periphery. Gratuitous animation creates cognitive fatigue and distraction.
**Check**: If this animation were removed, would the user lose context about what just happened in the interface?
**Bad example**: A dashboard where every widget bounces and fades in slowly upon page load, distracting the user from their actual data.
**Good example**: A newly added item in a list briefly flashes a subtle background color so the user knows exactly where it appeared.

### R6: Make Actions Easily Reversible
**Source**: Designing with the Mind in Mind
**Rule**: Provide an "Undo" mechanism for all destructive or complex actions rather than relying solely on confirmation dialogs.
**Why**: The cost of an error is proportional to the effort required to fix it. "Undo" provides a safety net that encourages exploration and reduces anxiety.
**Check**: If the user accidentally clicks the primary action, can they reverse it with a single click?
**Bad example**: Deleting an email instantly removes it forever without any way to recover it.
**Good example**: Deleting an email removes it from the list and shows a temporary toast notification with an "Undo" button.

### R7: Design the 4 Core Component States
**Source**: About Face
**Rule**: Every interactive element must have explicitly designed normal, hover, active (pressed), and disabled states.
**Why**: Missing states break the affordance feedback loop, leaving users uncertain if the system registered their intent.
**Check**: If I mouse over, click, and disable this component, does it visually change every time?
**Bad example**: A button that looks the same whether it is enabled or disabled, relying on an error message to explain why it won't click.
**Good example**: A button that dims and drops its shadow when disabled, darkens on hover, and visually depresses on click.

### R8: Use Skeleton Screens for Expected Layouts
**Source**: About Face
**Rule**: Use skeleton screens (placeholder UI) instead of loading spinners when the structure of the incoming data is predictable.
**Why**: Skeleton screens reduce perceived loading time by providing an immediate structural mental model, whereas spinners draw attention to the delay.
**Check**: Do you know the layout of the content that is currently loading?
**Bad example**: Showing a single, tiny spinning circle in the center of the page while a complex dashboard layout loads.
**Good example**: Showing a grayed-out wireframe representation of the text blocks and images that are about to populate the screen.

### R9: Use Spinners for Indeterminate or Background Processes
**Source**: About Face
**Rule**: Use loading spinners or progress bars for actions where the outcome is not a new visual layout (e.g., submitting a form, uploading a file).
**Why**: When the layout isn't changing, the user just needs to know that the system is actively working on their request.
**Check**: Is the system performing an action rather than fetching a new view?
**Bad example**: Freezing the UI with no feedback while a 5MB file uploads.
**Good example**: Replacing the "Submit" button text with a spinner and the word "Uploading..."

### R10: Empty States Must Educate and Prompt
**Source**: About Face
**Rule**: Never show a blank screen or a simple "No data" message for an empty state. Always explain what goes here and provide a clear primary action to populate it.
**Why**: The first-run empty state is the prime opportunity to set the user's mental model of what the feature does.
**Check**: Does the empty state tell the user exactly what to do next to make the screen useful?
**Bad example**: A saved items page that just says "0 Items Found" in the center of the screen.
**Good example**: A saved items page showing an illustration, the text "You haven't saved any articles yet", and a "Browse Articles" button.

### R11: Error States Must Provide a Path Forward
**Source**: About Face
**Rule**: Error messages must explicitly state what happened, why it happened (if helpful), and the exact action required to fix it.
**Why**: Reporting an error without a solution creates a dead end, punishing the user.
**Check**: Can the user read the error message and immediately know exactly what they must type or click to resolve the issue?
**Bad example**: "Form submission failed. Error Code: 400."
**Good example**: "We couldn't process your payment because the card has expired. Please update your expiration date or use a different card."

### R12: Justify Modals with Strict Interruption Rules
**Source**: About Face
**Rule**: Use modal dialogs strictly for critical actions that require the user's explicit attention before the system can proceed.
**Why**: Modals are hostile interruptions that break the user's flow. They should be reserved for irreversible actions or systemic blockages.
**Check**: If the user ignores this message, will the system break or will data be permanently lost?
**Bad example**: Using a modal dialog to show a non-critical marketing announcement or a "Successfully saved" confirmation.
**Good example**: Using a modal dialog to confirm the deletion of an entire project repository.

### R13: Prevent Confirmation Habituation
**Source**: About Face
**Rule**: For highly destructive actions, require the user to explicitly acknowledge the specific consequence, not just click "Yes" or "OK".
**Why**: Users quickly learn to blindly click "OK" on standard confirmation dialogs without reading them (habituation).
**Check**: Does the confirmation dialog require the user to read the button text to know what they are confirming?
**Bad example**: A dialog asking "Are you sure you want to delete this?" with the buttons "Cancel" and "OK".
**Good example**: A dialog asking "Delete the project 'Alpha'?" with the buttons "Keep Project" and "Delete Project".

### R14: Differentiate Primary and Secondary Actions
**Source**: About Face
**Rule**: In dialogs or forms, visually differentiate the primary action (commit) from the secondary action (cancel) using distinct weights.
**Why**: Two buttons of equal visual weight require the user to stop and read both to avoid making a mistake.
**Check**: If you blurred the screen, could you still tell which button moves the process forward?
**Bad example**: A form with two identical blue buttons side-by-side: "Submit" and "Cancel".
**Good example**: A form where "Submit" is a solid blue button, and "Cancel" is a borderless, gray text link.

### R15: Match Affordances to Gestural Expectations
**Source**: About Face
**Rule**: Use standard gestural patterns (swipe to delete, pinch to zoom) only where users natively expect them, and provide visible alternatives.
**Why**: Gestures are inherently invisible affordances; if a user doesn't know the gesture exists, the feature is inaccessible.
**Check**: Is there a visible button or menu option that accomplishes the same task as the hidden gesture?
**Bad example**: An email app where the *only* way to delete an email is to swipe left, with no delete button on the screen.
**Good example**: An email app that allows swiping left to delete, but also provides a visible "Edit" mode with explicit delete buttons.

### R16: Distinguish Clickable from Draggable
**Source**: About Face
**Rule**: Elements that support drag-and-drop must feature explicit drag affordances (like a grip icon) separate from their clickable areas.
**Why**: Mixing click and drag on the exact same surface creates ambiguity and frustrating mis-clicks.
**Check**: Does the user know exactly where to click to drag the element versus where to click to open it?
**Bad example**: A list item where clicking anywhere opens the detail view, but clicking and dragging anywhere also reorders the list.
**Good example**: A list item where the main body opens the detail view, and a distinct "six-dot" grip icon on the left edge is used exclusively for dragging.

### R17: Use Flat Navigation for Frequent Switching
**Source**: About Face
**Rule**: Use flat navigation (e.g., bottom tab bars) rather than hierarchical navigation (e.g., deep menus) when users need to rapidly switch between core views.
**Why**: Hierarchical navigation requires high interaction cost (backing out, opening menus, finding new targets) compared to the single-tap cost of a flat navigation bar.
**Check**: Do users need to frequently alternate between these two views in a single session?
**Bad example**: Placing the user's "Inbox" and "Calendar" inside a deep hamburger menu that requires 3 taps to switch between.
**Good example**: A bottom navigation bar with "Inbox" and "Calendar" icons, allowing 1-tap switching.

### R18: Retain Context in In-Place Editing
**Source**: About Face
**Rule**: Allow users to edit data in the context of its display (in-place editing) rather than navigating them to a separate "Edit" page.
**Why**: Taking users away from the context of their data forces them to memorize the context, increasing cognitive load.
**Check**: Can the user see the surrounding data while editing this specific field?
**Bad example**: Clicking an item in a list navigates the user to a completely new screen just to change the item's title.
**Good example**: Clicking the title of an item transforms the text into an input field right where it is.

### R19: Indicate Selection Clearly
**Source**: About Face
**Rule**: When an item is selected from a group, its selected state must be unambiguously distinct from the normal and hover states of the unselected items.
**Why**: Users must be able to glance at a list or navigation bar and immediately know their current location or selection.
**Check**: Is the selected state visually distinct enough that it cannot be confused with a hover state?
**Bad example**: The selected navigation tab is slightly darker gray, while hovering over an unselected tab makes it the exact same dark gray.
**Good example**: The selected navigation tab has a bold font, a distinct brand-colored background, and a strong underline, completely unlike the hover state.

### R20: Provide Feedback on Long Operations
**Source**: About Face
**Rule**: Any operation taking longer than 1 second requires a progress indicator; anything longer than 10 seconds requires a determinate progress bar and an option to cancel.
**Why**: Users perceive delays longer than 1 second as a system failure unless told otherwise. Delays over 10 seconds break the user's attention span.
**Check**: Does the user know roughly how much longer they have to wait for this operation to complete?
**Bad example**: An indeterminate spinning icon for a database migration that takes 3 minutes.
**Good example**: A determinate progress bar showing "45% complete (Estimated time: 2 mins)" with a "Cancel Migration" button.

### R21: Differentiate Hover from Focus
**Source**: About Face
**Rule**: Ensure the keyboard focus state (tabbing) is visually distinct from the mouse hover state to support accessibility and varied input modes.
**Why**: Focus states serve users navigating via keyboard, while hover states serve pointer devices. Conflating them can confuse keyboard users about where their focus actually lies.
**Check**: If you tab to a button, does it look specifically focused (e.g., an outline) rather than just hovered?
**Bad example**: Using `outline: none` for keyboard focus, relying entirely on a subtle background color change shared with the hover state.
**Good example**: A button that darkens on hover, but gains a prominent 2px blue focus ring when navigated to via the keyboard.

### R22: Avoid Form Format Rigidity
**Source**: Designing with the Mind in Mind
**Rule**: Accept data in whatever reasonable format the user provides, and let the system do the work of formatting it.
**Why**: Forcing users to translate their mental model of data (e.g., a phone number with parentheses) into the system's arbitrary format (e.g., strictly numbers) creates unnecessary friction and errors.
**Check**: Does the system reject common, human-readable formats for standard data types?
**Bad example**: Rejecting a phone number because the user typed `(555) 123-4567` instead of `5551234567`.
**Good example**: Accepting `(555) 123-4567` and having the application strip the formatting behind the scenes before saving.

## Anti-patterns

### The Invisible State
An interactive element lacks hover, active, or focus states. The user clicks it and receives no immediate visual confirmation that the button was pressed, leaving the affordance ambiguous.

### The Punishment UX
When an error occurs, the system provides a generic, unhelpful error code ("Error 500") and clears all the user's work, punishing them for the failure and offering no path to recovery.

### The Empty Void
A screen that has no content simply displays nothing or a sterile "No items found" message. It wastes the crucial first-run opportunity to educate the user on how to use the feature.

### The Sniper Target
Violating Fitts's Law by making a frequently used or primary action incredibly small or placing it in an isolated corner of the screen, forcing the user to carefully "snipe" the target.

### The Modality Trap
Interrupting the user's workflow with a modal dialog for non-critical information (like a marketing popup or a simple confirmation), forcing them to dismiss it before returning to their actual task.

### The Animation Hijack
Using excessive, continuous, or slow animations (like bouncing elements or drawn-out fade-ins) that distract the user's peripheral vision and slow down their ability to consume information.

## Quick Reference
- [ ] Are primary actions large and positioned close to the user's likely cursor/thumb location?
- [ ] Is color blindness accounted for (no red/green-only distinctions)?
- [ ] Is animation used strictly for meaning/feedback rather than decoration?
- [ ] Are options chunked into groups of 4±1?
- [ ] Are all 4 interaction states (normal, hover, pressed, disabled) designed?
- [ ] Do predictable layouts use skeleton screens instead of spinners?
- [ ] Do empty states educate the user and provide a clear call to action?
- [ ] Do error messages explain what went wrong and exactly how to fix it?
- [ ] Are modals reserved strictly for critical, workflow-blocking interruptions?
- [ ] Do destructive actions require explicit, uniquely labeled confirmation buttons?
