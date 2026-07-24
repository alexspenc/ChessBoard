package com.example.chessboard.ui.screen

/*
 * File role: verifies backup screen storage readiness, action availability, restore progress, and localization startup.
 * Allowed here:
 * - deterministic Compose tests for backup and restore UI behavior
 * - smoke tests for backup launchers inside localized composition
 * Not allowed here:
 * - broad app navigation coverage or real document-provider integration
 * Validation date: 2026-07-24
 */
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.chessboard.localization.AppLanguage
import com.example.chessboard.localization.ProvideAppLanguage
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.service.AppDocumentStorage
import com.example.chessboard.service.AppDocumentStructure
import com.example.chessboard.service.LineBackupRestoreProgress
import com.example.chessboard.service.LineBackupRestoreResult
import com.example.chessboard.ui.BackupContentTestTag
import com.example.chessboard.ui.BackupFullCreateTestTag
import com.example.chessboard.ui.BackupFullRestoreTestTag
import com.example.chessboard.ui.BackupFullStrictFileSelectionTestTag
import com.example.chessboard.ui.BackupLineCreateTestTag
import com.example.chessboard.ui.BackupLineRestoreTestTag
import com.example.chessboard.ui.BackupRestoreCancelTestTag
import com.example.chessboard.ui.BackupRestoreProgressDialogTestTag
import com.example.chessboard.ui.BackupStorageSelectTestTag
import com.example.chessboard.ui.BackupStorageStatusTestTag
import com.example.chessboard.ui.theme.ChessBoardTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.junit.Rule
import org.junit.Test

class BackupScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun backupScreen_canOpenInsideLocalizedComposition() {
        composeRule.setContent {
            ChessBoardTheme {
                ProvideAppLanguage(AppLanguage.RUSSIAN) {
                    BackupScreenContainer(
                        activity = composeRule.activity,
                        appDocumentStorage = FakeAppDocumentStorage(createReadyStorageState()),
                        screenContext =
                            ScreenContainerContext(
                                inDbProvider = DatabaseProvider.createInstance(composeRule.activity),
                            ),
                    )
                }
            }
        }

        composeRule.onNodeWithTag(BackupContentTestTag).assertIsDisplayed()
        composeRule.onNodeWithText("Full Database Backup").assertIsDisplayed()
        composeRule.onNodeWithTag(BackupFullCreateTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(BackupFullRestoreTestTag).assertIsDisplayed()
    }

    @Test
    fun backupScreen_fullBackupStrictFileSelectionEnabledByDefaultAndCanBeDisabled() {
        composeRule.setContent {
            ChessBoardTheme {
                BackupScreenContainer(
                    activity = composeRule.activity,
                    appDocumentStorage = FakeAppDocumentStorage(createReadyStorageState()),
                    screenContext =
                        ScreenContainerContext(
                            inDbProvider = DatabaseProvider.createInstance(composeRule.activity),
                        ),
                )
            }
        }

        composeRule
            .onNodeWithTag(BackupFullStrictFileSelectionTestTag)
            .assertIsOn()
            .performClick()
            .assertIsOff()
    }

    @Test
    fun backupScreen_restoreShowsProgressAndCanBeCanceledAtFifthLine() {
        val fakeRestoreUri = Uri.parse("content://test/backup.pgn")
        val waitForCancelAtFifthLine = CompletableDeferred<Unit>()

        composeRule.setContent {
            ChessBoardTheme {
                BackupScreenContainer(
                    activity = composeRule.activity,
                    appDocumentStorage = FakeAppDocumentStorage(createReadyStorageState()),
                    screenContext =
                        ScreenContainerContext(
                            inDbProvider = DatabaseProvider.createInstance(composeRule.activity),
                        ),
                    testRestoreUri = fakeRestoreUri,
                    restoreBackupRunner = { _, onProgress ->
                        val totalLines = 15
                        var processedLinesCount = 0
                        var restoredLinesCount = 0
                        var skippedLinesCount = 0

                        onProgress(
                            LineBackupRestoreProgress(
                                totalLines = totalLines,
                                processedLinesCount = processedLinesCount,
                                restoredLinesCount = restoredLinesCount,
                                skippedLinesCount = skippedLinesCount,
                            ),
                        )

                        repeat(totalLines) {
                            currentCoroutineContext().ensureActive()
                            processedLinesCount += 1
                            restoredLinesCount += 1
                            onProgress(
                                LineBackupRestoreProgress(
                                    totalLines = totalLines,
                                    processedLinesCount = processedLinesCount,
                                    restoredLinesCount = restoredLinesCount,
                                    skippedLinesCount = skippedLinesCount,
                                ),
                            )

                            if (processedLinesCount == 5) {
                                waitForCancelAtFifthLine.await()
                            }
                        }

                        LineBackupRestoreResult(
                            restoredLinesCount = restoredLinesCount,
                            skippedLinesCount = skippedLinesCount,
                        )
                    },
                )
            }
        }

        composeRule.onNodeWithText("Restore Lines").performClick()
        composeRule.onNodeWithText("Restore").performClick()

        waitForNodeWithTag(BackupRestoreProgressDialogTestTag)
        waitForText("Processed lines: 5/15")
        composeRule.onNodeWithTag(BackupRestoreCancelTestTag).performClick()

        waitForText("Restore canceled.")
        waitForText("Processed lines: 5/15")
        waitForNodeWithTagToDisappear(BackupRestoreProgressDialogTestTag)
    }

