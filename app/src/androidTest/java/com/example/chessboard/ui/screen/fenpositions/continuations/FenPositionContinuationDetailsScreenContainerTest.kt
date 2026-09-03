package com.example.chessboard.ui.screen.fenpositions.continuations

/*
 * File role: verifies continuation deletion and container return routing.
 * Allowed here:
 * - confirmation, asynchronous deletion, failure-safe return, and Room result assertions
 * Not allowed here:
 * - isolated Compose layout assertions or continuation parsing coverage
 * Validation date: 2026-09-03
 */

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.chessboard.MainActivity
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.service.CreateFenPositionResult
import com.example.chessboard.service.CreateFenPositionContinuationBatchResult
import com.example.chessboard.service.FenPositionContinuationBatchPreparation
import com.example.chessboard.ui.HomeRegularContentTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCatalogHomeEntryTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCatalogOpenTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationDetailsDeleteTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationDetailsDeleteConfirmTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsContentTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsContinuationsHeaderTestTag
import com.example.chessboard.ui.testtags.fenpositions.fenPositionCatalogCardTestTag
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FenPositionContinuationDetailsScreenContainerTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val dbProvider: DatabaseProvider
        get() = DatabaseProvider.createInstance(composeRule.activity)

    @Before
    fun setUp() {
        dbProvider.clearAllData()
    }

    @Test
    fun confirmingDeleteRemovesContinuationAndReturnsToPosition() {
        val ids = preparePositionWithContinuation()
        openViewer(ids.positionId)

        composeRule.onNodeWithTag(FenPositionContinuationDetailsDeleteTestTag).performClick()
        composeRule.onNodeWithText("Delete continuation?").assertIsDisplayed()
        composeRule
            .onNodeWithTag(FenPositionContinuationDetailsDeleteConfirmTestTag)
            .performClick()

        waitForTag(FenPositionDetailsContentTestTag)
        assertNull(runBlocking {
            dbProvider.createFenPositionContinuationService().getById(ids.continuationId)
        })
    }

    private fun preparePositionWithContinuation(): TestIds {
        return runBlocking {
            dbProvider.createUserProfileService().updateSettings(
                simpleViewEnabled = false,
                removeLineIfRepIsZero = false,
                hideLinesWithWeightZero = false,
            )
            val position = dbProvider.createFenPositionService().create(
                fen = InitialPositionFen,
                name = "Test position",
                theme = "Strategy",
                description = "Description",
            ) as CreateFenPositionResult.Success
            val result = dbProvider.createFenPositionContinuationService().createBatch(
                positionId = position.id,
                preparation = FenPositionContinuationBatchPreparation(
                    preparedUciLines = listOf(listOf("e2e4", "e7e5")),
                    sourceLinesCount = 1,
                    exactDuplicateLinesCount = 0,
                    coveredPrefixLinesCount = 0,
                ),
            ) as CreateFenPositionContinuationBatchResult.Success
            TestIds(position.id, result.insertedIds.single())
        }.also {
            composeRule.activityRule.scenario.recreate()
            waitForTag(HomeRegularContentTestTag)
        }
    }

    private fun openViewer(positionId: Long) {
        composeRule.onNodeWithTag(HomeRegularContentTestTag)
            .performScrollToNode(hasTestTag(FenPositionCatalogHomeEntryTestTag))
        composeRule.onNodeWithTag(FenPositionCatalogHomeEntryTestTag).performClick()
        waitForTag(fenPositionCatalogCardTestTag(positionId))
        composeRule.onNodeWithTag(fenPositionCatalogCardTestTag(positionId)).performClick()
        composeRule.onNodeWithTag(FenPositionCatalogOpenTestTag).performClick()
        composeRule.onNodeWithTag(FenPositionDetailsContinuationsHeaderTestTag)
            .performClick()
        composeRule.onNodeWithText("1. e4 e5").performClick()
        waitForTag(FenPositionContinuationDetailsDeleteTestTag)
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag(tag).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    private data class TestIds(val positionId: Long, val continuationId: Long)

    private companion object {
        const val InitialPositionFen =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKB1R w KQkq -"
    }
}
