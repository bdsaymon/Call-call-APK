# NeoTactile Chat — Native Android Messaging, Calling & Communities Platform

Production-grade native Android messaging and real-time communication application built with **Kotlin**, **Jetpack Compose (Material 3)**, **Android XML Layouts & Navigation**, **Room SQLite**, **Cloud Firestore**, **Firebase Authentication (Credential Manager Google OAuth + Anti-Fake Manual Verification)**, **MediaRecorder / Media3 ExoPlayer**, and a companion **Java 21 Spring Boot + WebRTC Signaling Backend**.

---

## 1. Repository Structure

```text
.
├── .env.example                                   # Safe environment template (no private secrets)
├── .github/
│   └── workflows/
│       └── android.yml                            # Automated GitHub Actions Debug & Release APK workflow
├── .gitignore                                     # Excludes .env, *.jks, *.keystore, local.properties, /build
├── README.md                                      # Architecture, SDK, Firebase, Backend & CI/CD documentation
├── build.gradle.kts                               # Root Gradle configuration (Kotlin DSL)
├── settings.gradle.kts                            # Project settings & repository definitions
├── gradle.properties                              # JVM, parallel build, and Google Services flags
├── gradlew                                        # Unix Gradle Wrapper executable script
├── gradlew.bat                                    # Windows Gradle Wrapper batch script
├── gradle/
│   ├── libs.versions.toml                         # Centralized Gradle Version Catalog
│   └── wrapper/
│       ├── gradle-wrapper.jar                     # Gradle Wrapper binary
│       └── gradle-wrapper.properties              # Gradle 9.3.1 distribution configuration
├── debug.keystore.base64                          # Base64-encoded standard Android debug keystore
├── firebase.json                                  # Firebase project & Firestore rules configuration
├── firebase-blueprint.json                        # Firestore schema blueprint
├── firestore.rules                                # Zero-trust Cloud Firestore security rules
├── firestore.test.js                              # Firebase Emulator security rules test suite
├── app/
│   ├── build.gradle.kts                           # Android app module configuration, signingConfigs & dependencies
│   ├── google-services.json                       # Firebase Android client configuration
│   ├── proguard-rules.pro                         # ProGuard / R8 rules for Release builds
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml                # App permissions, hardware features & MainActivity
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt                # App entry point, runtime permissions & navigation shell
│       │   │   ├── audio/TactileAudioEngine.kt    # AAC voice recorder, Media3 player & mobile storage manager
│       │   │   ├── data/
│       │   │   │   ├── local/                     # Room SQLite database (`local_aliases` table)
│       │   │   │   ├── model/DomainModels.kt      # UserProfile, ChatMessage, StoryItem, CallLogEntry, etc.
│       │   │   │   ├── remote/FirestoreError.kt   # Structured Firestore error diagnostics
│       │   │   │   └── repository/                # NeoTactileRepository (Firestore + Room + Manual Auth)
│       │   │   ├── di/AppModule.kt                # Constructor dependency injection provider
│       │   │   ├── security/                      # Hardware ANDROID_ID & Biometric/PIN security gatekeeper
│       │   │   └── ui/
│       │   │       ├── auth/AuthScreen.kt         # Liquid Glassmorphic Login/Register + Captcha + Google OAuth
│       │   │       ├── components/                # Reusable Material 3 & Glassmorphic UI components
│       │   │       ├── screens/                   # HomeScreen, ChatScreen, UsersDirectoryScreen, ProfileScreen
│       │   │       ├── theme/                     # Emerald Dark & Clean Green/White Light themes
│       │   │       └── viewmodel/                 # NeoTactileViewModel (state management)
│       │   └── res/
│       │       ├── drawable/                      # Adaptive launcher icons & vector artwork
│       │       ├── layout/activity_main_host.xml  # Native XML host layout
│       │       ├── mipmap-anydpi-v26/             # Custom adaptive launcher icon definitions
│       │       ├── navigation/                    # Android XML navigation graph (`mobile_navigation.xml`)
│       │       └── values/                        # strings.xml, themes.xml, firebase_applet_config.xml
│       └── test/java/com/example/                 # Unit, Robolectric & Firestore rules test suites
└── backend/
    ├── pom.xml                                    # Java 21 + Spring Boot 3.3.5 Maven configuration
    └── src/main/
        ├── java/com/neotactile/backend/           # Spring Security, WebRTC WebSocket Signaling & REST APIs
        └── resources/
            ├── application.yml                    # Environment-driven PostgreSQL, Redis & STUN/TURN config
            └── db/migration/V1__init_schema.sql   # Flyway PostgreSQL schema migration
```

---

## 2. Required Android SDK & Build Toolchain Configurations

| Component | Version / Configuration |
| :--- | :--- |
| **JDK Version** | **Java 21** (Temurin / OpenJDK 21 required for AGP 9.1.1 & Gradle 9.3.1; bytecode target `JavaVersion.VERSION_11`) |
| **Gradle Wrapper** | **Gradle 9.3.1** (`gradle/wrapper/gradle-wrapper.properties`) |
| **Android Gradle Plugin (AGP)** | **9.1.1** (`gradle/libs.versions.toml`) |
| **Kotlin Version** | **2.2.10** with KSP **2.3.5** |
| **compileSdk** | **API 36 (minorApiLevel = 1)** (`platforms;android-36.1`) |
| **targetSdk** | **36** |
| **minSdk** | **24** (Android 7.0+) |
| **Build Tools** | **36.0.0** |

