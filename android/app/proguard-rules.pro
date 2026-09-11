# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Cast SDK
-keep class com.google.android.gms.cast.** { *; }
-dontwarn com.google.android.gms.cast.**

# okhttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Coil
-keep class coil.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Guava (used by media3 session for ImmutableList, ListenableFuture)
-keep class com.google.common.** { *; }
-dontwarn com.google.common.**

# App data classes used in JSON parsing (Station, EpgSchedule, etc.)
-keep class dev.openradio.android.data.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Guava (used by media3 session for ImmutableList, ListenableFuture)
-keep class com.google.common.** { *; }
-dontwarn com.google.common.**

# App data classes used in JSON parsing (Station, EpgSchedule, etc.)
-keep class dev.openradio.android.data.** { *; }
