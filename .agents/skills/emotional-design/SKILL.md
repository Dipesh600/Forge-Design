---
name: emotional-design
description: Governs decisions related to user delight, habit-building, onboarding, engagement loops, animations, micro-interactions, and the overall "feel" and personality of a product. Use this skill whenever the user asks about visceral first impressions, making the app feel "premium" or "fun", building engagement and retention (the Hook model), designing ethical variable rewards, or ensuring the product's identity aligns with the user's self-image.
---

# Emotional Design

Human beings are not purely rational decision-makers; we are deeply emotional. Emotional design recognizes that how a product looks and feels is just as important as how it functions. A product that makes a user feel smart, capable, and delighted will build habits and loyalty, whereas a technically functional but sterile product will be abandoned. This skill leverages Don Norman's three levels of cognitive processing (Visceral, Behavioral, Reflective) and Nir Eyal's Hook Model to create engaging, habit-forming, and emotionally resonant interfaces.

## Core Principles

1. **Attractive things work better.** Positive affect (visceral delight) broadens human cognitive processing, making users more creative and more tolerant of minor usability flaws.
2. **Behavioral success builds trust.** Usability is the foundation of emotional design. If a product fails to function smoothly, no amount of aesthetic beauty will save it from user frustration.
3. **Products reflect identity.** Users choose products that align with their self-image. The reflective layer governs the long-term meaning and narrative the user attaches to the product.
4. **Habits require internal triggers.** Sustainable engagement cannot rely on external notifications; it must attach the product's use to an existing negative emotion or need (e.g., boredom, uncertainty).
5. **Investment increases valuation.** The more data, effort, and customization a user puts into a product, the more they overvalue it (the IKEA effect).

## Rules

### R1: The Visceral First Impression
**Source**: *Emotional Design* (Visceral Design)
**Rule**: Ensure the initial visual impression of the interface conveys high quality through harmonious colors, crisp typography, and balanced spacing before the user even interacts with it.
**Why**: The visceral level operates pre-consciously. Users judge a product's credibility and quality within 50 milliseconds based purely on aesthetics.
**Check**: Does the interface look immediately premium and professional at a glance, without requiring the user to read or click anything?
**Bad example**: A dense, gray data table with unaligned text that looks like a legacy database.
**Good example**: A clean, spacious dashboard with vibrant, intentional typography that invites interaction.

### R2: Positive Affect Masking
**Source**: *Emotional Design* (Attractive Things Work Better)
**Rule**: Inject small moments of aesthetic delight (e.g., smooth entrance animations, friendly empty states) to put the user in a positive state of mind.
**Why**: Positive affect makes users more tolerant of friction. If they encounter a minor bug or complex task while in a good mood, they are less likely to abandon the flow.
**Check**: Does the interface include elements designed purely to create a brief moment of joy or satisfaction?
**Bad example**: A sterile, instantaneous state change when an item is deleted.
**Good example**: A satisfying, physics-based "whoosh" animation when an item is successfully archived.

### R3: Behavioral Friction Reduction
**Source**: *Emotional Design* (Behavioral Design)
**Rule**: Ensure that all primary user goals can be accomplished without cognitive strain, ambiguous controls, or physical fatigue.
**Why**: Behavioral design is entirely about use and performance. Frustration here poisons both the visceral and reflective layers.
**Check**: Is the primary action on the screen immediately obvious, reachable, and responsive?
**Bad example**: A checkout button hidden in a hamburger menu.
**Good example**: A persistent, sticky, full-width "Checkout" button at the bottom of the screen.

### R4: The Reflective Identity Match
**Source**: *Emotional Design* (Reflective Design)
**Rule**: Align the product's tone, voice, and visual personality with the self-image the target user wants to project.
**Why**: People use products to signal who they are. A financial app for serious investors must reflect authority; a habit tracker for teens must reflect playfulness.
**Check**: Does the visual style and copy reinforce the user's desired identity?
**Bad example**: Using Comic Sans and confetti animations in an enterprise banking application.
**Good example**: Using elegant serif typography and muted tones in a high-end luxury shopping app.

