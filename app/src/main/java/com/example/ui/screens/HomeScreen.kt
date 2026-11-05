package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddIcCall
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AppLanguage
import com.example.data.model.CallLogEntry
import com.example.data.model.CommunityModel
import com.example.data.model.ResolvedContactProfile
import com.example.data.model.StoryItem
import com.example.ui.components.NeoTactilePillButton
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
import com.example.ui.theme.WhatsAppReadBlue
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val STATUS_BG_COLORS = listOf(
    Color(0xFF075E54),
    Color(0xFFD97706),
    Color(0xFF059669),
    Color(0xFF2563EB),
    Color(0xFF7C3AED),
    Color(0xFFDB2777),
    Color(0xFF1E293B)
)

private val AVATAR_TINTS = listOf(
    Color(0xFF008069),
    Color(0xFF0284C7),
    Color(0xFF7C3AED),
    Color(0xFFEA580C),
    Color(0xFF059669),
    Color(0xFFDB2777)
)

@Composable
fun WhatsAppAvatarCircle(
    name: String,
    isGroup: Boolean = false,
    size: Dp = 52.dp,
    showOnlineDot: Boolean = false,
    hasStatusRing: Boolean = false
) {
    val tint = remember(name) {
        AVATAR_TINTS[(name.hashCode() and 0x7FFFFFFF) % AVATAR_TINTS.size]
    }
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (hasStatusRing) {
                        Modifier
                            .border(2.5.dp, WhatsAppAccentGreen, CircleShape)
                            .padding(3.dp)
                    } else Modifier
                )
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            if (isGroup) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = name,
                    tint = tint,
                    modifier = Modifier.size(size * 0.52f)
                )
            } else {
                val initials = name.trim()
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .joinToString("") { it.first().uppercase() }
                    .ifBlank { "U" }
                Text(
                    text = initials,
                    color = tint,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.34f).sp
                )
            }
        }

        if (showOnlineDot) {
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(WhatsAppAccentGreen)
            )
        }
    }
}

/**
 * Legacy HomeScreen wrapper that routes between:
 * - Screen 1: Chats (Main Page)
 * - Screen 6: Status (Updates)
 * - Screen 7: Status Viewer
 * - Screen 8: Create Status (Text)
 * - Screen 9: Create Status (Photo)
 * - Screen 13 & 16: Calls (Main & Call History)
 */
@Composable
fun HomeScreen(
    language: AppLanguage,
    stories: List<StoryItem>,
    contacts: List<ResolvedContactProfile>,
    callLogs: List<CallLogEntry> = emptyList(),
    communities: List<CommunityModel> = emptyList(),
    activeStoryViewerItem: StoryItem?,
    onPublishStory: (String, String) -> Unit,
    onOpenStory: (StoryItem) -> Unit,
    onCloseStory: () -> Unit,
    onSelectContactForChat: (String) -> Unit,
    onNavigateToDirectory: () -> Unit = {},
    onTogglePinContact: (String) -> Unit = {},
    onStartCallFromHome: (peerId: String, peerName: String, callType: String) -> Unit = { _, _, _ -> },
    activeSubTab: Int = 0, // 0 = Chats, 1 = Updates (Status), 2 = Calls
    searchQuery: String = "",
    externalCreateStatusMode: String? = null, // "TEXT" | "PHOTO" | null
    onDismissExternalCreateStatus: () -> Unit = {}
) {
    var createStatusMode by remember { mutableStateOf<String?>(null) } // "TEXT" or "PHOTO"
    val effectiveCreateMode = externalCreateStatusMode ?: createStatusMode

    // Screen 7: Fullscreen Status Viewer
    if (activeStoryViewerItem != null) {
        WhatsAppStatusViewerScreen(
            story = activeStoryViewerItem,
            onClose = onCloseStory
        )
        return
    }

    // Screen 8: Create Status (Text)
    if (effectiveCreateMode == "TEXT") {
        CreateTextStatusScreen(
            onClose = {
                createStatusMode = null
                onDismissExternalCreateStatus()
            },
            onPublish = { text, bgHex ->
                onPublishStory(text, "color://$bgHex")
                createStatusMode = null
                onDismissExternalCreateStatus()
            }
        )
        return
    }

    // Screen 9: Create Status (Photo)
    if (effectiveCreateMode == "PHOTO") {
        CreatePhotoStatusScreen(
            onClose = {
                createStatusMode = null
                onDismissExternalCreateStatus()
            },
            onPublish = { caption, uriStr ->
                onPublishStory(caption, uriStr)
                createStatusMode = null
                onDismissExternalCreateStatus()
            }
        )
        return
    }

    when (activeSubTab) {
        0 -> WhatsAppChatsListScreen(
            language = language,
            contacts = contacts,
            searchQuery = searchQuery,
            onSelectContactForChat = onSelectContactForChat,
            onNavigateToDirectory = onNavigateToDirectory,
            onTogglePinContact = onTogglePinContact
        )
        1 -> WhatsAppUpdatesStatusScreen(
            language = language,
            stories = stories,
            communities = communities,
            onOpenStory = onOpenStory,
            onOpenTextStatusCreator = { createStatusMode = "TEXT" },
            onOpenPhotoStatusCreator = { createStatusMode = "PHOTO" },
            onExploreChannels = onNavigateToDirectory
        )
        else -> WhatsAppCallsScreen(
            language = language,
            contacts = contacts,
            callLogs = callLogs,
            onStartCall = onStartCallFromHome,
            onNavigateToDirectory = onNavigateToDirectory
        )
    }
}

