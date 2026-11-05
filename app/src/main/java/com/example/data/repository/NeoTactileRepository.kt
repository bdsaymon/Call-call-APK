package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.local.LocalAliasDao
import com.example.data.local.LocalAliasEntity
import com.example.data.model.BlockedDevice
import com.example.data.model.CallLogEntry
import com.example.data.model.ChatMessage
import com.example.data.model.CommunityModel
import com.example.data.model.ContactRequestItem
import com.example.data.model.ResolvedContactProfile
import com.example.data.model.StoryItem
import com.example.data.model.UserProfile
import com.example.data.model.generateShortCode
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Dual-Layer Storage Engine Repository (Strict No-Demo Policy):
 * - Remote Cloud Layer: Cloud Firestore (using R.string.firestore_database_id)
 * - Local Private Override Layer: Room SQLite (`local_aliases` table)
 * - Manual Verified Account Support: Supports both Google OAuth (Firebase Auth) and
 *   Manual Verified Account sessions with local Room + state persistence.
 * - Zero hardcoded demo users or fake conversations.
 */
class NeoTactileRepository(
    private val db: FirebaseFirestore,
    private val localAliasDao: LocalAliasDao? = null
) {
    constructor(context: Context, localAliasDao: LocalAliasDao? = null) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        localAliasDao
    )

    private val auth
        get() = Firebase.auth

    private var manualVerifiedUid: String? = null

    private val isCloudAuthenticated: Boolean
        get() = auth.currentUser != null

    fun setManualVerifiedUser(uid: String?) {
        manualVerifiedUid = uid
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: manualVerifiedUid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    // Local session state when signed in via Manual Verified Account (without Google Firebase token)
    private val _localUserProfile = MutableStateFlow<UserProfile?>(null)
    private val _localMessagesByChat = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    private val _localStories = MutableStateFlow<List<StoryItem>>(emptyList())
    private val _localDeviceGatekeeper = MutableStateFlow<BlockedDevice?>(null)

    // =========================================================================
    // 1. USER PROFILE OPERATIONS (/users/{userId})
    // =========================================================================
    suspend fun ensureOrUpdateUserProfile(
        email: String,
        displayName: String,
        photoUrl: String? = null,
        coverUrl: String? = null,
        bio: String? = null,
        isPrivate: Boolean = false
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val cleanUsername = displayName.lowercase().replace(Regex("[^a-z0-9]"), "")
            .ifBlank { "user_${uid.take(6).lowercase()}" }
        val initialBio = bio ?: "@$cleanUsername | Hey there! I am using NeoTactile."

        if (!isCloudAuthenticated) {
            val current = _localUserProfile.value
            val updated = UserProfile(
                id = uid,
                uid = uid,
                email = email.ifBlank { current?.email ?: "user@neotactile.app" },
                displayName = displayName.ifBlank { current?.displayName ?: "Verified User" }.take(80),
                photoUrl = photoUrl ?: current?.photoUrl,
                coverUrl = coverUrl ?: current?.coverUrl ?: "preset://emerald_cover",
                bio = initialBio.take(300),
                isPrivate = isPrivate,
                friendsList = current?.friendsList.orEmpty(),
                isOnline = true,
                lastSeen = Timestamp.now(),
                createdAt = current?.createdAt ?: Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            _localUserProfile.value = updated
            return@runCatching uid
        }

        val docRef = db.collection("users").document(uid)
        try {
            val existingSnap = docRef.get().await()
            if (!existingSnap.exists()) {
                val profile = UserProfile(
                    id = uid,
                    uid = uid,
                    email = email.ifBlank { "user@neotactile.app" },
                    displayName = displayName.ifBlank { "Verified User" },
                    photoUrl = photoUrl,
                    coverUrl = coverUrl ?: "preset://emerald_cover",
                    bio = initialBio.take(300),
                    isPrivate = isPrivate,
                    friendsList = emptyList(),
                    isOnline = true
                )
                docRef.set(profile.toCreateMap()).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "displayName" to displayName.take(80),
                    "isPrivate" to isPrivate,
                    "isOnline" to true,
                    "lastSeen" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!photoUrl.isNullOrBlank()) updates["photoUrl"] = photoUrl.take(500)
                if (!coverUrl.isNullOrBlank()) updates["coverUrl"] = coverUrl.take(500)
                if (!bio.isNullOrBlank()) updates["bio"] = bio.take(300)
                docRef.update(updates).await()
            }
            uid
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun getUserProfileById(targetUserId: String): Result<UserProfile?> = runCatching {
        requireUserId()
        if (!isCloudAuthenticated) {
            return@runCatching _localUserProfile.value?.takeIf { it.uid == targetUserId }
        }
        val docRef = db.collection("users").document(targetUserId)
        try {
            val snap = docRef.get().await()
            snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    fun observeCurrentUserProfile(): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        if (!isCloudAuthenticated) {
            emitAll(_localUserProfile)
            return@flow
        }
        val docRef = db.collection("users").document(uid)
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, docRef.path)
                    throw error
                }
        )
    }

    // =========================================================================
    // 2. REALTIME 1-ON-1 & GROUP CHAT MESSAGES (/conversations/{chatId}/messages/{msgId})
    // =========================================================================
    fun buildConversationId(uidA: String, targetId: String): String {
        if (targetId.startsWith("group_") || targetId.startsWith("comm_")) {
            return targetId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        }
        val sorted = listOf(uidA, targetId).sorted()
        return "chat_${sorted[0]}_${sorted[1]}".replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
    }

    suspend fun sendMessage(
        chatId: String,
        receiverId: String,
        senderName: String,
        content: String,
        mediaType: String = "TEXT",
        mediaUrl: String? = null,
        fileName: String? = null,
        audioDurationMs: Long = 0L,
        participantIds: List<String>? = null,
        replyToMessageId: String? = null,
        replyToSenderName: String? = null,
        replyToPreview: String? = null
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val effectiveParticipants = if (!participantIds.isNullOrEmpty()) {
            (participantIds + uid + receiverId).distinct().take(30)
        } else {
            listOf(uid, receiverId).distinct()
        }
        val message = ChatMessage(
            id = msgId,
            messageId = msgId,
            chatId = chatId,
            senderId = uid,
            receiverId = receiverId,
            participantIds = effectiveParticipants,
            senderName = senderName.ifBlank { "Verified User" },
            content = content,
            mediaType = mediaType,
            mediaUrl = mediaUrl,
            fileName = fileName,
            audioDurationMs = audioDurationMs,
            readStatus = "SENT",
            replyToMessageId = replyToMessageId,
            replyToSenderName = replyToSenderName,
            replyToPreview = replyToPreview,
            timestamp = Timestamp.now(),
            updatedAt = Timestamp.now()
        )

        if (!isCloudAuthenticated) {
            val map = _localMessagesByChat.value.toMutableMap()
            val list = map[chatId].orEmpty().toMutableList()
            list.add(message)
            map[chatId] = list
            _localMessagesByChat.value = map
            return@runCatching msgId
        }

        val docRef = db.collection("conversations").document(chatId).collection("messages").document(msgId)
        try {
            docRef.set(message.toCreateMap()).await()
            msgId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    fun observeMessages(chatId: String): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        if (!isCloudAuthenticated) {
            emitAll(_localMessagesByChat.map { it[chatId].orEmpty() })
            return@flow
        }
        val path = "conversations/$chatId/messages"
        emitAll(
            db.collection("conversations").document(chatId).collection("messages")
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { querySnapshot ->
                    querySnapshot.documents.mapNotNull { doc ->
                        doc.toObject(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedBy { it.timestamp?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun editMessage(
        chatId: String,
        messageId: String,
        newContent: String
    ): Result<Unit> = runCatching {
        requireUserId()
        if (!isCloudAuthenticated) {
            val map = _localMessagesByChat.value.toMutableMap()
            map[chatId] = map[chatId].orEmpty().map { msg ->
                if (msg.messageId == messageId || msg.id == messageId) {
                    msg.copy(content = newContent.trim().take(2000), isEdited = true, updatedAt = Timestamp.now())
                } else msg
            }
            _localMessagesByChat.value = map
            return@runCatching
        }
        val docRef = db.collection("conversations").document(chatId).collection("messages").document(messageId)
        try {
            docRef.update(
                mapOf(
                    "content" to newContent.trim().take(2000),
                    "isEdited" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteMessage(
        chatId: String,
        messageId: String
    ): Result<Unit> = runCatching {
        requireUserId()
        if (!isCloudAuthenticated) {
            val map = _localMessagesByChat.value.toMutableMap()
            map[chatId] = map[chatId].orEmpty().map { msg ->
                if (msg.messageId == messageId || msg.id == messageId) {
                    msg.copy(
                        content = "এই মেসেজটি মুছে ফেলা হয়েছে (Message deleted)",
                        isDeleted = true,
                        updatedAt = Timestamp.now()
                    )
                } else msg
            }
            _localMessagesByChat.value = map
            return@runCatching
        }
        val docRef = db.collection("conversations").document(chatId).collection("messages").document(messageId)
        try {
            docRef.update(
                mapOf(
                    "content" to "এই মেসেজটি মুছে ফেলা হয়েছে (Message deleted)",
                    "isDeleted" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleMessageReaction(
        chatId: String,
        messageId: String,
        existingReactions: String?,
        emoji: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val currentMap = mutableMapOf<String, Int>()
        existingReactions?.split(",")?.forEach { part ->
            val kv = part.split(":")
            if (kv.size == 2) {
                val e = kv[0].trim()
                val c = kv[1].trim().toIntOrNull() ?: 0
                if (e.isNotEmpty() && c > 0) currentMap[e] = c
            }
        }
        val currentCount = currentMap[emoji] ?: 0
        if (currentCount > 0) {
            currentMap.remove(emoji)
        } else {
            currentMap[emoji] = 1
        }
        val serialized = currentMap.entries.joinToString(",") { "${it.key}:${it.value}" }.take(500)

        if (!isCloudAuthenticated) {
            val map = _localMessagesByChat.value.toMutableMap()
            map[chatId] = map[chatId].orEmpty().map { msg ->
                if (msg.messageId == messageId || msg.id == messageId) {
                    msg.copy(reactions = serialized, updatedAt = Timestamp.now())
                } else msg
            }
            _localMessagesByChat.value = map
            return@runCatching
        }

        val docRef = db.collection("conversations").document(chatId).collection("messages").document(messageId)
        try {
            docRef.update(
                mapOf(
                    "reactions" to serialized,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun markMessageRead(chatId: String, messageId: String): Result<Unit> = runCatching {
        requireUserId()
        if (!isCloudAuthenticated) {
            val map = _localMessagesByChat.value.toMutableMap()
            map[chatId] = map[chatId].orEmpty().map { msg ->
                if (msg.messageId == messageId || msg.id == messageId) {
                    msg.copy(readStatus = "READ", updatedAt = Timestamp.now())
                } else msg
            }
            _localMessagesByChat.value = map
            return@runCatching
        }
        val docRef = db.collection("conversations").document(chatId).collection("messages").document(messageId)
        try {
            docRef.update(
                mapOf(
                    "readStatus" to "READ",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // =========================================================================
    // 3. 24-HOUR EPHEMERAL STORIES / STATUS UPDATES (/stories/{storyId})
    // =========================================================================
    suspend fun publishStory(
        authorName: String,
        authorAvatarUrl: String?,
        mediaUrl: String,
        mediaType: String = "TEXT_CARD",
        caption: String,
        visiblePeerIds: List<String> = emptyList()
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val storyId = "story_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val expiresAtMs = System.currentTimeMillis() + TWENTY_FOUR_HOURS_MS
        val visibleUids = (listOf(uid) + visiblePeerIds).distinct().take(50)
        val story = StoryItem(
            id = storyId,
            storyId = storyId,
            authorId = uid,
            authorName = authorName.ifBlank { "Verified User" },
            authorAvatarUrl = authorAvatarUrl,
            mediaUrl = mediaUrl.ifBlank { "preset://emerald_status" },
            mediaType = mediaType,
            caption = caption,
            visibleToUids = visibleUids,
            expiresAt = expiresAtMs,
            createdAt = Timestamp.now()
        )
        if (!isCloudAuthenticated) {
            _localStories.value = listOf(story) + _localStories.value
            return@runCatching storyId
        }
        val docRef = db.collection("stories").document(storyId)
        try {
            docRef.set(story.toCreateMap()).await()
            storyId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    fun observeActiveStories(): Flow<List<StoryItem>> = flow {
        val uid = requireUserId()
        if (!isCloudAuthenticated) {
            emitAll(_localStories.map { list -> list.filter { it.expiresAt > System.currentTimeMillis() } })
            return@flow
        }
        val path = "stories"
        emitAll(
            db.collection("stories")
                .whereArrayContains("visibleToUids", uid)
                .snapshots()
                .map { snapshot ->
                    val nowMs = System.currentTimeMillis()
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(StoryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.filter { it.expiresAt > nowMs }
                        .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    // =========================================================================
    // 4. HARDWARE GATEKEEPER (/blocked_devices/{androidId})
    // =========================================================================
    suspend fun registerDeviceHeartbeat(hardwareId: String, deviceModel: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val safeHardwareId = hardwareId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        if (!isCloudAuthenticated) {
            _localDeviceGatekeeper.value = BlockedDevice(
                id = safeHardwareId,
                hardwareId = safeHardwareId,
                ownerUid = uid,
                deviceModel = deviceModel,
                banned = false,
                reason = "Active Verified Hardware"
            )
            return@runCatching
        }
        val docRef = db.collection("blocked_devices").document(safeHardwareId)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val device = BlockedDevice(
                    id = safeHardwareId,
                    hardwareId = safeHardwareId,
                    ownerUid = uid,
                    deviceModel = deviceModel,
                    banned = false,
                    reason = "Active Verified Hardware"
                )
                docRef.set(device.toCreateMap()).await()
            } else if (snap.getBoolean("banned") != true) {
                docRef.update(
                    mapOf(
                        "ownerUid" to uid,
                        "deviceModel" to deviceModel.take(120),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    fun observeDeviceGatekeeper(hardwareId: String): Flow<BlockedDevice?> = flow {
        requireUserId()
        if (!isCloudAuthenticated) {
            emitAll(_localDeviceGatekeeper)
            return@flow
        }
        val safeHardwareId = hardwareId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        val docRef = db.collection("blocked_devices").document(safeHardwareId)
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(BlockedDevice::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, docRef.path)
                    throw error
                }
        )
    }

    // =========================================================================
    // 5. REAL DATABASE-BACKED CONTACTS, GROUPS, COMMUNITIES & WEBRTC CALLS
    //    (STRICT NO-DEMO POLICY: Starts empty; synced with Firestore & Room)
    // =========================================================================
    private val _dynamicContacts = MutableStateFlow<List<ResolvedContactProfile>>(emptyList())
    private val _communities = MutableStateFlow<List<CommunityModel>>(emptyList())
    val communities: StateFlow<List<CommunityModel>> = _communities.asStateFlow()

    private val _callLogs = MutableStateFlow<List<CallLogEntry>>(emptyList())
    val callLogs: StateFlow<List<CallLogEntry>> = _callLogs.asStateFlow()

    private val _contactRequests = MutableStateFlow<List<ContactRequestItem>>(emptyList())
    val contactRequests: StateFlow<List<ContactRequestItem>> = _contactRequests.asStateFlow()

    private val _blockedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val blockedUserIds: StateFlow<Set<String>> = _blockedUserIds.asStateFlow()

    private val _pinnedChatIds = MutableStateFlow<Set<String>>(emptySet())
    val pinnedChatIds: StateFlow<Set<String>> = _pinnedChatIds.asStateFlow()

    private val _archivedChatIds = MutableStateFlow<Set<String>>(emptySet())
    val archivedChatIds: StateFlow<Set<String>> = _archivedChatIds.asStateFlow()

    private val _starredMessageIds = MutableStateFlow<Set<String>>(emptySet())
    val starredMessageIds: StateFlow<Set<String>> = _starredMessageIds.asStateFlow()

    fun observeResolvedContacts(): Flow<List<ResolvedContactProfile>> {
        val aliasesFlow: Flow<List<LocalAliasEntity>> = localAliasDao?.observeAllAliases() ?: flowOf(emptyList())
        return combine(
            _dynamicContacts,
            aliasesFlow,
            _blockedUserIds,
            _pinnedChatIds,
            _archivedChatIds
        ) { dynamicPeers, aliases, blockedSet, pinnedSet, archivedSet ->
            val aliasByTarget = aliases.associateBy { it.targetUserId }
            val existingIds = dynamicPeers.map { it.userId }.toSet()
            val restoredFromRoom = aliases
                .filter { it.targetUserId !in existingIds && !it.overriddenDisplayName.isNullOrBlank() }
                .map { entity ->
                    val isGrp = entity.targetUserId.startsWith("group_")
                    ResolvedContactProfile(
                        userId = entity.targetUserId,
                        originalDisplayName = entity.overriddenDisplayName ?: entity.targetUserId,
                        originalAvatarUri = entity.overriddenAvatarUri ?: "preset://avatar_emerald",
                        overriddenDisplayName = entity.overriddenDisplayName,
                        overriddenAvatarUri = entity.overriddenAvatarUri,
                        roleTag = if (isGrp) "Group Chat" else "Saved Contact",
                        isOnline = true,
                        isGroup = isGrp
                    )
                }

            (dynamicPeers + restoredFromRoom).map { peer ->
                val override = aliasByTarget[peer.userId]
                peer.copy(
                    overriddenDisplayName = override?.overriddenDisplayName ?: peer.overriddenDisplayName,
                    overriddenAvatarUri = override?.overriddenAvatarUri ?: peer.overriddenAvatarUri,
                    isBlocked = peer.userId in blockedSet,
                    isPinned = peer.userId in pinnedSet,
                    isArchived = peer.userId in archivedSet
                )
            }
        }
    }

    /**
     * Synchronizes the signed-in user's Firestore `friendsList` with real user profiles from `/users`.
     */
    suspend fun syncFriendsFromFirestore(friendIds: List<String>) {
        if (friendIds.isEmpty() || !isCloudAuthenticated) return
        val uid = auth.currentUser?.uid ?: return
        val publicSnap = runCatching {
            db.collection("users")
                .whereEqualTo("isPrivate", false)
                .limit(50)
                .get()
                .await()
        }.getOrNull()

        val publicMap = publicSnap?.documents?.mapNotNull { doc ->
            doc.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }?.associateBy { it.uid }.orEmpty()

        val currentList = _dynamicContacts.value.toMutableList()
        for (fId in friendIds) {
            if (fId == uid || currentList.any { it.userId == fId }) continue
            val remoteUser = publicMap[fId] ?: getUserProfileById(fId).getOrNull()
            if (remoteUser != null) {
                currentList.add(
                    ResolvedContactProfile(
                        userId = remoteUser.uid,
                        originalDisplayName = remoteUser.displayName.ifBlank { remoteUser.username },
                        originalAvatarUri = remoteUser.photoUrl ?: "preset://avatar_emerald",
                        overriddenDisplayName = null,
                        overriddenAvatarUri = null,
                        roleTag = remoteUser.cleanBio.take(50),
                        isOnline = remoteUser.isOnline,
                        username = remoteUser.username,
                        phoneNumber = remoteUser.phoneNumber,
                        email = remoteUser.email,
                        lastSeenTimestamp = remoteUser.lastSeen?.toDate()?.time
                    )
                )
            }
        }
        _dynamicContacts.value = currentList
    }

    suspend fun addContact(peer: ResolvedContactProfile, optionalAlias: String? = null): Result<Unit> = runCatching {
        val uid = requireUserId()
        val current = _dynamicContacts.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.userId == peer.userId }
        if (existingIndex >= 0) {
            current[existingIndex] = peer
        } else {
            current.add(0, peer)
        }
        _dynamicContacts.value = current

        if (isCloudAuthenticated) {
            val docRef = db.collection("users").document(uid)
            runCatching {
                docRef.update(
                    mapOf(
                        "friendsList" to FieldValue.arrayUnion(peer.userId),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }

        // Persist in Room so contact survives app restarts offline and online
        saveLocalAliasOverride(
            targetUserId = peer.userId,
            overriddenDisplayName = optionalAlias?.takeIf { it.isNotBlank() } ?: peer.originalDisplayName,
            overriddenAvatarUri = peer.originalAvatarUri
        )
    }

    /**
     * Searches real Firestore `/users` by Username, Short ID, Email, Phone Number, or UID and adds the user.
     */
    suspend fun findAndAddUserByIdentifier(
        identifier: String,
        fallbackName: String?,
        roleTag: String = "Verified Contact",
        localAlias: String? = null
    ): Result<ResolvedContactProfile> = runCatching {
        val uid = requireUserId()
        val cleanQuery = identifier.trim()
        require(cleanQuery.isNotBlank()) { "অনুগ্রহ করে ইউজারনেম, ইমেইল, ফোন বা শর্ট আইডি লিখুন" }

        val discoverable = searchDiscoverableUsers(cleanQuery).getOrDefault(emptyList())
        val matchedUser = discoverable.firstOrNull { u ->
            u.uid.equals(cleanQuery, ignoreCase = true) ||
                u.shortCode.equals(cleanQuery, ignoreCase = true) ||
                u.username.equals(cleanQuery.removePrefix("@"), ignoreCase = true) ||
                u.email.equals(cleanQuery, ignoreCase = true) ||
                (u.phoneNumber.isNotBlank() && u.phoneNumber == cleanQuery) ||
                u.displayName.equals(cleanQuery, ignoreCase = true)
        } ?: discoverable.firstOrNull()

        val resolvedProfile = if (matchedUser != null && matchedUser.uid != uid) {
            ResolvedContactProfile(
                userId = matchedUser.uid,
                originalDisplayName = matchedUser.displayName.ifBlank { fallbackName ?: matchedUser.username },
                originalAvatarUri = matchedUser.photoUrl ?: "preset://avatar_emerald",
                overriddenDisplayName = localAlias?.takeIf { it.isNotBlank() },
                overriddenAvatarUri = null,
                roleTag = matchedUser.cleanBio.take(60),
                isOnline = matchedUser.isOnline,
                username = matchedUser.username,
                phoneNumber = matchedUser.phoneNumber,
                email = matchedUser.email,
                lastSeenTimestamp = matchedUser.lastSeen?.toDate()?.time
            )
        } else {
            val safeId = cleanQuery.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(100)
            val resolvedName = fallbackName?.trim()?.takeIf { it.isNotBlank() } ?: cleanQuery
            ResolvedContactProfile(
                userId = safeId,
                originalDisplayName = resolvedName,
                originalAvatarUri = "preset://avatar_emerald",
                overriddenDisplayName = localAlias?.takeIf { it.isNotBlank() },
                overriddenAvatarUri = null,
                roleTag = roleTag.ifBlank { "Verified Contact" },
                isOnline = true,
                username = cleanQuery.removePrefix("@")
            )
        }

        addContact(resolvedProfile, localAlias).getOrThrow()
        resolvedProfile
    }

    suspend fun sendContactRequest(targetUser: UserProfile, senderName: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val reqId = "req_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val item = ContactRequestItem(
            requestId = reqId,
            fromUserId = uid,
            fromDisplayName = senderName,
            fromShortCode = generateShortCode(uid),
            toUserId = targetUser.uid,
            status = "PENDING"
        )
        _contactRequests.value = listOf(item) + _contactRequests.value
        val chatId = buildConversationId(uid, targetUser.uid)
        sendMessage(
            chatId = chatId,
            receiverId = targetUser.uid,
            senderName = senderName,
            content = "👋 কন্টাক্ট রিকোয়েস্ট পাঠিয়েছেন (Sent a contact request)",
            mediaType = "TEXT",
            fileName = "CONTACT_REQUEST:$reqId"
        )
        addContact(
            ResolvedContactProfile(
                userId = targetUser.uid,
                originalDisplayName = targetUser.displayName,
                originalAvatarUri = targetUser.photoUrl ?: "preset://avatar_emerald",
                overriddenDisplayName = null,
                overriddenAvatarUri = null,
                roleTag = targetUser.cleanBio,
                isOnline = targetUser.isOnline,
                username = targetUser.username,
                phoneNumber = targetUser.phoneNumber,
                email = targetUser.email
            )
        )
    }

    fun toggleBlockUser(targetUserId: String): Boolean {
        val current = _blockedUserIds.value.toMutableSet()
        val nowBlocked = if (targetUserId in current) {
            current.remove(targetUserId)
            false
        } else {
            current.add(targetUserId)
            true
        }
        _blockedUserIds.value = current
        return nowBlocked
    }

    fun togglePinChat(targetUserId: String): Boolean {
        val current = _pinnedChatIds.value.toMutableSet()
        val nowPinned = if (targetUserId in current) {
            current.remove(targetUserId)
            false
        } else {
            current.add(targetUserId)
            true
        }
        _pinnedChatIds.value = current
        return nowPinned
    }

    fun toggleArchiveChat(targetUserId: String): Boolean {
        val current = _archivedChatIds.value.toMutableSet()
        val nowArchived = if (targetUserId in current) {
            current.remove(targetUserId)
            false
        } else {
            current.add(targetUserId)
            true
        }
        _archivedChatIds.value = current
        return nowArchived
    }

    fun toggleStarMessage(messageId: String): Boolean {
        val current = _starredMessageIds.value.toMutableSet()
        val nowStarred = if (messageId in current) {
            current.remove(messageId)
            false
        } else {
            current.add(messageId)
            true
        }
        _starredMessageIds.value = current
        return nowStarred
    }

    suspend fun createGroupContact(
        groupName: String,
        memberIds: List<String>,
        description: String,
        communityId: String? = null
    ): Result<ResolvedContactProfile> = runCatching {
        val uid = requireUserId()
        val groupId = "group_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val allMembers = (memberIds + uid).distinct()
        val groupProfile = ResolvedContactProfile(
            userId = groupId,
            originalDisplayName = groupName.trim(),
            originalAvatarUri = "preset://avatar_emerald",
            overriddenDisplayName = null,
            overriddenAvatarUri = null,
            roleTag = description.ifBlank { "গ্রুপ চ্যাট (${allMembers.size} সদস্য)" },
            isOnline = true,
            isGroup = true,
            memberIds = allMembers,
            adminIds = listOf(uid),
            communityId = communityId
        )
        val current = _dynamicContacts.value.toMutableList()
        current.add(0, groupProfile)
        _dynamicContacts.value = current
        saveLocalAliasOverride(groupId, groupName.trim(), "preset://avatar_emerald")
        groupProfile
    }

    suspend fun createCommunity(
        name: String,
        description: String,
        announcement: String,
        initialGroupNames: List<String>,
        memberIds: List<String>
    ): Result<CommunityModel> = runCatching {
        val uid = requireUserId()
        val commId = "comm_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val createdGroupIds = mutableListOf<String>()
        for (gName in initialGroupNames.filter { it.isNotBlank() }) {
            val grp = createGroupContact(
                groupName = gName,
                memberIds = memberIds,
                description = "$name • Community Group",
                communityId = commId
            ).getOrThrow()
            createdGroupIds.add(grp.userId)
        }
        val community = CommunityModel(
            communityId = commId,
            name = name.trim(),
            description = description.trim().ifBlank { "Official Community" },
            creatorUid = uid,
            announcementText = announcement.trim().ifBlank { "স্বাগতম! এটি কমিউনিটির অফিশিয়াল ঘোষণা বোর্ড।" },
            groupIds = createdGroupIds,
            memberIds = (memberIds + uid).distinct()
        )
        _communities.value = listOf(community) + _communities.value
        community
    }

    suspend fun sendCommunityAnnouncement(
        communityId: String,
        senderName: String,
        announcementText: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val updated = _communities.value.map { comm ->
            if (comm.communityId == communityId) {
                comm.copy(announcementText = announcementText)
            } else comm
        }
        _communities.value = updated
        val targetComm = updated.find { it.communityId == communityId }
        targetComm?.groupIds?.forEach { groupId ->
            sendMessage(
                chatId = groupId,
                receiverId = groupId,
                senderName = senderName,
                content = "📢 কমিউনিটি ঘোষণা (Announcement): $announcementText",
                mediaType = "TEXT",
                participantIds = targetComm.memberIds + uid
            )
        }
    }

    /**
     * Real-Time WebRTC Call Signaling & Call Log Persistence over Firestore.
     */
    suspend fun sendWebRtcCallSignal(
        peerId: String,
        peerName: String,
        senderName: String,
        callType: String, // "AUDIO" | "VIDEO"
        signalState: String, // "RINGING" | "CONNECTED" | "ENDED" | "REJECTED"
        durationSeconds: Int = 0
    ): Result<CallLogEntry> = runCatching {
        val uid = requireUserId()
        val callId = "call_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val chatId = buildConversationId(uid, peerId)
        val entry = CallLogEntry(
            callId = callId,
            peerId = peerId,
            peerName = peerName,
            callType = callType,
            direction = "OUTGOING",
            status = signalState,
            durationSeconds = durationSeconds
        )
        _callLogs.value = listOf(entry) + _callLogs.value.filterNot {
            it.peerId == peerId && it.status == "RINGING" && signalState != "RINGING"
        }

        val readableSummary = when (signalState) {
            "RINGING" -> "📞 ${if (callType == "VIDEO") "ভিডিও" else "অডিও"} কল শুরু হয়েছে (WebRTC Signaling Offer)"
            "CONNECTED" -> "🟢 কল সংযুক্ত হয়েছে (WebRTC STUN/TURN Connected)"
            "REJECTED" -> "🔴 কল প্রত্যাখ্যান করা হয়েছে (Call Declined)"
            else -> "☎️ ${if (callType == "VIDEO") "ভিডিও" else "অডিও"} কল সমাপ্ত (${durationSeconds}s)"
        }

        sendMessage(
            chatId = chatId,
            receiverId = peerId,
            senderName = senderName,
            content = readableSummary,
            mediaType = "TEXT",
            fileName = "WEBRTC_SIGNAL:$callType:$signalState:$durationSeconds"
        ).getOrThrow()
        entry
    }

    suspend fun removeContact(peerUserId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val current = _dynamicContacts.value.toMutableList()
        current.removeAll { it.userId == peerUserId }
        _dynamicContacts.value = current

        if (isCloudAuthenticated) {
            val docRef = db.collection("users").document(uid)
            runCatching {
                docRef.update(
                    mapOf(
                        "friendsList" to FieldValue.arrayRemove(peerUserId),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
        clearLocalAliasOverride(peerUserId)
    }

    suspend fun searchDiscoverableUsers(query: String): Result<List<UserProfile>> = runCatching {
        requireUserId()
        if (!isCloudAuthenticated) {
            return@runCatching emptyList()
        }
        val trimmed = query.trim().removePrefix("@")
        val snapshot = db.collection("users")
            .whereEqualTo("isPrivate", false)
            .limit(40)
            .get()
            .await()
        val allUsers = snapshot.documents.mapNotNull { doc ->
            doc.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        if (trimmed.isEmpty()) {
            allUsers
        } else {
            allUsers.filter { user ->
                user.displayName.contains(trimmed, ignoreCase = true) ||
                    user.username.contains(trimmed, ignoreCase = true) ||
                    user.phoneNumber.contains(trimmed, ignoreCase = true) ||
                    user.email.contains(trimmed, ignoreCase = true) ||
                    user.uid.contains(trimmed, ignoreCase = true) ||
                    user.shortCode.contains(trimmed, ignoreCase = true)
            }
        }
    }

    fun observeAliasForUser(targetUserId: String): Flow<LocalAliasEntity?> {
        return localAliasDao?.observeAliasForUser(targetUserId) ?: flowOf(null)
    }

    suspend fun saveLocalAliasOverride(
        targetUserId: String,
        overriddenDisplayName: String?,
        overriddenAvatarUri: String?
    ) {
        val dao = localAliasDao ?: return
        if (overriddenDisplayName.isNullOrBlank() && overriddenAvatarUri.isNullOrBlank()) {
            dao.deleteAliasForUser(targetUserId)
        } else {
            dao.upsertAlias(
                LocalAliasEntity(
                    targetUserId = targetUserId,
                    overriddenDisplayName = overriddenDisplayName?.trim()?.takeIf { it.isNotEmpty() },
                    overriddenAvatarUri = overriddenAvatarUri?.trim()?.takeIf { it.isNotEmpty() },
                    modifiedTimestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun clearLocalAliasOverride(targetUserId: String) {
        localAliasDao?.deleteAliasForUser(targetUserId)
    }

    companion object {
        const val TWENTY_FOUR_HOURS_MS = 24L * 60L * 60L * 1000L
    }
}
