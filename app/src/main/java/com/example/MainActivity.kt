package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.ai.AiEngineMode
import com.example.ai.AiEngineService
import com.example.ai.ChatSession
import com.example.ai.DesiMessage
import com.example.ai.DesiTone
import com.example.ai.ResponseStyle
import com.example.ui.RichDesiMarkdownText
import com.example.ui.SynthesizingPulsePill
import com.example.ui.theme.AuralisAITheme
import com.example.ui.theme.ChatAccentWhite
import com.example.ui.theme.ChatDarkBg
import com.example.ui.theme.ChatSurfaceDock
import com.example.ui.theme.ChatTextMuted
import com.example.ui.theme.ChatTextPrimary
import com.example.ui.theme.ChatTextSecondary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID

private val ChatUserBubble1F1F1F = Color(0xFF1F1F1F)
private val ChatDockSurface1E1E1E = Color(0xFF1E1E1E)
private val ChatDockBorder2E2E2E = Color(0xFF2E2E2E)
private val ChatDrawerSurface = Color(0xFF121212)

private val FastStarterChips = listOf(
    "Explain quantum computing in simple terms",
    "Give me a 15-minute high-protein dinner idea",
    "Draft a polite email to reschedule a meeting",
    "What are the best habits for deep focus?"
)

private val DeepThinkStarterChips = listOf(
    "Calculate (14892 * 3741) - (8923 * 4129) and verify every step",
    "Write a thread-safe LRU cache in Kotlin with a runnable test case",
    "3 switches outside a closed room control 3 bulbs inside. How do you find which is which in 1 trip?",
    "Find all 3-digit numbers equal to the sum of the cubes of their digits"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AuralisAITheme {
                AuralisAIMainScreen()
            }
        }
    }
}

@Composable
fun DesiAIMainScreen() {
    AuralisAIMainScreen()
}

private fun checkIsOnline(context: Context): Boolean {
    return try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (_: Exception) {
        true
    }
}

