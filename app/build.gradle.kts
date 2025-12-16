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

android {
    namespace = "com.Chenkham.Echofy"
    //noinspection GradleDependency
    compileSdk = 35

    defaultConfig {
        applicationId = "com.Chenkham.Echofy"
        minSdk = 24
        targetSdk = 35
        versionCode = 3
        versionName = "2.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
            isShrinkResources = true
            isCrunchPngs = true
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
            excludes += "DebugProbesKt.bin"
            excludes += "kotlin/**"
            excludes += "**/*.proto"
            excludes += "**/*.properties"
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

    // ✅  TODO a Java 21
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

    implementation(libs.activity)
    implementation(libs.navigation)
    implementation(libs.hilt.navigation)
    implementation(libs.datastore)

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.animation)
    implementation(libs.compose.reorderable)

    implementation(libs.viewmodel)
    implementation(libs.viewmodel.compose)

    implementation(libs.material3)
    implementation(libs.palette)
    implementation(projects.materialColorUtilities)

    implementation(libs.coil)
    implementation(libs.shimmer)

    implementation(libs.media3)
    implementation(libs.media3.session)
    implementation(libs.media3.okhttp)
    implementation(libs.squigglyslider)

    implementation(libs.room.runtime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.blurry)
    implementation(libs.material.ripple)
    implementation(libs.room.runtime.android)
    // Removed material-icons-extended - it adds ~20MB. Use drawable resources instead.
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.graphics.shapes)
    implementation(libs.work.runtime.ktx)
    implementation(libs.constraintlayout)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx)

    implementation(libs.apache.lang3)

    implementation(libs.hilt)
    implementation("org.jsoup:jsoup:1.18.1")
    kapt(libs.hilt.compiler)

    implementation(projects.innertube)
    implementation(projects.kugou)
    implementation(projects.lrclib)
    implementation(projects.kizzy)
    implementation(project(":jossredconnect"))

    implementation(libs.ktor.client.core)

    coreLibraryDesugaring(libs.desugaring)

    implementation(libs.timber)
    
    // Appwrite SDK for Listen Together feature (v5.1.0 fixes OkHttp conflicts)
    implementation("io.appwrite:sdk-for-android:5.1.0")
    
    // Firebase for push notifications via Appwrite Messaging
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    // Firebase In-App Messaging for modal cards and banners
    implementation("com.google.firebase:firebase-inappmessaging-display-ktx")
    // Firebase Analytics (REQUIRED for In-App Messaging to work properly)
    implementation("com.google.firebase:firebase-analytics-ktx")
}
