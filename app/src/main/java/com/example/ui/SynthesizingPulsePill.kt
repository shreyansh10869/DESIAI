package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Clean minimalist floating glassmorphic loading capsule with zero verbose text badges:
 * - Fast Mode (Gemini Active): 3 minimal glowing cyan dots (#00F2FE, 7.dp) in a staggered (120ms)
 *   infinite vertical sine wave bounce with subtle cyan bloom.
 * - Deep Think Mode (Reasoning Active): 4 sleek breathing pearl dots (#9B51E0, 6.5.dp) with soft
 *   neon purple glow, animating with smooth opacity (0.35f..1.0f) & scale (0.85f..1.15f)
 *   traveling horizontally like an intelligent neural wave.
 */
@Composable
fun SynthesizingPulsePill(
    isDeepThinkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val capsuleBackground = Color(0xFF0F111A).copy(alpha = 0.85f)
    val capsuleBorder = Color.White.copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(capsuleBackground)
            .border(1.dp, capsuleBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("synthesizing_pulse_pill"),
        contentAlignment = Alignment.Center
    ) {
        if (!isDeepThinkMode) {
            FastModeSineBounceDots()
        } else {
            DeepThinkNeuralPearlDots()
        }
    }
}

@Composable
private fun FastModeSineBounceDots() {
    val cyanColor = Color(0xFF00F2FE)
    val infiniteTransition = rememberInfiniteTransition(label = "fast_mode_sine_wave")

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val phase by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = (2f * PI).toFloat(),
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 900, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(offsetMillis = index * 120)
                ),
                label = "fast_dot_phase_$index"
            )

            val verticalOffsetPx = (-sin(phase.toDouble()) * 5.5).toFloat()
            val glowAlpha = (0.55f + 0.45f * ((sin(phase.toDouble()).toFloat() + 1f) / 2f)).coerceIn(0.35f, 1f)

            Box(
                modifier = Modifier
                    .size(7.dp)
                    .graphicsLayer {
                        translationY = verticalOffsetPx
                        alpha = glowAlpha
                    }
                    .drawBehind {
                        // Subtle cyan bloom aura behind each dot
                        drawCircle(
                            color = cyanColor.copy(alpha = 0.32f * glowAlpha),
                            radius = size.minDimension * 0.95f
                        )
                    }
                    .clip(CircleShape)
                    .background(cyanColor)
            )
        }
    }
}

@Composable
private fun DeepThinkNeuralPearlDots() {
    val purpleColor = Color(0xFF9B51E0)
    val infiniteTransition = rememberInfiniteTransition(label = "deep_think_neural_wave")

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(4) { index ->
            val pulseProgress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 680, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(offsetMillis = index * 140)
                ),
                label = "deep_pearl_pulse_$index"
            )

            val dotAlpha = 0.35f + (1.0f - 0.35f) * pulseProgress
            val dotScale = 0.85f + (1.15f - 0.85f) * pulseProgress

            Box(
                modifier = Modifier
                    .size(6.5.dp)
                    .graphicsLayer {
                        scaleX = dotScale
                        scaleY = dotScale
                        alpha = dotAlpha
                    }
                    .drawBehind {
                        // Soft neon purple glow around each breathing pearl dot
                        drawCircle(
                            color = purpleColor.copy(alpha = 0.36f * dotAlpha),
                            radius = size.minDimension * 1.05f
                        )
                    }
                    .clip(CircleShape)
                    .background(purpleColor)
            )
        }
    }
}
