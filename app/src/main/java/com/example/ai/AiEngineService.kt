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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

enum class AiEngineMode(val displayName: String, val badgeLabel: String) {
    FAST_MODE("Fast", "Fast"),
    DEEP_THINK("Deep Think", "Deep Think")
}

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val messages: List<DesiMessage> = emptyList()
)

data class ModelsConfig(
    val fastPrimary: String = "gemini-2.5-flash",
    val fastFallback: String = "gemini-1.5-flash",
    val fastActiveLite: String = "gemini-3.1-flash-lite",
    val deepThinkChain: List<String> = listOf("llama-3.3-70b-versatile"),
    val deepThinkGeminiFallback: String = "gemini-2.5-flash"
)

class AiEngineService(private val context: Context) {

    companion object {
        const val FAST_PRIMARY_MODEL = "gemini-2.5-flash"
        const val FAST_FALLBACK_MODEL = "gemini-1.5-flash"
        const val FAST_ACTIVE_LITE_MODEL = "gemini-3.1-flash-lite"
        const val DEEP_THINK_GROQ_MODEL = "llama-3.3-70b-versatile"

        const val GEMINI_PRIMARY_SSE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:streamGenerateContent?alt=sse"
        const val GEMINI_FALLBACK_SSE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:streamGenerateContent?alt=sse"
        const val GEMINI_ACTIVE_LITE_SSE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:streamGenerateContent?alt=sse"
        const val GROQ_COMPLETIONS_ENDPOINT =
            "https://api.groq.com/openai/v1/chat/completions"
        private const val UNIVERSAL_OPENAI_SSE_ENDPOINT =
            "https://text.pollinations.ai/openai"

        private val CONNECT_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(15).toInt()
        private val READ_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(60).toInt()

        private val unavailableEndpointUntil = ConcurrentHashMap<String, Long>()
        @Volatile
        private var isAmbientKeyRejectedExternally = false

        private fun isEndpointTemporarilyUnavailable(url: String): Boolean {
            val until = unavailableEndpointUntil[url] ?: return false
            if (System.currentTimeMillis() > until) {
                unavailableEndpointUntil.remove(url)
                return false
            }
            return true
        }

        private fun markEndpointUnavailable(url: String, ttlMs: Long = 600_000L) {
            unavailableEndpointUntil[url] = System.currentTimeMillis() + ttlMs
        }
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
                fastActiveLite = obj.optString("fast_active_lite", FAST_ACTIVE_LITE_MODEL).ifBlank { FAST_ACTIVE_LITE_MODEL },
                deepThinkChain = if (chainList.isNotEmpty()) chainList else listOf(DEEP_THINK_GROQ_MODEL),
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
            val array = serializeMessages(messages)
            prefs.edit().putString("chat_history_json", array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadChatHistory(): List<DesiMessage> {
        val jsonStr = prefs.getString("chat_history_json", null) ?: return emptyList()
        return try {
            deserializeMessages(JSONArray(jsonStr))
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadChatSessions(): List<ChatSession> {
        val raw = prefs.getString("chat_sessions_v2_json", null)
        val sessions = mutableListOf<ChatSession>()
        if (!raw.isNullOrBlank()) {
            try {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optString("id", UUID.randomUUID().toString())
                    val title = sanitizeStreamChunk(obj.optString("title", "Conversation")).ifBlank { "Conversation" }
                    val updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    val msgsArr = obj.optJSONArray("messages") ?: JSONArray()
                    val msgs = deserializeMessages(msgsArr)
                    if (msgs.isNotEmpty()) {
                        sessions.add(ChatSession(id = id, title = title, updatedAt = updatedAt, messages = msgs))
                    }
                }
            } catch (_: Exception) {
            }
        }
        if (sessions.isEmpty()) {
            val legacy = loadChatHistory()
            if (legacy.isNotEmpty()) {
                val firstUserText = legacy.firstOrNull { it.isUser }?.text?.take(38) ?: "Previous Chat"
                val migrated = ChatSession(
                    id = UUID.randomUUID().toString(),
                    title = firstUserText,
                    updatedAt = System.currentTimeMillis(),
                    messages = legacy
                )
                sessions.add(migrated)
                saveChatSessions(sessions)
            }
        }
        return sessions.sortedByDescending { it.updatedAt }
    }

    fun saveChatSessions(sessions: List<ChatSession>) {
        try {
            val arr = JSONArray()
            for (session in sessions.take(30)) {
                val cleanMessages = session.messages.filter { !it.isLoading && it.text.isNotBlank() }
                if (cleanMessages.isEmpty()) continue
                val obj = JSONObject().apply {
                    put("id", session.id)
                    put("title", sanitizeStreamChunk(session.title).take(48))
                    put("updatedAt", session.updatedAt)
                    put("messages", serializeMessages(cleanMessages))
                }
                arr.put(obj)
            }
            prefs.edit().putString("chat_sessions_v2_json", arr.toString()).apply()
        } catch (_: Exception) {
        }
    }

    private fun serializeMessages(messages: List<DesiMessage>): JSONArray {
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
        return array
    }

    private fun deserializeMessages(array: JSONArray): List<DesiMessage> {
        val list = mutableListOf<DesiMessage>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val text = sanitizeStreamChunk(obj.optString("text", ""))
            if (text.isBlank() ||
                text.contains("Core Insight | Next Step") ||
                text.contains("Auralis Fast Synthesis") ||
                text.contains("Auralis Deep Think Synthesis") ||
                text.contains("Auralis Verified Sources") ||
                text.contains("Namaste dost!") ||
                text.contains("Please check your Gemini API key") ||
                text.contains("Please add your Gemini API key") ||
                text.contains("Connection interrupted. Please try") ||
                text.contains("Service unavailable") ||
                text.contains("Unable to reach AI service")
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
                    id = obj.optString("id", UUID.randomUUID().toString()),
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
        return list
    }

    private fun buildSystemPrompt(engineMode: AiEngineMode): String {
        return if (engineMode == AiEngineMode.DEEP_THINK) {
            "You are Auralis AI, an advanced deep reasoning assistant. Break down complex problems step-by-step, analyze edge cases rigorously, and provide clear, well-structured, accurate answers without filler. Never output Chinese characters."
        } else {
            "You are Auralis AI, a fast, helpful, and natural conversational assistant. Respond clearly, directly, and concisely. Never output Chinese characters."
        }
    }

    private data class EncodedImagePayload(
        val mimeType: String,
        val base64Data: String
    )

    /**
     * Streams response using SSE (`?alt=sse`) and header authentication (`x-goog-api-key`):
     * - Fast Mode Primary: 'gemini-2.5-flash'
     *   URL: https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:streamGenerateContent?alt=sse
     * - Deep Think Engine: 'llama-3.3-70b-versatile' via Groq API (https://api.groq.com/openai/v1/chat/completions)
     * - Auto-Failover: If Groq key is blank or Deep Think fails, seamlessly falls back to 'gemini-2.5-flash'
     *   (and active Gemini/OpenAI SSE endpoints) with step-by-step reasoning system instruction without throwing connection errors.
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

        // 1. Deep Think Engine (Text-only): 'llama-3.3-70b-versatile' via Groq API
        if (engineMode == AiEngineMode.DEEP_THINK && encodedImage == null && groqKey.isNotBlank()) {
            val groqRes = tryGroqDeepThink(
                apiKey = groqKey,
                prompt = prompt,
                systemInstruction = systemInstruction,
                history = conversationHistory,
                onToken = onToken
            )
            groqRes.onSuccess { streamedText = it }
                .onFailure { lastErrorMessage = it.localizedMessage }
        }

        // 2. Fast Mode ('gemini-2.5-flash') & Deep Think Auto-Failover to Gemini SSE
        if (streamedText.isNullOrBlank() && geminiKey.isNotBlank() && !isAmbientKeyRejectedExternally) {
            val geminiRes = tryGeminiSseEndpoints(
                apiKey = geminiKey,
                prompt = prompt,
                encodedImage = encodedImage,
                systemInstruction = systemInstruction,
                history = conversationHistory,
                onToken = onToken
            )
            geminiRes.onSuccess { streamedText = it }
                .onFailure { lastErrorMessage = it.localizedMessage }
        }

        // 3. Universal Native APK Fallback (ensures physical Android phones work even when container AQ.* key is external)
        if (streamedText.isNullOrBlank()) {
            val universalRes = tryUniversalOpenAiSseStream(
                prompt = prompt,
                encodedImage = encodedImage,
                systemInstruction = systemInstruction,
                history = conversationHistory,
                onToken = onToken
            )
            universalRes.onSuccess { streamedText = it }
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
            ?: "Please check your internet connection and try again."
        Result.failure(Exception(cleanError))
    }

    /**
     * Streams SSE from Google Gemini endpoints using 'x-goog-api-key' header authentication
     * with 15s connectTimeout and 60s readTimeout, dispatching tokens onto Dispatchers.Main.
     */
    private suspend fun tryGeminiSseEndpoints(
        apiKey: String,
        prompt: String,
        encodedImage: EncodedImagePayload?,
        systemInstruction: String,
        history: List<DesiMessage>,
        onToken: suspend (String) -> Unit
    ): Result<String> {
        val requestBodyStandard = buildGeminiRequestPayload(
            prompt = prompt,
            encodedImage = encodedImage,
            systemInstruction = systemInstruction,
            history = history,
            lowThinkingForLite = false
        )
        val requestBodyLowThinking = buildGeminiRequestPayload(
            prompt = prompt,
            encodedImage = encodedImage,
            systemInstruction = systemInstruction,
            history = history,
            lowThinkingForLite = true
        )

        val candidateEndpoints = listOf(
            Pair(GEMINI_PRIMARY_SSE_URL, requestBodyStandard),
            Pair(GEMINI_FALLBACK_SSE_URL, requestBodyStandard),
            Pair(GEMINI_ACTIVE_LITE_SSE_URL, requestBodyLowThinking),
            Pair("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:streamGenerateContent?alt=sse", requestBodyStandard)
        )

        var lastError = "Gemini stream unavailable."

        for ((endpointUrl, requestBody) in candidateEndpoints) {
            if (isEndpointTemporarilyUnavailable(endpointUrl)) continue

            var conn: HttpURLConnection? = null
            try {
                conn = (URL(endpointUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("x-goog-api-key", apiKey)
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "text/event-stream")
                    setRequestProperty("Cache-Control", "no-cache")
                    doOutput = true
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                }

                OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                    writer.write(requestBody)
                    writer.flush()
                }

                val code = conn.responseCode
                if (code !in 200..299) {
                    val errText = try {
                        conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    } catch (_: Exception) {
                        ""
                    }
                    // If running on an external phone with an internal AQ.* session token, fail over immediately
                    if (apiKey.startsWith("AQ.") && (code == 400 || code == 401 || code == 403) &&
                        (errText.contains("API_KEY_INVALID", ignoreCase = true) || errText.contains("key not valid", ignoreCase = true))
                    ) {
                        isAmbientKeyRejectedExternally = true
                        return Result.failure(Exception("External device session"))
                    }
                    if (code == 404 || code == 400) {
                        markEndpointUnavailable(endpointUrl)
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
     * Deep Think Engine: 'llama-3.3-70b-versatile' via Groq API
     * (https://api.groq.com/openai/v1/chat/completions)
     */
    private suspend fun tryGroqDeepThink(
        apiKey: String,
        prompt: String,
        systemInstruction: String,
        history: List<DesiMessage>,
        onToken: suspend (String) -> Unit
    ): Result<String> {
        val messagesPayload = buildOpenAiMessages(systemInstruction, history, prompt, null)
        var lastError = "Groq Deep Think unavailable."

        for (candidateModel in modelsConfig.deepThinkChain) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL(GROQ_COMPLETIONS_ENDPOINT)
                conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $apiKey")
                    doOutput = true
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
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
                    lastError = "Groq HTTP $code"
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
                lastError = e.localizedMessage ?: "Network error contacting Groq."
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
     * Universal SSE Streaming Fallback for Native APK Builds on External Devices:
     * Streams line-by-line SSE tokens and dispatches directly onto Dispatchers.Main.
     */
    private suspend fun tryUniversalOpenAiSseStream(
        prompt: String,
        encodedImage: EncodedImagePayload?,
        systemInstruction: String,
        history: List<DesiMessage>,
        onToken: suspend (String) -> Unit
    ): Result<String> {
        var conn: HttpURLConnection? = null
        return try {
            val messagesArray = buildOpenAiMessages(systemInstruction, history, prompt, encodedImage)
            val requestJson = JSONObject().apply {
                put("model", "openai")
                put("stream", true)
                put("messages", messagesArray)
            }.toString()

            conn = (URL(UNIVERSAL_OPENAI_SSE_ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "text/event-stream")
                setRequestProperty("Cache-Control", "no-cache")
                doOutput = true
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(requestJson)
                writer.flush()
            }

            val code = conn.responseCode
            if (code !in 200..299) {
                return Result.failure(Exception("HTTP $code"))
            }

            val accumulated = StringBuilder()
            BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { reader ->
                val rawCollector = StringBuilder()
                var line: String? = reader.readLine()
                while (line != null) {
                    val trimmed = line.trim()
                    rawCollector.append(trimmed).append("\n")
                    if (trimmed.startsWith("data:")) {
                        val dataPayload = trimmed.removePrefix("data:").trim()
                        if (dataPayload.isNotEmpty() && dataPayload != "[DONE]") {
                            val delta = extractOpenAiDeltaContent(dataPayload)
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
                    try {
                        val obj = JSONObject(rawCollector.toString().trim())
                        val content = obj.optJSONArray("choices")
                            ?.optJSONObject(0)
                            ?.optJSONObject("message")
                            ?.optString("content", "")
                            .orEmpty()
                        val clean = sanitizeStreamChunk(content).trim()
                        if (clean.isNotEmpty()) {
                            accumulated.append(clean)
                            withContext(Dispatchers.Main) {
                                onToken(clean)
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
            }

            val finalResult = accumulated.toString().trim()
            if (finalResult.isNotBlank()) {
                Result.success(finalResult)
            } else {
                Result.failure(Exception("Empty stream response"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                conn?.disconnect()
            } catch (_: Exception) {
            }
        }
    }

    private fun extractOpenAiDeltaContent(jsonStr: String): String {
        return try {
            val obj = JSONObject(jsonStr)
            val choices = obj.optJSONArray("choices") ?: return ""
            if (choices.length() == 0) return ""
            val first = choices.optJSONObject(0) ?: return ""
            val delta = first.optJSONObject("delta")
            if (delta != null && delta.has("content") && !delta.isNull("content")) {
                return delta.optString("content", "")
            }
            val message = first.optJSONObject("message")
            if (message != null && message.has("content") && !message.isNull("content")) {
                return message.optString("content", "")
            }
            ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun buildGeminiRequestPayload(
        prompt: String,
        encodedImage: EncodedImagePayload?,
        systemInstruction: String,
        history: List<DesiMessage>,
        lowThinkingForLite: Boolean = false
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

        return JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
            })
            put("contents", contentsArray)
            if (lowThinkingForLite) {
                put("generationConfig", JSONObject().apply {
                    put("thinking_config", JSONObject().put("thinking_level", "low"))
                })
            }
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
        currentPrompt: String,
        encodedImage: EncodedImagePayload?
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
        if (encodedImage != null) {
            val contentParts = JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "text")
                    put("text", sanitizeStreamChunk(currentPrompt.ifBlank { "Describe and analyze this image in detail." }))
                })
                put(JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().apply {
                        put("url", "data:${encodedImage.mimeType};base64,${encodedImage.base64Data}")
                    })
                })
            }
            arr.put(JSONObject().apply {
                put("role", "user")
                put("content", contentParts)
            })
        } else {
            arr.put(JSONObject().apply {
                put("role", "user")
                put("content", sanitizeStreamChunk(currentPrompt))
            })
        }
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
