package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Wallpaper
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.UserProfile
import com.example.data.model.generateShortCode
import com.example.ui.components.GlowingCyanRingBox
import com.example.ui.components.NeoTactilePillButton
import com.example.ui.components.NeoTactileQrCodeView
import com.example.ui.components.NeoTactileToggleSwitch
import com.example.ui.components.frostedGlassCard
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlowingCyan
import com.example.ui.theme.TactileCrimson
import com.example.ui.theme.TactileEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ProfileScreen(
    language: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    isLightTheme: Boolean = false,
    onToggleLightTheme: (Boolean) -> Unit = {},
    blockedCount: Int = 0,
    userProfile: UserProfile?,
    currentUserId: String,
    fallbackEmail: String,
    fallbackName: String,
    hardwareId: String,
    deviceModel: String,
    biometricGateEnabled: Boolean,
    onUpdateProfile: (displayName: String, bio: String, isPrivate: Boolean, coverPreset: String, username: String, phone: String) -> Unit,
    onToggleBiometricGate: (Boolean) -> Unit,
    onLockNow: () -> Unit,
    onSignOut: () -> Unit
) {
    var displayName by remember(userProfile) {
        mutableStateOf(userProfile?.displayName?.ifBlank { fallbackName } ?: fallbackName)
    }
    var username by remember(userProfile) {
        mutableStateOf(userProfile?.username ?: fallbackName.lowercase().replace(Regex("[^a-z0-9]"), ""))
    }
    var phoneNumber by remember(userProfile) {
        mutableStateOf(userProfile?.phoneNumber ?: "")
    }
    var bio by remember(userProfile) {
        mutableStateOf(userProfile?.cleanBio ?: "Hey there! I am using NeoTactile.")
    }
    var isPrivate by remember(userProfile) {
        mutableStateOf(userProfile?.isPrivate ?: false)
    }
    var readReceiptsEnabled by remember { mutableStateOf(true) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var twoStepEnabled by remember { mutableStateOf(true) }
    var selectedWallpaper by remember { mutableStateOf("Emerald Matrix") }

    val shortCode = remember(currentUserId) { generateShortCode(currentUserId) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_screen_list"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Profile Header + Overlapping Avatar
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .frostedGlassCard(cornerRadius = 28.dp, accentGlow = ElectricBlue)
                    .padding(bottom = 16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(136.dp)
                            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_notice_banner_art),
                            contentDescription = "Profile Cover Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    GlowingCyanRingBox(
                        size = 90.dp,
                        isActive = true,
                        amplitude = 0.45f,
                        modifier = Modifier.offset(y = (-42).dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User Avatar",
                            tint = GlowingCyan,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .offset(y = (-28).dp)
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "@$username • ${userProfile?.email?.ifBlank { fallbackEmail } ?: fallbackEmail} • ID: $shortCode",
                            style = MaterialTheme.typography.labelSmall,
                            color = GlowingCyan
                        )
                    }
                }
            }
        }

        // 2. Theme (Green & White Light / Dark) & Language Preferences
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .frostedGlassCard(cornerRadius = 24.dp, accentGlow = GlowingCyan)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Contrast,
                        contentDescription = null,
                        tint = GlowingCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = language.tr("থিম ও অ্যাপের ভাষা (THEME & LANGUAGE)", "THEME & LANGUAGE"),
                        style = MaterialTheme.typography.labelLarge,
                        color = GlowingCyan
                    )
                }

                NeoTactileToggleSwitch(
                    checked = isLightTheme,
                    onCheckedChange = onToggleLightTheme,
                    label = language.tr("গ্রিন ও হোয়াইট লাইট থিম", "Green & White Light Theme"),
                    subtitle = language.tr(
                        "হালকা সবুজ ও সাদা মেসেজিং থিম অথবা ডার্ক থিম",
                        "Switch between Clean Green/White Light mode and Emerald Dark mode"
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppLanguage.entries.forEach { langOption ->
                        val isSelected = language == langOption
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) ElectricBlue else Color(0xFF12241C))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) GlowingCyan else Color.White.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onSelectLanguage(langOption) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = langOption.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // 3. My QR Code & Short ID Card
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .frostedGlassCard(cornerRadius = 24.dp)
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NeoTactileQrCodeView(
                    codeData = "$shortCode|$currentUserId",
                    size = 96.dp
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = language.tr("আমার কন্টাক্ট QR ও শর্ট আইডি", "MY CONTACT QR & SHORT ID"),
                        style = MaterialTheme.typography.labelMedium,
                        color = GlowingCyan
                    )
                    Text(
                        text = shortCode,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = language.tr(
                            "ইউজারনেম (@$username) বা এই শর্ট আইডি দিয়ে যে কেউ আপনাকে অ্যাড করতে পারবে।",
                            "Others can add you by @$username or this Short ID."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // 4. Account, Username, Phone & Profile Details Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .frostedGlassCard(cornerRadius = 24.dp)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = language.tr("অ্যাকাউন্ট, ইউজারনেম ও প্রোফাইল", "ACCOUNT, USERNAME & PROFILE"),
                    style = MaterialTheme.typography.labelLarge,
                    color = GlowingCyan
                )

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(language.shortTr("আপনার নাম (Display Name)", "Display Name")) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlowingCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_display_name_input")
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(language.shortTr("ইউজারনেম (@username)", "Username (@handle)")) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlowingCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_username_input")
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text(language.shortTr("ফোন নম্বর (Phone Number)", "Registered Phone Number")) },
                    placeholder = { Text("+8801...", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlowingCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text(language.shortTr("আপনার বায়ো (About / Bio)", "Profile Description / Bio")) },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlowingCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_bio_input")
                )

                NeoTactileToggleSwitch(
                    checked = isPrivate,
                    onCheckedChange = { isPrivate = it },
                    label = language.tr("প্রাইভেট প্রোফাইল", "Private Profile"),
                    subtitle = language.tr(
                        "চালু থাকলে শুধুমাত্র যুক্ত থাকা বন্ধুরা প্রোফাইল দেখতে পাবে",
                        "Only connected contacts can view your full profile"
                    ),
                    testTag = "profile_private_toggle"
                )

                NeoTactilePillButton(
                    text = language.tr("প্রোফাইল ও ইউজারনেম সেভ করুন", "Save Profile & Username"),
                    icon = Icons.Default.Save,
                    onClick = {
                        onUpdateProfile(
                            displayName,
                            bio,
                            isPrivate,
                            "preset://emerald_cover",
                            username,
                            phoneNumber
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "save_profile_button"
                )
            }
        }

        // 5. Complete Settings Organization (Chats, Wallpaper, Notifications, Storage, Privacy, Linked Devices)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .frostedGlassCard(cornerRadius = 24.dp)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = language.tr("সম্পূর্ণ অ্যাপ সেটিংস (ALL SETTINGS)", "COMPLETE SETTINGS"),
                    style = MaterialTheme.typography.labelLarge,
                    color = GlowingCyan
                )

                NeoTactileToggleSwitch(
                    checked = readReceiptsEnabled,
                    onCheckedChange = { readReceiptsEnabled = it },
                    label = language.tr("রিড রিসিপ্টস (Read Receipts)", "Read & Delivery Receipts"),
                    subtitle = language.tr("মেসেজ দেখা হলে ব্লু/গ্রিন টিক দেখাবে", "Show double-check read indicators")
                )

                NeoTactileToggleSwitch(
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it },
                    label = language.tr("নোটিফিকেশন ও কল অ্যালার্ট", "Push Notifications & Call Alerts"),
                    subtitle = language.tr("নতুন মেসেজ ও ইনকামিং WebRTC কলের নোটিফিকেশন", "FCM & real-time incoming call alerts")
                )

                NeoTactileToggleSwitch(
                    checked = twoStepEnabled,
                    onCheckedChange = { twoStepEnabled = it },
                    label = language.tr("টু-স্টেপ ভেরিফিকেশন (Two-Step PIN)", "Two-Step Verification"),
                    subtitle = language.tr("অ্যাকাউন্ট রিকভারি ও সিকিউরিটি পিন সুরক্ষা", "Extra PIN layer for account registration")
                )

                SettingsSummaryRow(
                    icon = Icons.Default.Wallpaper,
                    title = language.tr("চ্যাট ওয়ালপেপার (Chat Wallpaper)", "Chat Wallpaper"),
                    value = selectedWallpaper,
                    onClick = {
                        selectedWallpaper = if (selectedWallpaper == "Emerald Matrix") "Clean White Mint" else "Emerald Matrix"
                    }
                )

                SettingsSummaryRow(
                    icon = Icons.Default.Block,
                    title = language.tr("ব্লক করা কন্টাক্ট (Blocked Contacts)", "Blocked Contacts"),
                    value = "$blockedCount Users"
                )

                SettingsSummaryRow(
                    icon = Icons.Default.DataUsage,
                    title = language.tr("স্টোরেজ ও নেটওয়ার্ক ডাটা (Storage & Data)", "Storage, Cache & Network Usage"),
                    value = "Media Auto-Compress ON"
                )

                SettingsSummaryRow(
                    icon = Icons.Default.Devices,
                    title = language.tr("লিঙ্কড ডিভাইস (Linked Devices)", "Linked Devices"),
                    value = "$deviceModel ($hardwareId)"
                )
            }
        }

        // 6. Biometric / PIN Security Gate & Sign Out / Account Deletion Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .frostedGlassCard(cornerRadius = 24.dp, accentGlow = GlowingCyan)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = TactileEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = language.tr("সিকিউরিটি, বায়োমেট্রিক লক ও লগআউট", "SECURITY, APP LOCK & ACCOUNT"),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                }

                NeoTactileToggleSwitch(
                    checked = biometricGateEnabled,
                    onCheckedChange = onToggleBiometricGate,
                    label = language.tr("বায়োমেট্রিক / পিন লক", "Biometric / PIN App Lock"),
                    subtitle = language.tr(
                        "ব্যাকগ্রাউন্ডে ৬০ সেকেন্ড থাকলে পিন (2026) বা ফিঙ্গারপ্রিন্ট চাইবে",
                        "Require fingerprint or PIN (2026) after 60s in background"
                    ),
                    testTag = "biometric_gate_toggle"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NeoTactilePillButton(
                        text = language.shortTr("অ্যাপ লক করুন", "Lock App"),
                        icon = Icons.Default.Fingerprint,
                        onClick = onLockNow,
                        modifier = Modifier.weight(1f),
                        testTag = "test_app_lock_button"
                    )
                    NeoTactilePillButton(
                        text = language.shortTr("সাইন আউট", "Sign Out"),
                        icon = Icons.AutoMirrored.Filled.Logout,
                        onClick = onSignOut,
                        isActive = false,
                        accentColor = TactileCrimson,
                        modifier = Modifier.weight(1f),
                        testTag = "sign_out_button"
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSummaryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF102019))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = GlowingCyan,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = GlowingCyan,
            fontWeight = FontWeight.SemiBold
        )
    }
}
