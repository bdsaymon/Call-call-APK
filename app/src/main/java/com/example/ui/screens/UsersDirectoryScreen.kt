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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.CommunityModel
import com.example.data.model.ResolvedContactProfile
import com.example.data.model.UserProfile
import com.example.data.model.generateShortCode
import com.example.ui.components.NeoTactilePillButton
import com.example.ui.components.NeoTactileQrCodeView
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

/**
 * Implements:
 * - Screen 10: Communities (Main Page)
 * - Screen 11: Community Info
 * - Screen 12: Create Community
 * - Plus Real User Directory (Add by @username, email, phone, Short ID, QR Code, or New Group)
 */
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
    var activeSubScreen by remember { mutableStateOf("MAIN") } // "MAIN", "CREATE_COMMUNITY", "COMMUNITY_INFO"
    var selectedCommunity by remember { mutableStateOf<CommunityModel?>(null) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showMyQrDialog by remember { mutableStateOf(false) }

    val myShortCode = remember(currentUserId) { generateShortCode(currentUserId) }

    // Screen 12: Create Community Full Screen
    if (activeSubScreen == "CREATE_COMMUNITY") {
        BackHandler { activeSubScreen = "MAIN" }
        WhatsAppCreateCommunityScreen(
            onBack = { activeSubScreen = "MAIN" },
            onCreate = { name, desc ->
                onCreateCommunity(
                    name,
                    desc,
                    "Welcome to $name!",
                    listOf("$name General", "$name Updates"),
                    contacts.map { it.userId }
                )
                activeSubScreen = "MAIN"
            }
        )
        return
    }

    // Screen 11: Community Info Full Screen
    val commInfo = selectedCommunity
    if (activeSubScreen == "COMMUNITY_INFO" && commInfo != null) {
        BackHandler { activeSubScreen = "MAIN" }
        WhatsAppCommunityInfoScreen(
            community = commInfo,
            contacts = contacts,
            onBack = { activeSubScreen = "MAIN" },
            onSelectGroupChat = onSelectContactForChat,
            onAddGroup = { showCreateGroupDialog = true },
            onAddMembers = { showAddContactDialog = true },
            onSendAnnouncement = { text -> onPostAnnouncement(commInfo.communityId, text) }
        )
        return
    }

    // Screen 10: Communities (Main) + Contacts & User Addition Hub
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LightSurfaceSecondary)
            .testTag("users_directory_screen"),
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Communities Hero Banner (Matches Reference 2 Communities & Reference 1 #10/#12)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LightBackgroundWhite)
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(WhatsAppAccentGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = "Communities",
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Communities",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightTextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Bring together related groups for easier management.",
                    fontSize = 14.sp,
                    color = LightTextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(WhatsAppActionGreen)
                        .clickable { activeSubScreen = "CREATE_COMMUNITY" }
                        .testTag("create_community_top_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Create community",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 2. "+ Add community", "+ Add Contact", "+ New Group", and "My QR Code" Rows
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LightBackgroundWhite)
            ) {
                CommunityActionRow(
                    icon = Icons.Default.Add,
                    iconBg = WhatsAppActionGreen,
                    title = "Add community",
                    subtitle = "Create a new multi-group community",
                    onClick = { activeSubScreen = "CREATE_COMMUNITY" }
                )
                HorizontalDivider(color = LightDividerGray, thickness = 0.6.dp)
                CommunityActionRow(
                    icon = Icons.Default.PersonAdd,
                    iconBg = WhatsAppHeaderGreen,
                    title = language.tr("নতুন কন্টাক্ট / ইউজার অ্যাড করুন (Add Contact)", "Add New Contact / User"),
                    subtitle = "Add by @username, email, phone, or Short ID ($myShortCode)",
                    onClick = { showAddContactDialog = true },
                    testTag = "add_contact_top_button"
                )
                HorizontalDivider(color = LightDividerGray, thickness = 0.6.dp)
                CommunityActionRow(
                    icon = Icons.Default.GroupAdd,
                    iconBg = WhatsAppHeaderGreen,
                    title = language.tr("নতুন গ্রুপ তৈরি করুন (New Group)", "New Group Chat"),
                    subtitle = "Create a group conversation with your contacts",
                    onClick = { showCreateGroupDialog = true },
                    testTag = "create_group_top_button"
                )
                HorizontalDivider(color = LightDividerGray, thickness = 0.6.dp)
                CommunityActionRow(
                    icon = Icons.Default.QrCode2,
                    iconBg = WhatsAppActionGreen,
                    title = "My QR Code ($myShortCode)",
                    subtitle = "Share your QR code or Short ID with friends",
                    onClick = { showMyQrDialog = true },
                    testTag = "open_my_qr_button"
                )
            }
        }

        // 3. Your Communities List (Matches Screen 10 of Reference 1 & Reference 2)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LightBackgroundWhite)
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Your communities",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LightTextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (communities.isEmpty()) {
                    Text(
                        text = language.tr(
                            "আপনার এখনো কোনো কমিউনিটি নেই। উপরে 'Create community' বাটনে ট্যাপ করে তৈরি করুন।",
                            "No communities created yet. Tap 'Create community' above to start."
                        ),
                        fontSize = 14.sp,
                        color = LightTextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                } else {
                    communities.forEach { comm ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCommunity = comm
                                    activeSubScreen = "COMMUNITY_INFO"
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(WhatsAppAccentGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = comm.name,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = comm.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LightTextPrimary
                                )
                                Text(
                                    text = "${comm.groupIds.size.coerceAtLeast(1)} groups",
                                    fontSize = 13.sp,
                                    color = LightTextSecondary
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = LightTextMuted
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 78.dp),
                            color = LightDividerGray,
                            thickness = 0.6.dp
                        )
                    }
                }
            }
        }

        // 4. Saved Contacts & Groups Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LightBackgroundWhite)
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Contacts & Groups (${contacts.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LightTextSecondary
                    )
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search by username, phone, email, or ID...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = LightTextSecondary)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhatsAppActionGreen,
                        unfocusedBorderColor = LightDividerGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("directory_search_input")
                )

                contacts.forEach { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectContactForChat(contact.userId) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WhatsAppAvatarCircle(
                            name = contact.effectiveDisplayName,
                            isGroup = contact.isGroup,
                            size = 48.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = contact.effectiveDisplayName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LightTextPrimary
                            )
                            Text(
                                text = contact.roleTag.ifBlank { "Hey there! I am using WhatsApp." },
                                fontSize = 13.sp,
                                color = LightTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { onSelectContactForChat(contact.userId) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "Open Chat",
                                tint = WhatsAppHeaderGreen
                            )
                        }
                        IconButton(onClick = { onToggleBlockUser(contact.userId) }) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = "Block / Unblock",
                                tint = if (contact.isBlocked) TactileCrimson else LightTextMuted
                            )
                        }
                        IconButton(onClick = { onRemoveContact(contact.userId) }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Remove Contact",
                                tint = LightTextMuted
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 78.dp),
                        color = LightDividerGray,
                        thickness = 0.6.dp
                    )
                }

                // Real Registered Users Discovered from Firestore `/users`
                val discoverableFiltered = discoverableUsers.filter { it.uid != currentUserId }
                if (discoverableFiltered.isNotEmpty()) {
                    Text(
                        text = "Discoverable Registered Users",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WhatsAppHeaderGreen,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    discoverableFiltered.forEach { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WhatsAppAvatarCircle(name = user.displayName, size = 44.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.displayName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LightTextPrimary
                                )
                                Text(
                                    text = "@${user.username} • ${user.shortCode}",
                                    fontSize = 12.sp,
                                    color = LightTextSecondary
                                )
                            }
                            NeoTactilePillButton(
                                text = "+ Add",
                                onClick = {
                                    onAddContact(user.uid, user.displayName, user.photoUrl, user.cleanBio, null)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add New Contact Dialog
    if (showAddContactDialog) {
        var userIdentifier by remember { mutableStateOf("") }
        var contactDisplayName by remember { mutableStateOf("") }
        var contactPhoneOrBio by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            title = { Text("Add New Contact") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = userIdentifier,
                        onValueChange = { userIdentifier = it },
                        label = { Text("Username, Email, Phone, or Short ID") },
                        placeholder = { Text("@sadia or NT-XXXX or +8801...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_user_id_input")
                    )
                    OutlinedTextField(
                        value = contactDisplayName,
                        onValueChange = { contactDisplayName = it },
                        label = { Text("Display Name") },
                        placeholder = { Text("Sadia / Rafi Ahmed") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_display_name_input")
                    )
                    OutlinedTextField(
                        value = contactPhoneOrBio,
                        onValueChange = { contactPhoneOrBio = it },
                        label = { Text("About / Status (Optional)") },
                        placeholder = { Text("Hey there! I am using WhatsApp.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (userIdentifier.isNotBlank()) {
                            onAddContact(
                                userIdentifier.trim(),
                                contactDisplayName.trim().ifBlank { userIdentifier.trim() },
                                null,
                                contactPhoneOrBio.trim().ifBlank { "Hey there! I am using WhatsApp." },
                                null
                            )
                            showAddContactDialog = false
                        }
                    },
                    modifier = Modifier.testTag("dialog_confirm_add_contact")
                ) {
                    Text("Save Contact", color = WhatsAppActionGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddContactDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Create New Group Dialog
    if (showCreateGroupDialog) {
        var groupName by remember { mutableStateOf("") }
        var groupDesc by remember { mutableStateOf("") }
        val selectedMembers = remember { mutableStateListOf<String>() }
        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text("New Group") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group Name") },
                        placeholder = { Text("Family Group / Work Team") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_group_name_input")
                    )
                    OutlinedTextField(
                        value = groupDesc,
                        onValueChange = { groupDesc = it },
                        label = { Text("Group Description") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (contacts.isNotEmpty()) {
                        Text("Select participants:", fontSize = 13.sp, color = LightTextSecondary)
                        contacts.filter { !it.isGroup }.take(5).forEach { c ->
                            val isChecked = c.userId in selectedMembers
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) selectedMembers.remove(c.userId) else selectedMembers.add(c.userId)
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        if (it) selectedMembers.add(c.userId) else selectedMembers.remove(c.userId)
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = WhatsAppActionGreen)
                                )
                                Text(c.effectiveDisplayName, fontSize = 14.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (groupName.isNotBlank()) {
                            onCreateGroup(groupName.trim(), selectedMembers.toList(), groupDesc.trim())
                            showCreateGroupDialog = false
                        }
                    },
                    modifier = Modifier.testTag("dialog_confirm_create_group")
                ) {
                    Text("Create", color = WhatsAppActionGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // My QR Code Dialog (Matches Screen 29 of Reference 1)
    if (showMyQrDialog) {
        AlertDialog(
            onDismissRequest = { showMyQrDialog = false },
            title = { Text("QR code") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WhatsAppAvatarCircle(name = currentUserName, size = 56.dp)
                    Text(currentUserName, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    NeoTactileQrCodeView(codeData = "$myShortCode|$currentUserId", size = 180.dp)
                    Text(
                        text = "Scan this code to start a WhatsApp chat with me.\nShort ID: $myShortCode",
                        fontSize = 12.sp,
                        color = LightTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showMyQrDialog = false }) {
                    Text("Done", color = WhatsAppActionGreen)
                }
            }
        )
    }
}

@Composable
private fun CommunityActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String? = null
) {
    val mod = if (testTag != null) Modifier.testTag(testTag) else Modifier
    Row(
        modifier = mod
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = LightTextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = LightTextSecondary
            )
        }
    }
}

// ============================================================================
// SCREEN 11: COMMUNITY INFO — Matches WhatsApp_UI_Reference_1 #11
// ============================================================================
@Composable
fun WhatsAppCommunityInfoScreen(
    community: CommunityModel,
    contacts: List<ResolvedContactProfile>,
    onBack: () -> Unit,
    onSelectGroupChat: (String) -> Unit,
    onAddGroup: () -> Unit,
    onAddMembers: () -> Unit,
    onSendAnnouncement: (String) -> Unit
) {
    var showAnnouncementDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightSurfaceSecondary)
            .testTag("community_info_screen")
    ) {
        // Green Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(WhatsAppHeaderGreen)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            IconButton(onClick = { showAnnouncementDialog = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = Color.White
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Community Title Card + 3 Action Buttons (Add members, Search, More)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LightBackgroundWhite)
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(WhatsAppAccentGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = community.name,
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = community.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightTextPrimary
                    )
                    Text(
                        text = "${community.groupIds.size.coerceAtLeast(3)} groups • ${community.memberIds.size.coerceAtLeast(12)} members",
                        fontSize = 14.sp,
                        color = LightTextSecondary
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onAddMembers() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Add members",
                                tint = WhatsAppActionGreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Add members", fontSize = 12.sp, color = LightTextSecondary)
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onBack() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = WhatsAppActionGreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Search", fontSize = 12.sp, color = LightTextSecondary)
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { showAnnouncementDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = "Announcement",
                                tint = WhatsAppActionGreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Announce", fontSize = 12.sp, color = LightTextSecondary)
                        }
                    }
                }
            }

            // Groups inside Community + "+ Add group" Row (Matches Screen 11 of Reference 1)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LightBackgroundWhite)
                        .padding(vertical = 10.dp)
                ) {
                    Text(
                        text = "Groups",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LightTextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    val communityGroups = contacts.filter {
                        it.isGroup && (it.communityId == community.communityId || it.userId in community.groupIds)
                    }

                    communityGroups.forEach { grp ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectGroupChat(grp.userId) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WhatsAppAvatarCircle(name = grp.effectiveDisplayName, isGroup = true, size = 48.dp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = grp.effectiveDisplayName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LightTextPrimary
                                )
                                Text(
                                    text = grp.roleTag,
                                    fontSize = 13.sp,
                                    color = LightTextSecondary
                                )
                            }
                            Text("Today", fontSize = 12.sp, color = LightTextMuted)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddGroup() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add group",
                            tint = WhatsAppActionGreen
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Add group",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WhatsAppActionGreen
                        )
                    }
                }
            }
        }
    }

    if (showAnnouncementDialog) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAnnouncementDialog = false },
            title = { Text("Community Announcement") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Broadcast message to all groups") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (text.isNotBlank()) onSendAnnouncement(text.trim())
                        showAnnouncementDialog = false
                    }
                ) {
                    Text("Send", color = WhatsAppActionGreen)
                }
            }
        )
    }
}

// ============================================================================
// SCREEN 12: CREATE COMMUNITY — Matches WhatsApp_UI_Reference_1 #12
// ============================================================================
@Composable
fun WhatsAppCreateCommunityScreen(
    onBack: () -> Unit,
    onCreate: (name: String, description: String) -> Unit
) {
    var communityName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackgroundWhite)
            .testTag("create_community_screen")
    ) {
        // Top Green Header Bar: "< Create Community"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(WhatsAppHeaderGreen)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "Create Community",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(WhatsAppAccentGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Bring together related groups and people in one place.",
                fontSize = 14.sp,
                color = LightTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Community name",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LightTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = communityName,
                    onValueChange = { communityName = it },
                    placeholder = { Text("Enter name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhatsAppActionGreen,
                        unfocusedBorderColor = LightDividerGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Description (optional)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LightTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Add description") },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhatsAppActionGreen,
                        unfocusedBorderColor = LightDividerGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Bottom Full-Width Green "Create" Button (Matches Screen 12 of Reference 1)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(WhatsAppAccentGreen)
                .clickable {
                    if (communityName.isNotBlank()) {
                        onCreate(communityName.trim(), description.trim())
                    }
                }
                .testTag("confirm_create_community_button"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Create",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
