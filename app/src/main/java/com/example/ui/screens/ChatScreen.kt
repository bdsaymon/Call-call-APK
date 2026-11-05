package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.AppLanguage
import com.example.data.model.ChatMessage
import com.example.data.model.ResolvedContactProfile
import com.example.ui.components.GlowingCyanRingBox
import com.example.ui.components.NeoTactilePillButton
import com.example.ui.components.NeoTactileScrubSlider
import com.example.ui.components.frostedGlassCard
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlowingCyan
import com.example.ui.theme.TactileAmber
import com.example.ui.theme.TactileCrimson
import com.example.ui.theme.TactileEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private val QUICK_REACTIONS = listOf("❤️", "👍", "🔥", "😂", "😮")

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
    onEndWebRtcCall: (durationSec: Int) -> Unit = {}
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var showAliasModal by remember { mutableStateOf(false) }
    var activeCallMode by remember { mutableStateOf<String?>(null) } // "AUDIO" or "VIDEO"
    var showFileAttachModal by remember { mutableStateOf(false) }
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
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .frostedGlassCard(cornerRadius = 26.dp, accentGlow = GlowingCyan)
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = GlowingCyan,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = language.tr(
                        "কোনো কন্টাক্ট বা চ্যাট নেই",
                        "No Active Conversations Yet"
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = language.tr(
                        "কোনো ডেমো চ্যাট রাখা হয়নি। রিয়েল-টাইম মেসেজিং ও WebRTC অডিও/ভিডিও কল শুরু করতে ইউজারনেম, ইমেইল, ফোন নম্বর বা শর্ট আইডি দিয়ে একজন আসল ইউজার অ্যাড করুন।",
                        "Strict No-Demo Mode: Add a real registered user by username, email, phone number, or Short ID to start messaging and calling."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                NeoTactilePillButton(
                    text = language.shortTr("+ ইউজার অ্যাড করুন", "+ Add Real User"),
                    icon = Icons.Default.PersonAdd,
                    onClick = onNavigateToDirectory,
                    testTag = "empty_chat_add_user_button"
                )
            }
        }
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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Peer & Group Selector Strip
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(contacts, key = { it.userId }) { peer ->
                    val isSelected = peer.userId == activeContact.userId
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) ElectricBlue else Color(0xFF1A202C))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) GlowingCyan else Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { onSelectPeer(peer.userId) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("peer_tab_${peer.userId}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (peer.isGroup) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else TactileAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = peer.effectiveDisplayName,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                        if (peer.hasLocalOverride) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(GlowingCyan)
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF141A24))
                            .border(
                                width = 1.dp,
                                color = GlowingCyan.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { onNavigateToDirectory() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("chat_add_peer_pill"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add Peer",
                            tint = GlowingCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = language.shortTr("+ বন্ধু/গ্রুপ", "+ Peer"),
                            style = MaterialTheme.typography.labelMedium,
                            color = GlowingCyan
                        )
                    }
                }
            }

            // 2. Clean High-Contrast Chat Header with Audio Call, Video Call & Local Alias
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .frostedGlassCard(cornerRadius = 22.dp, accentGlow = GlowingCyan)
                    .combinedClickable(
                        onClick = { showAliasModal = true },
                        onLongClick = { showAliasModal = true }
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("chat_header_card"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    GlowingCyanRingBox(
                        size = 46.dp,
                        isActive = true,
                        amplitude = if (activeContact.hasLocalOverride || activeContact.isGroup) 0.5f else 0.15f,
                        ringColor = if (activeContact.isGroup) TactileAmber else GlowingCyan
                    ) {
                        Text(
                            text = activeContact.effectiveDisplayName.take(2).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (activeContact.isGroup) TactileAmber else GlowingCyan
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = activeContact.effectiveDisplayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("chat_header_display_name")
                            )
                            if (activeContact.hasLocalOverride) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = language.shortTr("(লোকাল নাম)", "(ALIAS)"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GlowingCyan
                                )
                            }
                        }
                        Text(
                            text = if (activeContact.isGroup) {
                                language.tr("গ্রুপ চ্যাট • ${activeContact.roleTag}", "Group Chat • ${activeContact.roleTag}")
                            } else {
                                language.tr("অনলাইন • আইডি: ${activeContact.shortCode}", "Online • ID: ${activeContact.shortCode}")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            activeCallMode = "AUDIO"
                            onStartWebRtcCall(activeContact.userId, activeContact.effectiveDisplayName, "AUDIO")
                        },
                        modifier = Modifier.testTag("chat_audio_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Audio Call",
                            tint = TactileEmerald
                        )
                    }

                    IconButton(
                        onClick = {
                            activeCallMode = "VIDEO"
                            onStartWebRtcCall(activeContact.userId, activeContact.effectiveDisplayName, "VIDEO")
                        },
                        modifier = Modifier.testTag("chat_video_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video Call",
                            tint = ElectricBlue
                        )
                    }

                    IconButton(
                        onClick = { showAliasModal = true },
                        modifier = Modifier.testTag("edit_local_alias_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Customize Local Alias",
                            tint = GlowingCyan
                        )
                    }
                }
            }

            // 3. Realtime Messages List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("chat_messages_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .frostedGlassCard(cornerRadius = 22.dp)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = GlowingCyan,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = language.tr("চ্যাট শুরু করার জন্য প্রস্তুত", "Ready to Start Chatting"),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = language.tr(
                                        "টেক্সট মেসেজ, ছবি, ফাইল অথবা ভয়েস নোট পাঠান। যেকোনো মেসেজে ট্যাপ করে রিপ্লাই বা রিঅ্যাকশন দিন।",
                                        "Send text, photos, files, or voice notes. Tap any message to reply, edit, or react."
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                items(messages, key = { it.id.ifBlank { it.messageId } }) { msg ->
                    val isMine = msg.senderId == currentUserId
                    ChatMessageBubble(
                        language = language,
                        message = msg,
                        isMine = isMine,
                        peerDisplayName = if (activeContact.isGroup) msg.senderName else activeContact.effectiveDisplayName,
                        isPlayingThis = activePlayingUrl == msg.mediaUrl && isPlayingVoice,
                        playbackProgress = if (activePlayingUrl == msg.mediaUrl) playbackProgress else 0f,
                        playbackAmplitude = if (activePlayingUrl == msg.mediaUrl && isPlayingVoice) playbackAmplitude else 0f,
                        onTogglePlay = {
                            val url = msg.mediaUrl ?: "preset://voice_${msg.messageId}"
                            onToggleVoicePlayback(url, msg.audioDurationMs)
                        },
                        onSeek = onSeekVoicePlayback,
                        onOpenMedia = { onOpenFullscreenMedia(msg) },
                        onBubbleTapOrLongPress = { selectedActionMessage = msg },
                        onQuickReactionClick = { emoji -> onReactToMessage(msg, emoji) }
                    )
                }
            }

            // 4. Reply or Edit Preview Bar
            AnimatedVisibility(visible = replyingToMessage != null || editingMessage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF182234))
                        .border(1.dp, GlowingCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (editingMessage != null) {
                                language.tr("মেসেজ এডিট করা হচ্ছে", "Editing Message")
                            } else {
                                language.tr(
                                    "${replyingToMessage?.senderName ?: ""} এর মেসেজে রিপ্লাই",
                                    "Replying to ${replyingToMessage?.senderName ?: ""}"
                                )
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = GlowingCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = editingMessage?.content ?: replyingToMessage?.content ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = {
                            if (editingMessage != null) inputText = ""
                            onCancelReplyOrEdit()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Reply or Edit",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 5. Voice Recorder Bar or Message Composer Bar
            if (isRecordingVoice) {
                VoiceRecordingActiveBar(
                    language = language,
                    liveAmplitude = liveVoiceAmplitude,
                    elapsedMs = recordingElapsedMs,
                    onCancel = onCancelVoiceRecording,
                    onStopAndSend = onStopAndSendVoice
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .frostedGlassCard(cornerRadius = 26.dp)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Photo Picker Button
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.testTag("pick_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Share Photo",
                            tint = GlowingCyan
                        )
                    }

                    // File Attachment Button
                    IconButton(
                        onClick = { showFileAttachModal = true },
                        modifier = Modifier.testTag("attach_file_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Share File",
                            tint = TactileAmber
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = language.shortTr("মেসেজ লিখুন...", "Write a message..."),
                                color = TextSecondary
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input")
                    )

                    // Voice Note Record Button
                    IconButton(
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                onStartVoiceRecording()
                            } else {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier.testTag("record_voice_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Record Voice Note",
                            tint = GlowingCyan
                        )
                    }

                    // Send Button
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSendText(inputText)
                                inputText = ""
                            }
                        },
                        modifier = Modifier.testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            tint = ElectricBlue
                        )
                    }
                }
            }
        }

        // Fullscreen Media & File Viewer Overlay
        if (fullscreenMediaMessage != null) {
            FullscreenMediaViewerOverlay(
                language = language,
                message = fullscreenMediaMessage,
                onClose = { onOpenFullscreenMedia(null) }
            )
        }
    }

    // Message Action Dialog (Reply, Edit, Delete, Emoji Reactions)
    if (selectedActionMessage != null) {
        val targetMsg = selectedActionMessage!!
        val isMine = targetMsg.senderId == currentUserId
        MessageActionsDialog(
            language = language,
            message = targetMsg,
            isMine = isMine,
            onDismiss = { selectedActionMessage = null },
            onReact = { emoji ->
                selectedActionMessage = null
                onReactToMessage(targetMsg, emoji)
            },
            onReply = {
                selectedActionMessage = null
                onStartReply(targetMsg)
            },
            onEdit = {
                selectedActionMessage = null
                onStartEdit(targetMsg)
            },
            onDelete = {
                selectedActionMessage = null
                onDeleteMessage(targetMsg)
            }
        )
    }

    // File Share Dialog
    if (showFileAttachModal) {
        SendFileModalDialog(
            language = language,
            onDismiss = { showFileAttachModal = false },
            onSendFile = { fileName, desc ->
                showFileAttachModal = false
                onSendFileAttachment(fileName, desc)
            }
        )
    }

    // Local Alias Override Modal
    if (showAliasModal) {
        LocalAliasOverrideModal(
            language = language,
            contact = activeContact,
            onDismiss = { showAliasModal = false },
            onSave = { newName, newAvatar ->
                showAliasModal = false
                onSaveLocalAlias(activeContact.userId, newName, newAvatar)
            },
            onReset = {
                showAliasModal = false
                onClearLocalAlias(activeContact.userId)
            }
        )
    }

    // Audio or Video Call Modal
    if (activeCallMode != null) {
        TactileCallDialog(
            language = language,
            contact = activeContact,
            initialIsVideo = activeCallMode == "VIDEO",
            onDismiss = { elapsedSec ->
                activeCallMode = null
                onEndWebRtcCall(elapsedSec)
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatMessageBubble(
    language: AppLanguage,
    message: ChatMessage,
    isMine: Boolean,
    peerDisplayName: String,
    isPlayingThis: Boolean,
    playbackProgress: Float,
    playbackAmplitude: Float,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onOpenMedia: () -> Unit,
    onBubbleTapOrLongPress: () -> Unit,
    onQuickReactionClick: (String) -> Unit
) {
    val parsedReactions = remember(message.reactions) { message.parsedReactions() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 120.dp, max = 310.dp)
                    .frostedGlassCard(
                        cornerRadius = 22.dp,
                        accentGlow = if (isMine) ElectricBlue else GlowingCyan
                    )
                    .combinedClickable(
                        onClick = onBubbleTapOrLongPress,
                        onLongClick = onBubbleTapOrLongPress
                    )
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isMine) language.shortTr("আপনি", "You") else peerDisplayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMine) GlowingCyan else TactileEmerald
                    )
                    if (message.isEdited && !message.isDeleted) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = language.shortTr("(এডিটেড)", "(Edited)"),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                // Reply Preview Box if this message is a reply
                if (!message.replyToPreview.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF121824))
                            .border(1.dp, GlowingCyan.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "↩ ${message.replyToSenderName ?: ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GlowingCyan
                        )
                        Text(
                            text = message.replyToPreview,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (message.isDeleted) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = TextMuted
                    )
                } else {
                    when (message.mediaType) {
                        "AUDIO" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                GlowingCyanRingBox(
                                    size = 46.dp,
                                    isActive = true,
                                    amplitude = if (isPlayingThis) playbackAmplitude else 0.25f
                                ) {
                                    IconButton(
                                        onClick = onTogglePlay,
                                        modifier = Modifier.testTag("play_voice_note_${message.messageId}")
                                    ) {
                                        Icon(
                                            imageVector = if (isPlayingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlayingThis) "Pause Voice Note" else "Play Voice Note",
                                            tint = GlowingCyan
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    NeoTactileScrubSlider(
                                        progress = playbackProgress,
                                        onSeek = onSeek,
                                        testTag = "voice_scrub_slider_${message.messageId}"
                                    )
                                    Text(
                                        text = "${(message.audioDurationMs / 1000L).coerceAtLeast(1L)}s • Voice Note",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GlowingCyan
                                    )
                                }
                            }
                        }

                        "IMAGE" -> {
                            if (!message.mediaUrl.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(165.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onOpenMedia() }
                                ) {
                                    AsyncImage(
                                        model = message.mediaUrl,
                                        contentDescription = message.content,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xAA0F1319))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = language.shortTr("ফুল-স্ক্রিন দেখুন", "Tap to Expand"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GlowingCyan
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary
                            )
                        }

                        "FILE" -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF141B29))
                                    .border(1.dp, TactileAmber.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                    .clickable { onOpenMedia() }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = TactileAmber,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = message.fileName ?: "Attached_Document.pdf",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = message.content,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        else -> {
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Emoji Reaction Pills
            if (parsedReactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    parsedReactions.forEach { (emoji, count) ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1B2434))
                                .border(1.dp, GlowingCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickable { onQuickReactionClick(emoji) }
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "$emoji $count", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageActionsDialog(
    language: AppLanguage,
    message: ChatMessage,
    isMine: Boolean,
    onDismiss: () -> Unit,
    onReact: (String) -> Unit,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161C28),
        shape = RoundedCornerShape(26.dp),
        title = {
            Text(
                text = language.tr("মেসেজ অপশন ও রিঅ্যাকশন", "Message Options & Reactions"),
                style = MaterialTheme.typography.titleMedium,
                color = GlowingCyan
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Quick Emoji Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0F141D))
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QUICK_REACTIONS.forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1D2536))
                                .clickable { onReact(emoji) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }

                // Reply Button
                NeoTactilePillButton(
                    text = language.tr("রিপ্লাই দিন", "Reply to Message"),
                    icon = Icons.AutoMirrored.Filled.Reply,
                    onClick = onReply,
                    accentColor = ElectricBlue,
                    modifier = Modifier.fillMaxWidth()
                )

                // Edit & Delete for sender's own messages
                if (isMine && !message.isDeleted && message.mediaType == "TEXT") {
                    NeoTactilePillButton(
                        text = language.tr("মেসেজ এডিট করুন", "Edit Message"),
                        icon = Icons.Default.Edit,
                        onClick = onEdit,
                        accentColor = GlowingCyan,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (isMine && !message.isDeleted) {
                    NeoTactilePillButton(
                        text = language.tr("মেসেজ ডিলিট করুন", "Delete Message"),
                        icon = Icons.Default.DeleteOutline,
                        onClick = onDelete,
                        accentColor = TactileCrimson,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            NeoTactilePillButton(
                text = language.shortTr("বন্ধ করুন", "Close"),
                onClick = onDismiss,
                isActive = false
            )
        }
    )
}

@Composable
private fun SendFileModalDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSendFile: (String, String) -> Unit
) {
    var fileName by remember { mutableStateOf("Project_Notes_2026.pdf") }
    var fileDesc by remember { mutableStateOf("ডকুমেন্ট ফাইল (PDF • 1.2 MB)") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161C28),
        shape = RoundedCornerShape(26.dp),
        title = {
            Text(
                text = language.tr("ফাইল বা ডকুমেন্ট পাঠান", "Share File or Document"),
                style = MaterialTheme.typography.titleMedium,
                color = TactileAmber
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text(language.shortTr("ফাইলের নাম", "File Name")) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TactileAmber,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fileDesc,
                    onValueChange = { fileDesc = it },
                    label = { Text(language.shortTr("বিবরণ বা নোট", "Description / Size")) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TactileAmber,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            NeoTactilePillButton(
                text = language.shortTr("ফাইল পাঠান", "Send File"),
                icon = Icons.Default.AttachFile,
                onClick = {
                    if (fileName.isNotBlank()) {
                        onSendFile(fileName.trim(), fileDesc.trim().ifBlank { "Shared File" })
                    }
                },
                accentColor = ElectricBlue
            )
        },
        dismissButton = {
            NeoTactilePillButton(
                text = language.shortTr("বাতিল", "Cancel"),
                onClick = onDismiss,
                isActive = false
            )
        }
    )
}

@Composable
private fun FullscreenMediaViewerOverlay(
    language: AppLanguage,
    message: ChatMessage,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF20A0E17))
            .padding(20.dp)
            .testTag("fullscreen_media_viewer"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = language.tr("মিডিয়া ও ফাইল প্রিভিউ", "Media & File Preview"),
                        style = MaterialTheme.typography.labelSmall,
                        color = GlowingCyan
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Media Viewer",
                        tint = Color.White
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp)
                    .frostedGlassCard(cornerRadius = 26.dp, accentGlow = GlowingCyan)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (message.mediaType == "IMAGE" && !message.mediaUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = message.mediaUrl,
                        contentDescription = message.content,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(18.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = TactileAmber,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = message.fileName ?: message.content,
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            NeoTactilePillButton(
                text = language.tr("প্রিভিউ বন্ধ করুন", "Close Preview"),
                onClick = onClose,
                accentColor = ElectricBlue
            )
        }
    }
}

