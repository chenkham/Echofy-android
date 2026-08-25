# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
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

## Google Mobile Ads (AdMob)
# R8 is enabled for release, and without these keeps the ads SDK loses classes it
# resolves reflectively, so ads silently fail to fill in the release APK only.
-keep class com.google.android.gms.ads.** { *; }
-keep interface com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

## ONNX Runtime (openWakeWord "Hey Jarvis")
# The native library resolves these classes by name through JNI (GetMethodID/FindClass), so
# R8 cannot see the references. Stripping them made OrtSession.run() abort the whole process
# with "ClassNotFoundException: ai.onnxruntime.TensorInfo" -> SIGABRT, which crashed the app
# on launch in release builds only.
-keep class ai.onnxruntime.** { *; }
-keep interface ai.onnxruntime.** { *; }
-keepclasseswithmembernames class ai.onnxruntime.** {
    native <methods>;
}
-dontwarn ai.onnxruntime.**

## Kotlin Serialization
# Keep `Companion` object fields of serializable classes.
# This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
-if @kotlinx.serialization.Serializable class **
-keepclasseswithmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclasseswithmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclasseswithmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,Signature

-dontwarn javax.servlet.ServletContainerInitializer
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
-dontwarn org.slf4j.impl.StaticLoggerBinder

## Rules for NewPipeExtractor
-keep class org.schabi.newpipe.extractor.timeago.patterns.** { *; }
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.javascript.engine.** { *; }
-dontwarn org.mozilla.javascript.JavaToJSONConverters
-dontwarn org.mozilla.javascript.tools.**
-keep class javax.script.** { *; }
-dontwarn javax.script.**
-keep class jdk.dynalink.** { *; }
-dontwarn jdk.dynalink.**

## Logging (does not affect Timber)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    ## Leave in release builds
    #public static int i(...);
    #public static int w(...);
    #public static int e(...);
}

# Generated automatically by the Android Gradle plugin.
-dontwarn java.beans.BeanDescriptor
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor
-dontwarn okhttp3.internal.Util

# Additional optimizations for smaller APK size
-optimizationpasses 5
-allowaccessmodification
-repackageclasses ''

# ==============================================================================
# CRASH DIAGNOSTICS — keep release stack traces readable
# ==============================================================================
# Without these, every Play Console crash report is obfuscated line noise. R8 still
# renames everything; this only preserves the mapping data needed to decode a trace,
# so it costs nothing at runtime and no size worth measuring.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ==============================================================================
# BASELINE PROFILE / ProfileInstaller
# ==============================================================================
# ProfileInstaller is triggered by the framework, not by app code, so R8 full mode
# can consider it unreachable and strip it. If that happens the baseline profile is
# never installed and the startup win silently disappears with no error.
-keep class androidx.profileinstaller.** { *; }
-dontwarn androidx.profileinstaller.**

# ==============================================================================
# WORKMANAGER — release radar worker is instantiated reflectively by name
# ==============================================================================
# WorkManager constructs workers via reflection from a class name string. Under R8
# full mode the class is renamed, the lookup fails at runtime, and the scheduled work
# throws instead of running. This is a release-only failure.
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Remove unused code more aggressively
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkNotNull(...);
    static void checkNotNullParameter(...);
    static void checkParameterIsNotNull(...);
    static void checkNotNullExpressionValue(...);
    static void checkExpressionValueIsNotNull(...);
    static void checkReturnedValueIsNotNull(...);
    static void throwUninitializedPropertyAccessException(...);
}

# Remove verbose/info Timber logs in release, but keep debug for Jam troubleshooting
-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** i(...);
}

# ==============================================================================
# SOCKET.IO — Required for Together (Listen Together) feature
# ==============================================================================
# Socket.IO uses reflection, engine.io transports, and OkHttp WebSocket internally.
# Without these rules, R8 strips critical transport classes, breaking connections.
-keep class io.socket.** { *; }
-keep class io.socket.client.** { *; }
-keep class io.socket.engineio.** { *; }
-keep class io.socket.parser.** { *; }
-dontwarn io.socket.**

# Keep Jam models (used with JSON serialization via JSONObject)
-keep class com.Chenkham.Echofy.jam.** { *; }

# Radio Browser API models. RadioStation has non-nullable fields without defaults, so if R8
# renames them or drops the generated serializer, every station response fails to parse and
# the radio screen shows an error instead of stations.
-keep class com.Chenkham.radiobrowser.** { *; }
-keepclassmembers class com.Chenkham.radiobrowser.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Same reasoning for the podcast and InnerTube API models: PodcastResult.title, Author.name
# and Episode.title are non-nullable without defaults, so a stripped serializer breaks the
# podcast screen rather than degrading gracefully.
-keep class com.Chenkham.ytmusicapi.** { *; }
-keepclassmembers class com.Chenkham.ytmusicapi.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.Chenkham.innertube.** { *; }
-keepclassmembers class com.Chenkham.innertube.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Coil optimizations
-dontwarn coil.**

# Ktor optimizations
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }

# ==============================================================================
# SPOTIFY-LIKE PERFORMANCE OPTIMIZATIONS
# ==============================================================================

# Compose performance optimizations
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Optimize Compose recomposition
-assumenosideeffects class androidx.compose.runtime.Loom* { *; }

# Keep Coil for fast image loading
-keep class coil.** { *; }
-keep class coil.compose.** { *; }

# Media3/ExoPlayer optimizations for smooth playback
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keep class com.google.android.exoplayer2.** { *; }

# Room database optimizations
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }

# Kotlin coroutines optimizations (faster async operations)
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Remove all debugs for release performance
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Optimize startup - don't obfuscate startup-critical classes
-keep class com.Chenkham.Echofy.App { *; }
-keep class com.Chenkham.Echofy.MainActivity { *; }
-keep class com.Chenkham.Echofy.playback.MusicService { *; }

# Firebase optimizations
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Appwrite optimizations
-keep class io.appwrite.** { *; }
-dontwarn io.appwrite.**

# Gson optimizations
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# OkHttp optimizations for network performance
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Optimization flags for R8
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-dontskipnonpubliclibraryclassmembers

# Aggressive inlining for faster execution
-allowaccessmodification
-repackageclasses ''
-flattenpackagehierarchy

# ==============================================================================
# GOOGLE ADMOB
# ==============================================================================

# Google Mobile Ads (AdMob)
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# Keep ad-related models
-keep class com.Chenkham.Echofy.ads.** { *; }
