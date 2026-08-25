@file:Suppress("UnstableApiUsage")

import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.serialization") version "2.1.0"
    kotlin("kapt")
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.compose.compiler)
    id("com.google.gms.google-services")
}

fun String.toBuildConfigString(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.Chenkham.Echofy"
    //noinspection GradleDependency
    compileSdk = 36

    defaultConfig {
        applicationId = "com.Chenkham.Echofy"
        minSdk = 26
        targetSdk = 36
        versionCode = 26
        versionName = "4.9.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true

        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }

        val lastFmKey = localProperties.getProperty("LASTFM_API_KEY") ?: System.getenv("LASTFM_API_KEY") ?: "dummy_api_key"
        val lastFmSecret = localProperties.getProperty("LASTFM_SECRET") ?: System.getenv("LASTFM_SECRET") ?: "dummy_api_secret"
        fun appProperty(name: String, fallback: String): String =
            localProperties.getProperty(name)
                ?: providers.gradleProperty(name).orNull
                ?: System.getenv(name)
                ?: fallback

        val appwriteEndpoint = appProperty("APPWRITE_ENDPOINT", "https://fra.cloud.appwrite.io/v1")
        val appwriteProjectId = appProperty("APPWRITE_PROJECT_ID", "69f0c83d001d9fc244d4")
        val appwriteDatabaseId = appProperty("APPWRITE_DATABASE_ID", "echofy")
        val appwriteSelfSigned = appProperty("APPWRITE_SELF_SIGNED", "false").toBooleanStrictOrNull() ?: false

        buildConfigField("String", "LASTFM_API_KEY", "\"$lastFmKey\"")
        buildConfigField("String", "LASTFM_SECRET", "\"$lastFmSecret\"")
        buildConfigField("String", "APPWRITE_ENDPOINT", appwriteEndpoint.toBuildConfigString())
        buildConfigField("String", "APPWRITE_PROJECT_ID", appwriteProjectId.toBuildConfigString())
        buildConfigField("String", "APPWRITE_DATABASE_ID", appwriteDatabaseId.toBuildConfigString())
        buildConfigField("boolean", "APPWRITE_SELF_SIGNED", appwriteSelfSigned.toString())
    }

    signingConfigs {
        getByName("debug") {
            if (System.getenv("MUSIC_DEBUG_SIGNING_STORE_PASSWORD") != null) {
                storeFile = file(System.getenv("MUSIC_DEBUG_KEYSTORE_FILE"))
                storePassword = System.getenv("MUSIC_DEBUG_SIGNING_STORE_PASSWORD")
                keyAlias = "debug"
                keyPassword = System.getenv("MUSIC_DEBUG_SIGNING_KEY_PASSWORD")
            }
        }
        create("release") {
            // Use keystore.properties for release signing
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val props = Properties()
                props.load(keystorePropertiesFile.inputStream())
                storeFile = file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true  // Android 9+ APK Signature Scheme v3
                enableV4Signing = true  // Android 11+ incremental installs
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = false
            isCrunchPngs = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Use release signing if available, otherwise use debug
            signingConfig = if (rootProject.file("keystore.properties").exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = ""
            versionNameSuffix = ""
        }
    }

    // Split APKs by ABI for smaller size
    splits {
        abi {
            isEnable = true
            reset()
            // Only include common architectures
            include("armeabi-v7a", "arm64-v8a", "x86_64")
            isUniversalApk = true  // Enable universal APK for sharing
        }
    }
    
    // Custom APK naming: Echofy_version_abi.apk
    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val abi = output.getFilter(com.android.build.api.variant.FilterConfiguration.FilterType.ABI.name) ?: "universal"
            val versionName = variant.versionName
            output.outputFileName = "Echofy_${versionName}_${abi}.apk"
        }
    }

    // Aggressive packaging options for smaller size
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/*.kotlin_module"
            excludes += "/META-INF/*.version"
            excludes += "/META-INF/proguard/*"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/io.netty.versions.properties"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            excludes += "META-INF/versions/**"
            excludes += "DebugProbesKt.bin"
            excludes += "kotlin/**"
            excludes += "**/*.proto"
            excludes += "**/*.properties"
            pickFirsts += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }

    // Disable lint for faster builds
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    // âœ…  TODO a Java 21
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }

    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
        jvmTarget = "21"
    }

    // Compose compiler optimizations for smoother performance
    composeCompiler {
        featureFlags.addAll(
            org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag.StrongSkipping,
            org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag.IntrinsicRemember,
            org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag.OptimizeNonSkippingGroups
        )
        // Additional Compose performance options
        stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("compose_compiler_config.conf"))
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }

    lint {
        disable += "MissingTranslation"
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}



ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Force consistent OkHttp version to fix Appwrite Realtime WebSocket crash
// Also force protobuf-javalite to fix Firebase In-App Messaging conflict
configurations.all {
    resolutionStrategy {
        force("com.squareup.okhttp3:okhttp:4.12.0")
        force("com.squareup.okhttp3:okhttp-bom:4.12.0")
        force("com.google.protobuf:protobuf-javalite:3.21.7")
    }
}

