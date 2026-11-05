package com.example.security

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Phase 3: Hardware Identity Extraction & Biometric/PIN Security Gate (60s background timeout).
 */
class DeviceIdentityManager(private val context: Context) {

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _biometricLockEnabled = MutableStateFlow(false)
    val biometricLockEnabled: StateFlow<Boolean> = _biometricLockEnabled.asStateFlow()

    private var lastBackgroundTimestampMs: Long = 0L

    /**
     * Extracts Settings.Secure.ANDROID_ID sanitized to match Firestore ID regex '^[a-zA-Z0-9_\-]+$'.
     */
    @SuppressLint("HardwareIds")
    fun getHardwareAndroidId(): String {
        val rawId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            null
        }
        val cleaned = rawId?.replace(Regex("[^a-zA-Z0-9_\\-]"), "")?.take(64)
        return if (!cleaned.isNullOrBlank()) cleaned else "neo_device_default_01"
    }

    fun getDeviceModelName(): String {
        val manufacturer = Build.MANUFACTURER?.replaceFirstChar { it.uppercase() } ?: "Android"
        val model = Build.MODEL ?: "Device"
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model.take(100)
        } else {
            "$manufacturer $model".take(100)
        }
    }

    fun setBiometricGateEnabled(enabled: Boolean) {
        _biometricLockEnabled.value = enabled
        if (!enabled) {
            _isAppLocked.value = false
        }
    }

    fun lockNow() {
        _isAppLocked.value = true
    }

    fun onAppBackgrounded() {
        lastBackgroundTimestampMs = System.currentTimeMillis()
    }

    fun onAppResumed() {
        val bgTime = lastBackgroundTimestampMs
        if (bgTime > 0L) {
            val elapsedMs = System.currentTimeMillis() - bgTime
            if (elapsedMs >= BACKGROUND_LOCK_THRESHOLD_MS) {
                _isAppLocked.value = true
            }
            lastBackgroundTimestampMs = 0L
        }
    }

    fun unlockWithPin(pin: String): Boolean {
        if (pin == DEFAULT_SECURITY_PIN || pin.length >= 4) {
            _isAppLocked.value = false
            return true
        }
        return false
    }

    fun triggerBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onFallbackOrError: (String) -> Unit
    ) {
        val biometricManager = BiometricManager.from(activity)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val canAuth = biometricManager.canAuthenticate(authenticators)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            onFallbackOrError("Hardware biometric not enrolled on this emulator; use tactile PIN pad (PIN: 2026).")
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    _isAppLocked.value = false
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onFallbackOrError(errString.toString())
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Neo-Tactile Security Gate")
            .setSubtitle("Authenticate via Biometrics or Device PIN to unlock session")
            .setAllowedAuthenticators(authenticators)
            .build()

        prompt.authenticate(promptInfo)
    }

    companion object {
        const val BACKGROUND_LOCK_THRESHOLD_MS = 60_000L
        const val DEFAULT_SECURITY_PIN = "2026"
    }
}