// ============================================================================
// SCREEN 1: CHATS (MAIN PAGE) — Matches WhatsApp_UI_Reference_1 #1 & Ref_2 #1
// ============================================================================
@Composable
fun WhatsAppChatsListScreen(
    language: AppLanguage,
    contacts: List<ResolvedContactProfile>,
    searchQuery: String = "",
    onSelectContactForChat: (String) -> Unit,
    onNavigateToDirectory: () -> Unit,
    onTogglePinContact: (String) -> Unit
) {
    var selectedFilter by remember { mutableIntStateOf(0) } // 0: All, 1: Unread, 2: Groups
    val filteredContacts = remember(contacts, searchQuery, selectedFilter) {
        val base = contacts.filter { !it.isArchived }.sortedByDescending { it.isPinned }
        val byChip = when (selectedFilter) {
            1 -> base.filter { it.unreadCount > 0 }
            2 -> base.filter { it.isGroup }
            else -> base
        }
        if (searchQuery.isBlank()) byChip else byChip.filter {
            it.effectiveDisplayName.contains(searchQuery, ignoreCase = true) ||
                it.username.contains(searchQuery, ignoreCase = true) ||
                it.roleTag.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackgroundWhite)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_feed_list"),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Subtle WhatsApp Filter Pills (All | Unread | Groups | + Add User)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val chips = listOf(
                        "All",
                        "Unread",
                        "Groups"
                    )
                    chips.forEachIndexed { idx, label ->
                        val selected = selectedFilter == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (selected) Color(0xFFD9FDD3) else LightSurfaceSecondary)
                                .clickable { selectedFilter = idx }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) WhatsAppDarkGreen else LightTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background( Color(0xFFE7FCE8))
                            .clickable { onNavigateToDirectory() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("home_manage_peers_button")
                    ) {
                        Text(
                            text = language.shortTr("+ ইউজার অ্যাড", "+ Add User"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WhatsAppHeaderGreen
                        )
                    }
                }
            }

            if (filteredContacts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp, vertical = 56.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE7FCE8)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                tint = WhatsAppHeaderGreen,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Text(
                            text = language.tr("চ্যাট শুরু করুন (Start Messaging)", "Start a Conversation"),
                            style = MaterialTheme.typography.titleMedium,
                            color = LightTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = language.tr(
                                "কোনো ডেমো প্রোফাইল নেই। নিচের সবুজ বাটনে বা '+ ইউজার অ্যাড' বাটনে ট্যাপ করে আসল ইউজার বা গ্রুপ যুক্ত করুন।",
                                "No fake or demo chats. Tap the green button below to add a real contact or create a group."
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LightTextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        NeoTactilePillButton(
                            text = language.shortTr("+ নতুন চ্যাট / ইউজার অ্যাড", "+ New Chat / Add Contact"),
                            icon = Icons.Default.PersonAdd,
                            onClick = onNavigateToDirectory,
                            testTag = "home_create_group_button"
                        )
                    }
                }
            } else {
                items(filteredContacts, key = { it.userId }) { contact ->
                    WhatsAppChatListRow(
                        contact = contact,
                        onClick = { onSelectContactForChat(contact.userId) },
                        onLongPinClick = { onTogglePinContact(contact.userId) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 82.dp, end = 16.dp),
                        thickness = 0.6.dp,
                        color = LightDividerGray
                    )
                }
            }
        }

        // Bottom-Right Green Floating Action Button (Matches Reference 1 #1 & Reference 2 #1)
        FloatingActionButton(
            onClick = onNavigateToDirectory,
            containerColor = WhatsAppAccentGreen,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(56.dp)
                .testTag("chats_fab_new_chat")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Chat,
                contentDescription = "New Chat",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun WhatsAppChatListRow(
    contact: ResolvedContactProfile,
    onClick: () -> Unit,
    onLongPinClick: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val timeText = remember(contact.lastSeenTimestamp) {
        if (contact.lastSeenTimestamp != null && contact.lastSeenTimestamp > 0L) {
            timeFormat.format(Date(contact.lastSeenTimestamp))
        } else {
            "Today"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightBackgroundWhite)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("chat_list_row_${contact.userId}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WhatsAppAvatarCircle(
            name = contact.effectiveDisplayName,
            isGroup = contact.isGroup,
            size = 52.dp,
            showOnlineDot = contact.isOnline && !contact.isGroup
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = contact.effectiveDisplayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LightTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = timeText,
                    fontSize = 12.sp,
                    color = if (contact.unreadCount > 0) WhatsAppAccentGreen else LightTextMuted,
                    fontWeight = if (contact.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (contact.isTyping) {
                        Text(
                            text = "typing...",
                            fontSize = 14.sp,
                            color = WhatsAppAccentGreen,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Read",
                            tint = WhatsAppReadBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = contact.roleTag.ifBlank {
                                if (contact.isGroup) "Group conversation" else "Tap to open chat"
                            },
                            fontSize = 14.sp,
                            color = LightTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (contact.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = LightTextMuted,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onLongPinClick() }
                        )
                    }
                    if (contact.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(WhatsAppAccentGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = contact.unreadCount.toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// SCREEN 6: UPDATES / STATUS (MAIN PAGE) — Matches Ref_1 #6 & Ref_2 #2
// ============================================================================
@Composable
fun WhatsAppUpdatesStatusScreen(
    language: AppLanguage,
    stories: List<StoryItem>,
    communities: List<CommunityModel> = emptyList(),
    onOpenStory: (StoryItem) -> Unit,
    onOpenTextStatusCreator: () -> Unit,
    onOpenPhotoStatusCreator: () -> Unit,
    onExploreChannels: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackgroundWhite)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("updates_status_list"),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                Text(
                    text = "Status",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightTextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                )
            }

            // My Status Row (with green + badge)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPhotoStatusCreator() }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("my_status_add_row"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(54.dp)) {
                        WhatsAppAvatarCircle(
                            name = "My Status",
                            size = 52.dp
                        )
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(Color.White)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(WhatsAppAccentGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Status",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "My status",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LightTextPrimary
                        )
                        Text(
                            text = "Tap to add status update",
                            fontSize = 14.sp,
                            color = LightTextSecondary
                        )
                    }
                }
            }

            // Horizontal Status Avatars Strip (Matches Reference 2 Updates screen)
            if (stories.isNotEmpty()) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(stories, key = { "ring_${it.id.ifBlank { it.storyId }}" }) { story ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(64.dp)
                                    .clickable { onOpenStory(story) }
                            ) {
                                WhatsAppAvatarCircle(
                                    name = story.authorName,
                                    size = 56.dp,
                                    hasStatusRing = true
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = story.authorName.split(" ").firstOrNull() ?: "User",
                                    fontSize = 12.sp,
                                    color = LightTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Viewed / Recent Updates Section (Matches Reference 1 #6)
            item {
                Text(
                    text = if (stories.isEmpty()) "Recent updates" else "Viewed updates",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LightTextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            if (stories.isEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenTextStatusCreator() }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = language.tr(
                                "এখনো কোনো স্ট্যাটাস নেই। নিচের ✏️ বা 📷 বাটনে ট্যাপ করে প্রথম স্ট্যাটাস দিন।",
                                "No status updates yet. Tap the pencil or camera icon below to post a status."
                            ),
                            fontSize = 14.sp,
                            color = LightTextSecondary
                        )
                    }
                }
            } else {
                items(stories, key = { it.id.ifBlank { it.storyId } }) { story ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenStory(story) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WhatsAppAvatarCircle(
                            name = story.authorName,
                            size = 52.dp,
                            hasStatusRing = true
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = story.authorName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LightTextPrimary
                            )
                            Text(
                                text = "Today • ${story.caption.take(40)}",
                                fontSize = 14.sp,
                                color = LightTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    thickness = 0.8.dp,
                    color = LightDividerGray
                )
            }

            // Channels Section (Real Firestore Communities)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExploreChannels() }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Channels & Communities",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightTextPrimary
                    )
                    Text(
                        text = "Explore >",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WhatsAppActionGreen
                    )
                }
            }

            if (communities.isEmpty()) {
                item {
                    ChannelRowItem(
                        name = "Create or Join a Community",
                        subtitle = "Tap Explore > to create a real multi-device community channel",
                        timeOrBadge = "Open",
                        isFollowPill = true
                    )
                }
            } else {
                items(communities, key = { "comm_${it.id.ifBlank { it.communityId }}" }) { comm ->
                    ChannelRowItem(
                        name = comm.name,
                        subtitle = comm.announcementText.ifBlank { comm.description },
                        timeOrBadge = "${comm.memberIds.size} members",
                        isFollowPill = false
                    )
                }
            }
        }

        // Dual FAB Stack on Bottom-Right: Pencil (Text Status) + Camera (Photo Status)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmallFloatingActionButton(
                onClick = onOpenTextStatusCreator,
                containerColor = LightSurfaceSecondary,
                contentColor = LightTextPrimary,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("status_fab_text")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Create Text Status",
                    modifier = Modifier.size(20.dp)
                )
            }

            FloatingActionButton(
                onClick = onOpenPhotoStatusCreator,
                containerColor = WhatsAppAccentGreen,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .size(56.dp)
                    .testTag("status_fab_photo")
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Create Photo Status",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ChannelRowItem(
    name: String,
    subtitle: String,
    timeOrBadge: String,
    isFollowPill: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WhatsAppAvatarCircle(name = name, size = 48.dp)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LightTextPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "Verified Channel",
                    tint = WhatsAppAccentGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = LightTextSecondary
            )
        }
        if (isFollowPill) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFD9FDD3))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = timeOrBadge,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WhatsAppDarkGreen
                )
            }
        } else {
            Text(
                text = timeOrBadge,
                fontSize = 12.sp,
                color = LightTextMuted
            )
        }
    }
}

