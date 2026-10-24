# Auralis AI — Minimalist ChatGPT-Style Redesign & Real API Streaming Plan

Redesign **Auralis AI** into a distraction-free, pure dark (`#0A0A0A`) ChatGPT-style interface and eliminate all hardcoded dummy templates, static comparison tables, and citation boxes so the app exclusively streams real responses from the configured AI engines.

## User Review & Critical Decisions

> [!IMPORTANT]
> Please review the following architectural and UX decisions before execution:

- **Eliminate All Dummy Fallbacks & Boilerplate Cards**: All static offline mock responses (`buildLocalAuralisResponse`), preset Markdown tables (`Core Insight` / `Stage` tables), and `Auralis Verified Sources` boxes will be removed. If an API key is missing or a network call fails, Auralis AI will render a single, clean one-line status message (e.g., `"Please add your Gemini or Groq API key in Settings to start chatting."` or `"Connection error: Unable to reach AI engine."`).
- **Minimalist ChatGPT-Style Layout**: Pure dark `#0A0A0A` canvas, a single-row minimalist top bar (`Auralis AI` title + compact `Fast` / `Deep Think` pill selector + Settings icon), unboxed left-aligned AI responses, subtle right-aligned user message bubbles (`#1E1E1E`), and a `RoundedCornerShape(26.dp)` floating bottom input bar with `+`, `Message` placeholder, and dynamic `Mic` / `Send` button.

---

## 1. Overview & Core Concept

- **What It Does**: Provides a fast, lightweight, clutter-free AI chat experience on Android powered by real-time API streaming from Gemini (`gemini-3.1-flash-lite-preview` / `gemini-2.5-flash`) in **Fast** mode and LongCat / Groq (`openai/gpt-oss-120b`) / Gemini in **Deep Think** mode.
- **Target Audience / Persona**: Users who want a clean, responsive, ChatGPT-grade mobile assistant without visual noise, artificial borders, or canned mock replies.
- **Key Value**: Instant readability, authentic live AI generation, and ergonomic one-handed mobile interaction.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Zero-State Launch**: Opens onto a clean `#0A0A0A` canvas with a minimal centered greeting (`"What can I help with?"`) and optional one-tap starter prompts.
  2. **Model Switching & Settings**: Top bar allows one-tap switching between `Fast` and `Deep Think` modes or opening the lightweight API key configuration dialog (plus a clean new-chat reset action).
  3. **Prompting & Streaming**: Tapping the bottom `RoundedCornerShape(26.dp)` pill lets the user attach an image via `+`, dictate via `Mic`, or type a message and tap `Send`. While waiting for the first token, a small capsule displays 3 subtle pulsing dots (no text labels). As tokens arrive on `Dispatchers.Main`, the dots transition into clean, left-aligned streaming text.
- **Visual Identity & Theme**:
  - *Aesthetic Direction*: Ultra-minimalist monochrome dark mode inspired by the official ChatGPT mobile app.
  - *Color Palette & Mood*:
    - Dominant Canvas (`60%`): Pure Dark `#0A0A0A` (`Color(0xFF0A0A0A)`).
    - Structural Surfaces (`30%`): Subtle elevated dark gray `#171717` (`Color(0xFF171717)`) for the input dock and `#1F1F1F` (`Color(0xFF1F1F1F)`) for user message bubbles, paired with `#ECECEC` primary text and `#8E8E93` muted secondary text.
    - Accent Budget (`10%`): Crisp white `#FFFFFF` primary action button with dark `#0A0A0A` icon contrast.
  - *Typography & Hierarchy*: Clean system sans-serif with `15.5sp` body prose (`23sp` line height) for effortless reading, bold section headers, and dark `#141414` monospace blocks for code snippets—with all artificial table wrappers removed.
  - *Component Styling & Layout*: Minimum `44dp × 44dp` touch hitboxes, generous vertical spacing between conversation turns, and sticky header + input bar occupying under `15%` of the viewport height.