    @Test
    fun backupScreen_notConfiguredDisablesCreationAndKeepsRestoreEnabled() {
        setBackupScreenContent(
            AppDocumentStorage.State.NotConfigured,
        )

        composeRule.onNodeWithTag(BackupStorageStatusTestTag).assertIsDisplayed()
        composeRule.onNodeWithText("Choose a folder before creating backups.").assertIsDisplayed()
        composeRule.onNodeWithTag(BackupStorageSelectTestTag).assertIsEnabled()
        composeRule.onNodeWithTag(BackupLineCreateTestTag).assertIsNotEnabled()
        composeRule.onNodeWithTag(BackupFullCreateTestTag).assertIsNotEnabled()
        composeRule.onNodeWithTag(BackupLineRestoreTestTag).assertIsEnabled()
        composeRule.onNodeWithTag(BackupFullRestoreTestTag).assertIsEnabled()
    }

    @Test
    fun backupScreen_permissionLostDisablesCreationAndKeepsRestoreEnabled() {
        setBackupScreenContent(
            AppDocumentStorage.State.PermissionLost(RootUri),
        )

        composeRule.onNodeWithText("Access to the configured folder was lost. Choose it again.").assertIsDisplayed()
        composeRule.onNodeWithTag(BackupLineCreateTestTag).assertIsNotEnabled()
        composeRule.onNodeWithTag(BackupFullCreateTestTag).assertIsNotEnabled()
        composeRule.onNodeWithTag(BackupLineRestoreTestTag).assertIsEnabled()
        composeRule.onNodeWithTag(BackupFullRestoreTestTag).assertIsEnabled()
    }

    @Test
    fun backupScreen_readyStorageEnablesCreateAndRestoreActions() {
        setBackupScreenContent(createReadyStorageState())

        composeRule.onNodeWithText("Backup folders are ready.").assertIsDisplayed()
        composeRule.onNodeWithTag(BackupLineCreateTestTag).assertIsEnabled()
        composeRule.onNodeWithTag(BackupFullCreateTestTag).assertIsEnabled()
        composeRule.onNodeWithTag(BackupLineRestoreTestTag).assertIsEnabled()
        composeRule.onNodeWithTag(BackupFullRestoreTestTag).assertIsEnabled()
    }

    private fun setBackupScreenContent(storageState: AppDocumentStorage.State) {
        composeRule.setContent {
            ChessBoardTheme {
                BackupScreenContainer(
                    activity = composeRule.activity,
                    appDocumentStorage = FakeAppDocumentStorage(storageState),
                    screenContext =
                        ScreenContainerContext(
                            inDbProvider = DatabaseProvider.createInstance(composeRule.activity),
                        ),
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun createReadyStorageState(): AppDocumentStorage.State.Ready {
        return AppDocumentStorage.State.Ready(
            AppDocumentStructure(
                rootUri = RootUri,
                lineBackupsUri = Uri.parse("$RootUri/line-backups"),
                databaseBackupsUri = Uri.parse("$RootUri/database-backups"),
                gameAnalysisUri = Uri.parse("$RootUri/game-analysis"),
            ),
        )
    }

    private fun waitForNodeWithTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithTag(tag).fetchSemanticsNode()
                true
            }.getOrDefault(false)
        }
    }

    private fun waitForNodeWithTagToDisappear(tag: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithTag(tag).fetchSemanticsNode()
                false
            }.getOrDefault(true)
        }
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithText(text, substring = true).fetchSemanticsNode()
                true
            }.getOrDefault(false)
        }
    }

    private class FakeAppDocumentStorage(
        private var state: AppDocumentStorage.State,
    ) : AppDocumentStorage {
        override suspend fun loadState(): AppDocumentStorage.State {
            return state
        }

        override suspend fun configureRoot(rootUri: Uri): AppDocumentStorage.State.Ready {
            val readyState =
                AppDocumentStorage.State.Ready(
                    AppDocumentStructure(
                        rootUri = rootUri,
                        lineBackupsUri = Uri.parse("$rootUri/line-backups"),
                        databaseBackupsUri = Uri.parse("$rootUri/database-backups"),
                        gameAnalysisUri = Uri.parse("$rootUri/game-analysis"),
                    ),
                )
            state = readyState
            return readyState
        }

        override suspend fun disconnectRoot() {
            state = AppDocumentStorage.State.NotConfigured
        }
    }

    private companion object {
        val RootUri: Uri = Uri.parse("content://test/tree/chessboard")
    }
}