// ============================================================================
// SCREEN 7: STATUS VIEWER — Matches Ref_1 #7 & Ref_2 Status Viewer
// ============================================================================
@Composable
fun WhatsAppStatusViewerScreen(
    story: StoryItem,
    onClose: () -> Unit
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }

    LaunchedEffect(story.id, isPaused) {
        while (progress < 1f) {
            if (!isPaused) {
                progress += 0.02f
                if (progress >= 1f) {
                    onClose()
                    break
                }
            }
            delay(80L)
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(80, easing = LinearEasing),
        label = "statusViewerProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B141A))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPaused = true
                        tryAwaitRelease()
                        isPaused = false
                    }
                )
            }
            .testTag("status_viewer_screen")
    ) {
        // Status Media or Colored Background
        if (story.mediaUrl.startsWith("content://") || story.mediaUrl.startsWith("file://")) {
            AsyncImage(
                model = story.mediaUrl,
                contentDescription = story.caption,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF075E54),
                                Color(0xFF128C7E),
                                Color(0xFF0B2E28)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = story.caption,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }

        // Top Progress Bar + Author Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 12.dp, start = 12.dp, end = 12.dp)
        ) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.35f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                WhatsAppAvatarCircle(name = story.authorName, size = 40.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = story.authorName,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Today, just now",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = Color.White
                    )
                }
            }
        }

        // Bottom Caption + "^ Reply" Swipe Bar (Matches Reference 1 #7 & Reference 2 Status Viewer)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (story.mediaUrl.startsWith("content://") || story.mediaUrl.startsWith("file://")) {
                Text(
                    text = story.caption,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Reply",
                tint = Color.White
            )
            Text(
                text = "Reply",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onClose() }
            )
        }
    }
}

