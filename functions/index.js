const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

/**
 * Dispatches a high-priority FCM push notification to all participants in a 1-to-1
 * conversation whenever a new message document is created in:
 *   /conversations/{chatId}/messages/{messageId}
 */
exports.onNewConversationMessage = onDocumentCreated(
  "conversations/{chatId}/messages/{messageId}",
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const msg = snap.data();
    const chatId = event.params.chatId;
    const messageId = event.params.messageId;
    const senderId = msg.senderId || "";
    const senderName = msg.senderName || "NeoTactile Contact";
    const textPreview = msg.text || `[${msg.mediaType || "Media"}]`;

    const convSnap = await db.collection("conversations").doc(chatId).get();
    if (!convSnap.exists) return;

    const participants = convSnap.data().participants || [];
    const recipientIds = participants.filter((uid) => uid && uid !== senderId);

    for (const recipientId of recipientIds) {
      const userSnap = await db.collection("users").doc(recipientId).get();
      if (!userSnap.exists) continue;
      const fcmToken = userSnap.data().fcmToken;
      if (!fcmToken) continue;

      await admin.messaging().send({
        token: fcmToken,
        data: {
          type: "chat_message",
          chatId: String(chatId),
          messageId: String(messageId),
          senderName: String(senderName),
          body: String(textPreview),
        },
        notification: {
          title: String(senderName),
          body: String(textPreview),
        },
        android: {
          priority: "high",
          notification: {
            channelId: "neotactile_messages",
            priority: "max",
          },
        },
      });
    }
  }
);

/**
 * Dispatches an urgent incoming WebRTC call FCM push notification when a new
 * call signaling document is created in:
 *   /calls/{callId}
 */
exports.onIncomingWebRtcCall = onDocumentCreated(
  "calls/{callId}",
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const callData = snap.data();
    if (callData.status !== "RINGING") return;

    const callId = event.params.callId;
    const calleeId = callData.calleeId;
    const callerName = callData.callerName || "Incoming Call";
    const callType = callData.callType || "AUDIO";

    if (!calleeId) return;

    const calleeSnap = await db.collection("users").doc(calleeId).get();
    if (!calleeSnap.exists) return;

    const fcmToken = calleeSnap.data().fcmToken;
    if (!fcmToken) return;

    await admin.messaging().send({
      token: fcmToken,
      data: {
        type: "incoming_call",
        callId: String(callId),
        callerName: String(callerName),
        callType: String(callType),
        body: `Incoming ${callType.toLowerCase()} call from ${callerName}`,
      },
      notification: {
        title: callerName,
        body: `Incoming ${callType.toLowerCase()} call`,
      },
      android: {
        priority: "high",
        ttl: 30000,
        notification: {
          channelId: "neotactile_calls",
          priority: "max",
        },
      },
    });
  }
);
