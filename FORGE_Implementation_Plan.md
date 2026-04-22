**FORGE**

Agentic Mobile Design Intelligence

**Complete Implementation Plan**

  -----------------------------------------------------------------------

  -----------------------------------------------------------------------

Stack: Kotlin · XML · MiniMax AI · Stitch MCP · Firebase

Architecture · Agents · Skills · Phases · Code · Timelines

**1. Executive Summary**

FORGE is an agentic Android application that generates, edits, and
regenerates production-quality mobile UI screens. Unlike every existing
AI design tool --- Stitch, Galileo AI, Uizard, Visily --- FORGE does not
generate by pattern-matching. It generates by reasoning, using a library
of skills extracted from canonical design books, orchestrated by a
multi-agent brain powered by MiniMax AI, with Stitch MCP as its
rendering engine.

**What makes FORGE different**

> **✦ Key point:** The output quality ceiling of Stitch is defined by
> what Stitch has seen. The quality ceiling of FORGE is defined by the
> principles in the books we encode into skills. These are fundamentally
> different ceilings.

  ------------------------------------------------------------------------
  **Capability**   **Stitch / Galileo /        **FORGE**
                   Uizard**                    
  ---------------- --------------------------- ---------------------------
  Generation       One pass. No revision loop. Multi-agent: generate →
                                               critique → score → revise.
                                               2--3 loops before user sees
                                               output.

  Principles       Implicit (pattern-matched   Explicit rules extracted
                   from training data).        from 9 canonical books,
                                               versioned in Firestore.

  Explainability   None. Output appears        Every decision annotated
                   without reasoning.          with the principle and book
                                               it came from.

  Cross-screen     No memory between screens.  Design DNA: every screen
                                               checks coherence with all
                                               previous screens.

  User adaptation  None.                       Memory Keeper builds a
                                               style profile. Output
                                               adapts to your taste over
                                               time.

  Motion           Static XML only.            Motion Director applies
                                               spring physics, easing,
                                               choreography timing.

  Accessibility    No audit.                   Dedicated Accessibility
                                               Auditor: WCAG 2.2,
                                               contrast, touch targets,
                                               reading order.

  Copy             Placeholder text.           Micro-copy Skill writes
                                               button labels, empty
                                               states, error messages from
                                               principles.
  ------------------------------------------------------------------------

**2. Technology Stack & Repository Structure**

**2.1 Full stack decision rationale**

  -------------------------------------------------------------------------
  **Layer**       **Technology**      **Why this, not the alternative**
  --------------- ------------------- -------------------------------------
  UI framework    Kotlin + XML        XML gives direct control over every
                  layouts             attribute, essential when the Code
                                      Surgeon agent needs surgical edits.
                                      Compose is harder to diff and patch
                                      programmatically.

  AI brain        MiniMax AI          1M token context window. Holds full
                  (MiniMax-Text-01)   conversation history + all activated
                                      skill definitions + design DNA
                                      simultaneously. GPT-4o and Gemini Pro
                                      max out at 128k--200k ---
                                      insufficient for multi-skill
                                      orchestration.

  Screen          Stitch MCP          Only tool that outputs production
  generation                          Android XML layouts, not Figma
                                      frames. Direct path from generation
                                      to running code.

  Auth + DB       Firebase Auth +     Real-time sync for multi-device;
                  Firestore           offline support; structured skill
                                      storage with versioning; Firebase
                                      Functions for server-side MCP calls.

  Local storage   Room Database +     Room for skill state, conversation
                  DataStore           history, design DNA. DataStore for
                                      user preferences and style profile.

  Skill ingestion Python pipeline     PDF parsing with pdfplumber,
                  (offline)           chunking, structuring. Runs offline
                                      to build the skill library; output
                                      stored in Firestore.

  Vector search   Firebase + Vertex   Semantic skill retrieval: Brain finds
                  AI Embeddings       the right skills for any user intent
                                      without exact-match keywords.
  -------------------------------------------------------------------------

**2.2 Repository structure**

+-----------------------------------------------------------------------+
| forge/                                                                |
|                                                                       |
| ├── app/                                                              |
|                                                                       |
| │ ├── src/main/                                                       |
|                                                                       |
| │ │ ├── java/com/forge/                                               |
|                                                                       |
| │ │ │ ├── agents/ ← all agent classes                                 |
|                                                                       |
| │ │ │ │ ├── DirectorAgent.kt                                          |
|                                                                       |
| │ │ │ │ ├── CriticAgent.kt                                            |
|                                                                       |
| │ │ │ │ ├── MemoryKeeperAgent.kt                                      |
|                                                                       |
| │ │ │ │ ├── IntentArchitectAgent.kt                                   |
|                                                                       |
| │ │ │ │ ├── ScreenComposerAgent.kt                                    |
|                                                                       |
| │ │ │ │ ├── VariantExplorerAgent.kt                                   |
|                                                                       |
| │ │ │ │ ├── AccessibilityAuditorAgent.kt                              |
|                                                                       |
| │ │ │ │ ├── MotionDirectorAgent.kt                                    |
|                                                                       |
| │ │ │ │ └── CodeSurgeonAgent.kt                                       |
|                                                                       |
| │ │ │ ├── skills/ ← skill loader + runtime                            |
|                                                                       |
| │ │ │ │ ├── SkillRepository.kt                                        |
|                                                                       |
| │ │ │ │ ├── SkillRouter.kt                                            |
|                                                                       |
| │ │ │ │ └── models/Skill.kt                                           |
|                                                                       |
| │ │ │ ├── brain/ ← MiniMax AI integration                             |
|                                                                       |
| │ │ │ │ ├── MiniMaxClient.kt                                          |
|                                                                       |
| │ │ │ │ ├── BrainOrchestrator.kt                                      |
|                                                                       |
| │ │ │ │ └── ConversationManager.kt                                    |
|                                                                       |
| │ │ │ ├── mcp/ ← Stitch MCP client                                    |
|                                                                       |
| │ │ │ │ ├── StitchMcpClient.kt                                        |
|                                                                       |
| │ │ │ │ └── McpToolExecutor.kt                                        |
|                                                                       |
| │ │ │ ├── memory/ ← design DNA + style profile                        |
|                                                                       |
| │ │ │ │ ├── DesignDna.kt                                              |
|                                                                       |
| │ │ │ │ ├── StyleProfile.kt                                           |
|                                                                       |
| │ │ │ │ └── WorkingMemory.kt                                          |
|                                                                       |
| │ │ │ ├── ui/ ← screens                                               |
|                                                                       |
| │ │ │ │ ├── chat/                                                     |
|                                                                       |
| │ │ │ │ ├── canvas/                                                   |
|                                                                       |
| │ │ │ │ ├── skills/                                                   |
|                                                                       |
| │ │ │ │ └── settings/                                                 |
|                                                                       |
| │ │ │ └── data/ ← Room DB, Firestore repos                            |
|                                                                       |
| │ │ └── res/                                                          |
|                                                                       |
| │ │ ├── layout/ ← app UI layouts                                      |
|                                                                       |
| │ │ └── values/                                                       |
|                                                                       |
| │ └── build.gradle.kts                                                |
|                                                                       |
| ├── ingestion/ ← offline Python skill pipeline                        |
|                                                                       |
| │ ├── parse_book.py                                                   |
|                                                                       |
| │ ├── structure_skill.py                                              |
|                                                                       |
| │ ├── embed_and_index.py                                              |
|                                                                       |
| │ └── deploy_skill.py                                                 |
|                                                                       |
| ├── functions/ ← Firebase Cloud Functions                             |
|                                                                       |
| │ ├── minimax_proxy.js                                                |
|                                                                       |
| │ └── stitch_mcp_proxy.js                                             |
|                                                                       |
| └── firebase.json                                                     |
+-----------------------------------------------------------------------+

