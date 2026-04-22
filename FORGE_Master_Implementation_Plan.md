# FORGE — Master Implementation Plan

> **Living Document.** Check off tasks as they are completed. Add notes under each task when implemented.
> Last updated: 2026-04-19 | GitHub: https://github.com/Dipesh600/Forge-Design.git

---

## What is FORGE?

FORGE is an **agentic AI-powered Android UI design studio**. Unlike tools like Stitch, Galileo, or Uizard that generate by pattern-matching, FORGE generates by **reasoning** — using a library of design skills extracted from 9 canonical design books, orchestrated by a **multi-agent brain** powered by **MiniMax AI**, with **Stitch MCP** as its rendering engine.

### Key Differentiators

| Capability | Stitch / Galileo / Uizard | FORGE |
|---|---|---|
| Generation | One pass. No revision. | Multi-agent: generate → critique → score → revise (2–3 loops) |
| Principles | Implicit (pattern-matched) | Explicit rules from 9 canonical books, versioned in Firestore |
| Explainability | None | Every decision annotated with source principle + book |
| Cross-screen | No memory | Design DNA: coherence checked across all screens |
| User adaptation | None | Memory Keeper builds a style profile over time |
| Motion | Static XML only | Motion Director: spring physics, easing, choreography |
| Accessibility | No audit | Dedicated Auditor: WCAG 2.2, contrast, touch targets |
| Copy | Placeholder text | Micro-copy Skill writes labels, errors, empty states |

---

## Technology Stack

| Layer | Technology | Why |
|---|---|---|
| UI | Kotlin + XML layouts | Surgical diff/patch control for Code Surgeon; Compose is harder to diff programmatically |
| AI Brain | MiniMax AI (MiniMax-Text-01) | 1M token context window — holds full history + skills + DNA; GPT-4o maxes at 128k |
| Screen generation | Stitch MCP | Only tool that outputs production Android XML; direct path to running code |
| Auth + DB | Firebase Auth + Firestore | Real-time sync, offline support, skill versioning, Cloud Functions for server calls |
| Local storage | Room Database + DataStore | Room for skill state, history, DNA; DataStore for user prefs + style profile |
| Skill ingestion | Python pipeline (offline) | PDF parsing with pdfplumber; output stored in Firestore |
| Vector search | Firebase + Vertex AI Embeddings | Semantic skill retrieval without keyword matching |

---

## Repository Structure

```
Vdesign2/  (GitHub: Forge-Design)
├── app/
│   └── src/main/java/com/forge/
│       ├── agents/
│       │   ├── DirectorAgent.kt
│       │   ├── CriticAgent.kt
│       │   ├── MemoryKeeperAgent.kt
│       │   ├── IntentArchitectAgent.kt
│       │   ├── ScreenComposerAgent.kt
│       │   ├── VariantExplorerAgent.kt
│       │   ├── AccessibilityAuditorAgent.kt
│       │   ├── MotionDirectorAgent.kt
│       │   └── CodeSurgeonAgent.kt
│       ├── skills/
│       │   ├── SkillRepository.kt
│       │   ├── SkillRouter.kt
│       │   └── models/Skill.kt
│       ├── brain/
│       │   ├── MiniMaxClient.kt
│       │   ├── BrainOrchestrator.kt
│       │   └── ConversationManager.kt
│       ├── mcp/
│       │   ├── StitchMcpClient.kt
│       │   └── McpToolExecutor.kt
│       ├── memory/
│       │   ├── DesignDna.kt
│       │   ├── StyleProfile.kt
│       │   └── WorkingMemory.kt
│       ├── ui/
│       │   ├── chat/
│       │   ├── canvas/
│       │   ├── skills/
│       │   └── settings/
│       └── data/
├── ingestion/
│   ├── parse_book.py
│   ├── structure_skill.py
│   ├── embed_and_index.py
│   └── deploy_skill.py
├── functions/
│   ├── minimax_proxy.js
│   └── stitch_mcp_proxy.js
├── firebase.json
├── FORGE_Project_Log.md
└── FORGE_Master_Implementation_Plan.md
```

---

## Agent Architecture (3 Tiers)

### Tier A — Meta-agents (Orchestration)

