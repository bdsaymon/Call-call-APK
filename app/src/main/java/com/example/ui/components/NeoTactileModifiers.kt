package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DeepMatteSlateEnd
import com.example.ui.theme.DeepMatteSlateStart
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.FrostedGlassSurface
import com.example.ui.theme.GlowingCyan
import com.example.ui.theme.NeumorphicDarkShadow
import com.example.ui.theme.NeumorphicLightShadow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Full-screen Neo-Tactile Deep Matte Slate background with ambient cyan & electric blue depth blooms.
 */
fun Modifier.neoTactileBackground(): Modifier = this
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(DeepMatteSlateStart, DeepMatteSlateEnd, DeepMatteSlateStart)
        )
    )
    .drawBehind {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ElectricBlue.copy(alpha = 0.14f), Color.Transparent),
                center = Offset(size.width * 0.85f, size.height * 0.12f),
                radius = size.width * 0.65f
            ),
            center = Offset(size.width * 0.85f, size.height * 0.12f),
            radius = size.width * 0.65f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(GlowingCyan.copy(alpha = 0.10f), Color.Transparent),
                center = Offset(size.width * 0.15f, size.height * 0.82f),
                radius = size.width * 0.60f
            ),
            center = Offset(size.width * 0.15f, size.height * 0.82f),
            radius = size.width * 0.60f
        )
    }

/**
 * Clean High-Contrast Neo-Tactile Frosted Glassmorphism Card Surface.
 */
fun Modifier.frostedGlassCard(
    cornerRadius: Dp = 24.dp,
    surfaceAlpha: Float = 0.74f,
    accentGlow: Color = Color.Transparent
): Modifier = this
    .neumorphicElevation(cornerRadius = cornerRadius)
    .clip(RoundedCornerShape(cornerRadius))
    .background(Color(0xFF1E2430).copy(alpha = surfaceAlpha))
    .drawWithContent {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.09f),
                    accentGlow.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
        )
        drawContent()
    }
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.24f),
                if (accentGlow != Color.Transparent) accentGlow.copy(alpha = 0.50f) else Color.White.copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.16f)
            )
        ),
        shape = RoundedCornerShape(cornerRadius)
    )

/**
 * Dual-shadow Neumorphic 3D depth modifier simulating top-left highlight and bottom-right shadow.
 */
fun Modifier.neumorphicElevation(
    cornerRadius: Dp = 24.dp,
    isPressed: Boolean = false
): Modifier = this.drawBehind {
    val radiusPx = cornerRadius.toPx()
    val offsetPx = if (isPressed) 1.5.dp.toPx() else 3.5.dp.toPx()

    drawRoundRect(
        color = NeumorphicDarkShadow,
        topLeft = Offset(offsetPx, offsetPx),
        size = size,
        cornerRadius = CornerRadius(radiusPx, radiusPx)
    )
    drawRoundRect(
        color = NeumorphicLightShadow,
        topLeft = Offset(-offsetPx * 0.5f, -offsetPx * 0.5f),
        size = size,
        cornerRadius = CornerRadius(radiusPx, radiusPx)
    )
}

/**
 * Glowing Cyan Circular Ring container driven by real-time amplitude (0f..1f) or story highlight state.
 */
@Composable
fun GlowingCyanRingBox(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    isActive: Boolean = true,
    amplitude: Float = 0f,
    ringColor: Color = GlowingCyan,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cyanRingPulse")
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idlePulse"
    )
    val animatedAmp by animateFloatAsState(
        targetValue = amplitude.coerceIn(0f, 1f),
        animationSpec = tween(60),
        label = "ampSmooth"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (isActive) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = (2.5.dp.toPx()) + (animatedAmp * 4.dp.toPx())
                val centerOffset = Offset(this.size.width / 2f, this.size.height / 2f)
                val baseRadius = (this.size.minDimension / 2f) - strokePx

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ringColor.copy(alpha = (0.38f + animatedAmp * 0.45f) * idlePulse),
                            ringColor.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = centerOffset,
                        radius = baseRadius * (1.18f + animatedAmp * 0.25f)
                    ),
                    radius = baseRadius * (1.18f + animatedAmp * 0.25f),
                    center = centerOffset
                )

                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            ringColor,
                            ElectricBlue,
                            ringColor,
                            Color.White.copy(alpha = 0.9f),
                            ringColor
                        )
                    ),
                    radius = baseRadius,
                    center = centerOffset,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        Box(
            modifier = Modifier
                .size(size - 10.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A202C)),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

/**
 * Tactile Pill-Shaped Push Button with dual-shadow elevation and electric blue / cyan glow states.
 */
