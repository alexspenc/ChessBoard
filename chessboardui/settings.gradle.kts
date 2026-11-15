/*
 * File role: configures the independent Android board library and its published chesscore dependency.
 * Keep application modules and application-specific repositories outside this build.
 * Validation date: 2026-11-15.
 */

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        exclusiveContent {
            forRepository {
                maven {
                    name = "localChesscore"
                    url = uri("../build/maven-repository")
                }
            }
            filter {
                includeModule("com.example.chessboard", "chesscore")
            }
        }
    }
}

rootProject.name = "chessboardui"
