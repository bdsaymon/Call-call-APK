package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.CallLogEntry
import com.example.data.model.ResolvedContactProfile
import com.example.data.model.StoryItem
import com.example.ui.components.GlowingCyanRingBox
import com.example.ui.components.NeoTactilePillButton
import com.example.ui.components.frostedGlassCard
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlowingCyan
import com.example.ui.theme.TactileAmber
import com.example.ui.theme.TactileEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    language: AppLanguage,
    stories: List<StoryItem>,
    contacts: List<ResolvedContactProfile>,
    callLogs: List<CallLogEntry> = emptyList(),
    activeStoryViewerItem: StoryItem?,
    onPublishStory: (String, String) -> Unit,
    onOpenStory: (StoryItem) -> Unit,
    onCloseStory: () -> Unit,
    onSelectContactForChat: (String) -> Unit,
    onNavigateToDirectory: () -> Unit = {},
    onTogglePinContact: (String) -> Unit = {},
    onStartCallFromHome: (peerId: String, peerName: String, callType: String) -> Unit = { _, _, _ -> }
) {
    var showCreateStoryDialog by remember { mutableStateOf(false) }
    var selectedFilterIndex by remember { mutableIntStateOf(0) } // 0: All, 1: Updates (Status), 2: Calls, 3: Groups

    val filteredContacts = remember(contacts, selectedFilterIndex) {
        val sorted = contacts.sortedByDescending { it.isPinned }
        when (selectedFilterIndex) {
            3 -> sorted.filter { it.isGroup }
            else -> sorted.filter { !it.isArchived }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_feed_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Emerald Green & White Quick-Action Header Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .frostedGlassCard(cornerRadius = 24.dp, accentGlow = GlowingCyan)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = language.tr("চ্যাট, স্ট্যাটাস ও কল হাব", "CHATS, UPDATES & CALLS"),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = language.tr(
                                    "${contacts.count { !it.isGroup }} কন্টাক্ট • ${contacts.count { it.isGroup }} গ্রুপ • কোনো ডেমো নেই",
                                    "${contacts.count { !it.isGroup }} Contacts • ${contacts.count { it.isGroup }} Groups • No Demo Data"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = GlowingCyan
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionButton(
                            icon = Icons.Default.PersonAdd,
                            label = language.shortTr("+ ইউজার অ্যাড", "+ Add User"),
                            accentColor = ElectricBlue,
                            onClick = onNavigateToDirectory,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_manage_peers_button")
                        )
                        QuickActionButton(
                            icon = Icons.Default.GroupAdd,
                            label = language.shortTr("+ নতুন গ্রুপ", "+ New Group"),
                            accentColor = GlowingCyan,
                            onClick = onNavigateToDirectory,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_create_group_button")
                        )
                    }

                    // Filter Chips: Chats | Updates | Calls | Groups
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filters = listOf(
                            language.shortTr("সব চ্যাট", "Chats"),
                            language.shortTr("স্ট্যাটাস", "Updates"),
                            language.shortTr("কল (${callLogs.size})", "Calls"),
                            language.shortTr("গ্রুপ", "Groups")
                        )
                        filters.forEachIndexed { idx, title ->
                            val isSel = selectedFilterIndex == idx
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSel) ElectricBlue else Color(0xFF13261E))
                                    .border(
                                        1.dp,
                                        if (isSel) GlowingCyan else Color.White.copy(alpha = 0.14f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { selectedFilterIndex = idx }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSel) Color.White else TextSecondary,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // 2. 24-Hour Status & Updates Tray
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = language.tr("২৪ ঘণ্টার স্ট্যাটাস ও আপডেট", "24H STATUS UPDATES"),
                            style = MaterialTheme.typography.labelLarge,
                            color = GlowingCyan
                        )
                        Text(
                            text = "${stories.size} Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        item {
                            AddStoryButtonItem(
                                label = language.shortTr("নতুন স্ট্যাটাস", "My Status"),
                                onClick = { showCreateStoryDialog = true }
                            )
                        }
                        items(stories, key = { it.id.ifBlank { it.storyId } }) { story ->
                            StoryRingAvatarItem(
                                story = story,
                                onClick = { onOpenStory(story) }
                            )
                        }
                    }
                }
            }

            // 3. Call History Section when Calls filter is selected
            if (selectedFilterIndex == 2) {
                item {
                    Text(
                        text = language.tr("সাম্প্রতিক WebRTC অডিও ও ভিডিও কল", "RECENT WEBRTC CALLS"),
                        style = MaterialTheme.typography.labelLarge,
                        color = GlowingCyan
                    )
                }
                if (callLogs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .frostedGlassCard(cornerRadius = 22.dp)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = language.tr(
                                    "এখনো কোনো কল রেকর্ড নেই। যেকোনো কন্টাক্টের পাশে কল আইকনে ট্যাপ করে রিয়েল-টাইম অডিও বা ভিডিও কল শুরু করুন।",
                                    "No call history yet. Tap the Audio or Video call icon next to any contact."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    items(callLogs, key = { it.callId }) { log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .frostedGlassCard(cornerRadius = 20.dp)
                                .clickable { onSelectContactForChat(log.peerId) }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (log.callType == "VIDEO") Icons.Default.Videocam else Icons.Default.Call,
                                    contentDescription = null,
                                    tint = GlowingCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = log.peerName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${log.callType} • ${log.status} • ${log.durationSeconds}s",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Text(
                                text = "STUN/TURN",
                                style = MaterialTheme.typography.labelSmall,
                                color = TactileEmerald
                            )
                        }
                    }
                }
            }

            // 4. Active Contacts & Groups List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = language.tr("চ্যাট ও কন্টাক্ট তালিকা", "CONVERSATIONS & CONTACTS"),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = language.shortTr("+ ইউজার অ্যাড করুন →", "+ Add User →"),
                        style = MaterialTheme.typography.labelSmall,
                        color = GlowingCyan,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { onNavigateToDirectory() }
                            .padding(4.dp)
                    )
                }
            }

            if (filteredContacts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .frostedGlassCard(cornerRadius = 24.dp, accentGlow = GlowingCyan)
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = GlowingCyan,
                            modifier = Modifier.size(46.dp)
                        )
                        Text(
                            text = language.tr(
                                "কোনো ডেমো চ্যাট নেই — নতুন ইউজার অ্যাড করুন",
                                "No Demo Chats — Add Real Users"
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = language.tr(
                                "ইউজারনেম (@username), ইমেইল, ফোন নম্বর অথবা শর্ট আইডি দিয়ে বন্ধু যুক্ত করে চ্যাট ও কল শুরু করুন।",
                                "Add contacts by @username, email, phone number, or Short ID to start messaging and calling."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        NeoTactilePillButton(
                            text = language.shortTr("+ ইউজার অ্যাড করুন", "+ Add User Now"),
                            icon = Icons.Default.PersonAdd,
                            onClick = onNavigateToDirectory,
                            testTag = "home_empty_add_user_button"
                        )
                    }
                }
            } else {
                items(filteredContacts, key = { it.userId }) { contact ->
                    ContactChannelCard(
                        language = language,
                        contact = contact,
                        onClick = { onSelectContactForChat(contact.userId) },
                        onPinClick = { onTogglePinContact(contact.userId) },
                        onAudioCallClick = {
                            onStartCallFromHome(contact.userId, contact.effectiveDisplayName, "AUDIO")
                            onSelectContactForChat(contact.userId)
                        },
                        onVideoCallClick = {
                            onStartCallFromHome(contact.userId, contact.effectiveDisplayName, "VIDEO")
                            onSelectContactForChat(contact.userId)
                        }
                    )
                }
            }
        }

        // Fullscreen Story Viewer
        if (activeStoryViewerItem != null) {
            FullscreenStoryViewerOverlay(
                language = language,
                story = activeStoryViewerItem,
                onClose = onCloseStory
            )
        }
    }

    if (showCreateStoryDialog) {
        CreateStoryModalDialog(
            language = language,
            onDismiss = { showCreateStoryDialog = false },
            onPublish = { caption, preset ->
                showCreateStoryDialog = false
                onPublishStory(caption, preset)
            }
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF12261E))
            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = accentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AddStoryButtonItem(
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .testTag("add_story_button")
    ) {
        GlowingCyanRingBox(
            size = 68.dp,
            isActive = true,
            amplitude = 0.25f,
            ringColor = ElectricBlue
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = label,
                tint = GlowingCyan,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary
        )
    }
}

