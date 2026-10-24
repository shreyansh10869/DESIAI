package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

enum class AiEngineMode(val displayName: String, val badgeLabel: String) {
    FAST_MODE("Fast", "Fast"),
    DEEP_THINK("Deep Think", "Deep Think")
}

class AiEngineService(private val context: Context) {

    private val prefs = context.getSharedPreferences("auralis_ai_prefs", Context.MODE_PRIVATE)
    private val legacyPrefs = context.getSharedPreferences("desiai_prefs", Context.MODE_PRIVATE)

    fun sanitizeStreamChunk(chunk: String): String {
        return chunk.replace("[\\u4E00-\\u9FFF\\u3400-\\u4DBF\\uF900-\\uFAFF]".toRegex(), "")
    }

    fun getGeminiApiKey(): String {
        val customKey = prefs.getString("custom_api_key", "")?.trim().orEmpty()
        if (customKey.isNotBlank()) return customKey
        val legacyKey = legacyPrefs.getString("custom_api_key", "")?.trim().orEmpty()
        if (legacyKey.isNotBlank()) return legacyKey

        return try {
            val buildKey = BuildConfig.GEMINI_API_KEY
            if (buildKey.isNotBlank() && buildKey != "null") buildKey.trim() else ""
        } catch (e: Exception) {
            ""
        }
    }

    fun getGroqApiKey(): String {
        val customGroq = prefs.getString("groq_api_key", "")?.trim().orEmpty()
        if (customGroq.isNotBlank()) return customGroq
        return try {
            val buildKey = BuildConfig.GROQ_API_KEY
            if (buildKey.isNotBlank() && buildKey != "null") buildKey.trim() else ""
        } catch (e: Exception) {
            ""
        }
    }

    fun getLongCatApiKey(): String {
        val customLongCat = prefs.getString("longcat_api_key", "")?.trim().orEmpty()
        if (customLongCat.isNotBlank()) return customLongCat
        return try {
            val buildKey = BuildConfig.LONGCAT_API_KEY
            if (buildKey.isNotBlank() && buildKey != "null") buildKey.trim() else ""
        } catch (e: Exception) {
            ""
        }
    }

    fun saveApiKey(geminiKey: String, groqKey: String = getGroqApiKey()) {
        prefs.edit()
            .putString("custom_api_key", geminiKey.trim())
            .putString("groq_api_key", groqKey.trim())
            .apply()
        legacyPrefs.edit().putString("custom_api_key", geminiKey.trim()).apply()
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
                // Filter out any legacy cached dummy template responses from earlier versions
                if (text.contains("Core Insight | Next Step") ||
                    text.contains("Auralis Fast Synthesis") ||
                    text.contains("Auralis Deep Think Synthesis") ||
                    text.contains("Namaste dost!")
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
            "You are Auralis AI, a helpful, clear, and thoughtful assistant. Think carefully step-by-step and provide direct, natural responses without unnecessary boilerplate tables or filler. Never output Chinese characters."
        } else {
            "You are Auralis AI, a fast, helpful, and concise assistant. Respond naturally and directly like a conversational partner without unnecessary boilerplate tables or filler. Never output Chinese characters."
        }
    }

