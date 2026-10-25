package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ChatCodeBg
import com.example.ui.theme.ChatTextMuted
import com.example.ui.theme.ChatTextPrimary
import com.example.ui.theme.ChatTextSecondary
import kotlinx.coroutines.delay

sealed class MarkdownBlock {
    data class TextBlock(val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class TableBlock(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock()
}

private fun isTableRowLine(line: String): Boolean {
    val trimmed = line.trim()
    return trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.count { it == '|' } >= 2
}

private fun isTableSeparatorLine(line: String): Boolean {
    val trimmed = line.trim()
    if (!isTableRowLine(trimmed)) return false
    val cells = trimmed.removePrefix("|").removeSuffix("|").split("|")
    return cells.isNotEmpty() && cells.all { cell ->
        val c = cell.trim()
        c.isNotEmpty() && c.all { ch -> ch == '-' || ch == ':' || ch == ' ' } && c.contains('-')
    }
}

private fun parseTableCells(line: String): List<String> {
    return line.trim()
        .removePrefix("|")
        .removeSuffix("|")
        .split("|")
        .map { it.trim() }
}

fun parseMarkdownBlocks(rawText: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = rawText.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // 1. Fenced Code Block
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

        // 2. Markdown Table Block
        if (i + 1 < lines.size && isTableRowLine(lines[i]) && isTableSeparatorLine(lines[i + 1])) {
            val headers = parseTableCells(lines[i])
            val tableRows = mutableListOf<List<String>>()
            i += 2
            while (i < lines.size && isTableRowLine(lines[i])) {
                if (!isTableSeparatorLine(lines[i])) {
                    tableRows.add(parseTableCells(lines[i]))
                }
                i++
            }
            blocks.add(MarkdownBlock.TableBlock(headers = headers, rows = tableRows))
            continue
        }

        // 3. Regular Text / Headings / Lists Block
        val textLines = mutableListOf<String>()
        while (
            i < lines.size &&
            !lines[i].trimStart().startsWith("```") &&
            !(i + 1 < lines.size && isTableRowLine(lines[i]) && isTableSeparatorLine(lines[i + 1]))
        ) {
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

private val bulletRegex = Regex("^([-*•])\\s+(.*)$")
private val numberedListRegex = Regex("^(\\d+[.)])\\s+(.*)$")
private val headingRegex = Regex("^(#{1,6})\\s+(.*)$")

fun buildStyledMarkdownAnnotatedString(text: String, isUserMessage: Boolean): AnnotatedString {
    val baseColor = if (isUserMessage) Color.White else ChatTextPrimary
    val boldColor = Color.White
    val codeBgColor = Color(0xFF1E1E1E)
    val linkColor = Color(0xFF7CB8FF)

    return buildAnnotatedString {
        val lines = text.lines()
        for ((lineIdx, line) in lines.withIndex()) {
            val trimmed = line.trim()
            val headingMatch = headingRegex.find(trimmed)
            val bulletMatch = if (headingMatch == null) bulletRegex.find(trimmed) else null
            val numberedMatch = if (headingMatch == null && bulletMatch == null) numberedListRegex.find(trimmed) else null

            when {
                headingMatch != null -> {
                    val level = headingMatch.groupValues[1].length
                    val headingContent = headingMatch.groupValues[2].trim()
                    val fontSize = when (level) {
                        1 -> 19.sp
                        2 -> 17.5.sp
                        3 -> 16.5.sp
                        else -> 15.8.sp
                    }
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSize,
                            color = boldColor
                        )
                    ) {
                        parseInlineFormatting(headingContent, boldColor, boldColor, codeBgColor, linkColor)
                    }
                }
                bulletMatch != null -> {
                    val itemText = bulletMatch.groupValues[2]
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = boldColor)) {
                        append("  •  ")
                    }
                    parseInlineFormatting(itemText, baseColor, boldColor, codeBgColor, linkColor)
                }
                numberedMatch != null -> {
                    val prefix = numberedMatch.groupValues[1]
                    val itemText = numberedMatch.groupValues[2]
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = boldColor)) {
                        append("  $prefix ")
                    }
                    parseInlineFormatting(itemText, baseColor, boldColor, codeBgColor, linkColor)
                }
                else -> {
                    parseInlineFormatting(line, baseColor, boldColor, codeBgColor, linkColor)
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
    codeBgColor: Color,
    linkColor: Color
) {
    var cursor = 0
    while (cursor < line.length) {
        // 1. Inline code: `code`
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

        // 2. Markdown link: [label](https://...)
        if (line[cursor] == '[') {
            val closeBracket = line.indexOf(']', cursor + 1)
            if (closeBracket != -1 && closeBracket + 1 < line.length && line[closeBracket + 1] == '(') {
                val closeParen = line.indexOf(')', closeBracket + 2)
                if (closeParen != -1) {
                    val label = line.substring(cursor + 1, closeBracket)
                    val url = line.substring(closeBracket + 2, closeParen).trim()
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        withLink(
                            LinkAnnotation.Url(
                                url = url,
                                styles = TextLinkStyles(
                                    style = SpanStyle(
                                        color = linkColor,
                                        textDecoration = TextDecoration.Underline,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            )
                        ) {
                            append(label.ifBlank { url })
                        }
                        cursor = closeParen + 1
                        continue
                    }
                }
            }
        }

        // 3. Bare URL: https://... or http://...
        if (line.startsWith("https://", cursor) || line.startsWith("http://", cursor)) {
            var endUrl = cursor
            while (endUrl < line.length && !line[endUrl].isWhitespace() && line[endUrl] !in listOf(')', ']', '>', '"', '\'')) {
                endUrl++
            }
            while (endUrl > cursor && line[endUrl - 1] in listOf('.', ',', ';', ':')) {
                endUrl--
            }
            val url = line.substring(cursor, endUrl)
            if (url.length > 8) {
                withLink(
                    LinkAnnotation.Url(
                        url = url,
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline
                            )
                        )
                    )
                ) {
                    append(url)
                }
                cursor = endUrl
                continue
            }
        }

        // 4. Bold + Italic: ***text***
        if (cursor + 2 < line.length && line.startsWith("***", cursor)) {
            val endIdx = line.indexOf("***", cursor + 3)
            if (endIdx != -1) {
                val content = line.substring(cursor + 3, endIdx)
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        color = boldColor
                    )
                ) {
                    append(content)
                }
                cursor = endIdx + 3
                continue
            }
        }

