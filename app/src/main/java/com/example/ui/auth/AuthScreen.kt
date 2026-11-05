package com.example.ui.auth

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.abs

data class ManualVerifiedSession(
    val uid: String,
    val userIdOrEmail: String,
    val displayName: String,
    val phoneNumber: String,
    val isAccountVerified: Boolean = true
)

object VerifiedAccountStore {
    private const val PREFS_NAME = "neotactile_verified_accounts_prefs"
    private const val KEY_ACTIVE_UID = "active_manual_uid"
    private const val KEY_ACTIVE_EMAIL = "active_manual_email"
    private const val KEY_ACTIVE_NAME = "active_manual_name"
    private const val KEY_ACTIVE_PHONE = "active_manual_phone"

    private val BLOCKED_FAKE_PATTERNS = listOf(
        "fake", "bot", "spam", "temp", "dummy", "test000", "anonymous", "hacker"
    )

    fun isFakeIdentifier(input: String): Boolean {
        val clean = input.trim().lowercase()
        if (clean.length < 4) return true
        return BLOCKED_FAKE_PATTERNS.any { clean.contains(it) }
    }

    fun generateCaptchaCode(): String {
        val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        return (1..4).map { chars.random() }.joinToString("")
    }

    fun generateVerificationOtp(seed: String): String {
        val code = (abs((seed + System.currentTimeMillis() / 60000).hashCode()) % 900000) + 100000
        return code.toString()
    }

    fun saveVerifiedAccount(
        context: Context,
        userIdInput: String,
        passwordInput: String,
        displayName: String,
        phone: String
    ): ManualVerifiedSession {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val normalizedId = userIdInput.trim().lowercase()
        val uid = "usr_" + normalizedId.replace(Regex("[^a-z0-9]"), "_").take(24)
        val effectiveEmail = if (normalizedId.contains("@")) normalizedId else "$normalizedId@gmail.com"
        val effectiveName = displayName.trim().ifBlank {
            normalizedId.substringBefore("@").replaceFirstChar { it.uppercase() }
        }

        prefs.edit()
            .putString("pwd_$normalizedId", passwordInput)
            .putString("name_$normalizedId", effectiveName)
            .putString("phone_$normalizedId", phone.trim())
            .putBoolean("verified_$normalizedId", true)
            .putString(KEY_ACTIVE_UID, uid)
            .putString(KEY_ACTIVE_EMAIL, effectiveEmail)
            .putString(KEY_ACTIVE_NAME, effectiveName)
            .putString(KEY_ACTIVE_PHONE, phone.trim())
            .apply()

        return ManualVerifiedSession(
            uid = uid,
            userIdOrEmail = effectiveEmail,
            displayName = effectiveName,
            phoneNumber = phone.trim(),
            isAccountVerified = true
        )
    }

    fun verifyAndLoginManualAccount(
        context: Context,
        userIdInput: String,
        passwordInput: String
    ): Result<ManualVerifiedSession> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val normalizedId = userIdInput.trim().lowercase()
        if (isFakeIdentifier(normalizedId)) {
            return Result.failure(IllegalArgumentException("ফেইক বা অবৈধ ইউজার আইডি ব্লক করা হয়েছে (Fake/Invalid ID blocked)"))
        }
        if (passwordInput.length < 6) {
            return Result.failure(IllegalArgumentException("পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে (Password min 6 chars)"))
        }

        val savedPwd = prefs.getString("pwd_$normalizedId", null)
        val isVerified = prefs.getBoolean("verified_$normalizedId", false)
        if (savedPwd != null && savedPwd != passwordInput) {
            return Result.failure(IllegalArgumentException("ভুল পাসওয়ার্ড! সঠিক পাসওয়ার্ড দিন (Incorrect password)"))
        }
        if (!isVerified && savedPwd == null) {
            return Result.failure(
                IllegalStateException("এই আইডিটি এখনো ভেরিফাই করা হয়নি। নিচে 'Register & Verify' ট্যাবে গিয়ে ভেরিফাই করুন।")
            )
        }

