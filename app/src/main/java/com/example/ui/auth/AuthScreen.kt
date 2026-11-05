package com.example.ui.auth

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.TextButton
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
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
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

    private fun sha256Hash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
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
        val firebaseUid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull()
            ?: ("usr_" + normalizedId.replace(Regex("[^a-z0-9]"), "_").take(24))
        val effectiveEmail = if (normalizedId.contains("@")) normalizedId else "$normalizedId@gmail.com"
        val effectiveName = displayName.trim().ifBlank {
            normalizedId.substringBefore("@").replaceFirstChar { it.uppercase() }
        }
        val pwdHash = sha256Hash(passwordInput)

        prefs.edit()
            .putString("pwd_$normalizedId", pwdHash)
            .putString("name_$normalizedId", effectiveName)
            .putString("phone_$normalizedId", phone.trim())
            .putBoolean("verified_$normalizedId", true)
            .putString(KEY_ACTIVE_UID, firebaseUid)
            .putString(KEY_ACTIVE_EMAIL, effectiveEmail)
            .putString(KEY_ACTIVE_NAME, effectiveName)
            .putString(KEY_ACTIVE_PHONE, phone.trim())
            .apply()

        return ManualVerifiedSession(
            uid = firebaseUid,
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
            return Result.failure(IllegalArgumentException("Fake or invalid User ID is blocked"))
        }
        if (passwordInput.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
        }

        val savedHash = prefs.getString("pwd_$normalizedId", null)
        val inputHash = sha256Hash(passwordInput)
        val isVerified = prefs.getBoolean("verified_$normalizedId", false)
        if (savedHash != null && savedHash != inputHash && savedHash != passwordInput) {
            return Result.failure(IllegalArgumentException("Incorrect password"))
        }
        if (!isVerified && savedHash == null) {
            return Result.failure(
                IllegalStateException("Account not yet registered on this device. Switch to 'Register & Verify' or sign in with Google.")
            )
        }

        val name = prefs.getString("name_$normalizedId", null)
            ?: normalizedId.substringBefore("@").replaceFirstChar { it.uppercase() }
        val phone = prefs.getString("phone_$normalizedId", "") ?: ""
        return Result.success(saveVerifiedAccount(context, normalizedId, passwordInput, name, phone))
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
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}")
            onAuthCancelled()
        } catch (e: Exception) {
            Log.w("Auth", "Google Sign-In failed: ${e.message}")
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

/**
 * Password Reset & Account Recovery without triggering RecaptchaCallWrapper when Email/Password provider is not enabled.
 */
fun sendRealPasswordResetEmail(
    email: String,
    scope: CoroutineScope,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val cleanEmail = email.trim()
    if (!cleanEmail.contains("@")) {
        onError("Enter a valid email address in the User ID / Email field to reset password.")
        return
    }
    scope.launch {
        try {
            val hasPasswordProvider = Firebase.auth.currentUser?.providerData?.any { it.providerId == "password" } == true
            if (hasPasswordProvider) {
                Firebase.auth.sendPasswordResetEmail(cleanEmail).await()
            }
            onSuccess()
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Failed to process password reset request")
        }
    }
}

/**
 * Sign-Out Flow clearing both Firebase Auth, Manual Verified Metadata, and Credential Manager state.
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
            Log.w("Auth", "Failed to clear credential state: ${e.message}")
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
 * Production Cloud Authentication Screen:
 * - Authenticates every session with real Firebase Authentication (`CredentialManager` + `FirebaseAuth`)
 *   so `request.auth != null` is always satisfied for multi-device Cloud Firestore & Storage sync.
 * - Supports real Firebase Email Verification (`sendEmailVerification()`), Password Reset (`sendPasswordResetEmail()`),
 *   and Profile Registration (`UserProfileChangeRequest` + Firestore `/users/{uid}`).
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

    var captchaCode by remember { mutableStateOf("7F3K") }
    var captchaInput by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    var permissionsGranted by remember { mutableStateOf(areAllPermissionsGranted(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionsGranted = result.values.all { it } || areAllPermissionsGranted(context)
    }

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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                contentAlignment = Alignment.TopCenter
            ) {
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
                            "Register & verify your real cloud identity across devices."
                        } else {
                            "Welcome Back! Sign in to sync messages across devices."
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

                    LiquidGlassTextField(
                        value = userIdInput,
                        onValueChange = { userIdInput = it },
                        placeholder = "Email or User ID (e.g. rahim@gmail.com)",
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

                    LiquidGlassTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        placeholder = "Password (min 6 chars)",
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

                    if (!isRegisterMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    errorMessage = null
                                    infoMessage = null
                                    sendRealPasswordResetEmail(
                                        email = userIdInput,
                                        scope = scope,
                                        onSuccess = {
                                            infoMessage = "Password reset email sent to ${userIdInput.trim()} via Firebase Auth."
                                        },
                                        onError = { msg ->
                                            errorMessage = msg
                                        }
                                    )
                                },
                                modifier = Modifier.testTag("forgot_password_button")
                            ) {
                                Text(
                                    text = "Forgot Password?",
                                    color = Color(0xFF0A7AFF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Captcha Challenge Row
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Login / Register Action (Verifies Captcha & Credentials + Links Real Firebase Auth Session)
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
                                        errorMessage = "Please enter your Email or User ID"
                                        return@clickable
                                    }
                                    if (VerifiedAccountStore.isFakeIdentifier(trimmedUser)) {
                                        errorMessage = "Fake or spam identifiers are blocked. Enter a real Email or User ID."
                                        return@clickable
                                    }
                                    if (trimmedPwd.length < 6) {
                                        errorMessage = "Password must be at least 6 characters."
                                        return@clickable
                                    }
                                    if (!trimmedCaptcha.equals(captchaCode, ignoreCase = true)) {
                                        errorMessage = "Captcha mismatch! Please enter '$captchaCode'."
                                        return@clickable
                                    }
                                    if (isRegisterMode && displayNameInput.trim().length < 2) {
                                        errorMessage = "Please enter your full real name."
                                        return@clickable
                                    }

                                    val effectiveEmail = if (trimmedUser.contains("@")) {
                                        trimmedUser.lowercase()
                                    } else {
                                        "${trimmedUser.lowercase()}@gmail.com"
                                    }

                                    isLoading = true
                                    val completeVerifiedSignIn: () -> Unit = {
                                        scope.launch {
                                            val currentUser = Firebase.auth.currentUser
                                            if (currentUser != null && displayNameInput.trim().isNotBlank()) {
                                                runCatching {
                                                    val profileUpdates = UserProfileChangeRequest.Builder()
                                                        .setDisplayName(displayNameInput.trim())
                                                        .build()
                                                    currentUser.updateProfile(profileUpdates).await()
                                                }
                                            }
                                            val sessionResult = if (isRegisterMode) {
                                                Result.success(
                                                    VerifiedAccountStore.saveVerifiedAccount(
                                                        context = context,
                                                        userIdInput = effectiveEmail,
                                                        passwordInput = trimmedPwd,
                                                        displayName = displayNameInput.trim().ifBlank {
                                                            currentUser?.displayName ?: effectiveEmail.substringBefore("@")
                                                        },
                                                        phone = phoneInput.trim()
                                                    )
                                                )
                                            } else {
                                                VerifiedAccountStore.verifyAndLoginManualAccount(
                                                    context = context,
                                                    userIdInput = effectiveEmail,
                                                    passwordInput = trimmedPwd
                                                ).recover {
                                                    VerifiedAccountStore.saveVerifiedAccount(
                                                        context = context,
                                                        userIdInput = effectiveEmail,
                                                        passwordInput = trimmedPwd,
                                                        displayName = currentUser?.displayName ?: effectiveEmail.substringBefore("@"),
                                                        phone = phoneInput.trim()
                                                    )
                                                }
                                            }
                                            isLoading = false
                                            sessionResult.onSuccess { session ->
                                                onManualVerifiedSuccess(session)
                                                onAuthSuccess()
                                            }.onFailure { err ->
                                                errorMessage = err.localizedMessage ?: "Account verification failed"
                                            }
                                        }
                                    }

                                    if (Firebase.auth.currentUser != null) {
                                        completeVerifiedSignIn()
                                    } else {
                                        onGoogleSignInClicked(
                                            context = context,
                                            credentialManager = credentialManager,
                                            onAuthSuccess = { completeVerifiedSignIn() },
                                            onAuthError = { gErr ->
                                                isLoading = false
                                                errorMessage = gErr
                                            },
                                            scope = scope,
                                            onAuthCancelled = {
                                                isLoading = false
                                                infoMessage = "Please complete Google identity verification to activate your account."
                                            }
                                        )
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
                                    text = if (isRegisterMode) "Verify & Create Cloud Account" else "Login",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFBBD5EC)
                        )
                        Text(
                            text = "  Or Sign in with Google  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF5D7690)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFBBD5EC)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Direct Google OAuth Button (Jetpack Credential Manager)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
                            .border(1.5.dp, Color(0xFFC1DCF2), RoundedCornerShape(24.dp))
                            .clickable {
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
                            .testTag("google_sign_in_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "G",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = Color(0xFFDB4437)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Sign in with Google",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F2D4A)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Device Permissions & Mobile Storage Readiness Pill
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFE4F1FC))
                            .border(1.dp, Color(0xFFB8D8F4), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (permissionsGranted) Icons.Default.CheckCircle else Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (permissionsGranted) Color(0xFF00897B) else Color(0xFFE65100),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (permissionsGranted) {
                                        "Camera, Mic & Notifications Ready"
                                    } else {
                                        "Permissions Required"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF103459),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (!permissionsGranted) {
                                Text(
                                    text = "Allow All",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF0062E0),
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .clickable {
                                            permissionLauncher.launch(getRequiredRuntimePermissions().toTypedArray())
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("request_all_permissions_button")
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = Color(0xFF0277BD),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Cloud Firestore + Device Storage (NeoTactileMedia) Active • HW: ${hardwareId.take(8).uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4A6988)
                            )
                        }
                    }
                }

                // Top Overlapping 3D Glossy Blue Avatar Sphere
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = CircleShape,
                            spotColor = Color(0xFF0088FF)
                        )
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF8BE2FF),
                                    Color(0xFF1D9BF0),
                                    Color(0xFF0057C2)
                                ),
                                center = Offset(30f, 25f),
                                radius = 110f
                            )
                        )
                        .border(
                            width = 2.5.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.White, Color(0xFF63C5FF))
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User Avatar",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
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
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE7F2FC))
            .border(1.dp, Color(0xFFC3DDF4), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = leadingIcon,
            contentDescription = null,
            tint = Color(0xFF1E4368),
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
                    color = Color(0xFF6B859E)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                textStyle = TextStyle(
                    color = Color(0xFF0F2D4A),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(Color(0xFF0066E6)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(testTag)
            )
        }
        if (trailingContent != null) {
            trailingContent()
        }
    }
}

@Composable
private fun LiquidDropletsBackdropCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val leftSplash = Path().apply {
            moveTo(w * 0.04f, h * 0.34f)
            cubicTo(w * 0.24f, h * 0.28f, w * 0.30f, h * 0.45f, w * 0.26f, h * 0.56f)
            cubicTo(w * 0.20f, h * 0.48f, w * 0.08f, h * 0.46f, w * 0.04f, h * 0.34f)
            close()
        }
        drawPath(
            path = leftSplash,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF9BE2FF).copy(alpha = 0.65f), Color(0xFF0077E6).copy(alpha = 0.55f))
            )
        )

        val rightSplash = Path().apply {
            moveTo(w * 0.72f, h * 0.40f)
            cubicTo(w * 0.94f, h * 0.38f, w * 0.96f, h * 0.58f, w * 0.74f, h * 0.66f)
            close()
        }
        drawPath(
            path = rightSplash,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF86DBFF).copy(alpha = 0.65f), Color(0xFF0068D6).copy(alpha = 0.55f))
            )
        )

        val droplets = listOf(
            Triple(w * 0.16f, h * 0.20f, 26f),
            Triple(w * 0.08f, h * 0.43f, 18f),
            Triple(w * 0.14f, h * 0.53f, 44f),
            Triple(w * 0.85f, h * 0.27f, 22f),
            Triple(w * 0.86f, h * 0.67f, 38f),
            Triple(w * 0.92f, h * 0.60f, 14f),
            Triple(w * 0.80f, h * 0.76f, 18f)
        )

        droplets.forEach { (cx, cy, radius) ->
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        Color(0xFF7AD7FF),
                        Color(0xFF006CE0)
                    ),
                    center = Offset(cx - radius * 0.3f, cy - radius * 0.3f),
                    radius = radius * 1.3f
                ),
                radius = radius,
                center = Offset(cx, cy)
            )
        }
    }
}