**3. Agent Design**

FORGE has nine agents in three tiers. Every agent is a Kotlin class that
constructs a structured prompt, calls MiniMax AI, parses the response,
and returns a typed result. Agents do not call each other directly ---
they communicate through the BrainOrchestrator, which manages
sequencing, shared state, and retry logic.

**3.1 Tier A --- Meta-agents (orchestration)**

+--------------+-------------------------------------------------------+
| **Director** | Receives the user\'s raw goal. Decomposes it into an  |
|              | ordered task graph --- nodes are tasks, edges are     |
|              | dependencies. Assigns the right agents and skills to  |
|              | each node.                                            |
|              |                                                       |
|              | **In:** User message + conversation history + design  |
|              | DNA                                                   |
|              |                                                       |
|              | **Out:** TaskGraph: JSON with nodes, edges, assigned  |
|              | agents, skill IDs                                     |
|              |                                                       |
|              | **Trigger:** Every new user request                   |
+--------------+-------------------------------------------------------+

+--------------+-------------------------------------------------------+
| **Critic**   | Receives any agent\'s output and a list of applicable |
|              | skill rule IDs. Scores the output against each rule   |
|              | (0--10). Returns pass/fail, scores, violated rules,   |
|              | and a suggested fix for each violation.               |
|              |                                                       |
|              | **In:** Agent output (XML or text) + skill rule IDs   |
|              |                                                       |
|              | **Out:** CriticReport: scores\[\], violations\[\],    |
|              | fixes\[\], overall_score                              |
|              |                                                       |
|              | **Trigger:** After every ScreenComposer or            |
|              | MotionDirector call                                   |
+--------------+-------------------------------------------------------+

+--------------+-------------------------------------------------------+
| **Memory     | Maintains three stores: working memory (current       |
| Keeper**     | session context), style profile (user\'s design       |
|              | preferences inferred over time), and design DNA (the  |
|              | established visual language of the current project).  |
|              |                                                       |
|              | **In:** Every agent output + user feedback signals    |
|              |                                                       |
|              | **Out:** Updated WorkingMemory, StyleProfile,         |
|              | DesignDna objects                                     |
|              |                                                       |
|              | **Trigger:** After every interaction                  |
+--------------+-------------------------------------------------------+

**3.2 Tier B --- Specialized agents**

+--------------+-------------------------------------------------------+
| **Intent     | Transforms vague user input into a precise design     |
| Architect**  | brief. Asks up to 2 clarifying questions if needed.   |
|              | Outputs a structured brief: screen type, user goal,   |
|              | constraints, mood, platform context.                  |
|              |                                                       |
|              | **In:** Raw user message                              |
|              |                                                       |
|              | **Out:** DesignBrief: screen_type, user_goal,         |
|              | constraints\[\], mood, platform                       |
|              |                                                       |
|              | **Trigger:** Start of every generation flow           |
+--------------+-------------------------------------------------------+

+--------------+-------------------------------------------------------+
| **Screen     | Core generation agent. Takes the DesignBrief +        |
| Composer**   | activated skills + design DNA. Constructs a detailed  |
|              | generation prompt. Calls Stitch MCP. Receives raw     |
|              | XML. Post-processes it --- applies skill-derived      |
|              | improvements to the raw output.                       |
|              |                                                       |
|              | **In:** DesignBrief + Skill\[\] + DesignDna           |
|              |                                                       |
|              | **Out:** Annotated XML layout + DesignReasoning\[\]   |
|              |                                                       |
|              | **Trigger:** After IntentArchitect completes          |
+--------------+-------------------------------------------------------+

+--------------+-------------------------------------------------------+
| **Variant    | Generates 3 directional variations from the same      |
| Explorer**   | brief. Each variation explores a different design     |
|              | philosophy: minimal, expressive, structured. Outputs  |
|              | all three with a one-paragraph trade-off explanation  |
|              | for each.                                             |
|              |                                                       |
|              | **In:** DesignBrief + ActivatedSkills\[\]             |
|              |                                                       |
|              | **Out:** VariantSet: Variant\[\] each with xml,       |
|              | philosophy, tradeoffs                                 |
|              |                                                       |
|              | **Trigger:** When user requests alternatives          |
+--------------+-------------------------------------------------------+

