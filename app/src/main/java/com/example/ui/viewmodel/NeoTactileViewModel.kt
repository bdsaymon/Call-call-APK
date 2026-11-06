package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.TactileAudioEngine
import com.example.data.model.AppLanguage
import com.example.data.model.BlockedDevice
import com.example.data.model.BroadcastListDocument
import com.example.data.model.CallLogEntry
import com.example.data.model.ChatMessage
import com.example.data.model.CommunityModel
import com.example.data.model.ContactRequestItem
import com.example.data.model.DevicePhoneContact
import com.example.data.model.LinkedDeviceSession
import com.example.data.model.ResolvedContactProfile
import com.example.data.model.StoryItem
import com.example.data.model.UserProfile
import com.example.data.model.generateShortCode
import com.example.data.repository.NeoTactileRepository
import com.example.notifications.NotificationHelper
import com.example.security.DeviceIdentityManager
import com.example.ui.components.WebRtcHardwareController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class NeoTactileViewModel(
    private val repository: NeoTactileRepository,
    private val audioEngine: TactileAudioEngine,
    private val deviceIdentityManager: DeviceIdentityManager,
    val currentUserId: String,
    val currentUserEmail: String,
    val currentUserDisplayName: String,
    val currentUserPhotoUrl: String?,
    private val appContext: Context? = null
) : ViewModel() {

    val hardwareId: String = deviceIdentityManager.getHardwareAndroidId()
    val deviceModel: String = deviceIdentityManager.getDeviceModelName()
    val currentUserShortCode: String = generateShortCode(currentUserId)

    private val _appLanguage = MutableStateFlow(AppLanguage.BILINGUAL)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _isLightTheme = MutableStateFlow(true)
    val isLightTheme: StateFlow<Boolean> = _isLightTheme.asStateFlow()

    private val _selectedPeerId = MutableStateFlow("")
    val selectedPeerId: StateFlow<String> = _selectedPeerId.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    // Reply, Edit & Fullscreen Media Viewer States
    private val _replyingToMessage = MutableStateFlow<ChatMessage?>(null)
    val replyingToMessage: StateFlow<ChatMessage?> = _replyingToMessage.asStateFlow()

    private val _editingMessage = MutableStateFlow<ChatMessage?>(null)
    val editingMessage: StateFlow<ChatMessage?> = _editingMessage.asStateFlow()

    private val _fullscreenMediaMessage = MutableStateFlow<ChatMessage?>(null)
    val fullscreenMediaMessage: StateFlow<ChatMessage?> = _fullscreenMediaMessage.asStateFlow()

    // Contact Search & Public User Discovery State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _discoverableUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    val discoverableUsers: StateFlow<List<UserProfile>> = _discoverableUsers.asStateFlow()

    // Story / Status viewer state
    private val _activeStoryViewerItem = MutableStateFlow<StoryItem?>(null)
    val activeStoryViewerItem: StateFlow<StoryItem?> = _activeStoryViewerItem.asStateFlow()

    // Active & Incoming WebRTC Call State
    private val _activeCallSession = MutableStateFlow<CallLogEntry?>(null)
    val activeCallSession: StateFlow<CallLogEntry?> = _activeCallSession.asStateFlow()

    private val _incomingCallSession = MutableStateFlow<CallLogEntry?>(null)
    val incomingCallSession: StateFlow<CallLogEntry?> = _incomingCallSession.asStateFlow()

    // Cache of materialized remote `firestore://media_blobs/{id}` -> local `file://` URI
    private val _materializedMediaUris = MutableStateFlow<Map<String, String>>(emptyMap())

    // Real Android Device Contacts
    private val _deviceContacts = MutableStateFlow<List<DevicePhoneContact>>(emptyList())
    val deviceContacts: StateFlow<List<DevicePhoneContact>> = _deviceContacts.asStateFlow()

    // Persistent Firestore-backed Communities, Call Logs, Linked Devices & Broadcast Lists
    val communities: StateFlow<List<CommunityModel>> = repository.observeCommunities()
        .catch { e ->
            Log.w(TAG, "Error observing communities", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private var notifiedCallIds = mutableSetOf<String>()
    private var notifiedMessageIds = mutableSetOf<String>()

    val callLogs: StateFlow<List<CallLogEntry>> = repository.observeCallLogs()
        .onEach { logs ->
            val ringingIncoming = logs.firstOrNull { call ->
                call.calleeId == currentUserId &&
                    call.callerId != currentUserId &&
                    call.status == "RINGING" &&
                    (System.currentTimeMillis() - call.timestampMs) < 60_000L
            }
            if (ringingIncoming != null) {
                _incomingCallSession.value = ringingIncoming
                if (notifiedCallIds.add(ringingIncoming.callId) && appContext != null) {
                    NotificationHelper.showIncomingCallNotification(
                        context = appContext,
                        callerName = ringingIncoming.callerName.ifBlank { ringingIncoming.peerName },
                        callType = ringingIncoming.callType
                    )
                }
            } else if (_incomingCallSession.value != null) {
                val currentInc = _incomingCallSession.value!!
                val updated = logs.find { it.callId == currentInc.callId }
                if (updated == null || updated.status != "RINGING") {
                    _incomingCallSession.value = null
                }
            }

            // Sync active call state & feed remote SDP Answer / ICE candidates to PeerConnection
            val active = _activeCallSession.value
            if (active != null) {
                val remoteMatch = logs.find { it.callId == active.callId }
                if (remoteMatch != null) {
                    if (remoteMatch.status == "ENDED" || remoteMatch.status == "REJECTED") {
                        WebRtcHardwareController.closeActivePeerConnection()
                        _activeCallSession.value = null
                    } else {
                        if (!remoteMatch.sdpAnswer.isNullOrBlank() || !remoteMatch.iceCandidates.isNullOrBlank()) {
                            WebRtcHardwareController.applyRemoteAnswerAndIce(
                                remoteSdpAnswer = remoteMatch.sdpAnswer,
                                remoteIceCandidates = remoteMatch.iceCandidates
                            )
                        }
                        if (remoteMatch.status != active.status || remoteMatch.sdpAnswer != active.sdpAnswer) {
                            _activeCallSession.value = remoteMatch
                        }
                    }
                }
            }
        }
        .catch { e ->
            Log.w(TAG, "Error observing call logs", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val linkedDevices: StateFlow<List<LinkedDeviceSession>> = repository.observeLinkedDevices()
        .catch { e ->
            Log.w(TAG, "Error observing linked devices", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val broadcastLists: StateFlow<List<BroadcastListDocument>> = repository.observeBroadcastLists()
        .catch { e ->
            Log.w(TAG, "Error observing broadcast lists", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val contactRequests: StateFlow<List<ContactRequestItem>> = kotlinx.coroutines.flow.combine(
        repository.contactRequests,
        repository.observeIncomingContactRequests().catch { emit(emptyList()) }
    ) { local, remote ->
        (remote + local).distinctBy { it.requestId }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )
    val blockedUserIds: StateFlow<Set<String>> = repository.blockedUserIds
    val pinnedChatIds: StateFlow<Set<String>> = repository.pinnedChatIds
    val archivedChatIds: StateFlow<Set<String>> = repository.archivedChatIds
    val starredMessageIds: StateFlow<Set<String>> = repository.starredMessageIds

    // Audio Engine Flows
    val isRecordingVoice: StateFlow<Boolean> = audioEngine.isRecording
    val liveVoiceAmplitude: StateFlow<Float> = audioEngine.liveAmplitude
    val recordingElapsedMs: StateFlow<Long> = audioEngine.recordingElapsedMs
    val activePlayingUrl: StateFlow<String?> = audioEngine.activePlayingUrl
    val isPlayingVoice: StateFlow<Boolean> = audioEngine.isPlaying
    val playbackProgress: StateFlow<Float> = audioEngine.playbackProgress
    val playbackAmplitude: StateFlow<Float> = audioEngine.playbackAmplitude

    // Security Gate Flows
    val isAppLocked: StateFlow<Boolean> = deviceIdentityManager.isAppLocked
    val biometricLockEnabled: StateFlow<Boolean> = deviceIdentityManager.biometricLockEnabled

    // Mobile Device Storage Stats Flow
    private val _deviceStorageStats = MutableStateFlow(audioEngine.getDeviceStorageStats())
    val deviceStorageStats: StateFlow<com.example.audio.DeviceStorageStats> = _deviceStorageStats.asStateFlow()

    private val _savedMediaFiles = MutableStateFlow<List<File>>(audioEngine.listSavedMediaFiles())
    val savedMediaFiles: StateFlow<List<File>> = _savedMediaFiles.asStateFlow()

    fun refreshDeviceStorageStats() {
        _deviceStorageStats.value = audioEngine.getDeviceStorageStats()
        _savedMediaFiles.value = audioEngine.listSavedMediaFiles()
        _deviceContacts.value = audioEngine.queryRealDeviceContacts()
    }

    // Remote User Profile StateFlow (Syncs real Firestore friendsList automatically)
    val userProfile: StateFlow<UserProfile?> = repository.observeCurrentUserProfile()
        .onEach { profile ->
            if (profile != null && profile.friendsList.isNotEmpty()) {
                repository.syncFriendsFromFirestore(profile.friendsList)
            }
        }
        .catch { e ->
            Log.w(TAG, "Error observing userProfile", e)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    // Dual-Layer Resolved Contacts & Groups
    val resolvedContacts: StateFlow<List<ResolvedContactProfile>> = repository.observeResolvedContacts()
        .onEach { list ->
            if (_selectedPeerId.value.isBlank() && list.isNotEmpty()) {
                _selectedPeerId.value = list.first().userId
            }
        }
        .catch { e ->
            Log.w(TAG, "Error observing resolved contacts", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Active 24-Hour Ephemeral Stories / Status Updates
    val activeStories: StateFlow<List<StoryItem>> = repository.observeActiveStories()
        .onEach { stories ->
            stories.forEach { story ->
                if (story.mediaUrl.startsWith("firestore://media_blobs/")) {
                    materializeRemoteMediaIfNeeded(story.mediaUrl)
                }
            }
        }
        .map { stories ->
            val cache = _materializedMediaUris.value
            stories.map { s ->
                val resolvedUrl = cache[s.mediaUrl] ?: s.mediaUrl
                s.copy(mediaUrl = resolvedUrl)
            }
        }
        .catch { e ->
            Log.w(TAG, "Error observing active stories", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Hardware Device Gatekeeper for this ANDROID_ID
    val currentDeviceGatekeeper: StateFlow<BlockedDevice?> = repository.observeDeviceGatekeeper(hardwareId)
        .catch { e ->
            Log.w(TAG, "Error observing device gatekeeper", e)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    // Realtime 1-on-1 & Group Chat Messages for Selected Peer/Group
    val currentChatMessages: StateFlow<List<ChatMessage>> = kotlinx.coroutines.flow.combine(
        _selectedPeerId.flatMapLatest { peerId ->
            if (peerId.isBlank()) {
                flowOf(emptyList())
            } else {
                val chatId = repository.buildConversationId(currentUserId, peerId)
                repository.observeMessages(chatId)
            }
        },
        _materializedMediaUris
    ) { messages, mediaCache ->
        messages.map { msg ->
            val rawUrl = msg.mediaUrl
            val needsLocalMaterialization = rawUrl != null && (
                rawUrl.startsWith("firestore://media_blobs/") ||
                    (rawUrl.startsWith("https://firebasestorage.googleapis.com") &&
                        (msg.mediaType == "AUDIO" || msg.mediaType == "FILE"))
                )
            if (needsLocalMaterialization && rawUrl != null) {
                val localUri = mediaCache[rawUrl]
                if (localUri != null) {
                    msg.copy(mediaUrl = localUri)
                } else {
                    materializeRemoteMediaIfNeeded(rawUrl, msg.fileName ?: "download_${msg.messageId}")
                    msg
                }
            } else {
                msg
            }
        }
    }
        .onEach { messages ->
            val peerId = _selectedPeerId.value
            if (peerId.isNotBlank()) {
                val chatId = repository.buildConversationId(currentUserId, peerId)
                messages.forEach { msg ->
                    if (msg.senderId != currentUserId && msg.readStatus != "READ") {
                        viewModelScope.launch {
                            repository.markMessageRead(chatId, msg.messageId.ifBlank { msg.id })
                        }
                    }
                    if (msg.senderId != currentUserId &&
                        !msg.isCallSignal &&
                        notifiedMessageIds.add(msg.messageId.ifBlank { msg.id })
                    ) {
                        val msgTime = msg.timestamp?.toDate()?.time ?: 0L
                        if (System.currentTimeMillis() - msgTime < 15_000L && appContext != null) {
                            NotificationHelper.showMessageNotification(
                                context = appContext,
                                senderName = msg.senderName,
                                messageText = msg.content
                            )
                        }
                    }
                }
            }
        }
        .catch { e ->
            Log.w(TAG, "Error observing chat messages", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private var typingDebounceJob: Job? = null

    init {
        initializeSession()
    }

    private fun initializeSession() {
        viewModelScope.launch {
            refreshDeviceStorageStats()
            repository.ensureOrUpdateUserProfile(
                email = currentUserEmail,
                displayName = currentUserDisplayName,
                photoUrl = currentUserPhotoUrl
            )
            repository.registerDeviceHeartbeat(
                hardwareId = hardwareId,
                deviceModel = deviceModel
            )
            searchAndDiscoverUsers("")
        }
    }

    private fun materializeRemoteMediaIfNeeded(remoteRef: String, fallbackFileName: String = "media_file") {
        if (_materializedMediaUris.value.containsKey(remoteRef)) return

        if (remoteRef.startsWith("https://firebasestorage.googleapis.com")) {
            viewModelScope.launch {
                repository.downloadFirebaseStorageBytes(remoteRef).onSuccess { bytes ->
                    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val file = audioEngine.materializeBase64BlobToLocalFile(
                        mediaId = "storage_${kotlin.math.abs(remoteRef.hashCode())}",
                        fileName = fallbackFileName,
                        base64Data = b64
                    )
                    if (file != null) {
                        val localUri = Uri.fromFile(file).toString()
                        _materializedMediaUris.value = _materializedMediaUris.value + (remoteRef to localUri)
                        refreshDeviceStorageStats()
                    }
                }
            }
            return
        }

        if (!remoteRef.startsWith("firestore://media_blobs/")) return
        val mediaId = remoteRef.removePrefix("firestore://media_blobs/").trim()
        if (mediaId.isBlank()) return

        viewModelScope.launch {
            repository.fetchMediaBlob(mediaId).onSuccess { blob ->
                if (blob != null && blob.base64Data.isNotBlank()) {
                    val file = audioEngine.materializeBase64BlobToLocalFile(
                        mediaId = blob.mediaId,
                        fileName = blob.fileName,
                        base64Data = blob.base64Data
                    )
                    if (file != null) {
                        val localUri = Uri.fromFile(file).toString()
                        _materializedMediaUris.value = _materializedMediaUris.value + (remoteRef to localUri)
                        refreshDeviceStorageStats()
                    }
                }
            }
        }
    }

    fun onMessageInputChanged(currentText: String) {
        val peerId = _selectedPeerId.value
        if (peerId.isBlank()) return
        val activeContact = resolvedContacts.value.find { it.userId == peerId }
        val chatId = repository.buildConversationId(currentUserId, peerId)
        val senderName = userProfile.value?.displayName?.ifBlank { currentUserDisplayName } ?: currentUserDisplayName
        val participants = activeContact?.memberIds?.takeIf { it.isNotEmpty() } ?: listOf(currentUserId, peerId)

        typingDebounceJob?.cancel()
        if (currentText.isBlank()) {
            viewModelScope.launch {
                repository.updateTypingIndicator(chatId, senderName, participants, isTyping = false)
            }
            return
        }

        typingDebounceJob = viewModelScope.launch {
            repository.updateTypingIndicator(chatId, senderName, participants, isTyping = true)
            delay(2800L)
            repository.updateTypingIndicator(chatId, senderName, participants, isTyping = false)
        }
    }

    fun toggleThemeMode() {
        val next = !_isLightTheme.value
        _isLightTheme.value = next
        _statusBannerMessage.value = if (next) {
            "Light Theme enabled"
        } else {
            "Dark Theme enabled"
        }
    }

    fun setLightTheme(enabled: Boolean) {
        _isLightTheme.value = enabled
    }

    fun cycleAppLanguage() {
        val next = when (_appLanguage.value) {
            AppLanguage.BILINGUAL -> AppLanguage.BENGALI
            AppLanguage.BENGALI -> AppLanguage.ENGLISH
            AppLanguage.ENGLISH -> AppLanguage.BILINGUAL
        }
        _appLanguage.value = next
        _statusBannerMessage.value = "Language set to: ${next.label}"
    }

    fun setAppLanguage(language: AppLanguage) {
        _appLanguage.value = language
        _statusBannerMessage.value = "Language set to: ${language.label}"
    }

    fun selectPeer(peerId: String) {
        _selectedPeerId.value = peerId
        _replyingToMessage.value = null
        _editingMessage.value = null
    }

    fun clearStatusBanner() {
        _statusBannerMessage.value = null
    }

    // =========================================================================
    // CHAT, REPLIES, EDITS, DELETES, FORWARD, STAR, REACTIONS & REAL CLOUD MEDIA
    // =========================================================================
    fun startReply(message: ChatMessage) {
        _editingMessage.value = null
        _replyingToMessage.value = message
    }

    fun startEdit(message: ChatMessage) {
        _replyingToMessage.value = null
        _editingMessage.value = message
    }

    fun cancelReplyOrEdit() {
        _replyingToMessage.value = null
        _editingMessage.value = null
    }

    fun openFullscreenMedia(message: ChatMessage?) {
        _fullscreenMediaMessage.value = message
    }

    fun sendTextMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val peerId = _selectedPeerId.value
        if (peerId.isBlank()) {
            _statusBannerMessage.value = "Please select or add a contact first"
            return
        }

        typingDebounceJob?.cancel()
        val activeContact = resolvedContacts.value.find { it.userId == peerId }
        val chatId = repository.buildConversationId(currentUserId, peerId)
        val senderName = userProfile.value?.displayName?.ifBlank { currentUserDisplayName } ?: currentUserDisplayName
        val participants = activeContact?.memberIds?.takeIf { it.isNotEmpty() } ?: listOf(currentUserId, peerId)

        viewModelScope.launch {
            repository.updateTypingIndicator(chatId, senderName, participants, isTyping = false)
        }

        val editing = _editingMessage.value
        if (editing != null) {
            _editingMessage.value = null
            viewModelScope.launch {
                repository.editMessage(chatId, editing.messageId.ifBlank { editing.id }, trimmed)
                    .onSuccess {
                        _statusBannerMessage.value = "Message edited"
                    }
                    .onFailure { e ->
                        _statusBannerMessage.value = "Edit failed: ${e.localizedMessage}"
                    }
            }
            return
        }

        if (activeContact?.isBlocked == true) {
            _statusBannerMessage.value = "This contact is blocked. Unblock to send messages."
            return
        }
        val replyMsg = _replyingToMessage.value
        _replyingToMessage.value = null

        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                receiverId = peerId,
                senderName = senderName,
                content = trimmed,
                mediaType = "TEXT",
                participantIds = activeContact?.memberIds,
                replyToMessageId = replyMsg?.messageId?.ifBlank { replyMsg.id },
                replyToSenderName = replyMsg?.senderName,
                replyToPreview = replyMsg?.content?.take(100)
            ).onFailure { e ->
                _statusBannerMessage.value = "Message failed: ${e.localizedMessage}"
            }
        }
    }

    fun forwardMessage(message: ChatMessage, targetPeerId: String) {
        val targetContact = resolvedContacts.value.find { it.userId == targetPeerId }
        val chatId = repository.buildConversationId(currentUserId, targetPeerId)
        val senderName = userProfile.value?.displayName?.ifBlank { currentUserDisplayName } ?: currentUserDisplayName
        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                receiverId = targetPeerId,
                senderName = senderName,
                content = "↪ Forwarded: ${message.content}",
                mediaType = message.mediaType,
                mediaUrl = message.mediaUrl,
                fileName = message.fileName,
                audioDurationMs = message.audioDurationMs,
                participantIds = targetContact?.memberIds
            ).onSuccess {
                _statusBannerMessage.value = "Message forwarded"
            }.onFailure { e ->
                _statusBannerMessage.value = "Forward failed: ${e.localizedMessage}"
            }
        }
    }

    fun toggleStarMessage(messageId: String) {
        viewModelScope.launch {
            val isStarred = repository.toggleStarMessage(messageId)
            _statusBannerMessage.value = if (isStarred) "Message starred ⭐" else "Removed from Starred"
        }
    }

    fun togglePinConversation(peerId: String) {
        viewModelScope.launch {
            val isPinned = repository.togglePinChat(peerId)
            _statusBannerMessage.value = if (isPinned) "Chat pinned 📌" else "Chat unpinned"
        }
    }

    fun toggleArchiveConversation(peerId: String) {
        viewModelScope.launch {
            val isArchived = repository.toggleArchiveChat(peerId)
            _statusBannerMessage.value = if (isArchived) "Chat archived" else "Chat unarchived"
        }
    }

    fun toggleBlockContact(peerId: String) {
        viewModelScope.launch {
            val isBlocked = repository.toggleBlockUser(peerId)
            _statusBannerMessage.value = if (isBlocked) "Contact blocked 🚫" else "Contact unblocked"
        }
    }

    fun deleteChatMessage(message: ChatMessage) {
        val peerId = _selectedPeerId.value
        if (peerId.isBlank()) return
        val chatId = repository.buildConversationId(currentUserId, peerId)
        val msgId = message.messageId.ifBlank { message.id }
        viewModelScope.launch {
            repository.deleteMessage(chatId, msgId)
                .onSuccess {
                    _statusBannerMessage.value = "Message deleted"
                }
                .onFailure { e ->
                    _statusBannerMessage.value = "Delete failed: ${e.localizedMessage}"
                }
        }
    }

    fun reactToMessage(message: ChatMessage, emoji: String) {
        val peerId = _selectedPeerId.value
        if (peerId.isBlank()) return
        val chatId = repository.buildConversationId(currentUserId, peerId)
        val msgId = message.messageId.ifBlank { message.id }
        viewModelScope.launch {
            repository.toggleMessageReaction(
                chatId = chatId,
                messageId = msgId,
                existingReactions = message.reactions,
                emoji = emoji
            )
        }
    }

    fun sendFileAttachment(fileName: String, fileDescription: String) {
        val peerId = _selectedPeerId.value
        if (peerId.isBlank()) return
        val activeContact = resolvedContacts.value.find { it.userId == peerId }
        val chatId = repository.buildConversationId(currentUserId, peerId)
        val senderName = userProfile.value?.displayName?.ifBlank { currentUserDisplayName } ?: currentUserDisplayName
        val participants = activeContact?.memberIds?.takeIf { it.isNotEmpty() } ?: listOf(currentUserId, peerId)
        val replyMsg = _replyingToMessage.value
        _replyingToMessage.value = null

        viewModelScope.launch {
            val savedFileUri = audioEngine.saveDocumentToDeviceStorage(fileName, fileDescription)
            val localFile = File(Uri.parse(savedFileUri).path ?: "")
            val b64 = audioEngine.encodeFileToBase64(localFile)
            val cloudMediaUrl = if (b64.isNotBlank()) {
                repository.uploadCrossDeviceMedia(
                    chatId = chatId,
                    fileName = fileName,
                    mimeType = "application/pdf",
                    mediaType = "FILE",
                    base64Data = b64,
                    participantIds = participants
                ).getOrDefault(savedFileUri)
            } else {
                savedFileUri
            }
            if (cloudMediaUrl.startsWith("firestore://media_blobs/")) {
                _materializedMediaUris.value = _materializedMediaUris.value + (cloudMediaUrl to savedFileUri)
            }
            refreshDeviceStorageStats()

            repository.sendMessage(
                chatId = chatId,
                receiverId = peerId,
                senderName = senderName,
                content = fileDescription,
                mediaType = "FILE",
                fileName = fileName,
                mediaUrl = cloudMediaUrl,
                participantIds = activeContact?.memberIds,
                replyToMessageId = replyMsg?.messageId?.ifBlank { replyMsg.id },
                replyToSenderName = replyMsg?.senderName,
                replyToPreview = replyMsg?.content?.take(100)
            ).onSuccess {
                _statusBannerMessage.value = "Document sent: $fileName"
            }.onFailure { e ->
                _statusBannerMessage.value = "File share error: ${e.localizedMessage}"
            }
        }
    }

    fun startVoiceNoteRecording() {
        audioEngine.startRecording()
    }

    fun stopAndSendVoiceNote() {
        val (file, durationMs) = audioEngine.stopRecording()
        refreshDeviceStorageStats()
        if (file == null) return
        val peerId = _selectedPeerId.value
        if (peerId.isBlank()) return
        val activeContact = resolvedContacts.value.find { it.userId == peerId }
        val chatId = repository.buildConversationId(currentUserId, peerId)
        val senderName = userProfile.value?.displayName?.ifBlank { currentUserDisplayName } ?: currentUserDisplayName
        val participants = activeContact?.memberIds?.takeIf { it.isNotEmpty() } ?: listOf(currentUserId, peerId)
        val localFileUri = Uri.fromFile(file).toString()
        val replyMsg = _replyingToMessage.value
        _replyingToMessage.value = null

        viewModelScope.launch {
            val b64 = audioEngine.encodeFileToBase64(file)
            val cloudMediaUrl = if (b64.isNotBlank()) {
                repository.uploadCrossDeviceMedia(
                    chatId = chatId,
                    fileName = file.name,
                    mimeType = "audio/mp4",
                    mediaType = "AUDIO",
                    base64Data = b64,
                    participantIds = participants
                ).getOrDefault(localFileUri)
            } else {
                localFileUri
            }
            if (cloudMediaUrl.startsWith("firestore://media_blobs/")) {
                _materializedMediaUris.value = _materializedMediaUris.value + (cloudMediaUrl to localFileUri)
            }

            repository.sendMessage(
                chatId = chatId,
                receiverId = peerId,
                senderName = senderName,
                content = "Voice message (${(durationMs / 1000).coerceAtLeast(1)}s)",
                mediaType = "AUDIO",
                mediaUrl = cloudMediaUrl,
                audioDurationMs = durationMs,
                participantIds = activeContact?.memberIds,
                replyToMessageId = replyMsg?.messageId?.ifBlank { replyMsg.id },
                replyToSenderName = replyMsg?.senderName,
                replyToPreview = replyMsg?.content?.take(100)
            ).onSuccess {
                _statusBannerMessage.value = "Voice message sent"
            }.onFailure { e ->
                _statusBannerMessage.value = "Voice message failed: ${e.localizedMessage}"
            }
        }
    }

    fun cancelVoiceNoteRecording() {
        audioEngine.stopRecording()
        refreshDeviceStorageStats()
    }

    fun toggleVoicePlayback(mediaUrl: String, durationMs: Long) {
        val resolvedUrl = _materializedMediaUris.value[mediaUrl] ?: mediaUrl
        audioEngine.togglePlayback(resolvedUrl, durationMs)
    }

    fun seekVoicePlayback(fraction: Float) {
        audioEngine.seekToFraction(fraction)
    }

    fun compressAndSendPhoto(uri: Uri, caption: String = "Photo") {
        val peerId = _selectedPeerId.value
        if (peerId.isBlank()) return
        val activeContact = resolvedContacts.value.find { it.userId == peerId }
        val chatId = repository.buildConversationId(currentUserId, peerId)
        val senderName = userProfile.value?.displayName?.ifBlank { currentUserDisplayName } ?: currentUserDisplayName
        val participants = activeContact?.memberIds?.takeIf { it.isNotEmpty() } ?: listOf(currentUserId, peerId)
        val replyMsg = _replyingToMessage.value
        _replyingToMessage.value = null

        viewModelScope.launch {
            val (localFile, b64) = audioEngine.compressAndCachePhotoWithBytes(uri)
            val localUriStr = if (localFile != null) Uri.fromFile(localFile).toString() else uri.toString()
            val cloudMediaUrl = if (b64.isNotBlank() && localFile != null) {
                repository.uploadCrossDeviceMedia(
                    chatId = chatId,
                    fileName = localFile.name,
                    mimeType = "image/jpeg",
                    mediaType = "IMAGE",
                    base64Data = b64,
                    participantIds = participants
                ).getOrDefault(localUriStr)
            } else {
                localUriStr
            }
            if (cloudMediaUrl.startsWith("firestore://media_blobs/")) {
                _materializedMediaUris.value = _materializedMediaUris.value + (cloudMediaUrl to localUriStr)
            }
            refreshDeviceStorageStats()

            repository.sendMessage(
                chatId = chatId,
                receiverId = peerId,
                senderName = senderName,
                content = caption,
                mediaType = "IMAGE",
                mediaUrl = cloudMediaUrl,
                participantIds = activeContact?.memberIds,
                replyToMessageId = replyMsg?.messageId?.ifBlank { replyMsg.id },
                replyToSenderName = replyMsg?.senderName,
                replyToPreview = replyMsg?.content?.take(100)
            ).onSuccess {
                _statusBannerMessage.value = "Photo sent"
            }.onFailure { e ->
                _statusBannerMessage.value = "Photo share failed: ${e.localizedMessage}"
            }
        }
    }

    // =========================================================================
    // WEBRTC REAL-TIME AUDIO & VIDEO CALLING ENGINE
    // =========================================================================
    fun startWebRtcCall(peerId: String, peerName: String, callType: String) {
        val senderName = userProfile.value?.displayName?.ifBlank { currentUserDisplayName } ?: currentUserDisplayName
        viewModelScope.launch {
            repository.sendWebRtcCallSignal(
                peerId = peerId,
                peerName = peerName,
                senderName = senderName,
                callType = callType,
                signalState = "RINGING",
                context = appContext
            ).onSuccess { entry ->
                _activeCallSession.value = entry
                _statusBannerMessage.value = "Calling $peerName (${callType})..."
            }.onFailure { e ->
                _statusBannerMessage.value = "Call signaling error: ${e.localizedMessage}"
            }
        }
    }

    fun acceptIncomingWebRtcCall() {
        val incoming = _incomingCallSession.value ?: return
        _incomingCallSession.value = null
        viewModelScope.launch {
            repository.answerIncomingCall(incoming, context = appContext).onSuccess { connected ->
                _selectedPeerId.value = incoming.callerId.ifBlank { incoming.peerId }
                _activeCallSession.value = connected
            }
        }
    }

    fun declineIncomingWebRtcCall() {
        val incoming = _incomingCallSession.value ?: return
        _incomingCallSession.value = null
        viewModelScope.launch {
            repository.rejectOrEndCall(incoming.callId, finalStatus = "REJECTED", durationSeconds = 0)
        }
    }

    fun endActiveWebRtcCall(durationSeconds: Int) {
        val active = _activeCallSession.value ?: return
        _activeCallSession.value = null
        viewModelScope.launch {
            repository.rejectOrEndCall(
                callId = active.callId,
                finalStatus = "ENDED",
                durationSeconds = durationSeconds
            )
            _statusBannerMessage.value = "Call ended (${durationSeconds}s)"
        }
    }

    // =========================================================================
    // LINKED DEVICES & BROADCAST LISTS
    // =========================================================================
    fun removeLinkedDevice(sessionId: String) {
        viewModelScope.launch {
            repository.removeLinkedDeviceSession(sessionId).onSuccess {
                _statusBannerMessage.value = "Device session logged out"
            }
        }
    }

    fun createNewBroadcastList(name: String, recipientIds: List<String>) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        val effectiveRecipients = recipientIds.ifEmpty {
            resolvedContacts.value.filter { !it.isGroup }.map { it.userId }
        }
        viewModelScope.launch {
            repository.createBroadcastList(cleanName, effectiveRecipients).onSuccess {
                _statusBannerMessage.value = "Broadcast list '$cleanName' saved to cloud"
            }.onFailure { e ->
                _statusBannerMessage.value = "Broadcast error: ${e.localizedMessage}"
            }
        }
    }

    fun sendBroadcastToList(list: BroadcastListDocument, messageText: String) {
        val cleanMsg = messageText.trim()
        if (cleanMsg.isBlank()) return
        val senderName = userProfile.value?.displayName?.ifBlank { currentUserDisplayName } ?: currentUserDisplayName
        viewModelScope.launch {
            repository.sendBroadcastMessage(list, senderName, cleanMsg).onSuccess { count ->
                _statusBannerMessage.value = "Broadcast delivered to $count recipients"
            }
        }
    }

    // =========================================================================
    // DUAL-LAYER ROOM LOCAL ALIAS OVERRIDES
    // =========================================================================
    fun saveLocalAlias(targetUserId: String, aliasName: String?, aliasAvatarPreset: String?) {
        viewModelScope.launch {
            repository.saveLocalAliasOverride(
                targetUserId = targetUserId,
                overriddenDisplayName = aliasName,
                overriddenAvatarUri = aliasAvatarPreset
            )
            _statusBannerMessage.value = "Local alias saved on device"
        }
    }

    fun clearLocalAlias(targetUserId: String) {
        viewModelScope.launch {
            repository.clearLocalAliasOverride(targetUserId)
            _statusBannerMessage.value = "Local alias cleared"
        }
    }

    // =========================================================================
    // USER ADDITION, SEARCH BY USERNAME/PHONE/EMAIL/SHORT-ID, GROUPS & COMMUNITIES
    // =========================================================================
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchAndDiscoverUsers(query)
    }

    fun searchAndDiscoverUsers(query: String) {
        viewModelScope.launch {
            repository.searchDiscoverableUsers(query)
                .onSuccess { users ->
                    _discoverableUsers.value = users.filter { it.uid != currentUserId }
                }
                .onFailure { e ->
                    Log.w(TAG, "Search discoverable users failed", e)
                }
        }
    }

    fun addNewPeerContact(
        targetUserId: String,
        displayName: String,
        avatarPreset: String? = null,
        roleTag: String = "Verified Contact",
        localAliasName: String? = null
    ) {
        val query = targetUserId.trim()
        if (query.isBlank()) {
            _statusBannerMessage.value = "Enter a Username, Email, Phone, or Short ID"
            return
        }

        viewModelScope.launch {
            repository.findAndAddUserByIdentifier(
                identifier = query,
                fallbackName = displayName.trim().ifBlank { query },
                roleTag = roleTag,
                localAlias = localAliasName
            ).onSuccess { added ->
                _selectedPeerId.value = added.userId
                _statusBannerMessage.value = "${added.effectiveDisplayName} added to your contacts"
            }.onFailure { e ->
                _statusBannerMessage.value = e.localizedMessage ?: "User not found"
            }
        }
    }

    fun sendFriendRequestToUser(targetUser: UserProfile) {
        val senderName = userProfile.value?.displayName?.ifBlank { currentUserDisplayName } ?: currentUserDisplayName
        viewModelScope.launch {
            repository.sendContactRequest(targetUser, senderName)
                .onSuccess {
                    _selectedPeerId.value = targetUser.uid
                    _statusBannerMessage.value = "${targetUser.displayName} added to your contacts"
                }
                .onFailure { e ->
                    _statusBannerMessage.value = "Request failed: ${e.localizedMessage}"
                }
        }
    }

    fun acceptIncomingContactRequest(request: ContactRequestItem) {
        val myName = userProfile.value?.displayName?.ifBlank { currentUserDisplayName } ?: currentUserDisplayName
        viewModelScope.launch {
            repository.acceptContactRequest(request, myName)
                .onSuccess {
                    _selectedPeerId.value = request.fromUserId
                    _statusBannerMessage.value = "Accepted contact request from ${request.fromDisplayName}"
                }
                .onFailure { e ->
                    _statusBannerMessage.value = "Accept failed: ${e.localizedMessage}"
                }
        }
    }

    fun rejectIncomingContactRequest(requestId: String) {
        viewModelScope.launch {
            repository.rejectContactRequest(requestId)
                .onSuccess {
                    _statusBannerMessage.value = "Contact request declined"
                }
        }
    }

    fun createNewGroupChat(
        groupName: String,
        selectedMemberIds: List<String>,
        description: String
    ) {
        val cleanName = groupName.trim().take(80)
        if (cleanName.isBlank()) {
            _statusBannerMessage.value = "Please enter a group name"
            return
        }
        viewModelScope.launch {
            repository.createGroupContact(cleanName, selectedMemberIds, description)
                .onSuccess { group ->
                    _selectedPeerId.value = group.userId
                    _statusBannerMessage.value = "Group '$cleanName' created & synced to cloud"
                }
                .onFailure { e ->
                    _statusBannerMessage.value = "Group creation failed: ${e.localizedMessage}"
                }
        }
    }

    fun createNewCommunity(
        name: String,
        description: String,
        announcement: String,
        groupNames: List<String>,
        memberIds: List<String>
    ) {
        val cleanName = name.trim().take(80)
        if (cleanName.isBlank()) {
            _statusBannerMessage.value = "Please enter a community name"
            return
        }
        viewModelScope.launch {
            repository.createCommunity(cleanName, description, announcement, groupNames, memberIds)
                .onSuccess {
                    _statusBannerMessage.value = "Community '$cleanName' created & synced to cloud"
                }
                .onFailure { e ->
                    _statusBannerMessage.value = "Community creation failed: ${e.localizedMessage}"
                }
        }
    }

    fun postCommunityAnnouncement(communityId: String, announcementText: String) {
        val senderName = userProfile.value?.displayName?.ifBlank { currentUserDisplayName } ?: currentUserDisplayName
        viewModelScope.launch {
            repository.sendCommunityAnnouncement(communityId, senderName, announcementText)
                .onSuccess {
                    _statusBannerMessage.value = "Community announcement sent to all groups"
                }
        }
    }

    fun removePeerContact(peerUserId: String) {
        viewModelScope.launch {
            repository.removeContact(peerUserId)
                .onSuccess {
                    if (_selectedPeerId.value == peerUserId) {
                        _selectedPeerId.value = resolvedContacts.value.firstOrNull { it.userId != peerUserId }?.userId.orEmpty()
                    }
                    _statusBannerMessage.value = "Contact removed"
                }
                .onFailure { e ->
                    _statusBannerMessage.value = "Remove failed: ${e.localizedMessage}"
                }
        }
    }

    // =========================================================================
    // 24-HOUR EPHEMERAL STORIES / STATUS UPDATES (WITH CLOUD PHOTO UPLOAD)
    // =========================================================================
    fun publishEphemeralStory(
        caption: String,
        mediaPreset: String = "preset://emerald_status",
        mediaType: String = "TEXT_CARD"
    ) {
        val trimmed = caption.trim().ifBlank { "Status update" }
        val profile = userProfile.value
        val authorName = profile?.displayName?.ifBlank { currentUserDisplayName } ?: currentUserDisplayName
        val peerIds = resolvedContacts.value.filter { !it.isGroup }.map { it.userId }

        viewModelScope.launch {
            val resolvedMediaUrl = if (mediaType == "IMAGE" && (mediaPreset.startsWith("content://") || mediaPreset.startsWith("file://"))) {
                val (localFile, b64) = audioEngine.compressAndCachePhotoWithBytes(Uri.parse(mediaPreset))
                if (b64.isNotBlank() && localFile != null) {
                    repository.uploadCrossDeviceMedia(
                        chatId = "story_${currentUserId.take(20)}",
                        fileName = localFile.name,
                        mimeType = "image/jpeg",
                        mediaType = "IMAGE",
                        base64Data = b64,
                        participantIds = peerIds + currentUserId
                    ).getOrDefault(mediaPreset)
                } else {
                    mediaPreset
                }
            } else {
                mediaPreset
            }

            repository.publishStory(
                authorName = authorName,
                authorAvatarUrl = profile?.photoUrl ?: currentUserPhotoUrl,
                mediaUrl = resolvedMediaUrl,
                mediaType = mediaType,
                caption = trimmed,
                visiblePeerIds = peerIds
            ).onSuccess {
                _statusBannerMessage.value = "24-hour status update published to cloud"
            }.onFailure { e ->
                _statusBannerMessage.value = "Status error: ${e.localizedMessage}"
            }
        }
    }

    fun openStoryViewer(story: StoryItem) {
        _activeStoryViewerItem.value = story
    }

    fun closeStoryViewer() {
        _activeStoryViewerItem.value = null
    }

    // =========================================================================
    // PROFILE & SECURITY GATE CONTROLS
    // =========================================================================
    fun updateProfileDetails(
        displayName: String,
        bio: String,
        isPrivate: Boolean,
        coverPreset: String,
        username: String = "",
        phoneNumber: String = ""
    ) {
        val cleanUser = username.trim().removePrefix("@").replace(Regex("[^a-zA-Z0-9_\\.]"), "")
        val cleanPhone = phoneNumber.trim().replace(Regex("[^+0-9\\-]"), "")
        val formattedBio = buildString {
            if (cleanUser.isNotBlank()) append("@$cleanUser | ")
            if (cleanPhone.isNotBlank()) append("tel:$cleanPhone | ")
            append(bio.trim())
        }.take(300)

        viewModelScope.launch {
            repository.ensureOrUpdateUserProfile(
                email = currentUserEmail,
                displayName = displayName,
                photoUrl = userProfile.value?.photoUrl ?: currentUserPhotoUrl,
                coverUrl = coverPreset,
                bio = formattedBio,
                isPrivate = isPrivate
            ).onSuccess {
                _statusBannerMessage.value = "Profile updated"
            }.onFailure { e ->
                _statusBannerMessage.value = "Profile update failed: ${e.localizedMessage}"
            }
        }
    }

    fun toggleBiometricGate(enabled: Boolean) {
        deviceIdentityManager.setBiometricGateEnabled(enabled)
        _statusBannerMessage.value = if (enabled) {
            "Biometric/PIN lock enabled"
        } else {
            "Biometric/PIN lock disabled"
        }
    }

    fun lockAppImmediately() {
        deviceIdentityManager.lockNow()
    }

    fun unlockWithPin(pin: String): Boolean {
        return deviceIdentityManager.unlockWithPin(pin)
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stopPlayback()
        audioEngine.stopRecording()
    }

    companion object {
        private const val TAG = "NeoTactileVM"
    }
}