| Agent | Role | Input | Output | Trigger |
|---|---|---|---|---|
| **DirectorAgent** | Decomposes user goal into ordered task graph | User message + history + DNA | `TaskGraph` JSON | Every new user request |
| **CriticAgent** | Scores output against skill rules | Agent output (XML/text) + skill rule IDs | `CriticReport` | After every ScreenComposer / MotionDirector call |
| **MemoryKeeperAgent** | Maintains working memory, style profile, design DNA | Every agent output + user feedback | Updated `WorkingMemory`, `StyleProfile`, `DesignDna` | After every interaction |

### Tier B — Specialized Agents

| Agent | Role | Input | Output | Trigger |
|---|---|---|---|---|
| **IntentArchitectAgent** | Transforms vague input → precise design brief | Raw user message | `DesignBrief` | Start of every generation flow |
| **ScreenComposerAgent** | Core generation — calls Stitch MCP + applies skill improvements | `DesignBrief` + `Skill[]` + `DesignDna` | Annotated XML + `DesignReasoning[]` | After IntentArchitect |
| **VariantExplorerAgent** | Generates 3 directional variants (minimal / expressive / structured) | `DesignBrief` + `ActivatedSkills[]` | `VariantSet` | User requests alternatives |
| **AccessibilityAuditorAgent** | WCAG 2.2 checks: contrast, touch targets, order | XML layout + resource values | `AuditReport` | After every screen generation |
| **MotionDirectorAgent** | Writes Animator XML + MotionLayout constraints | Static XML layout | `MotionSpec` | User enables motion layer |
| **CodeSurgeonAgent** | Surgical XML edit — minimum diff only | Existing XML + user change request | Diff + validated XML | User taps element → describes change |

---

## The 9 Design Skills

| # | Skill | Source Books | Key Rules |
|---|---|---|---|
| 1 | **Visual Hierarchy** | Refactoring UI; Universal Principles of Design | Max 3 font sizes; primary = highest weight; 8dp/32dp whitespace; F-pattern |
| 2 | **Typography System** | Elements of Typographic Style; Practical Typography | 45–75 chars/line; 1.4–1.6× line-height; max 2 typefaces; optical sizing |
| 3 | **Cognitive Load** | Don't Make Me Think; Design of Everyday Things | 3-click rule; recognition > recall; progressive disclosure; Miller's Law 7±2 |
| 4 | **Layout Grammar** | Grid Systems in Graphic Design (Müller-Brockmann) | Every element on a column; 8dp base unit; alignment is a promise |
| 5 | **Colour Intelligence** | Interaction of Color (Albers); Refactoring UI | Saturation = status; 4.5:1 contrast AA; colour system not one-off picks |
| 6 | **Emotional Design** | Emotional Design (Norman); Hooked (Nir Eyal) | 50ms visceral; behavioural = usability; delight moments; variable reward |
| 7 | **Interaction Patterns** | Designing with the Mind in Mind; About Face | Visible affordances; feedback < 100ms; consistent gestures; escape always available |
| 8 | **Micro-copy** | Strategic Writing for UX; Writing is Designing | Labels are verbs; error = what+why+fix; brand voice 3 adjectives |
| 9 | **Android Platform Fluency** | Material Design 3 Spec; Android Developer Guidelines | Edge-to-edge; 48dp touch targets; Material 3 components; predictive back API 34+ |

---

## Key Engineering Decisions

> **Why XML, not Compose:** Stitch MCP outputs XML. Code Surgeon works on XML. Export target is XML. Compose would need a translation layer in both directions — two hard problems for zero user benefit.

> **Why MiniMax, not GPT-4o:** 3 skills + moderately complex screen = 15–20k tokens/call. 10 skills + project history = 60–80k. MiniMax-Text-01's 1M context window absorbs this. GPT-4o's 128k forces truncation.

> **Why server-side Critic loop:** 2–3 MiniMax calls per generation. Battery, thermal throttling, network variability make on-device unreliable. Firebase Cloud Function runs the full loop and returns only the final result.

> **DNA drift prevention:** Every 5 screens, Memory Keeper reconciles all DNA snapshots → canonical merged version. Critic auto-fails any screen introducing colours/spacing not in DNA.

---

## Phase Breakdown & Task Tracking

> `[ ]` = Not started · `[/]` = In progress · `[x]` = Done
> Add *(implementation notes)* beneath completed tasks.

---

## ✦ PHASE 1 — Foundation: Android Shell + MiniMax Round-Trip
**Weeks 1–3** | **Goal:** Working Android app that talks to MiniMax AI through a secure Firebase proxy.
**DoD:** User can sign in, send a message, receive MiniMax AI response in chat UI.

