package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import com.example.audio.DeviceStorageStats
import com.example.data.model.AppLanguage
import com.example.data.model.BroadcastListDocument
import com.example.data.model.DevicePhoneContact
import com.example.data.model.LinkedDeviceSession
import com.example.data.model.ResolvedContactProfile
import com.example.data.model.UserProfile
import com.example.data.model.generateShortCode
import com.example.ui.components.NeoTactilePillButton
import com.example.ui.components.NeoTactileQrCodeView
import com.example.ui.components.RealCameraXPreview
import com.example.ui.theme.LightBackgroundWhite
import com.example.ui.theme.LightDividerGray
import com.example.ui.theme.LightSurfaceSecondary
import com.example.ui.theme.LightTextMuted
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.theme.TactileCrimson
import com.example.ui.theme.WhatsAppAccentGreen
import com.example.ui.theme.WhatsAppActionGreen
import com.example.ui.theme.WhatsAppDarkGreen
import com.example.ui.theme.WhatsAppHeaderGreen
import java.io.File

enum class SettingsSubRoute {
    MAIN_SETTINGS,     // Screen 18
    PROFILE_ME,        // Screen 17
    ACCOUNT,           // Screen 19
    PRIVACY,           // Screen 20
    CHATS,             // Screen 21
    WALLPAPER,         // Screen 22
    NOTIFICATIONS,     // Screen 23
    STORAGE_AND_DATA,  // Screen 24
    MANAGE_STORAGE,    // Screen 25
    LINKED_DEVICES,    // Screen 26
    STARRED_MESSAGES,  // Screen 27
    BROADCAST_LISTS,   // Screen 28
    QR_CODE,           // Screen 29
    INVITE_FRIENDS     // Screen 30
}

