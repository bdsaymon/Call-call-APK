package com.example.data.repository

import android.content.Context
import android.util.Base64
import com.example.R
import com.example.data.local.LocalAliasDao
import com.example.data.local.LocalAliasEntity
import com.example.data.model.BlockedDevice
import com.example.data.model.BroadcastListDocument
import com.example.data.model.CallLogEntry
import com.example.data.model.ChatMessage
import com.example.data.model.CommunityModel
import com.example.data.model.ContactRequestItem
import com.example.data.model.GroupChatDocument
import com.example.data.model.LinkedDeviceSession
import com.example.data.model.MediaBlobDocument
import com.example.data.model.ResolvedContactProfile
import com.example.data.model.StoryItem
import com.example.data.model.TypingStateDocument
import com.example.data.model.UserProfile
import com.example.data.model.generateShortCode
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.ui.components.WebRtcHardwareController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.storage
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
 * Production Multi-Device Cloud Repository:
 * - Remote Cloud Layer: Cloud Firestore (`R.string.firestore_database_id`) + Firebase Cloud Storage
 * - Local Private Override Layer: Room SQLite (`local_aliases` table)
 * - Enforces real Firebase Authentication (`request.auth != null`) for all operations.
 * - Zero hardcoded demo users, fake sessions, or in-memory-only mock lists.
 */