// Exclude protobuf-java to prevent conflict with protobuf-javalite
configurations.configureEach {
    exclude(group = "com.google.protobuf", module = "protobuf-java")
}

dependencies {
    implementation(libs.guava)
    implementation(libs.coroutines.guava)
    implementation(libs.concurrent.futures)
    implementation("androidx.multidex:multidex:2.0.1")

    implementation(libs.activity)
    implementation(libs.navigation)
    implementation(libs.hilt.navigation)
    implementation(libs.datastore)
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.animation)
    implementation(libs.compose.reorderable)
    implementation(libs.compose.cloudy)
    implementation(libs.compose.markdown)
    implementation(libs.multiplatform.markdown)

    implementation(libs.viewmodel)
    implementation(libs.viewmodel.compose)

    implementation(libs.material3)
    implementation(libs.palette)
    implementation(projects.materialColorUtilities)
    implementation("com.github.Kyant0:m3color:2025.4")

    implementation(libs.coil)
    implementation(libs.shimmer)

    implementation(libs.media3)
    implementation(libs.media3.session)
    implementation(libs.media3.okhttp)
    implementation(libs.media3.ui)
    implementation(libs.squigglyslider)
    implementation(libs.image.cropper)

    implementation(libs.room.runtime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.blurry)
    implementation(libs.material.ripple)

    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.graphics.shapes)
    implementation(libs.work.runtime.ktx)
    implementation(libs.profileinstaller)
    implementation(libs.constraintlayout)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx)

    implementation(libs.apache.lang3)

    implementation(libs.hilt)
    implementation("org.jsoup:jsoup:1.18.1")
    kapt(libs.hilt.compiler)

    implementation(projects.innertube)
    implementation(projects.ytmusicapi)
    implementation(projects.kugou)
    implementation(projects.lrclib)
    implementation(projects.kizzy)
    implementation(projects.musicbrainz)
    implementation(projects.radiobrowser)
    implementation(projects.songlink)
    implementation(projects.bandsintown)
    implementation(projects.genius)
    implementation(projects.freesound)
    implementation(projects.tastedive)
    implementation(projects.discogs)
    implementation(projects.theaudiodb)
    implementation(projects.mixcloud)
    implementation(project(":jossredconnect"))
    implementation(project(":canvas"))
    implementation(project(":betterlyrics"))
    implementation(project(":shazamkit"))
    implementation(project(":simpmusic"))
    implementation(project(":spotify"))
    implementation(project(":lastfm"))

    implementation(libs.ktor.client.core)
    coreLibraryDesugaring(libs.desugaring)
    implementation(libs.timber)
    
    // Appwrite SDK for Listen Together feature
    implementation("io.appwrite:sdk-for-android:5.1.0")
    
    // Firebase — push notifications and analytics
    // firebase-auth and firebase-database removed; Together now uses Appwrite
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("androidx.media:media:1.7.0")

    // Google AdMob SDK for ads monetization
    implementation("com.google.android.gms:play-services-ads:23.0.0")

    // Google Play In-App Updates
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")

    // Lottie for Seasonal Live Wallpapers
    implementation("com.airbnb.android:lottie-compose:6.3.0")
    
    // Google Sign-In / One Tap
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("androidx.credentials:credentials:1.2.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")

    // ONNX Runtime for openWakeWord ("Hey Jarvis") engine
    // 1.18.0 shipped .so files with 4 KB ELF LOAD alignment, which Play now rejects because
    // apps must support 16 KB memory page sizes. 1.23.0 is the first release built with
    // -Wl,-z,max-page-size=16384.
}

configurations.all {
    resolutionStrategy {
        force("androidx.core:core:1.15.0")
        force("androidx.core:core-ktx:1.15.0")
        force("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.0")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.1.0")
        force("org.jetbrains.kotlin:kotlin-stdlib-common:2.1.0")
    }
}
