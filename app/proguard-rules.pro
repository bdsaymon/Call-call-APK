# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preservation rules for WebRTC native JNI and reflection
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# Firebase SDK preservation
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# AndroidX Room
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# AndroidX Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# WebView & JavaScript interfaces
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class android.webkit.** { *; }

# Keep data model classes for serialization & reflection
-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.model.** { *; }
-keep class com.example.NeoTactileApplication { *; }