### R5: Micro-interaction Personality
**Source**: *Emotional Design* (Personality)
**Rule**: Design specific micro-interactions (loaders, toggles, success states) to exhibit a consistent brand personality.
**Why**: Personality makes digital products feel human. Small, thoughtful details have an outsized impact on the user's perception of quality and care.
**Check**: Do the micro-interactions have a distinct "feel" that aligns with the brand's voice?
**Bad example**: Using a default system spinner for loading states in a playful children's app.
**Good example**: Animating the app's mascot running on a wheel during a loading state.

### R6: The Internal Trigger Connection
**Source**: *Hooked* (Triggers)
**Rule**: Map the core action of the application to a specific internal emotional trigger (e.g., feeling bored, uncertain, or lonely) so the user instinctively turns to the app.
**Why**: External triggers (push notifications) are easily ignored or disabled. True habits form when a negative emotion automatically triggers the desire to use the product.
**Check**: Can you clearly identify the specific negative emotion the user feels immediately before opening the app?
**Bad example**: Sending daily "Don't forget to log your water!" push notifications without connecting to a real user need.
**Good example**: Designing a social feed that users instinctively open the moment they feel a pang of boredom in a waiting room.

### R7: Action Simplicity
**Source**: *Hooked* (Action)
**Rule**: Reduce the number of steps required to complete the core habit-forming action to the absolute minimum, removing any unnecessary cognitive or physical effort.
**Why**: According to the Fogg Behavior Model, for an action to occur, motivation and ability must be high. Since motivation fluctuates, ability (simplicity) must always remain extraordinarily high.
**Check**: Can the user complete the core action of the app with zero cognitive effort and minimal clicks?
**Bad example**: Requiring a user to log in, navigate to a tab, click a plus button, and fill out a 5-field form just to log a thought.
**Good example**: Opening directly to a camera view so the user can take a photo in one tap.

### R8: The Variable Reward Schedule
**Source**: *Hooked* (Variable Reward)
**Rule**: Ensure the outcome of the user's action provides a reward that is positive but unpredictable in its exact content, size, or timing.
**Why**: Predictable rewards lose their appeal quickly. The brain's dopamine system is highly stimulated by the *anticipation* of an uncertain reward (the slot machine effect).
**Check**: When the user performs the core action, is there an element of mystery or variability in what they receive back?
**Bad example**: A dashboard that looks exactly the same every time the user opens it.
**Good example**: A social feed where the user pulls to refresh, never knowing exactly what new, interesting content will appear.

### R9: Types of Variable Rewards
**Source**: *Hooked* (Variable Reward)
**Rule**: Categorize and utilize the three types of variable rewards appropriately: Rewards of the Tribe (social validation), Rewards of the Hunt (information/resources), and Rewards of the Self (mastery/completion).
**Why**: Different products require different reward structures. Social apps need Tribe rewards; utility apps need Self rewards.
**Check**: Does the reward type match the user's core motivation?
**Bad example**: Adding a generic leaderboard (Tribe) to a personal journaling app where privacy is paramount.
**Good example**: Using a satisfying, variable animation sequence when checking off a difficult to-do item (Self).

### R10: Ethical Application of Rewards
**Source**: *Hooked* (Variable Reward)
**Rule**: Use variable rewards to help users achieve *their* goals, not solely to extract attention or money against the user's best interests.
**Why**: Deceptive or coercive loops destroy trust at the Reflective level, leading to eventual churn and brand damage.
**Check**: Does the variable reward ultimately serve a goal the user genuinely wants to achieve?
**Bad example**: A hidden "loot box" mechanic that obscures the cost of acquiring a necessary in-app item.
**Good example**: A language learning app that provides variable encouragement and unexpected bonus lessons when the user studies consistently.

### R11: The Minimum Viable Investment
**Source**: *Hooked* (Investment)
**Rule**: Ask the user to perform a small, low-friction action that improves the service for their next visit (e.g., liking a post, saving a preference, adding a friend).
**Why**: The investment phase loads the next trigger and stores value in the product, making it harder to abandon.
**Check**: Does the app ask the user to input something that makes the app better tailored to them tomorrow?
**Bad example**: An app that never asks for preferences, treating the user like a stranger every time they open it.
**Good example**: A music app asking the user to "heart" a song, which immediately improves their personalized playlist.

