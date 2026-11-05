package com.example.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.TactileAudioEngine
import com.example.data.model.AppLanguage
import com.example.data.model.BlockedDevice
import com.example.data.model.CallLogEntry
import com.example.data.model.ChatMessage
import com.example.data.model.CommunityModel
import com.example.data.model.ContactRequestItem
import com.example.data.model.ResolvedContactProfile
import com.example.data.model.StoryItem
import com.example.data.model.UserProfile
import com.example.data.model.generateShortCode
import com.example.data.repository.NeoTactileRepository
import com.example.security.DeviceIdentityManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NeoTactileViewModel(
    private val repository: NeoTactileRepository,
    private val audioEngine: TactileAudioEngine,
    private val deviceIdentityManager: DeviceIdentityManager,
    val currentUserId: String,
    val currentUserEmail: String,
    val currentUserDisplayName: String,
    val currentUserPhotoUrl: String?
) : ViewModel() {

    val hardwareId: String = deviceIdentityManager.getHardwareAndroidId()
    val deviceModel: String = deviceIdentityManager.getDeviceModelName()
    val currentUserShortCode: String = generateShortCode(currentUserId)

    private val _appLanguage = MutableStateFlow(AppLanguage.BILINGUAL)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _isLightTheme = MutableStateFlow(false)
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

    // Active WebRTC Call State
    private val _activeCallSession = MutableStateFlow<CallLogEntry?>(null)
    val activeCallSession: StateFlow<CallLogEntry?> = _activeCallSession.asStateFlow()

    // Communities, Call Logs, Contact Requests, Starred/Pinned/Archived/Blocked
    val communities: StateFlow<List<CommunityModel>> = repository.communities
    val callLogs: StateFlow<List<CallLogEntry>> = repository.callLogs
    val contactRequests: StateFlow<List<ContactRequestItem>> = repository.contactRequests
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

    // Dual-Layer Resolved Contacts & Groups (Strict No-Demo Policy: Starts empty)
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
    val currentChatMessages: StateFlow<List<ChatMessage>> = _selectedPeerId
        .flatMapLatest { peerId ->
            if (peerId.isBlank()) {
                flowOf(emptyList())
            } else {
                val chatId = repository.buildConversationId(currentUserId, peerId)
                repository.observeMessages(chatId)
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

    init {
        initializeSession()
    }

    private fun initializeSession() {
        viewModelScope.launch {
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

    fun toggleThemeMode() {
        val next = !_isLightTheme.value
        _isLightTheme.value = next
        _statusBannerMessage.value = if (next) {
            "গ্রিন ও হোয়াইট লাইট থিম চালু হয়েছে (Light Theme)"
        } else {
            "এমেরাল্ড ডার্ক থিম চালু হয়েছে (Dark Theme)"
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
        _statusBannerMessage.value = "ভাষা পরিবর্তন হয়েছে: ${next.label}"
    }

    fun setAppLanguage(language: AppLanguage) {
        _appLanguage.value = language
        _statusBannerMessage.value = "ভাষা সেট করা হয়েছে: ${language.label}"
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
    // CHAT, REPLIES, EDITS, DELETES, FORWARD, STAR, REACTIONS & MEDIA
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
            _statusBannerMessage.value = "মেসেজ পাঠাতে প্রথমে একজন ইউজার অ্যাড বা সিলেক্ট করুন"
            return
        }

        val editing = _editingMessage.value
        if (editing != null) {
            val chatId = repository.buildConversationId(currentUserId, peerId)
            _editingMessage.value = null
            viewModelScope.launch {
                repository.editMessage(chatId, editing.messageId.ifBlank { editing.id }, trimmed)
                    .onSuccess {
                        _statusBannerMessage.value = "মেসেজ এডিট সম্পন্ন হয়েছে"
                    }
                    .onFailure { e ->
                        _statusBannerMessage.value = "এডিট ব্যর্থ: ${e.localizedMessage}"
                    }
            }
            return
        }

        val activeContact = resolvedContacts.value.find { it.userId == peerId }
        if (activeContact?.isBlocked == true) {
            _statusBannerMessage.value = "এই ইউজার ব্লক করা আছেন। আনব্লক করে মেসেজ পাঠান।"
            return
        }
        val chatId = repository.buildConversationId(currentUserId, peerId)
        val senderName = userProfile.value?.displayName?.ifBlank { currentUserDisplayName } ?: currentUserDisplayName
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
                _statusBannerMessage.value = "মেসেজ পাঠানো যায়নি: ${e.localizedMessage}"
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
                content = "↪ ফরোয়ার্ড করা মেসেজ: ${message.content}",
                mediaType = message.mediaType,
                mediaUrl = message.mediaUrl,
                fileName = message.fileName,
                audioDurationMs = message.audioDurationMs,
                participantIds = targetContact?.memberIds
            ).onSuccess {
                _statusBannerMessage.value = "মেসেজ ফরোয়ার্ড করা হয়েছে"
            }.onFailure { e ->
                _statusBannerMessage.value = "ফরোয়ার্ড ব্যর্থ: ${e.localizedMessage}"
            }
        }
    }

    fun toggleStarMessage(messageId: String) {
        val isStarred = repository.toggleStarMessage(messageId)
        _statusBannerMessage.value = if (isStarred) "মেসেজ স্টার করা হয়েছে ⭐" else "স্টার সরানো হয়েছে"
    }

    fun togglePinConversation(peerId: String) {
        val isPinned = repository.togglePinChat(peerId)
        _statusBannerMessage.value = if (isPinned) "চ্যাট পিন করা হয়েছে 📌" else "চ্যাট আনপিন করা হয়েছে"
    }

    fun toggleArchiveConversation(peerId: String) {
        val isArchived = repository.toggleArchiveChat(peerId)
        _statusBannerMessage.value = if (isArchived) "চ্যাট আর্কাইভ করা হয়েছে" else "আর্কাইভ থেকে সরানো হয়েছে"
    }

    fun toggleBlockContact(peerId: String) {
        val isBlocked = repository.toggleBlockUser(peerId)
        _statusBannerMessage.value = if (isBlocked) "ইউজার ব্লক করা হয়েছে 🚫" else "ইউজার আনব্লক করা হয়েছে"
    }

    fun deleteChatMessage(message: ChatMessage) {
        val peerId = _selectedPeerId.value
        if (peerId.isBlank()) return
        val chatId = repository.buildConversationId(currentUserId, peerId)
        val msgId = message.messageId.ifBlank { message.id }
        viewModelScope.launch {
            repository.deleteMessage(chatId, msgId)
                .onSuccess {
                    _statusBannerMessage.value = "মেসেজ মুছে ফেলা হয়েছে"
                }
                .onFailure { e ->
                    _statusBannerMessage.value = "ডিলিট ত্রুটি: ${e.localizedMessage}"
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
        val replyMsg = _replyingToMessage.value
        _replyingToMessage.value = null

        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                receiverId = peerId,
                senderName = senderName,
                content = fileDescription,
                mediaType = "FILE",
                fileName = fileName,
                mediaUrl = "file://attachment/$fileName",
                participantIds = activeContact?.memberIds,
                replyToMessageId = replyMsg?.messageId?.ifBlank { replyMsg.id },
                replyToSenderName = replyMsg?.senderName,
                replyToPreview = replyMsg?.content?.take(100)
            ).onSuccess {
                _statusBannerMessage.value = "ফাইল শেয়ার করা হয়েছে: $fileName"
            }.onFailure { e ->
                _statusBannerMessage.value = "ফাইল শেয়ার ত্রুটি: ${e.localizedMessage}"
            }
        }
    }

    fun startVoiceNoteRecording() {
        audioEngine.startRecording()
    }

    fun stopAndSendVoiceNote() {
        val (file, durationMs) = audioEngine.stopRecording()
        if (file == null) return
        val peerId = _selectedPeerId.value
        if (peerId.isBlank()) return
        val activeContact = resolvedContacts.value.find { it.userId == peerId }
        val chatId = repository.buildConversationId(currentUserId, peerId)
        val senderName = userProfile.value?.displayName?.ifBlank { currentUserDisplayName } ?: currentUserDisplayName
        val fileUri = Uri.fromFile(file).toString()
        val replyMsg = _replyingToMessage.value
        _replyingToMessage.value = null

        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                receiverId = peerId,
                senderName = senderName,
                content = "ভয়েস মেসেজ (${(durationMs / 1000).coerceAtLeast(1)} সেকেন্ড)",
                mediaType = "AUDIO",
                mediaUrl = fileUri,
                audioDurationMs = durationMs,
                participantIds = activeContact?.memberIds,
                replyToMessageId = replyMsg?.messageId?.ifBlank { replyMsg.id },
                replyToSenderName = replyMsg?.senderName,
                replyToPreview = replyMsg?.content?.take(100)
            ).onSuccess {
                _statusBannerMessage.value = "ভয়েস মেসেজ পাঠানো হয়েছে"
            }.onFailure { e ->
                _statusBannerMessage.value = "ভয়েস মেসেজ ত্রুটি: ${e.localizedMessage}"
            }
        }
    }

    fun cancelVoiceNoteRecording() {
        audioEngine.stopRecording()
    }

    fun toggleVoicePlayback(mediaUrl: String, durationMs: Long) {
        audioEngine.togglePlayback(mediaUrl, durationMs)
    }

    fun seekVoicePlayback(fraction: Float) {
        audioEngine.seekToFraction(fraction)
    }

    fun compressAndSendPhoto(uri: Uri, caption: String = "শেয়ার করা ছবি (Photo)") {
        val peerId = _selectedPeerId.value
        if (peerId.isBlank()) return
        val activeContact = resolvedContacts.value.find { it.userId == peerId }
        val chatId = repository.buildConversationId(currentUserId, peerId)
        val senderName = userProfile.value?.displayName?.ifBlank { currentUserDisplayName } ?: currentUserDisplayName
        val replyMsg = _replyingToMessage.value
        _replyingToMessage.value = null

        viewModelScope.launch {
            val compressedUriString = audioEngine.compressAndCachePhoto(uri)
            repository.sendMessage(
                chatId = chatId,
                receiverId = peerId,
                senderName = senderName,
                content = caption,
                mediaType = "IMAGE",
                mediaUrl = compressedUriString,
                participantIds = activeContact?.memberIds,
                replyToMessageId = replyMsg?.messageId?.ifBlank { replyMsg.id },
                replyToSenderName = replyMsg?.senderName,
                replyToPreview = replyMsg?.content?.take(100)
            ).onSuccess {
                _statusBannerMessage.value = "ছবি কম্প্রেস করে পাঠানো হয়েছে"
            }.onFailure { e ->
                _statusBannerMessage.value = "ছবি পাঠানো যায়নি: ${e.localizedMessage}"
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
                signalState = "RINGING"
            ).onSuccess { entry ->
                _activeCallSession.value = entry.copy(status = "CONNECTED")
                _statusBannerMessage.value = "WebRTC ${if (callType == "VIDEO") "ভিডিও" else "অডিও"} কল সংযুক্ত হচ্ছে..."
            }.onFailure { e ->
                _statusBannerMessage.value = "কল সংযোগ ত্রুটি: ${e.localizedMessage}"
            }
        }
    }

    fun endActiveWebRtcCall(durationSeconds: Int) {
        val active = _activeCallSession.value ?: return
        _activeCallSession.value = null
        val senderName = userProfile.value?.displayName?.ifBlank { currentUserDisplayName } ?: currentUserDisplayName
        viewModelScope.launch {
            repository.sendWebRtcCallSignal(
                peerId = active.peerId,
                peerName = active.peerName,
                senderName = senderName,
                callType = active.callType,
                signalState = "ENDED",
                durationSeconds = durationSeconds
            )
            _statusBannerMessage.value = "কল সমাপ্ত হয়েছে (${durationSeconds}s)"
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
            _statusBannerMessage.value = "লোকাল নাম (Alias) ডিভাইসে সেভ হয়েছে"
        }
    }

    fun clearLocalAlias(targetUserId: String) {
        viewModelScope.launch {
            repository.clearLocalAliasOverride(targetUserId)
            _statusBannerMessage.value = "লোকাল নাম মুছে মূল নাম ফিরিয়ে আনা হয়েছে"
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
        roleTag: String = "ভেরিফাইড ইউজার (Verified Contact)",
        localAliasName: String? = null
    ) {
        val query = targetUserId.trim()
        if (query.isBlank()) {
            _statusBannerMessage.value = "ইউজারনেম, ইমেইল, ফোন নম্বর বা শর্ট আইডি লিখুন"
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
                _statusBannerMessage.value = "${added.effectiveDisplayName} আপনার কন্টাক্ট তালিকায় যুক্ত হয়েছেন"
            }.onFailure { e ->
                _statusBannerMessage.value = "ইউজার অ্যাড করতে সমস্যা: ${e.localizedMessage}"
            }
        }
    }

    fun sendFriendRequestToUser(targetUser: UserProfile) {
        val senderName = userProfile.value?.displayName?.ifBlank { currentUserDisplayName } ?: currentUserDisplayName
        viewModelScope.launch {
            repository.sendContactRequest(targetUser, senderName)
                .onSuccess {
                    _selectedPeerId.value = targetUser.uid
                    _statusBannerMessage.value = "${targetUser.displayName}-কে কন্টাক্ট হিসেবে যুক্ত করা হয়েছে"
                }
                .onFailure { e ->
                    _statusBannerMessage.value = "রিকোয়েস্ট পাঠাতে সমস্যা: ${e.localizedMessage}"
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
            _statusBannerMessage.value = "গ্রুপের একটি নাম দিন"
            return
        }
        viewModelScope.launch {
            repository.createGroupContact(cleanName, selectedMemberIds, description)
                .onSuccess { group ->
                    _selectedPeerId.value = group.userId
                    _statusBannerMessage.value = "নতুন গ্রুপ '$cleanName' তৈরি হয়েছে"
                }
                .onFailure { e ->
                    _statusBannerMessage.value = "গ্রুপ তৈরি ব্যর্থ: ${e.localizedMessage}"
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
            _statusBannerMessage.value = "কমিউনিটির একটি নাম দিন"
            return
        }
        viewModelScope.launch {
            repository.createCommunity(cleanName, description, announcement, groupNames, memberIds)
                .onSuccess {
                    _statusBannerMessage.value = "নতুন কমিউনিটি '$cleanName' তৈরি হয়েছে"
                }
                .onFailure { e ->
                    _statusBannerMessage.value = "কমিউনিটি তৈরি ব্যর্থ: ${e.localizedMessage}"
                }
        }
    }

    fun postCommunityAnnouncement(communityId: String, announcementText: String) {
        val senderName = userProfile.value?.displayName?.ifBlank { currentUserDisplayName } ?: currentUserDisplayName
        viewModelScope.launch {
            repository.sendCommunityAnnouncement(communityId, senderName, announcementText)
                .onSuccess {
                    _statusBannerMessage.value = "কমিউনিটি ঘোষণা সব গ্রুপে পাঠানো হয়েছে"
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
                    _statusBannerMessage.value = "কন্টাক্ট তালিকা থেকে সরানো হয়েছে"
                }
                .onFailure { e ->
                    _statusBannerMessage.value = "সরাতে সমস্যা: ${e.localizedMessage}"
                }
        }
    }

    // =========================================================================
    // 24-HOUR EPHEMERAL STORIES / STATUS UPDATES
    // =========================================================================
    fun publishEphemeralStory(
        caption: String,
        mediaPreset: String = "preset://emerald_status",
        mediaType: String = "TEXT_CARD"
    ) {
        val trimmed = caption.trim().ifBlank { "২৪ ঘণ্টার নতুন স্ট্যাটাস আপডেট" }
        val profile = userProfile.value
        val authorName = profile?.displayName?.ifBlank { currentUserDisplayName } ?: currentUserDisplayName
        val peerIds = resolvedContacts.value.map { it.userId }

        viewModelScope.launch {
            repository.publishStory(
                authorName = authorName,
                authorAvatarUrl = profile?.photoUrl ?: currentUserPhotoUrl,
                mediaUrl = mediaPreset,
                mediaType = mediaType,
                caption = trimmed,
                visiblePeerIds = peerIds
            ).onSuccess {
                _statusBannerMessage.value = "২৪ ঘণ্টার স্ট্যাটাস আপডেট প্রকাশিত হয়েছে"
            }.onFailure { e ->
                _statusBannerMessage.value = "স্ট্যাটাস ত্রুটি: ${e.localizedMessage}"
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
                _statusBannerMessage.value = "প্রোফাইল ও ইউজারনেম সফলভাবে সেভ হয়েছে"
            }.onFailure { e ->
                _statusBannerMessage.value = "প্রোফাইল আপডেট ব্যর্থ: ${e.localizedMessage}"
            }
        }
    }

    fun toggleBiometricGate(enabled: Boolean) {
        deviceIdentityManager.setBiometricGateEnabled(enabled)
        _statusBannerMessage.value = if (enabled) {
            "বায়োমেট্রিক/পিন লক চালু করা হয়েছে"
        } else {
            "বায়োমেট্রিক/পিন লক বন্ধ করা হয়েছে"
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
