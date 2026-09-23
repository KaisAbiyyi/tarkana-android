# Minimal, evidence-based ProGuard/R8 rules for Tarkana Android
# Do not add blanket SDK keep rules for libraries not in use.

# Preserve line numbers and source files for release stack trace deobfuscation via mapping.txt
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve data models serialized/deserialized with org.json
-keep class com.kaisabiyyistudio.tarkana_android.model.** { *; }

# Preserve AndroidX Security Crypto primitives used by AuthSession EncryptedSharedPreferences
-keep class androidx.security.crypto.** { *; }
-dontwarn javax.annotation.**

# Preserve Facebook Shimmer layout components referenced in layouts
-keep class com.facebook.shimmer.ShimmerFrameLayout { *; }