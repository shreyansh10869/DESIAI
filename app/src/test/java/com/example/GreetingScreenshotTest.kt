package com.example

import com.example.ui.parseMarkdownBlocks
import org.junit.Assert.assertTrue
import org.junit.Test

class GreetingScreenshotTest {

    @Test
    fun `markdown parser parses Auralis AI blocks cleanly`() {
        val blocks = parseMarkdownBlocks("### Auralis AI\n**Cyber-Desi Luxe**")
        assertTrue(blocks.isNotEmpty())
    }
}
