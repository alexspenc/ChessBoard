/*
 * File role: builds chesscore, runs its unit tests, and publishes versioned JVM artifacts locally.
 * Keep chesslib adapters, Android plugins, and application dependencies outside this build.
 * Validation date: 2026-11-15.
 */

plugins {
    kotlin("jvm") version "2.2.10"
    `java-library`
    `maven-publish`
}

group = "com.example.chessboard"
version = "0.1.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set("chesscore")
                description.set("Chess models, position contracts, and PGN, SAN, UCI, and FEN text parsers.")
            }
        }
    }

    repositories {
        maven {
            name = "local"
            url = uri(layout.projectDirectory.dir("../build/maven-repository").asFile)
        }
    }
}