@Composable
fun AuralisAIMainScreen() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val aiEngine = remember { AiEngineService(context) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var inputPrompt by rememberSaveable { mutableStateOf("") }
    var isSynthesizing by remember { mutableStateOf(false) }
    var activeStreamJob by remember { mutableStateOf<Job?>(null) }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var drawerSearchQuery by rememberSaveable { mutableStateOf("") }

    var selectedEngineMode by remember { mutableStateOf(aiEngine.getSavedEngineMode()) }
    val responseStyle by aiEngine.responseStyleFlow.collectAsState(initial = aiEngine.getSavedResponseStyle())

    var attachedImageUri by remember { mutableStateOf<Uri?>(null) }
    var currentlySpeakingId by remember { mutableStateOf<String?>(null) }

    // Long-press dialogs state
    var selectedAiMessageForActions by remember { mutableStateOf<DesiMessage?>(null) }
    var editingUserMessage by remember { mutableStateOf<DesiMessage?>(null) }
    var editingUserText by remember { mutableStateOf("") }

    // Network connectivity state for Offline Banner
    var isOffline by remember { mutableStateOf(!checkIsOnline(context)) }
    DisposableEffect(context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOffline = false
            }

            override fun onLost(network: Network) {
                isOffline = !checkIsOnline(context)
            }
        }
        try {
            val req = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm?.registerNetworkCallback(req, callback)
        } catch (_: Exception) {
        }
        onDispose {
            try {
                cm?.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
            }
        }
    }

    // Previous sessions are loaded into the Navigation Drawer's 'Recent Chats' list.
    val chatSessions = remember {
        mutableStateListOf<ChatSession>().apply {
            addAll(aiEngine.loadChatSessions())
        }
    }
    val titledSessionIds = remember { mutableStateListOf<String>() }

    // Separate Chat Contexts for Fast & Deep Think, starting fresh on Cold Start ('New Chat' screen)
    var fastSessionId by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    var deepThinkSessionId by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }

    val fastChatMessages = remember { mutableStateListOf<DesiMessage>() }
    val deepThinkMessages = remember { mutableStateListOf<DesiMessage>() }

    val activeMessages: SnapshotStateList<DesiMessage> =
        if (selectedEngineMode == AiEngineMode.FAST_MODE) fastChatMessages else deepThinkMessages
    val activeSessionId: String =
        if (selectedEngineMode == AiEngineMode.FAST_MODE) fastSessionId else deepThinkSessionId

    var actionButtonPressed by remember { mutableStateOf(false) }
    val animatedActionScale by animateFloatAsState(
        targetValue = if (actionButtonPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "send_button_scale"
    )

    fun triggerHaptic(strong: Boolean = false) {
        try {
            if (strong) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(VibratorManager::class.java)?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(40)
                }
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        } catch (_: Exception) {
        }
    }

    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.forLanguageTag("hi-IN")
            }
        }
        ttsEngine = tts
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedImageUri = uri
            triggerHaptic(strong = false)
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        val spokenText = matches?.firstOrNull()?.trim().orEmpty()
        if (spokenText.isNotBlank()) {
            inputPrompt = spokenText
        }
    }

    fun persistModeSession(
        sessionId: String,
        modeMessages: List<DesiMessage>,
        mode: AiEngineMode,
        overrideTitle: String? = null
    ) {
        val validMessages = modeMessages.filter { !it.isLoading && !aiEngine.isErrorTurnMessage(it) }
        if (validMessages.isEmpty()) return

        aiEngine.saveChatHistory(validMessages)

        val existingIdx = chatSessions.indexOfFirst { it.id == sessionId }
        val existingTitle = if (existingIdx != -1) chatSessions[existingIdx].title else null

        val fallbackTitle = validMessages.firstOrNull { it.isUser }?.text
            ?.replace("\n", " ")
            ?.trim()
            ?.take(38)
            ?.ifBlank { "New Conversation" }
            ?: "New Conversation"

        val finalTitle = overrideTitle ?: existingTitle ?: fallbackTitle

        val updatedSession = ChatSession(
            id = sessionId,
            title = finalTitle,
            updatedAt = System.currentTimeMillis(),
            messages = validMessages,
            engineMode = mode
        )
        if (existingIdx != -1) {
            chatSessions[existingIdx] = updatedSession
        } else {
            chatSessions.add(0, updatedSession)
        }
        aiEngine.saveChatSessions(chatSessions)

        // Auto 3-5 word title generation after the first AI reply completes
        val aiReplies = validMessages.filter { !it.isUser }
        val firstUser = validMessages.firstOrNull { it.isUser }
        if (aiReplies.size == 1 && firstUser != null && sessionId !in titledSessionIds) {
            titledSessionIds.add(sessionId)
            val firstUserText = firstUser.text
            val firstAiText = aiReplies.first().text
            scope.launch {
                val autoTitle = aiEngine.generateChatTitle(firstUserText, firstAiText)
                if (autoTitle.isNotBlank()) {
                    val idx = chatSessions.indexOfFirst { it.id == sessionId }
                    if (idx != -1) {
                        chatSessions[idx] = chatSessions[idx].copy(title = autoTitle)
                        aiEngine.saveChatSessions(chatSessions)
                    }
                }
            }
        }
    }

    fun stopStreaming() {
        if (!isSynthesizing) return
        triggerHaptic(strong = false)
        aiEngine.cancelActiveStream()
        activeStreamJob?.cancel()
        activeStreamJob = null

        val targetList = if (selectedEngineMode == AiEngineMode.FAST_MODE) fastChatMessages else deepThinkMessages
        val sessionId = if (selectedEngineMode == AiEngineMode.FAST_MODE) fastSessionId else deepThinkSessionId
        val loadingIdx = targetList.indexOfLast { !it.isUser && it.isLoading }
        if (loadingIdx != -1) {
            val currentMsg = targetList[loadingIdx]
            if (currentMsg.text.isBlank()) {
                targetList.removeAt(loadingIdx)
            } else {
                targetList[loadingIdx] = currentMsg.copy(
                    isLoading = false,
                    error = null
                )
                persistModeSession(sessionId, targetList, selectedEngineMode)
            }
        }
        isSynthesizing = false
    }

    fun startNewChat() {
        if (isSynthesizing) {
            stopStreaming()
        }
        triggerHaptic(strong = false)
        ttsEngine?.stop()
        currentlySpeakingId = null
        if (selectedEngineMode == AiEngineMode.FAST_MODE) {
            if (fastChatMessages.isNotEmpty()) {
                persistModeSession(fastSessionId, fastChatMessages, AiEngineMode.FAST_MODE)
            }
            fastSessionId = UUID.randomUUID().toString()
            fastChatMessages.clear()
        } else {
            if (deepThinkMessages.isNotEmpty()) {
                persistModeSession(deepThinkSessionId, deepThinkMessages, AiEngineMode.DEEP_THINK)
            }
            deepThinkSessionId = UUID.randomUUID().toString()
            deepThinkMessages.clear()
        }
    }

    fun openChatSession(session: ChatSession) {
        if (isSynthesizing) {
            stopStreaming()
        }
        triggerHaptic(strong = false)
        ttsEngine?.stop()
        currentlySpeakingId = null

        val targetMode = session.engineMode
        selectedEngineMode = targetMode
        aiEngine.saveEngineMode(targetMode)

        if (targetMode == AiEngineMode.FAST_MODE) {
            fastSessionId = session.id
            fastChatMessages.clear()
            fastChatMessages.addAll(session.messages)
        } else {
            deepThinkSessionId = session.id
            deepThinkMessages.clear()
            deepThinkMessages.addAll(session.messages)
        }
    }

    fun deleteChatSession(session: ChatSession) {
        triggerHaptic(strong = false)
        chatSessions.removeAll { it.id == session.id }
        aiEngine.saveChatSessions(chatSessions)
        if (fastSessionId == session.id) {
            fastSessionId = UUID.randomUUID().toString()
            fastChatMessages.clear()
        }
        if (deepThinkSessionId == session.id) {
            deepThinkSessionId = UUID.randomUUID().toString()
            deepThinkMessages.clear()
        }
    }

    fun speakMessage(message: DesiMessage) {
        triggerHaptic(strong = false)
        val tts = ttsEngine ?: return
        if (currentlySpeakingId == message.id) {
            tts.stop()
            currentlySpeakingId = null
        } else {
            tts.stop()
            currentlySpeakingId = message.id
            val speechText = message.text
                .replace("*", "")
                .replace("#", "")
                .take(800)
            tts.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, message.id)
        }
    }

    fun shareMessage(text: String) {
        triggerHaptic(strong = false)
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share"))
        } catch (_: Exception) {
        }
    }

    fun exportCurrentChatAsMarkdown() {
        triggerHaptic(strong = false)
        val validMessages = activeMessages.filter { !it.isLoading && !aiEngine.isErrorTurnMessage(it) }
        val currentSessionTitle = chatSessions.firstOrNull { it.id == activeSessionId }?.title
            ?: "${AiEngineService.APP_NAME} Conversation"

        val mdBuilder = StringBuilder()
        mdBuilder.append("# ").append(currentSessionTitle).append("\n\n")
        mdBuilder.append("_Mode: ").append(selectedEngineMode.displayName)
            .append(" • Style: ").append(responseStyle.displayName).append("_\n\n---\n\n")

        if (validMessages.isEmpty()) {
            mdBuilder.append("_(No messages in this conversation yet.)_\n")
        } else {
            for (msg in validMessages) {
                val speaker = if (msg.isUser) "**You**" else "**${AiEngineService.APP_NAME}**"
                mdBuilder.append(speaker).append(":\n\n")
                mdBuilder.append(msg.text.trim()).append("\n\n---\n\n")
            }
        }

        val mdContent = mdBuilder.toString()
        try {
            val safeFileName = currentSessionTitle
                .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                .take(32)
                .ifBlank { "auralis_chat" } + ".md"
            val exportFile = File(context.cacheDir, safeFileName)
            exportFile.writeText(mdContent, Charsets.UTF_8)
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                exportFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/markdown"
                putExtra(Intent.EXTRA_SUBJECT, currentSessionTitle)
                putExtra(Intent.EXTRA_TEXT, mdContent)
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export Chat (.md)"))
        } catch (_: Exception) {
            shareMessage(mdContent)
        }
    }

    fun executeStreamTurn(
        promptText: String,
        imageToSend: Uri?,
        modeAtSend: AiEngineMode,
        targetList: SnapshotStateList<DesiMessage>,
        sessionIdAtSend: String,
        historySnapshot: List<DesiMessage>,
        assistantMsgId: String,
        existingPrefix: String = ""
    ) {
        isSynthesizing = true
        val job = scope.launch {
            try {
                withContext(Dispatchers.Main) {
                    if (targetList.isNotEmpty()) {
                        listState.animateScrollToItem(targetList.size - 1)
                    }
                }

                val result = aiEngine.streamResponse(
                    prompt = promptText,
                    imageUri = imageToSend,
                    engineMode = modeAtSend,
                    responseStyle = responseStyle,
                    conversationHistory = historySnapshot,
                    onToken = { partialText ->
                        withContext(Dispatchers.Main) {
                            val sanitizedPartial = partialText.replace(
                                "[\\u4E00-\\u9FFF\\u3400-\\u4DBF\\uF900-\\uFAFF]".toRegex(),
                                ""
                            )
                            val combined = if (existingPrefix.isNotEmpty()) {
                                existingPrefix + sanitizedPartial
                            } else {
                                sanitizedPartial
                            }
                            val idx = targetList.indexOfFirst { it.id == assistantMsgId }
                            if (idx != -1) {
                                targetList[idx] = targetList[idx].copy(
                                    text = combined,
                                    isLoading = true,
                                    error = null
                                )
                            }
                        }
                    }
                )

                withContext(Dispatchers.Main) {
                    val idx = targetList.indexOfFirst { it.id == assistantMsgId }
                    result.onSuccess { (finalText, _) ->
                        val cleanFinal = finalText.replace(
                            "[\\u4E00-\\u9FFF\\u3400-\\u4DBF\\uF900-\\uFAFF]".toRegex(),
                            ""
                        )
                        val combinedFinal = if (existingPrefix.isNotEmpty()) {
                            existingPrefix + cleanFinal
                        } else {
                            cleanFinal
                        }
                        if (idx != -1) {
                            targetList[idx] = targetList[idx].copy(
                                text = combinedFinal,
                                isLoading = false,
                                error = null,
                                isTruncated = aiEngine.wasLastStreamTruncated
                            )
                        }
                        persistModeSession(sessionIdAtSend, targetList, modeAtSend)
                    }.onFailure {
                        // Never save failed turns to history and never show error Toasts;
                        // show inline "Couldn't reply. Retry" under the failed turn.
                        if (idx != -1) {
                            if (existingPrefix.isNotEmpty()) {
                                targetList[idx] = targetList[idx].copy(
                                    text = existingPrefix,
                                    isLoading = false,
                                    error = null,
                                    isTruncated = true
                                )
                            } else {
                                targetList[idx] = targetList[idx].copy(
                                    text = "Couldn't reply.",
                                    isLoading = false,
                                    error = "Couldn't reply."
                                )
                            }
                        }
                    }
                    if (targetList.isNotEmpty()) {
                        listState.animateScrollToItem(targetList.size - 1)
                    }
                }
            } catch (_: CancellationException) {
                withContext(Dispatchers.Main) {
                    val idx = targetList.indexOfFirst { it.id == assistantMsgId }
                    if (idx != -1) {
                        val current = targetList[idx]
                        if (current.text.isBlank()) {
                            targetList.removeAt(idx)
                        } else {
                            targetList[idx] = current.copy(isLoading = false, error = null)
                            persistModeSession(sessionIdAtSend, targetList, modeAtSend)
                        }
                    }
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isSynthesizing = false
                    activeStreamJob = null
                }
            }
        }
        activeStreamJob = job
    }

    fun sendMessage(userText: String) {
        val trimmed = aiEngine.sanitizeStreamChunk(userText.trim())
        val imageToSend = attachedImageUri
        if (trimmed.isBlank() && imageToSend == null) return
        if (isSynthesizing) return

        triggerHaptic(strong = false)
        val modeAtSend = selectedEngineMode
        val targetList = if (modeAtSend == AiEngineMode.FAST_MODE) fastChatMessages else deepThinkMessages
        val sessionIdAtSend = if (modeAtSend == AiEngineMode.FAST_MODE) fastSessionId else deepThinkSessionId

        // Strip any failed/error turns before snapshotting history so they never poison future API calls
        targetList.removeAll { !it.isUser && aiEngine.isErrorTurnMessage(it) }
        val historySnapshot = targetList.toList()

        val userMessage = DesiMessage(
            text = trimmed.ifBlank { "Attached image" },
            isUser = true,
            imageUriString = imageToSend?.toString(),
            tone = DesiTone.DESI_FRIEND,
            engineMode = modeAtSend
        )
        targetList.add(userMessage)
        inputPrompt = ""
        attachedImageUri = null

        val assistantMsgId = UUID.randomUUID().toString()
        val assistantPlaceholder = DesiMessage(
            id = assistantMsgId,
            text = "",
            isUser = false,
            isLoading = true,
            tone = DesiTone.DESI_FRIEND,
            engineMode = modeAtSend
        )
        targetList.add(assistantPlaceholder)

        executeStreamTurn(
            promptText = trimmed,
            imageToSend = imageToSend,
            modeAtSend = modeAtSend,
            targetList = targetList,
            sessionIdAtSend = sessionIdAtSend,
            historySnapshot = historySnapshot,
            assistantMsgId = assistantMsgId
        )
    }

    fun retryOrRegenerateAiMessage(aiMessage: DesiMessage) {
        if (isSynthesizing) return
        triggerHaptic(strong = false)
        val modeAtSend = selectedEngineMode
        val targetList = if (modeAtSend == AiEngineMode.FAST_MODE) fastChatMessages else deepThinkMessages
        val sessionIdAtSend = if (modeAtSend == AiEngineMode.FAST_MODE) fastSessionId else deepThinkSessionId

        val aiIdx = targetList.indexOfFirst { it.id == aiMessage.id }
        if (aiIdx == -1) return

        // Find preceding user message
        val precedingUserIdx = (aiIdx - 1 downTo 0).firstOrNull { targetList[it].isUser } ?: return
        val userMsg = targetList[precedingUserIdx]

        // Remove everything after the preceding user message
        while (targetList.size > precedingUserIdx + 1) {
            targetList.removeAt(targetList.size - 1)
        }

        val historySnapshot = targetList.subList(0, precedingUserIdx).filter { !aiEngine.isErrorTurnMessage(it) }
        val newAssistantId = UUID.randomUUID().toString()
        targetList.add(
            DesiMessage(
                id = newAssistantId,
                text = "",
                isUser = false,
                isLoading = true,
                tone = DesiTone.DESI_FRIEND,
                engineMode = modeAtSend
            )
        )

        val imgUri = userMsg.imageUriString?.let { Uri.parse(it) }
        val promptToUse = if (userMsg.text == "Attached image") "" else userMsg.text

        executeStreamTurn(
            promptText = promptToUse,
            imageToSend = imgUri,
            modeAtSend = modeAtSend,
            targetList = targetList,
            sessionIdAtSend = sessionIdAtSend,
            historySnapshot = historySnapshot,
            assistantMsgId = newAssistantId
        )
    }

    fun continueTruncatedAiMessage(aiMessage: DesiMessage) {
        if (isSynthesizing) return
        triggerHaptic(strong = false)
        val modeAtSend = selectedEngineMode
        val targetList = if (modeAtSend == AiEngineMode.FAST_MODE) fastChatMessages else deepThinkMessages
        val sessionIdAtSend = if (modeAtSend == AiEngineMode.FAST_MODE) fastSessionId else deepThinkSessionId

        val aiIdx = targetList.indexOfFirst { it.id == aiMessage.id }
        if (aiIdx == -1) return

        val existingText = aiMessage.text
        val historySnapshot = targetList.take(aiIdx + 1).toList()

        targetList[aiIdx] = aiMessage.copy(
            isLoading = true,
            isTruncated = false,
            error = null
        )

        val joinPrefix = if (existingText.endsWith("\n") || existingText.endsWith(" ")) {
            existingText
        } else {
            "$existingText "
        }

        executeStreamTurn(
            promptText = "Continue from the exact point where you stopped, without repeating what you already wrote.",
            imageToSend = null,
            modeAtSend = modeAtSend,
            targetList = targetList,
            sessionIdAtSend = sessionIdAtSend,
            historySnapshot = historySnapshot,
            assistantMsgId = aiMessage.id,
            existingPrefix = joinPrefix
        )
    }

    fun confirmEditAndResendUserMessage(originalMessage: DesiMessage, updatedText: String) {
        val cleanText = aiEngine.sanitizeStreamChunk(updatedText.trim())
        if (cleanText.isBlank() || isSynthesizing) return
        triggerHaptic(strong = false)

        val modeAtSend = selectedEngineMode
        val targetList = if (modeAtSend == AiEngineMode.FAST_MODE) fastChatMessages else deepThinkMessages
        val idx = targetList.indexOfFirst { it.id == originalMessage.id }
        if (idx != -1) {
            while (targetList.size > idx) {
                targetList.removeAt(targetList.size - 1)
            }
        }
        attachedImageUri = originalMessage.imageUriString?.let { Uri.parse(it) }
        sendMessage(cleanText)
    }

    // Long-press AI Message Action Dialog (Copy, Regenerate, Share)
    selectedAiMessageForActions?.let { targetAiMsg ->
        AlertDialog(
            onDismissRequest = { selectedAiMessageForActions = null },
            containerColor = Color(0xFF161616),
            titleContentColor = ChatTextPrimary,
            textContentColor = ChatTextSecondary,
            title = {
                Text(
                    text = "Message Actions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF202020))
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText(AiEngineService.APP_NAME, targetAiMsg.text))
                                selectedAiMessageForActions = null
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy",
                            tint = ChatTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Copy", color = ChatTextPrimary, fontSize = 14.5.sp)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF202020))
                            .clickable {
                                val msgToRegen = targetAiMsg
                                selectedAiMessageForActions = null
                                retryOrRegenerateAiMessage(msgToRegen)
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Regenerate",
                            tint = ChatTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Regenerate", color = ChatTextPrimary, fontSize = 14.5.sp)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF202020))
                            .clickable {
                                val textToShare = targetAiMsg.text
                                selectedAiMessageForActions = null
                                shareMessage(textToShare)
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share",
                            tint = ChatTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Share", color = ChatTextPrimary, fontSize = 14.5.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedAiMessageForActions = null }) {
                    Text("Close", color = ChatTextSecondary)
                }
            }
        )
    }

    // Long-press User Message Edit & Resend Dialog
    editingUserMessage?.let { userMsgToEdit ->
        AlertDialog(
            onDismissRequest = { editingUserMessage = null },
            containerColor = Color(0xFF161616),
            titleContentColor = ChatTextPrimary,
            textContentColor = ChatTextSecondary,
            title = {
                Text(
                    text = "Edit & Resend",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            },
            text = {
                OutlinedTextField(
                    value = editingUserText,
                    onValueChange = { editingUserText = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ChatTextPrimary,
                        unfocusedTextColor = ChatTextPrimary,
                        focusedBorderColor = ChatAccentWhite,
                        unfocusedBorderColor = Color(0xFF333333),
                        cursorColor = ChatAccentWhite
                    )
                )
            },
            dismissButton = {
                TextButton(onClick = { editingUserMessage = null }) {
                    Text("Cancel", color = ChatTextSecondary)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updated = editingUserText
                        editingUserMessage = null
                        confirmEditAndResendUserMessage(userMsgToEdit, updated)
                    }
                ) {
                    Text("Resend", color = ChatAccentWhite, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    if (showSettingsSheet) {
        val cfg = aiEngine.modelsConfig
        AlertDialog(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = Color(0xFF161616),
            titleContentColor = ChatTextPrimary,
            textContentColor = ChatTextSecondary,
            title = {
                Text(
                    text = "${AiEngineService.APP_NAME} Settings",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Fast Mode: ${cfg.fastPrimary} → ${cfg.fastFallback} (Temp 0.7)",
                        style = MaterialTheme.typography.bodySmall,
                        color = ChatTextSecondary
                    )
                    Text(
                        text = "Deep Think Engine: ${cfg.deepThinkChain.joinToString(" → ")} (Temp 0.3, Math Tool Enabled)",
                        style = MaterialTheme.typography.bodySmall,
                        color = ChatTextSecondary
                    )
                    Text(
                        text = "Response Style: ${responseStyle.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ChatTextPrimary
                    )
                    Text(
                        text = "API Keys: Pre-configured via BuildConfig (Active)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8BE28B)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsSheet = false }) {
                    Text("Close", color = ChatTextPrimary)
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatGptSidebarDrawer(
                sessions = chatSessions,
                currentSessionId = activeSessionId,
                searchQuery = drawerSearchQuery,
                onSearchQueryChange = { drawerSearchQuery = it },
                onNewChatClick = {
                    startNewChat()
                    scope.launch { drawerState.close() }
                },
                onSelectSession = { session ->
                    openChatSession(session)
                    scope.launch { drawerState.close() }
                },
                onDeleteSession = { session ->
                    deleteChatSession(session)
                },
                onOpenSettings = {
                    scope.launch { drawerState.close() }
                    showSettingsSheet = true
                }
            )
        }
    ) {
        Scaffold(
            containerColor = ChatDarkBg,
            topBar = {
                ChatGptTopBar(
                    selectedEngineMode = selectedEngineMode,
                    currentResponseStyle = responseStyle,
                    onOpenDrawer = {
                        triggerHaptic(strong = false)
                        scope.launch { drawerState.open() }
                    },
                    onSelectEngineMode = { mode ->
                        if (selectedEngineMode != mode) {
                            triggerHaptic(strong = false)
                            selectedEngineMode = mode
                            aiEngine.saveEngineMode(mode)
                        }
                    },
                    onNewChat = {
                        startNewChat()
                    },
                    onSelectResponseStyle = { style ->
                        triggerHaptic(strong = false)
                        scope.launch {
                            aiEngine.saveResponseStyle(style)
                        }
                    },
                    onExportChat = {
                        exportCurrentChatAsMarkdown()
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ChatDarkBg)
                    .padding(innerPadding)
                    .navigationBarsPadding()
            ) {
                // Subtle Top Offline Banner when device has no internet
                AnimatedVisibility(visible = isOffline) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF261919))
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WifiOff,
                            contentDescription = "Offline",
                            tint = Color(0xFFFFB4AB),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "You're offline. Check your internet connection.",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
                            color = Color(0xFFFFDAD6)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (activeMessages.isEmpty()) {
                        val starterChips = if (selectedEngineMode == AiEngineMode.FAST_MODE) {
                            FastStarterChips
                        } else {
                            DeepThinkStarterChips
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF171717))
                                    .border(1.dp, Color(0xFF282828), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = "${AiEngineService.APP_NAME} Spark",
                                    tint = ChatTextPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "What can I help with today?",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 22.sp
                                ),
                                color = ChatTextPrimary
                            )
                            Spacer(modifier = Modifier.height(22.dp))

                            // 4 Tappable Starter Chips tailored to active mode
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                starterChips.forEach { chipPrompt ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFF151515))
                                            .border(1.dp, Color(0xFF252525), RoundedCornerShape(16.dp))
                                            .clickable { sendMessage(chipPrompt) }
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Text(
                                            text = chipPrompt,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 13.5.sp,
                                                lineHeight = 19.sp
                                            ),
                                            color = ChatTextSecondary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(top = 12.dp, bottom = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            items(activeMessages, key = { it.id }) { msg ->
                                MinimalistMessageItem(
                                    message = msg,
                                    isCurrentlySpeaking = currentlySpeakingId == msg.id,
                                    onCopy = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText(AiEngineService.APP_NAME, msg.text))
                                    },
                                    onShare = { shareMessage(msg.text) },
                                    onSpeak = { speakMessage(msg) },
                                    onRetry = { retryOrRegenerateAiMessage(msg) },
                                    onContinue = { continueTruncatedAiMessage(msg) },
                                    onLongPressMessage = {
                                        triggerHaptic(strong = true)
                                        if (msg.isUser) {
                                            editingUserText = if (msg.text == "Attached image") "" else msg.text
                                            editingUserMessage = msg
                                        } else if (!msg.isLoading) {
                                            selectedAiMessageForActions = msg
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Attached image preview above input bar
                AnimatedVisibility(
                    visible = attachedImageUri != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (attachedImageUri != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            ) {
                                AsyncImage(
                                    model = attachedImageUri,
                                    contentDescription = "Attached image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(3.dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.75f))
                                        .clickable { attachedImageUri = null },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Remove image",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // ChatGPT Floating Bottom Dock (RoundedCornerShape(28.dp), #1E1E1E, 1.dp #2E2E2E)
                ChatGptFloatingBottomDock(
                    inputPrompt = inputPrompt,
                    onInputChange = { inputPrompt = it },
                    hasAttachment = attachedImageUri != null,
                    isSynthesizing = isSynthesizing,
                    animatedActionScale = animatedActionScale,
                    onAttachImage = {
                        triggerHaptic(strong = false)
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onStopClick = {
                        stopStreaming()
                    },
                    onSendOrMicClick = {
                        scope.launch {
                            actionButtonPressed = true
                            delay(100)
                            actionButtonPressed = false
                        }
                        val hasContent = inputPrompt.isNotBlank() || attachedImageUri != null
                        if (hasContent) {
                            sendMessage(inputPrompt)
                        } else {
                            triggerHaptic(strong = false)
                            try {
                                val voiceIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(
                                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                    )
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                                }
                                speechLauncher.launch(voiceIntent)
                            } catch (_: Exception) {
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * Slide-out Left Navigation Drawer like ChatGPT:
 * - Top header: App title ('Auralis') with a subtle subtitle
 * - Quick actions: '+ New Chat' pill button
 * - Search bar at the top of Recent Chats filtering by title and message text
 * - Recent Chats list with delete/open actions
 * - Bottom profile section: Rounded user avatar, username/email, and Settings (⚙️) link
 */
@Composable
private fun ChatGptSidebarDrawer(
    sessions: List<ChatSession>,
    currentSessionId: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNewChatClick: () -> Unit,
    onSelectSession: (ChatSession) -> Unit,
    onDeleteSession: (ChatSession) -> Unit,
    onOpenSettings: () -> Unit
) {
    val filteredSessions = remember(sessions.toList(), searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) {
            sessions.toList()
        } else {
            sessions.filter { session ->
                session.title.contains(q, ignoreCase = true) ||
                    session.messages.any { it.text.contains(q, ignoreCase = true) }
            }
        }
    }

    ModalDrawerSheet(
        drawerContainerColor = ChatDrawerSurface,
        drawerContentColor = ChatTextPrimary,
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1F1F1F))
                        .border(1.dp, Color(0xFF2E2E2E), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = ChatTextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = AiEngineService.APP_NAME,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp
                        ),
                        color = ChatTextPrimary
                    )
                    Text(
                        text = "Conversational Intelligence",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = ChatTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // '+ New Chat' Pill Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1E1E1E))
                    .border(1.dp, Color(0xFF2E2E2E), RoundedCornerShape(24.dp))
                    .clickable { onNewChatClick() }
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "New Chat",
                    tint = ChatTextPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "New Chat",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.5.sp
                    ),
                    color = ChatTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search bar at top of Recent Chats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1A1A1A))
                    .border(1.dp, Color(0xFF292929), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search chats",
                    tint = ChatTextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search chats...",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = ChatTextMuted
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = ChatTextPrimary,
                            fontSize = 13.sp
                        ),
                        cursorBrush = SolidColor(ChatAccentWhite),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear search",
                        tint = ChatTextMuted,
                        modifier = Modifier
                            .size(15.dp)
                            .clickable { onSearchQueryChange("") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Recent Chats",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.5.sp
                ),
                color = ChatTextMuted,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            // Recent Chats List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (filteredSessions.isEmpty()) {
                    Text(
                        text = if (searchQuery.isBlank()) "No saved conversations yet" else "No matching chats",
                        style = MaterialTheme.typography.bodySmall,
                        color = ChatTextMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(filteredSessions, key = { it.id }) { session ->
                            val isCurrent = session.id == currentSessionId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isCurrent) Color(0xFF212121) else Color.Transparent)
                                    .clickable { onSelectSession(session) }
                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = if (isCurrent) ChatTextPrimary else ChatTextMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = session.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal
                                    ),
                                    color = if (isCurrent) ChatTextPrimary else ChatTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onDeleteSession(session) },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Delete chat",
                                        tint = ChatTextMuted,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF222222), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Profile Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onOpenSettings() }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF262626)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "RJ",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = ChatTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${AiEngineService.APP_NAME} Workspace",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.5.sp
                        ),
                        color = ChatTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "reeta.jaiswal9999@gmail.com",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = ChatTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = ChatTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Authentic ChatGPT Top App Bar:
 * - Left: Hamburger icon (☰) to toggle the sidebar drawer
 * - Center: Clean compact model indicator / pill toggle (Fast / Deep Think)
 * - Right: 'New Chat' edit icon + 3-dot Overflow Menu (Response Style & Export Chat .md)
 */
@Composable
private fun ChatGptTopBar(
    selectedEngineMode: AiEngineMode,
    currentResponseStyle: ResponseStyle,
    onOpenDrawer: () -> Unit,
    onSelectEngineMode: (AiEngineMode) -> Unit,
    onNewChat: () -> Unit,
    onSelectResponseStyle: (ResponseStyle) -> Unit,
    onExportChat: () -> Unit
) {
    var overflowExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChatDarkBg)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onOpenDrawer,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = "Open menu",
                tint = ChatTextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(ChatSurfaceDock)
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AiEngineMode.values().forEach { mode ->
                val isSelected = selectedEngineMode == mode
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(17.dp))
                        .background(if (isSelected) Color(0xFF2B2B2B) else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelectEngineMode(mode) }
                        .padding(horizontal = 13.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.displayName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 12.5.sp
                        ),
                        color = if (isSelected) ChatTextPrimary else ChatTextSecondary
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onNewChat,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "New Chat",
                    tint = ChatTextPrimary,
                    modifier = Modifier.size(19.dp)
                )
            }

            Box {
                IconButton(
                    onClick = { overflowExpanded = true },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More options",
                        tint = ChatTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = overflowExpanded,
                    onDismissRequest = { overflowExpanded = false },
                    modifier = Modifier.background(Color(0xFF1A1A1A))
                ) {
                    Text(
                        text = "Response Style",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp
                        ),
                        color = ChatTextMuted,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )

                    ResponseStyle.values().forEach { style ->
                        val isSelected = currentResponseStyle == style
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = style.displayName,
                                    color = if (isSelected) ChatTextPrimary else ChatTextSecondary,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            },
                            trailingIcon = {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = ChatAccentWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            onClick = {
                                onSelectResponseStyle(style)
                                overflowExpanded = false
                            }
                        )
                    }

                    HorizontalDivider(color = Color(0xFF2B2B2B), thickness = 1.dp)

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Export Chat (.md)",
                                color = ChatTextPrimary,
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.FileDownload,
                                contentDescription = "Export Chat",
                                tint = ChatTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            overflowExpanded = false
                            onExportChat()
                        }
                    )
                }
            }
        }
    }
}

