package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ChatCodeBg
import com.example.ui.theme.ChatTextPrimary
import com.example.ui.theme.ChatTextSecondary

sealed class MarkdownBlock {
    data class TextBlock(val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
}

fun parseMarkdownBlocks(rawText: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = rawText.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        if (line.trimStart().startsWith("```")) {
            val lang = line.trimStart().removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size) i++
            blocks.add(MarkdownBlock.CodeBlock(lang, codeLines.joinToString("\n")))
            continue
        }

        val textLines = mutableListOf<String>()
        while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
            textLines.add(lines[i])
            i++
        }

        if (textLines.isNotEmpty()) {
            val content = textLines.joinToString("\n").trim()
            if (content.isNotEmpty()) {
                blocks.add(MarkdownBlock.TextBlock(content))
            }
        }
    }

    return if (blocks.isEmpty()) listOf(MarkdownBlock.TextBlock(rawText)) else blocks
}

fun buildStyledMarkdownAnnotatedString(text: String, isUserMessage: Boolean): AnnotatedString {
    val baseColor = if (isUserMessage) Color.White else ChatTextPrimary
    val boldColor = Color.White
    val codeBgColor = Color(0xFF1E1E1E)

    return buildAnnotatedString {
        val lines = text.lines()
        for ((lineIdx, line) in lines.withIndex()) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("### ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = boldColor)) {
                        append(trimmed.removePrefix("### "))
                    }
                }
                trimmed.startsWith("## ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, color = boldColor)) {
                        append(trimmed.removePrefix("## "))
                    }
                }
                trimmed.startsWith("# ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = boldColor)) {
                        append(trimmed.removePrefix("# "))
                    }
                }
                else -> {
                    parseInlineFormatting(line, baseColor, boldColor, codeBgColor)
                }
            }

            if (lineIdx < lines.size - 1) {
                append("\n")
            }
        }
    }
}

private fun AnnotatedString.Builder.parseInlineFormatting(
    line: String,
    baseColor: Color,
    boldColor: Color,
    codeBgColor: Color
) {
    var cursor = 0
    while (cursor < line.length) {
        if (cursor + 1 < line.length && line[cursor] == '*' && line[cursor + 1] == '*') {
            val endIdx = line.indexOf("**", cursor + 2)
            if (endIdx != -1) {
                val boldContent = line.substring(cursor + 2, endIdx)
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = boldColor)) {
                    append(boldContent)
                }
                cursor = endIdx + 2
                continue
            }
        }

        if (line[cursor] == '`') {
            val endIdx = line.indexOf('`', cursor + 1)
            if (endIdx != -1) {
                val codeContent = line.substring(cursor + 1, endIdx)
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBgColor,
                        color = Color(0xFFE5E5E5),
                        fontSize = 13.5.sp
                    )
                ) {
                    append(" $codeContent ")
                }
                cursor = endIdx + 1
                continue
            }
        }

        withStyle(SpanStyle(color = baseColor)) {
            append(line[cursor])
        }
        cursor++
    }
}

@Composable
fun RichDesiMarkdownText(
    rawText: String,
    isUser: Boolean,
    modifier: Modifier = Modifier
) {
    val blocks = parseMarkdownBlocks(rawText)

    Column(modifier = modifier) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is MarkdownBlock.TextBlock -> {
                    Text(
                        text = buildStyledMarkdownAnnotatedString(block.text, isUser),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 15.5.sp,
                            lineHeight = 23.sp,
                            color = if (isUser) Color.White else ChatTextPrimary
                        )
                    )
                }
                is MarkdownBlock.CodeBlock -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ChatCodeBg)
                            .padding(12.dp)
                    ) {
                        if (block.language.isNotBlank()) {
                            Text(
                                text = block.language.lowercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = ChatTextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = block.code,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    color = Color(0xFFE5E5E5)
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            if (index < blocks.size - 1) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
