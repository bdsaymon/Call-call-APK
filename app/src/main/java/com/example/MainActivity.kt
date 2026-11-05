package com.example

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.AppLanguage
import com.example.di.AppModule
import com.example.security.DeviceIdentityManager
import com.example.ui.auth.LoginScreen
import com.example.ui.auth.VerifiedAccountStore
import com.example.ui.auth.areAllPermissionsGranted
import com.example.ui.auth.getRequiredRuntimePermissions
import com.example.ui.auth.performGoogleSignOut
import com.example.ui.screens.BiometricLockScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DeviceBannedScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsSubRoute
import com.example.ui.screens.UsersDirectoryScreen
import com.example.ui.theme.LightBackgroundWhite
import com.example.ui.theme.LightDividerGray
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.theme.NeoTactileTheme
import com.example.ui.theme.WhatsAppAccentGreen
import com.example.ui.theme.WhatsAppActionGreen
import com.example.ui.theme.WhatsAppDarkGreen
import com.example.ui.theme.WhatsAppHeaderGreen
import com.example.ui.viewmodel.NeoTactileViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class NeoTactileRoute(val routeId: String) {
    HOME("chats"),          // Screen 1: Chats (Main Page)
    UPDATES("updates"),     // Screen 6: Updates (Status)
    PEERS("communities"),   // Screen 10: Communities
    CALLS("calls"),         // Screen 13: Calls
    CHAT("open_chat"),      // Screen 2 & 3: Open Conversation / Group Chat
    PROFILE("settings")     // Screen 17-30: Settings & Profile Sub-Screens
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : FragmentActivity() {

    private lateinit var deviceIdentityManager: DeviceIdentityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deviceIdentityManager = AppModule.provideDeviceIdentityManager(this)
        enableEdgeToEdge()
        setContent {
            NeoTactileTheme(isLightTheme = true) {
                NeoTactileRootApp(deviceIdentityManager = deviceIdentityManager)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (::deviceIdentityManager.isInitialized) {
            deviceIdentityManager.onAppBackgrounded()
        }
    }

    override fun onStart() {
        super.onStart()
        if (::deviceIdentityManager.isInitialized) {
            deviceIdentityManager.onAppResumed()
        }
    }
}

@Composable
fun NeoTactileRootApp(
    deviceIdentityManager: DeviceIdentityManager,
    auth: FirebaseAuth = Firebase.auth
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val hardwareId = remember { deviceIdentityManager.getHardwareAndroidId() }
    val user = currentUser

    if (user == null) {
        LoginScreen(
            hardwareId = hardwareId,
            onAuthSuccess = {},
            onManualVerifiedSuccess = {}
        )
    } else {
        val effectiveUid = user.uid
        val effectiveEmail = user.email ?: "operator@neotactile.app"
        val effectiveDisplayName = user.displayName?.ifBlank { null }
            ?: effectiveEmail.substringBefore("@").uppercase()
        val effectivePhotoUrl = user.photoUrl?.toString()

        AuthenticatedMainShell(
            currentUserId = effectiveUid,
            currentUserEmail = effectiveEmail,
            currentUserDisplayName = effectiveDisplayName,
            currentUserPhotoUrl = effectivePhotoUrl,
            deviceIdentityManager = deviceIdentityManager,
            onSignOutComplete = {}
        )
    }
}

@Composable
private fun AuthenticatedMainShell(
    currentUserId: String,
    currentUserEmail: String,
    currentUserDisplayName: String,
    currentUserPhotoUrl: String?,
    deviceIdentityManager: DeviceIdentityManager,
    onSignOutComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var allPermissionsGranted by remember {
        mutableStateOf(areAllPermissionsGranted(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        allPermissionsGranted = areAllPermissionsGranted(context)
    }

    LaunchedEffect(Unit) {
        if (!allPermissionsGranted) {
            permissionLauncher.launch(getRequiredRuntimePermissions().toTypedArray())
        }
    }

    val viewModel: NeoTactileViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                NeoTactileViewModel(
                    repository = AppModule.provideRepository(app),
                    audioEngine = AppModule.provideAudioEngine(app),
                    deviceIdentityManager = AppModule.provideDeviceIdentityManager(app),
                    currentUserId = currentUserId,
                    currentUserEmail = currentUserEmail,
                    currentUserDisplayName = currentUserDisplayName,
                    currentUserPhotoUrl = currentUserPhotoUrl,
                    appContext = app
                )
            }
        }
    )

    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isLightTheme by viewModel.isLightTheme.collectAsStateWithLifecycle()
    val deviceGatekeeper by viewModel.currentDeviceGatekeeper.collectAsStateWithLifecycle()
    val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
    val biometricLockEnabled by viewModel.biometricLockEnabled.collectAsStateWithLifecycle()
    val deviceStorageStats by viewModel.deviceStorageStats.collectAsStateWithLifecycle()
    val savedMediaFiles by viewModel.savedMediaFiles.collectAsStateWithLifecycle()
    val deviceContacts by viewModel.deviceContacts.collectAsStateWithLifecycle()
    val linkedDevices by viewModel.linkedDevices.collectAsStateWithLifecycle()
    val broadcastLists by viewModel.broadcastLists.collectAsStateWithLifecycle()
    val activeCallSession by viewModel.activeCallSession.collectAsStateWithLifecycle()
    val incomingCallSession by viewModel.incomingCallSession.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val resolvedContacts by viewModel.resolvedContacts.collectAsStateWithLifecycle()
    val communities by viewModel.communities.collectAsStateWithLifecycle()
    val callLogs by viewModel.callLogs.collectAsStateWithLifecycle()
    val blockedUserIds by viewModel.blockedUserIds.collectAsStateWithLifecycle()
    val selectedPeerId by viewModel.selectedPeerId.collectAsStateWithLifecycle()
    val chatMessages by viewModel.currentChatMessages.collectAsStateWithLifecycle()
    val activeStories by viewModel.activeStories.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val activeStoryViewerItem by viewModel.activeStoryViewerItem.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val discoverableUsers by viewModel.discoverableUsers.collectAsStateWithLifecycle()

    val replyingToMessage by viewModel.replyingToMessage.collectAsStateWithLifecycle()
    val editingMessage by viewModel.editingMessage.collectAsStateWithLifecycle()
    val fullscreenMediaMessage by viewModel.fullscreenMediaMessage.collectAsStateWithLifecycle()

    val isRecordingVoice by viewModel.isRecordingVoice.collectAsStateWithLifecycle()
    val liveVoiceAmplitude by viewModel.liveVoiceAmplitude.collectAsStateWithLifecycle()
    val recordingElapsedMs by viewModel.recordingElapsedMs.collectAsStateWithLifecycle()
    val activePlayingUrl by viewModel.activePlayingUrl.collectAsStateWithLifecycle()
    val isPlayingVoice by viewModel.isPlayingVoice.collectAsStateWithLifecycle()
    val playbackProgress by viewModel.playbackProgress.collectAsStateWithLifecycle()
    val playbackAmplitude by viewModel.playbackAmplitude.collectAsStateWithLifecycle()

    var currentRoute by rememberSaveable { mutableStateOf(NeoTactileRoute.HOME) }
    var targetSettingsSubRoute by remember { mutableStateOf(SettingsSubRoute.MAIN_SETTINGS) }
    var externalCreateStatusMode by remember { mutableStateOf<String?>(null) }

    // 1. Hardware Device Gatekeeper Check
    val gatekeeper = deviceGatekeeper
    if (gatekeeper != null && gatekeeper.banned) {
        DeviceBannedScreen(
            device = gatekeeper,
            isAuthorizedAdmin = false,
            onEmergencyAdminUnban = {}
        )
        return
    }

    // 2. Biometric & PIN Security Gate Check
    if (isAppLocked) {
        BiometricLockScreen(
            hardwareId = viewModel.hardwareId,
            deviceIdentityManager = deviceIdentityManager,
            onUnlockWithPin = { pin -> viewModel.unlockWithPin(pin) }
        )
        return
    }

    // BackHandler for secondary tabs returning to HOME (Chats)
    if (currentRoute != NeoTactileRoute.HOME) {
        BackHandler {
            currentRoute = NeoTactileRoute.HOME
        }
    }

    val isFullScreenRoute = currentRoute == NeoTactileRoute.CHAT ||
        activeStoryViewerItem != null ||
        externalCreateStatusMode != null

    NeoTactileTheme(isLightTheme = isLightTheme) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(LightBackgroundWhite),
            containerColor = LightBackgroundWhite,
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                if (!isFullScreenRoute) {
                    WhatsAppReferenceTopBar(
                        currentRoute = currentRoute,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                        statusBanner = statusBanner,
                        allPermissionsGranted = allPermissionsGranted,
                        onRequestPermissions = {
                            permissionLauncher.launch(getRequiredRuntimePermissions().toTypedArray())
                        },
                        onSelectRoute = { route ->
                            if (route == NeoTactileRoute.PROFILE) {
                                targetSettingsSubRoute = SettingsSubRoute.MAIN_SETTINGS
                            }
                            currentRoute = route
                        },
                        onOpenSettingsSubRoute = { subRoute ->
                            targetSettingsSubRoute = subRoute
                            currentRoute = NeoTactileRoute.PROFILE
                        },
                        onOpenCameraStatus = {
                            currentRoute = NeoTactileRoute.UPDATES
                            externalCreateStatusMode = "PHOTO"
                        },
                        onDismissStatus = { viewModel.clearStatusBanner() }
                    )
                }
            },
            bottomBar = {
                if (!isFullScreenRoute) {
                    WhatsAppReferenceBottomBar(
                        currentRoute = currentRoute,
                        onSelectRoute = { route ->
                            if (route == NeoTactileRoute.PROFILE) {
                                targetSettingsSubRoute = SettingsSubRoute.MAIN_SETTINGS
                            }
                            currentRoute = route
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentRoute) {
                    NeoTactileRoute.HOME -> {
                        HomeScreen(
                            language = appLanguage,
                            stories = activeStories,
                            contacts = resolvedContacts,
                            callLogs = callLogs,
                            activeStoryViewerItem = activeStoryViewerItem,
                            onPublishStory = { caption, preset ->
                                viewModel.publishEphemeralStory(caption, preset)
                            },
                            onOpenStory = { story -> viewModel.openStoryViewer(story) },
                            onCloseStory = { viewModel.closeStoryViewer() },
                            onSelectContactForChat = { peerId ->
                                viewModel.selectPeer(peerId)
                                currentRoute = NeoTactileRoute.CHAT
                            },
                            onNavigateToDirectory = {
                                currentRoute = NeoTactileRoute.PEERS
                            },
                            onTogglePinContact = { peerId ->
                                viewModel.togglePinConversation(peerId)
                            },
                            onStartCallFromHome = { peerId, peerName, callType ->
                                viewModel.selectPeer(peerId)
                                viewModel.startWebRtcCall(peerId, peerName, callType)
                                currentRoute = NeoTactileRoute.CHAT
                            },
                            activeSubTab = 0,
                            searchQuery = searchQuery,
                            externalCreateStatusMode = externalCreateStatusMode,
                            onDismissExternalCreateStatus = { externalCreateStatusMode = null }
                        )
                    }

                    NeoTactileRoute.UPDATES -> {
                        HomeScreen(
                            language = appLanguage,
                            stories = activeStories,
                            contacts = resolvedContacts,
                            callLogs = callLogs,
                            communities = communities,
                            activeStoryViewerItem = activeStoryViewerItem,
                            onPublishStory = { caption, preset ->
                                viewModel.publishEphemeralStory(caption, preset)
                            },
                            onOpenStory = { story -> viewModel.openStoryViewer(story) },
                            onCloseStory = { viewModel.closeStoryViewer() },
                            onSelectContactForChat = { peerId ->
                                viewModel.selectPeer(peerId)
                                currentRoute = NeoTactileRoute.CHAT
                            },
                            onNavigateToDirectory = {
                                currentRoute = NeoTactileRoute.PEERS
                            },
                            onTogglePinContact = { peerId ->
                                viewModel.togglePinConversation(peerId)
                            },
                            onStartCallFromHome = { peerId, peerName, callType ->
                                viewModel.startWebRtcCall(peerId, peerName, callType)
                            },
                            activeSubTab = 1,
                            searchQuery = searchQuery,
                            externalCreateStatusMode = externalCreateStatusMode,
                            onDismissExternalCreateStatus = { externalCreateStatusMode = null }
                        )
                    }

                    NeoTactileRoute.CALLS -> {
                        HomeScreen(
                            language = appLanguage,
                            stories = activeStories,
                            contacts = resolvedContacts,
                            callLogs = callLogs,
                            activeStoryViewerItem = activeStoryViewerItem,
                            onPublishStory = { caption, preset ->
                                viewModel.publishEphemeralStory(caption, preset)
                            },
                            onOpenStory = { story -> viewModel.openStoryViewer(story) },
                            onCloseStory = { viewModel.closeStoryViewer() },
                            onSelectContactForChat = { peerId ->
                                viewModel.selectPeer(peerId)
                                currentRoute = NeoTactileRoute.CHAT
                            },
                            onNavigateToDirectory = {
                                currentRoute = NeoTactileRoute.PEERS
                            },
                            onTogglePinContact = { peerId ->
                                viewModel.togglePinConversation(peerId)
                            },
                            onStartCallFromHome = { peerId, peerName, callType ->
                                viewModel.selectPeer(peerId)
                                viewModel.startWebRtcCall(peerId, peerName, callType)
                                currentRoute = NeoTactileRoute.CHAT
                            },
                            activeSubTab = 2,
                            searchQuery = searchQuery,
                            externalCreateStatusMode = externalCreateStatusMode,
                            onDismissExternalCreateStatus = { externalCreateStatusMode = null }
                        )
                    }

                    NeoTactileRoute.CHAT -> {
                        ChatScreen(
                            language = appLanguage,
                            currentUserId = viewModel.currentUserId,
                            contacts = resolvedContacts,
                            selectedPeerId = selectedPeerId,
                            messages = chatMessages,
                            replyingToMessage = replyingToMessage,
                            editingMessage = editingMessage,
                            fullscreenMediaMessage = fullscreenMediaMessage,
                            isRecordingVoice = isRecordingVoice,
                            liveVoiceAmplitude = liveVoiceAmplitude,
                            recordingElapsedMs = recordingElapsedMs,
                            activePlayingUrl = activePlayingUrl,
                            isPlayingVoice = isPlayingVoice,
                            playbackProgress = playbackProgress,
                            playbackAmplitude = playbackAmplitude,
                            onSelectPeer = { viewModel.selectPeer(it) },
                            onSendText = { viewModel.sendTextMessage(it) },
                            onStartReply = { viewModel.startReply(it) },
                            onStartEdit = { viewModel.startEdit(it) },
                            onCancelReplyOrEdit = { viewModel.cancelReplyOrEdit() },
                            onDeleteMessage = { viewModel.deleteChatMessage(it) },
                            onReactToMessage = { msg, emoji -> viewModel.reactToMessage(msg, emoji) },
                            onSendFileAttachment = { name, desc -> viewModel.sendFileAttachment(name, desc) },
                            onOpenFullscreenMedia = { viewModel.openFullscreenMedia(it) },
                            onStartVoiceRecording = { viewModel.startVoiceNoteRecording() },
                            onStopAndSendVoice = { viewModel.stopAndSendVoiceNote() },
                            onCancelVoiceRecording = { viewModel.cancelVoiceNoteRecording() },
                            onToggleVoicePlayback = { url, dur -> viewModel.toggleVoicePlayback(url, dur) },
                            onSeekVoicePlayback = { frac -> viewModel.seekVoicePlayback(frac) },
                            onPickAndCompressPhoto = { uri -> viewModel.compressAndSendPhoto(uri) },
                            onSaveLocalAlias = { targetId, name, avatar ->
                                viewModel.saveLocalAlias(targetId, name, avatar)
                            },
                            onClearLocalAlias = { targetId ->
                                viewModel.clearLocalAlias(targetId)
                            },
                            onNavigateToDirectory = {
                                currentRoute = NeoTactileRoute.PEERS
                            },
                            onStartWebRtcCall = { peerId, peerName, callType ->
                                viewModel.startWebRtcCall(peerId, peerName, callType)
                            },
                            onEndWebRtcCall = { elapsedSec ->
                                viewModel.endActiveWebRtcCall(elapsedSec)
                            },
                            onTypingChanged = { text ->
                                viewModel.onMessageInputChanged(text)
                            },
                            activeCallSession = activeCallSession,
                            onBackToChatsList = {
                                currentRoute = NeoTactileRoute.HOME
                            }
                        )
                    }

                    NeoTactileRoute.PEERS -> {
                        UsersDirectoryScreen(
                            language = appLanguage,
                            currentUserId = viewModel.currentUserId,
                            currentUserName = userProfile?.displayName?.ifBlank { viewModel.currentUserDisplayName }
                                ?: viewModel.currentUserDisplayName,
                            contacts = resolvedContacts,
                            discoverableUsers = discoverableUsers,
                            communities = communities,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                            onAddContact = { id, name, avatar, role, alias ->
                                viewModel.addNewPeerContact(id, name, avatar, role, alias)
                            },
                            onCreateGroup = { gName, members, desc ->
                                viewModel.createNewGroupChat(gName, members, desc)
                                currentRoute = NeoTactileRoute.CHAT
                            },
                            onCreateCommunity = { name, desc, announcement, groupNames, memberIds ->
                                viewModel.createNewCommunity(name, desc, announcement, groupNames, memberIds)
                            },
                            onPostAnnouncement = { commId, text ->
                                viewModel.postCommunityAnnouncement(commId, text)
                            },
                            onToggleBlockUser = { peerId ->
                                viewModel.toggleBlockContact(peerId)
                            },
                            onRemoveContact = { viewModel.removePeerContact(it) },
                            onSaveLocalAlias = { id, name, avatar ->
                                viewModel.saveLocalAlias(id, name, avatar)
                            },
                            onClearLocalAlias = { viewModel.clearLocalAlias(it) },
                            onSelectContactForChat = { peerId ->
                                viewModel.selectPeer(peerId)
                                currentRoute = NeoTactileRoute.CHAT
                            }
                        )
                    }

                    NeoTactileRoute.PROFILE -> {
                        ProfileScreen(
                            language = appLanguage,
                            onSelectLanguage = { viewModel.setAppLanguage(it) },
                            isLightTheme = isLightTheme,
                            onToggleLightTheme = { viewModel.setLightTheme(it) },
                            blockedCount = blockedUserIds.size,
                            deviceStorageStats = deviceStorageStats,
                            allPermissionsGranted = allPermissionsGranted,
                            onRequestPermissions = {
                                permissionLauncher.launch(getRequiredRuntimePermissions().toTypedArray())
                            },
                            userProfile = userProfile,
                            currentUserId = viewModel.currentUserId,
                            fallbackEmail = viewModel.currentUserEmail,
                            fallbackName = viewModel.currentUserDisplayName,
                            hardwareId = viewModel.hardwareId,
                            deviceModel = viewModel.deviceModel,
                            biometricGateEnabled = biometricLockEnabled,
                            onUpdateProfile = { name, bio, isPriv, cover, username, phone ->
                                viewModel.updateProfileDetails(name, bio, isPriv, cover, username, phone)
                            },
                            onToggleBiometricGate = { viewModel.toggleBiometricGate(it) },
                            onLockNow = { viewModel.lockAppImmediately() },
                            onSignOut = {
                                performGoogleSignOut(
                                    context = context,
                                    onSignOutComplete = onSignOutComplete,
                                    scope = scope
                                )
                            },
                            linkedDevices = linkedDevices,
                            broadcastLists = broadcastLists,
                            deviceContacts = deviceContacts,
                            savedMediaFiles = savedMediaFiles,
                            contacts = resolvedContacts,
                            onRemoveLinkedDevice = { viewModel.removeLinkedDevice(it) },
                            onCreateBroadcastList = { name, ids -> viewModel.createNewBroadcastList(name, ids) },
                            onSendBroadcastMessage = { doc, text -> viewModel.sendBroadcastToList(doc, text) },
                            onRefreshContacts = { viewModel.refreshDeviceStorageStats() },
                            onDeleteMediaFile = { file ->
                                runCatching { file.delete() }
                                viewModel.refreshDeviceStorageStats()
                            },
                            onAddContactFromQr = { code ->
                                viewModel.addNewPeerContact(code, code, "preset://avatar_1", "Contact", null)
                            },
                            initialSubRoute = targetSettingsSubRoute
                        )
                    }
                }

                // Real-time Incoming Call Signaling Modal (Cross-Device Callee Alert)
                val incomingCall = incomingCallSession
                if (incomingCall != null) {
                    AlertDialog(
                        onDismissRequest = {},
                        title = {
                            Text(
                                text = "Incoming ${incomingCall.callType.lowercase().replaceFirstChar { it.uppercase() }} Call",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Text(
                                text = "${incomingCall.callerName} is calling you via encrypted WebRTC signaling."
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.acceptIncomingWebRtcCall()
                                    currentRoute = NeoTactileRoute.CHAT
                                }
                            ) {
                                Text("Accept", color = WhatsAppActionGreen, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    viewModel.declineIncomingWebRtcCall()
                                }
                            ) {
                                Text("Decline", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Matches the Green WhatsApp Header Bar in WhatsApp_UI_Reference_1.png & WhatsApp_UI_Reference_2.png:
 * - Row 1: "WhatsApp" (or current tab title) on left; Camera, Search, More (3-dots) on right.
 * - Row 2: [Camera] | Chats | Updates | Communities | Calls tab bar with white bottom underline.
 */
@Composable
private fun WhatsAppReferenceTopBar(
    currentRoute: NeoTactileRoute,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    statusBanner: String?,
    allPermissionsGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onSelectRoute: (NeoTactileRoute) -> Unit,
    onOpenSettingsSubRoute: (SettingsSubRoute) -> Unit,
    onOpenCameraStatus: () -> Unit,
    onDismissStatus: () -> Unit
) {
    var showSearchField by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WhatsAppHeaderGreen)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Row 1: Title + Camera, Search, More Icons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (showSearchField) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = LightTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        textStyle = TextStyle(color = LightTextPrimary, fontSize = 15.sp),
                        cursorBrush = SolidColor(WhatsAppActionGreen),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            onSearchQueryChange("")
                            showSearchField = false
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Search",
                            tint = LightTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = when (currentRoute) {
                        NeoTactileRoute.UPDATES -> "Updates"
                        NeoTactileRoute.PEERS -> "Communities"
                        NeoTactileRoute.CALLS -> "Calls"
                        NeoTactileRoute.PROFILE -> "Settings"
                        else -> "WhatsApp"
                    },
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenCameraStatus) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera",
                        tint = Color.White
                    )
                }

                IconButton(onClick = { showSearchField = !showSearchField }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White
                    )
                }

                Box {
                    IconButton(
                        onClick = { showOverflowMenu = true },
                        modifier = Modifier.testTag("top_bar_more_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("New group") },
                            onClick = {
                                showOverflowMenu = false
                                onSelectRoute(NeoTactileRoute.PEERS)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("New broadcast") },
                            onClick = {
                                showOverflowMenu = false
                                onOpenSettingsSubRoute(SettingsSubRoute.BROADCAST_LISTS)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Linked devices") },
                            onClick = {
                                showOverflowMenu = false
                                onOpenSettingsSubRoute(SettingsSubRoute.LINKED_DEVICES)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Starred messages") },
                            onClick = {
                                showOverflowMenu = false
                                onOpenSettingsSubRoute(SettingsSubRoute.STARRED_MESSAGES)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("My QR code") },
                            onClick = {
                                showOverflowMenu = false
                                onOpenSettingsSubRoute(SettingsSubRoute.QR_CODE)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Invite friends") },
                            onClick = {
                                showOverflowMenu = false
                                onOpenSettingsSubRoute(SettingsSubRoute.INVITE_FRIENDS)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                showOverflowMenu = false
                                onOpenSettingsSubRoute(SettingsSubRoute.MAIN_SETTINGS)
                            }
                        )
                    }
                }
            }
        }

        // Row 2: [Camera] | Chats | Updates | Communities | Calls Tab Strip inside Green Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clickable { onOpenCameraStatus() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Status Camera",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }

            val headerTabs = listOf(
                NeoTactileRoute.HOME to "Chats",
                NeoTactileRoute.UPDATES to "Updates",
                NeoTactileRoute.PEERS to "Communities",
                NeoTactileRoute.CALLS to "Calls"
            )

            headerTabs.forEach { (route, title) ->
                val isSelected = currentRoute == route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectRoute(route) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.75f),
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(if (isSelected) Color.White else Color.Transparent)
                    )
                }
            }
        }

        // Permission Verification Banner (only shown if any runtime permission is not yet granted)
        AnimatedVisibility(visible = !allPermissionsGranted) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE7FCE8))
                    .clickable { onRequestPermissions() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("main_permission_banner"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = WhatsAppHeaderGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Allow Camera, Mic & Notification permissions",
                        fontSize = 12.sp,
                        color = WhatsAppDarkGreen
                    )
                }
                Text(
                    text = "Allow",
                    fontSize = 12.sp,
                    color = WhatsAppActionGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Transient Action Feedback Toast Banner
        AnimatedVisibility(visible = !statusBanner.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFD9FDD3))
                    .clickable { onDismissStatus() }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = statusBanner ?: "",
                    fontSize = 12.sp,
                    color = WhatsAppDarkGreen,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Matches the Clean White Bottom Navigation Bar in WhatsApp_UI_Reference_1.png & WhatsApp_UI_Reference_2.png:
 * Tabs: Chats | Updates | Communities | Calls | Settings
 */
