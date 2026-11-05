# NeoTactile — Production Native Android Messaging, Calling & Communities Platform

## 1. Architecture Overview
- **Native Android Application (`/app`)**:
  - Written 100% in Kotlin using Android SDK, Jetpack Compose (Material 3), Android XML Layouts (`res/layout/activity_main_host.xml`), Android Navigation Resources (`res/navigation/mobile_navigation.xml`), Room SQLite Database (`local_aliases`), MediaRecorder (AAC 44.1kHz), Media3 ExoPlayer, and Jetpack BiometricPrompt.
  - **Green & White Design Language**: Supports both Clean Emerald Green & White Light Mode and Emerald Deep Slate Dark Mode (switchable from the Top Bar and Settings).
  - **Strict No-Demo Policy**: Zero hardcoded fake contacts, zero mock messages, and zero simulated histories. All contacts, groups, communities, statuses, messages, and call sessions are persisted in real databases (Cloud Firestore + Room SQLite).
- **Java Spring Boot & PostgreSQL Backend (`/backend`)**:
  - Java 21 + Spring Boot 3.3.5 (`backend/pom.xml`)
  - Spring Security + BCrypt + OAuth2 / JWT Stateless Authentication
  - Spring WebSocket (`/ws/signaling`) for real-time WebRTC SDP/ICE signaling & messaging
  - Flyway PostgreSQL Database Migration (`backend/src/main/resources/db/migration/V1__init_schema.sql`)

## 2. Core Functional Modules
1. **Chats**: 1-on-1 & Group real-time messaging, replies, edits, deletions, forwarding, emoji reactions, voice note recording & playback, photo compression & sharing, document/file attachments, pinned/archived filters, and starred messages.
2. **Updates (24-Hour Status)**: Ephemeral 24-hour text/media statuses with hold-to-pause progress bar and contact visibility isolation.
3. **Calls (WebRTC Audio & Video)**: Real-time WebRTC signaling over Firestore & WebSocket with STUN/TURN configuration, front/rear camera switching, microphone mute/unmute, speakerphone toggle, live call duration timer, and call history logs.
4. **Users, Groups & Communities**:
   - Search and add real registered users by `@username`, email address, phone number, or 6-character Short ID (`NT-XXXX`) / QR Code.
   - Block/unblock users, customize private device-only aliases (Room SQLite), create multi-member Groups, and create Communities with multi-group broadcast announcements.
5. **Complete Settings**: Account, Username & Phone, Privacy, Biometric/PIN App Lock, Theme (Green/White Light & Dark), App Language (Bengali, English, Bilingual), Read Receipts, Notifications, Two-Step Verification, Chat Wallpaper, Blocked Contacts, Storage & Data, and Linked Devices.

## 3. External Dependencies & Infrastructure Configuration
- **Firebase Auth & Cloud Firestore**: Configured via `app/google-services.json` and `app/src/main/res/values/firebase_applet_config.xml`. Zero-trust rules enforced in `/firestore.rules`.
- **Google Authentication**: Uses Jetpack `CredentialManager` (`GetSignInWithGoogleOption`) with `R.string.default_web_client_id`.
- **WebRTC STUN / TURN Servers**:
  - Default STUN: `stun:stun.l.google.com:19302`
  - Production TURN: Configure `WEBRTC_TURN_URL`, `WEBRTC_TURN_USER`, and `WEBRTC_TURN_SECRET` in `backend/src/main/resources/application.yml` or environment variables.
- **PostgreSQL & Redis (for standalone Spring Boot deployment)**:
  - Set `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, and `REDIS_HOST` environment variables, then run `mvn spring-boot:run` in `/backend`.
