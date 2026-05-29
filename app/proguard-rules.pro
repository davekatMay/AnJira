# Add project specific ProGuard rules here.
-keepattributes Signature
-keepattributes *Annotation*

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Gson
-keep class com.anjira.taskplanner.data.remote.dto.** { *; }
-keep class com.anjira.taskplanner.domain.model.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