+--------------+-------------------------------------------------------+
| **A          | Parses the generated XML. Checks: contrast ratio      |
| ccessibility | (WCAG AA/AAA), touch target size (48dp minimum),      |
| Auditor**    | reading order, focus order, content descriptions on   |
|              | all interactive elements, text scaling support.       |
|              |                                                       |
|              | **In:** XML layout + resource values                  |
|              |                                                       |
|              | **Out:** AuditReport: issues\[\], severity\[\],       |
|              | auto_fixes\[\]                                        |
|              |                                                       |
|              | **Trigger:** After every screen generation            |
+--------------+-------------------------------------------------------+

+--------------+-------------------------------------------------------+
| **Motion     | Reads the generated XML. Identifies transition points |
| Director**   | (screen entry, element reveal, state changes,         |
|              | micro-interactions). Writes the corresponding         |
|              | Animator XML and MotionLayout constraints.            |
|              |                                                       |
|              | **In:** Static XML layout                             |
|              |                                                       |
|              | **Out:** MotionSpec: animator_xmls\[\],               |
|              | motion_layout_constraints, timing_ms                  |
|              |                                                       |
|              | **Trigger:** When user enables motion layer           |
+--------------+-------------------------------------------------------+

+--------------+-------------------------------------------------------+
| **Code       | Performs precise edits to existing XML layouts. Never |
| Surgeon**    | rewrites the whole file. Identifies the minimum edit  |
|              | required, makes it, validates the result, and returns |
|              | a diff.                                               |
|              |                                                       |
|              | **In:** Existing XML + edit instruction               |
|              |                                                       |
|              | **Out:** EditResult: diff, changed_lines\[\],         |
|              | validation_pass                                       |
|              |                                                       |
|              | **Trigger:** When user says \'change X in the         |
|              | existing screen\'                                     |
+--------------+-------------------------------------------------------+

**3.3 BrainOrchestrator --- the conductor**

The BrainOrchestrator is not an AI agent. It is a Kotlin coroutine-based
state machine that sequences agents, manages the retry loop with the
Critic, and surfaces the result to the UI layer.

+-----------------------------------------------------------------------+
| // BrainOrchestrator.kt --- core loop                                 |
|                                                                       |
| suspend fun handle(userMessage: String): ForgeResult {                |
|                                                                       |
| val brief = intentArchitect.run(userMessage, workingMemory)           |
|                                                                       |
| val skills = skillRouter.findRelevant(brief)                          |
|                                                                       |
| val dna = memoryKeeper.getDesignDna()                                 |
|                                                                       |
| var xml = screenComposer.run(brief, skills, dna)                      |
|                                                                       |
| var loops = 0                                                         |
|                                                                       |
| while (loops \< MAX_REVISION_LOOPS) {                                 |
|                                                                       |
| val report = critic.evaluate(xml, skills)                             |
|                                                                       |
| if (report.overallScore \>= PASS_THRESHOLD) break                     |
|                                                                       |
| xml = screenComposer.revise(xml, report)                              |
|                                                                       |
| loops++                                                               |
|                                                                       |
| }                                                                     |
|                                                                       |
| val auditReport = accessibilityAuditor.run(xml)                       |
|                                                                       |
| memoryKeeper.update(xml, brief, skills)                               |
|                                                                       |
| return ForgeResult(xml, report, auditReport)                          |
|                                                                       |
| }                                                                     |
+-----------------------------------------------------------------------+

**4. Skill Library**

Each skill is a JSON document stored in Firestore. It has a name, source
book, a set of atomic rules (each independently evaluable), scored
examples, and a failure taxonomy. Skills are versioned. The SkillRouter
retrieves relevant skills using semantic vector search against the
user\'s design brief.

**4.1 Skill JSON schema**

+-----------------------------------------------------------------------+
| {                                                                     |
|                                                                       |
| \"id\": \"visual-hierarchy-v1\",                                      |
|                                                                       |
| \"name\": \"Visual Hierarchy\",                                       |
|                                                                       |
| \"version\": 1,                                                       |
|                                                                       |
| \"source_books\": \[\"Refactoring UI\", \"Universal Principles of     |
| Design (Lidwell)\"\],                                                 |
|                                                                       |
| \"embedding_vector\": \[\...\], // generated at ingestion time        |
|                                                                       |
| \"rules\": \[                                                         |
|                                                                       |
| {                                                                     |
|                                                                       |
| \"id\": \"vh-001\",                                                   |
|                                                                       |
| \"rule\": \"No more than 3 font sizes on a single screen.\",          |
|                                                                       |
| \"severity\": \"error\",                                              |
|                                                                       |
| \"check_type\": \"structural\",                                       |
|                                                                       |
| \"pass_example\": \"Body 16sp, heading 24sp, caption 12sp.\",         |
|                                                                       |
| \"fail_example\": \"Body 16sp, subheading 18sp, card-title 20sp,      |
| heading 28sp, hero 40sp.\"                                            |
|                                                                       |
| },                                                                    |
|                                                                       |
| {                                                                     |
|                                                                       |
| \"id\": \"vh-002\",                                                   |
|                                                                       |
| \"rule\": \"The primary action on every screen must have the          |
| strongest visual weight: largest size, highest contrast, most         |
| saturated colour.\",                                                  |
|                                                                       |
| \"severity\": \"error\",                                              |
|                                                                       |
| \"check_type\": \"semantic\",                                         |
|                                                                       |
| \"pass_example\": \"CTA button: filled, brand colour, 16sp bold.      |
| Secondary: outlined, neutral.\",                                      |
|                                                                       |
| \"fail_example\": \"All buttons same weight and colour.\"             |
|                                                                       |
| },                                                                    |
|                                                                       |
| {                                                                     |
|                                                                       |
| \"id\": \"vh-003\",                                                   |
|                                                                       |
| \"rule\": \"Use whitespace as a signal, not a filler. Group related   |
| elements (8dp gap), separate sections (32dp gap).\",                  |
|                                                                       |
| \"severity\": \"warning\",                                            |
|                                                                       |
| \"check_type\": \"structural\"                                        |
|                                                                       |
| }                                                                     |
|                                                                       |
| \],                                                                   |
|                                                                       |
| \"failure_taxonomy\": {                                               |
|                                                                       |
| \"attention_scatter\": \"Multiple elements competing for primary      |
| attention.\",                                                         |
|                                                                       |
| \"weight_inversion\": \"Secondary element visually heavier than       |
| primary.\",                                                           |
|                                                                       |
| \"orphaned_content\": \"Content unrelated to surrounding elements, no |
| grouping signal.\"                                                    |
|                                                                       |
| }                                                                     |
|                                                                       |
| }                                                                     |
+-----------------------------------------------------------------------+

