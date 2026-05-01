---
name: cognitive-load
description: Governs all decisions regarding cognitive load, usability, affordances, mental models, navigation conventions, error prevention, and user feedback loops. Use this skill whenever the user asks about usability, making things intuitive, reducing friction, navigation patterns, button/link design (affordances), empty states, error handling, or when evaluating how confusing a UI might be. Also trigger for any general UI/UX review task to ensure the design doesn't make the user think unnecessarily.
---

# Cognitive Load

Cognitive load is the amount of mental processing power required to use an interface. A well-designed interface minimizes cognitive load by relying on established conventions, clear affordances, and immediate feedback, allowing users to accomplish their goals without having to stop and think about *how* to use the system.

## Core Principles
1. **Don't make me think.** Every element should be self-evident, obvious, and self-explanatory. Users should never have to ponder whether something is clickable or what a label means.
2. **Design for scanning, not reading.** Users don't read pages; they scan them for keywords that match their current task.
3. **Users satisfice.** Users do not choose the optimal path; they choose the first reasonable path they encounter.
4. **Recognition over recall.** Never force a user to remember information from a previous screen or guess a command; show them the available options.
5. **Bridge the Gulfs.** Minimize the Gulf of Execution (figuring out how to do something) and the Gulf of Evaluation (figuring out if the action succeeded).
6. **Affordances require signifiers.** Just because an element *can* be clicked (affordance) doesn't mean it *looks* clickable (signifier). Always provide clear signifiers.

## Rules

### R1: Ensure Self-Evident Interactivity
**Source**: Don't Make Me Think
**Rule**: Visually differentiate interactive elements (buttons, links, inputs) from static content unequivocally.
**Why**: Users shouldn't have to hover or guess to discover what is clickable; ambiguity wastes mental energy.
**Check**: Can you tell what is clickable and what is static just by glancing at the screen?
**Bad example**: A list of categories where the text is standard black without underlines or button shapes, requiring hover to reveal interactivity.
**Good example**: Text links are underlined and use a distinct blue color; buttons have clear boundaries, background colors, and hover states.

### R2: Adhere to Navigation Conventions
**Source**: Don't Make Me Think
**Rule**: Place standard navigation elements (logo, search, main menu, utility links) in their universally expected locations.
**Why**: Conventions reduce cognitive load because users already know how they work. Breaking them forces users to relearn basic tasks.
**Check**: Are the primary navigation and site logo exactly where the user expects them to be?
**Bad example**: Hiding the main site navigation in a hamburger menu on a desktop layout to make the design look "cleaner".
**Good example**: The logo is in the top left, primary navigation runs horizontally across the top, and utility links (login/cart) are top right.

### R3: The 3-Click Rule (Nuanced)
**Source**: Don't Make Me Think
**Rule**: Prioritize the ease of the clicks (scent of information) over the absolute number of clicks. Three easy, thoughtless clicks are better than one agonizing click.
**Why**: Users don't mind clicking if each click clearly advances them toward their goal and requires no thought.
**Check**: Is the user certain they are moving in the right direction before they click?
**Bad example**: A mega-menu that requires the user to carefully parse 50 options just to reach a page in one click.
**Good example**: A clear, 3-step wizard where each step asks a single, obvious question.

### R4: Design for "Satisficing"
**Source**: Don't Make Me Think
**Rule**: Place the most likely desired option early in the scan path so users can find it without reading every option.
**Why**: Users don't weigh all options to find the best one; they pick the first one that seems "good enough."
**Check**: Is the most common choice positioned so it is seen first?
**Bad example**: An alphabetical list of countries in a dropdown where "United States" is buried at the bottom despite being the choice for 90% of users.
**Good example**: A country dropdown that pins the top 3 most common countries to the very top of the list.

### R5: Format for Scanning
**Source**: Don't Make Me Think
**Rule**: Use visual hierarchies, bold text, bulleted lists, and short paragraphs to make text highly scannable.
**Why**: Users scan pages looking for keywords relevant to their goal; dense text blocks block scanning.
**Check**: Can you extract the main points of the page by only reading the headings and bolded text?
**Bad example**: A feature description is written as a single, dense 400-word paragraph.
**Good example**: The feature is broken down into three bullet points with bolded keywords.

### R6: Omit Needless Words
**Source**: Don't Make Me Think
**Rule**: Cut the word count of instructional text in half, then cut it in half again. Remove "happy talk" entirely.
**Why**: Extraneous text dilutes the useful information, forcing the user to work harder to find what matters.
**Check**: Does every word on the screen directly help the user understand the interface or accomplish their task?
**Bad example**: "Welcome to our platform! We've worked hard to bring you these features. Please click the button below to get started with your account creation process."
**Good example**: "Create your account."