@Composable
private fun VoiceRecordingActiveBar(
    language: AppLanguage,
    liveAmplitude: Float,
    elapsedMs: Long,
    onCancel: () -> Unit,
    onStopAndSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlassCard(cornerRadius = 26.dp, accentGlow = GlowingCyan)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("voice_recording_active_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlowingCyanRingBox(
                size = 50.dp,
                isActive = true,
                amplitude = liveAmplitude,
                ringColor = GlowingCyan
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Recording",
                    tint = GlowingCyan,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = language.tr("ভয়েস রেকর্ড হচ্ছে • ${(elapsedMs / 1000L)}s", "Recording • ${(elapsedMs / 1000L)}s"),
                    style = MaterialTheme.typography.labelLarge,
                    color = GlowingCyan
                )
                Text(
                    text = "AAC 44.1kHz • ${(liveAmplitude * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeoTactilePillButton(
                text = language.shortTr("বাতিল", "Cancel"),
                onClick = onCancel,
                isActive = false,
                accentColor = TactileCrimson
            )
            NeoTactilePillButton(
                text = language.shortTr("পাঠান", "Send"),
                icon = Icons.Default.Stop,
                onClick = onStopAndSend,
                isActive = true,
                accentColor = ElectricBlue,
                testTag = "stop_and_send_voice_button"
            )
        }
    }
}

