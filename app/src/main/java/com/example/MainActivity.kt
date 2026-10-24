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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ai.AiEngineMode
import com.example.ai.AiEngineService
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
import com.example.ui.theme.ChatUserBubble
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

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

    var inputPrompt by remember { mutableStateOf("") }
    var isSynthesizing by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    var selectedEngineMode by remember { mutableStateOf(aiEngine.getSavedEngineMode()) }
    var attachedImageUri by remember { mutableStateOf<Uri?>(null) }
    var currentlySpeakingId by remember { mutableStateOf<String?>(null) }

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

    val messages = remember {
        val loaded = aiEngine.loadChatHistory()
        mutableStateListOf<DesiMessage>().apply {
            addAll(loaded)
        }
    }

    fun persistHistory() {
        aiEngine.saveChatHistory(messages)
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

        val assistantMsgId = java.util.UUID.randomUUID().toString()
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
                        persistHistory()
                    }.onFailure { error ->
                        val oneLineError = (error.localizedMessage ?: "Connection interrupted. Please try again.")
                            .replace("[\\u4E00-\\u9FFF\\u3400-\\u4DBF\\uF900-\\uFAFF]".toRegex(), "")
                            .lines()
                            .firstOrNull()
                            ?.trim()
                            .orEmpty()
                            .ifBlank { "Connection interrupted. Please try again." }
                        if (idx != -1) {
                            messages[idx] = messages[idx].copy(
                                text = oneLineError,
                                isLoading = false,
                                error = oneLineError
                            )
                        }
                        persistHistory()
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
            containerColor = Color(0xFF141414),
            titleContentColor = ChatTextPrimary,
            textContentColor = ChatTextSecondary,
            title = {
                Text(
                    text = "Settings",
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
                        text = "Deep Think Chain: ${cfg.deepThinkChain.joinToString(" → ")} → ${cfg.deepThinkGeminiFallback}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ChatTextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!isSynthesizing && messages.isNotEmpty()) {
                            triggerHaptic(strong = false)
                            ttsEngine?.stop()
                            currentlySpeakingId = null
                            messages.clear()
                            persistHistory()
                        }
                        showSettingsSheet = false
                    }
                ) {
                    Text("Clear Chat", color = ChatTextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsSheet = false }) {
                    Text("Done", color = ChatTextSecondary)
                }
            }
        )
    }

    Scaffold(
        containerColor = ChatDarkBg,
        topBar = {
            MinimalistTopBar(
                selectedEngineMode = selectedEngineMode,
                onSelectEngineMode = { mode ->
                    triggerHaptic(strong = false)
                    selectedEngineMode = mode
                    aiEngine.saveEngineMode(mode)
                },
                onOpenSettings = {
                    triggerHaptic(strong = false)
                    showSettingsSheet = true
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "What can I help with?",
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
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
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
                                    .background(Color.Black.copy(alpha = 0.7f))
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

            // Clean Floating Input Bar (RoundedCornerShape(26.dp))
            MinimalistFloatingInputBar(
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

/**
 * Minimal Top Bar:
 * Single-row header with Title ("Auralis AI"), compact Fast / Deep Think toggle capsule,
 * and Settings gear icon on the right. Zero decorative badges or extra borders.
 */
@Composable
private fun MinimalistTopBar(
    selectedEngineMode: AiEngineMode,
    onSelectEngineMode: (AiEngineMode) -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChatDarkBg)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Auralis AI",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            ),
            color = ChatTextPrimary
        )

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
                        .padding(horizontal = 12.dp, vertical = 6.dp),
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
            onClick = onOpenSettings,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = ChatTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Clean Floating Input Bar:
 * Shaped like ChatGPT's rounded pill (`RoundedCornerShape(26.dp)`),
 * simple '+' icon on the left, clean 'Message' placeholder in center,
 * and smooth dynamic Send/Mic button on the right.
 */
@Composable
private fun MinimalistFloatingInputBar(
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
                .clip(RoundedCornerShape(26.dp))
                .background(ChatSurfaceDock)
                .border(1.dp, Color(0xFF262626), RoundedCornerShape(26.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onAttachImage,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF242424))
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add attachment",
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
                        text = "Message",
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
                    .background(if (showSendButton) ChatAccentWhite else Color(0xFF242424))
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
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = ChatDarkBg,
                            modifier = Modifier.size(18.dp)
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
 * Minimalist Chat Message Item:
 * - User Message: Simple subtle bubble (#1F1F1F), right-aligned.
 * - AI Message: Clean, left-aligned plain text with natural formatting and zero card borders.
 * - Loading Indicator: 3 minimal subtle pulsing dots inside a small capsule (`SynthesizingPulsePill`).
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
                    .background(ChatUserBubble)
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
