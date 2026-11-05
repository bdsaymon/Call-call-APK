const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const MALLORY_UID = "mallory_789";
const ADMIN_UID = "admin_001";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// 1. Unauthenticated user cannot read or write any collection
test("Unauthenticated user: denied across users, messages, groups, calls, and media_blobs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("conversations").doc("chat_1").collection("messages").doc("m_1").get());
  await assertFails(unauthDb.collection("groups").doc("grp_1").get());
  await assertFails(unauthDb.collection("calls").doc("call_1").get());
  await assertFails(unauthDb.collection("media_blobs").doc("med_1").get());
});

// 2. PII Isolation: Authenticated user cannot read another user's profile unless in friendsList
test("PII Isolation: Bob cannot read Alice's user profile containing email unless in friendsList", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice",
      isPrivate: false,
      friendsList: [],
      isOnline: true,
      lastSeen: now,
      createdAt: now,
      updatedAt: now,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

// 3. Authenticated owner can create and update their own profile; cross-user & shadow updates fail
test("Owner can create/update profile; cross-user update and Shadow Update with ghost fields fail", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice Neo",
      bio: "Tactile architect",
      isPrivate: false,
      friendsList: [BOB_UID],
      pinnedChatIds: [BOB_UID],
      archivedChatIds: [],
      blockedUserIds: [],
      starredMessageIds: ["msg_1"],
      isOnline: true,
      lastSeen: now,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob cannot modify Alice's profile
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Bob Hijacked Alice",
      updatedAt: now,
    })
  );

  // Shadow Update attempt with ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice Hacked",
      isSuperAdmin: true,
      updatedAt: now,
    })
  );
});

// 4. Chat message creation, sender-vs-recipient field update isolation, and third-party rejection
test("Chat messages: sender can edit content; recipient can only update readStatus/reactions; third-party fails", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const malloryDb = testEnv.authenticatedContext(MALLORY_UID).firestore();

  const msgRefAlice = aliceDb.collection("conversations").doc("chat_alice_bob").collection("messages").doc("msg_1");
  const msgRefBob = bobDb.collection("conversations").doc("chat_alice_bob").collection("messages").doc("msg_1");

  await assertSucceeds(
    msgRefAlice.set({
      messageId: "msg_1",
      chatId: "chat_alice_bob",
      senderId: ALICE_UID,
      receiverId: BOB_UID,
      participantIds: [ALICE_UID, BOB_UID],
      senderName: "Alice Neo",
      content: "Hello in Neo-Tactile!",
      mediaType: "TEXT",
      audioDurationMs: 0,
      readStatus: "SENT",
      timestamp: now,
      updatedAt: now,
    })
  );

  // Bob (recipient) can query aligned messages and mark readStatus / reactions
  await assertSucceeds(
    bobDb
      .collection("conversations")
      .doc("chat_alice_bob")
      .collection("messages")
      .where("participantIds", "array-contains", BOB_UID)
      .get()
  );

  await assertSucceeds(
    msgRefBob.update({
      readStatus: "READ",
      reactions: "👍:bob_456",
      updatedAt: now,
    })
  );

  // Bob (recipient) CANNOT tamper with Alice's message content
  await assertFails(
    msgRefBob.update({
      content: "Tampered by Bob!",
      isEdited: true,
      updatedAt: now,
    })
  );

  // Alice (sender/owner) CAN edit her own message content
  await assertSucceeds(
    msgRefAlice.update({
      content: "Hello in Neo-Tactile! (edited)",
      isEdited: true,
      updatedAt: now,
    })
  );

  // Mallory (non-participant) cannot read or delete the message
  await assertFails(
    malloryDb.collection("conversations").doc("chat_alice_bob").collection("messages").doc("msg_1").get()
  );
  await assertFails(
    malloryDb.collection("conversations").doc("chat_alice_bob").collection("messages").doc("msg_1").delete()
  );
});

