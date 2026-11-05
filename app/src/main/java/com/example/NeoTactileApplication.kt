package com.example

import android.app.Application
import android.util.Log
import com.example.notifications.NotificationHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.webrtc.PeerConnectionFactory

/**
 * Production Application class for NeoTactile Chat:
 * - Guarantees safe, resilient FirebaseApp initialization (preventing IllegalStateException on launch).
 * - Pre-creates Android 8+ Notification Channels for instant real-time incoming messages and calls.
 * - Warm-starts native WebRTC PeerConnectionFactory JNI libraries in the background.
 * - Installs an uncaught exception safety logger to trap and report any startup anomalies.
 */
class NeoTactileApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        installUncaughtExceptionHandler()
        initFirebaseSafely()
        initNotificationChannelsSafely()
        warmupWebRtcSafely()
    }

    private fun installUncaughtExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "FATAL CRASH on thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun initFirebaseSafely() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.fromResource(this)
                if (options != null) {
                    FirebaseApp.initializeApp(this, options)
                    Log.i(TAG, "FirebaseApp initialized from Android resources.")
                } else {
                    FirebaseApp.initializeApp(this)
                    Log.i(TAG, "FirebaseApp auto-initialized.")
                }
            } else {
                Log.i(TAG, "FirebaseApp already initialized by ContentProvider.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Standard Firebase initialization failed; applying explicit fallback options", e)
            try {
                // Hardened fallback using provisioned parameters from google-services.json
                val fallbackOptions = FirebaseOptions.Builder()
                    .setApplicationId("1:826237155105:android:bc6874a5b5d4f971287060")
                    .setApiKey("AIzaSyC_uCLMk1w4S9CdtLRCM-JrTtduvlCjIbc")
                    .setProjectId("gen-lang-client-0292160925")
                    .setStorageBucket("gen-lang-client-0292160925.firebasestorage.app")
                    .setGcmSenderId("826237155105")
                    .build()
                FirebaseApp.initializeApp(this, fallbackOptions)
                Log.i(TAG, "FirebaseApp explicit fallback initialized successfully.")
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "All Firebase initialization paths failed", fallbackEx)
            }
        }
    }

    private fun initNotificationChannelsSafely() {
        try {
            NotificationHelper.ensureChannels(this)
        } catch (e: Exception) {
            Log.w(TAG, "Notification channel init skipped: ${e.message}")
        }
    }

    private fun warmupWebRtcSafely() {
        try {
            val initOptions = PeerConnectionFactory.InitializationOptions.builder(this)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
            PeerConnectionFactory.initialize(initOptions)
            Log.i(TAG, "WebRTC PeerConnectionFactory warm-up completed.")
        } catch (e: Throwable) {
            Log.w(TAG, "WebRTC warm-up deferred: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "NeoTactileApplication"
    }
}