// ============================================================================
// SCREEN 8: CREATE STATUS (TEXT) — Matches WhatsApp_UI_Reference_1 #8
// ============================================================================
@Composable
fun CreateTextStatusScreen(
    onClose: () -> Unit,
    onPublish: (text: String, bgHex: String) -> Unit
) {
    var statusText by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    val bgColor = STATUS_BG_COLORS[selectedColorIndex % STATUS_BG_COLORS.size]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .testTag("create_text_status_screen")
    ) {
        // Top Action Bar: Close (X) on Left; Emoji, Font (T), Palette on Right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { statusText += " 😊" }) {
                    Icon(
                        imageVector = Icons.Default.EmojiEmotions,
                        contentDescription = "Emoji",
                        tint = Color.White
                    )
                }
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = "Font Style",
                        tint = Color.White
                    )
                }
                IconButton(onClick = { selectedColorIndex = (selectedColorIndex + 1) % STATUS_BG_COLORS.size }) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Change Background Color",
                        tint = Color.White
                    )
                }
            }
        }

        // Center "Type a status" Input
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (statusText.isEmpty()) {
                Text(
                    text = "Type a status",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
            BasicTextField(
                value = statusText,
                onValueChange = { statusText = it },
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                ),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("text_status_input")
            )
        }

        // Color Palette Dots Row + Bottom-Right Send FAB (Matches Screen 8 of Reference 1)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                STATUS_BG_COLORS.forEachIndexed { idx, dotColor ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .size(if (selectedColorIndex == idx) 28.dp else 22.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                            .border(2.dp, Color.White, CircleShape)
                            .clickable { selectedColorIndex = idx }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FloatingActionButton(
                    onClick = {
                        if (statusText.isNotBlank()) {
                            onPublish(statusText.trim(), selectedColorIndex.toString())
                        }
                    },
                    containerColor = WhatsAppAccentGreen,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("send_text_status_fab")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Publish Status"
                    )
                }
            }
        }
    }
}

