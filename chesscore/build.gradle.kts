/*
 * File role: builds chesscore as a Kotlin/JVM library and runs its independent unit tests.
 * Keep chesslib adapters, Android plugins, and application dependencies outside this build.
 * Validation date: 2026-11-14.
 */

plugins {
    kotlin("jvm") version "2.2.10"
    `java-library`
}

group = "com.example.chessboard"
version = "0.1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
