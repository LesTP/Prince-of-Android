pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "prince-of-android"

include("SDLPoP-kotlin")

// Android app module — only included when Android SDK is available.
// Android Studio auto-creates local.properties with sdk.dir on first sync.
// On the Pi (no local.properties), only SDLPoP-kotlin is configured,
// so existing tests work without needing AGP or the Android SDK.
if (file("local.properties").exists()) {
    include("app")
}
