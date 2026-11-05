package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.CommunityModel
import com.example.data.model.ResolvedContactProfile
import com.example.data.model.UserProfile
import com.example.data.model.generateShortCode
import com.example.ui.components.GlowingCyanRingBox
import com.example.ui.components.NeoTactilePillButton
import com.example.ui.components.NeoTactileQrCodeView
import com.example.ui.components.frostedGlassCard
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlowingCyan
import com.example.ui.theme.TactileAmber
import com.example.ui.theme.TactileCrimson
import com.example.ui.theme.TactileEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private val AVATAR_PRESETS = listOf(
    "preset://avatar_emerald" to ("Emerald Green" to TactileEmerald),
    "preset://avatar_cyan" to ("Teal Pulse" to GlowingCyan),
    "preset://avatar_electric" to ("Deep Green" to ElectricBlue),
    "preset://avatar_amber" to ("Amber Gold" to TactileAmber),
    "preset://avatar_crimson" to ("Crimson Rose" to TactileCrimson),
    "preset://avatar_purple" to ("Royal Violet" to Color(0xFFB388FF))
)

@Composable
fun UsersDirectoryScreen(
    language: AppLanguage,
    currentUserId: String,
    currentUserName: String,
    contacts: List<ResolvedContactProfile>,
    discoverableUsers: List<UserProfile>,
    communities: List<CommunityModel> = emptyList(),
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddContact: (id: String, name: String, avatar: String?, role: String, alias: String?) -> Unit,
    onCreateGroup: (groupName: String, memberIds: List<String>, description: String) -> Unit,
    onCreateCommunity: (name: String, desc: String, announcement: String, groupNames: List<String>, memberIds: List<String>) -> Unit = { _, _, _, _, _ -> },
    onPostAnnouncement: (communityId: String, text: String) -> Unit = { _, _ -> },
    onToggleBlockUser: (String) -> Unit = {},
    onRemoveContact: (id: String) -> Unit,
    onSaveLocalAlias: (id: String, name: String?, avatar: String?) -> Unit,
    onClearLocalAlias: (id: String) -> Unit,
    onSelectContactForChat: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: My Contacts, 1: Search & Add Users, 2: Communities & Groups
    var showAddContactDialog by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showCreateCommunityDialog by remember { mutableStateOf(false) }
    var showMyQrDialog by remember { mutableStateOf(false) }
    var aliasTargetContact by remember { mutableStateOf<ResolvedContactProfile?>(null) }
    var detailTargetContact by remember { mutableStateOf<ResolvedContactProfile?>(null) }

    val myShortCode = remember(currentUserId) { generateShortCode(currentUserId) }

    val directContacts = remember(contacts, searchQuery) {
        val base = contacts.filter { !it.isGroup }
        if (searchQuery.isBlank()) base else base.filter {
            it.effectiveDisplayName.contains(searchQuery, ignoreCase = true) ||
                it.username.contains(searchQuery, ignoreCase = true) ||
                it.phoneNumber.contains(searchQuery, ignoreCase = true) ||
                it.email.contains(searchQuery, ignoreCase = true) ||
                it.userId.contains(searchQuery, ignoreCase = true) ||
                it.shortCode.contains(searchQuery, ignoreCase = true) ||
                it.roleTag.contains(searchQuery, ignoreCase = true)
        }
    }

    val groupContacts = remember(contacts, searchQuery) {
        val base = contacts.filter { it.isGroup }
        if (searchQuery.isBlank()) base else base.filter {
            it.effectiveDisplayName.contains(searchQuery, ignoreCase = true) ||
                it.roleTag.contains(searchQuery, ignoreCase = true)
        }
    }

    // Strict No-Demo Policy: Only real registered users from Firestore `/users` collection
    val realDiscoverableUsers = remember(discoverableUsers, currentUserId, searchQuery) {
        val base = discoverableUsers.filter { it.uid != currentUserId }
        if (searchQuery.isBlank()) {
            base
        } else {
            base.filter { u ->
                u.displayName.contains(searchQuery, ignoreCase = true) ||
                    u.username.contains(searchQuery, ignoreCase = true) ||
                    u.phoneNumber.contains(searchQuery, ignoreCase = true) ||
                    u.email.contains(searchQuery, ignoreCase = true) ||
                    u.uid.contains(searchQuery, ignoreCase = true) ||
                    u.shortCode.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("users_directory_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header Card: My Short ID, QR Code, + Add User, + Group, + Community
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
                                text = language.tr("ইউজার ম্যানেজমেন্ট ও কমিউনিটি", "USERS, GROUPS & COMMUNITIES"),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = language.tr(
                                    "আপনার আইডি: $myShortCode (কোনো ডেমো প্রোফাইল নেই)",
                                    "My ID: $myShortCode • 100% Real Users"
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = GlowingCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF132920))
                                .border(1.dp, GlowingCyan.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                                .clickable { showMyQrDialog = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("open_my_qr_button"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = "My QR Code",
                                tint = GlowingCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = language.shortTr("আমার QR", "My QR"),
                                style = MaterialTheme.typography.labelMedium,
                                color = GlowingCyan
                            )
                        }
                    }

                    // Action Buttons: + Add User | + New Group | + Community
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NeoTactilePillButton(
                            text = language.shortTr("+ ইউজার অ্যাড", "+ Add User"),
                            icon = Icons.Default.PersonAdd,
                            onClick = { showAddContactDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_contact_top_button")
                        )
                        NeoTactilePillButton(
                            text = language.shortTr("+ গ্রুপ তৈরি", "+ Group"),
                            icon = Icons.Default.GroupAdd,
                            accentColor = TactileAmber,
                            onClick = { showCreateGroupDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("create_group_top_button")
                        )
                    }

                    NeoTactilePillButton(
                        text = language.tr("+ নতুন কমিউনিটি তৈরি করুন (Create Community)", "+ Create New Community"),
                        icon = Icons.Default.Forum,
                        accentColor = ElectricBlue,
                        onClick = { showCreateCommunityDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_community_top_button")
                    )

                    // Search Bar by Username, Phone, Email, or Short ID
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = {
                            Text(
                                text = language.shortTr(
                                    "ইউজারনেম (@name), ইমেইল, ফোন বা শর্ট আইডি ($myShortCode) দিয়ে খুঁজুন...",
                                    "Search by @username, email, phone, or Short ID..."
                                ),
                                color = TextMuted
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Users",
                                tint = GlowingCyan
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = TextSecondary
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GlowingCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = Color(0xFF111E19),
                            unfocusedContainerColor = Color(0xFF111E19)
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_peers_input")
                    )

                    // 3-Tab Selector: Contacts | Find Users | Communities & Groups
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TabPill(
                            title = language.shortTr("কন্টাক্ট (${directContacts.size})", "Contacts (${directContacts.size})"),
                            isSelected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tab_my_peers")
                        )
                        TabPill(
                            title = language.shortTr("ইউজার খুঁজুন (${realDiscoverableUsers.size})", "Find (${realDiscoverableUsers.size})"),
                            isSelected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tab_discover_nodes")
                        )
                        TabPill(
                            title = language.shortTr("কমিউনিটি (${communities.size + groupContacts.size})", "Communities"),
                            isSelected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tab_my_groups")
                        )
                    }
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    if (directContacts.isEmpty()) {
                        item {
                            EmptyPeersCard(
                                language = language,
                                titleBn = "আপনার কন্টাক্ট তালিকা খালি (No Contacts Yet)",
                                subtitleBn = "কোনো ফেইক বা ডেমো ইউজার রাখা হয়নি। উপরে '+ ইউজার অ্যাড' বাটনে চাপ দিয়ে ইউজারনেম, ইমেইল, ফোন নম্বর বা শর্ট আইডি দিয়ে আসল ইউজার অ্যাড করুন।",
                                subtitleEn = "Strict No-Demo Mode: Add real registered users by @username, email, phone number, or Short ID.",
                                onAddClick = { showAddContactDialog = true }
                            )
                        }
                    } else {
                        items(directContacts, key = { it.userId }) { peer ->
                            ConnectedPeerCard(
                                language = language,
                                peer = peer,
                                onMessageClick = { onSelectContactForChat(peer.userId) },
                                onEditAliasClick = { aliasTargetContact = peer },
                                onDetailsClick = { detailTargetContact = peer },
                                onToggleBlockClick = { onToggleBlockUser(peer.userId) },
                                onRemoveClick = { onRemoveContact(peer.userId) }
                            )
                        }
                    }
                }

                1 -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = language.tr("রেজিস্টার্ড রিয়েল ইউজার তালিকা", "REGISTERED REAL USERS"),
                                style = MaterialTheme.typography.labelMedium,
                                color = GlowingCyan,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                            Text(
                                text = language.shortTr("+ সরাসরি আইডি দিয়ে অ্যাড", "+ Add by ID"),
                                style = MaterialTheme.typography.labelSmall,
                                color = GlowingCyan,
                                modifier = Modifier.clickable { showAddContactDialog = true }
                            )
                        }
                    }

                    if (realDiscoverableUsers.isEmpty()) {
                        item {
                            EmptyPeersCard(
                                language = language,
                                titleBn = "সার্চে কোনো পাবলিক ইউজার পাওয়া যায়নি",
                                subtitleBn = "অন্যান্য ব্যবহারকারী অ্যাপে সাইন-ইন করলে এখানে দেখা যাবে, অথবা আপনি সরাসরি শর্ট আইডি / ইউজারনেম / ইমেইল দিয়ে অ্যাড করতে পারেন।",
                                subtitleEn = "No other public accounts matched your search yet. Tap below to add by Short ID, Username, Email, or Phone.",
                                onAddClick = { showAddContactDialog = true }
                            )
                        }
                    } else {
                        items(realDiscoverableUsers, key = { it.uid }) { user ->
                            val isAlreadyConnected = contacts.any { it.userId == user.uid }
                            DiscoverableRealUserCard(
                                language = language,
                                user = user,
                                isAlreadyConnected = isAlreadyConnected,
                                onConnectClick = {
                                    onAddContact(
                                        user.uid,
                                        user.displayName.ifBlank { user.username },
                                        user.photoUrl,
                                        user.cleanBio,
                                        null
                                    )
                                },
                                onMessageClick = { onSelectContactForChat(user.uid) }
                            )
                        }
                    }
                }

                else -> {
                    // Communities Section
                    if (communities.isNotEmpty()) {
                        item {
                            Text(
                                text = language.tr("আপনার কমিউনিটিসমূহ (Communities)", "YOUR COMMUNITIES"),
                                style = MaterialTheme.typography.labelLarge,
                                color = GlowingCyan
                            )
                        }
                        items(communities, key = { it.communityId }) { comm ->
                            CommunityItemCard(
                                language = language,
                                community = comm,
                                onBroadcastAnnouncement = { text ->
                                    onPostAnnouncement(comm.communityId, text)
                                }
                            )
                        }
                    }

                    item {
                        Text(
                            text = language.tr("আপনার গ্রুপ চ্যাটসমূহ (Groups)", "YOUR GROUPS"),
                            style = MaterialTheme.typography.labelLarge,
                            color = GlowingCyan
                        )
                    }

                    if (groupContacts.isEmpty()) {
                        item {
                            EmptyPeersCard(
                                language = language,
                                titleBn = "কোনো গ্রুপ বা কমিউনিটি নেই",
                                subtitleBn = "নতুন গ্রুপ বা একাধিক গ্রুপসহ কমিউনিটি তৈরি করুন।",
                                subtitleEn = "Create a new group chat or a multi-group community.",
                                onAddClick = { showCreateGroupDialog = true }
                            )
                        }
                    } else {
                        items(groupContacts, key = { it.userId }) { group ->
                            ConnectedPeerCard(
                                language = language,
                                peer = group,
                                onMessageClick = { onSelectContactForChat(group.userId) },
                                onEditAliasClick = { aliasTargetContact = group },
                                onDetailsClick = { detailTargetContact = group },
                                onToggleBlockClick = {},
                                onRemoveClick = { onRemoveContact(group.userId) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal: My QR Code & Short ID
    if (showMyQrDialog) {
        MyQrCodeModalDialog(
            language = language,
            userName = currentUserName,
            shortCode = myShortCode,
            fullUserId = currentUserId,
            onDismiss = { showMyQrDialog = false }
        )
    }

    // Modal: Add New Contact by Username / Email / Phone / Short ID / UID
    if (showAddContactDialog) {
        AddNewContactModalDialog(
            language = language,
            onDismiss = { showAddContactDialog = false },
            onConfirmAdd = { id, name, avatar, role, alias ->
                showAddContactDialog = false
                onAddContact(id, name, avatar, role, alias)
            }
        )
    }

    // Modal: Create New Group Chat
    if (showCreateGroupDialog) {
        CreateGroupModalDialog(
            language = language,
            availableContacts = contacts.filter { !it.isGroup },
            onDismiss = { showCreateGroupDialog = false },
            onConfirmCreate = { groupName, memberIds, desc ->
                showCreateGroupDialog = false
                onCreateGroup(groupName, memberIds, desc)
            }
        )
    }

    // Modal: Create New Community
    if (showCreateCommunityDialog) {
        CreateCommunityModalDialog(
            language = language,
            availableContacts = contacts.filter { !it.isGroup },
            onDismiss = { showCreateCommunityDialog = false },
            onConfirmCreate = { name, desc, announcement, groupNames, memberIds ->
                showCreateCommunityDialog = false
                onCreateCommunity(name, desc, announcement, groupNames, memberIds)
            }
        )
    }

    // Modal: Local Room Alias Override
    if (aliasTargetContact != null) {
        LocalAliasModalDialog(
            language = language,
            contact = aliasTargetContact!!,
            onDismiss = { aliasTargetContact = null },
            onSave = { name, avatar ->
                onSaveLocalAlias(aliasTargetContact!!.userId, name, avatar)
                aliasTargetContact = null
            },
            onClear = {
                onClearLocalAlias(aliasTargetContact!!.userId)
                aliasTargetContact = null
            }
        )
    }

    // Modal: Peer Details & QR Card
    if (detailTargetContact != null) {
        PeerDetailsModalDialog(
            language = language,
            contact = detailTargetContact!!,
            onDismiss = { detailTargetContact = null },
            onMessageNow = {
                val uid = detailTargetContact!!.userId
                detailTargetContact = null
                onSelectContactForChat(uid)
            },
            onEditAlias = {
                val c = detailTargetContact!!
                detailTargetContact = null
                aliasTargetContact = c
            }
        )
    }
}

@Composable
private fun CommunityItemCard(
    language: AppLanguage,
    community: CommunityModel,
    onBroadcastAnnouncement: (String) -> Unit
) {
    var announcementInput by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlassCard(cornerRadius = 22.dp, accentGlow = GlowingCyan)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Forum,
                contentDescription = null,
                tint = GlowingCyan,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = community.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "${community.description} • ${community.groupIds.size} Groups",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF10241C))
                .border(1.dp, GlowingCyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = null,
                    tint = GlowingCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = community.announcementText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = announcementInput,
                onValueChange = { announcementInput = it },
                placeholder = {
                    Text(
                        language.shortTr("নতুন কমিউনিটি ঘোষণা লিখুন...", "New community announcement..."),
                        color = TextMuted
                    )
                },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            NeoTactilePillButton(
                text = language.shortTr("ঘোষণা", "Post"),
                onClick = {
                    if (announcementInput.isNotBlank()) {
                        onBroadcastAnnouncement(announcementInput.trim())
                        announcementInput = ""
                    }
                }
            )
        }
    }
}

@Composable
private fun TabPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (isSelected) ElectricBlue else Color(0xFF15251E))
            .border(
                width = 1.dp,
                color = if (isSelected) GlowingCyan else Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) Color.White else TextSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun ConnectedPeerCard(
    language: AppLanguage,
    peer: ResolvedContactProfile,
    onMessageClick: () -> Unit,
    onEditAliasClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onToggleBlockClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlassCard(
                cornerRadius = 22.dp,
                accentGlow = if (peer.isBlocked) TactileCrimson else if (peer.isGroup) TactileAmber else ElectricBlue
            )
            .padding(16.dp)
            .testTag("peer_card_${peer.userId}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlowingCyanRingBox(
                    size = 52.dp,
                    isActive = peer.isOnline && !peer.isBlocked,
                    amplitude = if (peer.isOnline) 0.55f else 0.1f,
                    ringColor = if (peer.isGroup) TactileAmber else GlowingCyan
                ) {
                    Icon(
                        imageVector = if (peer.isGroup) Icons.Default.Groups else Icons.Default.Person,
                        contentDescription = peer.effectiveDisplayName,
                        tint = if (peer.isGroup) TactileAmber else GlowingCyan,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = peer.effectiveDisplayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        if (peer.isBlocked) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(TactileCrimson.copy(alpha = 0.22f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = language.shortTr("ব্লকড", "BLOCKED"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TactileCrimson
                                )
                            }
                        } else if (peer.hasLocalOverride) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(TactileAmber.copy(alpha = 0.18f))
                                    .border(1.dp, TactileAmber.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = language.shortTr("লোকাল নাম", "ALIAS"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TactileAmber
                                )
                            }
                        }
                    }

                    Text(
                        text = buildString {
                            if (peer.username.isNotBlank()) append("@${peer.username} • ")
                            append("ID: ${peer.shortCode} • ${peer.roleTag}")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (peer.isOnline && !peer.isBlocked) TactileEmerald else Color.Gray)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ElectricBlue)
                        .clickable { onMessageClick() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = language.shortTr("মেসেজ পাঠান", "Message"),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF15251E))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                        .clickable { onEditAliasClick() }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Local Alias",
                            tint = GlowingCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = language.shortTr("নাম", "Alias"),
                            style = MaterialTheme.typography.labelSmall,
                            color = GlowingCyan
                        )
                    }
                }

                if (!peer.isGroup) {
                    IconButton(
                        onClick = onToggleBlockClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF23181C))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Block or Unblock User",
                            tint = if (peer.isBlocked) TactileCrimson else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDetailsClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF15251E))
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Details & QR",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onRemoveClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF26191E))
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Remove Contact",
                        tint = TactileCrimson,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoverableRealUserCard(
    language: AppLanguage,
    user: UserProfile,
    isAlreadyConnected: Boolean,
    onConnectClick: () -> Unit,
    onMessageClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlassCard(cornerRadius = 22.dp)
            .padding(16.dp)
            .testTag("discover_node_${user.uid}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlowingCyanRingBox(
                size = 50.dp,
                isActive = user.isOnline,
                amplitude = if (user.isOnline) 0.5f else 0.1f,
                ringColor = if (user.isOnline) GlowingCyan else Color.Gray.copy(alpha = 0.3f)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = user.displayName,
                    tint = GlowingCyan,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName.ifBlank { user.username },
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "@${user.username} • Short ID: ${user.shortCode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GlowingCyan
                )
                Text(
                    text = user.cleanBio,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            if (isAlreadyConnected) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF142426))
                        .border(1.dp, TactileEmerald.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable { onMessageClick() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = TactileEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = language.shortTr("যুক্ত", "Added"),
                            style = MaterialTheme.typography.labelSmall,
                            color = TactileEmerald
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(ElectricBlue)
                        .clickable { onConnectClick() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = language.shortTr("+ অ্যাড করুন", "+ Add"),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPeersCard(
    language: AppLanguage,
    titleBn: String = "তালিকা খালি রয়েছে",
    subtitleBn: String = "ইউজারনেম, ইমেইল, ফোন নম্বর বা শর্ট আইডি দিয়ে নতুন ইউজার যুক্ত করুন।",
    subtitleEn: String = "Add users by username, email, phone number, or Short ID.",
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlassCard(cornerRadius = 24.dp)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.PersonAdd,
            contentDescription = null,
            tint = GlowingCyan,
            modifier = Modifier.size(44.dp)
        )
        Text(
            text = language.tr(titleBn, "NO CONTACTS YET"),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Text(
            text = language.tr(subtitleBn, subtitleEn),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        NeoTactilePillButton(
            text = language.shortTr("+ ইউজার অ্যাড করুন", "+ Add Real User"),
            icon = Icons.Default.Add,
            onClick = onAddClick
        )
    }
}

@Composable
private fun MyQrCodeModalDialog(
    language: AppLanguage,
    userName: String,
    shortCode: String,
    fullUserId: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF102019),
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = language.tr("আমার QR কোড ও শর্ট আইডি", "MY QR CODE & SHORT ID"),
                style = MaterialTheme.typography.titleMedium,
                color = GlowingCyan
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NeoTactileQrCodeView(
                    codeData = "$shortCode|$fullUserId",
                    size = 180.dp
                )
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0A1611))
                        .border(1.dp, GlowingCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "SHORT ID: $shortCode",
                        style = MaterialTheme.typography.titleMedium,
                        color = GlowingCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = language.tr(
                        "অন্য ব্যবহারকারীরা এই শর্ট আইডি বা আপনার ইউজারনেম দিয়ে আপনাকে সরাসরি অ্যাড করতে পারবেন।",
                        "Other users can enter this Short ID or your @username to add you."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            NeoTactilePillButton(
                text = language.shortTr("বন্ধ করুন", "Done"),
                onClick = onDismiss,
                accentColor = ElectricBlue
            )
        }
    )
}