@Composable
fun ProfileScreen(
    language: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    isLightTheme: Boolean = true,
    onToggleLightTheme: (Boolean) -> Unit = {},
    blockedCount: Int = 0,
    deviceStorageStats: DeviceStorageStats? = null,
    allPermissionsGranted: Boolean = true,
    onRequestPermissions: () -> Unit = {},
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
    onSignOut: () -> Unit,
    linkedDevices: List<LinkedDeviceSession> = emptyList(),
    broadcastLists: List<BroadcastListDocument> = emptyList(),
    deviceContacts: List<DevicePhoneContact> = emptyList(),
    savedMediaFiles: List<File> = emptyList(),
    contacts: List<ResolvedContactProfile> = emptyList(),
    onRemoveLinkedDevice: (String) -> Unit = {},
    onCreateBroadcastList: (String, List<String>) -> Unit = { _, _ -> },
    onSendBroadcastMessage: (BroadcastListDocument, String) -> Unit = { _, _ -> },
    onRefreshContacts: () -> Unit = {},
    onDeleteMediaFile: (File) -> Unit = {},
    onAddContactFromQr: (String) -> Unit = {},
    initialSubRoute: SettingsSubRoute = SettingsSubRoute.MAIN_SETTINGS
) {
    val context = LocalContext.current
    var currentSubRoute by remember(initialSubRoute) { mutableStateOf(initialSubRoute) }

    var displayName by remember(userProfile) {
        mutableStateOf(userProfile?.displayName?.ifBlank { fallbackName } ?: fallbackName)
    }
    var username by remember(userProfile) {
        mutableStateOf(userProfile?.username ?: fallbackName.lowercase().replace(Regex("[^a-z0-9]"), ""))
    }
    var phoneNumber by remember(userProfile) {
        mutableStateOf(userProfile?.phoneNumber?.ifBlank { "+880 1712 345678" } ?: "+880 1712 345678")
    }
    var bio by remember(userProfile) {
        mutableStateOf(userProfile?.cleanBio?.ifBlank { "Hey there! I am using WhatsApp." } ?: "Hey there! I am using WhatsApp.")
    }
    var isPrivate by remember(userProfile) {
        mutableStateOf(userProfile?.isPrivate ?: false)
    }

    var readReceiptsEnabled by remember { mutableStateOf(true) }
    var enterIsSend by remember { mutableStateOf(false) }
    var mediaVisibility by remember { mutableStateOf(true) }
    var conversationTones by remember { mutableStateOf(true) }
    var highPriorityNotifications by remember { mutableStateOf(true) }
    var lessDataForCalls by remember { mutableStateOf(false) }
    var selectedWallpaperName by remember { mutableStateOf("Default Beige Doodle") }

    val shortCode = remember(currentUserId) { generateShortCode(currentUserId) }

    if (currentSubRoute != SettingsSubRoute.MAIN_SETTINGS) {
        BackHandler {
            currentSubRoute = when (currentSubRoute) {
                SettingsSubRoute.WALLPAPER -> SettingsSubRoute.CHATS
                SettingsSubRoute.MANAGE_STORAGE -> SettingsSubRoute.STORAGE_AND_DATA
                else -> SettingsSubRoute.MAIN_SETTINGS
            }
        }
    }

    when (currentSubRoute) {
        SettingsSubRoute.MAIN_SETTINGS -> {
            // SCREEN 18: SETTINGS (MAIN) — Matches Ref_1 #18 & Ref_2 Settings
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightBackgroundWhite)
                    .testTag("profile_screen_list"),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                // Top User Profile Row + Green QR Code Button
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { currentSubRoute = SettingsSubRoute.PROFILE_ME }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WhatsAppAvatarCircle(name = displayName, size = 60.dp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayName.uppercase(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = LightTextPrimary
                            )
                            Text(
                                text = bio.ifBlank { "Available" },
                                fontSize = 14.sp,
                                color = LightTextSecondary
                            )
                            Text(
                                text = phoneNumber,
                                fontSize = 12.sp,
                                color = LightTextMuted
                            )
                        }
                        IconButton(
                            onClick = { currentSubRoute = SettingsSubRoute.QR_CODE },
                            modifier = Modifier.testTag("settings_qr_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = "QR Code",
                                tint = WhatsAppActionGreen,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = LightDividerGray, thickness = 0.8.dp)
                }

                // Settings Menu List (Matches Reference 1 #18 & Reference 2 Settings)
                item {
                    WhatsAppSettingsRow(
                        icon = Icons.Default.Key,
                        title = "Account",
                        subtitle = "Security notifications, change number",
                        onClick = { currentSubRoute = SettingsSubRoute.ACCOUNT }
                    )
                    WhatsAppSettingsRow(
                        icon = Icons.Default.Lock,
                        title = "Privacy",
                        subtitle = "Block contacts, disappearing messages",
                        onClick = { currentSubRoute = SettingsSubRoute.PRIVACY }
                    )
                    WhatsAppSettingsRow(
                        icon = Icons.Default.AccountCircle,
                        title = "Avatar & Profile",
                        subtitle = "Create, edit, profile photo",
                        onClick = { currentSubRoute = SettingsSubRoute.PROFILE_ME }
                    )
                    WhatsAppSettingsRow(
                        icon = Icons.AutoMirrored.Filled.Chat,
                        title = "Chats",
                        subtitle = "Theme, wallpapers, chat history",
                        onClick = { currentSubRoute = SettingsSubRoute.CHATS }
                    )
                    WhatsAppSettingsRow(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        subtitle = "Message, group & call tones",
                        onClick = { currentSubRoute = SettingsSubRoute.NOTIFICATIONS }
                    )
                    WhatsAppSettingsRow(
                        icon = Icons.Default.DataUsage,
                        title = "Storage and data",
                        subtitle = "Network usage, auto-download, mobile storage",
                        onClick = { currentSubRoute = SettingsSubRoute.STORAGE_AND_DATA }
                    )
                    WhatsAppSettingsRow(
                        icon = Icons.Default.Devices,
                        title = "Linked devices",
                        subtitle = "$deviceModel ($hardwareId)",
                        onClick = { currentSubRoute = SettingsSubRoute.LINKED_DEVICES }
                    )
                    WhatsAppSettingsRow(
                        icon = Icons.Default.Star,
                        title = "Starred messages",
                        subtitle = "View bookmarked messages",
                        onClick = { currentSubRoute = SettingsSubRoute.STARRED_MESSAGES }
                    )
                    WhatsAppSettingsRow(
                        icon = Icons.Default.Campaign,
                        title = "Broadcast lists",
                        subtitle = "Send messages to multiple contacts",
                        onClick = { currentSubRoute = SettingsSubRoute.BROADCAST_LISTS }
                    )
                    WhatsAppSettingsRow(
                        icon = Icons.Default.Language,
                        title = "App language",
                        subtitle = "${language.label} (Tap to switch)",
                        onClick = {
                            val next = when (language) {
                                AppLanguage.BILINGUAL -> AppLanguage.ENGLISH
                                AppLanguage.ENGLISH -> AppLanguage.BENGALI
                                AppLanguage.BENGALI -> AppLanguage.BILINGUAL
                            }
                            onSelectLanguage(next)
                        }
                    )
                    WhatsAppSettingsRow(
                        icon = Icons.Default.PersonAdd,
                        title = "Invite a friend",
                        subtitle = "Share link or invite contacts",
                        onClick = { currentSubRoute = SettingsSubRoute.INVITE_FRIENDS }
                    )
                }

                // Quick Permissions & Mobile Storage Status Footer
                item {
                    HorizontalDivider(color = LightDividerGray, thickness = 0.8.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!allPermissionsGranted) onRequestPermissions()
                                else currentSubRoute = SettingsSubRoute.MANAGE_STORAGE
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = WhatsAppActionGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (allPermissionsGranted) "All Permissions Granted ✅" else "Tap to Allow Permissions",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LightTextPrimary
                            )
                            if (deviceStorageStats != null) {
                                Text(
                                    text = "Mobile Storage: ${deviceStorageStats.freeDeviceGb} GB Free / ${deviceStorageStats.totalDeviceGb} GB • ${deviceStorageStats.savedFilesCount} media files",
                                    fontSize = 12.sp,
                                    color = LightTextSecondary
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NeoTactilePillButton(
                            text = "Lock App",
                            icon = Icons.Default.Fingerprint,
                            onClick = onLockNow,
                            modifier = Modifier.weight(1f),
                            testTag = "test_app_lock_button"
                        )
                        NeoTactilePillButton(
                            text = "Sign Out",
                            icon = Icons.AutoMirrored.Filled.Logout,
                            accentColor = TactileCrimson,
                            onClick = onSignOut,
                            modifier = Modifier.weight(1f),
                            testTag = "sign_out_button"
                        )
                    }
                }
            }
        }

        SettingsSubRoute.PROFILE_ME -> {
            // SCREEN 17: PROFILE ("Me") — Matches Ref_1 #17 & Ref_2 Profile
            WhatsAppSubScreenScaffold(
                title = "Profile",
                onBack = { currentSubRoute = SettingsSubRoute.MAIN_SETTINGS }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(132.dp)) {
                        WhatsAppAvatarCircle(name = displayName, size = 128.dp)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(WhatsAppActionGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Change Profile Photo",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = WhatsAppActionGreen) },
                        trailingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = WhatsAppActionGreen) },
                        supportingText = {
                            Text("This is not your username or pin. This name will be visible to your contacts.")
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WhatsAppActionGreen,
                            unfocusedBorderColor = LightDividerGray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_display_name_input")
                    )

                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("About") },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = WhatsAppActionGreen) },
                        trailingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = WhatsAppActionGreen) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WhatsAppActionGreen,
                            unfocusedBorderColor = LightDividerGray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_bio_input")
                    )

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = WhatsAppActionGreen) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WhatsAppActionGreen,
                            unfocusedBorderColor = LightDividerGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username / Links") },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = WhatsAppActionGreen) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WhatsAppActionGreen,
                            unfocusedBorderColor = LightDividerGray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_username_input")
                    )

                    NeoTactilePillButton(
                        text = "Save Profile",
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
                            currentSubRoute = SettingsSubRoute.MAIN_SETTINGS
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "save_profile_button"
                    )
                }
            }
        }

        SettingsSubRoute.ACCOUNT -> {
            // SCREEN 19: ACCOUNT SETTINGS — Matches Ref_1 #19 & Ref_2 Account
            WhatsAppSubScreenScaffold(
                title = "Account",
                onBack = { currentSubRoute = SettingsSubRoute.MAIN_SETTINGS }
            ) {
                WhatsAppSettingsRow(
                    icon = Icons.Default.Security,
                    title = "Security notifications",
                    subtitle = "Show security notifications on this device",
                    onClick = {}
                )
                WhatsAppSettingsRow(
                    icon = Icons.Default.Key,
                    title = "Passkeys",
                    subtitle = "Use passkeys to log in more securely",
                    onClick = {}
                )
                WhatsAppSettingsRow(
                    icon = Icons.Default.Email,
                    title = "Email address",
                    subtitle = userProfile?.email?.ifBlank { fallbackEmail } ?: fallbackEmail,
                    onClick = {}
                )
                WhatsAppSettingsRow(
                    icon = Icons.Default.Pin,
                    title = "Two-step verification",
                    subtitle = "Enabled (Captcha + OTP Anti-Fake Verification)",
                    onClick = {}
                )
                WhatsAppSettingsRow(
                    icon = Icons.Default.PhoneAndroid,
                    title = "Change number",
                    subtitle = phoneNumber,
                    onClick = {}
                )
                WhatsAppSettingsRow(
                    icon = Icons.Default.Description,
                    title = "Request account info",
                    subtitle = "Get a report of your account information",
                    onClick = {}
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSignOut() }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete account",
                        tint = TactileCrimson
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Delete account / Sign out",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TactileCrimson
                    )
                }
            }
        }

        SettingsSubRoute.PRIVACY -> {
            // SCREEN 20: PRIVACY SETTINGS — Matches Ref_1 #20 & Ref_2 Privacy
            WhatsAppSubScreenScaffold(
                title = "Privacy",
                onBack = { currentSubRoute = SettingsSubRoute.MAIN_SETTINGS }
            ) {
                Text(
                    text = "Who can see my personal info",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LightTextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                WhatsAppSimpleValueRow("Last seen and online", if (isPrivate) "My contacts" else "Everyone") {
                    isPrivate = !isPrivate
                }
                WhatsAppSimpleValueRow("Profile photo", if (isPrivate) "My contacts" else "Everyone") {
                    isPrivate = !isPrivate
                }
                WhatsAppSimpleValueRow("About", "Everyone") {}
                WhatsAppSimpleValueRow("Status", "My contacts") {}

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Read receipts", fontSize = 16.sp, color = LightTextPrimary)
                        Text(
                            "If turned off, you won't send or receive Read receipts. Read receipts are always sent for group chats.",
                            fontSize = 13.sp,
                            color = LightTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = readReceiptsEnabled,
                        onCheckedChange = { readReceiptsEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = WhatsAppActionGreen
                        )
                    )
                }

                HorizontalDivider(color = LightDividerGray, thickness = 0.6.dp)
                WhatsAppSimpleValueRow("Disappearing messages", "Off") {}
                WhatsAppSimpleValueRow("Groups", "Everyone") {}
                WhatsAppSimpleValueRow("Blocked contacts", "$blockedCount contacts") {}

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("App lock (Biometric / PIN)", fontSize = 16.sp, color = LightTextPrimary)
                        Text("Require fingerprint or PIN (2026) to unlock", fontSize = 13.sp, color = LightTextSecondary)
                    }
                    Switch(
                        checked = biometricGateEnabled,
                        onCheckedChange = onToggleBiometricGate,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = WhatsAppActionGreen
                        ),
                        modifier = Modifier.testTag("biometric_gate_toggle")
                    )
                }
            }
        }

        SettingsSubRoute.CHATS -> {
            // SCREEN 21: CHAT SETTINGS — Matches Ref_1 #21
            WhatsAppSubScreenScaffold(
                title = "Chats",
                onBack = { currentSubRoute = SettingsSubRoute.MAIN_SETTINGS }
            ) {
                Text(
                    text = "Display",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LightTextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                WhatsAppSettingsRow(
                    icon = Icons.Default.Contrast,
                    title = "Theme",
                    subtitle = if (isLightTheme) "Light (Green & White)" else "Dark",
                    onClick = { onToggleLightTheme(!isLightTheme) }
                )
                WhatsAppSettingsRow(
                    icon = Icons.Default.Wallpaper,
                    title = "Wallpaper",
                    subtitle = selectedWallpaperName,
                    onClick = { currentSubRoute = SettingsSubRoute.WALLPAPER }
                )

                HorizontalDivider(color = LightDividerGray, thickness = 0.6.dp)

                Text(
                    text = "Chat settings",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LightTextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )

                WhatsAppSwitchRow(
                    title = "Enter is send",
                    subtitle = "Enter key will send your message",
                    checked = enterIsSend,
                    onCheckedChange = { enterIsSend = it }
                )
                WhatsAppSwitchRow(
                    title = "Media visibility",
                    subtitle = "Show newly downloaded media in your device's gallery",
                    checked = mediaVisibility,
                    onCheckedChange = { mediaVisibility = it }
                )
                WhatsAppSimpleValueRow("Font size", "Medium") {}
                HorizontalDivider(color = LightDividerGray, thickness = 0.6.dp)
                WhatsAppSettingsRow(
                    icon = Icons.Default.Backup,
                    title = "Chat backup",
                    subtitle = "Back up messages to cloud and device storage",
                    onClick = {}
                )
            }
        }

        SettingsSubRoute.WALLPAPER -> {
            // SCREEN 22: WALLPAPER SETTINGS — Matches Ref_1 #22
            WhatsAppSubScreenScaffold(
                title = "Wallpaper",
                onBack = { currentSubRoute = SettingsSubRoute.CHATS }
            ) {
                WhatsAppSettingsRow(
                    icon = Icons.Default.Image,
                    title = "Choose from gallery",
                    subtitle = "Pick a custom photo from mobile storage",
                    onClick = { selectedWallpaperName = "Custom Gallery Wallpaper" }
                )
                WhatsAppSettingsRow(
                    icon = Icons.Default.Palette,
                    title = "Solid colors",
                    subtitle = "Clean WhatsApp solid background colors",
                    onClick = { selectedWallpaperName = "Solid Mint Green" }
                )
                WhatsAppSettingsRow(
                    icon = Icons.Default.Wallpaper,
                    title = "Default wallpaper",
                    subtitle = "Classic WhatsApp beige doodle wallpaper",
                    onClick = { selectedWallpaperName = "Default Beige Doodle" }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val wallpapers = listOf(
                        "Beige Doodle" to listOf(Color(0xFFEFEAE2), Color(0xFFD9FDD3)),
                        "Ocean Blue" to listOf(Color(0xFF38BDF8), Color(0xFF1D4ED8)),
                        "Emerald" to listOf(Color(0xFF10B981), Color(0xFF047857)),
                        "Sunset" to listOf(Color(0xFFF472B6), Color(0xFFDB2777))
                    )
                    wallpapers.forEach { (name, colors) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(130.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.verticalGradient(colors))
                                .border(
                                    width = if (selectedWallpaperName.contains(name)) 2.5.dp else 1.dp,
                                    color = if (selectedWallpaperName.contains(name)) WhatsAppActionGreen else LightDividerGray,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedWallpaperName = name },
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Text(
                                text = name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LightTextPrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.85f))
                                    .padding(vertical = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        SettingsSubRoute.NOTIFICATIONS -> {
            // SCREEN 23: NOTIFICATIONS SETTINGS — Matches Ref_1 #23 & Ref_2 Notifications
            WhatsAppSubScreenScaffold(
                title = "Notifications",
                onBack = { currentSubRoute = SettingsSubRoute.MAIN_SETTINGS }
            ) {
                WhatsAppSwitchRow(
                    title = "Conversation tones",
                    subtitle = "Play sounds for incoming and outgoing messages",
                    checked = conversationTones,
                    onCheckedChange = { conversationTones = it }
                )
                HorizontalDivider(color = LightDividerGray, thickness = 0.6.dp)
                Text(
                    text = "Messages",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LightTextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
                WhatsAppSimpleValueRow("Notification tone", "Default (Pixie Dust)") {}
                WhatsAppSimpleValueRow("Vibrate", "Default") {}
                WhatsAppSimpleValueRow("Popup notification", "Not available") {}
                WhatsAppSimpleValueRow("Light", "White") {}
                WhatsAppSwitchRow(
                    title = "Use high priority notifications",
                    subtitle = "Show previews of notifications at the top of the screen",
                    checked = highPriorityNotifications,
                    onCheckedChange = { highPriorityNotifications = it }
                )
                HorizontalDivider(color = LightDividerGray, thickness = 0.6.dp)
                Text(
                    text = "Groups",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LightTextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
                WhatsAppSimpleValueRow("Notification tone", "Default (Pixie Dust)") {}
            }
        }

        SettingsSubRoute.STORAGE_AND_DATA -> {
            // SCREEN 24: STORAGE AND DATA — Matches Ref_1 #24 & Ref_2 Storage & Data
            WhatsAppSubScreenScaffold(
                title = "Storage and data",
                onBack = { currentSubRoute = SettingsSubRoute.MAIN_SETTINGS }
            ) {
                WhatsAppSettingsRow(
                    icon = Icons.Default.Folder,
                    title = "Manage storage",
                    subtitle = if (deviceStorageStats != null) {
                        "${deviceStorageStats.appMediaUsedKb} KB used • ${deviceStorageStats.freeDeviceGb} GB free"
                    } else "1.2 GB",
                    onClick = { currentSubRoute = SettingsSubRoute.MANAGE_STORAGE }
                )
                HorizontalDivider(color = LightDividerGray, thickness = 0.6.dp)
                WhatsAppSettingsRow(
                    icon = Icons.Default.DataUsage,
                    title = "Network usage",
                    subtitle = "652 MB sent • 1.2 GB received",
                    onClick = {}
                )
                WhatsAppSwitchRow(
                    title = "Use less data for calls",
                    subtitle = "Reduce data used during a WebRTC call",
                    checked = lessDataForCalls,
                    onCheckedChange = { lessDataForCalls = it }
                )
                HorizontalDivider(color = LightDividerGray, thickness = 0.6.dp)
                Text(
                    text = "Media auto-download",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LightTextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
                WhatsAppSimpleValueRow("When using mobile data", "Photos") {}
                WhatsAppSimpleValueRow("When connected on Wi-Fi", "All media") {}
                WhatsAppSimpleValueRow("When roaming", "No media") {}
            }
        }

        SettingsSubRoute.MANAGE_STORAGE -> {
            // SCREEN 25: MANAGE STORAGE — Real local files in NeoTactileMedia
            WhatsAppSubScreenScaffold(
                title = "Manage storage",
                onBack = { currentSubRoute = SettingsSubRoute.STORAGE_AND_DATA }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Used", fontSize = 12.sp, color = LightTextSecondary)
                            Text(
                                text = if (deviceStorageStats != null) "${deviceStorageStats.appMediaUsedKb} KB" else "0 KB",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhatsAppActionGreen
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Free on Mobile", fontSize = 12.sp, color = LightTextSecondary)
                            Text(
                                text = "${deviceStorageStats?.freeDeviceGb ?: "0"} GB free",
                                fontSize = 14.sp,
                                color = LightTextSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = {
                            val usedKb = (deviceStorageStats?.appMediaUsedKb ?: 0L).toFloat()
                            (usedKb / 50000f).coerceIn(0.02f, 1f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = WhatsAppAccentGreen,
                        trackColor = LightDividerGray
                    )
                }
                HorizontalDivider(color = LightDividerGray, thickness = 0.6.dp)
                WhatsAppSettingsRow(
                    icon = Icons.Default.Image,
                    title = "Photos",
                    subtitle = "Saved in NeoTactileMedia/images",
                    onClick = {}
                )
                WhatsAppSettingsRow(
                    icon = Icons.Default.Mic,
                    title = "Voice messages",
                    subtitle = "Saved in NeoTactileMedia/voice_notes",
                    onClick = {}
                )
                WhatsAppSettingsRow(
                    icon = Icons.Default.Description,
                    title = "Documents & Downloaded files",
                    subtitle = "${deviceStorageStats?.savedFilesCount ?: savedMediaFiles.size} files in NeoTactileMedia",
                    onClick = {}
                )
                if (savedMediaFiles.isNotEmpty()) {
                    HorizontalDivider(color = LightDividerGray, thickness = 0.6.dp)
                    Text(
                        text = "Saved Media Files (Tap to delete)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LightTextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                    savedMediaFiles.take(15).forEach { file ->
                        WhatsAppSettingsRow(
                            icon = Icons.Default.DeleteOutline,
                            title = file.name,
                            subtitle = "${(file.length() / 1024L).coerceAtLeast(1L)} KB • Tap to remove from device",
                            onClick = { onDeleteMediaFile(file) }
                        )
                    }
                }
            }
        }

        SettingsSubRoute.LINKED_DEVICES -> {
            // SCREEN 26: LINKED DEVICES — Real Firestore /linked_devices sessions
            WhatsAppSubScreenScaffold(
                title = "Linked devices",
                onBack = { currentSubRoute = SettingsSubRoute.MAIN_SETTINGS }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFE7FCE8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = "Linked Devices",
                            tint = WhatsAppActionGreen,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Use WhatsApp on other devices",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightTextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(WhatsAppActionGreen)
                            .clickable { currentSubRoute = SettingsSubRoute.QR_CODE },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Link a device",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                HorizontalDivider(color = LightDividerGray, thickness = 0.8.dp)
                Text(
                    text = "Device Status (${linkedDevices.size.coerceAtLeast(1)} active)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LightTextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
                if (linkedDevices.isEmpty()) {
                    WhatsAppSettingsRow(
                        icon = Icons.Default.PhoneAndroid,
                        title = deviceModel,
                        subtitle = "Active now • Hardware ID: $hardwareId",
                        onClick = {}
                    )
                } else {
                    linkedDevices.forEach { session ->
                        WhatsAppSettingsRow(
                            icon = Icons.Default.PhoneAndroid,
                            title = "${session.deviceModel} (${session.platform})",
                            subtitle = "Hardware ID: ${session.hardwareId} • Tap to unlink",
                            onClick = { onRemoveLinkedDevice(session.id) }
                        )
                    }
                }
            }
        }

        SettingsSubRoute.STARRED_MESSAGES -> {
            // SCREEN 27: STARRED MESSAGES — Real Firestore starredMessageIds
            val starredIds = userProfile?.starredMessageIds.orEmpty()
            WhatsAppSubScreenScaffold(
                title = "Starred messages",
                onBack = { currentSubRoute = SettingsSubRoute.MAIN_SETTINGS }
            ) {
                if (starredIds.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = WhatsAppActionGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No starred messages yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LightTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap and hold any message in a chat to star it across all your devices.",
                            fontSize = 13.sp,
                            color = LightTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    starredIds.forEach { msgId ->
                        WhatsAppSettingsRow(
                            icon = Icons.Default.Star,
                            title = "Starred Message",
                            subtitle = "Cloud Message ID: $msgId",
                            onClick = {}
                        )
                    }
                }
            }
        }

        SettingsSubRoute.BROADCAST_LISTS -> {
            // SCREEN 28: BROADCAST LISTS — Real Firestore /broadcast_lists + multi-recipient delivery
            var showCreateBroadcastDialog by remember { mutableStateOf(false) }
            var selectedBroadcastForSend by remember { mutableStateOf<BroadcastListDocument?>(null) }
            var newBroadcastName by remember { mutableStateOf("") }
            var broadcastMessageText by remember { mutableStateOf("") }

            WhatsAppSubScreenScaffold(
                title = "Broadcast lists",
                onBack = { currentSubRoute = SettingsSubRoute.MAIN_SETTINGS }
            ) {
                WhatsAppSettingsRow(
                    icon = Icons.Default.Campaign,
                    title = "New broadcast",
                    subtitle = "Create a persistent broadcast list (${contacts.count { !it.isGroup }} contacts available)",
                    onClick = { showCreateBroadcastDialog = true }
                )
                HorizontalDivider(color = LightDividerGray, thickness = 0.6.dp)
                if (broadcastLists.isEmpty()) {
                    Text(
                        text = "No broadcast lists yet. Tap 'New broadcast' above to create one.",
                        fontSize = 13.sp,
                        color = LightTextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    broadcastLists.forEach { listDoc ->
                        WhatsAppSettingsRow(
                            icon = Icons.Default.Campaign,
                            title = listDoc.name,
                            subtitle = "${listDoc.recipientIds.size} recipients • Tap to send broadcast message",
                            onClick = { selectedBroadcastForSend = listDoc }
                        )
                    }
                }
            }

            if (showCreateBroadcastDialog) {
                AlertDialog(
                    onDismissRequest = { showCreateBroadcastDialog = false },
                    title = { Text("New Broadcast List") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = newBroadcastName,
                                onValueChange = { newBroadcastName = it },
                                label = { Text("Broadcast list name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Includes ${contacts.count { !it.isGroup }} direct contacts",
                                fontSize = 12.sp,
                                color = LightTextSecondary
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val recipientIds = contacts.filter { !it.isGroup }.map { it.userId }
                                onCreateBroadcastList(newBroadcastName.ifBlank { "VIP Broadcast" }, recipientIds)
                                newBroadcastName = ""
                                showCreateBroadcastDialog = false
                            }
                        ) {
                            Text("Create", color = WhatsAppActionGreen, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateBroadcastDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            val targetBroadcast = selectedBroadcastForSend
            if (targetBroadcast != null) {
                AlertDialog(
                    onDismissRequest = { selectedBroadcastForSend = null },
                    title = { Text("Broadcast to ${targetBroadcast.name}") },
                    text = {
                        OutlinedTextField(
                            value = broadcastMessageText,
                            onValueChange = { broadcastMessageText = it },
                            label = { Text("Message to all recipients") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (broadcastMessageText.isNotBlank()) {
                                    onSendBroadcastMessage(targetBroadcast, broadcastMessageText)
                                    broadcastMessageText = ""
                                    selectedBroadcastForSend = null
                                }
                            }
                        ) {
                            Text("Send Broadcast", color = WhatsAppActionGreen, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { selectedBroadcastForSend = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }

        SettingsSubRoute.QR_CODE -> {
            // SCREEN 29: QR CODE — My Code + Live Hardware CameraX Scan Code Tab
            var qrTabIndex by remember { mutableStateOf(0) } // 0 = My code, 1 = Scan code
            var scannedCodeText by remember { mutableStateOf("") }

            WhatsAppSubScreenScaffold(
                title = "QR code",
                onBack = { currentSubRoute = SettingsSubRoute.MAIN_SETTINGS }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LightSurfaceSecondary)
                ) {
                    listOf("My code", "Scan code").forEachIndexed { idx, tabTitle ->
                        val selected = qrTabIndex == idx
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { qrTabIndex = idx }
                                .padding(top = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = tabTitle,
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) WhatsAppActionGreen else LightTextSecondary,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .background(if (selected) WhatsAppActionGreen else Color.Transparent)
                            )
                        }
                    }
                }

                if (qrTabIndex == 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        WhatsAppAvatarCircle(name = displayName, size = 64.dp)
                        Text(
                            text = displayName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightTextPrimary
                        )
                        Text(
                            text = "WhatsApp contact • ID: $shortCode",
                            fontSize = 13.sp,
                            color = LightTextSecondary
                        )
                        NeoTactileQrCodeView(
                            codeData = "$shortCode|$currentUserId",
                            size = 210.dp
                        )
                        Text(
                            text = "Scan this code to start a WhatsApp chat with me.",
                            fontSize = 13.sp,
                            color = LightTextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .background(WhatsAppAccentGreen)
                                .clickable {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Add me on WhatsApp! My Short ID is $shortCode (UID: $currentUserId)"
                                        )
                                    }
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, "Share QR Contact ID").apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Share QR Contact ID",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .border(2.dp, WhatsAppAccentGreen, RoundedCornerShape(18.dp))
                        ) {
                            RealCameraXPreview(
                                modifier = Modifier.fillMaxSize(),
                                useFrontCamera = false,
                                enableQrAnalysis = true
                            )
                        }
                        Text(
                            text = "Point your camera at a contact's QR code or enter their Short ID below:",
                            fontSize = 13.sp,
                            color = LightTextSecondary,
                            textAlign = TextAlign.Center
                        )
                        OutlinedTextField(
                            value = scannedCodeText,
                            onValueChange = { scannedCodeText = it },
                            label = { Text("Contact Short ID, Email, or UID") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .background(WhatsAppActionGreen)
                                .clickable {
                                    if (scannedCodeText.isNotBlank()) {
                                        onAddContactFromQr(scannedCodeText.trim())
                                        scannedCodeText = ""
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Add Scanned Contact",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        SettingsSubRoute.INVITE_FRIENDS -> {
            // SCREEN 30: INVITE FRIENDS — Real Android Share Sheet + Real ContactsContract
            androidx.compose.runtime.LaunchedEffect(Unit) {
                onRefreshContacts()
            }
            val inviteMessage = "Hey! Chat with me on WhatsApp. Add my Short ID: $shortCode (or email: $fallbackEmail)"

            WhatsAppSubScreenScaffold(
                title = "Invite friends",
                onBack = { currentSubRoute = SettingsSubRoute.MAIN_SETTINGS }
            ) {
                Text(
                    text = "Share WhatsApp with your friends",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LightTextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                WhatsAppSettingsRow(
                    icon = Icons.Default.Share,
                    title = "Share link",
                    subtitle = "Short ID: $shortCode • Opens Android Share Sheet",
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, inviteMessage)
                        }
                        context.startActivity(
                            Intent.createChooser(sendIntent, "Invite a friend").apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    }
                )
                WhatsAppSettingsRow(
                    icon = Icons.Default.ContentCopy,
                    title = "Copy link & Short ID",
                    subtitle = "Copy Short ID ($shortCode) to clipboard",
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("WhatsApp Short ID", inviteMessage))
                    }
                )
                HorizontalDivider(color = LightDividerGray, thickness = 0.6.dp)
                Text(
                    text = "From Contacts (${deviceContacts.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LightTextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                if (deviceContacts.isEmpty()) {
                    WhatsAppSettingsRow(
                        icon = Icons.Default.PersonAdd,
                        title = "Refresh Phone Contacts",
                        subtitle = "Tap to load contacts from your phone address book",
                        onClick = onRefreshContacts
                    )
                } else {
                    deviceContacts.forEach { phoneContact ->
                        WhatsAppSettingsRow(
                            icon = Icons.Default.PersonAdd,
                            title = phoneContact.name,
                            subtitle = "${phoneContact.phoneNumber} • Tap to invite",
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, inviteMessage)
                                }
                                context.startActivity(
                                    Intent.createChooser(sendIntent, "Invite ${phoneContact.name}").apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WhatsAppSubScreenScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackgroundWhite)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(WhatsAppHeaderGreen)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = Color.White
                )
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            item {
                content()
            }
        }
    }
}

@Composable
private fun WhatsAppSettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = LightTextSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = LightTextPrimary
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = LightTextSecondary
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = LightTextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun WhatsAppSimpleValueRow(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            color = LightTextPrimary
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = LightTextSecondary
        )
    }
}

@Composable
private fun WhatsAppSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, color = LightTextPrimary)
            Text(text = subtitle, fontSize = 13.sp, color = LightTextSecondary)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = WhatsAppActionGreen
            )
        )
    }
}