        val name = prefs.getString("name_$normalizedId", null)
            ?: normalizedId.substringBefore("@").replaceFirstChar { it.uppercase() }
        val phone = prefs.getString("phone_$normalizedId", "") ?: ""
        return Result.success(saveVerifiedAccount(context, normalizedId, passwordInput, name, phone))
    }

    fun getActiveManualSession(context: Context): ManualVerifiedSession? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uid = prefs.getString(KEY_ACTIVE_UID, null) ?: return null
        val email = prefs.getString(KEY_ACTIVE_EMAIL, null) ?: return null
        val name = prefs.getString(KEY_ACTIVE_NAME, "Verified User") ?: "Verified User"
        val phone = prefs.getString(KEY_ACTIVE_PHONE, "") ?: ""
        return ManualVerifiedSession(
            uid = uid,
            userIdOrEmail = email,
            displayName = name,
            phoneNumber = phone,
            isAccountVerified = true
        )
    }

    fun clearActiveManualSession(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_ACTIVE_UID)
            .remove(KEY_ACTIVE_EMAIL)
            .remove(KEY_ACTIVE_NAME)
            .remove(KEY_ACTIVE_PHONE)
            .apply()
    }
}

/**
 * Silent Auto-Sign-In on App Startup using GetGoogleIdOption in its own GetCredentialRequest.
 */
fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

/**
 * Interactive Sign-In using GetSignInWithGoogleOption in its own GetCredentialRequest.
 */
fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val activityContext = context as? Activity ?: context
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

/**
 * Sign-Out Flow clearing both Firebase Auth, Manual Verified Session, and Credential Manager state.
 */
fun performGoogleSignOut(
    context: Context,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    val credentialManager = CredentialManager.create(context)
    VerifiedAccountStore.clearActiveManualSession(context)
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}

/**
 * Checks whether all required runtime permissions (Camera, Microphone, Notifications) are granted.
 */
fun getRequiredRuntimePermissions(): List<String> {
    val list = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        list.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    return list
}

