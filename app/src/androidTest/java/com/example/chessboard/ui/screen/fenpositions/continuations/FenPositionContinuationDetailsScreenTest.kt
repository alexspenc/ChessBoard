package com.example.chessboard.ui.screen.fenpositions.continuations

/*
 * File role: verifies the FEN continuation viewer through the app's real navigation.
 * Allowed here:
 * - viewer board, SAN presentation, home navigation, and move controls
 * Not allowed here:
 * - Room implementation details or deletion workflow coverage
 * Validation date: 2026-09-03
 */

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.example.chessboard.MainActivity
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.service.CreateFenPositionResult
import com.example.chessboard.service.CreateFenPositionContinuationBatchResult
import com.example.chessboard.service.FenPositionContinuationBatchPreparation
import com.example.chessboard.ui.HomeRegularContentTestTag
import com.example.chessboard.ui.MoveTreeBoxTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCatalogHomeEntryTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCatalogOpenTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationDetailsBoardTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationDetailsContentTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsContinuationsHeaderTestTag
import com.example.chessboard.ui.testtags.fenpositions.fenPositionCatalogCardTestTag
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FenPositionContinuationDetailsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val dbProvider: DatabaseProvider
        get() = DatabaseProvider.createInstance(composeRule.activity)

    @Before
    fun setUp() {
        dbProvider.clearAllData()
    }

    @Test
    fun viewerShowsInitialBoardAndSanLine() {
        val positionId = preparePositionWithContinuation()
        openViewer(positionId)

        waitForTag(FenPositionContinuationDetailsContentTestTag)
        composeRule.onNodeWithTag(FenPositionContinuationDetailsBoardTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(MoveTreeBoxTestTag).assertIsDisplayed()
        composeRule.onNodeWithText("e4").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Home").assertIsDisplayed()
    }

    @Test
    fun moveControlsFollowInitialAndFinalBoundaries() {
        val positionId = preparePositionWithContinuation()
        openViewer(positionId)

        composeRule.onNodeWithText("Back").assertIsNotEnabled()
        composeRule.onNodeWithText("Forward").assertIsEnabled().performClick()
        composeRule.onNodeWithText("Back").assertIsEnabled().performClick()
        composeRule.onNodeWithText("Back").assertIsNotEnabled()
    }

    private fun preparePositionWithContinuation(): Long {
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
            )
            assertTrue(result is CreateFenPositionContinuationBatchResult.Success)
            position.id
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
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("e4").performClick()
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag(tag).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    private companion object {
        const val InitialPositionFen =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -"
    }
}