@Composable
private fun LocalAliasOverrideModal(
    language: AppLanguage,
    contact: ResolvedContactProfile,
    onDismiss: () -> Unit,
    onSave: (String?, String?) -> Unit,
    onReset: () -> Unit
) {
    var aliasInput by remember(contact) {
        mutableStateOf(contact.overriddenDisplayName ?: contact.originalDisplayName)
    }
    var avatarPreset by remember(contact) {
        mutableStateOf(contact.overriddenAvatarUri ?: "preset://avatar_cyan_ring")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181E2B),
        title = {
            Text(
                text = language.tr("লোকাল নাম পরিবর্তন (Room Alias)", "Local Device Alias Override"),
                style = MaterialTheme.typography.titleLarge,
                color = GlowingCyan
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = language.tr(
                        "এই নামটি শুধুমাত্র আপনার ফোনে (Room SQLite) সেভ থাকবে, ক্লাউডে যাবে না।",
                        "Stored strictly on this device (Room SQLite). Never sent to Firebase."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    text = "মূল নাম: ${contact.originalDisplayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TactileEmerald
                )
                OutlinedTextField(
                    value = aliasInput,
                    onValueChange = { aliasInput = it },
                    label = { Text(language.shortTr("নতুন লোকাল নাম", "Local Display Name")) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlowingCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("local_alias_name_input")
                )
            }
        },
        confirmButton = {
            NeoTactilePillButton(
                text = language.shortTr("সেভ করুন", "Save Alias"),
                onClick = { onSave(aliasInput, avatarPreset) },
                accentColor = ElectricBlue,
                testTag = "save_local_alias_button"
            )
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (contact.hasLocalOverride) {
                    NeoTactilePillButton(
                        text = language.shortTr("রিসেট", "Reset"),
                        onClick = onReset,
                        isActive = false,
                        testTag = "reset_local_alias_button"
                    )
                }
                NeoTactilePillButton(
                    text = language.shortTr("বন্ধ", "Close"),
                    onClick = onDismiss,
                    isActive = false
                )
            }
        }
    )
}