### R7: Progressive Disclosure
**Source**: Don't Make Me Think / The Design of Everyday Things
**Rule**: Reveal advanced or secondary features only when the user specifically requests them.
**Why**: Showing every possible option upfront overwhelms the user and clutters the interface.
**Check**: Are you showing options that less than 20% of users will need by default?
**Bad example**: A search form that shows 15 advanced filtering criteria immediately upon loading.
**Good example**: A search form with a single text input and an "Advanced Filters" toggle that expands the remaining options.

### R8: Recognition Over Recall
**Source**: Don't Make Me Think
**Rule**: Display available options or required formats rather than forcing the user to remember them.
**Why**: The human brain is much better at recognizing something it sees than recalling it from memory.
**Check**: Does the user have to rely on their memory from a previous step to complete this step?
**Bad example**: An API key field that asks the user to type their key without providing a link or dropdown to select existing keys.
**Good example**: An input field that shows a dropdown of recently used API keys for immediate selection.

### R9: Prevent Errors via Constraints
**Source**: The Design of Everyday Things
**Rule**: Design interfaces so that incorrect actions are impossible to make (forcing functions).
**Why**: Preventing an error is infinitely better than providing a great error message after the fact.
**Check**: Is it physically possible for the user to submit invalid data?
**Bad example**: A text input for a date that allows the user to type "February 30th", returning an error upon submission.
**Good example**: A date picker UI that greys out invalid dates, making them unselectable.

### R10: Provide Immediate, Unambiguous Feedback
**Source**: The Design of Everyday Things
**Rule**: Every user action must result in immediate visual or auditory feedback indicating the system received the input.
**Why**: Without feedback, users assume the system is broken or the action failed, often leading them to repeat the action.
**Check**: Does the interface react within 100ms when the user interacts with it?
**Bad example**: Clicking "Submit" on a form does nothing visually until the next page loads 3 seconds later.
**Good example**: Clicking "Submit" immediately disables the button and changes its label to "Saving..." alongside a spinner.

### R11: Clearly Map Controls to Effects
**Source**: The Design of Everyday Things
**Rule**: Ensure the spatial layout of controls directly maps to the spatial arrangement of the elements they affect.
**Why**: Natural mapping leverages physical analogies, making the interface instantly intuitive without instructions.
**Check**: If there are multiple controls, does their layout mirror the layout of the things they control?
**Bad example**: A row of four identical switches to control four different lights, requiring labels to differentiate them.
**Good example**: A volume slider where moving the thumb UP increases the volume and DOWN decreases it.

### R12: Provide Clear Signifiers for Affordances
**Source**: The Design of Everyday Things
**Rule**: Ensure every affordance (what an element *can* do) has a visible signifier (a cue telling the user *how* to do it).
**Why**: An invisible affordance is useless; users must know an action is possible before they can perform it.
**Check**: Does the element visually communicate how it should be interacted with?
**Bad example**: A card component that is entirely clickable but lacks hover states, an arrow icon, or button styling.
**Good example**: A draggable list item that displays a "grip" icon (six dots) to signify it can be grabbed and moved.

### R13: Bridge the Gulf of Execution
**Source**: The Design of Everyday Things
**Rule**: Make the sequence of actions required to achieve a goal explicit and visible.
**Why**: Users struggle when they know their goal but cannot figure out which buttons to press to achieve it.
**Check**: Does the user know exactly what to do next to progress toward their goal?
**Bad example**: A complex settings page where the "Save" button is hidden inside a separate tab.
**Good example**: A multi-step checkout process with a clear progress bar and an obvious "Continue" button at the bottom of each step.

### R14: Bridge the Gulf of Evaluation
**Source**: The Design of Everyday Things
**Rule**: Provide clear, plain-language confirmation that an action successfully achieved the user's intent.
**Why**: Users need to know not just that a button was clicked, but that the *system state* has changed as desired.
**Check**: After taking an action, is the user absolutely certain of the new system state?
**Bad example**: A generic "Success!" toast message after updating a profile, without showing the updated profile.
**Good example**: A toast message saying "Email address updated to user@example.com" and instantly reflecting the change in the UI.

