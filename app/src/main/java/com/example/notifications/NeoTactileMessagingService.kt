package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Production Firebase Cloud Messaging Service & System Notification Dispatcher:
 * - Automatically synchronizes the device's FCM registration token into `/users/{uid}.fcmToken`
 *   on the provisioned Firestore database (`R.string.firestore_database_id`).
 * - Handles incoming FCM data and notification payloads for real-time messages and WebRTC calls.
 */
class NeoTactileMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token: ${token.take(12)}...")
        val currentUser = Firebase.auth.currentUser ?: return
        try {
            val dbId = applicationContext.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(dbId)
            db.collection("users").document(currentUser.uid)
                .update(
                    mapOf(
                        "fcmToken" to token.take(300),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update FCM token in Firestore", e)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        val type = data["type"] ?: "MESSAGE"
        val title = remoteMessage.notification?.title
            ?: data["senderName"]
            ?: getString(R.string.app_name)
        val body = remoteMessage.notification?.body
            ?: data["content"]
            ?: "New message received"

        if (type == "CALL") {
            val callType = data["callType"] ?: "AUDIO"
            NotificationHelper.showIncomingCallNotification(
                context = applicationContext,
                callerName = title,
                callType = callType
            )
        } else {
            NotificationHelper.showMessageNotification(
                context = applicationContext,
                senderName = title,
                messageText = body
            )
        }
    }

    companion object {
        private const val TAG = "NeoTactileFCM"
    }
}

object NotificationHelper {
    private const val CHANNEL_MESSAGES = "neotactile_messages_channel"
    private const val CHANNEL_CALLS = "neotactile_calls_channel"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val msgChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time incoming chat message notifications"
                enableVibration(true)
            }
            val callChannel = NotificationChannel(
                CHANNEL_CALLS,
                "Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming voice and video call alerts"
                enableVibration(true)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                    null
                )
            }
            manager.createNotificationChannel(msgChannel)
            manager.createNotificationChannel(callChannel)
        }
    }

    private fun canPostNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showMessageNotification(
        context: Context,
        senderName: String,
        messageText: String
    ) {
        if (!canPostNotifications(context)) return
        ensureChannels(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            senderName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setContentTitle(senderName)
            .setContentText(messageText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context)
                .notify(senderName.hashCode() and 0x7FFFFFFF, notification)
        }
    }

    fun showIncomingCallNotification(
        context: Context,
        callerName: String,
        callType: String
    ) {
        if (!canPostNotifications(context)) return
        ensureChannels(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (callerName + "_call").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val callLabel = if (callType == "VIDEO") "Incoming video call" else "Incoming voice call"
        val notification = NotificationCompat.Builder(context, CHANNEL_CALLS)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle(callerName)
            .setContentText(" WhatsApp $callLabel")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context)
                .notify((callerName + "_call").hashCode() and 0x7FFFFFFF, notification)
        }
    }
}
