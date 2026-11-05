package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LightBackgroundWhite
import com.example.ui.theme.LightDividerGray
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.theme.WhatsAppAccentGreen
import com.example.ui.theme.WhatsAppActionGreen
import com.example.ui.theme.WhatsAppHeaderGreen

/**
 * Clean White background modifier matching the WhatsApp reference screens.
 */
fun Modifier.neoTactileBackground(): Modifier = this
    .background(LightBackgroundWhite)

/**
 * Clean Flat White Card Surface with subtle gray divider border (no dark glassmorphism).
 */
fun Modifier.frostedGlassCard(
    cornerRadius: Dp = 12.dp,
    surfaceAlpha: Float = 1f,
    accentGlow: Color = Color.Transparent
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(Color.White.copy(alpha = surfaceAlpha))
    .border(
        width = 1.dp,
        color = LightDividerGray,
        shape = RoundedCornerShape(cornerRadius)
    )

fun Modifier.neumorphicElevation(
    cornerRadius: Dp = 12.dp,
    isPressed: Boolean = false
): Modifier = this

/**
 * Clean Circular Avatar / Status Ring Box matching the green status ring in WhatsApp UI Reference.
 */
@Composable
fun GlowingCyanRingBox(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    isActive: Boolean = true,
    amplitude: Float = 0f,
    ringColor: Color = WhatsAppAccentGreen,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (isActive) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = 2.5.dp.toPx()
                val centerOffset = Offset(this.size.width / 2f, this.size.height / 2f)
                val baseRadius = (this.size.minDimension / 2f) - strokePx
                drawCircle(
                    color = ringColor,
                    radius = baseRadius,
                    center = centerOffset,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        Box(
            modifier = Modifier
                .size(if (isActive) size - 6.dp else size)
                .clip(CircleShape)
                .background(Color(0xFFE7FCE8)),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

/**
 * Solid Green Pill Button matching "Create", "Create community", "Link a device", and "Save to gallery" in reference UI.
 */
@Composable
fun NeoTactilePillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isActive: Boolean = true,
    accentColor: Color = WhatsAppActionGreen,
    enabled: Boolean = true,
    testTag: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val bgColor = if (isActive && enabled) accentColor else Color(0xFFE9EDEF)
    val contentColor = if (isActive && enabled) Color.White else LightTextSecondary
    val taggedModifier = if (testTag != null) modifier.testTag(testTag) else modifier

    Row(
        modifier = taggedModifier
            .minimumInteractiveComponentSize()
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        )
    }
}

/**
 * Standard Clean Green & White Toggle Switch Row matching Privacy / Chat Settings in Reference 1 & 2.
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
            .background(Color.White)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = LightTextPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LightTextSecondary
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = WhatsAppActionGreen,
                uncheckedThumbColor = Color(0xFF8696A0),
                uncheckedTrackColor = Color(0xFFE9EDEF)
            )
        )
    }
}

/**
 * Smooth Voice Note Playback Slider.
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
        modifier = taggedModifier.height(24.dp),
        colors = SliderDefaults.colors(
            thumbColor = WhatsAppActionGreen,
            activeTrackColor = WhatsAppActionGreen,
            inactiveTrackColor = Color(0xFFD1D7DB)
        )
    )
}

/**
 * Black & White Crisp QR Code Renderer matching Screen 29 (QR code) in WhatsApp_UI_Reference_1.png.
 */
@Composable
fun NeoTactileQrCodeView(
    codeData: String,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp
) {
    val seed = remember(codeData) { codeData.ifBlank { "WHATSAPP_QR" }.hashCode() }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, LightDividerGray, RoundedCornerShape(12.dp))
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val grid = 17
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
                                color = if (isCenter) WhatsAppHeaderGreen else Color(0xFF111B21),
                                topLeft = Offset((startC + dc) * cellSize, (startR + dr) * cellSize),
                                size = androidx.compose.ui.geometry.Size(cellSize * 0.9f, cellSize * 0.9f),
                                cornerRadius = CornerRadius(cellSize * 0.15f, cellSize * 0.15f)
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
                            color = Color(0xFF111B21),
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = androidx.compose.ui.geometry.Size(cellSize * 0.86f, cellSize * 0.86f),
                            cornerRadius = CornerRadius(cellSize * 0.12f, cellSize * 0.12f)
                        )
                    }
                }
            }
        }
    }
}