### R12: The IKEA Effect
**Source**: *Hooked* (Investment)
**Rule**: Allow users to customize their environment or input personal data so they feel a sense of ownership over the product.
**Why**: People place disproportionately high value on things they helped create or configure.
**Check**: Is there a meaningful way for the user to make the interface or the data feel like "theirs"?
**Bad example**: A rigid dashboard with fixed widgets that the user cannot rearrange.
**Good example**: Allowing users to upload a custom avatar and select a workspace color theme during onboarding.

### R13: Loading the Next Trigger
**Source**: *Hooked* (Investment)
**Rule**: Ensure the user's investment sets the stage for an external trigger that will bring them back (e.g., sending a message loads the trigger for a reply notification).
**Why**: A habit loop must be a closed circuit. The investment must naturally lead back to the start of the loop.
**Check**: Does the action the user just took guarantee they will have a reason to return?
**Bad example**: A user completes a task list and the app just says "Done," providing no hook for the next session.
**Good example**: After completing a task, the app asks, "What do you want to tackle tomorrow?" and sets a reminder.

### R14: Tolerating Minor Usability Flaws
**Source**: *Emotional Design* (Visceral Design)
**Rule**: Prioritize fixing severe behavioral blockers, but recognize that users will forgive minor usability friction if the visceral and reflective experience is overwhelmingly positive.
**Why**: A beautiful, emotionally resonant design creates a halo effect that compensates for minor UX quirks.
**Check**: Are you over-optimizing a minor flow at the expense of the product's overall character and delight?
**Bad example**: Stripping out a delightful transition animation because it takes 200ms longer, making the app feel dead.
**Good example**: Keeping a charming, custom-illustrated loading screen even if it slightly delays the perception of performance.

### R15: Beautiful AND Functional
**Source**: *Emotional Design* (Three Levels of Design)
**Rule**: Never sacrifice the Behavioral level for the Visceral level. An interface must be functional first, and beautiful second.
**Why**: Ugly but functional will eventually be replaced by beautiful and functional, but beautiful and broken will be abandoned immediately.
**Check**: Does the aesthetic design interfere with legibility, contrast, or clickability?
**Bad example**: Using a gorgeous, ultra-thin, low-contrast font that users cannot actually read.
**Good example**: Using a highly legible, high-contrast typeface that is also elegantly typeset and visually striking.

### R16: Designing for the Emotional Peak and End
**Source**: *Emotional Design* (Memory and Emotion)
**Rule**: Over-index design effort on the most intense emotional moment of the user journey (the peak) and the final moment (the end).
**Why**: The Peak-End rule dictates that users judge an experience almost entirely on its peak and its conclusion, forgetting the average moments.
**Check**: Is the final screen of the core flow designed to leave a lasting, positive emotional impression?
**Bad example**: A checkout flow that ends with a plain text "Transaction successful" message.
**Good example**: A checkout flow that ends with a celebratory animation and a warm "You're all set! We're packing your order right now." message.

## Anti-patterns

### 1. The Sterile Product
A product that is behaviorally excellent (fast, usable, bug-free) but viscerally and reflectively cold. It has no personality, no moments of delight, and fails to build any emotional connection, making it highly vulnerable to competitors.

### 2. The Deceptive Hook
Using the mechanics of variable reward (like infinite scrolling or unpredictable notifications) to create compulsion and extract attention without providing genuine value in exchange. This creates short-term engagement but long-term reflective regret and churn.

### 3. The Reflective Mismatch
A product whose visceral personality (colors, typography, tone of voice) completely conflicts with the self-image of the target user. For example, a serious B2B enterprise tool that uses juvenile illustrations and overly casual slang.

## Quick Reference

- [ ] Does the UI make an immediately positive visceral impression before interaction?
- [ ] Are there small moments of delight (animations, copy) to create positive affect?
- [ ] Is the primary behavioral action effortless and obvious?
- [ ] Does the visual style align with the user's desired self-image (Reflective layer)?
- [ ] Do micro-interactions exhibit a consistent brand personality?
- [ ] Is the core action mapped to a specific internal emotional trigger?
- [ ] Is the action required to get the reward as simple as possible?
- [ ] Does the product provide a variable, unpredictable reward?
- [ ] Does the reward type (Tribe, Hunt, Self) match the user's underlying motivation?
- [ ] Does the app ask for a small investment (customization, data) to improve future use?
- [ ] Does the user's investment naturally load the next trigger to return?
- [ ] Is the peak moment and the end of the flow emotionally resonant?
