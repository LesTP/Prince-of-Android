plugins {
    id("com.android.application") version "8.2.2"
    id("org.jetbrains.kotlin.android") version "1.9.22"
}

android {
    namespace = "com.sdlpop.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sdlpop.android"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.16.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    // Game logic from the existing Kotlin translation
    implementation(project(":SDLPoP-kotlin"))

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
}