- **Interactive Feedback & Motion**:
  - **Typing Indicator**: 3 subtle pulsing dots inside a compact dark capsule (`#171717`) with zero text labels.
  - **Send / Mic Transition**: `150ms` smooth crossfade between `Mic` (when input is empty) and upward/forward `Send` arrow (when text or image is present).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Strict Removal of Offline Mock Templates**
  - *Chosen Approach*: Remove all canned fallback strings and hardcoded Markdown tables. Return real API output or a concise one-line error string.
  - *Why*: Canned responses mislead users into thinking the model generated boilerplate tables and obscure API key configuration issues.
  - *Alternatives Considered*: Keeping a shorter offline demo response (rejected because the user explicitly requested zero dummy templates).
- **Decision 2: Unboxed Left-Aligned AI Prose**
  - *Chosen Approach*: Render AI responses directly on the `#0A0A0A` background without card borders, colored outlines, or citation boxes, keeping subtle inline action icons (Copy, Share, Speak) below completed responses.
  - *Why*: Matches the official ChatGPT reading experience and maximizes horizontal screen real estate on mobile viewports.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:

```
┌───────────────────────────────────────────────────────────────────┐
│                         MainActivity UI                           │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │ Minimal Top Bar: Title | [Fast / Deep Think] Pill | Settings│  │
│  └─────────────────────────────────────────────────────────────┘  │
│                                 │                                 │
│  ┌──────────────────────────────▼──────────────────────────────┐  │
│  │                     Chat Conversation                       │  │
│  │  • Empty State: Minimal centered "What can I help with?"    │  │
│  │  • User Turn: Subtle right-aligned bubble (#1F1F1F)         │  │
│  │  • AI Turn: Left-aligned plain formatted text (no border)   │  │
│  │  • Thinking State: 3 minimal pulsing dots capsule           │  │
│  └──────────────────────────────┬──────────────────────────────┘  │
│                                 │                                 │
│  ┌──────────────────────────────▼──────────────────────────────┐  │
│  │    Floating Input Bar (RoundedCornerShape(26.dp), #171717)  │  │
│  │    [+] Attach Image  |  "Message" Input  |  [Mic / Send]    │  │
│  └──────────────────────────────┬──────────────────────────────┘  │
└─────────────────────────────────┼─────────────────────────────────┘
                                  │ Dispatchers.Main (try / finally)
┌─────────────────────────────────▼─────────────────────────────────┐
│                         AiEngineService                           │
│  • Fast Mode  ──► Gemini API (gemini-3.1-flash-lite / 2.5-flash)  │
│  • Deep Think ──► Primary Reasoning (3.5s timeout)                │
│                     └─► Fallback: Groq (openai/gpt-oss-120b)      │
│                     └─► Fallback: Gemini API                      │
│  • CJK Regex Sanitizer + Clean 1-Line Error on Missing Key/Failure│
└───────────────────────────────────────────────────────────────────┘
```

- **Data Model & State**:
  - `ChatMessage` (`DesiMessage`): Holds `id`, `text`, `isUser`, `isLoading`, `error`, `imageUriString`, and `engineMode` (citations list unused/empty).
  - Persistent history stored in `SharedPreferences` (`auralis_ai_prefs`) with automatic sanitization.
- **Interactive Component & State Mapping**:
  - **Model Selector Pill**: Toggles `selectedEngineMode` between `FAST_MODE` and `DEEP_THINK` and persists selection.
  - **Settings Dialog**: Allows viewing/saving Gemini, Groq, and LongCat API keys and clearing conversation history.
  - **Streaming Lifecycle**: `sendMessage` appends the user message and an empty loading placeholder (`isLoading = true, text = ""`), streams sanitized chunks onto `Dispatchers.Main`, replaces the placeholder with a concise one-line message on error, and guarantees `isSynthesizing = false` inside `finally` on `Dispatchers.Main`.
