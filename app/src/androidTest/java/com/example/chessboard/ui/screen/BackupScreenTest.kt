package com.example.chessboard.ui.screen

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.service.GameBackupRestoreProgress
import com.example.chessboard.service.GameBackupRestoreResult
import com.example.chessboard.ui.BackupRestoreCancelTestTag
import com.example.chessboard.ui.BackupRestoreProgressDialogTestTag
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
    fun backupScreen_restoreShowsProgressAndCanBeCanceledAtFifthGame() {
        val fakeRestoreUri = Uri.parse("content://test/backup.pgn")
        val waitForCancelAtFifthGame = CompletableDeferred<Unit>()

        composeRule.setContent {
            ChessBoardTheme {
                BackupScreenContainer(
                    activity = composeRule.activity,
                    screenContext = ScreenContainerContext(
                        inDbProvider = DatabaseProvider.createInstance(composeRule.activity)
                    ),
                    testRestoreUri = fakeRestoreUri,
                    restoreBackupRunner = { _, onProgress ->
                        val totalGames = 15
                        var processedGamesCount = 0
                        var restoredGamesCount = 0
                        var skippedGamesCount = 0

                        onProgress(
                            GameBackupRestoreProgress(
                                totalGames = totalGames,
                                processedGamesCount = processedGamesCount,
                                restoredGamesCount = restoredGamesCount,
                                skippedGamesCount = skippedGamesCount,
                            )
                        )

                        repeat(totalGames) {
                            currentCoroutineContext().ensureActive()
                            processedGamesCount += 1
                            restoredGamesCount += 1
                            onProgress(
                                GameBackupRestoreProgress(
                                    totalGames = totalGames,
                                    processedGamesCount = processedGamesCount,
                                    restoredGamesCount = restoredGamesCount,
                                    skippedGamesCount = skippedGamesCount,
                                )
                            )

                            if (processedGamesCount == 5) {
                                waitForCancelAtFifthGame.await()
                            }
                        }

                        GameBackupRestoreResult(
                            restoredGamesCount = restoredGamesCount,
                            skippedGamesCount = skippedGamesCount
                        )
                    }
                )
            }
        }

        composeRule.onNodeWithText("Restore Games").performClick()
        composeRule.onNodeWithText("Restore").performClick()

        waitForNodeWithTag(BackupRestoreProgressDialogTestTag)
        waitForText("Processed games: 5/15")
        composeRule.onNodeWithTag(BackupRestoreCancelTestTag).performClick()

        waitForText("Restore canceled.")
        waitForText("Processed games: 5/15")
        waitForNodeWithTagToDisappear(BackupRestoreProgressDialogTestTag)
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
}
