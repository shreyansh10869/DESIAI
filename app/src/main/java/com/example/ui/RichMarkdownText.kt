package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DesiAmberSecondary
import com.example.ui.theme.DesiDarkBorder
import com.example.ui.theme.DesiDarkCard
import com.example.ui.theme.DesiDarkSurface
import com.example.ui.theme.DesiOrangePrimary
import com.example.ui.theme.DesiTextMuted
import com.example.ui.theme.DesiTextPrimary
import com.example.ui.theme.DesiTextSecondary

/**
 * Parses markdown into text segments, code blocks, and structured markdown tables.
 */
sealed class MarkdownBlock {
    data class TextBlock(val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class TableBlock(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock()
}

fun parseMarkdownBlocks(rawText: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = rawText.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // Check for Code Block ```
        if (line.trimStart().startsWith("```")) {
            val lang = line.trimStart().removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size) i++ // skip closing ```
            blocks.add(MarkdownBlock.CodeBlock(lang, codeLines.joinToString("\n")))
            continue
        }

        // Check for Markdown Table (Line starting with | and contains | and next line is separator |---|)
        if (line.trim().startsWith("|") && line.trim().endsWith("|") && i + 1 < lines.size) {
            val nextLine = lines[i + 1].trim()
            val isDivider = nextLine.startsWith("|") && nextLine.contains("---")
            if (isDivider) {
                val headerCells = line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                val rows = mutableListOf<List<String>>()
                i += 2 // skip header and divider

                while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                    val rowCells = lines[i].split("|").map { it.trim() }.filter { it.isNotEmpty() }
                    if (rowCells.isNotEmpty()) {
                        rows.add(rowCells)
                    }
                    i++
                }

                if (headerCells.isNotEmpty() && rows.isNotEmpty()) {
                    blocks.add(MarkdownBlock.TableBlock(headerCells, rows))
                    continue
                }
            }
        }

        // Standard Text Block (gather consecutive non-special lines)
        val textLines = mutableListOf<String>()
        while (i < lines.size) {
            val currentLine = lines[i]
            val isNextCode = currentLine.trimStart().startsWith("```")
            val isNextTable = currentLine.trim().startsWith("|") &&
                    currentLine.trim().endsWith("|") &&
                    i + 1 < lines.size &&
                    lines[i + 1].trim().startsWith("|") &&
                    lines[i + 1].trim().contains("---")

            if (isNextCode || isNextTable) {
                break
            }
            textLines.add(currentLine)
            i++
        }

        if (textLines.isNotEmpty()) {
            val textContent = textLines.joinToString("\n").trim()
            if (textContent.isNotEmpty()) {
                blocks.add(MarkdownBlock.TextBlock(textContent))
            }
        }
    }

    return if (blocks.isEmpty()) listOf(MarkdownBlock.TextBlock(rawText)) else blocks
}

/**
 * Builds an AnnotatedString from markdown text handling **bold**, *italic*, `code`, and # headings.
 */
fun buildStyledMarkdownAnnotatedString(text: String, isUserMessage: Boolean): AnnotatedString {
    val baseColor = if (isUserMessage) Color.White else DesiTextPrimary
    val boldColor = if (isUserMessage) Color.White else Color(0xFFFFD54F) // warm bright amber for bold highlights
    val codeBgColor = if (isUserMessage) Color(0x33000000) else Color(0xFF2A2A2A)

    return buildAnnotatedString {
        val lines = text.lines()
        for ((lineIdx, line) in lines.withIndex()) {
            val trimmed = line.trim()
            
            // Check for Heading (#, ##, ###)
            when {
                trimmed.startsWith("### ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (isUserMessage) Color.White else DesiAmberSecondary)) {
                        append(trimmed.removePrefix("### "))
                    }
                }
                trimmed.startsWith("## ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isUserMessage) Color.White else DesiOrangePrimary)) {
                        append(trimmed.removePrefix("## "))
                    }
                }
                trimmed.startsWith("# ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = if (isUserMessage) Color.White else DesiOrangePrimary)) {
                        append(trimmed.removePrefix("# "))
                    }
                }
                else -> {
                    // Parse inline bold **text**, italic *text*, `code`
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
        // Check for Bold **...**
        if (cursor + 1 < line.length && line[cursor] == '*' && line[cursor + 1] == '*') {
            val endIdx = line.indexOf("**", cursor + 2)
            if (endIdx != -1) {
                val boldContent = line.substring(cursor + 2, endIdx)
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = boldColor)) {
                    append(boldContent)
                }
                cursor = endIdx + 2
                continue
            }
        }

        // Check for Inline Code `...`
        if (line[cursor] == '`') {
            val endIdx = line.indexOf('`', cursor + 1)
            if (endIdx != -1) {
                val codeContent = line.substring(cursor + 1, endIdx)
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBgColor,
                        color = Color(0xFFFFB74D),
                        fontSize = 13.sp
                    )
                ) {
                    append(" $codeContent ")
                }
                cursor = endIdx + 1
                continue
            }
        }

        // Regular character
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
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 22.sp,
                            letterSpacing = 0.2.sp
                        )
                    )
                }
                is MarkdownBlock.CodeBlock -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DesiDarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            if (block.language.isNotBlank()) {
                                Text(
                                    text = block.language.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = DesiOrangePrimary,
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Text(
                                text = block.code,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                ),
                                color = Color(0xFFECEFF1)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                is MarkdownBlock.TableBlock -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    MarkdownTable(headers = block.headers, rows = block.rows)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
            if (index < blocks.size - 1 && block !is MarkdownBlock.TableBlock && block !is MarkdownBlock.CodeBlock) {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
fun MarkdownTable(
    headers: List<String>,
    rows: List<List<String>>,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DesiDarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DesiDarkBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Table Header Row
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF2A221C))
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    headers.forEachIndexed { idx, header ->
                        Box(
                            modifier = Modifier
                                .width(135.dp)
                                .padding(horizontal = 6.dp)
                        ) {
                            Text(
                                text = header.replace("**", "").trim(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DesiOrangePrimary
                                ),
                                fontSize = 12.sp
                            )
                        }
                        if (idx < headers.size - 1) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(18.dp)
                                    .background(DesiDarkBorder)
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    thickness = 1.dp,
                    color = DesiDarkBorder
                )

                // Table Data Rows
                rows.forEachIndexed { rowIdx, rowCells ->
                    val isEven = rowIdx % 2 == 0
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isEven) Color(0x0DFFFFFF) else Color.Transparent)
                            .padding(vertical = 7.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        headers.indices.forEach { colIdx ->
                            val cellContent = rowCells.getOrNull(colIdx) ?: ""
                            Box(
                                modifier = Modifier
                                    .width(135.dp)
                                    .padding(horizontal = 6.dp)
                            ) {
                                Text(
                                    text = buildStyledMarkdownAnnotatedString(cellContent, isUserMessage = false),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.5.sp,
                                        lineHeight = 16.sp
                                    )
                                )
                            }
                            if (colIdx < headers.size - 1) {
                                Box(
                                    modifier = Modifier
                                    .width(1.dp)
                                    .height(18.dp)
                                    .background(Color(0x1AFFFFFF))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
