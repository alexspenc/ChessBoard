/*
 * File role: configures the independent chesscore build and its dependency repositories.
 * Keep Android and application-specific configuration outside this build.
 * Validation date: 2026-11-14.
 */

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "chesscore"