    /**
     * Calls the configured live AI API (Gemini 3.1 Flash-Lite / 2.5 Flash or Groq / LongCat)
     * and streams real generated text onto Dispatchers.Main.
     * Never outputs hardcoded dummy tables or canned mock templates.
     */
    suspend fun streamResponse(
        prompt: String,
        imageUri: Uri?,
        engineMode: AiEngineMode,
        tone: DesiTone = DesiTone.DESI_FRIEND,
        conversationHistory: List<DesiMessage>,
        onToken: suspend (String) -> Unit
    ): Result<Pair<String, List<Citation>>> = withContext(Dispatchers.IO) {
        val geminiKey = getGeminiApiKey()
        val groqKey = getGroqApiKey()
        val longCatKey = getLongCatApiKey()

        if (geminiKey.isBlank() && groqKey.isBlank() && longCatKey.isBlank()) {
            return@withContext Result.failure(
                Exception("Please add your API key in Settings to start chatting.")
            )
        }

        val base64Image = encodeImageIfPresent(imageUri)
        val systemInstruction = buildSystemPrompt(engineMode)
        var generatedText: String? = null
        var lastErrorMessage: String? = null

        // 1. Deep Think Mode: try primary reasoning (3.5s timeout), then Groq openai/gpt-oss-120b, then Gemini
        if (engineMode == AiEngineMode.DEEP_THINK && base64Image == null) {
            if (longCatKey.isNotBlank()) {
                val res = tryPrimaryReasoningWithTimeout(longCatKey, prompt, systemInstruction, conversationHistory)
                if (res != null) generatedText = res
            }

            if (generatedText == null && groqKey.isNotBlank()) {
                val res = tryGroqEngine(groqKey, prompt, systemInstruction, conversationHistory)
                res.onSuccess { generatedText = it }
                    .onFailure { lastErrorMessage = it.localizedMessage }
            }
        }

        // 2. Fast Mode or Deep Think fallback: call Gemini (gemini-3.1-flash-lite-preview / gemini-2.5-flash)
        if (generatedText == null && geminiKey.isNotBlank()) {
            val res = tryGeminiEngine(geminiKey, prompt, base64Image, engineMode, systemInstruction, conversationHistory)
            res.onSuccess { generatedText = it }
                .onFailure { lastErrorMessage = it.localizedMessage }
        }

        // 3. If Fast Mode was selected and user only provided Groq key, use Groq
        if (generatedText == null && groqKey.isNotBlank() && base64Image == null) {
            val res = tryGroqEngine(groqKey, prompt, systemInstruction, conversationHistory)
            res.onSuccess { generatedText = it }
                .onFailure { lastErrorMessage = it.localizedMessage }
        }

        val finalResponse = generatedText?.let { sanitizeStreamChunk(it).trim() }
        if (finalResponse.isNullOrBlank()) {
            val cleanError = lastErrorMessage
                ?.let { sanitizeStreamChunk(it).lines().firstOrNull()?.take(120) }
                ?.takeIf { it.isNotBlank() }
                ?: "Unable to reach AI service. Please check your API key in Settings."
            return@withContext Result.failure(Exception(cleanError))
        }

        // Stream the real API response smoothly onto Dispatchers.Main
        val words = finalResponse.split(Regex("(?<=\\s)"))
        val accumulated = StringBuilder()
        val batchSize = (words.size / 30).coerceIn(2, 8)
        var index = 0
        while (index < words.size) {
            val end = (index + batchSize).coerceAtMost(words.size)
            val chunk = words.subList(index, end).joinToString("")
                .replace("[\\u4E00-\\u9FFF\\u3400-\\u4DBF\\uF900-\\uFAFF]".toRegex(), "")
            accumulated.append(chunk)
            val snapshot = accumulated.toString()
            withContext(Dispatchers.Main) {
                onToken(snapshot)
            }
            index = end
            if (index < words.size) {
                delay(14L)
            }
        }

        Result.success(Pair(finalResponse, emptyList()))
    }

