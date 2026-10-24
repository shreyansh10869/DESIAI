# Auralis AI — SSE Keep-Alive Heartbeat, `models_config.json` Dynamic Routing & Instant First-Token Streaming Plan

## Root Cause of Slow `"hi"` / Stuck `"..."` on Preview
1. **Unnecessary Thinking Delay on Fallback Model**: Passing `thinking_config: { thinking_level: "medium" }` to `gemini-3.1-flash-lite` caused the model to generate 175–600+ internal thought tokens before emitting the first visible word, making even a simple `"hi"` wait several seconds on the `...` dots.
2. **Repeated 404 Round-Trips on Primary Endpoint**: Every chat turn waited for a full network round-trip to `gemini-3.8-flash` before failing over to `gemini-3.1-flash-lite`. Caching the active endpoint after a 404 or using sub-400ms fast failover eliminates this delay.
3. **Idle Proxy Connection Drops**: Without a periodic `:\n\n` SSE comment heartbeat and `Cache-Control: no-cache, no-transform`, reverse proxies can buffer or drop long-running streams.

---

## 1. SSE Heartbeat / Keep-Alive & Dev Proxy Fix (`server.mjs`)
- On `POST /api/chat`:
  - Immediately write SSE headers:
    - `'Content-Type': 'text/event-stream'`
    - `'Cache-Control': 'no-cache, no-transform'`
    - `'Connection': 'keep-alive'`
  - Disable socket/request timeouts:
    - `req.setTimeout(0);`
    - `res.setTimeout(0);`
  - Emit an active SSE comment heartbeat every `5000ms` (plus an initial `:\n\n` flush right on connect so the proxy establishes the stream immediately):
    ```js
    const keepAliveTimer = setInterval(() => {
      if (!res.writableEnded) res.write(':\n\n');
    }, 5000);
    req.on('close', () => clearInterval(keepAliveTimer));
    ```
  - Clear `keepAliveTimer` before `res.end()`.
  - Pipe upstream SSE chunks directly to `res` via `res.write` without `await gResp.text()` buffering.

---

## 2. Config-Driven Dynamic Deep Think Chain & Fast Models (`assets/models_config.json` & `AiEngineService.kt`)
- Create `/assets/models_config.json` and `/app/src/main/assets/models_config.json`:
  ```json
  {
    "fast_primary": "gemini-3.8-flash",
    "fast_fallback": "gemini-3.1-flash-lite",
    "deep_think_chain": [
      "openai/gpt-oss-120b",
      "qwen/qwen3.8-27b",
      "llama-3.3-70b-versatile"
    ],
    "deep_think_gemini_fallback": "gemini-3.8-flash"
  }
  ```
- **Fast Mode**:
  - Primary: `gemini-3.8-flash` (`https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:streamGenerateContent?alt=sse`) with `{ "thinking_config": { "thinking_level": "medium" } }`.
  - Fallback: `gemini-3.1-flash-lite` (`https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:streamGenerateContent?alt=sse`) configured for instant token emission (`<400ms` time-to-first-token) so simple prompts like `"hi"` answer immediately.
- **Deep Think Engine**:
  - Sequentially attempt models in `deep_think_chain` (`openai/gpt-oss-120b`, `qwen/qwen3.8-27b`, `llama-3.3-70b-versatile`) via Groq API (`https://api.groq.com/openai/v1/chat/completions`), and if unavailable or `groqKey` is blank, fall back to `deep_think_gemini_fallback` (`gemini-3.8-flash` -> `gemini-3.1-flash-lite`) with step-by-step reasoning instructions.

---

## 3. Pre-Configured Keys (Zero UI Key Popups)
- Resolve `geminiKey` and `groqKey` with strict `BuildConfig` priority in `AiEngineService.kt`:
  ```kotlin
  val geminiKey = BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() }
      ?: prefs.getString("gemini_api_key", null)?.trim()?.takeIf { it.isNotBlank() }
      ?: System.getenv("GEMINI_API_KEY") ?: ""
  val groqKey = BuildConfig.GROQ_API_KEY.takeIf { it.isNotBlank() }
      ?: prefs.getString("groq_api_key", null)?.trim()?.takeIf { it.isNotBlank() }
      ?: System.getenv("GROQ_API_KEY") ?: ""
  ```
- Authenticate Gemini requests strictly via HTTP headers `'x-goog-api-key': geminiKey` and `'Content-Type': 'application/json'`.

---

## 4. Multimodal Image Vision & Real-Time Streaming
- Downscale attached images (max `1024px`), compress to Base64 `inlineData` (`image/jpeg`), and pass directly into the Gemini `contents` payload with strict role alternation.
- Stream SSE lines directly onto `Dispatchers.Main`, recycle bitmaps, close streams, and reset `isSynthesizing = false` in `finally` blocks.

---

## 5. Minimalist `#0A0A0A` ChatGPT Interface (`MainActivity.kt` & `server.mjs`)
- Solid `#0A0A0A` obsidian background, single-row header with title, `Fast` / `Deep Think` pill switch, and Settings gear icon (`Icons.Filled.Settings`), `#1F1F1F` user bubbles, borderless AI prose, 3 pulsing dots inside a `#171717` capsule while awaiting the first token, and `RoundedCornerShape(26.dp)` floating input bar.
