package com.example

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.example.ui.components.frostedGlassCard
import com.example.ui.components.neoTactileBackground
import com.example.ui.screens.BiometricLockScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DeviceBannedScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.UsersDirectoryScreen
import com.example.ui.theme.DeepMatteSlateStart
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlowingCyan
import com.example.ui.theme.NeoTactileTheme
import com.example.ui.theme.TactileAmber
import com.example.ui.theme.TactileEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.NeoTactileViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class NeoTactileRoute(val routeId: String) {
    HOME("home"),
    CHAT("chat"),
    PEERS("peers"),
    PROFILE("profile")
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
            NeoTactileTheme {
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
    val context = LocalContext.current
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var manualSession by remember {
        mutableStateOf(VerifiedAccountStore.getActiveManualSession(context))
    }
    val hardwareId = remember { deviceIdentityManager.getHardwareAndroidId() }
    val user = currentUser
    val activeManual = manualSession

    if (user == null && activeManual == null) {
        LoginScreen(
            hardwareId = hardwareId,
            onAuthSuccess = {
                manualSession = VerifiedAccountStore.getActiveManualSession(context)
            },
            onManualVerifiedSuccess = { session ->
                manualSession = session
            }
        )
    } else {
        val effectiveUid = user?.uid ?: activeManual?.uid ?: "verified_user"
        val effectiveEmail = user?.email ?: activeManual?.userIdOrEmail ?: "operator@neotactile.app"
        val effectiveDisplayName = user?.displayName ?: activeManual?.displayName ?: "Verified User"
        val effectivePhotoUrl = user?.photoUrl?.toString()

        AuthenticatedMainShell(
            currentUserId = effectiveUid,
            currentUserEmail = effectiveEmail,
            currentUserDisplayName = effectiveDisplayName,
            currentUserPhotoUrl = effectivePhotoUrl,
            deviceIdentityManager = deviceIdentityManager,
            onSignOutComplete = {
                manualSession = null
            }
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

    // App-entry runtime permissions check (Camera, Microphone, Notifications)
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
                    currentUserPhotoUrl = currentUserPhotoUrl
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

    // BackHandler for secondary tabs returning to HOME
    if (currentRoute != NeoTactileRoute.HOME) {
        BackHandler {
            currentRoute = NeoTactileRoute.HOME
        }
    }

    NeoTactileTheme(isLightTheme = isLightTheme) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .neoTactileBackground(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                NeoTactileTopBar(
                    shortCode = viewModel.currentUserShortCode,
                    language = appLanguage,
                    statusBanner = statusBanner,
                    allPermissionsGranted = allPermissionsGranted,
                    onRequestPermissions = {
                        permissionLauncher.launch(getRequiredRuntimePermissions().toTypedArray())
                    },
                    onCycleLanguage = { viewModel.cycleAppLanguage() },
                    onToggleTheme = { viewModel.toggleThemeMode() },
                    onDismissStatus = { viewModel.clearStatusBanner() },
                    onQuickLock = { viewModel.lockAppImmediately() }
                )
            },
            bottomBar = {
                NeoTactileBottomBar(
                    language = appLanguage,
                    currentRoute = currentRoute,
                    onSelectRoute = { currentRoute = it }
                )
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
                                viewModel.startWebRtcCall(peerId, peerName, callType)
                            }
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
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NeoTactileTopBar(
    shortCode: String,
    language: AppLanguage,
    statusBanner: String?,
    allPermissionsGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onCycleLanguage: () -> Unit,
    onToggleTheme: () -> Unit,
    onDismissStatus: () -> Unit,
    onQuickLock: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = GlowingCyan,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(id = R.string.auth_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "MY ID: $shortCode",
                        style = MaterialTheme.typography.labelSmall,
                        color = GlowingCyan
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Language Switch Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF14281F))
                        .border(1.dp, GlowingCyan.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        .clickable { onCycleLanguage() }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                        .testTag("language_toggle_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Switch Language",
                        tint = GlowingCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = language.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Theme Switch Button
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF14281F))
                        .testTag("top_bar_theme_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Contrast,
                        contentDescription = "Toggle Green/White Light or Dark Theme",
                        tint = GlowingCyan
                    )
                }

                // Quick Lock Button
                IconButton(
                    onClick = onQuickLock,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF14281F))
                        .testTag("top_bar_lock_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock Session Now",
                        tint = GlowingCyan
                    )
                }
            }
        }

        AnimatedVisibility(visible = !allPermissionsGranted) {
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .frostedGlassCard(cornerRadius = 16.dp, accentGlow = TactileAmber)
                    .clickable { onRequestPermissions() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
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
                        tint = TactileAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = language.tr(
                            "কল, ক্যামেরা, ভয়েস ও মোবাইল স্টোরেজ ব্যবহারের জন্য সব পারমিশন Allow করুন",
                            "Tap to Allow Camera, Mic & Notification permissions"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary
                    )
                }
                Text(
                    text = "Allow",
                    style = MaterialTheme.typography.labelSmall,
                    color = GlowingCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        AnimatedVisibility(visible = !statusBanner.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .frostedGlassCard(cornerRadius = 16.dp, accentGlow = TactileEmerald)
                    .clickable { onDismissStatus() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = statusBanner ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = TactileEmerald
                )
            }
        }
    }
}