    private fun tryPrimaryReasoningWithTimeout(
        apiKey: String,
        prompt: String,
        systemInstruction: String,
        history: List<DesiMessage>
    ): String? {
        return try {
            val url = URL("https://api.longcat.chat/v1/chat/completions")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Authorization", "Bearer $apiKey")
                doOutput = true
                connectTimeout = 3500
                readTimeout = 3500
            }

            val payload = JSONObject().apply {
                put("model", "LongCat-2.0")
                put("messages", buildOpenAiMessages(systemInstruction, history, prompt))
                put("temperature", 0.5)
                put("max_tokens", 2048)
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val code = conn.responseCode
            if (code == 402 || code == 429 || code !in 200..299) return null

            val body = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
            val json = JSONObject(body)
            val content = json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .optString("content", "")
            sanitizeStreamChunk(content).takeIf { it.isNotBlank() }
        } catch (timeout: SocketTimeoutException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun tryGroqEngine(
        apiKey: String,
        prompt: String,
        systemInstruction: String,
        history: List<DesiMessage>
    ): Result<String> {
        val models = listOf("openai/gpt-oss-120b", "llama-3.3-70b-versatile")
        var lastErr = "Groq request failed."
        for (model in models) {
            try {
                val url = URL("https://api.groq.com/openai/v1/chat/completions")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Authorization", "Bearer $apiKey")
                    doOutput = true
                    connectTimeout = 8000
                    readTimeout = 18000
                }

                val payload = JSONObject().apply {
                    put("model", model)
                    put("messages", buildOpenAiMessages(systemInstruction, history, prompt))
                    put("temperature", 0.5)
                    put("max_tokens", 2048)
                }

                OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                val code = conn.responseCode
                if (code !in 200..299) {
                    lastErr = "Groq API error (HTTP $code)."
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
                    return Result.success(sanitized)
                }
            } catch (e: Exception) {
                lastErr = e.localizedMessage ?: "Network error contacting Groq."
            }
        }
        return Result.failure(Exception(lastErr))
    }

    private fun tryGeminiEngine(
        apiKey: String,
        prompt: String,
        base64Image: String?,
        engineMode: AiEngineMode,
        systemInstruction: String,
        history: List<DesiMessage>
    ): Result<String> {
        val candidateModels = listOf("gemini-3.1-flash-lite-preview", "gemini-2.5-flash")
        var lastError = "Gemini API request failed."

        for (modelName in candidateModels) {
            try {
                val urlString = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    doOutput = true
                    connectTimeout = 10000
                    readTimeout = 25000
                }

                val contentsArray = JSONArray()
                val validHistory = history.filter { !it.isLoading && it.text.isNotBlank() && it.error == null }.takeLast(10)
                var lastRole: String? = null
                for (msg in validHistory) {
                    val role = if (msg.isUser) "user" else "model"
                    if (role == lastRole && contentsArray.length() > 0) {
                        val lastTurn = contentsArray.getJSONObject(contentsArray.length() - 1)
                        lastTurn.getJSONArray("parts").put(JSONObject().put("text", "\n" + sanitizeStreamChunk(msg.text)))
                    } else {
                        contentsArray.put(JSONObject().apply {
                            put("role", role)
                            put("parts", JSONArray().put(JSONObject().put("text", sanitizeStreamChunk(msg.text))))
                        })
                        lastRole = role
                    }
                }

                val currentParts = JSONArray()
                if (base64Image != null) {
                    currentParts.put(JSONObject().apply {
                        put("inline_data", JSONObject().apply {
                            put("mime_type", "image/jpeg")
                            put("data", base64Image)
                        })
                    })
                }

                val actualPrompt = prompt.ifBlank { "Describe and analyze this image." }
                currentParts.put(JSONObject().put("text", actualPrompt))

                contentsArray.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", currentParts)
                })

                val jsonBody = JSONObject().apply {
                    put("system_instruction", JSONObject().apply {
                        put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
                    })
                    put("contents", contentsArray)
                    put("generationConfig", JSONObject().apply {
                        put("temperature", if (engineMode == AiEngineMode.DEEP_THINK) 0.4 else 0.7)
                        put("maxOutputTokens", 2048)
                    })
                }

                OutputStreamWriter(conn.outputStream, "UTF-8").use { os ->
                    os.write(jsonBody.toString())
                    os.flush()
                }

                val code = conn.responseCode
                if (code !in 200..299) {
                    lastError = if (code == 400 || code == 403) {
                        "Invalid Gemini API key. Please check Settings."
                    } else if (code == 429) {
                        "Rate limit reached (HTTP 429). Please try again shortly."
                    } else {
                        "AI service returned HTTP $code."
                    }
                    continue
                }

                val responseText = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
                val rootJson = JSONObject(responseText)
                val candidates = rootJson.optJSONArray("candidates") ?: continue
                if (candidates.length() == 0) continue

                val candidateObj = candidates.getJSONObject(0)
                val partsArray = candidateObj.getJSONObject("content").getJSONArray("parts")
                val sb = StringBuilder()
                for (i in 0 until partsArray.length()) {
                    val partObj = partsArray.getJSONObject(i)
                    if (partObj.has("text")) {
                        sb.append(partObj.getString("text"))
                    }
                }

                val sanitized = sanitizeStreamChunk(sb.toString()).trim()
                if (sanitized.isNotBlank()) {
                    return Result.success(sanitized)
                }
            } catch (e: Exception) {
                lastError = "Connection error: Unable to reach AI service."
            }
        }
        return Result.failure(Exception(lastError))
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

    private fun encodeImageIfPresent(imageUri: Uri?): String? {
        if (imageUri == null) return null
        return try {
            context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
                val maxDimension = 1024
                val scale = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                    maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
                } else 1f
                val scaledBitmap = if (scale < 1f) {
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * scale).toInt(),
                        (bitmap.height * scale).toInt(),
                        true
                    )
                } else bitmap
                val out = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            null
        }
    }
}