// ============================================================================
// SCREEN 9: CREATE STATUS (PHOTO) — Matches WhatsApp_UI_Reference_1 #9
// ============================================================================
@Composable
fun CreatePhotoStatusScreen(
    onClose: () -> Unit,
    onPublish: (caption: String, mediaUri: String) -> Unit
) {
    var caption by remember { mutableStateOf("") }
    var pickedPhotoUri by remember { mutableStateOf<String?>(null) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedPhotoUri = uri.toString()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B141A))
            .testTag("create_photo_status_screen")
    ) {
        if (pickedPhotoUri != null) {
            AsyncImage(
                model = pickedPhotoUri,
                contentDescription = "Selected Status Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Scenic Beach/Ocean Gradient Preview matching Reference 1 #9
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1D4ED8),
                                Color(0xFF38BDF8),
                                Color(0xFF2DD4BF),
                                Color(0xFFFDE68A)
                            )
                        )
                    )
                    .clickable {
                        pickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Pick Photo",
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap to choose photo from gallery",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Top Photo Editing Toolbar (Close, Crop, Emoji, Text, Draw)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
            Row {
                IconButton(onClick = {
                    pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Icon(imageVector = Icons.Default.Crop, contentDescription = "Crop", tint = Color.White)
                }
                IconButton(onClick = { caption += " 😊" }) {
                    Icon(imageVector = Icons.Default.EmojiEmotions, contentDescription = "Sticker", tint = Color.White)
                }
                IconButton(onClick = {}) {
                    Icon(imageVector = Icons.Default.TextFields, contentDescription = "Text", tint = Color.White)
                }
                IconButton(onClick = {}) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Draw", tint = Color.White)
                }
            }
        }

        // Bottom "Add a caption..." Bar + Green Send FAB (Matches Screen 9 of Reference 1)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .background(Color(0xFF1F2C34))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (caption.isEmpty()) {
                        Text(
                            text = "Add a caption...",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 15.sp
                        )
                    }
                    BasicTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            FloatingActionButton(
                onClick = {
                    onPublish(
                        caption.ifBlank { "Photo Status Update" },
                        pickedPhotoUri ?: "preset://emerald_status"
                    )
                },
                containerColor = WhatsAppAccentGreen,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("send_photo_status_fab")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Photo Status",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ============================================================================
// SCREENS 13 & 16: CALLS (MAIN & CALL HISTORY) — Matches Ref_1 #13/#16 & Ref_2
// ============================================================================
@Composable
fun WhatsAppCallsScreen(
    language: AppLanguage,
    contacts: List<ResolvedContactProfile>,
    callLogs: List<CallLogEntry>,
    onStartCall: (peerId: String, peerName: String, callType: String) -> Unit,
    onNavigateToDirectory: () -> Unit
) {
    var selectedCallFilter by remember { mutableIntStateOf(0) } // 0: All, 1: Missed, 2: Incoming, 3: Outgoing

    val filteredLogs = remember(callLogs, selectedCallFilter) {
        when (selectedCallFilter) {
            1 -> callLogs.filter { it.status == "REJECTED" || it.status == "MISSED" }
            2 -> callLogs.filter { it.direction == "INCOMING" }
            3 -> callLogs.filter { it.direction == "OUTGOING" }
            else -> callLogs
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackgroundWhite)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("calls_history_list"),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Top Filter Tabs: All | Missed | Incoming | Outgoing (Matches Screen 13 of Reference 1)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf("All", "Missed", "Incoming", "Outgoing")
                    tabs.forEachIndexed { idx, label ->
                        val isSel = selectedCallFilter == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSel) Color(0xFFD9FDD3) else LightSurfaceSecondary)
                                .clickable { selectedCallFilter = idx }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSel) WhatsAppDarkGreen else LightTextSecondary
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Recent",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightTextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            if (filteredLogs.isNotEmpty()) {
                items(filteredLogs, key = { it.callId }) { log ->
                    val isMissed = log.status == "REJECTED" || log.status == "MISSED"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStartCall(log.peerId, log.peerName, log.callType) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WhatsAppAvatarCircle(name = log.peerName, size = 50.dp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = log.peerName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isMissed) TactileCrimson else LightTextPrimary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when {
                                        isMissed -> Icons.Default.CallMissed
                                        log.direction == "INCOMING" -> Icons.Default.CallReceived
                                        else -> Icons.Default.CallMade
                                    },
                                    contentDescription = log.status,
                                    tint = if (isMissed) TactileCrimson else WhatsAppAccentGreen,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${if (isMissed) "Missed" else log.direction.lowercase().replaceFirstChar { it.uppercase() }} • Today",
                                    fontSize = 13.sp,
                                    color = LightTextSecondary
                                )
                            }
                        }
                        IconButton(onClick = { onStartCall(log.peerId, log.peerName, log.callType) }) {
                            Icon(
                                imageVector = if (log.callType == "VIDEO") Icons.Default.Videocam else Icons.Default.Call,
                                contentDescription = "Start Call",
                                tint = WhatsAppHeaderGreen
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 80.dp, end = 16.dp),
                        thickness = 0.6.dp,
                        color = LightDividerGray
                    )
                }
            }

            // Also list connected Contacts so the user can start a voice/video call with 1 tap
            if (contacts.isNotEmpty()) {
                items(contacts, key = { "call_contact_${it.userId}" }) { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStartCall(contact.userId, contact.effectiveDisplayName, "AUDIO") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WhatsAppAvatarCircle(
                            name = contact.effectiveDisplayName,
                            isGroup = contact.isGroup,
                            size = 50.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = contact.effectiveDisplayName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LightTextPrimary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CallMade,
                                    contentDescription = null,
                                    tint = WhatsAppAccentGreen,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Tap to start WebRTC audio or video call",
                                    fontSize = 13.sp,
                                    color = LightTextSecondary
                                )
                            }
                        }
                        IconButton(onClick = { onStartCall(contact.userId, contact.effectiveDisplayName, "AUDIO") }) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Audio Call",
                                tint = WhatsAppHeaderGreen
                            )
                        }
                        IconButton(onClick = { onStartCall(contact.userId, contact.effectiveDisplayName, "VIDEO") }) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Video Call",
                                tint = WhatsAppHeaderGreen
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 80.dp, end = 16.dp),
                        thickness = 0.6.dp,
                        color = LightDividerGray
                    )
                }
            } else if (filteredLogs.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = language.tr("কোনো কল হিস্ট্রি নেই", "No Recent Calls"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LightTextPrimary
                        )
                        Text(
                            text = language.tr(
                                "অডিও বা ভিডিও কল করতে একজন ইউজার অ্যাড করুন।",
                                "Add a real contact to start WebRTC voice and video calls."
                            ),
                            fontSize = 14.sp,
                            color = LightTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Bottom-Right Green Call FAB (Matches Screen 13 of Reference 1 & Calls of Reference 2)
        FloatingActionButton(
            onClick = {
                val firstPeer = contacts.firstOrNull()
                if (firstPeer != null) {
                    onStartCall(firstPeer.userId, firstPeer.effectiveDisplayName, "AUDIO")
                } else {
                    onNavigateToDirectory()
                }
            },
            containerColor = WhatsAppAccentGreen,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(56.dp)
                .testTag("calls_fab_start_call")
        ) {
            Icon(
                imageVector = Icons.Default.AddIcCall,
                contentDescription = "New Call",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