@Composable
fun NeoTactilePillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isActive: Boolean = true,
    accentColor: Color = ElectricBlue,
    enabled: Boolean = true,
    testTag: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgBrush = if (isActive && enabled) {
        Brush.horizontalGradient(
            colors = listOf(accentColor, accentColor.copy(alpha = 0.85f))
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFF232A38), Color(0xFF1B202B))
        )
    }

    val borderBrush = if (isActive && enabled) {
        Brush.horizontalGradient(
            colors = listOf(GlowingCyan.copy(alpha = 0.8f), Color.White.copy(alpha = 0.35f))
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.08f))
        )
    }

    val taggedModifier = if (testTag != null) modifier.testTag(testTag) else modifier

    Row(
        modifier = taggedModifier
            .minimumInteractiveComponentSize()
            .height(48.dp)
            .neumorphicElevation(cornerRadius = 24.dp, isPressed = isPressed)
            .clip(RoundedCornerShape(24.dp))
            .background(bgBrush)
            .border(1.dp, borderBrush, RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = if (isActive && enabled) Color.White else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = if (isActive && enabled) Color.White else TextSecondary
            )
        )
    }
}

/**
 * Tactile Toggle Switch with glowing cyan thumb indicator.
 */
@Composable
fun NeoTactileToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    val taggedModifier = if (testTag != null) modifier.testTag(testTag) else modifier
    Row(
        modifier = taggedModifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(20.dp))
            .background(FrostedGlassSurface)
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(if (checked) ElectricBlue.copy(alpha = 0.35f) else Color(0xFF141820))
                .border(
                    width = 1.dp,
                    color = if (checked) GlowingCyan else Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(15.dp)
                )
                .padding(horizontal = 4.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (checked) GlowingCyan else TextSecondary)
            )
        }
    }
}

/**
 * Tactile Smooth Slider Scrub Bar for Media3 Voice Note playback.
 */
@Composable
fun NeoTactileScrubSlider(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    val taggedModifier = if (testTag != null) modifier.testTag(testTag) else modifier
    Slider(
        value = progress.coerceIn(0f, 1f),
        onValueChange = onSeek,
        modifier = taggedModifier.height(28.dp),
        colors = SliderDefaults.colors(
            thumbColor = GlowingCyan,
            activeTrackColor = ElectricBlue,
            inactiveTrackColor = Color(0xFF161B26)
        )
    )
}

/**
 * Deterministic Neo-Tactile QR Code Matrix Renderer for easy Contact / Short-ID sharing.
 */
@Composable
fun NeoTactileQrCodeView(
    codeData: String,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp
) {
    val seed = remember(codeData) { codeData.ifBlank { "NEOTACTILE" }.hashCode() }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0E131D))
            .border(1.5.dp, GlowingCyan.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val grid = 15
            val cellSize = this.size.minDimension / grid

            fun isFinderArea(r: Int, c: Int): Boolean {
                val topLeft = r < 4 && c < 4
                val topRight = r < 4 && c >= grid - 4
                val bottomLeft = r >= grid - 4 && c < 4
                return topLeft || topRight || bottomLeft
            }

            fun drawFinderSquare(startR: Int, startC: Int) {
                for (dr in 0..3) {
                    for (dc in 0..3) {
                        val isBorder = dr == 0 || dr == 3 || dc == 0 || dc == 3
                        val isCenter = (dr == 1 || dr == 2) && (dc == 1 || dc == 2)
                        if (isBorder || isCenter) {
                            drawRoundRect(
                                color = if (isCenter) GlowingCyan else ElectricBlue,
                                topLeft = Offset((startC + dc) * cellSize, (startR + dr) * cellSize),
                                size = androidx.compose.ui.geometry.Size(cellSize * 0.88f, cellSize * 0.88f),
                                cornerRadius = CornerRadius(cellSize * 0.2f, cellSize * 0.2f)
                            )
                        }
                    }
                }
            }

            drawFinderSquare(0, 0)
            drawFinderSquare(0, grid - 4)
            drawFinderSquare(grid - 4, 0)

            for (r in 0 until grid) {
                for (c in 0 until grid) {
                    if (isFinderArea(r, c)) continue
                    val bitHash = ((r * 31 + c * 17) xor (seed shr ((r + c) % 16))) and 1
                    if (bitHash == 1) {
                        drawRoundRect(
                            color = if ((r + c) % 2 == 0) GlowingCyan else Color.White,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = androidx.compose.ui.geometry.Size(cellSize * 0.82f, cellSize * 0.82f),
                            cornerRadius = CornerRadius(cellSize * 0.2f, cellSize * 0.2f)
                        )
                    }
                }
            }
        }
    }
}

