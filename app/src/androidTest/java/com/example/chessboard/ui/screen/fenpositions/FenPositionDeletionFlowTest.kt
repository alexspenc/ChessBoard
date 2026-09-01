package com.example.chessboard.ui.screen.fenpositions

/*
 * File role: verifies the shared FEN position deletion dialog flow against Room.
 * Allowed here:
 * - confirmation, asynchronous delete result, failure dialog, and callback assertions
 * Not allowed here:
 * - catalog/details layout, app routing, or unrelated persistence behavior
 * Validation date: 2026-09-01
 */

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.chessboard.repository.AppDatabase
import com.example.chessboard.service.FenPositionService
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FenPositionDeletionFlowTest {
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
    fun missingPositionShowsFailureAndDoesNotReportDeletion() {
        var deletedCalls = 0
        composeRule.setContent {
            ChessBoardTheme {
                FenPositionDeletionFlow(
                    positionId = MissingPositionId,
                    fenPositionService = service,
                    strings = deletionStrings(),
                    onDismiss = ::recordIgnoredDismiss,
                    onDeleted = {
                        deletedCalls += 1
                    },
                )
            }
        }

        composeRule.onNodeWithText("Delete").performClick()

        // Room deletion and the following Compose recomposition are asynchronous. Keep a
        // deliberate margin here so this assertion is not converted into a timing race.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("Deletion failed").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithText("Could not delete the selected position").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(0, deletedCalls)
        }
    }

    private fun deletionStrings(): FenPositionDeletionStrings {
        return FenPositionDeletionStrings(
            title = "Delete position?",
            message = "Delete selected position",
            confirm = "Delete",
            deletingTitle = "Deleting position",
            deletingMessage = "Deleting",
            failedTitle = "Deletion failed",
            failedMessage = "Could not delete the selected position",
        )
    }

    private fun recordIgnoredDismiss() = Unit

    private companion object {
        const val MissingPositionId = -1L
    }
}