**4.2 All 9 skills --- source books and top rules**

+-----------------------------------------------------------------------+
| **1. Visual Hierarchy**                                               |
+-----------------------------------------------------------------------+
| **Source: Refactoring UI (Wathan & Schoger) + Universal Principles of |
| Design (Lidwell)**                                                    |
|                                                                       |
| -   Max 3 font sizes per screen. Every additional size adds cognitive |
|     load.                                                             |
|                                                                       |
| -   Primary action must have the highest visual weight on screen ---  |
|     size, contrast, saturation.                                       |
|                                                                       |
| -   Use whitespace as a signal: 8dp between related elements, 32dp    |
|     between sections.                                                 |
|                                                                       |
| -   Never use colour as the only differentiator between UI states.    |
|                                                                       |
| -   The human eye travels F-pattern (top-left to bottom-right).       |
|     Critical info goes top-left.                                      |
+-----------------------------------------------------------------------+

+-----------------------------------------------------------------------+
| **2. Typography System**                                              |
+-----------------------------------------------------------------------+
| **Source: The Elements of Typographic Style (Bringhurst) + Practical  |
| Typography (Butterick)**                                              |
|                                                                       |
| -   Line length: 45--75 characters per line. Beyond 75, the eye loses |
|     its place returning.                                              |
|                                                                       |
| -   Line height: 1.4--1.6× the font size for body text. Tighter for   |
|     headings.                                                         |
|                                                                       |
| -   Never use more than 2 typefaces in a single interface.            |
|                                                                       |
| -   Optical sizing: small text needs more letter-spacing. Large text  |
|     (headings) needs less.                                            |
|                                                                       |
| -   Use typographic contrast --- size, weight, style --- not colour   |
|     alone.                                                            |
+-----------------------------------------------------------------------+

+-----------------------------------------------------------------------+
| **3. Cognitive Load**                                                 |
+-----------------------------------------------------------------------+
| **Source: Don\'t Make Me Think (Krug) + The Design of Everyday Things |
| (Norman)**                                                            |
|                                                                       |
| -   The 3-click rule: any goal should be reachable in 3 interactions  |
|     or fewer.                                                         |
|                                                                       |
| -   Recognition over recall: show options, don\'t make users remember |
|     them.                                                             |
|                                                                       |
| -   Progressive disclosure: reveal complexity only when the user      |
|     needs it.                                                         |
|                                                                       |
| -   Error prevention before error recovery: design to make mistakes   |
|     impossible, not just recoverable.                                 |
|                                                                       |
| -   Miller\'s Law: 7±2 items in any list or menu. Split anything      |
|     larger.                                                           |
+-----------------------------------------------------------------------+

+-----------------------------------------------------------------------+
| **4. Layout Grammar**                                                 |
+-----------------------------------------------------------------------+
| **Source: Grid Systems in Graphic Design (Müller-Brockmann)**         |
|                                                                       |
| -   Every element must belong to a column. No freeform placement.     |
|                                                                       |
| -   Use a base unit (8dp on Android). All spacing is a multiple of 8. |
|                                                                       |
| -   Alignment is a promise to the user. Break it only with strong     |
|     intent.                                                           |
|                                                                       |
| -   Columns create rhythm. Rhythm creates trust. Trust precedes       |
|     engagement.                                                       |
|                                                                       |
| -   Breathing room is not empty space --- it is negative space that   |
|     frames the content.                                               |
+-----------------------------------------------------------------------+

+-----------------------------------------------------------------------+
| **5. Colour Intelligence**                                            |
+-----------------------------------------------------------------------+
| **Source: Interaction of Color (Albers) + Refactoring UI colour       |
| chapter**                                                             |
|                                                                       |
| -   Saturation communicates status: saturated = active/primary, muted |
|     = secondary/disabled.                                             |
|                                                                       |
| -   Contrast ratio: 4.5:1 minimum for body text (WCAG AA), 3:1 for    |
|     large text.                                                       |
|                                                                       |
| -   Simultaneous contrast: the same colour looks different on         |
|     different backgrounds. Always test in context.                    |
|                                                                       |
| -   Build a colour system (primary, secondary, neutrals, semantic)    |
|     --- never pick colours one-off.                                   |
|                                                                       |
| -   Dark mode is not just colour inversion. Elevation is expressed    |
|     through lighter surfaces, not shadows.                            |
+-----------------------------------------------------------------------+

+-----------------------------------------------------------------------+
| **6. Emotional Design**                                               |
+-----------------------------------------------------------------------+
| **Source: Emotional Design (Norman) + Hooked (Nir Eyal)**             |
|                                                                       |
| -   Visceral layer: first impression in 50ms. Shape, colour, and      |
|     motion carry it.                                                  |
|                                                                       |
| -   Behavioural layer: usability and fluency. Friction here breaks    |
|     trust.                                                            |
|                                                                       |
| -   Reflective layer: meaning and identity. What does using this app  |
|     say about the user?                                               |
|                                                                       |
| -   Delight moments: small surprises --- micro-animations,            |
|     personality in copy --- build loyalty.                            |
|                                                                       |
| -   Variable reward drives habit. Design the right amount of          |
|     unpredictability into engagement patterns.                        |
+-----------------------------------------------------------------------+