@Composable
private fun StoryRingAvatarItem(
    story: StoryItem,
    onClick: () -> Unit
) {
    val hoursRemaining = remember(story.expiresAt) {
        val diffMs = (story.expiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
        (diffMs / (1000L * 60L * 60L)).coerceAtLeast(1L)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(74.dp)
            .clickable { onClick() }
            .testTag("story_item_${story.storyId}")
    ) {
        GlowingCyanRingBox(
            size = 68.dp,
            isActive = true,
            amplitude = 0.5f,
            ringColor = GlowingCyan
        ) {
            Text(
                text = story.authorName.take(2).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = GlowingCyan
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = story.authorName,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${hoursRemaining}h",
            style = MaterialTheme.typography.labelSmall,
            color = GlowingCyan
        )
    }
}

@Composable
private fun ContactChannelCard(
    language: AppLanguage,
    contact: ResolvedContactProfile,
    onClick: () -> Unit,
    onPinClick: () -> Unit,
    onAudioCallClick: () -> Unit,
    onVideoCallClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlassCard(
                cornerRadius = 22.dp,
                accentGlow = when {
                    contact.isPinned -> GlowingCyan
                    contact.isGroup -> TactileAmber
                    else -> ElectricBlue
                }
            )
            .clickable { onClick() }
            .padding(16.dp)
            .testTag("contact_card_${contact.userId}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlowingCyanRingBox(
            size = 54.dp,
            isActive = contact.isOnline,
            amplitude = if (contact.hasLocalOverride || contact.isGroup) 0.45f else 0.15f,
            ringColor = when {
                contact.isGroup -> TactileAmber
                contact.hasLocalOverride -> GlowingCyan
                else -> ElectricBlue
            }
        ) {
            Icon(
                imageVector = if (contact.isGroup) Icons.Default.Groups else Icons.Default.Person,
                contentDescription = contact.effectiveDisplayName,
                tint = if (contact.isGroup) TactileAmber else GlowingCyan,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = contact.effectiveDisplayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                if (contact.isPinned) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = GlowingCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
                if (contact.isGroup) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(TactileAmber.copy(alpha = 0.2f))
                            .border(1.dp, TactileAmber.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = language.shortTr("গ্রুপ", "GROUP"),
                            style = MaterialTheme.typography.labelSmall,
                            color = TactileAmber
                        )
                    }
                } else if (contact.hasLocalOverride) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(GlowingCyan.copy(alpha = 0.18f))
                            .border(1.dp, GlowingCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = language.shortTr("লোকাল নাম", "ALIAS"),
                            style = MaterialTheme.typography.labelSmall,
                            color = GlowingCyan
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${contact.roleTag} • ID: ${contact.shortCode}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onPinClick,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = "Pin Chat",
                    tint = if (contact.isPinned) GlowingCyan else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onAudioCallClick,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Audio Call",
                    tint = TactileEmerald,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onVideoCallClick,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Video Call",
                    tint = GlowingCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Chat,
                contentDescription = "Open Chat",
                tint = GlowingCyan,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun FullscreenStoryViewerOverlay(
    language: AppLanguage,
    story: StoryItem,
    onClose: () -> Unit
) {
    var progress by remember(story.storyId) { mutableFloatStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }

    LaunchedEffect(story.storyId, isPaused) {
        while (progress < 1f) {
            if (!isPaused) {
                progress = (progress + 0.02f).coerceAtMost(1f)
            }
            delay(80L)
        }
        onClose()
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(80, easing = LinearEasing),
        label = "storyProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A1410), Color(0xFF102A20), Color(0xFF0A1410))
                )
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPaused = true
                        tryAwaitRelease()
                        isPaused = false
                    }
                )
            }
            .padding(24.dp)
            .testTag("fullscreen_story_viewer")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = GlowingCyan,
                trackColor = Color.White.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlowingCyanRingBox(size = 44.dp, isActive = true, amplitude = 0.6f) {
                        Text(
                            text = story.authorName.take(2).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = GlowingCyan
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = story.authorName,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isPaused) {
                                language.tr("বিরতি (ধরে রাখা হয়েছে)", "PAUSED")
                            } else {
                                language.tr("২৪ ঘণ্টার স্ট্যাটাস আপডেট", "24H STATUS UPDATE")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = GlowingCyan
                        )
                    }
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("close_story_viewer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Story Viewer",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .frostedGlassCard(cornerRadius = 28.dp, accentGlow = GlowingCyan)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = GlowingCyan,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = story.caption,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = language.tr("থামাতে স্ক্রিনে ধরে রাখুন", "Hold anywhere to pause"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CreateStoryModalDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onPublish: (String, String) -> Unit
) {
    var caption by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF102019),
        title = {
            Text(
                text = language.tr("নতুন স্ট্যাটাস আপডেট শেয়ার করুন", "Share 24-Hour Status Update"),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = language.tr(
                        "এই স্ট্যাটাসটি ২৪ ঘণ্টা পর্যন্ত আপনার কন্টাক্টদের কাছে দৃশ্যমান থাকবে।",
                        "Visible to your contacts in Firestore for 24 hours."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text(language.shortTr("আপনার স্ট্যাটাস লিখুন...", "Status Text / Caption")) },
                    singleLine = false,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlowingCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("story_caption_input")
                )
            }
        },
        confirmButton = {
            NeoTactilePillButton(
                text = language.shortTr("পোস্ট করুন", "Publish"),
                onClick = { onPublish(caption, "preset://emerald_status") },
                accentColor = ElectricBlue,
                testTag = "confirm_publish_story_button"
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
