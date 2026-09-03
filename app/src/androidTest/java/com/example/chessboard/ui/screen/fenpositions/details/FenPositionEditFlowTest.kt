package com.example.chessboard.ui.screen.fenpositions.details

/*
 * File role: verifies failure handling in the Room-backed FEN position edit flow.
 * Allowed here:
 * - missing-position save results, failure dialog, and completion callback assertions
 * Not allowed here:
 * - details-screen layout, successful persistence coverage, or app navigation
 * Validation date: 2026-09-02
 */

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.chessboard.repository.AppDatabase
import com.example.chessboard.service.FenPositionService
import com.example.chessboard.ui.testtags.fenpositions.FenPositionEditConfirmTestTag
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FenPositionEditFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var database: AppDatabase
    private lateinit var service: FenPositionService

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        service = FenPositionService(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun missingPositionShowsFailureAndReportsMissingAfterDismiss() {
        var updatedCalls = 0
        var missingCalls = 0
        composeRule.setContent {
            ChessBoardTheme {
                FenPositionEditFlow(
                    position = missingPosition(),
                    fenPositionService = service,
                    strings = dialogStrings(),
                    onDismiss = ::recordIgnoredDismiss,
                    onUpdated = {
                        updatedCalls += 1
                    },
                    onPositionMissing = {
                        missingCalls += 1
                    },
                )
            }
        }

        composeRule.onNodeWithTag(FenPositionEditConfirmTestTag).performClick()

        // Room saving and the following Compose recomposition are asynchronous. Keep a deliberate
        // timeout margin so the error assertion remains independent from emulator speed.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("Save failed").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithText("Position no longer exists").assertIsDisplayed()
        composeRule.onNodeWithText("OK").performClick()
        composeRule.runOnIdle {
            assertEquals(0, updatedCalls)
            assertEquals(1, missingCalls)
        }
    }

    private fun missingPosition(): FenPositionDetailsItem {
        return FenPositionDetailsItem(
            id = MissingPositionId,
            fen = InitialPositionFen,
            name = "Missing position",
            theme = "Strategy",
            description = "Description",
            continuationUciLines = emptyList(),
            catalogIndex = 0,
            previousPositionId = null,
            nextPositionId = null,
        )
    }

    private fun dialogStrings(): FenPositionEditDialogStrings {
        return FenPositionEditDialogStrings(
            title = "Edit position",
            nameLabel = "Name",
            namePlaceholder = "Position name",
            themeLabel = "Theme *",
            themePlaceholder = "Strategy",
            descriptionLabel = "Description",
            descriptionPlaceholder = "Notes",
            themeRequired = "Theme is required",
            savingTitle = "Saving position",
            savingMessage = "Saving position changes…",
            saveFailedTitle = "Save failed",
            saveFailed = "Could not save position changes",
            positionNotFound = "Position no longer exists",
            cancel = "Cancel",
            save = "Save",
        )
    }

    private fun recordIgnoredDismiss() = Unit

    private companion object {
        const val MissingPositionId = -1L
        const val InitialPositionFen = "4k3/8/8/8/8/8/8/4K3 w - -"
    }
}