@Composable
private fun AddNewContactModalDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onConfirmAdd: (id: String, name: String, avatar: String, role: String, alias: String?) -> Unit
) {
    var nodeId by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var roleTag by remember { mutableStateOf("ভেরিফাইড কন্টাক্ট (Verified Contact)") }
    var localAlias by remember { mutableStateOf("") }
    var selectedAvatarPreset by remember { mutableStateOf(AVATAR_PRESETS.first().first) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF102019),
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = language.tr("নতুন ইউজার অ্যাড করুন", "ADD USER TO CONTACTS"),
                style = MaterialTheme.typography.titleMedium,
                color = GlowingCyan
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = nodeId,
                    onValueChange = { nodeId = it },
                    label = {
                        Text(
                            language.shortTr(
                                "ইউজারনেম (@username), ইমেইল, ফোন বা শর্ট আইডি",
                                "Username, Email, Phone, or Short ID"
                            )
                        )
                    },
                    placeholder = { Text("e.g. @rahim, user@gmail.com, +8801..., or NT-4821", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlowingCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_contact_node_id_input")
                )

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(language.shortTr("ইউজার নাম (Display Name)", "Display Name")) },
                    placeholder = { Text("e.g. Rahim Uddin", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlowingCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_contact_name_input")
                )

                OutlinedTextField(
                    value = roleTag,
                    onValueChange = { roleTag = it },
                    label = { Text(language.shortTr("পরিচয় বা নোট", "Role / Note")) },
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
                    value = localAlias,
                    onValueChange = { localAlias = it },
                    label = { Text(language.shortTr("গোপন লোকাল নাম (ঐচ্ছিক)", "Private Local Alias (Optional)")) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlowingCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AVATAR_PRESETS) { (presetUri, meta) ->
                        val (_, color) = meta
                        val isSelected = selectedAvatarPreset == presetUri
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedAvatarPreset = presetUri }
                        )
                    }
                }
            }
        },
        confirmButton = {
            NeoTactilePillButton(
                text = language.shortTr("অ্যাড করুন", "Add User"),
                onClick = {
                    val effectiveName = displayName.trim().ifBlank { nodeId.trim().removePrefix("@") }
                    onConfirmAdd(nodeId, effectiveName, selectedAvatarPreset, roleTag, localAlias.ifBlank { null })
                },
                accentColor = ElectricBlue,
                testTag = "confirm_add_contact_button"
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
private fun CreateGroupModalDialog(
    language: AppLanguage,
    availableContacts: List<ResolvedContactProfile>,
    onDismiss: () -> Unit,
    onConfirmCreate: (groupName: String, memberIds: List<String>, description: String) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val selectedMembers = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF102019),
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = language.tr("নতুন গ্রুপ চ্যাট তৈরি করুন", "CREATE NEW GROUP CHAT"),
                style = MaterialTheme.typography.titleMedium,
                color = TactileAmber
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text(language.shortTr("গ্রুপের নাম", "Group Name")) },
                    placeholder = { Text("e.g. Project Team", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TactileAmber,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_group_name_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(language.shortTr("গ্রুপের বিবরণ", "Group Description")) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TactileAmber,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = language.tr("সদস্য নির্বাচন করুন:", "Select Members:"),
                    style = MaterialTheme.typography.labelMedium,
                    color = GlowingCyan
                )

                if (availableContacts.isEmpty()) {
                    Text(
                        text = language.shortTr(
                            "আপনি নিজে অ্যাডমিন হিসেবে গ্রুপ তৈরি করতে পারেন এবং পরে সদস্য যুক্ত করতে পারবেন।",
                            "You can create the group now as admin and add members anytime."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        availableContacts.take(6).forEach { contact ->
                            val isChecked = selectedMembers.contains(contact.userId)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF0E1B15))
                                    .clickable {
                                        if (isChecked) selectedMembers.remove(contact.userId)
                                        else selectedMembers.add(contact.userId)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedMembers.add(contact.userId)
                                        else selectedMembers.remove(contact.userId)
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = TactileAmber)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = contact.effectiveDisplayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            NeoTactilePillButton(
                text = language.shortTr("গ্রুপ তৈরি করুন", "Create Group"),
                onClick = { onConfirmCreate(groupName, selectedMembers.toList(), description) },
                accentColor = TactileAmber,
                testTag = "confirm_create_group_button"
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
private fun CreateCommunityModalDialog(
    language: AppLanguage,
    availableContacts: List<ResolvedContactProfile>,
    onDismiss: () -> Unit,
    onConfirmCreate: (name: String, desc: String, announcement: String, groupNames: List<String>, memberIds: List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var announcement by remember { mutableStateOf("স্বাগতম! এটি কমিউনিটির অফিশিয়াল ঘোষণা চ্যানেল।") }
    var groupsCsv by remember { mutableStateOf("General Discussion, Announcements") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF102019),
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = language.tr("নতুন কমিউনিটি তৈরি করুন", "CREATE NEW COMMUNITY"),
                style = MaterialTheme.typography.titleMedium,
                color = GlowingCyan
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(language.shortTr("কমিউনিটির নাম", "Community Name")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(language.shortTr("কমিউনিটির বিবরণ", "Community Description")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = announcement,
                    onValueChange = { announcement = it },
                    label = { Text(language.shortTr("প্রথম ঘোষণা (Announcement)", "Welcome Announcement")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = groupsCsv,
                    onValueChange = { groupsCsv = it },
                    label = { Text(language.shortTr("কমিউনিটির ভেতরের গ্রুপসমূহ (কমা দিয়ে)", "Groups inside Community (comma-separated)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            NeoTactilePillButton(
                text = language.shortTr("কমিউনিটি তৈরি করুন", "Create Community"),
                onClick = {
                    val parsedGroups = groupsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    onConfirmCreate(
                        name,
                        description,
                        announcement,
                        parsedGroups,
                        availableContacts.map { it.userId }
                    )
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
private fun LocalAliasModalDialog(
    language: AppLanguage,
    contact: ResolvedContactProfile,
    onDismiss: () -> Unit,
    onSave: (String?, String?) -> Unit,
    onClear: () -> Unit
) {
    var aliasInput by remember(contact) { mutableStateOf(contact.overriddenDisplayName ?: "") }
    var selectedAvatar by remember(contact) {
        mutableStateOf(contact.overriddenAvatarUri ?: contact.originalAvatarUri ?: AVATAR_PRESETS.first().first)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF102019),
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = language.tr("গোপন লোকাল নাম (Room Local Alias)", "LOCAL DEVICE ALIAS"),
                style = MaterialTheme.typography.titleMedium,
                color = GlowingCyan
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = language.tr(
                        "আসল নাম: ${contact.originalDisplayName}\nএই লোকাল নাম শুধুমাত্র আপনার ফোনে (Room DB) সেভ থাকবে।",
                        "Original: ${contact.originalDisplayName}\nSaved strictly on your device in Room SQLite."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = aliasInput,
                    onValueChange = { aliasInput = it },
                    label = { Text(language.shortTr("কাস্টম লোকাল নাম", "Custom Local Alias")) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlowingCyan,
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
                text = language.shortTr("সেভ করুন", "Save Alias"),
                onClick = { onSave(aliasInput.ifBlank { null }, selectedAvatar) },
                accentColor = ElectricBlue
            )
        },
        dismissButton = {
            NeoTactilePillButton(
                text = language.shortTr("রিসেট", "Reset"),
                onClick = onClear,
                isActive = false
            )
        }
    )
}

@Composable
private fun PeerDetailsModalDialog(
    language: AppLanguage,
    contact: ResolvedContactProfile,
    onDismiss: () -> Unit,
    onMessageNow: () -> Unit,
    onEditAlias: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF102019),
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = contact.effectiveDisplayName,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NeoTactileQrCodeView(
                    codeData = "${contact.shortCode}|${contact.userId}",
                    size = 150.dp
                )
                Text(
                    text = "Short ID: ${contact.shortCode}",
                    style = MaterialTheme.typography.titleMedium,
                    color = GlowingCyan,
                    fontWeight = FontWeight.Bold
                )
                if (contact.username.isNotBlank()) {
                    Text(
                        text = "Username: @${contact.username}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
                Text(
                    text = contact.roleTag,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            NeoTactilePillButton(
                text = language.shortTr("মেসেজ পাঠান", "Message"),
                onClick = onMessageNow,
                accentColor = ElectricBlue
            )
        },
        dismissButton = {
            NeoTactilePillButton(
                text = language.shortTr("লোকাল নাম", "Edit Alias"),
                onClick = onEditAlias,
                isActive = false
            )
        }
    )
}