+-----------------------------------------------------------------------+
| **7. Interaction Patterns**                                           |
+-----------------------------------------------------------------------+
| **Source: Designing with the Mind in Mind (Johnson) + About Face      |
| (Cooper)**                                                            |
|                                                                       |
| -   Affordances must be visible: a button must look tappable before   |
|     it is tapped.                                                     |
|                                                                       |
| -   Feedback must be immediate (\< 100ms) and proportional to the     |
|     interaction weight.                                               |
|                                                                       |
| -   Consistent mapping: same gesture always does the same thing       |
|     across the app.                                                   |
|                                                                       |
| -   Provide an escape: every action must be undoable or at minimum    |
|     confirmable.                                                      |
|                                                                       |
| -   States must be explicit: loading, empty, error, and success each  |
|     need a designed state.                                            |
+-----------------------------------------------------------------------+

+-----------------------------------------------------------------------+
| **8. Micro-copy**                                                     |
+-----------------------------------------------------------------------+
| **Source: Strategic Writing for UX (Podmajersky) + Writing is         |
| Designing (Metts & Welfle)**                                          |
|                                                                       |
| -   Button labels are verbs: \'Save changes\', not \'OK\'. \'Delete   |
|     account\', not \'Confirm\'.                                       |
|                                                                       |
| -   Error messages say what happened, why, and what to do. Never just |
|     \'Error\'.                                                        |
|                                                                       |
| -   Empty states are opportunities: explain what will be here and how |
|     to get started.                                                   |
|                                                                       |
| -   Avoid jargon: write at a 6th-grade reading level for broad        |
|     audiences.                                                        |
|                                                                       |
| -   Voice and tone must be consistent: define 3 adjectives for your   |
|     brand voice and apply them.                                       |
+-----------------------------------------------------------------------+

+-----------------------------------------------------------------------+
| **9. Android Platform Fluency**                                       |
+-----------------------------------------------------------------------+
| **Source: Material Design 3 Specification + Android Developer         |
| Guidelines**                                                          |
|                                                                       |
| -   Edge-to-edge by default: draw behind system bars, apply           |
|     WindowInsets.                                                     |
|                                                                       |
| -   Touch target minimum: 48×48dp for all interactive elements (WCAG  |
|     2.5.5).                                                           |
|                                                                       |
| -   Use Material 3 components: do not reinvent chips, dialogs, FABs   |
|     --- extend them.                                                  |
|                                                                       |
| -   Dynamic Color (Material You): support system-generated palettes   |
|     via MaterialTheme.                                                |
|                                                                       |
| -   Navigation: back gesture = back stack. Predictive back animation  |
|     is required from API 34+.                                         |
+-----------------------------------------------------------------------+

**5. Implementation Phases**

The build is structured in 5 phases across 20 weeks. Each phase ends
with a testable deliverable. Never start Phase N+1 until Phase N\'s
Definition of Done is fully met.

  -------- ----------------------------------------------- ----------------
  **P1**   **Foundation --- Android shell + MiniMax        Weeks 1--3
           round-trip**                                    

  -------- ----------------------------------------------- ----------------

Goal: A working Android app that sends a message to MiniMax AI and
displays the response. This is the skeleton everything else attaches to.

**Deliverables**

  --------------------------------------------------------------------------------
  **Task**                    **What to build**      **Complexity**   **Output**
  --------------------------- ---------------------- ---------------- ------------
  Firebase project setup      Auth (email + Google), Low              Firebase
                              Firestore with                          console live
                              security rules,                         
                              Storage bucket,                         
                              Functions stub                          

  Android project init        Kotlin DSL build       Low              App builds
                              files, Material 3                       and runs
                              theme, ViewBinding                      
                              enabled, Hilt                           
                              dependency injection                    

  MiniMaxClient.kt            Retrofit client        Medium           Unit test
                              targeting MiniMax API.                  passes
                              Auth via Firebase                       
                              Function proxy (never                   
                              expose key                              
                              client-side)                            

  ChatViewModel.kt            StateFlow-based. Sends Medium           UI shows AI
                              message → gets                          response
                              response → emits                        
                              state. Handles                          
                              loading + error                         
                              states.                                 

  Chat UI (XML)               RecyclerView with      Medium           Functional
                              DiffUtil. User bubble                   chat screen
                              (right), AI bubble                      
                              (left). Typing                          
                              indicator. Input bar.                   

  ConversationManager.kt      Maintains message      Medium           History
                              history. Truncates                      survives
                              context at 80% of                       rotation
                              MiniMax token limit.                    
                              Persists to Room.                       
  --------------------------------------------------------------------------------

> **✦ Key point:** The MiniMax API key must NEVER be in the Android app.
> Route all calls through a Firebase Cloud Function that holds the key
> in environment config. This is non-negotiable for security.

+-----------------------------------------------------------------------+
| // Firebase Function: minimax_proxy.js                                |
|                                                                       |
| exports.minimaxProxy = functions.https.onCall(async (data, context)   |
| =\> {                                                                 |
|                                                                       |
| if (!context.auth) throw new                                          |
| functions.https.HttpsError(\'unauthenticated\');                      |
|                                                                       |
| const response = await                                                |
| fetch(\'https://api.minimax.chat/v1/text/chatcompletion_v2\', {       |
|                                                                       |
| method: \'POST\',                                                     |
|                                                                       |
| headers: {                                                            |
|                                                                       |
| \'Authorization\': \`Bearer \${process.env.MINIMAX_API_KEY}\`,        |
|                                                                       |
| \'Content-Type\': \'application/json\'                                |
|                                                                       |
| },                                                                    |
|                                                                       |
| body: JSON.stringify(data.payload)                                    |
|                                                                       |
| });                                                                   |
|                                                                       |
| return response.json();                                               |
|                                                                       |
| });                                                                   |
+-----------------------------------------------------------------------+

  -------- ----------------------------------------------- ----------------
  **P2**   **Stitch MCP Integration --- screen             Weeks 4--7
           generation**                                    

  -------- ----------------------------------------------- ----------------

Goal: The app can take a text description and produce a rendered Android
XML layout preview via Stitch MCP, displayed in the Screen Canvas.