/**
 * ChatGPT Floating Bottom Dock:
 * - Floating input container: RoundedCornerShape(28.dp), background #1E1E1E, subtle 1.dp border #2E2E2E
 * - Left side: Circular '+' icon button to open photo gallery / attach images
 * - Center: Clean text field with placeholder 'Message Auralis...'
 * - Right side: Turns into a Stop button while streaming, or animates between Mic and Send arrow
 */
@Composable
private fun ChatGptFloatingBottomDock(
    inputPrompt: String,
    onInputChange: (String) -> Unit,
    hasAttachment: Boolean,
    isSynthesizing: Boolean,
    animatedActionScale: Float,
    onAttachImage: () -> Unit,
    onStopClick: () -> Unit,
    onSendOrMicClick: () -> Unit
) {
    val showSendButton = inputPrompt.isNotBlank() || hasAttachment

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(ChatDockSurface1E1E1E)
                .border(1.dp, ChatDockBorder2E2E2E, RoundedCornerShape(28.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onAttachImage,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2A2A2A))
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Attach image",
                    tint = ChatTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
                    .testTag("chat_input_field"),
                contentAlignment = Alignment.CenterStart
            ) {
                if (inputPrompt.isEmpty()) {
                    Text(
                        text = "Message ${AiEngineService.APP_NAME}...",
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.5.sp),
                        color = ChatTextMuted
                    )
                }
                BasicTextField(
                    value = inputPrompt,
                    onValueChange = onInputChange,
                    textStyle = TextStyle(
                        color = ChatTextPrimary,
                        fontSize = 15.5.sp,
                        lineHeight = 21.sp
                    ),
                    cursorBrush = SolidColor(ChatAccentWhite),
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .graphicsLayer {
                        scaleX = animatedActionScale
                        scaleY = animatedActionScale
                    }
                    .clip(CircleShape)
                    .background(
                        if (isSynthesizing || showSendButton) ChatAccentWhite else Color(0xFF2A2A2A)
                    )
                    .clickable {
                        if (isSynthesizing) {
                            onStopClick()
                        } else {
                            onSendOrMicClick()
                        }
                    }
                    .testTag("send_button"),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = when {
                        isSynthesizing -> "STOP"
                        showSendButton -> "SEND"
                        else -> "MIC"
                    },
                    transitionSpec = {
                        (fadeIn(tween(150)) + scaleIn(initialScale = 0.8f, animationSpec = tween(150)))
                            .togetherWith(fadeOut(tween(120)) + scaleOut(targetScale = 0.8f, animationSpec = tween(120)))
                    },
                    label = "send_stop_or_mic_button"
                ) { state ->
                    when (state) {
                        "STOP" -> {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = "Stop generating",
                                tint = ChatDarkBg,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        "SEND" -> {
                            Icon(
                                imageVector = Icons.Filled.ArrowUpward,
                                contentDescription = "Send",
                                tint = ChatDarkBg,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = "Voice input",
                                tint = ChatTextPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Conversation Canvas & Chat Bubbles:
 * - User Bubble: Sleek right-aligned rounded bubble (#1F1F1F) with long-press Edit & Resend.
 * - AI Response: Left-aligned borderless natural prose rendered directly on the dark canvas (#0A0A0A)
 *   with long-press Copy / Regenerate / Share, "Continue" chip on cut-off replies, and inline Retry on errors.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MinimalistMessageItem(
    message: DesiMessage,
    isCurrentlySpeaking: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onSpeak: () -> Unit,
    onRetry: () -> Unit,
    onContinue: () -> Unit,
    onLongPressMessage: () -> Unit
) {
    val isUser = message.isUser
    val isWaitingForFirstToken = !isUser && message.isLoading && message.text.isEmpty()

    if (isUser) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            if (message.imageUriString != null) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    AsyncImage(
                        model = message.imageUriString,
                        contentDescription = "Uploaded image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Box(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(ChatUserBubble1F1F1F)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = onLongPressMessage
                    )
                    .padding(horizontal = 16.dp, vertical = 11.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.5.sp,
                        lineHeight = 22.sp
                    ),
                    color = ChatTextPrimary
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongPressMessage
                ),
            horizontalAlignment = Alignment.Start
        ) {
            Crossfade(
                targetState = isWaitingForFirstToken,
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                label = "typing_to_text_crossfade"
            ) { showDots ->
                if (showDots) {
                    SynthesizingPulsePill(
                        isDeepThinkMode = message.engineMode == AiEngineMode.DEEP_THINK
                    )
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (message.error != null) {
                            // Inline "Couldn't reply. Retry" row (never saved to history, no error Toast)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF1A1414))
                                    .border(1.dp, Color(0xFF382424), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Couldn't reply.",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                    color = Color(0xFFE5B8B8)
                                )
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF2B2020))
                                        .clickable { onRetry() }
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = "Retry",
                                        tint = ChatAccentWhite,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Retry",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.5.sp
                                        ),
                                        color = ChatAccentWhite
                                    )
                                }
                            }
                        } else {
                            RichDesiMarkdownText(
                                rawText = message.text,
                                isUser = false
                            )

                            // Tappable "Continue" chip when an AI reply stops due to token limit
                            if (!message.isLoading && message.isTruncated) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF1E1E1E))
                                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(16.dp))
                                        .clickable { onContinue() }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = "Continue",
                                        tint = ChatAccentWhite,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Continue",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.5.sp
                                        ),
                                        color = ChatAccentWhite
                                    )
                                }
                            }

                            if (!message.isLoading && message.text.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    IconButton(
                                        onClick = onCopy,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ContentCopy,
                                            contentDescription = "Copy",
                                            tint = ChatTextMuted,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = onRetry,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Refresh,
                                            contentDescription = "Regenerate",
                                            tint = ChatTextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = onSpeak,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isCurrentlySpeaking) {
                                                Icons.Filled.Stop
                                            } else {
                                                Icons.AutoMirrored.Filled.VolumeUp
                                            },
                                            contentDescription = "Read aloud",
                                            tint = if (isCurrentlySpeaking) ChatAccentWhite else ChatTextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = onShare,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Share,
                                            contentDescription = "Share",
                                            tint = ChatTextMuted,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
