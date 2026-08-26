package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ai.Citation
import com.example.ai.DesiAIService
import com.example.ai.DesiMessage
import com.example.ai.DesiTone
import com.example.ui.theme.DesiAITheme
import com.example.ui.theme.DesiAmberSecondary
import com.example.ui.theme.DesiDarkBg
import com.example.ui.theme.DesiDarkBorder
import com.example.ui.theme.DesiDarkCard
import com.example.ui.theme.DesiDarkSurface
import com.example.ui.theme.DesiOrangePrimary
import com.example.ui.theme.DesiTextMuted
import com.example.ui.theme.DesiTextPrimary
import com.example.ui.theme.DesiTextSecondary
import kotlinx.coroutines.launch
import java.util.Locale

import com.example.ui.RichDesiMarkdownText
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DesiAITheme {
                DesiAIMainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesiAIMainScreen() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val desiService = remember { DesiAIService(context) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputPrompt by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf(desiService.getApiKey()) }

    var selectedTone by remember { mutableStateOf(desiService.getSavedTone()) }
    var attachedImageUri by remember { mutableStateOf<Uri?>(null) }
    var currentlySpeakingId by remember { mutableStateOf<String?>(null) }

    // Haptic and Completion Notification Banner
    var showCompletionBanner by remember { mutableStateOf(false) }
    var completionBannerText by remember { mutableStateOf("") }

    fun triggerHaptic(strong: Boolean = false) {
        try {
            if (strong) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(70)
                }
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        } catch (e: Exception) {
            // Ignored on platforms without vibration
        }
    }

    LaunchedEffect(showCompletionBanner) {
        if (showCompletionBanner) {
            delay(4000)
            showCompletionBanner = false
        }
    }

    // TTS engine setup
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("hi", "IN")
            }
        }
        ttsEngine = tts
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedImageUri = uri
            triggerHaptic(strong = false)
        }
    }

    // Quick Prompt Chips adapted to selected Tone
    val quickChips = remember(selectedTone) {
        if (selectedTone == DesiTone.EXAM_MODE) {
            listOf(
                "📚 Top 5 High-Yield Topics",
                "⚡ Quick Formula Derivation",
                "⏱️ 3-Hour Exam Speed Strategy",
                "📝 Standard Step-Marking Format",
                "🎯 Active Recall Mnemonics",
                "💡 Solve Attached Problem Photo"
            )
        } else {
            listOf(
                "⚡ Latest Tech & AI News",
                "🏏 Today's Cricket Live Update",
                "☕ Kadak Chai Secret Recipe",
                "💻 Python in Simple Hinglish",
                "🚀 Daily Jugaad Productivity",
                "📸 Explain This Photo Dost"
            )
        }
    }

    // Persistent Chat Message History
    val messages = remember {
        val loaded = desiService.loadChatHistory()
        mutableStateListOf<DesiMessage>().apply {
            if (loaded.isNotEmpty()) {
                addAll(loaded)
            } else {
                add(
                    DesiMessage(
                        text = "Namaste dost! 🙏 Main hoon **DesiAI** — aapka personal smart companion with **Deep Step-by-Step Reasoning**, **Google Search Grounding** 🌐, & **Visual AI** 📸.\n\nMode switch karo, image attach karo ya markdown comparisons dekho. Sunne ke liye speaker icon tap karo!",
                        isUser = false,
                        citations = listOf(
                            Citation(
                                title = "Google Search Grounding & Multimodal AI",
                                sourceOrContext = "Gemini 2.5 Flash",
                                keyTakeaway = "Live internet search grounding, rich tables, photo Q&A & speech synthesis enabled."
                            )
                        ),
                        tone = selectedTone
                    )
                )
            }
        }
    }

    // Save to persistent storage whenever messages change
    fun persistHistory() {
        desiService.saveChatHistory(messages)
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
            // Strip markdown asterisks for clean speech
            val speechText = message.text.replace("*", "").replace("#", "").replace("|", " ").take(800)
            tts.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, message.id)
        }
    }

    fun shareMessage(text: String) {
        triggerHaptic(strong = false)
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "DesiAI Response:\n\n$text")
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share DesiAI Answer")
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot share message", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        val imageToSend = attachedImageUri
        if (trimmed.isBlank() && imageToSend == null) return
        if (isGenerating) return

        triggerHaptic(strong = false)

        val userMessage = DesiMessage(
            text = trimmed.ifBlank { "📸 Attached photo for analysis" },
            isUser = true,
            imageUriString = imageToSend?.toString(),
            tone = selectedTone
        )
        messages.add(userMessage)
        inputPrompt = ""
        attachedImageUri = null
        isGenerating = true

        val loadingPlaceholder = DesiMessage(
            text = if (imageToSend != null) "Photo analyze ho rahi hai aur search chalu hai..." else "Step-by-step reasoning & live search chalu hai dost...",
            isUser = false,
            isLoading = true,
            tone = selectedTone
        )
        messages.add(loadingPlaceholder)

        scope.launch {
            listState.animateScrollToItem(messages.size - 1)
            val result = desiService.generateDesiResponse(trimmed, imageToSend, selectedTone, messages.dropLast(1))
            messages.remove(loadingPlaceholder)

            result.onSuccess { (responseText, citations) ->
                messages.add(
                    DesiMessage(
                        text = responseText,
                        isUser = false,
                        citations = citations,
                        tone = selectedTone
                    )
                )
                persistHistory()
                // 3. Haptic feedback and complete notification
                triggerHaptic(strong = true)
                completionBannerText = if (selectedTone == DesiTone.EXAM_MODE) {
                    "Exam Topper: Structured solution ready! 📚"
                } else {
                    "DesiAI: Answer ready dost! ✨"
                }
                showCompletionBanner = true
            }.onFailure { error ->
                messages.add(
                    DesiMessage(
                        text = "Arre bhai, thoda glitch ho gaya! Phir se try karo ya API key check karo.\n\n*Error: ${error.localizedMessage}*",
                        isUser = false,
                        error = error.localizedMessage,
                        tone = selectedTone
                    )
                )
                persistHistory()
                triggerHaptic(strong = true)
            }
            isGenerating = false
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(DesiDarkSurface)) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(DesiOrangePrimary, DesiAmberSecondary)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "D",
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontSize = 22.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "DesiAI",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = DesiTextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(DesiOrangePrimary.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.Public,
                                                contentDescription = null,
                                                tint = DesiOrangePrimary,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "SEARCH & VISION",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = DesiOrangePrimary,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (selectedTone == DesiTone.EXAM_MODE) "Exam Topper Mode Active 📚" else "Aapka Apna Smart AI Dost 😎",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selectedTone == DesiTone.EXAM_MODE) DesiAmberSecondary else DesiTextMuted
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                ttsEngine?.stop()
                                currentlySpeakingId = null
                                messages.clear()
                                messages.add(
                                    DesiMessage(
                                        text = "Namaste dost! 🙏 Chat refresh ho gaya. Poochho kuch bhi!",
                                        isUser = false,
                                        tone = selectedTone
                                    )
                                )
                                persistHistory()
                                Toast.makeText(context, "Chat cleared!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = "Clear Chat",
                                tint = DesiTextSecondary
                            )
                        }
                        IconButton(
                            onClick = {
                                apiKeyInput = desiService.getApiKey()
                                showApiKeyDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Key,
                                contentDescription = "API Settings",
                                tint = DesiOrangePrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DesiDarkSurface
                    )
                )

                // Feature 1: Interactive Tone Toggle Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(DesiDarkCard)
                        .border(1.dp, DesiDarkBorder, RoundedCornerShape(20.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DesiTone.values().forEach { tone ->
                        val isSelected = selectedTone == tone
                        val animatedBg by animateColorAsState(
                            targetValue = if (isSelected) DesiOrangePrimary else Color.Transparent,
                            label = "tone_bg"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(animatedBg)
                                .clickable {
                                    triggerHaptic(strong = false)
                                    selectedTone = tone
                                    desiService.saveTone(tone)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = tone.iconEmoji,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tone.displayName,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) Color.White else DesiTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = DesiDarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Quick Prompt Chips Scrollable Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickChips.forEach { chipText ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            triggerHaptic(strong = false)
                            if (chipText.contains("Attached Problem Photo")) {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            } else {
                                sendMessage(chipText.substringAfter(" "))
                            }
                        },
                        label = {
                            Text(
                                text = chipText,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                color = DesiTextPrimary
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = DesiDarkCard,
                            labelColor = DesiTextPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = DesiDarkBorder,
                            enabled = true,
                            selected = false
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
                }
            }

            // Chat Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    MessageBubble(
                        message = msg,
                        isCurrentlySpeaking = currentlySpeakingId == msg.id,
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("DesiAI Response", msg.text)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        onShare = {
                            shareMessage(msg.text)
                        },
                        onSpeak = {
                            speakMessage(msg)
                        },
                        onOpenUrl = { url ->
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // Floating Generation Completion Notification Banner
            AnimatedVisibility(
                visible = showCompletionBanner,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    color = Color(0xFF1B281E),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable {
                            showCompletionBanner = false
                            scope.launch {
                                if (messages.isNotEmpty()) {
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF69F0AE),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = completionBannerText,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFFE8F5E9),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Jump to answer ↓",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = DesiOrangePrimary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Attached Image Preview Strip above Input Bar
            AnimatedVisibility(
                visible = attachedImageUri != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (attachedImageUri != null) {
                    Surface(
                        color = DesiDarkSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, DesiOrangePrimary, RoundedCornerShape(8.dp))
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
                                    text = "Photo Attached 📸",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = DesiOrangePrimary
                                )
                                Text(
                                    text = "DesiAI will analyze image + text prompt",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DesiTextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            IconButton(
                                onClick = { attachedImageUri = null },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove photo",
                                    tint = DesiTextMuted
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Input Bar with Camera / Photo Attachment
            Surface(
                color = DesiDarkSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Feature 2: Image Attachment Button
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (attachedImageUri != null) DesiOrangePrimary.copy(alpha = 0.2f) else DesiDarkCard)
                            .border(1.dp, if (attachedImageUri != null) DesiOrangePrimary else DesiDarkBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddPhotoAlternate,
                            contentDescription = "Attach Image",
                            tint = if (attachedImageUri != null) DesiOrangePrimary else DesiTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = inputPrompt,
                        onValueChange = { inputPrompt = it },
                        placeholder = {
                            Text(
                                text = if (attachedImageUri != null) "Ask about this photo..." else "Poochho DesiAI se...",
                                color = DesiTextMuted,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DesiOrangePrimary,
                            unfocusedBorderColor = DesiDarkBorder,
                            focusedContainerColor = DesiDarkCard,
                            unfocusedContainerColor = DesiDarkCard,
                            focusedTextColor = DesiTextPrimary,
                            unfocusedTextColor = DesiTextPrimary
                        ),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    val canSend = (inputPrompt.isNotBlank() || attachedImageUri != null) && !isGenerating
                    IconButton(
                        onClick = { sendMessage(inputPrompt) },
                        enabled = canSend,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (canSend) DesiOrangePrimary else DesiDarkCard)
                            .testTag("send_button")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = DesiOrangePrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Send,
                                contentDescription = "Send",
                                tint = if (canSend) Color.White else DesiTextMuted
                            )
                        }
                    }
                }
            }
        }
    }

    // API Key Configuration Dialog
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Key, contentDescription = null, tint = DesiOrangePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gemini API Key", fontWeight = FontWeight.Bold, color = DesiTextPrimary)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Enter your Google Gemini API Key to enable real-time Google Search Grounding with live web citations & multimodal image analysis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DesiTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("API Key (AIzaSy...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        desiService.saveApiKey(apiKeyInput)
                        showApiKeyDialog = false
                        Toast.makeText(context, "API Key Saved!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DesiOrangePrimary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancel", color = DesiTextSecondary)
                }
            },
            containerColor = DesiDarkSurface
        )
    }
}

@Composable
fun MessageBubble(
    message: DesiMessage,
    isCurrentlySpeaking: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onSpeak: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val isUser = message.isUser

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (message.tone == DesiTone.EXAM_MODE) DesiAmberSecondary else DesiOrangePrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (message.tone == DesiTone.EXAM_MODE) "📚" else "D",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = if (message.tone == DesiTone.EXAM_MODE) 14.sp else 16.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(if (isUser) 0.85f else 0.90f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // If user attached an image, render preview inside the bubble
            if (isUser && message.imageUriString != null) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, DesiOrangePrimary, RoundedCornerShape(14.dp))
                ) {
                    AsyncImage(
                        model = message.imageUriString,
                        contentDescription = "User uploaded photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isUser) 18.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 18.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser) DesiOrangePrimary else DesiDarkCard
                ),
                border = if (!isUser) androidx.compose.foundation.BorderStroke(1.dp, DesiDarkBorder) else null
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (message.isLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = DesiOrangePrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = message.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = DesiTextSecondary
                            )
                        }
                    } else {
                        // 2. Rich Data Formatting: Markdown Tables, Bullets, Headers & Bold Highlighting
                        RichDesiMarkdownText(
                            rawText = message.text,
                            isUser = isUser
                        )

                        // Feature 3 & 4: Copy, Share, and Text-to-Speech Action Row below AI response
                        if (!isUser) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Text to Speech button
                                IconButton(
                                    onClick = onSpeak,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (isCurrentlySpeaking) DesiOrangePrimary.copy(alpha = 0.2f) else Color.Transparent)
                                ) {
                                    Icon(
                                        imageVector = if (isCurrentlySpeaking) Icons.Filled.Stop else Icons.Filled.VolumeUp,
                                        contentDescription = "Listen out loud",
                                        tint = if (isCurrentlySpeaking) DesiOrangePrimary else DesiTextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Copy button
                                IconButton(
                                    onClick = onCopy,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ContentCopy,
                                        contentDescription = "Copy text",
                                        tint = DesiTextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Share button
                                IconButton(
                                    onClick = onShare,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Share,
                                        contentDescription = "Share answer",
                                        tint = DesiTextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Structured Citations / Sources Block with Google Search Grounding links
            if (!isUser && message.citations.isNotEmpty() && !message.isLoading) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DesiDarkSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DesiDarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Language,
                                contentDescription = null,
                                tint = DesiAmberSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "VERIFIED SOURCES & CITATIONS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = DesiAmberSecondary,
                                fontSize = 10.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        message.citations.forEach { citation ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .then(
                                        if (citation.url.isNotBlank()) Modifier.clickable { onOpenUrl(citation.url) }
                                        else Modifier
                                    )
                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "🌐 ${citation.title}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (citation.url.isNotBlank()) DesiOrangePrimary else DesiTextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (citation.url.isNotBlank()) {
                                        Icon(
                                            imageVector = Icons.Filled.OpenInNew,
                                            contentDescription = "Open source link",
                                            tint = DesiOrangePrimary,
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
