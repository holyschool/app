# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for readable crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------------------
# Gson / reflection-based (de)serialization
# ---------------------------------------------------------------------------
# Gson reads/writes fields reflectively by their declared name, so model classes
# and their fields must not be renamed or removed.
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# App + library models that are read/written with Gson.
-keep class com.wiffles.edupage.data.** { *; }
-keep class com.edupage.api.model.** { *; }
-keep class com.wiffles.edupage.network.** { *; }
-keep class com.wiffles.edupage.ui.widgets.** { *; }

# Keep enum values (Gson stores/reads them by name).
-keepclassmembers enum * { *; }

# Kotlin metadata used by reflection / coroutines.
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }

# ---------------------------------------------------------------------------
# Networking
# ---------------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn com.google.crypto.tink.**

# ---------------------------------------------------------------------------
# Firebase / Google Play services (their libraries ship their own rules, but
# keep these informational warnings quiet for optional components).
# ---------------------------------------------------------------------------
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**