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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.ui.theme.AuralisCapsuleBg
import com.example.ui.theme.AuralisCyan
import com.example.ui.theme.AuralisGlassCard
import com.example.ui.theme.AuralisHairlineBorder
import com.example.ui.theme.AuralisObsidianBg
import com.example.ui.theme.AuralisPurple
import com.example.ui.theme.DesiTextMuted
import com.example.ui.theme.DesiTextPrimary
import com.example.ui.theme.DesiTextSecondary
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
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var geminiKeyInput by remember { mutableStateOf(aiEngine.getGeminiApiKey()) }
    var groqKeyInput by remember { mutableStateOf(aiEngine.getGroqApiKey()) }

    var selectedEngineMode by remember { mutableStateOf(aiEngine.getSavedEngineMode()) }
    var selectedTone by remember { mutableStateOf(aiEngine.getSavedTone()) }
    var attachedImageUri by remember { mutableStateOf<Uri?>(null) }
    var currentlySpeakingId by remember { mutableStateOf<String?>(null) }

    // Tactile spring scale state for Send/Mic action button
    var actionButtonPressed by remember { mutableStateOf(false) }
    val animatedActionScale by animateFloatAsState(
        targetValue = if (actionButtonPressed) 0.86f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "elastic_action_scale"
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
                    vibrator?.vibrate(VibrationEffect.createOneShot(55, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(55)
                }
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        } catch (e: Exception) {
            // Ignored on unsupported devices
        }
    }

    // Modernized TTS with Locale.forLanguageTag("hi-IN")
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

    // Visual media photo picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedImageUri = uri
            triggerHaptic(strong = false)
        }
    }

    // Speech recognition launcher for Mic button
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        val spokenText = matches?.firstOrNull()?.trim().orEmpty()
        if (spokenText.isNotBlank()) {
            inputPrompt = spokenText
        }
    }

    // Persistent Chat Message History (starts empty so Empty State Hero shines on fresh launch)
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
                .replace("|", " ")
                .take(800)
            tts.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, message.id)
        }
    }

    fun shareMessage(text: String) {
        triggerHaptic(strong = false)
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, "Auralis AI:\n\n$text")
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share Auralis AI Response"))
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to share message", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendMessage(userText: String) {
        val trimmed = aiEngine.sanitizeStreamChunk(userText.trim())
        val imageToSend = attachedImageUri
        if (trimmed.isBlank() && imageToSend == null) return
        if (isSynthesizing) return

        triggerHaptic(strong = false)
        val activeEngineMode = selectedEngineMode
        val activeTone = selectedTone

        val userMessage = DesiMessage(
            text = trimmed.ifBlank { "Attached visual input for Auralis AI analysis" },
            isUser = true,
            imageUriString = imageToSend?.toString(),
            tone = activeTone,
            engineMode = activeEngineMode
        )
        val historySnapshot = messages.toList()
        messages.add(userMessage)
        inputPrompt = ""
        attachedImageUri = null
        isSynthesizing = true

        // Minimalist loading placeholder: empty text triggers compact glassmorphic glowing dots (NO verbose text badges)
        val assistantMsgId = java.util.UUID.randomUUID().toString()
        val assistantPlaceholder = DesiMessage(
            id = assistantMsgId,
            text = "",
            isUser = false,
            isLoading = true,
            tone = activeTone,
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
                    tone = activeTone,
                    conversationHistory = historySnapshot,
                    onToken = { partialText ->
                        // Dispatched strictly on Dispatchers.Main inside AiEngineService
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
                )

                withContext(Dispatchers.Main) {
                    val idx = messages.indexOfFirst { it.id == assistantMsgId }
                    result.onSuccess { (finalText, citations) ->
                        val cleanFinal = finalText.replace(
                            "[\\u4E00-\\u9FFF\\u3400-\\u4DBF\\uF900-\\uFAFF]".toRegex(),
                            ""
                        )
                        if (idx != -1) {
                            messages[idx] = messages[idx].copy(
                                text = cleanFinal,
                                citations = citations,
                                isLoading = false
                            )
                        }
                        persistHistory()
                        triggerHaptic(strong = true)
                    }.onFailure { error ->
                        val safeError = (error.localizedMessage ?: "Connection interrupted")
                            .replace("[\\u4E00-\\u9FFF\\u3400-\\u4DBF\\uF900-\\uFAFF]".toRegex(), "")
                        if (idx != -1) {
                            messages[idx] = messages[idx].copy(
                                text = "Auralis AI encountered a temporary network interruption. Please retry or verify your API key.\n\n*Details: $safeError*",
                                isLoading = false,
                                error = safeError
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

    Scaffold(
        containerColor = AuralisObsidianBg,
        topBar = {
            AuralisLiquidSpringHeader(
                selectedEngineMode = selectedEngineMode,
                selectedTone = selectedTone,
                onSelectEngineMode = { mode ->
                    triggerHaptic(strong = false)
                    selectedEngineMode = mode
                    aiEngine.saveEngineMode(mode)
                },
                onToggleTone = {
                    triggerHaptic(strong = false)
                    val nextTone = if (selectedTone == DesiTone.DESI_FRIEND) {
                        DesiTone.EXAM_MODE
                    } else {
                        DesiTone.DESI_FRIEND
                    }
                    selectedTone = nextTone
                    aiEngine.saveTone(nextTone)
                },
                onClearChat = {
                    triggerHaptic(strong = false)
                    ttsEngine?.stop()
                    currentlySpeakingId = null
                    messages.clear()
                    persistHistory()
                },
                onOpenApiKeys = {
                    geminiKeyInput = aiEngine.getGeminiApiKey()
                    groqKeyInput = aiEngine.getGroqApiKey()
                    showApiKeyDialog = true
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AuralisObsidianBg)
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            // Main viewport: Empty State Hero when history is empty, or Chat Message Stream
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Crossfade(
                    targetState = messages.isEmpty(),
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                    label = "empty_vs_chat_crossfade"
                ) { isEmpty ->
                    if (isEmpty) {
                        AuralisEmptyStateHero(
                            selectedEngineMode = selectedEngineMode,
                            selectedTone = selectedTone,
                            onSuggestionClick = { suggestion ->
                                if (suggestion.contains("Photo", ignoreCase = true) ||
                                    suggestion.contains("Visual", ignoreCase = true)
                                ) {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                } else {
                                    sendMessage(suggestion)
                                }
                            }
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(top = 12.dp, bottom = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(messages, key = { it.id }) { msg ->
                                AuralisMessageBubble(
                                    message = msg,
                                    isCurrentlySpeaking = currentlySpeakingId == msg.id,
                                    onCopy = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Auralis AI Response", msg.text))
                                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    onShare = { shareMessage(msg.text) },
                                    onSpeak = { speakMessage(msg) },
                                    onOpenUrl = { url ->
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Attached Image Preview Strip above Elastic Action Dock
            AnimatedVisibility(
                visible = attachedImageUri != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut()
            ) {
                if (attachedImageUri != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(AuralisGlassCard)
                            .border(1.dp, AuralisHairlineBorder, RoundedCornerShape(18.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, AuralisCyan.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        ) {
                            AsyncImage(
                                model = attachedImageUri,
                                contentDescription = "Attached photo preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Visual Input Ready",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = AuralisCyan
                            )
                            Text(
                                text = "Auralis AI will synthesize image + prompt",
                                style = MaterialTheme.typography.bodySmall,
                                color = DesiTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(
                            onClick = { attachedImageUri = null },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove attached photo",
                                tint = DesiTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Elevated Elastic Action Dock (RoundedCornerShape(28.dp))
            AuralisElasticActionDock(
                inputPrompt = inputPrompt,
                onInputChange = { inputPrompt = it },
                hasAttachment = attachedImageUri != null,
                isSynthesizing = isSynthesizing,
                engineMode = selectedEngineMode,
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
                        delay(110)
                        actionButtonPressed = false
                    }
                    val hasTypedContent = inputPrompt.isNotBlank() || attachedImageUri != null
                    if (hasTypedContent) {
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
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Auralis AI...")
                            }
                            speechLauncher.launch(voiceIntent)
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Type a prompt or tap a suggestion chip to begin",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )
        }
    }

    // API Keys Configuration Dialog
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Key, contentDescription = null, tint = AuralisCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Auralis AI Engine Keys",
                        fontWeight = FontWeight.Bold,
                        color = DesiTextPrimary
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Configure your Gemini API Key (Fast Mode & Search Grounding) and optional Groq API Key (Deep Think fallback).",
                        style = MaterialTheme.typography.bodySmall,
                        color = DesiTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = geminiKeyInput,
                        onValueChange = { geminiKeyInput = it },
                        label = { Text("Gemini API Key (AIzaSy...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = groqKeyInput,
                        onValueChange = { groqKeyInput = it },
                        label = { Text("Groq API Key (gsk_... Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        aiEngine.saveApiKey(geminiKeyInput, groqKeyInput)
                        showApiKeyDialog = false
                        Toast.makeText(context, "Auralis AI Keys Saved", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuralisCyan,
                        contentColor = AuralisObsidianBg
                    )
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancel", color = DesiTextSecondary)
                }
            },
            containerColor = Color(0xFF0F111A)
        )
    }
}

/**
 * Liquid Spring Header:
 * Floating rounded pill header with smooth spring slider (`spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)`)
 * that slides a liquid neon glow between Fast Mode (#00F2FE) and Deep Think (#9B51E0) toggles.
 */
@Composable
private fun AuralisLiquidSpringHeader(
    selectedEngineMode: AiEngineMode,
    selectedTone: DesiTone,
    onSelectEngineMode: (AiEngineMode) -> Unit,
    onToggleTone: () -> Unit,
    onClearChat: () -> Unit,
    onOpenApiKeys: () -> Unit
) {
    val activeAccent by animateColorAsState(
        targetValue = if (selectedEngineMode == AiEngineMode.FAST_MODE) AuralisCyan else AuralisPurple,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow),
        label = "header_accent_color"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AuralisObsidianBg.copy(alpha = 0.92f))
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // Top Row: Official Auralis AI Emblem + Brand Title + Persona & Utility Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AuralisCyan, AuralisPurple)
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "A",
                        color = Color(0xFF090A0F),
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Auralis AI",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.4.sp
                            ),
                            color = DesiTextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(activeAccent.copy(alpha = 0.16f))
                                .border(0.8.dp, activeAccent.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = selectedEngineMode.badgeLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.5.sp
                                ),
                                color = activeAccent
                            )
                        }
                    }
                    Text(
                        text = if (selectedTone == DesiTone.EXAM_MODE) {
                            "Exam Topper • Step-by-Step Precision"
                        } else {
                            "Cyber-Desi Neural Intelligence"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = DesiTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Persona Quick Switch (Dost vs Exam Topper)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(AuralisGlassCard)
                        .border(1.dp, AuralisHairlineBorder, RoundedCornerShape(14.dp))
                        .clickable { onToggleTone() }
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (selectedTone == DesiTone.EXAM_MODE) {
                                Icons.Filled.School
                            } else {
                                Icons.Filled.AutoAwesome
                            },
                            contentDescription = "Switch Persona",
                            tint = if (selectedTone == DesiTone.EXAM_MODE) AuralisPurple else AuralisCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = selectedTone.badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = DesiTextPrimary,
                            fontSize = 10.sp
                        )
                    }
                }

                IconButton(
                    onClick = onClearChat,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = "Clear Chat",
                        tint = DesiTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onOpenApiKeys,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Key,
                        contentDescription = "Configure API Keys",
                        tint = activeAccent,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Floating Rounded Pill Header with Liquid Spring Slider
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(AuralisCapsuleBg)
                .border(1.dp, AuralisHairlineBorder, RoundedCornerShape(22.dp))
                .padding(3.dp)
        ) {
            val segmentWidth = maxWidth / 2
            val indicatorOffset by animateDpAsState(
                targetValue = if (selectedEngineMode == AiEngineMode.FAST_MODE) 0.dp else segmentWidth,
                animationSpec = spring(
                    dampingRatio = 0.75f,
                    stiffness = Spring.StiffnessLow
                ),
                label = "liquid_spring_pill_offset"
            )

            // Liquid Neon Glow Sliding Pill
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(segmentWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(19.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = if (selectedEngineMode == AiEngineMode.FAST_MODE) {
                                listOf(
                                    AuralisCyan.copy(alpha = 0.26f),
                                    AuralisCyan.copy(alpha = 0.14f)
                                )
                            } else {
                                listOf(
                                    AuralisPurple.copy(alpha = 0.30f),
                                    AuralisPurple.copy(alpha = 0.16f)
                                )
                            }
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = activeAccent.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(19.dp)
                    )
            )

            Row(modifier = Modifier.fillMaxSize()) {
                // Fast Mode Tab
                val isFast = selectedEngineMode == AiEngineMode.FAST_MODE
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(19.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelectEngineMode(AiEngineMode.FAST_MODE) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = if (isFast) AuralisCyan else DesiTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Fast Mode",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isFast) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isFast) DesiTextPrimary else DesiTextSecondary
                        )
                    }
                }

                // Deep Think Tab
                val isDeep = selectedEngineMode == AiEngineMode.DEEP_THINK
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(19.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelectEngineMode(AiEngineMode.DEEP_THINK) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Psychology,
                            contentDescription = null,
                            tint = if (isDeep) AuralisPurple else DesiTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Deep Think",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isDeep) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isDeep) DesiTextPrimary else DesiTextSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Empty State Hero:
 * Centered AI Breathing Core with infinite rotating gradient aura between #00F2FE and #9B51E0,
 * accompanied by staggered sliding suggestion chips.
 */
@Composable
private fun AuralisEmptyStateHero(
    selectedEngineMode: AiEngineMode,
    selectedTone: DesiTone,
    onSuggestionClick: (String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "auralis_breathing_core")
    val auraRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aura_rotation"
    )
    val coreScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_scale"
    )

    val suggestions = remember(selectedEngineMode, selectedTone) {
        if (selectedTone == DesiTone.EXAM_MODE) {
            listOf(
                "Derive key physics & math formulas with SI units",
                "Compare O(N) vs O(N log N) algorithms with Kotlin code",
                "High-yield 3-hour exam revision strategy table",
                "Analyze an attached problem photo step-by-step"
            )
        } else {
            listOf(
                "Architect a production Kotlin协程/Flow clean pattern",
                "Explain quantum computing in witty Hinglish",
                "Compare top flagship AI models in a structured table",
                "Analyze an attached visual photo with Auralis Vision"
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Centered AI Breathing Core with rotating #00F2FE <-> #9B51E0 gradient aura
        Box(
            modifier = Modifier
                .size(118.dp)
                .graphicsLayer {
                    scaleX = coreScale
                    scaleY = coreScale
                }
                .drawBehind {
                    val sweepBrush = Brush.sweepGradient(
                        colors = listOf(
                            AuralisCyan,
                            AuralisPurple,
                            AuralisCyan.copy(alpha = 0.7f),
                            AuralisPurple,
                            AuralisCyan
                        ),
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                    rotate(degrees = auraRotation) {
                        drawCircle(
                            brush = sweepBrush,
                            radius = size.minDimension / 2f,
                            alpha = 0.35f
                        )
                    }
                    drawCircle(
                        color = if (selectedEngineMode == AiEngineMode.FAST_MODE) {
                            AuralisCyan.copy(alpha = 0.16f)
                        } else {
                            AuralisPurple.copy(alpha = 0.20f)
                        },
                        radius = size.minDimension * 0.42f
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F111A))
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(listOf(AuralisCyan, AuralisPurple)),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = if (selectedEngineMode == AiEngineMode.FAST_MODE) AuralisCyan else AuralisPurple
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Auralis AI",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.6.sp
            ),
            color = DesiTextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (selectedEngineMode == AiEngineMode.DEEP_THINK) {
                "Deep Think Neural Engine • First-Principles Chain-of-Thought"
            } else {
                "Fast Mode Gemini Synthesis • Instant Multimodal Intelligence"
            },
            style = MaterialTheme.typography.bodySmall,
            color = DesiTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(26.dp))

        // Staggered Sliding Suggestion Chips
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            suggestions.forEachIndexed { index, chipText ->
                var chipVisible by remember { mutableStateOf(false) }
                LaunchedEffect(selectedEngineMode, selectedTone) {
                    chipVisible = false
                    delay(index * 75L)
                    chipVisible = true
                }

                AnimatedVisibility(
                    visible = chipVisible,
                    enter = fadeIn(tween(260)) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = 0.78f,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetY = { it / 2 }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(AuralisGlassCard)
                            .border(1.dp, AuralisHairlineBorder, RoundedCornerShape(18.dp))
                            .clickable { onSuggestionClick(chipText) }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = chipText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.5.sp
                            ),
                            color = DesiTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = if (selectedEngineMode == AiEngineMode.FAST_MODE) AuralisCyan else AuralisPurple,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Elevated Bottom Prompt Input Dock with `shape = RoundedCornerShape(28.dp)`,
 * tactile spring scale rebound on Send/Mic tap, and smooth Mic <-> Send Arrow crossfade.
 */
@Composable
private fun AuralisElasticActionDock(
    inputPrompt: String,
    onInputChange: (String) -> Unit,
    hasAttachment: Boolean,
    isSynthesizing: Boolean,
    engineMode: AiEngineMode,
    animatedActionScale: Float,
    onAttachImage: () -> Unit,
    onSendOrMicClick: () -> Unit
) {
    val activeAccent = if (engineMode == AiEngineMode.FAST_MODE) AuralisCyan else AuralisPurple
    val showSendArrow = inputPrompt.isNotBlank() || hasAttachment

    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(AuralisGlassCard)
                .border(1.dp, AuralisHairlineBorder, RoundedCornerShape(28.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Multimodal Photo Attachment Button
            IconButton(
                onClick = onAttachImage,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (hasAttachment) activeAccent.copy(alpha = 0.2f) else Color(0xFF0F111A)
                    )
                    .border(
                        1.dp,
                        if (hasAttachment) activeAccent else AuralisHairlineBorder,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.AddPhotoAlternate,
                    contentDescription = "Attach Image",
                    tint = if (hasAttachment) activeAccent else DesiTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Clean Glassmorphic Text Input
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
                    .testTag("chat_input_field"),
                contentAlignment = Alignment.CenterStart
            ) {
                if (inputPrompt.isEmpty()) {
                    Text(
                        text = if (hasAttachment) {
                            "Ask Auralis AI about this photo..."
                        } else if (engineMode == AiEngineMode.DEEP_THINK) {
                            "Ask Auralis Deep Think..."
                        } else {
                            "Ask Auralis AI anything..."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = DesiTextMuted,
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = inputPrompt,
                    onValueChange = onInputChange,
                    textStyle = TextStyle(
                        color = DesiTextPrimary,
                        fontSize = 14.5.sp,
                        lineHeight = 20.sp
                    ),
                    cursorBrush = SolidColor(activeAccent),
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Elastic Action Button with spring scale rebound & smooth Mic <-> Send Arrow morph
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .graphicsLayer {
                        scaleX = animatedActionScale
                        scaleY = animatedActionScale
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = if (showSendArrow) {
                                listOf(activeAccent, AuralisPurple)
                            } else {
                                listOf(
                                    Color(0xFF1F2336),
                                    Color(0xFF161826)
                                )
                            }
                        )
                    )
                    .border(
                        1.dp,
                        if (showSendArrow) activeAccent.copy(alpha = 0.7f) else AuralisHairlineBorder,
                        CircleShape
                    )
                    .clickable(enabled = !isSynthesizing) { onSendOrMicClick() }
                    .testTag("send_button"),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = showSendArrow,
                    transitionSpec = {
                        (fadeIn(tween(180)) + scaleIn(initialScale = 0.75f, animationSpec = tween(180)))
                            .togetherWith(fadeOut(tween(140)) + scaleOut(targetScale = 0.75f, animationSpec = tween(140)))
                    },
                    label = "mic_to_send_morph"
                ) { isSendMode ->
                    if (isSendMode) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send prompt",
                            tint = Color(0xFF090A0F),
                            modifier = Modifier.size(19.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Voice input",
                            tint = activeAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Message Bubble with Zero-Flicker Crossfade from `SynthesizingPulsePill` (compact glassmorphic
 * glowing dots) into the streaming markdown bubble the moment the first token is emitted.
 */
@Composable
fun AuralisMessageBubble(
    message: DesiMessage,
    isCurrentlySpeaking: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onSpeak: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val isUser = message.isUser
    val isWaitingForFirstToken = !isUser && message.isLoading && message.text.isEmpty()
    val modeAccent = if (message.engineMode == AiEngineMode.DEEP_THINK) AuralisPurple else AuralisCyan

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F111A))
                    .border(
                        1.dp,
                        Brush.linearGradient(listOf(AuralisCyan, AuralisPurple)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A",
                    fontWeight = FontWeight.Black,
                    color = modeAccent,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.fillMaxWidth(if (isUser) 0.85f else 0.92f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Optional user uploaded image preview
            if (isUser && message.imageUriString != null) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, AuralisCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    AsyncImage(
                        model = message.imageUriString,
                        contentDescription = "User uploaded image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Zero-Flicker Transition: compact floating glassmorphic dots -> streaming message card
            Crossfade(
                targetState = isWaitingForFirstToken,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                label = "dots_to_stream_crossfade"
            ) { showDotsCapsule ->
                if (showDotsCapsule) {
                    SynthesizingPulsePill(
                        isDeepThinkMode = message.engineMode == AiEngineMode.DEEP_THINK
                    )
                } else {
                    Card(
                        shape = RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = if (isUser) 20.dp else 6.dp,
                            bottomEnd = if (isUser) 6.dp else 20.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) {
                                Color(0xFF1B2538)
                            } else {
                                AuralisGlassCard
                            }
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isUser) {
                                AuralisCyan.copy(alpha = 0.32f)
                            } else {
                                AuralisHairlineBorder
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            RichDesiMarkdownText(
                                rawText = message.text,
                                isUser = isUser
                            )

                            if (!isUser && !message.isLoading) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = onSpeak,
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isCurrentlySpeaking) {
                                                    modeAccent.copy(alpha = 0.2f)
                                                } else {
                                                    Color.Transparent
                                                }
                                            )
                                    ) {
                                        Icon(
                                            imageVector = if (isCurrentlySpeaking) {
                                                Icons.Filled.Stop
                                            } else {
                                                Icons.AutoMirrored.Filled.VolumeUp
                                            },
                                            contentDescription = "Listen out loud",
                                            tint = if (isCurrentlySpeaking) modeAccent else DesiTextMuted,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    IconButton(
                                        onClick = onCopy,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ContentCopy,
                                            contentDescription = "Copy response",
                                            tint = DesiTextMuted,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    IconButton(
                                        onClick = onShare,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Share,
                                            contentDescription = "Share response",
                                            tint = DesiTextMuted,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Structured Verified Citations Block
            if (!isUser && message.citations.isNotEmpty() && !message.isLoading) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AuralisCapsuleBg
                    ),
                    border = BorderStroke(1.dp, AuralisHairlineBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Language,
                                contentDescription = null,
                                tint = modeAccent,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AURALIS VERIFIED SOURCES",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = modeAccent,
                                fontSize = 9.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        message.citations.forEach { citation ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .then(
                                        if (citation.url.isNotBlank()) {
                                            Modifier.clickable { onOpenUrl(citation.url) }
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = citation.title,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (citation.url.isNotBlank()) AuralisCyan else DesiTextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (citation.url.isNotBlank()) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = "Open source link",
                                            tint = AuralisCyan,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                                if (citation.keyTakeaway.isNotBlank()) {
                                    Text(
                                        text = citation.keyTakeaway,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DesiTextSecondary,
                                        fontSize = 11.sp
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