@Composable
private fun TactileCallDialog(
    language: AppLanguage,
    contact: ResolvedContactProfile,
    initialIsVideo: Boolean,
    onDismiss: (Int) -> Unit
) {
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(true) }
    var isFrontCamera by remember { mutableStateOf(true) }
    var isVideoEnabled by remember { mutableStateOf(initialIsVideo) }
    var elapsedSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000L)
            elapsedSeconds += 1
        }
    }

    val formattedDuration = remember(elapsedSeconds) {
        val mins = elapsedSeconds / 60
        val secs = elapsedSeconds % 60
        "%02d:%02d".format(mins, secs)
    }

    AlertDialog(
        onDismissRequest = { onDismiss(elapsedSeconds) },
        containerColor = Color(0xFF0E1D17),
        shape = RoundedCornerShape(30.dp),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isVideoEnabled) {
                        language.tr("WebRTC ভিডিও কল ($formattedDuration)", "WebRTC VIDEO CALL ($formattedDuration)")
                    } else {
                        language.tr("WebRTC অডিও কল ($formattedDuration)", "WebRTC AUDIO CALL ($formattedDuration)")
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = GlowingCyan
                )
                Text(
                    text = language.tr(
                        "STUN/TURN সংযুক্ত • ${if (isSpeakerOn) "স্পিকার অন" else "ইয়ারপিস"}",
                        "STUN/TURN Connected • ${if (isSpeakerOn) "Speaker" else "Earpiece"}"
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = TactileEmerald
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (isVideoEnabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF102A20), Color(0xFF0A1611))
                                )
                            )
                            .border(1.5.dp, GlowingCyan.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
                            .clickable { isFrontCamera = !isFrontCamera },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            GlowingCyanRingBox(
                                size = 76.dp,
                                isActive = true,
                                amplitude = if (!isMuted) 0.7f else 0.15f
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = GlowingCyan,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = contact.effectiveDisplayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }

                        // Self Camera PiP Corner Preview (Tap to switch Front/Rear Camera)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                                .size(width = 78.dp, height = 88.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF162D23))
                                .border(1.dp, ElectricBlue, RoundedCornerShape(12.dp))
                                .clickable { isFrontCamera = !isFrontCamera },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isFrontCamera) {
                                        language.shortTr("ফ্রন্ট ক্যামেরা", "Front Cam")
                                    } else {
                                        language.shortTr("ব্যাক ক্যামেরা", "Rear Cam")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GlowingCyan
                                )
                                Text(
                                    text = language.shortTr("(ট্যাপ=সুইচ)", "(Tap=Flip)"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    GlowingCyanRingBox(
                        size = 104.dp,
                        isActive = true,
                        amplitude = if (!isMuted) 0.75f else 0.15f
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = GlowingCyan,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = contact.effectiveDisplayName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "ID: ${contact.shortCode} • $formattedDuration",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute Mic Toggle
                IconButton(
                    onClick = { isMuted = !isMuted },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isMuted) TactileAmber else Color(0xFF162C23))
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Toggle Mute",
                        tint = if (isMuted) Color.Black else GlowingCyan
                    )
                }

                // Speakerphone Toggle
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSpeakerOn) ElectricBlue else Color(0xFF162C23))
                        .clickable { isSpeakerOn = !isSpeakerOn }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (isSpeakerOn) language.shortTr("স্পিকার", "Speaker") else language.shortTr("ইয়ার", "Ear"),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }

                // Video Camera Toggle
                IconButton(
                    onClick = { isVideoEnabled = !isVideoEnabled },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isVideoEnabled) ElectricBlue else Color(0xFF162C23))
                ) {
                    Icon(
                        imageVector = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        contentDescription = "Toggle Video",
                        tint = Color.White
                    )
                }

                // End Call Button
                IconButton(
                    onClick = { onDismiss(elapsedSeconds) },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(TactileCrimson)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White
                    )
                }
            }
        },
        dismissButton = {}
    )
}