        // 5. Bold: **text**
        if (cursor + 1 < line.length && line[cursor] == '*' && line[cursor + 1] == '*') {
            val endIdx = line.indexOf("**", cursor + 2)
            if (endIdx != -1) {
                val boldContent = line.substring(cursor + 2, endIdx)
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = boldColor)) {
                    parseInlineFormatting(boldContent, boldColor, boldColor, codeBgColor, linkColor)
                }
                cursor = endIdx + 2
                continue
            }
        }

        // 6. Italic: *text* (when not **)
        if (line[cursor] == '*' && (cursor + 1 < line.length && line[cursor + 1] != '*' && line[cursor + 1] != ' ')) {
            val endIdx = line.indexOf('*', cursor + 1)
            if (endIdx != -1 && endIdx > cursor + 1) {
                val italicContent = line.substring(cursor + 1, endIdx)
                withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = baseColor)) {
                    append(italicContent)
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
    val blocks = remember(rawText) { parseMarkdownBlocks(rawText) }

    Column(modifier = modifier.fillMaxWidth()) {
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
                    MarkdownCodeBlockCard(
                        language = block.language,
                        code = block.code
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                is MarkdownBlock.TableBlock -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    MarkdownTableCard(
                        headers = block.headers,
                        rows = block.rows
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            if (index < blocks.size - 1) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MarkdownCodeBlockCard(
    language: String,
    code: String
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1800)
            copied = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ChatCodeBg)
            .border(1.dp, Color(0xFF242424), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language.lowercase().ifBlank { "code" },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = ChatTextSecondary,
                    fontSize = 11.5.sp
                )
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("code", code))
                        copied = true
                    }
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                    contentDescription = "Copy code",
                    tint = if (copied) Color(0xFF8BE28B) else ChatTextMuted,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (copied) "Copied" else "Copy",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.5.sp,
                        color = if (copied) Color(0xFF8BE28B) else ChatTextSecondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Color(0xFF222222), thickness = 1.dp)
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Text(
                text = code,
                softWrap = false,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = Color(0xFFE5E5E5)
                )
            )
        }
    }
}

@Composable
private fun MarkdownTableCard(
    headers: List<String>,
    rows: List<List<String>>
) {
    val columnCount = maxOf(headers.size, rows.maxOfOrNull { it.size } ?: 0)
    if (columnCount == 0) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF141414))
            .border(1.dp, Color(0xFF282828), RoundedCornerShape(10.dp))
            .horizontalScroll(rememberScrollState())
    ) {
        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1D1D1D))
                    .padding(vertical = 8.dp, horizontal = 6.dp)
            ) {
                for (col in 0 until columnCount) {
                    val cellText = headers.getOrNull(col).orEmpty()
                    Box(
                        modifier = Modifier
                            .widthIn(min = 96.dp, max = 240.dp)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = buildStyledMarkdownAnnotatedString(cellText, false),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF2A2A2A), thickness = 1.dp)

            // Data rows
            rows.forEachIndexed { rowIdx, rowCells ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (rowIdx % 2 == 0) Color(0xFF141414) else Color(0xFF181818))
                        .padding(vertical = 7.dp, horizontal = 6.dp)
                ) {
                    for (col in 0 until columnCount) {
                        val cellText = rowCells.getOrNull(col).orEmpty()
                        Box(
                            modifier = Modifier
                                .widthIn(min = 96.dp, max = 240.dp)
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = buildStyledMarkdownAnnotatedString(cellText, false),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    color = ChatTextPrimary
                                )
                            )
                        }
                    }
                }
                if (rowIdx < rows.size - 1) {
                    HorizontalDivider(color = Color(0xFF222222), thickness = 0.5.dp)
                }
            }
        }
    }
}
