# Security Specification (Payload-First Security TDD)

## 1. Data Invariants
1. **Authentication Gate**: No unauthenticated read or write is permitted on any path (`request.auth != null`).
2. **PII Isolation (`/users/{userId}`)**: User profiles store `email` and `fcmToken`; `get` and `list` are strictly restricted to `request.auth.uid == userId` or `isAdmin()`.
3. **Participant-Scoped Messaging (`/conversations/{chatId}/messages/{msgId}`)**: Only participants (`senderId`, `receiverId`, or `request.auth.uid in resource.data.participantIds`) may read/list messages. Only the authenticated `senderId == request.auth.uid` may create a message. Only `readStatus` and `updatedAt` may be mutated on update, while `messageId`, `chatId`, `senderId`, `receiverId`, and `timestamp` are immortal.
4. **Scoped Ephemeral Stories (`/stories/{storyId}`)**: `list` and `get` require `resource.data.authorId == request.auth.uid || request.auth.uid in resource.data.visibleToUids || isAdmin()`.
5. **Hardware Gatekeeper Integrity (`/blocked_devices/{androidId}`)**: A standard user can register their own device ONLY with `banned == false` and `ownerUid == request.auth.uid`. A non-admin user can NEVER flip `banned` from `true` to `false` or ban another user's device.
6. **Admin Control Center (`/admin_config/settings` & `/admins/{adminId}`)**: Only verified admins (`exists(/databases/$(database)/documents/admins/$(request.auth.uid))` or verified `msftashop@gmail.com` with `email_verified == true`) can create or update `/admin_config/settings` or manage `/admins/{adminId}`.

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Profile Read**: `unauthDb.collection("users").doc("alice").get()` -> Reject (`PERMISSION_DENIED`).
2. **Cross-User PII Leak**: Bob (`bob_456`) calls `db.collection("users").doc("alice_123").get()` -> Reject (`PERMISSION_DENIED`).
3. **Unfiltered User List Scraping**: Alice calls `db.collection("users").get()` without `.where("uid", "==", "alice_123")` -> Reject (`PERMISSION_DENIED`).
4. **Shadow Field Injection on Profile**: Alice updates `/users/alice_123` with `{ isVerifiedAdmin: true }` -> Reject via `hasOnly()`.
5. **Identity Spoofing on Chat Message**: Bob creates `/conversations/c1/messages/m1` with `senderId: "alice_123"` -> Reject (`incoming().senderId == request.auth.uid`).
6. **Unauthorized Third-Party Message Read**: Mallory (`mallory_789`) reads `/conversations/c1/messages/m1` between Alice and Bob -> Reject (`PERMISSION_DENIED`).
7. **Message Content Tampering on Update**: Receiver Bob attempts to modify `content` of Alice's message during a `readStatus` update -> Reject via `affectedKeys().hasOnly(['readStatus', 'updatedAt'])`.
8. **Unaligned Story List Query**: Alice calls `db.collection("stories").get()` without `.where("visibleToUids", "array-contains", "alice_123")` -> Reject (`PERMISSION_DENIED`).
9. **Self-Unbanning a Blocked Device**: Banned user Alice attempts to update `/blocked_devices/hw_alice` with `{ banned: false }` -> Reject (`incoming().banned == existing().banned` for non-admin).
10. **Non-Admin Modifying Admin Config**: Regular user Bob writes to `/admin_config/settings` -> Reject (`isAdmin()` required).
11. **Admin Email Spoofing (Unverified Email)**: Attacker authenticates with token `{ email: "msftashop@gmail.com", email_verified: false }` and attempts to write `/admin_config/settings` -> Reject (`request.auth.token.email_verified == true` required).
12. **Value Poisoning / Oversized Payload**: Alice creates a message with `mediaType: "EXE_PAYLOAD"` or 5000-char `content` -> Reject via `isValidChatMessage()`.
