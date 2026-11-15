/*
 * File role: configures the application build, its repositories, and the included board UI build.
 * Keep library implementation and independent library build settings in their own directories.
 * Validation date: 2026-11-15.
 */

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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
                    url = uri("build/maven-repository")
                }
            }
            filter {
                includeModule("com.example.chessboard", "chesscore")
            }
        }
    }
}

rootProject.name = "ChessBoard"
include(":app")
includeBuild("chessboardui")