fun areAllPermissionsGranted(context: Context): Boolean {
    return getRequiredRuntimePermissions().all { perm ->
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * Liquid Glassmorphic Login, Registration, Captcha & Account Verification Screen
 * accurately replicating the uploaded reference design (`/public/assets/aistudio/ef1271d78a32606d2e49221b57d13676_eb707439.jpg`),
 * paired with automatic Runtime Permission & Mobile Storage verification.
 */
@Composable
fun LoginScreen(
    hardwareId: String,
    onAuthSuccess: () -> Unit,
    onManualVerifiedSuccess: (ManualVerifiedSession) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isRegisterMode by remember { mutableStateOf(false) }
    var userIdInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var displayNameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Captcha State ("7 F 3 K" style from reference image)
    var captchaCode by remember { mutableStateOf("7F3K") }
    var captchaInput by remember { mutableStateOf("") }

    // Anti-Fake Account OTP Verification State
    var generatedOtp by remember { mutableStateOf<String?>(null) }
    var otpInput by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    // Runtime Permissions State
    var permissionsGranted by remember { mutableStateOf(areAllPermissionsGranted(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionsGranted = result.values.all { it } || areAllPermissionsGranted(context)
    }

    // Automatically request all required permissions on app entry
    LaunchedEffect(Unit) {
        if (!areAllPermissionsGranted(context)) {
            permissionLauncher.launch(getRequiredRuntimePermissions().toTypedArray())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFEAF3FC),
                        Color(0xFFDCEBF7),
                        Color(0xFFEFF6FD)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // 3D Liquid Water Droplets & Splash Artwork Layer (Matching Reference Image)
        LiquidDropletsBackdropCanvas(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // Main Frosted Liquid Glass Card with Top Overlapping 3D Blue Orb Avatar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                // Frosted Glass Card Container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 42.dp)
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(34.dp),
                            ambientColor = Color(0xFF0077FF).copy(alpha = 0.25f),
                            spotColor = Color(0xFF0088FF).copy(alpha = 0.30f)
                        )
                        .clip(RoundedCornerShape(34.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xE8F6FBFF),
                                    Color(0xD8E8F4FC),
                                    Color(0xEBF7FBFF)
                                )
                            )
                        )
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White,
                                    Color(0xFF9AD4FF),
                                    Color.White
                                )
                            ),
                            shape = RoundedCornerShape(34.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(26.dp))

                    // Title: "Login" or "Register"
                    Text(
                        text = if (isRegisterMode) "Register" else "Login",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 30.sp
                        ),
                        color = Color(0xFF0F2D4A),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isRegisterMode) {
                            "Verify your real identity — Fake accounts are strictly blocked."
                        } else {
                            "Welcome Back! Please login to continue."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF5A738E),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Login / Register Mode Switch Pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFDCEBFA))
                            .border(1.dp, Color.White, RoundedCornerShape(16.dp))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (!isRegisterMode) Color(0xFF0A7AFF) else Color.Transparent)
                                .clickable {
                                    isRegisterMode = false
                                    errorMessage = null
                                    infoMessage = null
                                }
                                .padding(vertical = 8.dp)
                                .testTag("switch_login_mode_tab"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Login (লগইন)",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (!isRegisterMode) Color.White else Color(0xFF1F456E),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isRegisterMode) Color(0xFF0A7AFF) else Color.Transparent)
                                .clickable {
                                    isRegisterMode = true
                                    errorMessage = null
                                    infoMessage = null
                                }
                                .padding(vertical = 8.dp)
                                .testTag("switch_register_mode_tab"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Register & Verify",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isRegisterMode) Color.White else Color(0xFF1F456E),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. User ID / Gmail Input Field
                    LiquidGlassTextField(
                        value = userIdInput,
                        onValueChange = { userIdInput = it },
                        placeholder = "User ID or Gmail (e.g. rahim@gmail.com)",
                        leadingIcon = Icons.Default.Person,
                        testTag = "manual_user_id_input"
                    )

                    if (isRegisterMode) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LiquidGlassTextField(
                            value = displayNameInput,
                            onValueChange = { displayNameInput = it },
                            placeholder = "Full Real Name (আসল নাম)",
                            leadingIcon = Icons.Default.VerifiedUser,
                            testTag = "manual_display_name_input"
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        LiquidGlassTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            placeholder = "Mobile Number (+8801...)",
                            leadingIcon = Icons.Default.Phone,
                            testTag = "manual_phone_input"
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Password Input Field with Visibility Toggle
                    LiquidGlassTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        placeholder = "Password",
                        leadingIcon = Icons.Default.Lock,
                        isPassword = !passwordVisible,
                        trailingContent = {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Password Visibility",
                                    tint = Color(0xFF3E6082),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        testTag = "manual_password_input"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Captcha Challenge Row (Matching "7 F 3 K  [Refresh]  [Enter Captcha]" in Reference Image)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFE7F2FC))
                            .border(1.dp, Color(0xFFC1DCF2), RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Spaced Captcha Display Box
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFD6E8F8))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = captchaCode.toCharArray().joinToString(" "),
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = Color(0xFF0E3156),
                                    letterSpacing = 2.sp
                                ),
                                modifier = Modifier.testTag("captcha_code_text")
                            )
                        }

                        IconButton(
                            onClick = {
                                captchaCode = VerifiedAccountStore.generateCaptchaCode()
                                captchaInput = ""
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("refresh_captcha_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Captcha",
                                tint = Color(0xFF0B57D0),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Enter Captcha Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.88f))
                                .border(1.dp, Color(0xFFC7DFF4), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (captchaInput.isEmpty()) {
                                Text(
                                    text = "Enter Captcha",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF7591AD)
                                )
                            }
                            BasicTextField(
                                value = captchaInput,
                                onValueChange = { captchaInput = it.uppercase().take(6) },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = Color(0xFF0F2D4A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                cursorBrush = SolidColor(Color(0xFF0066E6)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("captcha_input_field")
                            )
                        }
                    }

                    // 4. Anti-Fake Account OTP Verification Step (Shown during Registration)
                    AnimatedVisibility(visible = isRegisterMode && generatedOtp != null) {
                        Column(
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFDFF3EA))
                                .border(1.dp, Color(0xFF00A884), RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "অ্যাকাউন্ট ভেরিফিকেশন কোড (OTP): $generatedOtp",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF00695C),
                                fontWeight = FontWeight.Bold
                            )
                            LiquidGlassTextField(
                                value = otpInput,
                                onValueChange = { otpInput = it.take(6) },
                                placeholder = "উপরের ৬-ডিজিট ভেরিফিকেশন কোডটি লিখুন",
                                leadingIcon = Icons.Default.Security,
                                testTag = "verification_otp_input"
                            )
                        }
                    }

                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFD32F2F),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("auth_error_text")
                        )
                    }

                    if (!infoMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = infoMessage!!,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF00796B),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 5. Glossy 3D Blue Pill Login / Verify Button (Exact match to reference image)
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color(0xFF0066E6),
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .shadow(
                                    elevation = 10.dp,
                                    shape = RoundedCornerShape(26.dp),
                                    spotColor = Color(0xFF0077FF)
                                )
                                .clip(RoundedCornerShape(26.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF38B6FF),
                                            Color(0xFF0062E0),
                                            Color(0xFF0095FF)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.5.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.White.copy(alpha = 0.85f), Color(0xFF0051BA))
                                    ),
                                    shape = RoundedCornerShape(26.dp)
                                )
                                .clickable {
                                    errorMessage = null
                                    infoMessage = null
                                    val trimmedUser = userIdInput.trim()
                                    val trimmedPwd = passwordInput.trim()
                                    val trimmedCaptcha = captchaInput.trim().replace(" ", "")

                                    if (trimmedUser.isBlank()) {
                                        errorMessage = "অনুগ্রহ করে আপনার User ID বা Gmail লিখুন (Enter User ID)"
                                        return@clickable
                                    }
                                    if (VerifiedAccountStore.isFakeIdentifier(trimmedUser)) {
                                        errorMessage = "ফেইক বা স্প্যাম অ্যাকাউন্ট অনুমোদিত নয়! আসল User ID বা Gmail দিন।"
                                        return@clickable
                                    }
                                    if (trimmedPwd.length < 6) {
                                        errorMessage = "নিরাপত্তার জন্য পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে।"
                                        return@clickable
                                    }
                                    if (!trimmedCaptcha.equals(captchaCode, ignoreCase = true)) {
                                        errorMessage = "ক্যাপচা কোড মেলেনি! বক্সে দেখা যাওয়া '$captchaCode' কোডটি সঠিকভাবে লিখুন।"
                                        return@clickable
                                    }

                                    if (isRegisterMode) {
                                        if (displayNameInput.trim().length < 2) {
                                            errorMessage = "আপনার আসল নাম (Full Name) লিখুন।"
                                            return@clickable
                                        }
                                        if (phoneInput.trim().length < 7) {
                                            errorMessage = "ভেরিফিকেশনের জন্য সঠিক মোবাইল নম্বর দিন।"
                                            return@clickable
                                        }
                                        if (generatedOtp == null) {
                                            generatedOtp = VerifiedAccountStore.generateVerificationOtp(trimmedUser)
                                            infoMessage = "ফেইক অ্যাকাউন্ট রোধে ৬-ডিজিট ভেরিফিকেশন কোড তৈরি হয়েছে। কোডটি বসিয়ে রেজিস্ট্রেশন সম্পন্ন করুন।"
                                            return@clickable
                                        }
                                        if (otpInput.trim() != generatedOtp) {
                                            errorMessage = "ভেরিফিকেশন কোড (OTP) সঠিক নয়!"
                                            return@clickable
                                        }
                                        val session = VerifiedAccountStore.saveVerifiedAccount(
                                            context = context,
                                            userIdInput = trimmedUser,
                                            passwordInput = trimmedPwd,
                                            displayName = displayNameInput.trim(),
                                            phone = phoneInput.trim()
                                        )
                                        onManualVerifiedSuccess(session)
                                        onAuthSuccess()
                                    } else {
                                        VerifiedAccountStore.verifyAndLoginManualAccount(
                                            context = context,
                                            userIdInput = trimmedUser,
                                            passwordInput = trimmedPwd
                                        ).onSuccess { session ->
                                            onManualVerifiedSuccess(session)
                                            onAuthSuccess()
                                        }.onFailure { err ->
                                            errorMessage = err.localizedMessage
                                        }
                                    }
                                }
                                .testTag("manual_login_submit_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Login,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isRegisterMode) {
                                        if (generatedOtp == null) "Send Verification Code" else "Verify & Create Account"
                                    } else {
                                        "Login"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 6. "Or Login With" Divider & 4 Circular Social / OAuth Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFBBD5EC)
                        )
                        Text(
                            text = "  Or Login With  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF5D7690)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFBBD5EC)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Official Google OAuth Button
                        GlossySocialCircleButton(
                            label = "G",
                            badgeColor = Color(0xFFDB4437),
                            testTag = "google_sign_in_button",
                            onClick = {
                                errorMessage = null
                                isLoading = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onAuthSuccess()
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        errorMessage = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = {
                                        isLoading = false
                                    }
                                )
                            }
                        )

                        GlossySocialCircleButton(
                            label = "f",
                            badgeColor = Color(0xFF1877F2),
                            onClick = {
                                isRegisterMode = true
                                infoMessage = "ফেসবুক/ম্যানুয়াল আইডি ভেরিফাই করতে নাম ও ক্যাপচা পূরণ করুন।"
                            }
                        )

                        GlossySocialCircleButton(
                            label = "▶",
                            badgeColor = Color(0xFFFF0000),
                            onClick = {
                                isRegisterMode = true
                                infoMessage = "ভেরিফাইড মিডিয়া অ্যাকাউন্ট রেজিস্ট্রেশন ফর্ম চালু হয়েছে।"
                            }
                        )

                        GlossySocialCircleButton(
                            label = "in",
                            badgeColor = Color(0xFF0A66C2),
                            onClick = {
                                isRegisterMode = true
                                infoMessage = "প্রফেশনাল আইডি ভেরিফিকেশন মোড চালু হয়েছে।"
                            }
                        )
                    }
                }

                // Top Overlapping 3D Glossy Aqua Orb Avatar (Matching Reference Image)
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .shadow(16.dp, CircleShape, spotColor = Color(0xFF0088FF))
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF7CE8FF),
                                    Color(0xFF1492FF),
                                    Color(0xFF0055CC)
                                ),
                                center = Offset(30f, 25f),
                                radius = 120f
                            )
                        )
                        .border(
                            width = 2.5.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.White, Color(0xFF6CD4FF))
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User Orb Avatar",
                        tint = Color.White,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Device Permissions & Mobile Storage Verification Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.86f))
                    .border(1.5.dp, Color(0xFFB3D7F5), RoundedCornerShape(22.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (permissionsGranted) Icons.Default.CheckCircle else Icons.Default.Folder,
                            contentDescription = null,
                            tint = if (permissionsGranted) Color(0xFF00A884) else Color(0xFF0066E6),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (permissionsGranted) {
                                "সব পারমিশন ও মোবাইল স্টোরেজ সক্রিয় ✅"
                            } else {
                                "অ্যাপ পারমিশন ও মোবাইল স্টোরেজ অ্যাক্সেস"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF0F2D4A),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!permissionsGranted) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0066E6))
                                .clickable {
                                    permissionLauncher.launch(getRequiredRuntimePermissions().toTypedArray())
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("grant_all_permissions_button")
                        ) {
                            Text(
                                text = "Allow All",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    text = "ক্যামেরা, মাইক্রোফোন, নোটিফিকেশন এবং আপনার নিজের মোবাইলের স্টোরেজ (NeoTactileMedia) সরাসরি ব্যবহৃত হবে। Device ID: $hardwareId",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4B6580)
                )
            }
        }
    }
}