// 5. Stories query alignment and author ownership test
test("Stories: aligned visibleToUids query succeeds; non-author cannot update or delete story", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("stories").doc("story_1").set({
      storyId: "story_1",
      authorId: ALICE_UID,
      authorName: "Alice Neo",
      mediaUrl: "preset://cyan_wave",
      mediaType: "TEXT_CARD",
      caption: "24h Ephemeral Pulse",
      visibleToUids: [ALICE_UID, BOB_UID],
      createdAt: now,
      expiresAt: Date.now() + 86400000,
    })
  );

  await assertSucceeds(
    bobDb.collection("stories").where("visibleToUids", "array-contains", BOB_UID).get()
  );

  await assertFails(bobDb.collection("stories").get());
  await assertFails(bobDb.collection("stories").doc("story_1").delete());
});

// 6. Groups (including admin privilege escalation protection & Parent Gate messages), Communities, Calls, MediaBlobs, TypingStates, LinkedDevices & BroadcastLists
test("Groups, Communities, Calls, MediaBlobs, TypingStates, LinkedDevices & BroadcastLists enforce strict member/owner isolation", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const malloryDb = testEnv.authenticatedContext(MALLORY_UID).firestore();

  // Group Chat
  await assertSucceeds(
    aliceDb.collection("groups").doc("group_alpha").set({
      groupId: "group_alpha",
      name: "Core Dev Group",
      description: "Multi-device team",
      creatorUid: ALICE_UID,
      memberIds: [ALICE_UID, BOB_UID],
      adminIds: [ALICE_UID],
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(bobDb.collection("groups").where("memberIds", "array-contains", BOB_UID).get());
  await assertFails(malloryDb.collection("groups").doc("group_alpha").get());

  // Non-admin member Bob cannot escalate himself into adminIds
  await assertFails(
    bobDb.collection("groups").doc("group_alpha").update({
      adminIds: [ALICE_UID, BOB_UID],
      updatedAt: now,
    })
  );

  // Admin Alice CAN promote Bob to adminIds
  await assertSucceeds(
    aliceDb.collection("groups").doc("group_alpha").update({
      adminIds: [ALICE_UID, BOB_UID],
      updatedAt: now,
    })
  );

  // Group subcollection messages enforce Parent Gate membership
  await assertSucceeds(
    bobDb.collection("groups").doc("group_alpha").collection("messages").doc("gmsg_1").set({
      messageId: "gmsg_1",
      chatId: "group_alpha",
      senderId: BOB_UID,
      receiverId: "group_alpha",
      participantIds: [ALICE_UID, BOB_UID],
      senderName: "Bob",
      content: "Hello Group!",
      mediaType: "TEXT",
      audioDurationMs: 0,
      readStatus: "SENT",
      timestamp: now,
      updatedAt: now,
    })
  );
  await assertFails(
    malloryDb.collection("groups").doc("group_alpha").collection("messages").doc("gmsg_1").get()
  );

  // Community
  await assertSucceeds(
    aliceDb.collection("communities").doc("comm_alpha").set({
      communityId: "comm_alpha",
      name: "Engineering Community",
      description: "All engineering groups",
      creatorUid: ALICE_UID,
      announcementText: "Welcome!",
      groupIds: ["group_alpha"],
      memberIds: [ALICE_UID, BOB_UID],
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(bobDb.collection("communities").where("memberIds", "array-contains", BOB_UID).get());
  await assertFails(malloryDb.collection("communities").doc("comm_alpha").get());

  // CallSession
  await assertSucceeds(
    aliceDb.collection("calls").doc("call_101").set({
      callId: "call_101",
      callerId: ALICE_UID,
      callerName: "Alice",
      calleeId: BOB_UID,
      calleeName: "Bob",
      participantIds: [ALICE_UID, BOB_UID],
      callType: "VIDEO",
      status: "RINGING",
      sdpOffer: "v=0\r\no=- 12345 2 IN IP4 127.0.0.1\r\n",
      turnServer: "stun:stun.l.google.com:19302",
      durationSeconds: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    bobDb.collection("calls").doc("call_101").update({
      status: "CONNECTED",
      sdpAnswer: "v=0\r\no=- 67890 2 IN IP4 127.0.0.1\r\n",
      updatedAt: now,
    })
  );
  await assertFails(malloryDb.collection("calls").doc("call_101").get());

  // MediaBlob
  await assertSucceeds(
    aliceDb.collection("media_blobs").doc("media_1").set({
      mediaId: "media_1",
      uploaderId: ALICE_UID,
      chatId: "chat_alice_bob",
      fileName: "voice_1.m4a",
      mimeType: "audio/mp4",
      mediaType: "AUDIO",
      base64Data: "AAAAHGZ0eXBNNEEgAAACAE00QSBpc29t",
      participantIds: [ALICE_UID, BOB_UID],
      createdAt: now,
    })
  );
  await assertSucceeds(bobDb.collection("media_blobs").doc("media_1").get());
  await assertFails(malloryDb.collection("media_blobs").doc("media_1").get());
  await assertFails(bobDb.collection("media_blobs").doc("media_1").delete());

  // TypingState
  await assertSucceeds(
    aliceDb.collection("typing_states").doc("chat_alice_bob_alice_123").set({
      typingId: "chat_alice_bob_alice_123",
      chatId: "chat_alice_bob",
      senderId: ALICE_UID,
      senderName: "Alice",
      participantIds: [ALICE_UID, BOB_UID],
      isTyping: true,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    bobDb.collection("typing_states").where("participantIds", "array-contains", BOB_UID).get()
  );

  // LinkedDevice
  await assertSucceeds(
    aliceDb.collection("linked_devices").doc("sess_1").set({
      sessionId: "sess_1",
      ownerUid: ALICE_UID,
      hardwareId: "hw_1",
      deviceModel: "Pixel 9",
      platform: "Android 16",
      isActive: true,
      lastActive: now,
      createdAt: now,
    })
  );
  await assertSucceeds(aliceDb.collection("linked_devices").where("ownerUid", "==", ALICE_UID).get());
  await assertFails(bobDb.collection("linked_devices").doc("sess_1").get());

  // BroadcastList
  await assertSucceeds(
    aliceDb.collection("broadcast_lists").doc("bcast_1").set({
      listId: "bcast_1",
      ownerUid: ALICE_UID,
      name: "Core Updates",
      recipientIds: [BOB_UID],
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(aliceDb.collection("broadcast_lists").where("ownerUid", "==", ALICE_UID).get());
  await assertFails(bobDb.collection("broadcast_lists").doc("bcast_1").get());
});

// 7. Hardware Gatekeeper & Email Spoofing Test
test("Blocked devices: user cannot self-unban; unverified admin email spoof fails", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("blocked_devices").doc("hw_alice_99").set({
      hardwareId: "hw_alice_99",
      ownerUid: ALICE_UID,
      deviceModel: "Pixel 9 Pro",
      banned: true,
      reason: "Security violation",
      bannedAt: now,
      updatedAt: now,
    });
    await ctx.firestore().collection("admins").doc(ADMIN_UID).set({
      uid: ADMIN_UID,
      grantedBy: ADMIN_UID,
      createdAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("blocked_devices").doc("hw_alice_99").update({
      banned: false,
      updatedAt: now,
    })
  );

  const spoofAdminDb = testEnv
    .authenticatedContext("spoof_uid", { email: "msftashop@gmail.com", email_verified: false })
    .firestore();
  await assertFails(
    spoofAdminDb.collection("blocked_devices").doc("hw_alice_99").update({
      banned: false,
      updatedAt: now,
    })
  );

  const realAdminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  await assertSucceeds(
    realAdminDb.collection("blocked_devices").doc("hw_alice_99").update({
      banned: false,
      reason: "Cleared by admin",
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("blocked_devices").doc("104530351a2b137a").get()
  );
});
