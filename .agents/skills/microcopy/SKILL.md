---
name: microcopy
description: Governs all decisions regarding UI text, labels, buttons, error messages, empty states, onboarding, and conversational tone. Use this skill whenever the user asks about what words to use, UX writing, label design, plain language, accessibility in text, button labels, error messaging, placeholders, or when evaluating the clarity and tone of an interface. Trigger for any review task involving text content or instructional copy.
---

# Microcopy

Microcopy encompasses the small bits of text in user interfaces—labels, buttons, error messages, and hints—that help users navigate, accomplish tasks, and understand context. Words are design decisions; the right copy reduces friction, while the wrong copy can break an otherwise perfect visual design.

## Core Principles
1. **Words are design decisions.** Writing is not a decorative layer applied at the end; it is foundational to the user experience and must be designed alongside the visuals.
2. **The Conversation Model.** Write interface copy as if you are a helpful human speaking to another human. Eliminate robotic, system-centric phrasing.
3. **Voice is constant; tone is variable.** The brand's personality (voice) remains the same, but the emotional delivery (tone) must adapt to the user's current situation (e.g., celebratory vs. apologetic).
4. **Prefer plain language.** Use common, everyday words. Avoid jargon, corporate speak, and overly complex sentence structures.
5. **Design for stress cases.** Assume the user is frustrated, hurried, or confused when reading your copy. If it doesn't work for them then, it fails.

## Rules

### R1: Use the [Verb + Object] Formula for Buttons
**Source**: Strategic Writing for UX
**Rule**: Label action buttons with a strong verb followed by the object of the action.
**Why**: This explicit structure provides a clear "scent of information," telling the user exactly what the button does without requiring surrounding context.
**Check**: Can the user understand what happens when they click the button by reading *only* the button text?
**Bad example**: A button that says "Submit" or "Continue".
**Good example**: A button that says "Save Changes" or "Create Account".

### R2: Never Use "Click Here"
**Source**: Strategic Writing for UX / Writing is Designing
**Rule**: Make the descriptive text itself the link, rather than using generic instructional phrases.
**Why**: "Click here" provides no context for screen readers, forces users to read surrounding text to understand the link, and assumes a mouse interaction (excluding touch users).
**Check**: Does the link text accurately describe the destination on its own?
**Bad example**: "To view our pricing plans, [click here]."
**Good example**: "View our [pricing plans]."

### R3: The Empty State Formula
**Source**: Strategic Writing for UX
**Rule**: When a screen has no data, use this formula: Describe the current state, explain why it's empty, and provide a clear call to action to populate it.
**Why**: Empty states are the prime opportunity to onboard a user. Without guidance, the user hits a dead end.
**Check**: Does the empty state explicitly tell the user what they need to do next?
**Bad example**: "No documents found."
**Good example**: "You haven't uploaded any documents yet. Upload a PDF to start analyzing your data."

### R4: The Error Message Formula
**Source**: Strategic Writing for UX
**Rule**: When an error occurs, state what happened, explain why it happened (if useful to the user), and state exactly what to do next.
**Why**: Errors are stressful. The user needs a clear path to recovery, not just an acknowledgment of failure.
**Check**: Does the message provide a direct, actionable solution to the problem?
**Bad example**: "Authentication error. Code 403."
**Good example**: "We couldn't sign you in because your password has expired. Please click 'Reset Password' to create a new one."

### R5: Never Blame the User
**Source**: Strategic Writing for UX / Writing is Designing
**Rule**: Remove accusatory words ("You failed," "Invalid," "Wrong") from error messages. Frame the issue as the system's inability to understand, not the user's failure to comply.
**Why**: Accusatory language triggers a defensive emotional response, worsening the user's frustration (designing for the stress case).
**Check**: Does the error message imply that the user made a mistake?
**Bad example**: "You entered an invalid email address format."
**Good example**: "We don't recognize that email format. Please use the format name@example.com."

