# Kotlin
-dontwarn kotlinx.serialization.**

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Media3 / ExoPlayer
-dontwarn androidx.media3.**
-keep class androidx.media3.** { *; }

# VLC/libVLC
-keep class org.videolan.libvlc.** { *; }
-dontwarn org.videolan.**

# Coil
-dontwarn coil.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# kotlinx-serialization (generated serializers)
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}