plugins {
    id("java-library")
    alias(libs.plugins.jetbrainsKotlinJvm)
    id("application")
    alias(libs.plugins.serialization)
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}
dependencies {
    implementation(libs.ktor.server.core.jvm)
    implementation(libs.ktor.ktor.server.netty.jvm)
    implementation(libs.ktor.server.content.negotiation.jvm)
    implementation(libs.ktor.server.cors.jvm)
    implementation(libs.ktor.server.caching.headers.jvm)
    implementation(libs.ktor.serialization.kotlinx.json.jvm)
    
    // Database
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.h2)
}
application {
    mainClass.set("com.aleskrot.server.ServerKt")
}
