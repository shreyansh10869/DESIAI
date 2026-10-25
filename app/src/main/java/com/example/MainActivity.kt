package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import coil.compose.AsyncImage
import com.example.ai.AiEngineMode
import com.example.ai.AiEngineService
import com.example.ai.ChatSession
import com.example.ai.DesiMessage
import com.example.ai.DesiTone
import com.example.ui.RichDesiMarkdownText
import com.example.ui.SynthesizingPulsePill
import com.example.ui.theme.AuralisAITheme
import com.example.ui.theme.ChatAccentWhite
import com.example.ui.theme.ChatDarkBg
import com.example.ui.theme.ChatSurfaceDock
import com.example.ui.theme.ChatTextMuted
import com.example.ui.theme.ChatTextPrimary
import com.example.ui.theme.ChatTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

private val ChatUserBubble212121 = Color(0xFF212121)
private val ChatDockSurface1E1E1E = Color(0xFF1E1E1E)
private val ChatDockBorder2E2E2E = Color(0xFF2E2E2E)
private val ChatDrawerSurface = Color(0xFF121212)

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

@Composable
fun AuralisAIMainScreen() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val aiEngine = remember { AiEngineService(context) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var inputPrompt by remember { mutableStateOf("") }
    var isSynthesizing by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    var selectedEngineMode by remember { mutableStateOf(aiEngine.getSavedEngineMode()) }
    var attachedImageUri by remember { mutableStateOf<Uri?>(null) }
    var currentlySpeakingId by remember { mutableStateOf<String?>(null) }

    val chatSessions = remember {
        mutableStateListOf<ChatSession>().apply {
            addAll(aiEngine.loadChatSessions())
        }
    }
    var currentSessionId by remember {
        mutableStateOf(chatSessions.firstOrNull()?.id ?: UUID.randomUUID().toString())
    }

    val messages = remember {
        val initialMessages = chatSessions.firstOrNull()?.messages ?: aiEngine.loadChatHistory()
        mutableStateListOf<DesiMessage>().apply {
            addAll(initialMessages)
        }
    }

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
        } catch (e: Exception) {
            // Ignored
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

    fun persistCurrentSession() {
        val validMessages = messages.filter { !it.isLoading && it.text.isNotBlank() }
        aiEngine.saveChatHistory(validMessages)
        if (validMessages.isEmpty()) return

        val titleText = validMessages.firstOrNull { it.isUser }?.text
            ?.replace("\n", " ")
            ?.trim()
            ?.take(38)
            ?.ifBlank { "New Conversation" }
            ?: "New Conversation"

        val existingIdx = chatSessions.indexOfFirst { it.id == currentSessionId }
        val updatedSession = ChatSession(
            id = currentSessionId,
            title = titleText,
            updatedAt = System.currentTimeMillis(),
            messages = validMessages
        )
        if (existingIdx != -1) {
            chatSessions[existingIdx] = updatedSession
        } else {
            chatSessions.add(0, updatedSession)
        }
        aiEngine.saveChatSessions(chatSessions)
    }

    fun startNewChat() {
        if (isSynthesizing) return
        triggerHaptic(strong = false)
        ttsEngine?.stop()
        currentlySpeakingId = null
        if (messages.isNotEmpty()) {
            persistCurrentSession()
        }
        currentSessionId = UUID.randomUUID().toString()
        messages.clear()
        aiEngine.saveChatHistory(emptyList())
    }

    fun openChatSession(session: ChatSession) {
        if (isSynthesizing) return
        triggerHaptic(strong = false)
        ttsEngine?.stop()
        currentlySpeakingId = null
        currentSessionId = session.id
        messages.clear()
        messages.addAll(session.messages)
        aiEngine.saveChatHistory(messages)
    }

    fun deleteChatSession(session: ChatSession) {
        triggerHaptic(strong = false)
        chatSessions.removeAll { it.id == session.id }
        aiEngine.saveChatSessions(chatSessions)
        if (currentSessionId == session.id) {
            val nextSession = chatSessions.firstOrNull()
            if (nextSession != null) {
                currentSessionId = nextSession.id
                messages.clear()
                messages.addAll(nextSession.messages)
                aiEngine.saveChatHistory(messages)
            } else {
                currentSessionId = UUID.randomUUID().toString()
                messages.clear()
                aiEngine.saveChatHistory(emptyList())
            }
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
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to share", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendMessage(userText: String) {
        val trimmed = aiEngine.sanitizeStreamChunk(userText.trim())
        val imageToSend = attachedImageUri
        if (trimmed.isBlank() && imageToSend == null) return
        if (isSynthesizing) return

        triggerHaptic(strong = false)
        val activeEngineMode = selectedEngineMode

        val userMessage = DesiMessage(
            text = trimmed.ifBlank { "Attached image" },
            isUser = true,
            imageUriString = imageToSend?.toString(),
            tone = DesiTone.DESI_FRIEND,
            engineMode = activeEngineMode
        )
        val historySnapshot = messages.toList()
        messages.add(userMessage)
        inputPrompt = ""
        attachedImageUri = null
        isSynthesizing = true

        val assistantMsgId = UUID.randomUUID().toString()
        val assistantPlaceholder = DesiMessage(
            id = assistantMsgId,
            text = "",
            isUser = false,
            isLoading = true,
            tone = DesiTone.DESI_FRIEND,
            engineMode = activeEngineMode
        )
        messages.add(assistantPlaceholder)

        scope.launch {
            try {
                withContext(Dispatchers.Main) {
                    if (messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.size - 1)
                    }
                }

                val result = aiEngine.streamResponse(
                    prompt = trimmed,
                    imageUri = imageToSend,
                    engineMode = activeEngineMode,
                    conversationHistory = historySnapshot,
                    onToken = { partialText ->
                        withContext(Dispatchers.Main) {
                            val sanitizedPartial = partialText.replace(
                                "[\\u4E00-\\u9FFF\\u3400-\\u4DBF\\uF900-\\uFAFF]".toRegex(),
                                ""
                            )
                            val idx = messages.indexOfFirst { it.id == assistantMsgId }
                            if (idx != -1) {
                                messages[idx] = messages[idx].copy(
                                    text = sanitizedPartial,
                                    isLoading = true
                                )
                            }
                        }
                    }
                )

                withContext(Dispatchers.Main) {
                    val idx = messages.indexOfFirst { it.id == assistantMsgId }
                    result.onSuccess { (finalText, _) ->
                        val cleanFinal = finalText.replace(
                            "[\\u4E00-\\u9FFF\\u3400-\\u4DBF\\uF900-\\uFAFF]".toRegex(),
                            ""
                        )
                        if (idx != -1) {
                            messages[idx] = messages[idx].copy(
                                text = cleanFinal,
                                isLoading = false,
                                error = null
                            )
                        }
                        persistCurrentSession()
                    }.onFailure { error ->
                        val oneLineError = (error.localizedMessage ?: "Unable to reach AI service. Please try again.")
                            .replace("[\\u4E00-\\u9FFF\\u3400-\\u4DBF\\uF900-\\uFAFF]".toRegex(), "")
                            .lines()
                            .firstOrNull()
                            ?.trim()
                            .orEmpty()
                            .ifBlank { "Unable to reach AI service. Please try again." }
                        if (idx != -1) {
                            messages[idx] = messages[idx].copy(
                                text = oneLineError,
                                isLoading = false,
                                error = oneLineError
                            )
                        }
                        persistCurrentSession()
                    }
                    if (messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.size - 1)
                    }
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isSynthesizing = false
                }
            }
        }
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
                    text = "Auralis AI Settings",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Fast Mode: ${cfg.fastPrimary} → ${cfg.fastFallback}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ChatTextSecondary
                    )
                    Text(
                        text = "Deep Think Engine: ${cfg.deepThinkChain.joinToString(" → ")} (Fallback: ${cfg.deepThinkGeminiFallback})",
                        style = MaterialTheme.typography.bodySmall,
                        color = ChatTextSecondary
                    )
                    Text(
                        text = "API Keys: Pre-configured via BuildConfig (Active)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8BE28B)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        startNewChat()
                        showSettingsSheet = false
                    }
                ) {
                    Text("New Chat", color = ChatTextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsSheet = false }) {
                    Text("Close", color = ChatTextSecondary)
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatGptSidebarDrawer(
                sessions = chatSessions,
                currentSessionId = currentSessionId,
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
                    onOpenDrawer = {
                        triggerHaptic(strong = false)
                        scope.launch { drawerState.open() }
                    },
                    onSelectEngineMode = { mode ->
                        triggerHaptic(strong = false)
                        selectedEngineMode = mode
                        aiEngine.saveEngineMode(mode)
                    },
                    onNewChat = {
                        startNewChat()
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
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (messages.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
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
                                    contentDescription = "Auralis AI Spark",
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
                            items(messages, key = { it.id }) { msg ->
                                MinimalistMessageItem(
                                    message = msg,
                                    isCurrentlySpeaking = currentlySpeakingId == msg.id,
                                    onCopy = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Auralis AI", msg.text))
                                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                                    },
                                    onShare = { shareMessage(msg.text) },
                                    onSpeak = { speakMessage(msg) }
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
                            } catch (e: Exception) {
                                Toast.makeText(context, "Voice input unavailable", Toast.LENGTH_SHORT).show()
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
 * - Top header: App title ('Auralis AI') with a subtle subtitle
 * - Quick actions: '+ New Chat' pill button
 * - Chat History list with delete/open actions
 * - Bottom profile section: Rounded user avatar, username/email, and Settings (⚙️) link
 */
@Composable
private fun ChatGptSidebarDrawer(
    sessions: List<ChatSession>,
    currentSessionId: String,
    onNewChatClick: () -> Unit,
    onSelectSession: (ChatSession) -> Unit,
    onDeleteSession: (ChatSession) -> Unit,
    onOpenSettings: () -> Unit
) {
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
                        text = "Auralis AI",
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

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Recent Chats",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.5.sp
                ),
                color = ChatTextMuted,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            // Chat History List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (sessions.isEmpty()) {
                    Text(
                        text = "No saved conversations yet",
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
                        items(sessions, key = { it.id }) { session ->
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
                        text = "Auralis Workspace",
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
 * - Right: 'New Chat' edit icon (to clear conversation with a fresh screen)
 * - Background: Pure dark #0A0A0A with no harsh dividers
 */
@Composable
private fun ChatGptTopBar(
    selectedEngineMode: AiEngineMode,
    onOpenDrawer: () -> Unit,
    onSelectEngineMode: (AiEngineMode) -> Unit,
    onNewChat: () -> Unit
) {
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

        IconButton(
            onClick = onNewChat,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "New Chat",
                tint = ChatTextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * ChatGPT Floating Bottom Dock:
 * - Floating input container: RoundedCornerShape(28.dp), background #1E1E1E, subtle 1.dp border #2E2E2E
 * - Left side: Circular '+' icon button to open photo gallery / attach images
 * - Center: Clean text field with placeholder 'Message Auralis...'
 * - Right side: Dynamic send button that animates from a mic icon to an arrow-up send circle button when text is entered
 */
@Composable
private fun ChatGptFloatingBottomDock(
    inputPrompt: String,
    onInputChange: (String) -> Unit,
    hasAttachment: Boolean,
    isSynthesizing: Boolean,
    animatedActionScale: Float,
    onAttachImage: () -> Unit,
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
                        text = "Message Auralis...",
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
                    .background(if (showSendButton) ChatAccentWhite else Color(0xFF2A2A2A))
                    .clickable(enabled = !isSynthesizing) { onSendOrMicClick() }
                    .testTag("send_button"),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = showSendButton,
                    transitionSpec = {
                        (fadeIn(tween(150)) + scaleIn(initialScale = 0.8f, animationSpec = tween(150)))
                            .togetherWith(fadeOut(tween(120)) + scaleOut(targetScale = 0.8f, animationSpec = tween(120)))
                    },
                    label = "send_or_mic_button"
                ) { isSend ->
                    if (isSend) {
                        Icon(
                            imageVector = Icons.Filled.ArrowUpward,
                            contentDescription = "Send",
                            tint = ChatDarkBg,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
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

/**
 * Conversation Canvas & Chat Bubbles:
 * - User Bubble: Sleek right-aligned rounded bubble (#212121) with soft padding.
 * - AI Response: Left-aligned borderless natural prose rendered directly on the dark canvas (#0A0A0A).
 * - Loading State: 3 subtle pulsing dots inside a tiny pill capsule while awaiting the first token.
 */
@Composable
private fun MinimalistMessageItem(
    message: DesiMessage,
    isCurrentlySpeaking: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onSpeak: () -> Unit
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
                    .background(ChatUserBubble212121)
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
            modifier = Modifier.fillMaxWidth(),
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
                        RichDesiMarkdownText(
                            rawText = message.text,
                            isUser = false
                        )

                        if (!message.isLoading && message.error == null && message.text.isNotBlank()) {
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
