## Overview

A program for **reviewing chess opening lines**.

This tool is designed for **repetition, not discovery**.  
Opening lines must be prepared in advance by the user — either found manually or created with the help of an engine — and then added to the program.

The current goal is to keep the product **minimal and focused**, with possible extra features added later.

## First build

Configure a JDK and the Android SDK before building. The application uses the published `chesscore:0.1.0` JAR and `chessboardui:0.1.0` AAR from the local Maven repository at `build/maven-repository/`. The independent `chessboardui` build also uses the published `chesscore:0.1.0` JAR.

**Configure the SDK location for both Android builds before running commands or tests from the IDE.** When using `local.properties`, the files belong at these locations:

| Build | SDK configuration file relative to the repository root |
|---|---|
| Application (`app`) | `local.properties` |
| Board library (`chessboardui`) | `chessboardui/local.properties` |

Set `sdk.dir` to the absolute path of the installed Android SDK in each file, for example (replace the example path):

```properties
sdk.dir=/absolute/path/to/Android/Sdk
```

Both files may point to the same SDK installation. The root `local.properties` does not configure the independent `chessboardui` build. Alternatively, set `ANDROID_HOME` in the environment used to launch both Android Studio and terminal builds. These machine-specific files are excluded from Git and must be configured on each computer. The Kotlin/JVM `chesscore` build does not require an Android SDK.

**After cloning the repository, publish `chesscore` first, then `chessboardui`, before syncing or building the application.** The generated Maven repository is excluded from Git. Run these commands from the repository root:

```shell
./gradlew -p chesscore build
./gradlew -p chesscore publishMavenJavaPublicationToLocalRepository
./gradlew -p chessboardui testReleaseUnitTest assembleRelease
./gradlew -p chessboardui publishReleasePublicationToLocalRepository
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Repeat local publication if `build/maven-repository/` is removed or a required library version is missing. Both libraries have independent builds and are built and published in dependency order using `-p`.

Changes to library source directories reach consumers through publication of a new version and an update to the dependency. Application build and test commands do not run the libraries' own tests; target each library explicitly. For artifact contents, publication details, and library usage, see [chesscore/Readme.md](chesscore/Readme.md) and [chessboardui/Readme.md](chessboardui/Readme.md).

## Working in Android Studio

Open the repository root and sync Gradle after the initial publications above. The root `settings.gradle.kts` includes `chesscore` and `chessboardui` using `includeBuild`, so their sources, tests, and Gradle tasks can be imported alongside the application in one IDE window. Each library retains its independent settings, repositories, and publication configuration. Use the Project view to inspect all source directories and the Gradle tool window to select tasks for each build.

Both `app/build.gradle.kts` and `chessboardui/build.gradle.kts` disable automatic composite dependency substitution for all their configurations:

```kotlin
configurations.configureEach {
    resolutionStrategy.useGlobalDependencySubstitutionRules.set(false)
}
```

The application resolves the published `chesscore` JAR and `chessboardui` AAR; `chessboardui` resolves the published `chesscore` JAR. This applies to compilation, runtime, unit-test, and instrumented-test dependencies, including transitive dependencies. Editing library sources affects that library's own tests immediately; consumers continue using the selected published versions until a new version is published and their dependency is updated. Missing artifacts must be published before sync; inclusion of their sources does not provide a fallback.

If a `chessboardui` unit-test launch from the IDE fails with `SDK location not found`, check `chessboardui/local.properties` as described under [First build](#first-build). A terminal build may succeed because its environment supplies `ANDROID_HOME`, while the IDE process has a different environment. After configuring the library's SDK path, sync Gradle and repeat the test launch. Successful dependency reports or `--dry-run` output alone do not confirm SDK availability for test compilation. Machine-specific `.idea` settings and SDK paths remain outside Git.

Run tasks for a specific product from the repository root:

```shell
./gradlew :chesscore:test
./gradlew :chessboardui:testDebugUnitTest :chessboardui:testReleaseUnitTest
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

The independent `./gradlew -p chesscore ...` and `./gradlew -p chessboardui ...` commands remain available. Use explicit task paths when selecting a product in the composite build.

### Verify artifact resolution and IDE import

After sync, confirm that all three products are available in the IDE and run one test from each product's own test source directory. For instrumented tests, select a device or emulator; use API 26 or above for the board library's visual tests.

Inspect the consumer dependency reports from the repository root:

```shell
./gradlew :app:dependencyInsight --configuration debugRuntimeClasspath --dependency com.example.chessboard:chesscore
./gradlew :app:dependencyInsight --configuration debugRuntimeClasspath --dependency com.example.chessboard:chessboardui
./gradlew :chessboardui:dependencyInsight --configuration releaseRuntimeClasspath --dependency com.example.chessboard:chesscore
```

The selected components must be the versioned Maven modules (`0.1.0` currently), with no substitution to `project :chesscore` or `project :chessboardui`. Repeat the relevant reports for debug/release compile and runtime classpaths and the unit-test and instrumented-test classpaths to verify all consumer contexts. A missing dependency or failed resolution is a failed check, even if the command completes.

Before an application-only build, inspect its task graph:

```shell
./gradlew :app:assembleDebug --dry-run
```

It must not contain compilation, test, or publication tasks from the included library builds. Successful application compilation alone does not verify that published artifacts were used.

## Goals

- **Fully offline operation**
    - The program must work without an internet connection.
    - Internet access is still not always available, and the app should remain fully usable offline.

- **Opening line repetition**
    - The same line played from **White's side** and **Black's side** is treated as **two different lines**.
    - Even if the resulting FEN positions are identical, the training purpose is different:
        - when studying as White, Black may play bad moves that White must punish
        - when studying as Black, Black should only play correct moves
    - Because of this, matching by FEN alone is not enough. An additional side/training context flag is required in the database.

- **Edit a line during training**
    - Rename a line with a human-readable title
    - Change line weight / priority
    - Delete a line
    - Shorten or extend a variation
    - Branch off from any move to create a new line

- **Find all lines containing a selected position**
    - Move number should be ignored
    - Side to move must be respected
    - This helps when many variations already exist and some of them need to be deactivated or removed after a repertoire change

- **Build a training plan based on line weight / priority**
    - Example:
        - Line 1 → priority 1
        - Line 2 → priority 3
        - Line 3 → priority 2
        - Line 4 → inactive
    - Training plan:
        - Line 1 repeated 1 time
        - Line 2 repeated 3 times
        - Line 3 repeated 2 times
    - After all required repetitions are completed, the plan is considered finished

- **Training plan templates**
    - Create reusable templates
    - For example:
        - for a specific opening
        - from a specific position
        - for a specific training goal

- **Statistics collection**
    - Track training performance for each line
    - The main metric for now is **mistake frequency during repetition**
    - In the future, statistics may be used to generate training plans automatically

- **Position metadata**
    - Add custom metadata to any position
    - For example:
        - comments
        - evaluation
        - notes
        - any other useful information
