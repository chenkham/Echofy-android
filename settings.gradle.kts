@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.10.0")
}

rootProject.name = "Echofy"
include(":app")
include(":innertube")
include(":ytmusicapi")
include(":kugou")
include(":lrclib")
include(":kizzy")
include(":material-color-utilities")
include(":jossredconnect")
include(":musicbrainz")
include(":radiobrowser")
include(":songlink")
include(":bandsintown")
include(":genius")
include(":freesound")
include(":tastedive")
include(":discogs")
include(":theaudiodb")
include(":mixcloud")
include(":canvas")
include(":betterlyrics")
include(":shazamkit")
include(":simpmusic")
include(":spotify")
include(":lastfm")
include(":ui")