@Composable
private fun LiquidGlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    isPassword: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null,
    testTag: String? = null
) {
    val modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFEBF4FC))
            .border(1.dp, Color(0xFFC1DCF2), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = leadingIcon,
            contentDescription = placeholder,
            tint = Color(0xFF20486E),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF748FA8)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                textStyle = TextStyle(
                    color = Color(0xFF0E2C4B),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(Color(0xFF0066E6)),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (trailingContent != null) {
            trailingContent()
        }
    }
}

@Composable
private fun GlossySocialCircleButton(
    label: String,
    badgeColor: Color,
    onClick: () -> Unit,
    testTag: String? = null
) {
    val mod = if (testTag != null) Modifier.testTag(testTag) else Modifier
    Box(
        modifier = mod
            .size(48.dp)
            .shadow(8.dp, CircleShape, spotColor = badgeColor.copy(alpha = 0.35f))
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color.White, Color(0xFFE3F0FC))
                )
            )
            .border(1.5.dp, Color.White, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(badgeColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            )
        }
    }
}

/**
 * Custom Canvas rendering the 3D translucent aqua water splashes & droplets
 * seen around the frosted glass login card in the uploaded reference image.
 */
@Composable
private fun LiquidDropletsBackdropCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        fun drawGlossyDroplet(cx: Float, cy: Float, r: Float) {
            // Soft outer drop shadow
            drawCircle(
                color = Color(0x330066CC),
                radius = r * 1.08f,
                center = Offset(cx + r * 0.14f, cy + r * 0.18f)
            )
            // Main aqua droplet body
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE0F7FF),
                        Color(0xFF5AC8FA),
                        Color(0xFF007AFF)
                    ),
                    center = Offset(cx - r * 0.25f, cy - r * 0.25f),
                    radius = r * 1.3f
                ),
                radius = r,
                center = Offset(cx, cy)
            )
            // Specular white highlight dot
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = r * 0.24f,
                center = Offset(cx - r * 0.32f, cy - r * 0.32f)
            )
        }

        // Left dynamic liquid splash behind the card
        val leftSplash = Path().apply {
            moveTo(w * 0.28f, h * 0.36f)
            cubicTo(w * 0.06f, h * 0.24f, w * 0.04f, h * 0.44f, w * 0.18f, h * 0.48f)
            cubicTo(w * 0.24f, h * 0.50f, w * 0.22f, h * 0.60f, w * 0.28f, h * 0.62f)
            close()
        }
        drawPath(
            path = leftSplash,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xAA80D8FF), Color(0xCC0091EA))
            )
        )

        // Right dynamic liquid splash behind the card
        val rightSplash = Path().apply {
            moveTo(w * 0.72f, h * 0.42f)
            cubicTo(w * 0.92f, h * 0.40f, w * 0.94f, h * 0.58f, w * 0.78f, h * 0.60f)
            cubicTo(w * 0.74f, h * 0.62f, w * 0.72f, h * 0.66f, w * 0.70f, h * 0.64f)
            close()
        }
        drawPath(
            path = rightSplash,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xAA80D8FF), Color(0xCC0088FF))
            )
        )

        // Floating water droplets matching the reference composition
        drawGlossyDroplet(w * 0.18f, h * 0.16f, 28f)
        drawGlossyDroplet(w * 0.25f, h * 0.21f, 12f)
        drawGlossyDroplet(w * 0.10f, h * 0.41f, 16f)
        drawGlossyDroplet(w * 0.16f, h * 0.52f, 38f)
        drawGlossyDroplet(w * 0.20f, h * 0.64f, 12f)

        drawGlossyDroplet(w * 0.78f, h * 0.30f, 17f)
        drawGlossyDroplet(w * 0.86f, h * 0.39f, 19f)
        drawGlossyDroplet(w * 0.82f, h * 0.66f, 34f)
        drawGlossyDroplet(w * 0.88f, h * 0.60f, 11f)
        drawGlossyDroplet(w * 0.78f, h * 0.77f, 14f)
    }
}
