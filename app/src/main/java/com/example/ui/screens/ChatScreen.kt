package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.AppLanguage
import com.example.data.model.CallLogEntry
import com.example.data.model.ChatMessage
import com.example.data.model.ResolvedContactProfile
import com.example.ui.components.NeoTactilePillButton
import com.example.ui.components.NeoTactileScrubSlider
import com.example.ui.components.RealCameraXPreview
import com.example.ui.components.WebRtcHardwareController
import com.example.ui.components.WebRtcVideoTrackRenderer
import com.example.ui.theme.LightBackgroundWhite
import com.example.ui.theme.LightDividerGray
import com.example.ui.theme.LightSurfaceSecondary
import com.example.ui.theme.LightTextMuted
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.theme.TactileCrimson
import com.example.ui.theme.WhatsAppAccentGreen
import com.example.ui.theme.WhatsAppActionGreen
import com.example.ui.theme.WhatsAppCallScreenBg
import com.example.ui.theme.WhatsAppChatWallpaper
import com.example.ui.theme.WhatsAppDarkGreen
import com.example.ui.theme.WhatsAppHeaderGreen
import com.example.ui.theme.WhatsAppIncomingBubble
import com.example.ui.theme.WhatsAppOutgoingBubble
import com.example.ui.theme.WhatsAppPdfRed
import com.example.ui.theme.WhatsAppReadBlue
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale

private val QUICK_REACTIONS = listOf("❤️", "👍", "🔥", "😂", "😮", "🙏")