### Installing Required SDK Packages via `sdkmanager` (Ubuntu / CLI)
```bash
yes | sdkmanager --licenses
sdkmanager "platform-tools" "build-tools;36.0.0" "platforms;android-36" "platforms;android-36.1"
```

---

## 3. Automated APK Generation via GitHub Actions (`.github/workflows/android.yml`)

The workflow at `.github/workflows/android.yml` runs automatically on every `push` and `pull_request` to `main`, `master`, or `develop`, and supports manual execution via `workflow_dispatch`.

### What the Workflow Does
1. **Ubuntu Runner & Toolchain Setup**: Provisions `ubuntu-latest`, installs **Temurin JDK 21**, sets up the **Android SDK** (`platforms;android-36.1`, `build-tools;36.0.0`), and enables Gradle dependency caching.
2. **Unit & Robolectric Tests**: Executes `:app:testDebugUnitTest` in headless mode.
3. **Automated Debug APK Build**:
   - Restores `debug.keystore` from `debug.keystore.base64` (or generates a standard debug key).
   - Runs `./gradlew :app:assembleDebug --no-daemon`.
   - Uploads `app/build/outputs/apk/debug/app-debug.apk` as the **`NeoTactile-debug-apk`** workflow artifact.
4. **Automated Release APK Build**:
   - Uses GitHub Repository Secrets (`RELEASE_KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`) if configured; otherwise generates an ephemeral CI signing keystore (alias `upload`) so Release APK builds succeed out-of-the-box without committing private keys.
   - Runs `./gradlew :app:assembleRelease --no-daemon`.
   - Uploads `app/build/outputs/apk/release/app-release.apk` as the **`NeoTactile-release-apk`** workflow artifact.
   - Deletes all temporary keystore files in an `always()` cleanup step.

### Configuring Production Release Signing Secrets in GitHub (Optional)
Never commit `.jks` or `.keystore` files to Git. To sign Release APKs with your own production upload key in GitHub Actions, navigate to **GitHub Repository → Settings → Secrets and variables → Actions** and add:

| GitHub Secret Name | Description |
| :--- | :--- |
| `RELEASE_KEYSTORE_BASE64` | Base64 string of your upload keystore (`base64 -w 0 my-upload-key.jks`) containing key alias `upload` |
| `STORE_PASSWORD` | Keystore password for `my-upload-key.jks` |
| `KEY_PASSWORD` | Key password for the `upload` key alias |

---

## 4. Local Build Instructions

### Step 1: Prepare Environment File
```bash
cp .env.example .env
```

### Step 2: Prepare Debug Keystore
```bash
base64 --decode debug.keystore.base64 > debug.keystore
```

### Step 3: Build Debug APK
```bash
./gradlew :app:assembleDebug
```
- **Output Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`

### Step 4: Build Signed Release APK
```bash
export KEYSTORE_PATH="/absolute/path/to/my-upload-key.jks"
export STORE_PASSWORD="your_keystore_password"
export KEY_PASSWORD="your_key_password"
./gradlew :app:assembleRelease
```
- **Output Release APK**: `app/build/outputs/apk/release/app-release.apk`

### Step 5: Run Unit & Robolectric Tests
```bash
./gradlew :app:testDebugUnitTest
```

---

## 5. Firebase & Backend Configuration Requirements

### A. Firebase Configuration (Authentication & Cloud Firestore)
1. **Config Files**:
   - `app/google-services.json` provides the Firebase project configuration. Note that `gradle.properties` sets `googleServices.missing.passthrough=true` and `app/build.gradle.kts` sets `MissingGoogleServicesStrategy.WARN` so builds also succeed in clean CI environments.
   - `app/src/main/res/values/firebase_applet_config.xml` defines `firestore_database_id` and `default_web_client_id`.
2. **Google Sign-In (Credential Manager)**:
   - Register the SHA-1 and SHA-256 certificate fingerprints of your Debug and Release signing keys in the **Firebase Console → Project Settings → Your Apps (Android)**.
3. **Cloud Firestore Security Rules**:
   - Zero-trust security rules are defined in `/firestore.rules` and verified by `/firestore.test.js`.
   - Deploy rules using `firebase deploy --only firestore:rules`.

### B. Spring Boot & WebRTC Signaling Backend (`/backend`)
The `/backend` directory contains a standalone Java 21 + Spring Boot 3.3.5 server for REST user management and WebSocket WebRTC signaling (`/ws/signaling`). All sensitive credentials are injected strictly via environment variables (no secrets are stored in `application.yml`):

| Environment Variable | Purpose | Default (Local Dev) |
| :--- | :--- | :--- |
| `DATABASE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/neotactile_db` |
| `DATABASE_USER` | PostgreSQL username | `neotactile_user` |
| `DATABASE_PASSWORD` | PostgreSQL password | *(Set via environment)* |
| `REDIS_HOST` / `REDIS_PORT` | Redis pub/sub & session cache | `localhost` / `6379` |
| `JWT_SECRET_KEY` | HMAC-SHA256 JWT signing secret | *(Set via environment)* |
| `WEBRTC_STUN_URL` | WebRTC STUN server URI | `stun:stun.l.google.com:19302` |
| `WEBRTC_TURN_URL` | WebRTC TURN relay server URI | `turn:turn.neotactile.example.com:3478` |
| `WEBRTC_TURN_USER` | TURN credential username | *(Set via environment)* |
| `WEBRTC_TURN_SECRET` | TURN credential password/secret | *(Set via environment)* |