### 1.1 Firebase Setup
- [x] Create Firebase project (Auth, Firestore, Storage, Functions stub)
  - [x] Enable Email + Google Sign-In
  - [x] Firestore security rules (deny all by default, auth-gated reads)
  - [x] Create Storage bucket
  - [x] Deploy stub Cloud Function
- [x] Add `google-services.json` to Android project

### 1.2 Android Project Init
- [x] Update package namespace (`com.shuvmarg.vdesign` → decide on `com.forge`)
- [x] Add dependencies to `build.gradle.kts`:
  - [x] Firebase BOM (Auth, Firestore, Functions, Analytics)
  - [x] Hilt dependency injection
  - [x] Retrofit + OkHttp
  - [x] Room Database + KSP processor
  - [x] DataStore Preferences
  - [x] Kotlin Coroutines + Flow
  - [x] Navigation Component
  - [x] ViewBinding enabled
- [x] FORGE brand MaterialTheme (primary, secondary, neutral palette)
- [x] App builds and runs cleanly ✅

### 1.3 MiniMax AI Integration
> ⚠️ **Security rule: MiniMax API key must NEVER be in the Android app.** Always proxy through Firebase Cloud Function.

- [x] `functions/minimax_proxy.js` — validates Firebase Auth, holds API key in env config, forwards to MiniMax API
- [x] Deploy `minimax_proxy` to Firebase Functions
- [x] `MiniMaxClient.kt` — Retrofit client pointing to Firebase Function proxy
  - [x] Unit test passes

### 1.4 Chat Foundation
- [x] `ConversationManager.kt` — maintains message history, truncates at 80% of MiniMax token limit, persists to Room
- [x] `ChatViewModel.kt` — StateFlow state machine: `Idle → Loading → Success/Error`
- [x] Chat UI (`chat_fragment.xml`) — RecyclerView with DiffUtil, user bubble (right), AI bubble (left), typing indicator, input bar

**W3 Milestone:** Firebase + Chat live. Sign in → message MiniMax → see response.

---

## ✦ PHASE 2 — Stitch MCP: Screen Generation ✅
**Weeks 4–7** | **Goal:** Text description → rendered Android XML layout in Screen Canvas.
**DoD:** Typing description in chat produces rendered XML layout in canvas.

### 2.1 Stitch MCP Server Integration
- [x] `functions/stitch_mcp_proxy.js` — Firebase Function wrapping Stitch MCP calls
  - Tools: `generate_screen`, `edit_screen`, `regenerate_screen`
  - *(Deployed 2026-04-19 — uses @modelcontextprotocol/sdk + stitch-mcp npm package)*
- [x] `StitchMcpClient.kt` — calls Firebase proxy, returns raw XML
- [x] `McpToolExecutor.kt` — typed Kotlin wrappers for all 3 tools with error handling + retry (3 attempts, exponential backoff)

### 2.2 XML Runtime Renderer
- [x] `XmlRenderer.kt`
  - Runtime parse via `LayoutInflater` in sandboxed container
  - Manual fallback parser for unknown views
  - Resolves `@color`, `@dimen`, `@string` refs from local resource stubs
  - Handles ConstraintLayout, LinearLayout, FrameLayout, ScrollView, CardView
  - Graceful error state (no crash)

### 2.3 Screen Canvas
- [x] `ScreenCanvasActivity.kt` — sandboxed XML preview with phone frame (340×720dp)
- [x] Canvas UI: loading state, error state with retry, design palette swatches, typography info
- [x] Design Reasoning panel — shows decisions + principles + source books
- [x] Bottom action bar: Variants | Regenerate | Export (Phase 4/5 stubs)
- [x] `CanvasViewModel.kt` — full IntentArchitect → Stitch → ScreenComposer pipeline

### 2.4 IntentArchitect Agent
- [x] `DesignBrief.kt` data class (`screen_type`, `user_goal`, `constraints[]`, `mood`, `platform`)
- [x] `IntentArchitectAgent.kt` — vague message → structured brief, clarification flow
- [x] Wire IntentArchitect into chat flow — design requests show Canvas Card + auto-launch canvas
  - *(ChatViewModel detects design requests via keyword heuristic + MiniMax structured output)*
  - *(Canvas Card in RecyclerView with "Open in Canvas →" CTA)*
  - *(MainActivity collects canvasLaunchEvents and starts ScreenCanvasActivity)*

