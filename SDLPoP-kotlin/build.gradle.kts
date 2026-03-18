plugins {
    kotlin("jvm") version "1.9.22"
    application
}

group = "com.sdlpop"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(17)
}

application {
    // Will be set when replay runner is implemented
    mainClass.set("com.sdlpop.MainKt")
}

tasks.test {
    useJUnitPlatform()
}
