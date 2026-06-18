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

# --- hoght (Firestore + R8) ---
# Firestore's `toObject()` uses reflection + field/property names.
# If R8 obfuscates model properties, mapping can break in release builds.
-keepnames class com.prod.singles_date.model.** { *; }
-keepclassmembers class com.prod.singles_date.model.** { *; }

# Remove low-value logs from release builds. Keep warnings/errors for crash diagnosis.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}