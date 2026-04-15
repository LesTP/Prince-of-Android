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
    systemProperty("sdlpop.referenceTraceRoot", rootProject.file("../SDLPoP/traces/reference").absolutePath)
    systemProperty("sdlpop.kotlinTraceOutput", layout.buildDirectory.dir("oracle/layer1-regression/test").get().asFile.absolutePath)
}

tasks.register<Test>("layer1ReplayRegression") {
    description = "Runs the Layer 1 replay-regression oracle workflow against the reference trace manifest."
    group = "verification"
    useJUnitPlatform {
        includeTags("layer1-regression")
    }
    systemProperty("sdlpop.referenceTraceRoot", rootProject.file("../SDLPoP/traces/reference").absolutePath)
    systemProperty("sdlpop.kotlinTraceOutput", layout.buildDirectory.dir("oracle/layer1-regression/workflow").get().asFile.absolutePath)
    shouldRunAfter(tasks.test)
}