**W5 Milestone ✅ First screen generated. W7 ✅ IntentArchitect live.**

---

## ✦ PHASE 3 — Skill System + Critic + Revision Loop
**Weeks 8–12** | **Goal:** First skill live, Critic evaluates screens, BrainOrchestrator runs the revision loop.
**DoD:** generate → critique → revise loop runs. Critic score improves across iterations.

### 3.1 Skill Data Model
- [ ] `Skill.kt` + `SkillRule.kt` data classes (id, name, version, sourceBooks, rules[], failureTaxonomy, embedding)
- [ ] `SkillRepository.kt` — reads from Firestore `skills/` → caches in Room

### 3.2 Python Skill Ingestion Pipeline
- [ ] `ingestion/parse_book.py` — PDF parse with `pdfplumber`
- [ ] `ingestion/structure_skill.py` — MiniMax structures passages → `SkillRule` JSON
- [ ] `ingestion/embed_and_index.py` — Vertex AI embeddings → Firestore
- [ ] `ingestion/deploy_skill.py` — uploads to Firestore `skills/` collection
- [ ] Run pipeline for **Skill 1: Visual Hierarchy** first
- [ ] Run pipeline for remaining 8 skills (incremental)

### 3.3 Skill Router
- [ ] `SkillRouter.kt` — takes `DesignBrief` → semantic similarity search → returns top-N skills
- [ ] Target: correct skills for 85%+ of test briefs

### 3.4 Critic Agent
- [ ] `CriticReport.kt` (`overallScore`, `passed`, `ruleScores`, `violations[]`, `suggestedFixes[]`)
- [ ] `CriticAgent.kt` — evaluates XML against skill rules via MiniMax, returns `CriticReport`
- [ ] Revision threshold: `overallScore < 7.0` triggers revision

### 3.5 Brain Orchestrator
- [ ] `BrainOrchestrator.kt` — full loop:
  1. `IntentArchitect` → DesignBrief
  2. `SkillRouter` → relevant skills
  3. `ScreenComposer` → draft XML
  4. `Critic` → CriticReport
  5. If score < 7.0 AND loops < 3 → revise
  6. Return final XML + CriticReport
- [ ] Max 3 revision loops (prevents infinite)
- [ ] Progress events: `GeneratingScreen`, `Critiquing`, `Revising(N)`, `Done`

### 3.6 Screen Composer Agent
- [ ] `ScreenComposerAgent.kt` — takes DesignBrief + Skill[] + DesignDna → calls Stitch MCP → post-processes XML → returns annotated XML + DesignReasoning[]

### 3.7 Skill Browser UI
- [ ] `SkillBrowserFragment.kt` — RecyclerView of 9 skills, tap for rules/books/version, toggle activate/deactivate

**W10 Milestone:** First skill evaluates. W12: Full revision loop with measurable improvement.

---

## ✦ PHASE 4 — Agentic Layer: All 9 Agents + Memory
**Weeks 13–17** | **Goal:** All agents live, Design DNA persists across sessions, all agents reachable from UI.
**DoD:** Every agent callable. New screens visually consistent with existing ones.

### 4.1 Memory Keeper Agent
- [ ] `DesignDna.kt` — `projectId`, `primaryColor`, `secondaryColor`, `neutralPalette`, `typescale`, `cornerRadius`, `spacingUnit`, `componentPatterns`, `brandVoice`, `screenCount`, `lastUpdated`
- [ ] `StyleProfile.kt` — user inferred design preferences
- [ ] `WorkingMemory.kt` — current session context
- [ ] `MemoryKeeperAgent.kt` — extracts DNA per screen, reconciles every 5 screens, persists all to Firestore

### 4.2 Variant Explorer Agent
- [ ] `VariantExplorerAgent.kt` — 3 variants: minimal / expressive / structured, horizontal scroll carousel in canvas

### 4.3 Accessibility Auditor Agent
- [ ] `AccessibilityAuditorAgent.kt`
  - Contrast ratio (WCAG AA: 4.5:1 body, 3:1 large text)
  - Touch targets ≥ 48×48dp
  - Reading order, focus order, `contentDescription` on all interactive elements
  - Text scaling support
  - Returns `AuditReport` with severity + `auto_fixes[]`

### 4.4 Motion Director Agent
- [ ] `MotionDirectorAgent.kt` — identifies transition points → writes Animator XML + MotionLayout constraints
- [ ] Motion preview in canvas (plays transition on element tap)