**Deliverables**

  ------------------------------------------------------------------------------------
  **Task**                    **What to build**          **Complexity**   **Output**
  --------------------------- -------------------------- ---------------- ------------
  StitchMcpClient.kt          Firebase Function acts as  High             Stitch
                              MCP client. Forwards                        returns
                              generate/edit/regenerate                    valid XML
                              tool calls to Stitch.                       
                              Returns raw XML.                            

  McpToolExecutor.kt          Wraps three Stitch tools:  Medium           All three
                              generate_screen,                            tools
                              edit_screen,                                callable
                              regenerate_screen. Typed                    
                              Kotlin wrappers.                            

  ScreenCanvasActivity.kt     Renders XML layout at      High             Screen
                              runtime using                               visible in
                              LayoutInflater in a                         canvas
                              sandboxed container. Shows                  
                              phone frame around it.                      

  XML Runtime Renderer        Parses Stitch XML.         High             Renders
                              Resolves \@color, \@dimen,                  without
                              \@string refs from local                    crash
                              resource stubs. Handles                     
                              ConstraintLayout.                           

  Canvas UI (XML)             Split view: AI chat on     Medium           Navigation
                              left, canvas preview on                     works
                              right (tablet) or bottom                    
                              sheet (phone). Toolbar:                     
                              Edit, Variants, Export.                     

  IntentArchitectAgent.kt     First agent integration.   Medium           Brief JSON
                              Converts chat message into                  returned
                              DesignBrief. One MiniMax                    
                              call with structured                        
                              output.                                     
  ------------------------------------------------------------------------------------

> **⚠ Warning:** Stitch MCP runs server-side. The Android app never
> calls Stitch directly. All MCP communication goes through your
> Firebase Cloud Function which holds the Stitch credentials. This is
> both a security requirement and an architecture requirement --- the
> agent pipeline runs in the cloud, not on device.

