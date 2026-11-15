/*
 * File role: builds the board renderer, runs its tests, and publishes release Android artifacts locally.
 * Keep application themes, chesslib adapters, persistence, and screen dependencies outside this build.
 * Validation date: 2026-11-15.
 */

plugins {
    id("com.android.library") version "9.4.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
    `maven-publish`
}

group = "com.example.chessboard"
version = "0.1.0"

android {
    namespace = "com.example.chessboard.chessboardui"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // These types are exposed by the public scene, geometry, and drawing contracts.
    api("com.example.chessboard:chesscore:0.1.0")
    api(platform("androidx.compose:compose-bom:2026.03.01"))
    api("androidx.compose.ui:ui")
    api("androidx.compose.ui:ui-graphics")

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.activity:activity-compose:1.12.4")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.03.01"))
    androidTestImplementation("androidx.compose.foundation:foundation")
    androidTestImplementation("androidx.compose.runtime:runtime:1.11.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

publishing {
    publications {
        create<MavenPublication>("release") {
            // AGP creates the selected Android component after project evaluation.
            afterEvaluate {
                from(components["release"])
            }
            pom {
                name.set("chessboardui")
                description.set("Android Compose chessboard rendering and animation geometry using chesscore models.")
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
