package com.example.ai

import android.content.Context
import android.net.Uri

enum class DesiTone(val displayName: String, val badgeText: String, val iconEmoji: String) {
    DESI_FRIEND("Standard", "STANDARD", ""),
    EXAM_MODE("Academic", "ACADEMIC", "")
}

enum class ResponseStyle(val displayName: String, val promptInstruction: String) {
    SHORT(
        displayName = "Short",
        promptInstruction = "Response style: Keep replies brief, direct, and concise (under 4 sentences unless code is requested)."
    ),
    BALANCED(
        displayName = "Balanced",
        promptInstruction = "Response style: Provide a balanced, clear, and well-structured response."
    ),
    DETAILED(
        displayName = "Detailed",
        promptInstruction = "Response style: Provide thorough, comprehensive, in-depth explanations covering all nuances and examples."
    )
}

data class Citation(
    val title: String,
    val sourceOrContext: String,
    val keyTakeaway: String,
    val url: String = ""
)

data class DesiMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val citations: List<Citation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val imageUriString: String? = null,
    val tone: DesiTone = DesiTone.DESI_FRIEND,
    val engineMode: AiEngineMode = AiEngineMode.FAST_MODE,
    val isTruncated: Boolean = false
)

class DesiAIService(context: Context) {
    private val engine = AiEngineService(context)

    fun getApiKey(): String = engine.getGeminiApiKey()
    fun saveApiKey(key: String) = engine.saveApiKey(key)
    fun getSavedTone(): DesiTone = engine.getSavedTone()
    fun saveTone(tone: DesiTone) = engine.saveTone(tone)
    fun saveChatHistory(messages: List<DesiMessage>) = engine.saveChatHistory(messages)
    fun loadChatHistory(): List<DesiMessage> = engine.loadChatHistory()

    suspend fun generateDesiResponse(
        prompt: String,
        imageUri: Uri?,
        tone: DesiTone,
        conversationHistory: List<DesiMessage>
    ): Result<Pair<String, List<Citation>>> {
        return engine.streamResponse(
            prompt = prompt,
            imageUri = imageUri,
            engineMode = engine.getSavedEngineMode(),
            tone = tone,
            conversationHistory = conversationHistory,
            onToken = {}
        )
    }
}
