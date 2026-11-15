/*
 * File role: configures the application build, published-library repositories, and included builds for IDE work.
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
                    name = "localChessLibraries"
                    url = uri("build/maven-repository")
                }
            }
            filter {
                includeModule("com.example.chessboard", "chesscore")
                includeModule("com.example.chessboard", "chessboardui")
            }
        }
    }
}

rootProject.name = "ChessBoard"
include(":app")
// Load library sources and tests together; consumers resolve their published artifacts.
includeBuild("chesscore")
includeBuild("chessboardui")
