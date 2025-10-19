# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep all public APIs
-keep public class com.swiftest.core.** {
    public *;
}

# Keep callback interfaces
-keep interface com.swiftest.core.interfaces.** {
    *;
}

# Keep model classes
-keep class com.swiftest.core.models.** {
    *;
}

# OkHttp rules
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# WebSocket specific
-keep class okhttp3.ws.** { *; }
-keep interface okhttp3.ws.** { *; }