class NeoTactileRepository(
    private val db: FirebaseFirestore,
    private val localAliasDao: LocalAliasDao? = null
) {
    constructor(context: Context, localAliasDao: LocalAliasDao? = null) : this(
        createConfiguredFirestore(context),
        localAliasDao
    )

    private val auth: com.google.firebase.auth.FirebaseAuth?
        get() = runCatching { Firebase.auth }.getOrNull()

    private fun requireUserId(): String {
        return auth?.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Firebase Auth before accessing Firestore.")
    }

    // =========================================================================
    // 1. USER PROFILE & PERSISTENT SETTINGS (/users/{userId})
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
        val initialBio = bio ?: "@$cleanUsername | Hey there! I am using WhatsApp."
        val fcmToken = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()

        val docRef = db.collection("users").document(uid)
        try {
            val existingSnap = docRef.get().await()
            if (!existingSnap.exists()) {
                val profile = UserProfile(
                    id = uid,
                    uid = uid,
                    email = email.ifBlank { auth?.currentUser?.email ?: "user@whatsapp.app" },
                    displayName = displayName.ifBlank { auth?.currentUser?.displayName ?: "Verified User" },
                    photoUrl = photoUrl,
                    coverUrl = coverUrl ?: "preset://emerald_cover",
                    bio = initialBio.take(300),
                    isPrivate = isPrivate,
                    friendsList = emptyList(),
                    pinnedChatIds = emptyList(),
                    archivedChatIds = emptyList(),
                    blockedUserIds = emptyList(),
                    starredMessageIds = emptyList(),
                    fcmToken = fcmToken,
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
                if (!fcmToken.isNullOrBlank()) updates["fcmToken"] = fcmToken.take(300)
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
        val docRef = db.collection("users").document(uid)
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    val profile = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (profile != null) {
                        _pinnedChatIds.value = profile.pinnedChatIds.toSet()
                        _archivedChatIds.value = profile.archivedChatIds.toSet()
                        _blockedUserIds.value = profile.blockedUserIds.toSet()
                        _starredMessageIds.value = profile.starredMessageIds.toSet()
                    }
                    profile
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
            (participantIds + uid).distinct().take(30)
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
            replyToPreview = replyToPreview
        )

        val convRef = db.collection("conversations").document(chatId)
        val docRef = convRef.collection("messages").document(msgId)
        try {
            val convData = mapOf(
                "chatId" to chatId,
                "participants" to effectiveParticipants,
                "lastMessage" to if (mediaType == "TEXT") content.take(300) else "[$mediaType]",
                "lastSenderId" to uid,
                "lastSenderName" to senderName.ifBlank { "Verified User" },
                "lastMediaType" to mediaType,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            runCatching { convRef.set(convData, SetOptions.merge()).await() }
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
        val docRef = db.collection("conversations").document(chatId).collection("messages").document(messageId)
        try {
            docRef.update(
                mapOf(
                    "content" to "🚫 This message was deleted",
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
    // 3. CROSS-DEVICE MEDIA UPLOAD & DOWNLOAD (Firebase Storage + /media_blobs)
    // =========================================================================
    suspend fun uploadCrossDeviceMedia(
        chatId: String,
        fileName: String,
        mimeType: String,
        mediaType: String,
        base64Data: String,
        participantIds: List<String>
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val mediaId = "media_${UUID.randomUUID().toString().replace("-", "").take(18)}"
        val effectiveParticipants = (participantIds + uid).distinct().take(30)

        // 1. Try Firebase Cloud Storage upload first if bucket is active
        val storageDownloadUrl = runCatching {
            val rawBytes = Base64.decode(base64Data, Base64.DEFAULT)
            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType(mimeType.ifBlank { "application/octet-stream" })
                .setCustomMetadata("uploaderId", uid)
                .setCustomMetadata("chatId", chatId.take(80))
                .setCustomMetadata("participantsCsv", effectiveParticipants.joinToString(","))
                .build()
            val storageRef = Firebase.storage.reference
                .child("chat_media")
                .child(chatId.take(80))
                .child(uid)
                .child("${mediaId}_$fileName")
            storageRef.putBytes(rawBytes, metadata).await()
            storageRef.downloadUrl.await().toString()
        }.getOrNull()

        if (!storageDownloadUrl.isNullOrBlank()) {
            return@runCatching storageDownloadUrl
        }

        // 2. Guaranteed Cloud Firestore `/media_blobs/{mediaId}` synchronization across devices
        val blobDoc = MediaBlobDocument(
            id = mediaId,
            mediaId = mediaId,
            uploaderId = uid,
            chatId = chatId,
            fileName = fileName,
            mimeType = mimeType,
            mediaType = mediaType,
            base64Data = base64Data.take(750_000),
            participantIds = effectiveParticipants
        )
        val docRef = db.collection("media_blobs").document(mediaId)
        try {
            docRef.set(blobDoc.toCreateMap()).await()
            "firestore://media_blobs/$mediaId"
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun fetchMediaBlob(mediaId: String): Result<MediaBlobDocument?> = runCatching {
        requireUserId()
        val docRef = db.collection("media_blobs").document(mediaId)
        try {
            val snap = docRef.get().await()
            snap.toObject(MediaBlobDocument::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun downloadFirebaseStorageBytes(storageUrl: String): Result<ByteArray> = runCatching {
        requireUserId()
        val ref = Firebase.storage.getReferenceFromUrl(storageUrl)
        ref.getBytes(15L * 1024L * 1024L).await()
    }

    // =========================================================================
    // 4. REAL-TIME TYPING INDICATORS (/typing_states/{typingId})
    // =========================================================================
    suspend fun updateTypingIndicator(
        chatId: String,
        senderName: String,
        participantIds: List<String>,
        isTyping: Boolean
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val typingId = "${chatId}_$uid".replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        val effectiveParticipants = (participantIds + uid).distinct().take(30)
        val doc = TypingStateDocument(
            id = typingId,
            typingId = typingId,
            chatId = chatId,
            senderId = uid,
            senderName = senderName.ifBlank { "User" },
            participantIds = effectiveParticipants,
            isTyping = isTyping
        )
        val docRef = db.collection("typing_states").document(typingId)
        try {
            docRef.set(doc.toCreateMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    fun observeActiveTypingStates(): Flow<List<TypingStateDocument>> = flow {
        val uid = requireUserId()
        val path = "typing_states"
        emitAll(
            db.collection("typing_states")
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snap ->
                    val nowMs = System.currentTimeMillis()
                    snap.documents.mapNotNull { doc ->
                        doc.toObject(TypingStateDocument::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.filter { state ->
                        val updatedMs = state.updatedAt?.toDate()?.time ?: nowMs
                        state.senderId != uid && state.isTyping && (nowMs - updatedMs) < 15_000L
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    // =========================================================================
    // 5. 24-HOUR EPHEMERAL STORIES / STATUS UPDATES (/stories/{storyId})
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
            expiresAt = expiresAtMs
        )
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
    // 6. HARDWARE GATEKEEPER & LINKED DEVICES (/blocked_devices & /linked_devices)
    // =========================================================================
    suspend fun registerDeviceHeartbeat(hardwareId: String, deviceModel: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val safeHardwareId = hardwareId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
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

        // Also register in `/linked_devices/{sessionId}` for real multi-device session management
        val sessionId = "${uid}_$safeHardwareId".replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        val sessionRef = db.collection("linked_devices").document(sessionId)
        runCatching {
            val session = LinkedDeviceSession(
                id = sessionId,
                sessionId = sessionId,
                ownerUid = uid,
                hardwareId = safeHardwareId,
                deviceModel = deviceModel,
                platform = "Android OS",
                isActive = true
            )
            sessionRef.set(session.toCreateMap()).await()
        }
    }

    fun observeDeviceGatekeeper(hardwareId: String): Flow<BlockedDevice?> = flow {
        requireUserId()
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

    fun observeLinkedDevices(): Flow<List<LinkedDeviceSession>> = flow {
        val uid = requireUserId()
        val path = "linked_devices"
        emitAll(
            db.collection("linked_devices")
                .whereEqualTo("ownerUid", uid)
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull { doc ->
                        doc.toObject(LinkedDeviceSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedByDescending { it.lastActive?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun removeLinkedDeviceSession(sessionId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("linked_devices").document(sessionId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // =========================================================================
    // 7. PERSISTENT MULTI-DEVICE GROUPS (/groups/{groupId})
    // =========================================================================
    fun observeMyGroups(): Flow<List<GroupChatDocument>> = flow {
        val uid = requireUserId()
        val path = "groups"
        emitAll(
            db.collection("groups")
                .whereArrayContains("memberIds", uid)
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull { doc ->
                        doc.toObject(GroupChatDocument::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedByDescending { it.updatedAt?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createGroupContact(
        groupName: String,
        memberIds: List<String>,
        description: String,
        communityId: String? = null
    ): Result<ResolvedContactProfile> = runCatching {
        val uid = requireUserId()
        val groupId = "group_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val allMembers = (memberIds + uid).distinct().take(30)
        val groupDoc = GroupChatDocument(
            id = groupId,
            groupId = groupId,
            name = groupName.trim().take(80),
            description = description.trim().ifBlank { "Group Chat (${allMembers.size} members)" }.take(300),
            avatarUrl = "preset://avatar_emerald",
            creatorUid = uid,
            memberIds = allMembers,
            adminIds = listOf(uid),
            communityId = communityId
        )
        val docRef = db.collection("groups").document(groupId)
        try {
            docRef.set(groupDoc.toCreateMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }

        val groupProfile = ResolvedContactProfile(
            userId = groupId,
            originalDisplayName = groupDoc.name,
            originalAvatarUri = groupDoc.avatarUrl,
            overriddenDisplayName = null,
            overriddenAvatarUri = null,
            roleTag = groupDoc.description,
            isOnline = true,
            isGroup = true,
            memberIds = allMembers,
            adminIds = listOf(uid),
            communityId = communityId
        )
        saveLocalAliasOverride(groupId, groupDoc.name, groupDoc.avatarUrl)
        groupProfile
    }

    suspend fun addMemberToGroup(groupId: String, newMemberUid: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("groups").document(groupId)
        try {
            docRef.update(
                mapOf(
                    "memberIds" to FieldValue.arrayUnion(newMemberUid),
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
    // 8. PERSISTENT MULTI-DEVICE COMMUNITIES (/communities/{communityId})
    // =========================================================================
    fun observeCommunities(): Flow<List<CommunityModel>> = flow {
        val uid = requireUserId()
        val path = "communities"
        emitAll(
            db.collection("communities")
                .whereArrayContains("memberIds", uid)
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull { doc ->
                        doc.toObject(CommunityModel::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedByDescending { it.updatedAt?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createCommunity(
        name: String,
        description: String,
        announcement: String,
        initialGroupNames: List<String>,
        memberIds: List<String>
    ): Result<CommunityModel> = runCatching {
        val uid = requireUserId()
        val commId = "comm_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val allMembers = (memberIds + uid).distinct().take(50)
        val createdGroupIds = mutableListOf<String>()
        for (gName in initialGroupNames.filter { it.isNotBlank() }) {
            val grp = createGroupContact(
                groupName = gName,
                memberIds = allMembers,
                description = "$name • Community Group",
                communityId = commId
            ).getOrThrow()
            createdGroupIds.add(grp.userId)
        }
        val community = CommunityModel(
            id = commId,
            communityId = commId,
            name = name.trim().take(80),
            description = description.trim().ifBlank { "Official Community" }.take(300),
            creatorUid = uid,
            announcementText = announcement.trim().ifBlank { "Welcome to $name Announcements!" }.take(1000),
            groupIds = createdGroupIds,
            memberIds = allMembers
        )
        val docRef = db.collection("communities").document(commId)
        try {
            docRef.set(community.toCreateMap()).await()
            community
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun sendCommunityAnnouncement(
        communityId: String,
        senderName: String,
        announcementText: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("communities").document(communityId)
        val snap = docRef.get().await()
        val comm = snap.toObject(CommunityModel::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?: return@runCatching

        docRef.update(
            mapOf(
                "announcementText" to announcementText.take(1000),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        comm.groupIds.forEach { groupId ->
            sendMessage(
                chatId = groupId,
                receiverId = groupId,
                senderName = senderName,
                content = "📢 Community Announcement: $announcementText",
                mediaType = "TEXT",
                participantIds = (comm.memberIds + uid).distinct()
            )
        }
    }

    // =========================================================================
    // 9. REAL-TIME WEBRTC CALL SIGNALING & CALL LOGS (/calls/{callId})
    // =========================================================================
    fun observeCallLogs(): Flow<List<CallLogEntry>> = flow {
        val uid = requireUserId()
        val path = "calls"
        emitAll(
            db.collection("calls")
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull { doc ->
                        doc.toObject(CallLogEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?.resolvedForUser(uid)
                    }.sortedByDescending { it.timestampMs }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun sendWebRtcCallSignal(
        peerId: String,
        peerName: String,
        senderName: String,
        callType: String, // "AUDIO" | "VIDEO"
        signalState: String, // "RINGING" | "CONNECTED" | "ENDED" | "REJECTED"
        durationSeconds: Int = 0,
        existingCallId: String? = null,
        context: Context? = null
    ): Result<CallLogEntry> = runCatching {
        val uid = requireUserId()
        val callId = existingCallId?.takeIf { it.isNotBlank() }
            ?: "call_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("calls").document(callId)

        val (sdpOffer, iceCandidates) = WebRtcHardwareController.createRealSdpOffer(
            context = context,
            callType = callType,
            callerUid = uid,
            onIceCandidateGathered = { updatedCandidates ->
                docRef.update(
                    mapOf(
                        "iceCandidates" to updatedCandidates.take(8000),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        )
        val participants = listOf(uid, peerId).filter { it.isNotBlank() }.distinct()

        val entry = CallLogEntry(
            id = callId,
            callId = callId,
            callerId = uid,
            callerName = senderName.ifBlank { "Verified User" },
            calleeId = peerId,
            calleeName = peerName.ifBlank { "Contact" },
            participantIds = participants,
            callType = callType,
            status = signalState,
            sdpOffer = sdpOffer,
            iceCandidates = iceCandidates,
            turnServer = "stun:stun.l.google.com:19302",
            durationSeconds = durationSeconds,
            peerId = peerId,
            peerName = peerName,
            direction = "OUTGOING"
        )

        try {
            if (existingCallId.isNullOrBlank()) {
                docRef.set(entry.toCreateMap()).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "status" to signalState,
                    "durationSeconds" to durationSeconds.coerceIn(0, 86400),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (signalState == "CONNECTED") {
                    updates["sdpAnswer"] = WebRtcHardwareController.buildSdpAnswer(callType, uid)
                    updates["iceCandidates"] = iceCandidates
                }
                docRef.update(updates).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }

        val chatId = buildConversationId(uid, peerId)
        val readableSummary = when (signalState) {
            "RINGING" -> "📞 ${if (callType == "VIDEO") "Video" else "Voice"} call started"
            "CONNECTED" -> "🟢 Call connected"
            "REJECTED" -> "🔴 Call declined"
            else -> "☎️ ${if (callType == "VIDEO") "Video" else "Voice"} call ended (${durationSeconds}s)"
        }
        sendMessage(
            chatId = chatId,
            receiverId = peerId,
            senderName = senderName,
            content = readableSummary,
            mediaType = "TEXT",
            fileName = "WEBRTC_SIGNAL:$callType:$signalState:$durationSeconds"
        )
        entry
    }

    suspend fun answerIncomingCall(
        callEntry: CallLogEntry,
        context: Context? = null
    ): Result<CallLogEntry> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("calls").document(callEntry.callId)
        val (sdpAnswer, iceCandidates) = WebRtcHardwareController.createRealSdpAnswer(
            context = context,
            callType = callEntry.callType,
            calleeUid = uid,
            remoteSdpOffer = callEntry.sdpOffer,
            remoteIceCandidates = callEntry.iceCandidates,
            onIceCandidateGathered = { updatedCandidates ->
                val combined = listOfNotNull(callEntry.iceCandidates, updatedCandidates)
                    .filter { it.isNotBlank() }
                    .joinToString(";")
                docRef.update(
                    mapOf(
                        "iceCandidates" to combined.take(8000),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        )
        val mergedCandidates = listOfNotNull(callEntry.iceCandidates, iceCandidates)
            .filter { it.isNotBlank() }
            .joinToString(";")
            .take(8000)

        try {
            docRef.update(
                mapOf(
                    "status" to "CONNECTED",
                    "sdpAnswer" to sdpAnswer,
                    "iceCandidates" to mergedCandidates,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            callEntry.copy(
                status = "CONNECTED",
                sdpAnswer = sdpAnswer,
                iceCandidates = mergedCandidates
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun rejectOrEndCall(callId: String, finalStatus: String, durationSeconds: Int): Result<Unit> = runCatching {
        requireUserId()
        WebRtcHardwareController.closeActivePeerConnection()
        val docRef = db.collection("calls").document(callId)
        try {
            docRef.update(
                mapOf(
                    "status" to finalStatus,
                    "durationSeconds" to durationSeconds.coerceIn(0, 86400),
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
    // 10. PERSISTENT BROADCAST LISTS (/broadcast_lists/{listId})
    // =========================================================================
    fun observeBroadcastLists(): Flow<List<BroadcastListDocument>> = flow {
        val uid = requireUserId()
        val path = "broadcast_lists"
        emitAll(
            db.collection("broadcast_lists")
                .whereEqualTo("ownerUid", uid)
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull { doc ->
                        doc.toObject(BroadcastListDocument::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedByDescending { it.updatedAt?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createBroadcastList(name: String, recipientIds: List<String>): Result<BroadcastListDocument> = runCatching {
        val uid = requireUserId()
        val listId = "bcast_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val doc = BroadcastListDocument(
            id = listId,
            listId = listId,
            ownerUid = uid,
            name = name.trim().take(80),
            recipientIds = recipientIds.distinct().take(50)
        )
        val docRef = db.collection("broadcast_lists").document(listId)
        try {
            docRef.set(doc.toCreateMap()).await()
            doc
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun sendBroadcastMessage(
        broadcastList: BroadcastListDocument,
        senderName: String,
        content: String
    ): Result<Int> = runCatching {
        val uid = requireUserId()
        var deliveredCount = 0
        for (recipientId in broadcastList.recipientIds) {
            if (recipientId.isBlank() || recipientId == uid) continue
            val chatId = buildConversationId(uid, recipientId)
            sendMessage(
                chatId = chatId,
                receiverId = recipientId,
                senderName = senderName,
                content = "📢 [Broadcast: ${broadcastList.name}] $content",
                mediaType = "TEXT"
            ).onSuccess { deliveredCount++ }
        }
        deliveredCount
    }

    // =========================================================================
    // 11. CONTACTS, DISCOVERY & PERSISTENT PIN / ARCHIVE / BLOCK / STAR
    // =========================================================================
    private val _dynamicContacts = MutableStateFlow<List<ResolvedContactProfile>>(emptyList())

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
        val groupsFlow: Flow<List<GroupChatDocument>> = observeMyGroups().catch { emit(emptyList()) }
        val typingFlow: Flow<List<TypingStateDocument>> = observeActiveTypingStates().catch { emit(emptyList()) }

        return combine(
            _dynamicContacts,
            aliasesFlow,
            groupsFlow,
            typingFlow,
            combine(_blockedUserIds, _pinnedChatIds, _archivedChatIds) { b, p, a -> Triple(b, p, a) }
        ) { dynamicPeers, aliases, remoteGroups, typingStates, (blockedSet, pinnedSet, archivedSet) ->
            val aliasByTarget = aliases.associateBy { it.targetUserId }
            val typingByChatOrUser = typingStates.map { it.senderId }.toSet() +
                typingStates.map { it.chatId }.toSet()

            val groupProfiles = remoteGroups.map { grp ->
                ResolvedContactProfile(
                    userId = grp.groupId,
                    originalDisplayName = grp.name,
                    originalAvatarUri = grp.avatarUrl ?: "preset://avatar_emerald",
                    overriddenDisplayName = null,
                    overriddenAvatarUri = null,
                    roleTag = grp.description.ifBlank { "Group • ${grp.memberIds.size} members" },
                    isOnline = true,
                    isGroup = true,
                    memberIds = grp.memberIds,
                    adminIds = grp.adminIds,
                    communityId = grp.communityId
                )
            }

            val mergedDynamic = (groupProfiles + dynamicPeers).distinctBy { it.userId }
            val existingIds = mergedDynamic.map { it.userId }.toSet()

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

            (mergedDynamic + restoredFromRoom).map { peer ->
                val override = aliasByTarget[peer.userId]
                peer.copy(
                    overriddenDisplayName = override?.overriddenDisplayName ?: peer.overriddenDisplayName,
                    overriddenAvatarUri = override?.overriddenAvatarUri ?: peer.overriddenAvatarUri,
                    isBlocked = peer.userId in blockedSet,
                    isPinned = peer.userId in pinnedSet,
                    isArchived = peer.userId in archivedSet,
                    isTyping = peer.userId in typingByChatOrUser
                )
            }
        }
    }

    suspend fun syncFriendsFromFirestore(friendIds: List<String>) {
        if (friendIds.isEmpty()) return
        val uid = auth?.currentUser?.uid ?: return
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
            if (fId == uid) continue
            val remoteUser = publicMap[fId] ?: getUserProfileById(fId).getOrNull()
            if (remoteUser != null) {
                val resolved = ResolvedContactProfile(
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
                val idx = currentList.indexOfFirst { it.userId == fId }
                if (idx >= 0) currentList[idx] = resolved else currentList.add(resolved)
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

        val docRef = db.collection("users").document(uid)
        runCatching {
            docRef.update(
                mapOf(
                    "friendsList" to FieldValue.arrayUnion(peer.userId),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        saveLocalAliasOverride(
            targetUserId = peer.userId,
            overriddenDisplayName = optionalAlias?.takeIf { it.isNotBlank() } ?: peer.originalDisplayName,
            overriddenAvatarUri = peer.originalAvatarUri
        )
    }

    suspend fun findAndAddUserByIdentifier(
        identifier: String,
        fallbackName: String?,
        roleTag: String = "Verified Contact",
        localAlias: String? = null
    ): Result<ResolvedContactProfile> = runCatching {
        val uid = requireUserId()
        val cleanQuery = identifier.trim()
        require(cleanQuery.isNotBlank()) { "Please enter a Username, Email, Phone, or Short ID" }

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
            throw IllegalArgumentException("No registered user found matching '$cleanQuery'. Ask your contact to sign in first.")
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
        _contactRequests.value = listOf(item) + _contactRequests.value.filter { it.requestId != reqId }

        // Persist to Firestore contact_requests collection
        runCatching {
            db.collection("contact_requests").document(reqId).set(
                mapOf(
                    "requestId" to reqId,
                    "fromUserId" to uid,
                    "fromDisplayName" to senderName,
                    "fromShortCode" to generateShortCode(uid),
                    "toUserId" to targetUser.uid,
                    "status" to "PENDING",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        val chatId = buildConversationId(uid, targetUser.uid)
        sendMessage(
            chatId = chatId,
            receiverId = targetUser.uid,
            senderName = senderName,
            content = "👋 Sent a contact request",
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

    fun observeIncomingContactRequests(): Flow<List<ContactRequestItem>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("contact_requests")
                .whereEqualTo("toUserId", uid)
                .whereEqualTo("status", "PENDING")
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull { doc ->
                        val reqId = doc.getString("requestId") ?: doc.id
                        val fromUid = doc.getString("fromUserId") ?: return@mapNotNull null
                        val fromName = doc.getString("fromDisplayName") ?: "NeoTactile User"
                        val shortCode = doc.getString("fromShortCode") ?: generateShortCode(fromUid)
                        val status = doc.getString("status") ?: "PENDING"
                        val timestampMs = doc.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis()
                        ContactRequestItem(
                            requestId = reqId,
                            fromUserId = fromUid,
                            fromDisplayName = fromName,
                            fromShortCode = shortCode,
                            toUserId = uid,
                            status = status,
                            timestampMs = timestampMs
                        )
                    }
                }
                .catch { emit(emptyList()) }
        )
    }

    suspend fun acceptContactRequest(
        request: ContactRequestItem,
        myDisplayName: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        // 1. Update contact_requests document status to ACCEPTED
        runCatching {
            db.collection("contact_requests").document(request.requestId).update(
                mapOf(
                    "status" to "ACCEPTED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        // 2. Add requester to current user's friendsList
        runCatching {
            db.collection("users").document(uid).update(
                mapOf(
                    "friendsList" to FieldValue.arrayUnion(request.fromUserId),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        // 3. Add current user to requester's friendsList
        runCatching {
            db.collection("users").document(request.fromUserId).update(
                mapOf(
                    "friendsList" to FieldValue.arrayUnion(uid),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        // 4. Update in-memory contact requests
        _contactRequests.value = _contactRequests.value.filter { it.requestId != request.requestId }

        // 5. Add to local resolved contacts
        addContact(
            ResolvedContactProfile(
                userId = request.fromUserId,
                originalDisplayName = request.fromDisplayName,
                originalAvatarUri = "preset://avatar_emerald",
                overriddenDisplayName = null,
                overriddenAvatarUri = null,
                roleTag = "Verified Contact",
                isOnline = true
            )
        )

        // 6. Send acceptance message
        val chatId = buildConversationId(uid, request.fromUserId)
        sendMessage(
            chatId = chatId,
            receiverId = request.fromUserId,
            senderName = myDisplayName,
            content = "🤝 Contact request accepted! You can now call and message each other.",
            mediaType = "TEXT"
        )
    }

    suspend fun rejectContactRequest(requestId: String): Result<Unit> = runCatching {
        requireUserId()
        runCatching {
            db.collection("contact_requests").document(requestId).update(
                mapOf(
                    "status" to "REJECTED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        _contactRequests.value = _contactRequests.value.filter { it.requestId != requestId }
    }

    suspend fun toggleBlockUser(targetUserId: String): Boolean {
        val uid = requireUserId()
        val current = _blockedUserIds.value.toMutableSet()
        val nowBlocked = if (targetUserId in current) {
            current.remove(targetUserId)
            false
        } else {
            current.add(targetUserId)
            true
        }
        _blockedUserIds.value = current
        runCatching {
            db.collection("users").document(uid).update(
                mapOf(
                    "blockedUserIds" to current.take(50),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        return nowBlocked
    }

    suspend fun togglePinChat(targetUserId: String): Boolean {
        val uid = requireUserId()
        val current = _pinnedChatIds.value.toMutableSet()
        val nowPinned = if (targetUserId in current) {
            current.remove(targetUserId)
            false
        } else {
            current.add(targetUserId)
            true
        }
        _pinnedChatIds.value = current
        runCatching {
            db.collection("users").document(uid).update(
                mapOf(
                    "pinnedChatIds" to current.take(50),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        return nowPinned
    }

    suspend fun toggleArchiveChat(targetUserId: String): Boolean {
        val uid = requireUserId()
        val current = _archivedChatIds.value.toMutableSet()
        val nowArchived = if (targetUserId in current) {
            current.remove(targetUserId)
            false
        } else {
            current.add(targetUserId)
            true
        }
        _archivedChatIds.value = current
        runCatching {
            db.collection("users").document(uid).update(
                mapOf(
                    "archivedChatIds" to current.take(50),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        return nowArchived
    }

    suspend fun toggleStarMessage(messageId: String): Boolean {
        val uid = requireUserId()
        val current = _starredMessageIds.value.toMutableSet()
        val nowStarred = if (messageId in current) {
            current.remove(messageId)
            false
        } else {
            current.add(messageId)
            true
        }
        _starredMessageIds.value = current
        runCatching {
            db.collection("users").document(uid).update(
                mapOf(
                    "starredMessageIds" to current.take(50),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        return nowStarred
    }

    suspend fun removeContact(peerUserId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val current = _dynamicContacts.value.toMutableList()
        current.removeAll { it.userId == peerUserId }
        _dynamicContacts.value = current

        val docRef = db.collection("users").document(uid)
        runCatching {
            docRef.update(
                mapOf(
                    "friendsList" to FieldValue.arrayRemove(peerUserId),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        clearLocalAliasOverride(peerUserId)
    }

    suspend fun searchDiscoverableUsers(query: String): Result<List<UserProfile>> = runCatching {
        requireUserId()
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
        val cleanName = overriddenDisplayName?.trim()?.takeIf { it.isNotEmpty() }
        val cleanAvatar = overriddenAvatarUri?.trim()?.takeIf { it.isNotEmpty() }
        if (cleanName == null && cleanAvatar == null) {
            dao.deleteAliasForUser(targetUserId)
        } else {
            dao.upsertAlias(
                LocalAliasEntity(
                    targetUserId = targetUserId,
                    overriddenDisplayName = cleanName,
                    overriddenAvatarUri = cleanAvatar,
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

        private fun createConfiguredFirestore(context: Context): FirebaseFirestore {
            val databaseId = try {
                context.applicationContext.getString(R.string.firestore_database_id)
            } catch (_: Exception) {
                "ai-studio-android-bb67e5ae-b69c-4855-b37e-606187dc144b"
            }
            if (com.google.firebase.FirebaseApp.getApps(context.applicationContext).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(context.applicationContext)
            }
            val instance = try {
                if (databaseId.isNotBlank() && databaseId != "(default)") {
                    FirebaseFirestore.getInstance(databaseId)
                } else {
                    FirebaseFirestore.getInstance()
                }
            } catch (_: Exception) {
                FirebaseFirestore.getInstance()
            }
            runCatching {
                instance.firestoreSettings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                    .build()
            }
            return instance
        }
    }
}
