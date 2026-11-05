package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.abs

enum class AppLanguage(val label: String) {
    BILINGUAL("বাংলা + EN"),
    BENGALI("বাংলা"),
    ENGLISH("English");

    fun tr(bn: String, en: String): String = when (this) {
        BILINGUAL -> "$bn ($en)"
        BENGALI -> bn
        ENGLISH -> en
    }

    fun shortTr(bn: String, en: String): String = when (this) {
        BILINGUAL -> bn
        BENGALI -> bn
        ENGLISH -> en
    }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val coverUrl: String? = null,
    val bio: String? = null,
    val isPrivate: Boolean = false,
    val friendsList: List<String> = emptyList(),
    val pinnedChatIds: List<String> = emptyList(),
    val archivedChatIds: List<String> = emptyList(),
    val blockedUserIds: List<String> = emptyList(),
    val starredMessageIds: List<String> = emptyList(),
    val fcmToken: String? = null,
    val isOnline: Boolean = true,
    @ServerTimestamp val lastSeen: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val shortCode: String
        get() = generateShortCode(uid.ifBlank { id })

    val username: String
        get() {
            val rawBio = bio.orEmpty()
            val match = Regex("@([a-zA-Z0-9_\\.]+)").find(rawBio)
            if (match != null) return match.groupValues[1]
            val base = displayName.lowercase().replace(Regex("[^a-z0-9]"), "")
            return if (base.isNotBlank()) base else "user_${shortCode.lowercase().replace("-", "")}"
        }

    val phoneNumber: String
        get() {
            val rawBio = bio.orEmpty()
            val match = Regex("tel:([+0-9\\-]+)").find(rawBio)
            return match?.groupValues?.get(1) ?: ""
        }

    val cleanBio: String
        get() {
            val rawBio = bio.orEmpty()
            return rawBio
                .replace(Regex("@[a-zA-Z0-9_\\.]+\\s*\\|?\\s*"), "")
                .replace(Regex("tel:[+0-9\\-]+\\s*\\|?\\s*"), "")
                .trim()
                .ifBlank { "Hey there! I am using WhatsApp." }
        }

