package com.example.chessboard.ui.screen.fenpositions.catalog

/*
 * File role: verifies app navigation into the FEN position catalog.
 * Allowed here:
 * - MainActivity routing from shared entry points into the catalog and FEN analysis
 * - top-level catalog navigation behavior
 * Not allowed here:
 * - detailed catalog rendering, Room/service behavior, or other FEN feature screens
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
import com.example.chessboard.ui.HomeRegularContentTestTag
import com.example.chessboard.ui.LineAnalysisContentTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCatalogAnalyzeTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCatalogEmptyTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCatalogHomeEntryTestTag
import com.example.chessboard.ui.testtags.fenpositions.fenPositionCatalogCardTestTag
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FenPositionCatalogNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val dbProvider: DatabaseProvider
        get() = DatabaseProvider.createInstance(composeRule.activity)

    @Before
    fun setUp() {
        dbProvider.clearAllData()
    }

    @Test
    fun regularHome_fenPositionCatalogEntryOpensEmptyCatalog() {
        openRegularHome()
        composeRule
            .onNodeWithTag(HomeRegularContentTestTag)
            .performScrollToNode(hasTestTag(FenPositionCatalogHomeEntryTestTag))

        composeRule
            .onNodeWithTag(FenPositionCatalogHomeEntryTestTag)
            .assertIsDisplayed()
            .performClick()

        waitForNodeDisplayed(FenPositionCatalogEmptyTestTag)
        composeRule.onNodeWithText("No positions").assertIsDisplayed()
    }

    @Test
    fun selectedCatalogPositionOpensFenAnalysisWithoutContinuations() {
        val positionId = prepareRegularHomeWithPosition()
        openCatalog()

        waitForNodeDisplayed(fenPositionCatalogCardTestTag(positionId))
        composeRule.onNodeWithTag(fenPositionCatalogCardTestTag(positionId)).performClick()
        composeRule.onNodeWithTag(FenPositionCatalogAnalyzeTestTag).performClick()

        waitForNodeDisplayed(LineAnalysisContentTestTag)
        composeRule.onNodeWithText("Analyze Line").assertIsDisplayed()
        composeRule.onNodeWithText("e4").assertDoesNotExist()
    }

    @Test
    fun simpleHome_doesNotShowFenPositionCatalogEntry() {
        composeRule.activityRule.scenario.recreate()
        waitForTextDisplayed("Search openings...")

        composeRule.onNodeWithTag(FenPositionCatalogHomeEntryTestTag).assertDoesNotExist()
    }

    private fun openRegularHome() {
        runBlocking {
            dbProvider.createUserProfileService().updateSettings(
                simpleViewEnabled = false,
                removeLineIfRepIsZero = false,
                hideLinesWithWeightZero = false,
            )
        }
        composeRule.activityRule.scenario.recreate()
        waitForNodeDisplayed(HomeRegularContentTestTag)
    }

    private fun prepareRegularHomeWithPosition(): Long {
        val positionId = runBlocking {
            val result = dbProvider.createFenPositionService().create(
                fen = InitialPositionFen,
                name = "Selected position",
                theme = "Strategy",
                description = "",
            ) as CreateFenPositionResult.Success
            result.id
        }
        openRegularHome()
        return positionId
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

    private fun waitForTextDisplayed(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText(text).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    private companion object {
        const val InitialPositionFen =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -"
    }
}
