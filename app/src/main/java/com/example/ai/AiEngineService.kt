package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

enum class AiEngineMode(val displayName: String, val badgeLabel: String) {
    FAST_MODE("Fast", "Fast"),
    DEEP_THINK("Deep Think", "Deep Think")
}

data class ModelsConfig(
    val fastPrimary: String = "gemini-3.8-flash",
    val fastFallback: String = "gemini-3.1-flash-lite",
    val deepThinkChain: List<String> = listOf(
        "openai/gpt-oss-120b",
        "qwen/qwen3.8-27b",
        "llama-3.3-70b-versatile"
    ),
    val deepThinkGeminiFallback: String = "gemini-3.8-flash"
)

class AiEngineService(private val context: Context) {

    companion object {
        const val FAST_PRIMARY_MODEL = "gemini-3.8-flash"
        const val FAST_FALLBACK_MODEL = "gemini-3.1-flash-lite"
        const val DEEP_THINK_GROQ_MODEL = "llama-3.3-70b-versatile"

        const val GEMINI_PRIMARY_SSE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:streamGenerateContent?alt=sse"
        const val GEMINI_FALLBACK_SSE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:streamGenerateContent?alt=sse"
        const val GROQ_COMPLETIONS_ENDPOINT =
            "https://api.groq.com/openai/v1/chat/completions"

        private val CLOUD_PROXY_ENDPOINTS = listOf(
            "https://ais-pre-qamaf3efozgtsy6v3vnyw3-886284290712.asia-southeast1.run.app/api/chat",
            "https://ais-dev-qamaf3efozgtsy6v3vnyw3-886284290712.asia-southeast1.run.app/api/chat"
        )
    }

    private val prefs = context.getSharedPreferences("auralis_ai_prefs", Context.MODE_PRIVATE)
    private val legacyPrefs = context.getSharedPreferences("desiai_prefs", Context.MODE_PRIVATE)

    val modelsConfig: ModelsConfig by lazy { loadModelsConfigFromAssets() }

    private fun loadModelsConfigFromAssets(): ModelsConfig {
        return try {
            val jsonText = context.assets.open("models_config.json").bufferedReader().use { it.readText() }
            val obj = JSONObject(jsonText)
            val chainArr = obj.optJSONArray("deep_think_chain")
            val chainList = mutableListOf<String>()
            if (chainArr != null) {
                for (i in 0 until chainArr.length()) {
                    val item = chainArr.optString(i, "").trim()
                    if (item.isNotEmpty()) chainList.add(item)
                }
            }
            ModelsConfig(
                fastPrimary = obj.optString("fast_primary", FAST_PRIMARY_MODEL).ifBlank { FAST_PRIMARY_MODEL },
                fastFallback = obj.optString("fast_fallback", FAST_FALLBACK_MODEL).ifBlank { FAST_FALLBACK_MODEL },
                deepThinkChain = if (chainList.isNotEmpty()) chainList else listOf(
                    "openai/gpt-oss-120b",
                    "qwen/qwen3.8-27b",
                    "llama-3.3-70b-versatile"
                ),
                deepThinkGeminiFallback = obj.optString("deep_think_gemini_fallback", FAST_PRIMARY_MODEL)
                    .ifBlank { FAST_PRIMARY_MODEL }
            )
        } catch (_: Exception) {
            ModelsConfig()
        }
    }

    fun sanitizeStreamChunk(chunk: String): String {
        return chunk.replace("[\\u4E00-\\u9FFF\\u3400-\\u4DBF\\uF900-\\uFAFF]".toRegex(), "")
    }

