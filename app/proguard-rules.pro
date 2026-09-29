# MessMate ProGuard / R8 Rules

# Preserve generic type signatures and annotations for reflection/serialization
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Preserve DTOs and Domain Models used with Gson
-keep class com.messmate.android.data.remote.dto.** { *; }
-keep class com.messmate.android.domain.model.** { *; }

# Retrofit 2
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# OkHttp 3 & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keepclassmembers enum * { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Hilt / Dagger
-keep class * extends dagger.hilt.internal.GeneratedComponentManager { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponentManager { *; }
-keepclassmembers class * {
    @javax.inject.* *;
    @dagger.* *;
}

# Firebase Cloud Messaging
-dontwarn com.google.firebase.**
-keep class com.google.firebase.** { *; }
