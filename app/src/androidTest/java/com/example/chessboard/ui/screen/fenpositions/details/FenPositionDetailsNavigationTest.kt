package com.example.chessboard.ui.screen.fenpositions.details

/*
 * File role: verifies app routing between the FEN catalog and one persisted position's details.
 * Allowed here:
 * - real MainActivity navigation, selected-id routing, details loading, and return selection
 * Not allowed here:
 * - isolated details layout assertions, editing, deletion, or unrelated navigation
 * Validation date: 2026-09-01
 */

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.chessboard.MainActivity
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.service.CreateFenPositionResult
import com.example.chessboard.ui.HomeRegularContentTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCatalogHomeEntryTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCatalogOpenTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsContentTestTag
import com.example.chessboard.ui.testtags.fenpositions.fenPositionCatalogCardTestTag
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FenPositionDetailsNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val dbProvider: DatabaseProvider
        get() = DatabaseProvider.createInstance(composeRule.activity)

    @Before
    fun setUp() {
        dbProvider.clearAllData()
    }

    @Test
    fun selectedCatalogPositionOpensDetailsAndRemainsSelectedAfterBack() {
        val positionId = prepareRegularHomeWithPosition()
        openCatalog()

        waitForNodeDisplayed(fenPositionCatalogCardTestTag(positionId))
        composeRule.onNodeWithTag(fenPositionCatalogCardTestTag(positionId)).performClick()
        composeRule.onNodeWithTag(FenPositionCatalogOpenTestTag)
            .assertIsEnabled()
            .performClick()

        waitForNodeDisplayed(FenPositionDetailsContentTestTag)
        composeRule.onNodeWithText("Selected position").assertIsDisplayed()
        composeRule.onNodeWithText("Position description").assertDoesNotExist()

        composeRule.onNodeWithContentDescription("Back").performClick()

        waitForNodeDisplayed(fenPositionCatalogCardTestTag(positionId))
        composeRule.onNodeWithTag(fenPositionCatalogCardTestTag(positionId)).assertIsSelected()
    }

    private fun prepareRegularHomeWithPosition(): Long {
        return runBlocking {
            dbProvider.createUserProfileService().updateSettings(
                simpleViewEnabled = false,
                removeLineIfRepIsZero = false,
                hideLinesWithWeightZero = false,
            )
            val result = dbProvider.createFenPositionService().create(
                fen = InitialPositionFen,
                name = "Selected position",
                theme = "Strategy",
                description = "Position description",
            )
            assertTrue(result is CreateFenPositionResult.Success)
            (result as CreateFenPositionResult.Success).id
        }.also {
            composeRule.activityRule.scenario.recreate()
            waitForNodeDisplayed(HomeRegularContentTestTag)
        }
    }

    private fun openCatalog() {
        composeRule
            .onNodeWithTag(HomeRegularContentTestTag)
            .performScrollToNode(hasTestTag(FenPositionCatalogHomeEntryTestTag))
        composeRule.onNodeWithTag(FenPositionCatalogHomeEntryTestTag).performClick()
    }

    private fun waitForNodeDisplayed(testTag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag(testTag).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    private companion object {
        const val InitialPositionFen =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -"
    }
}
