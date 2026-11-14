/*
 * File role: configures the independent Android board library and its local chesscore dependency.
 * Keep application modules and application-specific repositories outside this build.
 * Validation date: 2026-11-14.
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
    }
}

rootProject.name = "chessboardui"
includeBuild("../chesscore")