    @Suppress("UNCHECKED_CAST")
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "email" to email.take(200),
            "displayName" to displayName.take(80),
            "photoUrl" to photoUrl?.take(500),
            "coverUrl" to coverUrl?.take(500),
            "bio" to bio?.take(300),
            "isPrivate" to isPrivate,
            "friendsList" to friendsList.take(50),
            "pinnedChatIds" to pinnedChatIds.take(50),
            "archivedChatIds" to archivedChatIds.take(50),
            "blockedUserIds" to blockedUserIds.take(50),
            "starredMessageIds" to starredMessageIds.take(50),
            "fcmToken" to fcmToken?.take(300),
            "isOnline" to isOnline,
            "lastSeen" to FieldValue.serverTimestamp(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val messageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val participantIds: List<String> = emptyList(),
    val senderName: String = "",
    val content: String = "",
    val mediaType: String = "TEXT", // TEXT | IMAGE | VIDEO | AUDIO | FILE
    val mediaUrl: String? = null,
    val fileName: String? = null,
    val audioDurationMs: Long = 0L,
    val readStatus: String = "SENT", // SENT | DELIVERED | READ
    val replyToMessageId: String? = null,
    val replyToSenderName: String? = null,
    val replyToPreview: String? = null,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val reactions: String? = null,
    @ServerTimestamp val timestamp: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isCallSignal: Boolean
        get() = fileName?.startsWith("WEBRTC_SIGNAL:") == true

    fun parsedReactions(): List<Pair<String, Int>> {
        val raw = reactions?.trim().orEmpty()
        if (raw.isEmpty()) return emptyList()
        return raw.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val emoji = parts[0].trim()
                val count = parts[1].trim().toIntOrNull() ?: 1
                if (emoji.isNotEmpty() && count > 0) emoji to count else null
            } else null
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun toCreateMap(): Map<String, Any> {
        val effectiveParticipants = if (participantIds.isNotEmpty()) {
            participantIds.distinct().take(30)
        } else {
            listOf(senderId, receiverId).distinct()
        }
        return mapOf(
            "messageId" to messageId,
            "chatId" to chatId,
            "senderId" to senderId,
            "receiverId" to receiverId,
            "participantIds" to effectiveParticipants,
            "senderName" to senderName.take(80),
            "content" to content.take(2000),
            "mediaType" to mediaType,
            "mediaUrl" to mediaUrl?.take(500),
            "fileName" to fileName?.take(200),
            "audioDurationMs" to audioDurationMs.coerceIn(0L, 600_000L),
            "readStatus" to readStatus,
            "replyToMessageId" to replyToMessageId?.take(128),
            "replyToSenderName" to replyToSenderName?.take(80),
            "replyToPreview" to replyToPreview?.take(200),
            "isEdited" to isEdited,
            "isDeleted" to isDeleted,
            "reactions" to reactions?.take(500),
            "timestamp" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class StoryItem(
    @DocumentId val id: String = "",
    val storyId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String? = null,
    val mediaUrl: String = "",
    val mediaType: String = "TEXT_CARD", // IMAGE | VIDEO | TEXT_CARD
    val caption: String = "",
    val visibleToUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    val expiresAt: Long = 0L
) {
    @Suppress("UNCHECKED_CAST")
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "storyId" to storyId,
            "authorId" to authorId,
            "authorName" to authorName.take(80),
            "authorAvatarUrl" to authorAvatarUrl?.take(500),
            "mediaUrl" to mediaUrl.take(500),
            "mediaType" to mediaType,
            "caption" to caption.take(300),
            "visibleToUids" to visibleToUids.take(50),
            "createdAt" to FieldValue.serverTimestamp(),
            "expiresAt" to expiresAt
        ).filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class GroupChatDocument(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val name: String = "",
    val description: String = "",
    val avatarUrl: String? = null,
    val creatorUid: String = "",
    val memberIds: List<String> = emptyList(),
    val adminIds: List<String> = emptyList(),
    val communityId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @Suppress("UNCHECKED_CAST")
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "groupId" to groupId,
            "name" to name.take(80),
            "description" to description.take(300),
            "avatarUrl" to avatarUrl?.take(500),
            "creatorUid" to creatorUid,
            "memberIds" to memberIds.distinct().take(30),
            "adminIds" to adminIds.distinct().take(10),
            "communityId" to communityId?.take(128),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class CommunityModel(
    @DocumentId val id: String = "",
    val communityId: String = "",
    val name: String = "",
    val description: String = "",
    val creatorUid: String = "",
    val announcementText: String = "",
    val groupIds: List<String> = emptyList(),
    val memberIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val createdAtMs: Long
        get() = createdAt?.toDate()?.time ?: System.currentTimeMillis()

    @Suppress("UNCHECKED_CAST")
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "communityId" to communityId,
            "name" to name.take(80),
            "description" to description.take(300),
            "creatorUid" to creatorUid,
            "announcementText" to announcementText.take(1000),
            "groupIds" to groupIds.distinct().take(20),
            "memberIds" to memberIds.distinct().take(50),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class CallLogEntry(
    @DocumentId val id: String = "",
    val callId: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val calleeId: String = "",
    val calleeName: String = "",
    val participantIds: List<String> = emptyList(),
    val callType: String = "AUDIO", // "AUDIO" | "VIDEO"
    val status: String = "RINGING", // "RINGING" | "CONNECTED" | "REJECTED" | "ENDED" | "MISSED"
    val sdpOffer: String = "",
    val sdpAnswer: String? = null,
    val iceCandidates: String? = null,
    val turnServer: String = "stun:stun.l.google.com:19302",
    val durationSeconds: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val peerId: String = "",
    val peerName: String = "",
    val direction: String = "OUTGOING"
) {
    val timestampMs: Long
        get() = createdAt?.toDate()?.time ?: System.currentTimeMillis()

    val sdpInfo: String
        get() = sdpOffer.ifBlank { "v=0 o=- ${callId.hashCode()} 2 IN IP4 0.0.0.0 s=WebRTC t=0 0 a=group:BUNDLE 0" }

    fun resolvedForUser(currentUid: String): CallLogEntry {
        val isOutgoing = callerId == currentUid || callerId.isBlank()
        val effectivePeerId = when {
            peerId.isNotBlank() -> peerId
            isOutgoing -> calleeId
            else -> callerId
        }
        val effectivePeerName = when {
            peerName.isNotBlank() -> peerName
            isOutgoing -> calleeName
            else -> callerName
        }
        val effectiveDirection = when {
            status == "MISSED" || (status == "REJECTED" && !isOutgoing) -> "MISSED"
            isOutgoing -> "OUTGOING"
            else -> "INCOMING"
        }
        return copy(
            peerId = effectivePeerId,
            peerName = effectivePeerName.ifBlank { "Contact" },
            direction = effectiveDirection
        )
    }

    @Suppress("UNCHECKED_CAST")
    fun toCreateMap(): Map<String, Any> {
        val participants = if (participantIds.isNotEmpty()) {
            participantIds.distinct().take(10)
        } else {
            listOf(callerId, calleeId).filter { it.isNotBlank() }.distinct()
        }
        return mapOf(
            "callId" to callId,
            "callerId" to callerId,
            "callerName" to callerName.take(80),
            "calleeId" to calleeId,
            "calleeName" to calleeName.take(80),
            "participantIds" to participants,
            "callType" to callType,
            "status" to status,
            "sdpOffer" to sdpOffer.take(8000),
            "sdpAnswer" to sdpAnswer?.take(8000),
            "iceCandidates" to iceCandidates?.take(8000),
            "turnServer" to turnServer.take(200),
            "durationSeconds" to durationSeconds.coerceIn(0, 86400),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class MediaBlobDocument(
    @DocumentId val id: String = "",
    val mediaId: String = "",
    val uploaderId: String = "",
    val chatId: String = "",
    val fileName: String = "",
    val mimeType: String = "application/octet-stream",
    val mediaType: String = "FILE", // IMAGE | VIDEO | AUDIO | FILE
    val base64Data: String = "",
    val participantIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    @Suppress("UNCHECKED_CAST")
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "mediaId" to mediaId,
            "uploaderId" to uploaderId,
            "chatId" to chatId.take(128),
            "fileName" to fileName.take(200),
            "mimeType" to mimeType.take(100),
            "mediaType" to mediaType,
            "base64Data" to base64Data.take(750_000),
            "participantIds" to participantIds.distinct().take(30),
            "createdAt" to FieldValue.serverTimestamp()
        )
    }
}

@IgnoreExtraProperties
data class TypingStateDocument(
    @DocumentId val id: String = "",
    val typingId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val participantIds: List<String> = emptyList(),
    val isTyping: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "typingId" to typingId,
            "chatId" to chatId.take(128),
            "senderId" to senderId,
            "senderName" to senderName.take(80),
            "participantIds" to participantIds.distinct().take(30),
            "isTyping" to isTyping,
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

@IgnoreExtraProperties
data class LinkedDeviceSession(
    @DocumentId val id: String = "",
    val sessionId: String = "",
    val ownerUid: String = "",
    val hardwareId: String = "",
    val deviceModel: String = "",
    val platform: String = "",
    val isActive: Boolean = true,
    @ServerTimestamp val lastActive: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "sessionId" to sessionId,
            "ownerUid" to ownerUid,
            "hardwareId" to hardwareId.take(128),
            "deviceModel" to deviceModel.take(120),
            "platform" to platform.take(80),
            "isActive" to isActive,
            "lastActive" to FieldValue.serverTimestamp(),
            "createdAt" to FieldValue.serverTimestamp()
        )
    }
}

@IgnoreExtraProperties
data class BroadcastListDocument(
    @DocumentId val id: String = "",
    val listId: String = "",
    val ownerUid: String = "",
    val name: String = "",
    val recipientIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "listId" to listId,
            "ownerUid" to ownerUid,
            "name" to name.take(80),
            "recipientIds" to recipientIds.distinct().take(50),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

@IgnoreExtraProperties
data class BlockedDevice(
    @DocumentId val id: String = "",
    val hardwareId: String = "",
    val ownerUid: String = "",
    val deviceModel: String = "",
    val banned: Boolean = false,
    val reason: String = "",
    @ServerTimestamp val bannedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "hardwareId" to hardwareId.take(128),
            "ownerUid" to ownerUid.take(128),
            "deviceModel" to deviceModel.take(120),
            "banned" to banned,
            "reason" to reason.take(300),
            "bannedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }
}

data class ResolvedContactProfile(
    val userId: String,
    val originalDisplayName: String,
    val originalAvatarUri: String?,
    val overriddenDisplayName: String?,
    val overriddenAvatarUri: String?,
    val roleTag: String = "Verified Contact",
    val isOnline: Boolean = true,
    val isGroup: Boolean = false,
    val memberIds: List<String> = emptyList(),
    val adminIds: List<String> = emptyList(),
    val communityId: String? = null,
    val username: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isBlocked: Boolean = false,
    val unreadCount: Int = 0,
    val lastSeenTimestamp: Long? = null,
    val isTyping: Boolean = false
) {
    val effectiveDisplayName: String
        get() = overriddenDisplayName?.takeIf { it.isNotBlank() } ?: originalDisplayName

    val effectiveAvatarUri: String?
        get() = overriddenAvatarUri?.takeIf { it.isNotBlank() } ?: originalAvatarUri

    val hasLocalOverride: Boolean
        get() = !overriddenDisplayName.isNullOrBlank() || !overriddenAvatarUri.isNotBlankOrNull()

    val shortCode: String
        get() = generateShortCode(userId)

    private fun String?.isNotBlankOrNull(): Boolean = !this.isNullOrBlank()
}

data class ContactRequestItem(
    val requestId: String,
    val fromUserId: String,
    val fromDisplayName: String,
    val fromShortCode: String,
    val toUserId: String,
    val status: String = "PENDING", // PENDING | ACCEPTED | REJECTED
    val timestampMs: Long = System.currentTimeMillis()
)

data class DevicePhoneContact(
    val name: String,
    val phoneNumber: String
)

fun generateShortCode(rawId: String): String {
    if (rawId.isBlank()) return "NT-0000"
    val clean = rawId.filter { it.isLetterOrDigit() }.uppercase()
    val hashNum = abs(rawId.hashCode()) % 9000 + 1000
    val prefix = clean.take(2).padEnd(2, 'X')
    return "$prefix-$hashNum"
}