private val GROUP_MEMBER_COLORS = listOf(
    Color(0xFF0284C7),
    Color(0xFF7C3AED),
    Color(0xFFD97706),
    Color(0xFF059669),
    Color(0xFFDB2777)
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    language: AppLanguage,
    currentUserId: String,
    contacts: List<ResolvedContactProfile>,
    selectedPeerId: String,
    messages: List<ChatMessage>,
    replyingToMessage: ChatMessage?,
    editingMessage: ChatMessage?,
    fullscreenMediaMessage: ChatMessage?,
    isRecordingVoice: Boolean,
    liveVoiceAmplitude: Float,
    recordingElapsedMs: Long,
    activePlayingUrl: String?,
    isPlayingVoice: Boolean,
    playbackProgress: Float,
    playbackAmplitude: Float,
    onSelectPeer: (String) -> Unit,
    onSendText: (String) -> Unit,
    onStartReply: (ChatMessage) -> Unit,
    onStartEdit: (ChatMessage) -> Unit,
    onCancelReplyOrEdit: () -> Unit,
    onDeleteMessage: (ChatMessage) -> Unit,
    onReactToMessage: (ChatMessage, String) -> Unit,
    onSendFileAttachment: (String, String) -> Unit,
    onOpenFullscreenMedia: (ChatMessage?) -> Unit,
    onStartVoiceRecording: () -> Unit,
    onStopAndSendVoice: () -> Unit,
    onCancelVoiceRecording: () -> Unit,
    onToggleVoicePlayback: (String, Long) -> Unit,
    onSeekVoicePlayback: (Float) -> Unit,
    onPickAndCompressPhoto: (android.net.Uri) -> Unit,
    onSaveLocalAlias: (String, String?, String?) -> Unit,
    onClearLocalAlias: (String) -> Unit,
    onNavigateToDirectory: () -> Unit = {},
    onStartWebRtcCall: (peerId: String, peerName: String, callType: String) -> Unit = { _, _, _ -> },
    onEndWebRtcCall: (durationSec: Int) -> Unit = {},
    onTypingChanged: (String) -> Unit = {},
    activeCallSession: CallLogEntry? = null,
    onBackToChatsList: () -> Unit = {}
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var showContactOrGroupInfo by remember { mutableStateOf(false) }
    var activeCallMode by remember { mutableStateOf<String?>(null) } // "AUDIO" or "VIDEO"

    LaunchedEffect(activeCallSession?.id, activeCallSession?.status) {
        if (activeCallSession != null && (activeCallSession.status == "RINGING" || activeCallSession.status == "CONNECTED")) {
            activeCallMode = activeCallSession.callType
        } else if (activeCallSession == null && activeCallMode != null) {
            activeCallMode = null
        }
    }
    var showAliasModal by remember { mutableStateOf(false) }
    var showFileAttachModal by remember { mutableStateOf(false) }
    var showTopMoreMenu by remember { mutableStateOf(false) }
    var selectedActionMessage by remember { mutableStateOf<ChatMessage?>(null) }

    LaunchedEffect(editingMessage) {
        if (editingMessage != null) {
            inputText = editingMessage.content
        }
    }

    val activeContact = remember(contacts, selectedPeerId) {
        contacts.find { it.userId == selectedPeerId } ?: contacts.firstOrNull()
    }

    if (activeContact == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LightBackgroundWhite)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = WhatsAppHeaderGreen,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = language.tr("কোনো চ্যাট নির্বাচন করা হয়নি", "No Active Conversation"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightTextPrimary
                )
                Text(
                    text = language.tr(
                        "মেসেজিং বা অডিও/ভিডিও কল শুরু করতে একজন আসল ইউজার অ্যাড করুন।",
                        "Add a real registered user to start messaging and calling."
                    ),
                    fontSize = 14.sp,
                    color = LightTextSecondary
                )
                NeoTactilePillButton(
                    text = language.shortTr("+ ইউজার অ্যাড করুন", "+ Add Contact"),
                    icon = Icons.Default.PersonAdd,
                    onClick = onNavigateToDirectory,
                    testTag = "empty_chat_add_user_button"
                )
            }
        }
        return
    }

    // Screen 14 & Screen 15: Full-screen Voice Call or Video Call Screen
    if (activeCallMode != null) {
        WhatsAppActiveCallScreen(
            contact = activeContact,
            callMode = activeCallMode ?: "AUDIO",
            activeCallSession = activeCallSession,
            onSwitchToVideo = { activeCallMode = "VIDEO" },
            onEndCall = { elapsedSec ->
                activeCallMode = null
                onEndWebRtcCall(elapsedSec)
            }
        )
        return
    }

    // Screen 4 (Chat Info / Contact Info) & Screen 5 (Group Info)
    if (showContactOrGroupInfo) {
        BackHandler { showContactOrGroupInfo = false }
        WhatsAppChatOrGroupInfoScreen(
            contact = activeContact,
            messages = messages,
            onBack = { showContactOrGroupInfo = false },
            onStartAudioCall = {
                showContactOrGroupInfo = false
                activeCallMode = "AUDIO"
                onStartWebRtcCall(activeContact.userId, activeContact.effectiveDisplayName, "AUDIO")
            },
            onStartVideoCall = {
                showContactOrGroupInfo = false
                activeCallMode = "VIDEO"
                onStartWebRtcCall(activeContact.userId, activeContact.effectiveDisplayName, "VIDEO")
            },
            onOpenAliasEditor = { showAliasModal = true },
            onAddParticipants = onNavigateToDirectory
        )
        return
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickAndCompressPhoto(uri)
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        onStartVoiceRecording()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhatsAppChatWallpaper)
            .testTag("open_chat_screen")
    ) {
        // 1. GREEN CONVERSATION HEADER BAR (Matches Screen 2 & Screen 3 of Reference 1 & Reference 2)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(WhatsAppHeaderGreen)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackToChatsList) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showContactOrGroupInfo = true }
                    .padding(vertical = 2.dp)
                    .testTag("chat_header_info_trigger"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WhatsAppAvatarCircle(
                    name = activeContact.effectiveDisplayName,
                    isGroup = activeContact.isGroup,
                    size = 40.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeContact.effectiveDisplayName,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            activeContact.isTyping -> "typing..."
                            activeContact.isGroup -> "${activeContact.memberIds.size.coerceAtLeast(3)} members"
                            activeContact.isOnline -> "online"
                            else -> "last seen today"
                        },
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = {
                    activeCallMode = "VIDEO"
                    onStartWebRtcCall(activeContact.userId, activeContact.effectiveDisplayName, "VIDEO")
                },
                modifier = Modifier.testTag("start_video_call_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Video Call",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = {
                    activeCallMode = "AUDIO"
                    onStartWebRtcCall(activeContact.userId, activeContact.effectiveDisplayName, "AUDIO")
                },
                modifier = Modifier.testTag("start_audio_call_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Voice Call",
                    tint = Color.White
                )
            }

            Box {
                IconButton(onClick = { showTopMoreMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = Color.White
                    )
                }
                DropdownMenu(
                    expanded = showTopMoreMenu,
                    onDismissRequest = { showTopMoreMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (activeContact.isGroup) "Group info" else "View contact") },
                        onClick = {
                            showTopMoreMenu = false
                            showContactOrGroupInfo = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Media, links, and docs") },
                        onClick = {
                            showTopMoreMenu = false
                            showContactOrGroupInfo = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Set custom nickname (Local Alias)") },
                        onClick = {
                            showTopMoreMenu = false
                            showAliasModal = true
                        }
                    )
                    if (contacts.size > 1) {
                        DropdownMenuItem(
                            text = { Text("Switch contact") },
                            onClick = {
                                showTopMoreMenu = false
                                val nextIdx = (contacts.indexOfFirst { it.userId == activeContact.userId } + 1) % contacts.size
                                onSelectPeer(contacts[nextIdx].userId)
                            }
                        )
                    }
                }
            }
        }

        // Optional Contact Switcher Strip when multiple contacts exist (compact & clean)
        if (contacts.size > 1) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE6E0D6))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(contacts, key = { it.userId }) { peer ->
                    val isSelected = peer.userId == activeContact.userId
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) WhatsAppHeaderGreen else Color.White)
                            .clickable { onSelectPeer(peer.userId) }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .testTag("peer_tab_${peer.userId}")
                    ) {
                        Text(
                            text = peer.effectiveDisplayName,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) Color.White else LightTextPrimary
                        )
                    }
                }
            }
        }

        // 2. CONVERSATION MESSAGE AREA WITH SUBTLE DOODLE WALLPAPER
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Subtle WhatsApp Doodle Pattern Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 72.dp.toPx()
                val dotColor = Color(0xFFDCD5C9).copy(alpha = 0.55f)
                var y = 24f
                while (y < size.height) {
                    var x = 24f
                    while (x < size.width) {
                        drawCircle(
                            color = dotColor,
                            radius = 3.dp.toPx(),
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = dotColor,
                            radius = 2.dp.toPx(),
                            center = Offset(x + step * 0.5f, y + step * 0.5f)
                        )
                        x += step
                    }
                    y += step
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
                    .testTag("chat_messages_list"),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Encryption Banner (Matches WhatsApp Reference)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFFF5C4))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF54656F),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Messages and calls are end-to-end encrypted.",
                                fontSize = 11.sp,
                                color = Color(0xFF54656F)
                            )
                        }
                    }
                }

                items(messages, key = { it.id.ifBlank { it.messageId } }) { msg ->
                    val isOutgoing = msg.senderId == currentUserId
                    WhatsAppMessageBubbleItem(
                        message = msg,
                        isOutgoing = isOutgoing,
                        isGroupChat = activeContact.isGroup,
                        activePlayingUrl = activePlayingUrl,
                        isPlayingVoice = isPlayingVoice,
                        playbackProgress = playbackProgress,
                        onToggleVoicePlayback = onToggleVoicePlayback,
                        onSeekVoicePlayback = onSeekVoicePlayback,
                        onOpenFullscreenMedia = { onOpenFullscreenMedia(msg) },
                        onLongClickMessage = { selectedActionMessage = msg },
                        onReact = { emoji -> onReactToMessage(msg, emoji) }
                    )
                }
            }
        }

        // 3. REPLY / EDIT PREVIEW BANNER (if active)
        AnimatedVisibility(visible = replyingToMessage != null || editingMessage != null) {
            val target = replyingToMessage ?: editingMessage
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(WhatsAppActionGreen)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (editingMessage != null) "Editing message" else (target?.senderName ?: "Reply"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhatsAppActionGreen
                    )
                    Text(
                        text = target?.content.orEmpty(),
                        fontSize = 13.sp,
                        color = LightTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onCancelReplyOrEdit) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = LightTextMuted
                    )
                }
            }
        }

        // 4. VOICE RECORDING BAR OR WHATSAPP BOTTOM MESSAGE INPUT PILL
        if (isRecordingVoice) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.White)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Recording",
                        tint = TactileCrimson,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recording ${(recordingElapsedMs / 1000)}s...",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LightTextPrimary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onCancelVoiceRecording) {
                        Text("Cancel", color = TactileCrimson)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(WhatsAppActionGreen)
                            .clickable { onStopAndSendVoice() }
                            .testTag("stop_send_voice_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Voice Note",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        } else {
            // Standard WhatsApp Input Row: [ 😊  Type a message  📎  📷 ]  ( 🎤 / ➤ )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .shadow(1.dp, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { inputText += " 😊" },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEmotions,
                            contentDescription = "Emoji",
                            tint = LightTextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (inputText.isEmpty()) {
                            Text(
                                text = "Type a message",
                                color = LightTextMuted,
                                fontSize = 16.sp
                            )
                        }
                        BasicTextField(
                            value = inputText,
                            onValueChange = {
                                inputText = it
                                onTypingChanged(it)
                            },
                            textStyle = TextStyle(
                                color = LightTextPrimary,
                                fontSize = 16.sp
                            ),
                            cursorBrush = SolidColor(WhatsAppActionGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_message_input")
                        )
                    }

                    IconButton(
                        onClick = { showFileAttachModal = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("attach_file_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attach Document or File",
                            tint = LightTextMuted,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("pick_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera / Gallery Photo",
                            tint = LightTextMuted,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Far-Right Circular Green Mic / Send Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(2.dp, CircleShape)
                        .clip(CircleShape)
                        .background(WhatsAppActionGreen)
                        .clickable {
                            if (inputText.isNotBlank()) {
                                onSendText(inputText)
                                inputText = ""
                                onTypingChanged("")
                            } else {
                                val hasMicPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasMicPerm) {
                                    onStartVoiceRecording()
                                } else {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }
                        .testTag("send_or_voice_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (inputText.isNotBlank()) {
                            Icons.AutoMirrored.Filled.Send
                        } else {
                            Icons.Default.Mic
                        },
                        contentDescription = if (inputText.isNotBlank()) "Send Message" else "Record Voice Message",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }

    // Message Action Dialog (Reply, Edit, Delete, React)
    val actionMsg = selectedActionMessage
    if (actionMsg != null) {
        AlertDialog(
            onDismissRequest = { selectedActionMessage = null },
            title = { Text("Message Options") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        QUICK_REACTIONS.forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 24.sp,
                                modifier = Modifier.clickable {
                                    onReactToMessage(actionMsg, emoji)
                                    selectedActionMessage = null
                                }
                            )
                        }
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onStartReply(actionMsg)
                                selectedActionMessage = null
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = WhatsAppHeaderGreen)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Reply")
                    }
                    if (actionMsg.senderId == currentUserId && !actionMsg.isDeleted) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onStartEdit(actionMsg)
                                    selectedActionMessage = null
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = WhatsAppHeaderGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Edit message")
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDeleteMessage(actionMsg)
                                    selectedActionMessage = null
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = TactileCrimson)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Delete message", color = TactileCrimson)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedActionMessage = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Document / PDF Attachment Dialog (Matches "Project Plan.pdf • 2.4 MB • PDF" in Screen 3 of Reference 1)
    if (showFileAttachModal) {
        var docName by remember { mutableStateOf("Project Plan.pdf") }
        var docDesc by remember { mutableStateOf("2.4 MB • PDF") }
        AlertDialog(
            onDismissRequest = { showFileAttachModal = false },
            title = { Text("Share Document / PDF") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = docName,
                        onValueChange = { docName = it },
                        label = { Text("File Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = docDesc,
                        onValueChange = { docDesc = it },
                        label = { Text("Details / Size") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (docName.isNotBlank()) {
                            onSendFileAttachment(docName.trim(), docDesc.trim().ifBlank { "1.2 MB • PDF" })
                        }
                        showFileAttachModal = false
                    }
                ) {
                    Text("Send Document", color = WhatsAppActionGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFileAttachModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Local Alias Modal
    if (showAliasModal) {
        var aliasInput by remember { mutableStateOf(activeContact.overriddenDisplayName ?: "") }
        AlertDialog(
            onDismissRequest = { showAliasModal = false },
            title = { Text("Custom Contact Nickname") },
            text = {
                OutlinedTextField(
                    value = aliasInput,
                    onValueChange = { aliasInput = it },
                    label = { Text("Local Alias Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSaveLocalAlias(activeContact.userId, aliasInput, null)
                        showAliasModal = false
                    }
                ) {
                    Text("Save", color = WhatsAppActionGreen)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onClearLocalAlias(activeContact.userId)
                        showAliasModal = false
                    }
                ) {
                    Text("Reset")
                }
            }
        )
    }

    // Fullscreen Image Viewer Overlay
    if (fullscreenMediaMessage != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onOpenFullscreenMedia(null) },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = fullscreenMediaMessage.mediaUrl,
                contentDescription = fullscreenMediaMessage.content,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            IconButton(
                onClick = { onOpenFullscreenMedia(null) },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WhatsAppMessageBubbleItem(
    message: ChatMessage,
    isOutgoing: Boolean,
    isGroupChat: Boolean,
    activePlayingUrl: String?,
    isPlayingVoice: Boolean,
    playbackProgress: Float,
    onToggleVoicePlayback: (String, Long) -> Unit,
    onSeekVoicePlayback: (Float) -> Unit,
    onOpenFullscreenMedia: () -> Unit,
    onLongClickMessage: () -> Unit,
    onReact: (String) -> Unit
) {
    val timeFmt = remember { SimpleDateFormat("hh:mm", Locale.getDefault()) }
    val timeText = remember(message.timestamp) {
        val date = message.timestamp?.toDate()
        if (date != null) timeFmt.format(date) else "10:20"
    }
    val bubbleBg = if (isOutgoing) WhatsAppOutgoingBubble else WhatsAppIncomingBubble
    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(topStart = 12.dp, topEnd = 2.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
    } else {
        RoundedCornerShape(topStart = 2.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isOutgoing && isGroupChat) {
            WhatsAppAvatarCircle(name = message.senderName, size = 28.dp)
            Spacer(modifier = Modifier.width(6.dp))
        }

        Column(
            horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 90.dp, max = 300.dp)
                    .shadow(0.8.dp, bubbleShape)
                    .clip(bubbleShape)
                    .background(bubbleBg)
                    .combinedClickable(
                        onClick = {
                            if (message.mediaType == "IMAGE") onOpenFullscreenMedia()
                        },
                        onLongClick = onLongClickMessage
                    )
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                // Colored Sender Name in Group Chat (Matches Screen 3 of Reference 1)
                if (!isOutgoing && isGroupChat) {
                    val nameColor = GROUP_MEMBER_COLORS[
                        (message.senderName.hashCode() and 0x7FFFFFFF) % GROUP_MEMBER_COLORS.size
                    ]
                    Text(
                        text = message.senderName,
                        color = nameColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // Quoted Reply Block
                if (!message.replyToPreview.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.05f))
                            .padding(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(28.dp)
                                .background(WhatsAppActionGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = message.replyToSenderName ?: "Reply",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhatsAppActionGreen
                            )
                            Text(
                                text = message.replyToPreview,
                                fontSize = 12.sp,
                                color = LightTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Media Content Types
                when (message.mediaType) {
                    "IMAGE" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(175.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFD1D7DB))
                                .clickable { onOpenFullscreenMedia() }
                        ) {
                            AsyncImage(
                                model = message.mediaUrl,
                                contentDescription = message.content,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        if (message.content.isNotBlank() && message.content != "শেয়ার করা ছবি (Photo)") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = message.content,
                                fontSize = 15.sp,
                                color = LightTextPrimary
                            )
                        }
                    }

                    "FILE" -> {
                        // Matches "Project Plan.pdf • 2.4 MB • PDF" card in Screen 3 of Reference 1
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.04f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(WhatsAppPdfRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "PDF",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = message.fileName ?: "Document.pdf",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LightTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = message.content.ifBlank { "2.4 MB • PDF" },
                                    fontSize = 12.sp,
                                    color = LightTextSecondary
                                )
                            }
                        }
                    }

                    "AUDIO" -> {
                        val isThisPlaying = isPlayingVoice && activePlayingUrl == message.mediaUrl
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val url = message.mediaUrl.orEmpty()
                                    if (url.isNotBlank()) {
                                        onToggleVoicePlayback(url, message.audioDurationMs)
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play Voice Note",
                                    tint = WhatsAppActionGreen,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                NeoTactileScrubSlider(
                                    progress = if (isThisPlaying) playbackProgress else 0f,
                                    onSeek = onSeekVoicePlayback
                                )
                                Text(
                                    text = message.content,
                                    fontSize = 11.sp,
                                    color = LightTextSecondary
                                )
                            }
                        }
                    }

                    else -> {
                        Text(
                            text = message.content,
                            fontSize = 15.sp,
                            color = if (message.isDeleted) LightTextMuted else LightTextPrimary,
                            fontStyle = if (message.isDeleted) FontStyle.Italic else FontStyle.Normal
                        )
                    }
                }

                // Bottom-Right Timestamp + Blue Double-Check Receipt
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (message.isEdited) {
                        Text(
                            text = "Edited",
                            fontSize = 10.sp,
                            color = LightTextMuted
                        )
                    }
                    Text(
                        text = timeText,
                        fontSize = 11.sp,
                        color = LightTextMuted
                    )
                    if (isOutgoing) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Read Receipt",
                            tint = WhatsAppReadBlue,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // Emoji Reactions Pill below bubble
            if (!message.reactions.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .shadow(1.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    message.reactions.split(",").forEach { entry ->
                        val emoji = entry.substringBefore(":")
                        if (emoji.isNotBlank()) {
                            Text(
                                text = emoji,
                                fontSize = 13.sp,
                                modifier = Modifier.clickable { onReact(emoji) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// SCREENS 4 & 5: CHAT INFO (CONTACT INFO) & GROUP INFO
// Matches WhatsApp_UI_Reference_1 #4 & #5 and Reference_2 Group/Contact Info
// ============================================================================
@Composable
fun WhatsAppChatOrGroupInfoScreen(
    contact: ResolvedContactProfile,
    messages: List<ChatMessage>,
    onBack: () -> Unit,
    onStartAudioCall: () -> Unit,
    onStartVideoCall: () -> Unit,
    onOpenAliasEditor: () -> Unit,
    onAddParticipants: () -> Unit
) {
    var mediaVisibility by remember { mutableStateOf(true) }
    var muteNotifications by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightSurfaceSecondary)
            .testTag("chat_or_group_info_screen")
    ) {
        // Top Green Header Bar
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
                    text = contact.effectiveDisplayName,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            IconButton(onClick = onOpenAliasEditor) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = Color.White
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Top White Profile Card with Large Circular Avatar + Audio / Video / Search Buttons
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(vertical = 20.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    WhatsAppAvatarCircle(
                        name = contact.effectiveDisplayName,
                        isGroup = contact.isGroup,
                        size = 104.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = contact.effectiveDisplayName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (contact.isGroup) {
                            "Group • ${contact.memberIds.size.coerceAtLeast(5)} members"
                        } else {
                            contact.phoneNumber.ifBlank { "online" }
                        },
                        fontSize = 14.sp,
                        color = LightTextSecondary
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // 3 Outlined Action Cards: Audio | Video | Search
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                    ) {
                        InfoActionButton(
                            icon = Icons.Default.Call,
                            label = "Audio",
                            onClick = onStartAudioCall
                        )
                        InfoActionButton(
                            icon = Icons.Default.Videocam,
                            label = "Video",
                            onClick = onStartVideoCall
                        )
                        InfoActionButton(
                            icon = Icons.Default.Search,
                            label = "Search",
                            onClick = onBack
                        )
                    }
                }
            }

            // About / Group Description Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (contact.isGroup) "Add group description" else "About",
                        fontSize = 13.sp,
                        color = LightTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = contact.roleTag.ifBlank { "Hey there! I am using WhatsApp." },
                        fontSize = 15.sp,
                        color = LightTextPrimary
                    )
                }
            }

            // Group Add Participants Row (Screen 5 of Reference 1)
            if (contact.isGroup) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .clickable { onAddParticipants() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(WhatsAppActionGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Add participants",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Add participants",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = LightTextPrimary
                        )
                    }
                }
            }

            // Media, links and docs Strip (Matches Screen 4 & 5 of Reference 1 & Reference 2)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Media, links and docs",
                            fontSize = 14.sp,
                            color = LightTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${messages.count { it.mediaType != "TEXT" }.coerceAtLeast(4)} >",
                            fontSize = 14.sp,
                            color = LightTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val thumbColors = listOf(
                            Color(0xFF0284C7),
                            Color(0xFF059669),
                            Color(0xFFD97706),
                            Color(0xFFE9EDEF)
                        )
                        thumbColors.forEachIndexed { idx, c ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(68.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(c),
                                contentAlignment = Alignment.Center
                            ) {
                                if (idx < 3) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                } else {
                                    Text(
                                        text = "+12",
                                        fontWeight = FontWeight.Bold,
                                        color = LightTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Notifications, Media Visibility, Mute & Encryption Rows
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = LightTextSecondary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Notifications", fontSize = 16.sp, color = LightTextPrimary)
                            Text("Custom notifications", fontSize = 13.sp, color = LightTextSecondary)
                        }
                    }

                    HorizontalDivider(color = LightDividerGray, thickness = 0.6.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = LightTextSecondary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Media visibility", fontSize = 16.sp, color = LightTextPrimary)
                        }
                        Switch(
                            checked = mediaVisibility,
                            onCheckedChange = { mediaVisibility = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = WhatsAppActionGreen
                            )
                        )
                    }

                    HorizontalDivider(color = LightDividerGray, thickness = 0.6.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = LightTextSecondary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Mute notifications", fontSize = 16.sp, color = LightTextPrimary)
                                Text("Until 8:00 AM", fontSize = 12.sp, color = LightTextSecondary)
                            }
                        }
                        Switch(
                            checked = muteNotifications,
                            onCheckedChange = { muteNotifications = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = WhatsAppActionGreen
                            )
                        )
                    }

                    HorizontalDivider(color = LightDividerGray, thickness = 0.6.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = LightTextSecondary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Encryption", fontSize = 16.sp, color = LightTextPrimary)
                            Text(
                                "Messages and calls are end-to-end encrypted.",
                                fontSize = 13.sp,
                                color = LightTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(88.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, LightDividerGray, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = WhatsAppActionGreen,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = WhatsAppActionGreen,
            fontWeight = FontWeight.Medium
        )
    }
}

// ============================================================================
// SCREENS 14 & 15: VOICE CALL & VIDEO CALL SCREENS
// Matches WhatsApp_UI_Reference_1 #14 (Voice Call) & #15 (Video Call)
// ============================================================================
@Composable
private fun WhatsAppActiveCallScreen(
    contact: ResolvedContactProfile,
    callMode: String,
    activeCallSession: CallLogEntry? = null,
    onSwitchToVideo: () -> Unit,
    onEndCall: (Int) -> Unit
) {
    val context = LocalContext.current
    var elapsedSec by remember { mutableIntStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(callMode == "VIDEO") }
    var isCameraOn by remember { mutableStateOf(callMode == "VIDEO") }
    var useFrontCamera by remember { mutableStateOf(true) }

    val localVideoTrack by WebRtcHardwareController.localVideoTrackFlow.collectAsStateWithLifecycle()
    val remoteVideoTrack by WebRtcHardwareController.remoteVideoTrackFlow.collectAsStateWithLifecycle()
    val iceState by WebRtcHardwareController.connectionStateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(isSpeakerOn, isMuted) {
        WebRtcHardwareController.configureCallAudioSession(
            context = context,
            enableSpeakerphone = isSpeakerOn,
            muteMicrophone = isMuted
        )
    }

    LaunchedEffect(isCameraOn) {
        WebRtcHardwareController.setVideoEnabled(isCameraOn)
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            WebRtcHardwareController.releaseCallAudioSession(context)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            elapsedSec++
        }
    }

    val signalingStatus = activeCallSession?.status ?: "RINGING"
    val formattedDuration = remember(elapsedSec, signalingStatus, iceState) {
        if (signalingStatus == "RINGING" && elapsedSec < 3) {
            "Ringing..."
        } else {
            String.format(Locale.US, "%02d:%02d", elapsedSec / 60, elapsedSec % 60)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WhatsAppCallScreenBg)
            .testTag("active_call_screen")
    ) {
        if (callMode == "VIDEO" && isCameraOn) {
            // Remote Peer Video Track (SurfaceViewRenderer) or Avatar while waiting for remote track (Screen 15)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF134E4A),
                                Color(0xFF0F766E),
                                Color(0xFF0B2E28)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                val activeRemoteTrack = remoteVideoTrack
                if (activeRemoteTrack != null) {
                    WebRtcVideoTrackRenderer(
                        videoTrack = activeRemoteTrack,
                        modifier = Modifier.fillMaxSize(),
                        mirror = false
                    )
                } else {
                    WhatsAppAvatarCircle(name = contact.effectiveDisplayName, size = 148.dp)
                }
            }

            // Floating Live WebRTC Local VideoTrack / CameraX PiP Card on Bottom-Right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 110.dp)
                    .width(104.dp)
                    .height(144.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1F2C34))
                    .border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                val activeLocalTrack = localVideoTrack
                if (activeLocalTrack != null) {
                    WebRtcVideoTrackRenderer(
                        videoTrack = activeLocalTrack,
                        modifier = Modifier.fillMaxSize(),
                        mirror = useFrontCamera
                    )
                } else {
                    RealCameraXPreview(
                        modifier = Modifier.fillMaxSize(),
                        useFrontCamera = useFrontCamera,
                        enableQrAnalysis = false
                    )
                }
            }
        } else {
            // Screen 14: Voice Call Screen Centered Avatar
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                WhatsAppAvatarCircle(
                    name = contact.effectiveDisplayName,
                    isGroup = contact.isGroup,
                    size = 140.dp
                )
            }
        }

        // Top Call Header: Back Arrow + Centered Name + "Ringing..." / "Video call"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 18.dp, start = 12.dp, end = 12.dp)
        ) {
            IconButton(
                onClick = { onEndCall(elapsedSec) },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = contact.effectiveDisplayName,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (callMode == "VIDEO" && elapsedSec < 2) "Video call" else formattedDuration,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
        }

        // Bottom 4 Circular Control Buttons (Speaker/SwitchCam, Video, Mute, Red End Call)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 28.dp, vertical = 28.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Button 1: Speaker (Audio Call) or Switch Camera (Video Call)
            IconButton(
                onClick = {
                    if (callMode == "VIDEO") {
                        useFrontCamera = !useFrontCamera
                        WebRtcHardwareController.switchCamera()
                    } else {
                        isSpeakerOn = !isSpeakerOn
                    }
                },
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSpeakerOn) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.14f)
                    )
            ) {
                Icon(
                    imageVector = if (callMode == "VIDEO") {
                        Icons.Default.Cameraswitch
                    } else {
                        Icons.AutoMirrored.Filled.VolumeUp
                    },
                    contentDescription = "Speaker / Switch Camera",
                    tint = Color.White
                )
            }

            // Button 2: Video On/Off
            IconButton(
                onClick = {
                    isCameraOn = !isCameraOn
                    if (callMode != "VIDEO") onSwitchToVideo()
                },
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
            ) {
                Icon(
                    imageVector = if (isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    contentDescription = "Toggle Video",
                    tint = Color.White
                )
            }

            // Button 3: Mute / Unmute Microphone
            IconButton(
                onClick = { isMuted = !isMuted },
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (isMuted) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.14f)
                    )
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Mute Microphone",
                    tint = Color.White
                )
            }

            // Button 4: Red End Call Button
            IconButton(
                onClick = { onEndCall(elapsedSec) },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(TactileCrimson)
                    .testTag("end_call_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    tint = Color.White
                )
            }
        }
    }
}
