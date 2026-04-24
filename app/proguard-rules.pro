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

# ── KEEP GSON AND NETWORK / MCP MODELS ──────────────────────
-keepattributes *Annotation*, Signature
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }

# Keep all data models so Gson serialization doesn't break in release builds
-keep class com.forge.vdesign.mcp.** { *; }
-keep class com.forge.vdesign.domain.model.** { *; }
-keep class com.forge.vdesign.skills.models.** { *; }
-keep class com.forge.vdesign.brain.** { *; }