    fun getGeminiApiKey(): String {
        val geminiKey = BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() }
            ?: prefs.getString("gemini_api_key", null)?.trim()?.takeIf { it.isNotBlank() }
            ?: System.getenv("GEMINI_API_KEY") ?: ""
        return geminiKey.trim()
    }

    fun getGroqApiKey(): String {
        val groqKey = BuildConfig.GROQ_API_KEY.takeIf { it.isNotBlank() }
            ?: prefs.getString("groq_api_key", null)?.trim()?.takeIf { it.isNotBlank() }
            ?: System.getenv("GROQ_API_KEY") ?: ""
        return groqKey.trim()
    }

    fun saveApiKey(geminiKey: String, groqKey: String = getGroqApiKey()) {
        val trimmedGemini = geminiKey.trim()
        val trimmedGroq = groqKey.trim()
        prefs.edit()
            .putString("gemini_api_key", trimmedGemini)
            .putString("custom_api_key", trimmedGemini)
            .putString("groq_api_key", trimmedGroq)
            .apply()
        legacyPrefs.edit()
            .putString("gemini_api_key", trimmedGemini)
            .putString("custom_api_key", trimmedGemini)
            .apply()
    }

    fun getSavedEngineMode(): AiEngineMode {
        val saved = prefs.getString("engine_mode", AiEngineMode.FAST_MODE.name)
        return try {
            AiEngineMode.valueOf(saved ?: AiEngineMode.FAST_MODE.name)
        } catch (e: Exception) {
            AiEngineMode.FAST_MODE
        }
    }

    fun saveEngineMode(mode: AiEngineMode) {
        prefs.edit().putString("engine_mode", mode.name).apply()
    }

    fun getSavedTone(): DesiTone {
        val saved = prefs.getString("selected_tone", DesiTone.DESI_FRIEND.name)
        return try {
            DesiTone.valueOf(saved ?: DesiTone.DESI_FRIEND.name)
        } catch (e: Exception) {
            DesiTone.DESI_FRIEND
        }
    }

    fun saveTone(tone: DesiTone) {
        prefs.edit().putString("selected_tone", tone.name).apply()
    }

    fun saveChatHistory(messages: List<DesiMessage>) {
        try {
            val array = JSONArray()
            for (msg in messages.filter { !it.isLoading && it.text.isNotBlank() }) {
                val obj = JSONObject().apply {
                    put("id", msg.id)
                    put("text", sanitizeStreamChunk(msg.text))
                    put("isUser", msg.isUser)
                    put("timestamp", msg.timestamp)
                    put("imageUriString", msg.imageUriString ?: "")
                    put("tone", msg.tone.name)
                    put("engineMode", msg.engineMode.name)
                    if (msg.error != null) put("error", msg.error)
                }
                array.put(obj)
            }
            prefs.edit().putString("chat_history_json", array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadChatHistory(): List<DesiMessage> {
        val jsonStr = prefs.getString("chat_history_json", null) ?: return emptyList()
        val list = mutableListOf<DesiMessage>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val text = sanitizeStreamChunk(obj.optString("text", ""))
                if (text.contains("Core Insight | Next Step") ||
                    text.contains("Auralis Fast Synthesis") ||
                    text.contains("Auralis Deep Think Synthesis") ||
                    text.contains("Auralis Verified Sources") ||
                    text.contains("Namaste dost!") ||
                    text.contains("Please check your Gemini API key") ||
                    text.contains("Please add your Gemini API key") ||
                    text.contains("Connection interrupted. Please try")
                ) {
                    continue
                }
                val toneName = obj.optString("tone", DesiTone.DESI_FRIEND.name)
                val tone = try { DesiTone.valueOf(toneName) } catch (e: Exception) { DesiTone.DESI_FRIEND }
                val modeName = obj.optString("engineMode", AiEngineMode.FAST_MODE.name)
                val engineMode = try { AiEngineMode.valueOf(modeName) } catch (e: Exception) { AiEngineMode.FAST_MODE }
                val imageUri = obj.optString("imageUriString", "").ifBlank { null }
                val error = obj.optString("error", "").ifBlank { null }

                list.add(
                    DesiMessage(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        text = text,
                        isUser = obj.optBoolean("isUser", false),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        citations = emptyList(),
                        isLoading = false,
                        error = error,
                        imageUriString = imageUri,
                        tone = tone,
                        engineMode = engineMode
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun buildSystemPrompt(engineMode: AiEngineMode): String {
        return if (engineMode == AiEngineMode.DEEP_THINK) {
            "You are Auralis AI, an advanced reasoning assistant. Break down complex problems step-by-step, think deeply and rigorously, and provide clear, accurate, and natural answers without unnecessary boilerplate tables or filler. Never output Chinese characters."
        } else {
            "You are Auralis AI, a fast, helpful, and concise assistant. Respond naturally and directly like a conversational partner without unnecessary boilerplate tables or filler. Never output Chinese characters."
        }
    }

    private data class EncodedImagePayload(
        val mimeType: String,
        val base64Data: String
    )

    /**
     * Streams response using SSE (`?alt=sse`) and header authentication (`x-goog-api-key`):
     * - Fast Mode Primary: 'gemini-3.8-flash'
     *   URL: https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:streamGenerateContent?alt=sse
     * - Fast Mode Fallback: 'gemini-3.1-flash-lite'
     *   URL: https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:streamGenerateContent?alt=sse
     * - Deep Think Chain: sequentially attempts `deep_think_chain` (`openai/gpt-oss-120b` -> `qwen/qwen3.8-27b` -> `llama-3.3-70b-versatile`) via Groq API,
     *   failing over within <400ms on 404/429/deprecated status, and falling back to `deep_think_gemini_fallback` ('gemini-3.8-flash' -> 'gemini-3.1-flash-lite').
     */
    suspend fun streamResponse(
        prompt: String,
        imageUri: Uri?,
        engineMode: AiEngineMode,
        tone: DesiTone = DesiTone.DESI_FRIEND,
        conversationHistory: List<DesiMessage>,
        onToken: suspend (String) -> Unit
    ): Result<Pair<String, List<Citation>>> = withContext(Dispatchers.IO) {
        val geminiKey = BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() }
            ?: prefs.getString("gemini_api_key", null)?.trim()?.takeIf { it.isNotBlank() }
            ?: System.getenv("GEMINI_API_KEY") ?: ""

        val groqKey = BuildConfig.GROQ_API_KEY.takeIf { it.isNotBlank() }
            ?: prefs.getString("groq_api_key", null)?.trim()?.takeIf { it.isNotBlank() }
            ?: System.getenv("GROQ_API_KEY") ?: ""

        val encodedImage = encodeImageIfPresent(imageUri)
        val systemInstruction = buildSystemPrompt(engineMode)
        var streamedText: String? = null
        var lastErrorMessage: String? = null

        // 1. Deep Think Mode (text-only): Cascading `deep_think_chain` via Groq API
        if (engineMode == AiEngineMode.DEEP_THINK && encodedImage == null && groqKey.isNotBlank()) {
            val groqRes = tryGroqDeepThinkChain(
                apiKey = groqKey,
                prompt = prompt,
                systemInstruction = systemInstruction,
                history = conversationHistory,
                onToken = onToken
            )
            groqRes.onSuccess { streamedText = it }
                .onFailure { lastErrorMessage = it.localizedMessage }
        }

        // 2. Fast Mode (Text & Multimodal Images) or Deep Think Gemini Fallback -> Gemini SSE endpoints
        if (streamedText.isNullOrBlank() && geminiKey.isNotBlank()) {
            val geminiRes = tryGeminiSseEndpoints(
                apiKey = geminiKey,
                prompt = prompt,
                encodedImage = encodedImage,
                engineMode = engineMode,
                systemInstruction = systemInstruction,
                history = conversationHistory,
                onToken = onToken
            )
            geminiRes.onSuccess { streamedText = it }
                .onFailure { lastErrorMessage = it.localizedMessage }
        }

        // 3. Seamless Cloud SSE Proxy fallback (handles ambient AQ.* keys on external devices)
        if (streamedText.isNullOrBlank()) {
            val proxyRes = tryCloudSseProxy(
                prompt = prompt,
                encodedImage = encodedImage,
                engineMode = engineMode,
                history = conversationHistory,
                onToken = onToken
            )
            proxyRes.onSuccess { streamedText = it }
                .onFailure { if (lastErrorMessage == null) lastErrorMessage = it.localizedMessage }
        }

        val finalResponse = streamedText?.let { sanitizeStreamChunk(it).trim() }
        if (!finalResponse.isNullOrBlank()) {
            withContext(Dispatchers.Main) {
                onToken(finalResponse)
            }
            return@withContext Result.success(Pair(finalResponse, emptyList()))
        }

        val cleanError = lastErrorMessage
            ?.let { sanitizeStreamChunk(it).lines().firstOrNull()?.take(120) }
            ?.takeIf { it.isNotBlank() }
            ?: "Unable to reach AI service. Please check your internet connection."
        Result.failure(Exception(cleanError))
    }

    /**
     * Streams SSE from Gemini endpoints using 'x-goog-api-key' header and thinking_config.thinking_level = "medium".
     */
    private suspend fun tryGeminiSseEndpoints(
        apiKey: String,
        prompt: String,
        encodedImage: EncodedImagePayload?,
        engineMode: AiEngineMode,
        systemInstruction: String,
        history: List<DesiMessage>,
        onToken: suspend (String) -> Unit
    ): Result<String> {
        val payloadWithThinking = buildGeminiRequestPayload(
            prompt = prompt,
            encodedImage = encodedImage,
            systemInstruction = systemInstruction,
            history = history,
            includeThinkingConfig = true
        )
        val payloadStandard = buildGeminiRequestPayload(
            prompt = prompt,
            encodedImage = encodedImage,
            systemInstruction = systemInstruction,
            history = history,
            includeThinkingConfig = false
        )

        val primaryModel = if (engineMode == AiEngineMode.DEEP_THINK) {
            modelsConfig.deepThinkGeminiFallback
        } else {
            modelsConfig.fastPrimary
        }
        val fallbackModel = modelsConfig.fastFallback

        val primaryUrl =
            "https://generativelanguage.googleapis.com/v1beta/models/$primaryModel:streamGenerateContent?alt=sse"
        val fallbackUrl =
            "https://generativelanguage.googleapis.com/v1beta/models/$fallbackModel:streamGenerateContent?alt=sse"

        val attempts = listOf(
            Triple(primaryUrl, payloadWithThinking, 1200),
            Triple(fallbackUrl, payloadStandard, 15000),
            Triple("https://generativelanguage.googleapis.com/v1beta/models/${fallbackModel}-preview:streamGenerateContent?alt=sse", payloadStandard, 15000)
        )

        var lastError = "Unable to connect to Gemini stream."

        for ((endpointUrl, requestBody, timeoutMs) in attempts) {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(endpointUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("x-goog-api-key", apiKey)
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "text/event-stream")
                    setRequestProperty("Cache-Control", "no-cache")
                    doOutput = true
                    connectTimeout = timeoutMs
                    readTimeout = timeoutMs
                }

                OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                    writer.write(requestBody)
                    writer.flush()
                }

                val code = conn.responseCode
                if (code !in 200..299) {
                    try {
                        conn.errorStream?.use { it.readBytes() }
                    } catch (_: Exception) {
                    }
                    lastError = "HTTP $code"
                    continue
                }

                val accumulated = StringBuilder()
                BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { reader ->
                    val rawCollector = StringBuilder()
                    var line: String? = reader.readLine()
                    while (line != null) {
                        val trimmed = line.trim()
                        if (trimmed.startsWith(":")) {
                            line = reader.readLine()
                            continue
                        }
                        rawCollector.append(trimmed).append("\n")
                        if (trimmed.startsWith("data:")) {
                            val jsonStr = trimmed.removePrefix("data:").trim()
                            if (jsonStr.isNotEmpty() && jsonStr != "[DONE]") {
                                val delta = extractCandidateTextFromJson(jsonStr)
                                if (delta.isNotEmpty()) {
                                    accumulated.append(sanitizeStreamChunk(delta))
                                    val snapshot = accumulated.toString()
                                    withContext(Dispatchers.Main) {
                                        onToken(snapshot)
                                    }
                                }
                            }
                        }
                        line = reader.readLine()
                    }

                    if (accumulated.isEmpty() && rawCollector.isNotEmpty()) {
                        val fallbackParsed = sanitizeStreamChunk(parseGeminiBulkPayload(rawCollector.toString())).trim()
                        if (fallbackParsed.isNotEmpty()) {
                            accumulated.append(fallbackParsed)
                            withContext(Dispatchers.Main) {
                                onToken(fallbackParsed)
                            }
                        }
                    }
                }

                val finalStr = accumulated.toString().trim()
                if (finalStr.isNotBlank()) {
                    return Result.success(finalStr)
                }
            } catch (e: Exception) {
                lastError = e.localizedMessage ?: "Stream connection error."
            } finally {
                try {
                    conn?.disconnect()
                } catch (_: Exception) {
                }
            }
        }

        return Result.failure(Exception(lastError))
    }

    /**
     * Streams SSE from the AI Studio Cloud Run server proxy (`server.mjs`) if direct device calls fail.
     */
    private suspend fun tryCloudSseProxy(
        prompt: String,
        encodedImage: EncodedImagePayload?,
        engineMode: AiEngineMode,
        history: List<DesiMessage>,
        onToken: suspend (String) -> Unit
    ): Result<String> {
        val historyJson = JSONArray()
        for (msg in history.filter { !it.isLoading && it.text.isNotBlank() && it.error == null }.takeLast(8)) {
            historyJson.put(JSONObject().apply {
                put("role", if (msg.isUser) "user" else "model")
                put("text", sanitizeStreamChunk(msg.text))
            })
        }

        val payloadJson = JSONObject().apply {
            put("prompt", prompt)
            put("engineMode", engineMode.name)
            put("history", historyJson)
            if (encodedImage != null) {
                put("imageBase64", "data:${encodedImage.mimeType};base64,${encodedImage.base64Data}")
            }
        }.toString()

        var lastErr = "Service unavailable."
        for (proxyUrl in CLOUD_PROXY_ENDPOINTS) {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(proxyUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "text/event-stream")
                    doOutput = true
                    connectTimeout = 8000
                    readTimeout = 25000
                }

                OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                    writer.write(payloadJson)
                    writer.flush()
                }

                val code = conn.responseCode
                if (code !in 200..299) {
                    try {
                        conn.errorStream?.use { it.readBytes() }
                    } catch (_: Exception) {
                    }
                    continue
                }

                val accumulated = StringBuilder()
                BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        val trimmed = line.trim()
                        if (trimmed.startsWith(":")) {
                            line = reader.readLine()
                            continue
                        }
                        if (trimmed.startsWith("data:")) {
                            val jsonStr = trimmed.removePrefix("data:").trim()
                            if (jsonStr.isNotEmpty() && jsonStr != "[DONE]") {
                                val delta = extractCandidateTextFromJson(jsonStr)
                                if (delta.isNotEmpty()) {
                                    accumulated.append(sanitizeStreamChunk(delta))
                                    val snapshot = accumulated.toString()
                                    withContext(Dispatchers.Main) {
                                        onToken(snapshot)
                                    }
                                }
                            }
                        } else if (trimmed.startsWith("{") && trimmed.contains("\"reply\"")) {
                            val obj = JSONObject(trimmed)
                            val rep = sanitizeStreamChunk(obj.optString("reply", "")).trim()
                            if (rep.isNotEmpty()) {
                                accumulated.append(rep)
                                withContext(Dispatchers.Main) {
                                    onToken(rep)
                                }
                            }
                        }
                        line = reader.readLine()
                    }
                }

                val resultText = accumulated.toString().trim()
                if (resultText.isNotBlank()) {
                    return Result.success(resultText)
                }
            } catch (e: Exception) {
                lastErr = e.localizedMessage ?: "Network error."
            } finally {
                try {
                    conn?.disconnect()
                } catch (_: Exception) {
                }
            }
        }
        return Result.failure(Exception(lastErr))
    }

    /**
     * Deep Think Cascading Engine:
     * Sequentially attempts models in `modelsConfig.deepThinkChain`:
     *   1. "openai/gpt-oss-120b"
     *   2. "qwen/qwen3.8-27b"
     *   3. "llama-3.3-70b-versatile"
     * via Groq API (`https://api.groq.com/openai/v1/chat/completions`), failing over within <400ms on 404/429/deprecated.
     */
    private suspend fun tryGroqDeepThinkChain(
        apiKey: String,
        prompt: String,
        systemInstruction: String,
        history: List<DesiMessage>,
        onToken: suspend (String) -> Unit
    ): Result<String> {
        val messagesPayload = buildOpenAiMessages(systemInstruction, history, prompt)
        var lastError = "Deep Think Groq chain unavailable."

        for (candidateModel in modelsConfig.deepThinkChain) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL(GROQ_COMPLETIONS_ENDPOINT)
                conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $apiKey")
                    doOutput = true
                    connectTimeout = 3500
                    readTimeout = 15000
                }

                val payload = JSONObject().apply {
                    put("model", candidateModel)
                    put("messages", messagesPayload)
                    put("temperature", 0.5)
                    put("max_tokens", 2048)
                }

                OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                val code = conn.responseCode
                if (code !in 200..299) {
                    try {
                        conn.errorStream?.use { it.readBytes() }
                    } catch (_: Exception) {
                    }
                    lastError = "Groq ($candidateModel) HTTP $code"
                    continue
                }

                val body = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
                val json = JSONObject(body)
                val content = json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .optString("content", "")
                val sanitized = sanitizeStreamChunk(content).trim()
                if (sanitized.isNotBlank()) {
                    withContext(Dispatchers.Main) {
                        onToken(sanitized)
                    }
                    return Result.success(sanitized)
                }
            } catch (e: Exception) {
                lastError = e.localizedMessage ?: "Network error contacting Groq ($candidateModel)."
            } finally {
                try {
                    conn?.disconnect()
                } catch (_: Exception) {
                }
            }
        }
        return Result.failure(Exception(lastError))
    }

    private fun buildGeminiRequestPayload(
        prompt: String,
        encodedImage: EncodedImagePayload?,
        systemInstruction: String,
        history: List<DesiMessage>,
        includeThinkingConfig: Boolean
    ): String {
        val contentsArray = JSONArray()
        val validHistory = history
            .filter { !it.isLoading && it.text.isNotBlank() && it.error == null }
            .takeLast(10)
            .dropWhile { !it.isUser }

        var lastRole: String? = null
        for (msg in validHistory) {
            val role = if (msg.isUser) "user" else "model"
            val cleanText = sanitizeStreamChunk(msg.text).trim()
            if (cleanText.isEmpty()) continue

            if (role == lastRole && contentsArray.length() > 0) {
                val lastTurn = contentsArray.getJSONObject(contentsArray.length() - 1)
                lastTurn.getJSONArray("parts").put(JSONObject().put("text", "\n$cleanText"))
            } else {
                contentsArray.put(JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().put(JSONObject().put("text", cleanText)))
                })
                lastRole = role
            }
        }

        val currentParts = JSONArray()
        if (encodedImage != null) {
            currentParts.put(JSONObject().apply {
                put("inlineData", JSONObject().apply {
                    put("mimeType", encodedImage.mimeType)
                    put("data", encodedImage.base64Data)
                })
            })
        }

        val actualPrompt = prompt.ifBlank { "Describe and analyze this image in detail." }
        currentParts.put(JSONObject().put("text", actualPrompt))

        if (lastRole == "user" && contentsArray.length() > 0) {
            val lastTurn = contentsArray.getJSONObject(contentsArray.length() - 1)
            val existingParts = lastTurn.getJSONArray("parts")
            for (i in 0 until currentParts.length()) {
                existingParts.put(currentParts.getJSONObject(i))
            }
        } else {
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", currentParts)
            })
        }

        val generationConfigObj = JSONObject()
        if (includeThinkingConfig) {
            generationConfigObj.put(
                "thinking_config",
                JSONObject().put("thinking_level", "medium")
            )
        }

        return JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
            })
            put("contents", contentsArray)
            put("generationConfig", generationConfigObj)
        }.toString()
    }

    private fun extractCandidateTextFromJson(jsonStr: String): String {
        return try {
            val obj = JSONObject(jsonStr)
            if (obj.has("text") && !obj.has("candidates")) {
                return obj.optString("text", "")
            }
            val sb = StringBuilder()
            appendCandidateText(obj, sb)
            sb.toString()
        } catch (_: Exception) {
            ""
        }
    }

    private fun parseGeminiBulkPayload(rawBody: String): String {
        val trimmed = rawBody.trim()
        if (trimmed.isEmpty()) return ""
        val sb = StringBuilder()
        try {
            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    appendCandidateText(item, sb)
                }
            } else if (trimmed.startsWith("{")) {
                appendCandidateText(JSONObject(trimmed), sb)
            }
        } catch (_: Exception) {
        }
        return sb.toString()
    }

    private fun appendCandidateText(rootObj: JSONObject, out: StringBuilder) {
        val candidates = rootObj.optJSONArray("candidates") ?: return
        if (candidates.length() == 0) return
        val contentObj = candidates.optJSONObject(0)?.optJSONObject("content") ?: return
        val partsArray = contentObj.optJSONArray("parts") ?: return
        for (i in 0 until partsArray.length()) {
            val partObj = partsArray.optJSONObject(i) ?: continue
            if (partObj.optBoolean("thought", false)) continue
            if (partObj.has("text")) {
                out.append(partObj.optString("text", ""))
            }
        }
    }

    private fun buildOpenAiMessages(
        systemInstruction: String,
        history: List<DesiMessage>,
        currentPrompt: String
    ): JSONArray {
        val arr = JSONArray()
        arr.put(JSONObject().apply {
            put("role", "system")
            put("content", systemInstruction)
        })
        for (msg in history.filter { !it.isLoading && it.text.isNotBlank() && it.error == null }.takeLast(8)) {
            arr.put(JSONObject().apply {
                put("role", if (msg.isUser) "user" else "assistant")
                put("content", sanitizeStreamChunk(msg.text))
            })
        }
        arr.put(JSONObject().apply {
            put("role", "user")
            put("content", sanitizeStreamChunk(currentPrompt))
        })
        return arr
    }

    private fun encodeImageIfPresent(imageUri: Uri?): EncodedImagePayload? {
        if (imageUri == null) return null
        var originalBitmap: Bitmap? = null
        var scaledBitmap: Bitmap? = null
        return try {
            context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                originalBitmap = BitmapFactory.decodeStream(inputStream)
            }
            val bitmap = originalBitmap ?: return null

            val maxDimension = 1024
            val scale = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
            } else {
                1f
            }

            scaledBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt().coerceAtLeast(1),
                    (bitmap.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                bitmap
            }

            val base64String = ByteArrayOutputStream().use { out ->
                scaledBitmap?.compress(Bitmap.CompressFormat.JPEG, 85, out)
                out.flush()
                Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            }

            EncodedImagePayload(mimeType = "image/jpeg", base64Data = base64String)
        } catch (e: Exception) {
            null
        } finally {
            try {
                if (scaledBitmap != null && scaledBitmap != originalBitmap) {
                    scaledBitmap?.recycle()
                }
                originalBitmap?.recycle()
            } catch (_: Exception) {
            }
        }
    }
}
