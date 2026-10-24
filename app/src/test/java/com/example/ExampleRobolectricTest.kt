package com.example

import com.example.ai.AiEngineMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleRobolectricTest {

    @Test
    fun `verify Auralis AI engine modes`() {
        assertEquals("Fast Mode", AiEngineMode.FAST_MODE.displayName)
        assertEquals("Deep Think", AiEngineMode.DEEP_THINK.displayName)
    }
}
