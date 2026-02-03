# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ===== KIDGUARD SPECIFIC RULES =====

# Keep all KidGuard classes (WICHTIG!)
-keep class com.example.safespark.** { *; }
-keepclassmembers class com.example.safespark.** { *; }

# Keep MainActivity and AccessibilityService (müssen vom System gefunden werden)
-keep public class com.example.safespark.MainActivity
-keep public class com.example.safespark.GuardianAccessibilityService

# ===== KOTLIN =====
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { *; }
-keepclassmembers class kotlin.Metadata { *; }
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

# ===== COROUTINES =====
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.** { *; }

# ===== TENSORFLOW LITE =====
-keep class org.tensorflow.** { *; }
-keepclassmembers class org.tensorflow.** { *; }
-dontwarn org.tensorflow.**

# ===== ROOM DATABASE =====
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ===== ANDROIDX =====
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**
