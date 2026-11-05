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

// 1. Unauthenticated user cannot read users
test("Unauthenticated user: cannot read user profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

// 2. PII Isolation: Authenticated user cannot read another user's profile
test("PII Isolation: Bob cannot read Alice's user profile containing email", async () => {
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

// 3. Authenticated owner can create and update their own profile, but Shadow Update fails
test("Owner can create/update profile; Shadow Update with ghost key is rejected", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice Neo",
      bio: "Tactile architect",
      isPrivate: false,
      friendsList: [BOB_UID],
      isOnline: true,
      lastSeen: now,
      createdAt: now,
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

// 4. Chat message creation, participant query alignment, and third-party rejection
test("Chat messages: participants can create and query aligned messages; third-party fails", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const malloryDb = testEnv.authenticatedContext(MALLORY_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("conversations").doc("chat_alice_bob").collection("messages").doc("msg_1").set({
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

  // Bob can query with participantIds array-contains
  await assertSucceeds(
    bobDb
      .collection("conversations")
      .doc("chat_alice_bob")
      .collection("messages")
      .where("participantIds", "array-contains", BOB_UID)
      .get()
  );

  // Mallory cannot read or query
  await assertFails(
    malloryDb.collection("conversations").doc("chat_alice_bob").collection("messages").doc("msg_1").get()
  );
});

// 5. Stories query alignment test
test("Stories: aligned visibleToUids query succeeds; unaligned blanket query fails", async () => {
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

  // Blanket unaligned list query must fail
  await assertFails(bobDb.collection("stories").get());
});

// 6. Hardware Gatekeeper & Email Spoofing Test
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

  // Banned user Alice tries to unban her own device
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("blocked_devices").doc("hw_alice_99").update({
      banned: false,
      updatedAt: now,
    })
  );

  // Spoofed admin email with email_verified: false tries to unban or update admin_config
  const spoofAdminDb = testEnv
    .authenticatedContext("spoof_uid", { email: "msftashop@gmail.com", email_verified: false })
    .firestore();
  await assertFails(
    spoofAdminDb.collection("blocked_devices").doc("hw_alice_99").update({
      banned: false,
      updatedAt: now,
    })
  );

  // Verified admin in /admins/{uid} succeeds
  const realAdminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  await assertSucceeds(
    realAdminDb.collection("blocked_devices").doc("hw_alice_99").update({
      banned: false,
      reason: "Cleared by admin",
      updatedAt: now,
    })
  );

  // Reading a non-existent hardware ID (resource == null) before registering heartbeat succeeds
  await assertSucceeds(
    aliceDb.collection("blocked_devices").doc("104530351a2b137a").get()
  );
});