@Composable
private fun NeoTactileBottomBar(
    language: AppLanguage,
    currentRoute: NeoTactileRoute,
    onSelectRoute: (NeoTactileRoute) -> Unit
) {
    NavigationBar(
        containerColor = DeepMatteSlateStart.copy(alpha = 0.96f),
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationBarItem(
            selected = currentRoute == NeoTactileRoute.HOME,
            onClick = { onSelectRoute(NeoTactileRoute.HOME) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = language.shortTr("হোম ও স্ট্যাটাস", "Updates")
                )
            },
            label = { Text(language.shortTr("হোম", "Updates")) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = GlowingCyan,
                selectedTextColor = GlowingCyan,
                indicatorColor = ElectricBlue.copy(alpha = 0.28f),
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            ),
            modifier = Modifier.testTag("nav_tab_home")
        )

        NavigationBarItem(
            selected = currentRoute == NeoTactileRoute.CHAT,
            onClick = { onSelectRoute(NeoTactileRoute.CHAT) },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = language.shortTr("চ্যাট ও কল", "Chats")
                )
            },
            label = { Text(language.shortTr("চ্যাট", "Chats")) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = GlowingCyan,
                selectedTextColor = GlowingCyan,
                indicatorColor = ElectricBlue.copy(alpha = 0.28f),
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            ),
            modifier = Modifier.testTag("nav_tab_chat")
        )

        NavigationBarItem(
            selected = currentRoute == NeoTactileRoute.PEERS,
            onClick = { onSelectRoute(NeoTactileRoute.PEERS) },
            icon = {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = language.shortTr("ইউজার ও কমিউনিটি", "Users")
                )
            },
            label = { Text(language.shortTr("ইউজার অ্যাড", "Communities")) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = GlowingCyan,
                selectedTextColor = GlowingCyan,
                indicatorColor = ElectricBlue.copy(alpha = 0.28f),
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            ),
            modifier = Modifier.testTag("nav_tab_peers")
        )

        NavigationBarItem(
            selected = currentRoute == NeoTactileRoute.PROFILE,
            onClick = { onSelectRoute(NeoTactileRoute.PROFILE) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = language.shortTr("সেটিংস", "Settings")
                )
            },
            label = { Text(language.shortTr("সেটিংস", "Settings")) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = GlowingCyan,
                selectedTextColor = GlowingCyan,
                indicatorColor = ElectricBlue.copy(alpha = 0.28f),
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            ),
            modifier = Modifier.testTag("nav_tab_profile")
        )
    }
}