+-----------------------------------------------------------------------+
| // StitchMcpClient (Firebase Function side)                           |
|                                                                       |
| const { Client } =                                                    |
| require(\'@modelcontextprotocol/sdk/client/index.js\');               |
|                                                                       |
| async function generateScreen(brief, skillContext) {                  |
|                                                                       |
| const client = new Client({ name: \'forge-backend\', version:         |
| \'1.0.0\' });                                                         |
|                                                                       |
| await client.connect(stitchTransport);                                |
|                                                                       |
| const result = await client.callTool({                                |
|                                                                       |
| name: \'generate_screen\',                                            |
|                                                                       |
| arguments: {                                                          |
|                                                                       |
| description: brief.user_goal,                                         |
|                                                                       |
| platform: \'android\',                                                |
|                                                                       |
| constraints: skillContext // injected skill rules                     |
|                                                                       |
| }                                                                     |
|                                                                       |
| });                                                                   |
|                                                                       |
| return result.content\[0\].text; // raw XML                           |
|                                                                       |
| }                                                                     |
+-----------------------------------------------------------------------+

  -------- ----------------------------------------------- ----------------
  **P3**   **Skill System --- ingestion pipeline +         Weeks 8--12
           routing**                                       

  -------- ----------------------------------------------- ----------------

Goal: All 9 skills are ingested from their source books, stored in
Firestore with embeddings, and the SkillRouter can find and inject the
right skills for any design brief. The CriticAgent runs its first real
evaluation loop.

**3a. Offline ingestion pipeline (Python)**

+-----------------------------------------------------------------------+
| \# ingestion/parse_book.py                                            |
|                                                                       |
| import pdfplumber, json, re                                           |
|                                                                       |
| def extract_chapters(pdf_path: str) -\> list\[dict\]:                 |
|                                                                       |
| chapters = \[\]                                                       |
|                                                                       |
| with pdfplumber.open(pdf_path) as pdf:                                |
|                                                                       |
| current = {\'title\': \'\', \'text\': \'\'}                           |
|                                                                       |
| for page in pdf.pages:                                                |
|                                                                       |
| text = page.extract_text() or \'\'                                    |
|                                                                       |
| \# Detect chapter headings (heuristic: short line, title case)        |
|                                                                       |
| lines = text.split(\'\\n\')                                           |
|                                                                       |
| for line in lines:                                                    |
|                                                                       |
| if re.match(r\'\^Chapter \\d+\|\^CHAPTER\', line.strip()):            |
|                                                                       |
| if current\[\'text\'\]: chapters.append(current)                      |
|                                                                       |
| current = {\'title\': line.strip(), \'text\': \'\'}                   |
|                                                                       |
| else:                                                                 |
|                                                                       |
| current\[\'text\'\] += line + \' \'                                   |
|                                                                       |
| if current\[\'text\'\]: chapters.append(current)                      |
|                                                                       |
| return chapters                                                       |
|                                                                       |
| \# ingestion/structure_skill.py                                       |
|                                                                       |
| \# Uses MiniMax to convert raw chapter text into skill JSON           |
|                                                                       |
| def structure_skill(chapter_text: str, skill_name: str) -\> dict:     |
|                                                                       |
| prompt = f\'\'\'                                                      |
|                                                                       |
| You are a design expert. Extract concrete, machine-checkable rules    |
| from this text.                                                       |
|                                                                       |
| Output ONLY valid JSON matching this schema:                          |
|                                                                       |
| {{\"id\": string, \"name\": string, \"rules\": \[{{\"id\": string,    |
| \"rule\": string, \"severity\": \"error\"\|\"warning\",               |
| \"check_type\": \"structural\"\|\"semantic\"}}\]}}                    |
|                                                                       |
| Text: {chapter_text\[:4000\]}                                         |
|                                                                       |
| Skill name: {skill_name}                                              |
|                                                                       |
| \'\'\'                                                                |
|                                                                       |
| \# \... call MiniMax, parse response, validate JSON schema            |
+-----------------------------------------------------------------------+

**3b. Android skill runtime**

  --------------------------------------------------------------------------------
  **Task**                    **What to build**      **Complexity**   **Output**
  --------------------------- ---------------------- ---------------- ------------
  SkillRepository.kt          Loads skills from      Medium           Skills
                              Firestore on app                        available
                              start. Caches in Room                   offline
                              DB. Refreshes when                      
                              version changes.                        

  SkillRouter.kt              Embeds the design      High             Correct
                              brief text. Computes                    skills
                              cosine similarity                       returned
                              against skill                           
                              embedding vectors.                      
                              Returns top-N skills.                   

  CriticAgent.kt              Evaluates XML output   High             Revision
                              against each skill                      loop works
                              rule. Returns                           
                              CriticReport with                       
                              scores per rule.                        

  BrainOrchestrator.kt        Full loop:             High             End-to-end
                              IntentArchitect →                       generation
                              SkillRouter →                           
                              ScreenComposer →                        
                              Critic → revise (×3                     
                              max)                                    

  Skill Browser UI            RecyclerView showing   Low              Browser
                              all skills. Tap to see                  navigable
                              rules, source book,                     
                              version. Toggle to                      
                              activate/deactivate.                    
  --------------------------------------------------------------------------------

  -------- ----------------------------------------------- ----------------
  **P4**   **Agentic Layer --- all 9 agents + memory**     Weeks 13--17

  -------- ----------------------------------------------- ----------------

Goal: All specialized agents are live. Memory Keeper builds and
maintains design DNA. Variant Explorer, Accessibility Auditor, Motion
Director, and Code Surgeon are all callable from the UI.

  -----------------------------------------------------------------------------------
  **Task**                       **What to build**      **Complexity**   **Output**
  ------------------------------ ---------------------- ---------------- ------------
  MemoryKeeperAgent.kt           WorkingMemory          High             DNA survives
                                 (session),                              app restart
                                 StyleProfile                            
                                 (persistent),                           
                                 DesignDna                               
                                 (per-project). All                      
                                 persisted in                            
                                 Firestore.                              

  VariantExplorerAgent.kt        Generates 3 variants:  High             3 variants
                                 minimal, expressive,                    displayed
                                 structured. Shows in                    
                                 horizontal scroll                       
                                 carousel in canvas.                     

  AccessibilityAuditorAgent.kt   Parses XML. Checks     High             Issues
                                 contrast via WCAG                       shown +
                                 formula, touch                          fixed
                                 targets, content                        
                                 descriptions.                           
                                 Auto-applies fixes.                     

  MotionDirectorAgent.kt         Generates animator XML High             Motion
                                 and MotionLayout                        preview in
                                 constraints for entry,                  canvas
                                 exit, and state-change                  
                                 transitions.                            

  CodeSurgeonAgent.kt            Surgical XML edit      High             Diff shown
                                 mode. User taps                         to user
                                 element in canvas,                      
                                 describes change,                       
                                 agent edits minimum                     
                                 lines.                                  

  DesignReasoning UI             Below each generated   Medium           Reasoning
                                 screen: expandable                      readable
                                 list of decisions and                   
                                 the principle + book                    
                                 each came from.                         

  Cross-screen coherence         Memory Keeper extracts High             New screens
                                 design DNA after each                   match
                                 screen. New                             existing
                                 generations receive                     
                                 DNA in context.                         
  -----------------------------------------------------------------------------------

+-----------------------------------------------------------------------+
| // DesignDna.kt --- what the Memory Keeper extracts and stores        |
|                                                                       |
| data class DesignDna(                                                 |
|                                                                       |
| val projectId: String,                                                |
|                                                                       |
| val primaryColor: String, // e.g. \'#1D9E75\'                         |
|                                                                       |
| val secondaryColor: String,                                           |
|                                                                       |
| val neutralPalette: List\<String\>,                                   |
|                                                                       |
| val typescale: TypeScale, // headline, body, caption sizes            |
|                                                                       |
| val cornerRadius: Int, // base corner radius in dp                    |
|                                                                       |
| val spacingUnit: Int, // base 8dp grid unit                           |
|                                                                       |
| val componentPatterns: List\<String\>, // \'bottom-nav\',             |
| \'card-list\', \'fab\'                                                |
|                                                                       |
| val brandVoice: String, // e.g. \'friendly, clear, confident\'        |
|                                                                       |
| val screenCount: Int,                                                 |
|                                                                       |
| val lastUpdated: Timestamp                                            |
|                                                                       |
| )                                                                     |
+-----------------------------------------------------------------------+

  -------- ----------------------------------------------- ----------------
  **P5**   **Polish + differentiation features**           Weeks 18--20

  -------- ----------------------------------------------- ----------------

  --------------------------------------------------------------------------------
  **Task**                    **What to build**      **Complexity**   **Output**
  --------------------------- ---------------------- ---------------- ------------
  Export flow                 Export: Kotlin         Medium           Imports into
                              ViewBinding code, XML                   Android
                              layout, drawable                        Studio
                              resources, colour                       
                              tokens. ZIP download.                   

  Voice input                 SpeechRecognizer       Low              Voice
                              integration. Voice →                    triggers
                              text →                                  generation
                              IntentArchitect. Works                  
                              hands-free.                             

  Skill editor                In-app admin view to   Medium           Rules
                              edit skill rules, add                   editable in
                              examples, change                        app
                              severity. Version                       
                              bumps on save.                          

  Onboarding                  3-screen onboarding:   Medium           Profile set
                              style quiz (colour                      on first run
                              pref, density pref,                     
                              brand voice) → seeds                    
                              StyleProfile.                           

  Analytics                   Firebase Analytics:    Low              Dashboard
                              which skills fire                       visible
                              most, Critic pass                       
                              rate, avg revision                      
                              loops, user retention.                  
  --------------------------------------------------------------------------------

**6. Milestone Schedule**

  ----------------------------------------------------------------------------
  **Week**   **Milestone**     **Definition of Done**          **Risk**
  ---------- ----------------- ------------------------------- ---------------
  W3         Firebase + chat   User can sign in, send a        MiniMax API
             live              message to MiniMax, receive a   cost runaway
                               response. MiniMax proxy Cloud   
                               Function deployed.              

  W5         First screen      Typing a description in chat    Stitch XML
             generated         produces a rendered XML layout  parser edge
                               in the canvas. Stitch MCP       cases
                               end-to-end confirmed.           

  W7         IntentArchitect   Vague prompts are converted to  Brief quality
             live              structured DesignBriefs before  inconsistency
                               generation. Canvas shows        
                               structured output.              

  W10        First skill       Visual Hierarchy skill is       Rule check
             evaluates         ingested, stored, and the       accuracy
                               Critic scores a generated       
                               screen against its rules.       

  W12        Full revision     BrainOrchestrator runs generate Loop infinite
             loop              → critique → revise loop.       if thresholds
                               Critic score improves across    wrong
                               iterations.                     

  W14        All 9 skills      All skills ingested and routing Embedding
             active            correctly. SkillRouter returns  quality on
                               right skills for 85%+ of test   domain text
                               briefs.                         

  W16        Memory + DNA live Design DNA persists across      DNA drift over
                               sessions. New screen generation many screens
                               visually consistent with        
                               previous screens.               

  W17        All 9 agents      Every agent reachable from UI.  Motion XML
             callable          Accessibility audit, motion     compat with
                               spec, code surgeon all return   device API
                               usable output.                  

  W19        Export works      Generated screen exports as     XML namespace
                               valid Android Studio project    issues
                               structure. Imports and compiles 
                               without errors.                 

  W20        Beta-ready        Onboarding, analytics, voice    Performance on
                               input all live. App stable on   mid-range
                               API 28--34. Crash-free rate \>  devices
                               99%.                            
  ----------------------------------------------------------------------------

**7. Key Engineering Decisions**

**7.1 Why XML layouts, not Jetpack Compose**

Stitch MCP outputs XML. The Code Surgeon agent works on XML. The export
target is XML. Building in Compose would require a translation layer in
both directions --- from Stitch XML to Compose, and from Compose edits
back to exportable XML. This is two hard problems for zero user-facing
benefit. Build in XML, add a Compose export option in v2.

**7.2 Why MiniMax, not GPT-4o or Gemini**

The Brain needs to hold the full conversation, the activated skill
definitions (each \~2k tokens), the design DNA (\~1k tokens), and the
current XML layout (\~3--8k tokens) simultaneously. With 3 activated
skills and a moderately complex screen, context is 15--20k tokens per
call. With 10 skills and a complex project history, it can reach
60--80k. MiniMax-Text-01\'s 1M context window absorbs this with
headroom. GPT-4o\'s 128k limit would force context truncation that
degrades coherence quality.

**7.3 Why the critic loop runs on the server, not on device**

The revision loop makes 2--3 MiniMax calls per generation. Each call is
15--20k tokens. This is 300--500ms per call on a fast connection. On
device, battery, thermal throttling, and network variability would make
this unreliable. The Firebase Cloud Function runs the full loop ---
IntentArchitect → ScreenComposer → Critic → revise --- and returns only
the final result to the device. The device shows a progress indicator
during this time.

**7.4 Design DNA drift prevention**

As a project grows to 20+ screens, the design DNA document grows. Left
unchecked, late screens can contradict early ones. The Memory Keeper
runs a reconciliation step every 5 new screens: it re-reads all existing
DNA snapshots and produces a canonical merged version. The Critic checks
coherence as a top-level rule: any new screen that introduces a colour,
corner radius, or spacing value not in the DNA fails automatically and
triggers a revision.

**7.5 Skill versioning strategy**

Skills are versioned with a simple integer. When a skill is updated (new
rule added, rule severity changed), the version increments and the new
version is uploaded to Firestore. Existing screens are tagged with the
skill version they were generated under. The Critic always uses the
skill version that was active at generation time for consistency
scoring, but can optionally re-evaluate against the latest version and
flag drift.

**8. What to Build First --- Priority Order**

If you can only do one thing at a time, build in this exact order. Each
step creates the foundation the next one requires.

  ------- ---------------------- --------------------------------------------
  **1**   **Firebase project +   Everything else depends on being able to
          MiniMax proxy          call the AI.
          Function**             

  ------- ---------------------- --------------------------------------------

  ------- ---------------------- --------------------------------------------
  **2**   **Chat UI +            You need to see AI responses before you can
          MiniMaxClient.kt**     evaluate them.

  ------- ---------------------- --------------------------------------------

  ------- ---------------------- --------------------------------------------
  **3**   **Stitch MCP           This is the core value prop. Get it working
          integration + XML      early, iterate on quality.
          renderer**             

  ------- ---------------------- --------------------------------------------

  ------- ----------------------------- --------------------------------------------
  **4**   **IntentArchitectAgent.kt**   Vague input → structured brief. This is what
                                        makes generation reliable.

  ------- ----------------------------- --------------------------------------------

  ------- ---------------------- --------------------------------------------
  **5**   **First skill (Visual  This is what makes FORGE different from
          Hierarchy) +           Stitch alone.
          CriticAgent.kt**       

  ------- ---------------------- --------------------------------------------

  ------- ---------------------- --------------------------------------------
  **6**   **BrainOrchestrator    This is the quality multiplier. Even 1
          revision loop**        revision loop dramatically improves output.

  ------- ---------------------- --------------------------------------------

  ------- -------------------------- --------------------------------------------
  **7**   **MemoryKeeperAgent.kt +   This is what makes FORGE better with every
          DesignDna**                screen.

  ------- -------------------------- --------------------------------------------

  ------- ---------------------- --------------------------------------------
  **8**   **Remaining 8 skills** More skills = wider capability, higher
                                 quality ceiling.

  ------- ---------------------- --------------------------------------------

  ------- ---------------------- --------------------------------------------
  **9**   **All specialized      Accessibility, motion, variants, code
          agents**               surgeon.

  ------- ---------------------- --------------------------------------------

  -------- ---------------------- --------------------------------------------
  **10**   **Export + polish**    Only after everything above works reliably.

  -------- ---------------------- --------------------------------------------
