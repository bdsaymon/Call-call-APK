package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.example.data.model.BlockedDevice
import com.example.security.DeviceIdentityManager
import com.example.ui.components.GlowingCyanRingBox
import com.example.ui.components.NeoTactilePillButton
import com.example.ui.components.frostedGlassCard
import com.example.ui.components.neoTactileBackground
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlowingCyan
import com.example.ui.theme.TactileCrimson
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Phase 3.2: Device Gatekeeper Lockout Screen when blocked_devices/{androidId}.banned == true.
 */
@Composable
fun DeviceBannedScreen(
    device: BlockedDevice,
    isAuthorizedAdmin: Boolean,
    onEmergencyAdminUnban: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .neoTactileBackground()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp)
            .testTag("device_banned_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .frostedGlassCard(cornerRadius = 32.dp, accentGlow = TactileCrimson)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GlowingCyanRingBox(
                size = 88.dp,
                isActive = true,
                amplitude = 0.8f,
                ringColor = TactileCrimson
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = "Hardware Banned",
                    tint = TactileCrimson,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(id = R.string.device_banned_title),
                style = MaterialTheme.typography.headlineMedium,
                color = TactileCrimson,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Hardware ID (${device.hardwareId}) is blacklisted in Firestore blocked_devices.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Reason: ${device.reason.ifBlank { "Administrative hardware revocation" }}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            if (isAuthorizedAdmin) {
                Spacer(modifier = Modifier.height(24.dp))
                NeoTactilePillButton(
                    text = "Admin Emergency Unban This Device",
                    onClick = onEmergencyAdminUnban,
                    accentColor = ElectricBlue,
                    testTag = "emergency_admin_unban_button"
                )
            }
        }
    }
}

/**
 * Phase 3.3: Biometric & PIN Security Gate triggered on launch or >60s background resume.
 */
@Composable
fun BiometricLockScreen(
    hardwareId: String,
    deviceIdentityManager: DeviceIdentityManager,
    onUnlockWithPin: (String) -> Boolean
) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var feedbackText by remember {
        mutableStateOf("Session locked after background timeout. Authenticate via BiometricPrompt or PIN (2026).")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .neoTactileBackground()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp)
            .testTag("biometric_lock_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .frostedGlassCard(cornerRadius = 32.dp, accentGlow = GlowingCyan)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlowingCyanRingBox(
                size = 88.dp,
                isActive = true,
                amplitude = 0.6f
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Biometric Security Gate",
                    tint = GlowingCyan,
                    modifier = Modifier.size(44.dp)
                )
            }

            Text(
                text = stringResource(id = R.string.biometric_lock_title),
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = feedbackText,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = pinInput,
                onValueChange = { pinInput = it },
                label = { Text("Security PIN (Default: 2026)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GlowingCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("biometric_pin_input")
            )

            NeoTactilePillButton(
                text = "Unlock with PIN",
                icon = Icons.Default.LockOpen,
                onClick = {
                    val ok = onUnlockWithPin(pinInput.ifBlank { "2026" })
                    if (!ok) {
                        feedbackText = "Invalid PIN. Enter 2026 to unlock."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "unlock_pin_button"
            )

            NeoTactilePillButton(
                text = "Launch BiometricPrompt",
                icon = Icons.Default.Fingerprint,
                isActive = false,
                onClick = {
                    val fragmentActivity = context as? FragmentActivity
                    if (fragmentActivity != null) {
                        deviceIdentityManager.triggerBiometricPrompt(
                            activity = fragmentActivity,
                            onSuccess = {},
                            onFallbackOrError = { msg -> feedbackText = msg }
                        )
                    } else {
                        feedbackText = "Use tactile PIN pad above (PIN: 2026)."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "trigger_biometric_prompt_button"
            )
        }
    }
}