@Composable
private fun WhatsAppReferenceBottomBar(
    currentRoute: NeoTactileRoute,
    onSelectRoute: (NeoTactileRoute) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = LightDividerGray, thickness = 0.8.dp)
        NavigationBar(
            containerColor = LightBackgroundWhite,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            val itemColors = NavigationBarItemDefaults.colors(
                selectedIconColor = WhatsAppDarkGreen,
                selectedTextColor = LightTextPrimary,
                indicatorColor = Color(0xFFD9FDD3),
                unselectedIconColor = LightTextSecondary,
                unselectedTextColor = LightTextSecondary
            )

            NavigationBarItem(
                selected = currentRoute == NeoTactileRoute.HOME,
                onClick = { onSelectRoute(NeoTactileRoute.HOME) },
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Chats"
                    )
                },
                label = { Text("Chats", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                colors = itemColors,
                modifier = Modifier.testTag("nav_tab_home")
            )

            NavigationBarItem(
                selected = currentRoute == NeoTactileRoute.UPDATES,
                onClick = { onSelectRoute(NeoTactileRoute.UPDATES) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.RadioButtonChecked,
                        contentDescription = "Updates"
                    )
                },
                label = { Text("Updates", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                colors = itemColors,
                modifier = Modifier.testTag("nav_tab_updates")
            )

            NavigationBarItem(
                selected = currentRoute == NeoTactileRoute.PEERS,
                onClick = { onSelectRoute(NeoTactileRoute.PEERS) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = "Communities"
                    )
                },
                label = { Text("Communities", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                colors = itemColors,
                modifier = Modifier.testTag("nav_tab_peers")
            )

            NavigationBarItem(
                selected = currentRoute == NeoTactileRoute.CALLS,
                onClick = { onSelectRoute(NeoTactileRoute.CALLS) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Calls"
                    )
                },
                label = { Text("Calls", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                colors = itemColors,
                modifier = Modifier.testTag("nav_tab_chat")
            )

            NavigationBarItem(
                selected = currentRoute == NeoTactileRoute.PROFILE,
                onClick = { onSelectRoute(NeoTactileRoute.PROFILE) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Settings"
                    )
                },
                label = { Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                colors = itemColors,
                modifier = Modifier.testTag("nav_tab_profile")
            )
        }
    }
}