### R6: Progressively Disclose Onboarding Copy
**Source**: Strategic Writing for UX
**Rule**: Show instructional copy only at the moment the user needs it to complete the immediate task.
**Why**: Presenting a wall of instructions upfront overwhelms working memory; users will skip it and immediately get stuck.
**Check**: Can this instructional text be hidden until the user actually interacts with the relevant feature?
**Bad example**: A modal on first login with 5 paragraphs explaining how to use every feature in the dashboard.
**Good example**: A single tooltip pointing to the "Create Project" button on first login, with further instructions appearing only inside the project editor.

### R7: Use Specific Action Labels in Confirmations
**Source**: Strategic Writing for UX
**Rule**: The primary button in a confirmation dialog must explicitly state the destructive action, never just "Yes" or "OK".
**Why**: Users often blindly click confirmation buttons without reading the dialog text (habituation). The button itself must carry the risk.
**Check**: If the user didn't read the dialog title or body, would they still know what they are confirming?
**Bad example**: Title: "Delete account?" Button: "Yes".
**Good example**: Title: "Delete account?" Button: "Delete Account".

### R8: Do Not Use Placeholders as Labels
**Source**: Strategic Writing for UX
**Rule**: Placeholders (inside input fields) must be used strictly for hints or examples, never as a replacement for the input's permanent label.
**Why**: Placeholders disappear as soon as the user starts typing, forcing them to delete their input to remember what the field was for.
**Check**: If the input field contains text, can the user still see the label indicating what the field is?
**Bad example**: An input field with no external label, relying on the placeholder text "Phone Number" inside the box.
**Good example**: A permanent label "Phone Number" above the input field, with the placeholder text "(555) 123-4567" inside the box.

### R9: The Loading State Formula
**Source**: Strategic Writing for UX
**Rule**: For operations taking noticeable time, tell users what the system is currently doing and, if possible, set an expectation for how long it will take.
**Why**: Ambiguity during waiting periods breeds anxiety. Clear communication reassures the user that the system hasn't crashed.
**Check**: Does the loading copy explain the specific action currently being processed?
**Bad example**: "Loading..."
**Good example**: "Generating your report. This usually takes about 30 seconds."

### R10: The Success Message Formula
**Source**: Strategic Writing for UX
**Rule**: After a successful action, confirm exactly what was accomplished AND what will (or should) happen next.
**Why**: Users need to know that their goal was met, but they also need guidance on where to direct their attention next to continue their workflow.
**Check**: Does the success message transition the user to the next logical step?
**Bad example**: "Success!"
**Good example**: "Your profile has been updated. Return to the dashboard to see your changes."

### R11: Write in the Active Voice
**Source**: Writing is Designing
**Rule**: Structure sentences so the subject performs the action (Active), rather than the action being performed upon the subject (Passive).
**Why**: Active voice is more direct, easier to parse, and typically requires fewer words, reducing cognitive load.
**Check**: Is the "actor" of the sentence explicitly stated before the verb?
**Bad example**: "Your application has been received by our team." (Passive)
**Good example**: "Our team received your application." (Active)

### R12: Adapt Tone to the User's Emotional State
**Source**: Writing is Designing
**Rule**: Adjust the enthusiasm, formality, and directness of the copy based on the user's likely emotional state at that moment in the journey.
**Why**: A celebratory tone is great for completing a milestone, but deeply annoying when applied to a payment failure.
**Check**: Does the tone of this message match how the user feels right now?
**Bad example**: "Oopsie! Looks like your payment completely failed! Try again!" (Inappropriate celebratory/casual tone for a stressful event).
**Good example**: "We couldn't process your payment. Please check your card details and try again." (Direct, helpful, neutral).

### R13: Remove Ability Assumptions (Inclusivity)
**Source**: Writing is Designing
**Rule**: Avoid language that assumes a specific physical ability, sensory capability, or input device.
**Why**: Directing users to "see the red text" or "listen to the chime" excludes users with visual or auditory impairments.
**Check**: Does the instruction make sense regardless of how the user is perceiving the interface?
**Bad example**: "Click the red button below to continue."
**Good example**: "Select the 'Continue' button to proceed."

