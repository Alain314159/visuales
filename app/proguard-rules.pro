# ProGuard / R8 rules for Visuales

# --- General ---
-dontwarn org.jetbrains.annotations.**
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# --- Kotlin ---
-dontwarn kotlin.**
-keepclassmembers class kotlin.Metadata { *; }

# --- Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# --- Room ---
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# --- Retrofit / OkHttp ---
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# --- Jsoup ---
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# --- kotlinx.serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# --- Coil ---
-dontwarn coil.**

# --- Media3 ---
-dontwarn androidx.media3.**

# --- Hilt ---
-keep class com.alain.visuales.** { *; }
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