### 4.5 Code Surgeon Agent
- [ ] `CodeSurgeonAgent.kt` — tap element in canvas → describe change → agent edits minimum lines → shows diff → user confirms
- [ ] Tap-to-edit gesture on canvas elements

### 4.6 Design Reasoning UI
- [ ] Below each screen: expandable list of decisions (decision → principle → source book)

**W14 Milestone:** All 9 skills active + routing correctly. W16: DNA persists. W17: All 9 agents callable.

---

## ✦ PHASE 5 — Polish & Differentiation
**Weeks 18–20** | **Goal:** Export, voice, onboarding, analytics. Beta-ready.
**DoD:** Crash-free > 99%. API 28–34 stable. All features live.

### 5.1 Export Flow
- [ ] Kotlin ViewBinding code + XML layout + drawables + colour tokens → ZIP
- [ ] ZIP imports and compiles in Android Studio without errors

### 5.2 Voice Input
- [ ] `SpeechRecognizer` integration → voice → text → IntentArchitect
- [ ] Push-to-talk button in chat UI

### 5.3 Skill Editor (Admin)
- [ ] In-app admin view: edit rules, add examples, change severity
- [ ] Version auto-increments on save

### 5.4 Onboarding (3 screens)
- [ ] Colour preference quiz → density preference → brand voice selection
- [ ] Seeds initial `StyleProfile` in Firestore

### 5.5 Analytics
- [ ] Firebase Analytics: which skills fire most, Critic pass rate, avg revision loops, retention

**W19 Milestone:** Export works and compiles. W20: Beta-ready.

---

## Milestone Schedule

| Week | Milestone | DoD | Risk |
|---|---|---|---|
| W3 | Firebase + Chat live | Sign in → message MiniMax → response in UI | MiniMax API cost runaway |
| W5 | First screen generated | Description → rendered XML in canvas | Stitch XML parser edge cases |
| W7 | IntentArchitect live | Vague prompts → structured DesignBriefs | Brief quality inconsistency |
| W10 | First skill evaluates | Visual Hierarchy ingested, Critic scores a screen | Rule check accuracy |
| W12 | Full revision loop | generate → critique → revise. Score improves. | Infinite loop if thresholds wrong |
| W14 | All 9 skills active | Routing correct for 85%+ of test briefs | Embedding quality on domain text |
| W16 | Memory + DNA live | DNA persists across sessions. New screens coherent. | DNA drift over many screens |
| W17 | All 9 agents callable | Every agent reachable. Audit, motion, surgeon all return usable output. | Motion XML compat with device API |
| W19 | Export works | Exports as valid Android Studio project. Imports + compiles. | XML namespace issues |
| W20 | Beta-ready | Onboarding, analytics, voice live. Crash-free > 99%. | Performance on mid-range devices |

---

## GitHub Protocol

**Remote:** https://github.com/Dipesh600/Forge-Design.git

### Commit Convention
```
feat(phase1): add MiniMaxClient.kt with Firebase proxy route
fix(canvas): resolve XML renderer crash on missing @dimen refs
chore(deps): add Hilt + Room to build.gradle.kts
docs(plan): tick off Phase 1.3 MiniMax integration tasks
```

### Branching Strategy
```
main          ← always the last known-working state
develop       ← integration branch
feature/p1-firebase
feature/p1-minimax-client
feature/p2-stitch-mcp
feature/p3-skills
feature/p4-agents
```

### Checkpoint Rule (before merging to main)
1. App builds without errors
2. App runs on emulator/device (API 28+)
3. Phase DoD criteria are met
4. This plan's task is ticked off
5. Log entry added to `FORGE_Project_Log.md`

---

## Current Status (2026-04-19)

| Phase | Status | Completion |
|---|---|---|
| P1 — Foundation | 🟡 In progress | 10% |
| P2 — Stitch MCP | 🔴 Not started | 0% |
| P3 — Skill System | 🔴 Not started | 0% |
| P4 — Agentic Layer | 🔴 Not started | 0% |
| P5 — Polish | 🔴 Not started | 0% |

**Baseline app state:**
- Package: `com.shuvmarg.vdesign` | MinSDK: 24 | TargetSDK: 36
- Has: `MainActivity`, basic XML layout, Material 3 theme, standard AndroidX dependencies
- Missing: Firebase, Hilt, Room, Retrofit, MiniMax integration, Stitch, all agents, all skills