### R14: Remove Gendered Language (Inclusivity)
**Source**: Writing is Designing
**Rule**: Use gender-neutral terms unless referring to a specific person whose pronouns are known.
**Why**: Unnecessary gendered language can alienate users and make interfaces feel outdated or exclusionary.
**Check**: Could this copy be rewritten to avoid gendered pronouns without losing clarity?
**Bad example**: "A good manager always supports his team."
**Good example**: "A good manager always supports their team."

### R15: Start with the Goal (Front-loading)
**Source**: Writing is Designing
**Rule**: When providing instructions, state the goal first, then the action required to achieve it.
**Why**: Users scan for their goal. If the action comes first, they may stop reading before realizing the action applies to what they want to do.
**Check**: Is the outcome mentioned before the required action?
**Bad example**: "Click on 'Settings' and then navigate to 'Security' to change your password."
**Good example**: "To change your password, go to Settings > Security."

### R16: Avoid Capitalization for Emphasis
**Source**: Writing is Designing
**Rule**: Do not use ALL CAPS to emphasize words; use bolding instead. Reserve ALL CAPS strictly for acronyms.
**Why**: ALL CAPS is visually harder to read because the letters form a uniform block without ascenders or descenders, and it visually implies shouting.
**Check**: Is capitalization being used to denote importance rather than an acronym?
**Bad example**: "You MUST save your work before exiting."
**Good example**: "You **must** save your work before exiting."

### R17: Avoid System-Centric Terminology
**Source**: Writing is Designing
**Rule**: Translate database terms, API jargon, and internal company phrasing into the user's vocabulary.
**Why**: Exposing the system's implementation details forces the user to learn your architecture to use your product.
**Check**: Would an average user use this term in everyday conversation?
**Bad example**: "Query returned 0 results. Check your parameter syntax."
**Good example**: "We couldn't find any matches for your search. Try using different keywords."

### R18: Keep Sentences Short
**Source**: Writing is Designing
**Rule**: Limit sentences to one primary idea. Break complex sentences into shorter ones or use bulleted lists.
**Why**: Long sentences increase cognitive load and hurt scannability. Short sentences are easier to translate and faster to read.
**Check**: Can this sentence be broken into two distinct sentences?
**Bad example**: "If you want to upgrade your plan, you must first ensure that your billing information is up to date and then navigate to the subscription panel where you can select the new tier."
**Good example**: "To upgrade your plan, first update your billing information. Then, select a new tier in the subscription panel."

## Anti-patterns

### The Vague Button
A button label that describes the mechanical action ("Submit", "OK", "Next") rather than the user's intent or the outcome of the action ("Create Account", "Delete Post").

### The Accusatory Error
An error message that uses words like "Invalid", "Wrong", or "Failed" to blame the user for not perfectly adhering to the system's rigid requirements.

### The Mysterious Placeholder
Using placeholder text inside an input field as the only label. When the user starts typing, the label vanishes, leaving them to rely on short-term memory to remember what the field is for.

### The Robot Voice
Copy that reads like it was generated by a database log rather than written by a human. It uses passive voice, system jargon, and lacks any conversational empathy.

### The "Click Here" Trap
Links labeled simply "Click here" or "Read more". They provide terrible "scent of information", ruin scannability, and are completely useless for screen-reader users navigating by links.

## Quick Reference
- [ ] Are buttons labeled with the [Verb + Object] formula?
- [ ] Are there zero instances of "Click here" or "OK"?
- [ ] Does the empty state explain why it's empty and what to do?
- [ ] Do error messages explain what went wrong and how to fix it without blaming the user?
- [ ] Are labels permanent and external to input fields (not just placeholders)?
- [ ] Is the copy written in active voice with short, scannable sentences?
- [ ] Does the tone appropriately match the user's emotional state in this exact moment?
- [ ] Is the copy completely free of internal system jargon?
- [ ] Are goal-oriented instructions front-loaded (Goal -> Action)?