### R15: Match the User's Mental Model
**Source**: The Design of Everyday Things
**Rule**: Design the interface based on how the user *thinks* the system works, not how the underlying engineering actually works.
**Why**: Exposing the system's implementation model confuses users who lack the technical context.
**Check**: Does the interface use terminology and structures that a non-expert understands?
**Bad example**: An error message that says "Database constraint violation: duplicate entry on key 'email_idx'".
**Good example**: An error message that says "An account with this email address already exists."

### R16: Avoid the Illusion of Completeness
**Source**: Don't Make Me Think
**Rule**: Ensure that content extending below the fold provides a visual cue (a "cut-off" element) indicating there is more to see.
**Why**: If a layout perfectly aligns with the bottom edge of the screen, users may assume they've reached the end and stop scrolling.
**Check**: Does the layout clearly look like it continues beyond the visible screen edge?
**Bad example**: A grid of cards where exactly two rows fit perfectly on screen, leaving no partial cards visible at the bottom.
**Good example**: A list where the bottom-most item is cut in half by the edge of the screen, inviting the user to scroll.

### R17: Provide Escape Hatches
**Source**: Don't Make Me Think
**Rule**: Always provide an obvious way to cancel an action, go back, or return to the home page.
**Why**: Users make mistakes and explore dead ends; feeling trapped breeds anxiety and frustration.
**Check**: Can the user easily back out of their current screen or workflow without using the browser's back button?
**Bad example**: A modal dialog that has no "X" button and no "Cancel" button, forcing the user to commit.
**Good example**: A multi-step form that includes a "Cancel" link next to the primary action on every step.

### R18: Prioritize Error Recovery
**Source**: The Design of Everyday Things
**Rule**: When errors do occur, explain exactly what went wrong and provide a direct path to fix it.
**Why**: Blaming the user or providing dead-end errors destroys trust. The cost of an error is the time it takes to recover from it.
**Check**: Does the error message tell the user exactly what to do to fix the problem?
**Bad example**: A form submission fails with the message "Invalid data" and clears all the user's inputs.
**Good example**: The form preserves the user's inputs, highlights the exact field that failed, and explains the required format.

### R19: Make Utilities Distinct
**Source**: Don't Make Me Think
**Rule**: Visually separate utility navigation (login, help, settings) from primary content navigation.
**Why**: Mixing tools with content confuses the user's mental map of the site's structure.
**Check**: Are utility links visually distinct and positioned separately from the main navigational categories?
**Bad example**: Placing "Help" and "Sign Out" in the exact same styling and visual list as "Products" and "Services".
**Good example**: Primary navigation is large and centered, while utility links are smaller and pushed to the top right corner.

### R20: Standardize Link Styling
**Source**: Don't Make Me Think
**Rule**: Apply a single, consistent visual treatment to all text links across the entire interface.
**Why**: Making links look different on different pages breaks the user's learned recognition, forcing them to re-evaluate what is clickable.
**Check**: Are all text links instantly recognizable by a consistent color or underline?
**Bad example**: Using blue links in the sidebar, green links in the footer, and red links in the body text.
**Good example**: Using a specific shade of blue and an underline for all inline text links globally.

## Anti-patterns

### The Memory Tax
Requiring the user to remember information (like an ID number, a setting, or a code) from a previous screen to complete a task on the current screen. It violates "Recognition over Recall".

### The Ghost Affordance
An element that looks interactive (e.g., has a drop shadow, button shape, or is a distinct color) but does nothing when clicked. It trains users to distrust the interface's visual cues.

### The Mystery Meat Navigation
Navigation links that rely entirely on ambiguous icons without text labels, forcing the user to hover over each one or click blindly to figure out what they do.

### The Silent Failure
The user takes an action (like submitting a form or clicking save), but the interface provides no feedback. The user is left wondering if the action succeeded or if the app is broken.

### The Trap Door
A workflow or screen that the user can enter but cannot exit. Missing "Cancel" buttons, broken back buttons, or modals without close controls fall into this category.

### The Implementation Leak
Exposing the underlying database structure, API limits, or system architecture directly to the user through the UI or error messages, rather than translating it into the user's mental model.

## Quick Reference
- [ ] Are all clickable elements instantly recognizable as clickable?
- [ ] Have you eliminated all "happy talk" and needless instructions?
- [ ] Is the primary navigation where the user expects it to be?
- [ ] Can the user easily recover from an error?
- [ ] Does every action provide immediate visual feedback?
- [ ] Are you showing options instead of making the user guess or remember?
- [ ] Is the layout designed for scanning (headings, bullets, bold text)?
- [ ] Are secondary options hidden behind progressive disclosure?
- [ ] Does the UI map to the user's mental model, not the system's?
- [ ] Is there an obvious "escape hatch" on every screen?
