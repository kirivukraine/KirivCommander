# KirivCommander ProGuard rules

# libsu
-keep class com.topjohnwu.superuser.** { *; }

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }

# Coil
-keep class coil.** { *; }

# Commons Compress
-keep class org.apache.commons.compress.** { *; }

# SMBj
-keep class com.hierynomus.** { *; }

# JSch
-keep class com.jcraft.** { *; }

# Parcelize
-keep class * implements android.os.Parcelable { *; }
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Keep all Kotlin metadata
-keepattributes *Annotation*, Signature, Exception

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
