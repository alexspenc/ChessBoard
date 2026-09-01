package com.example.chessboard.ui.screen.fenpositions.details

/*
 * File role: verifies app routing between the FEN catalog and one persisted position's details.
 * Allowed here:
 * - real MainActivity routing, adjacent-position navigation, details loading, and return selection
 * Not allowed here:
 * - isolated details layout assertions, editing, deletion, or unrelated navigation
 * Validation date: 2026-09-01
 */

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
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
import com.example.chessboard.ui.HomeRegularContentTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCatalogHomeEntryTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCatalogOpenTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsContentTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsDescriptionBodyTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsDescriptionHeaderTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsNextPositionTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsPreviousPositionTestTag
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

    @Test
    fun topBarNavigationFollowsNewestFirstOrderAndResetsPositionContent() {
        val positionIds = prepareRegularHomeWithThreePositions()
        openCatalog()

        waitForNodeExists(fenPositionCatalogCardTestTag(positionIds.middle))
        composeRule.onNodeWithTag(fenPositionCatalogCardTestTag(positionIds.middle))
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag(FenPositionCatalogOpenTestTag).performClick()
        waitForTextDisplayed("Middle position")

        composeRule.onNodeWithTag(FenPositionDetailsDescriptionHeaderTestTag)
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag(FenPositionDetailsDescriptionBodyTestTag)
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithTag(FenPositionDetailsPreviousPositionTestTag)
            .assertIsEnabled()
            .performClick()
        waitForTextDisplayed("Newest position")
        composeRule.onNodeWithTag(FenPositionDetailsDescriptionBodyTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(FenPositionDetailsPreviousPositionTestTag).assertIsNotEnabled()

        composeRule.onNodeWithTag(FenPositionDetailsNextPositionTestTag)
            .assertIsEnabled()
            .performClick()
        waitForTextDisplayed("Middle position")
        composeRule.onNodeWithTag(FenPositionDetailsNextPositionTestTag).performClick()
        waitForTextDisplayed("Oldest position")
        composeRule.onNodeWithTag(FenPositionDetailsNextPositionTestTag).assertIsNotEnabled()

        composeRule.onNodeWithContentDescription("Back").performClick()

        waitForNodeExists(fenPositionCatalogCardTestTag(positionIds.oldest))
        composeRule.onNodeWithTag(fenPositionCatalogCardTestTag(positionIds.oldest))
            .performScrollTo()
            .assertIsSelected()
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

    private fun prepareRegularHomeWithThreePositions(): TestPositionIds {
        return runBlocking {
            dbProvider.createUserProfileService().updateSettings(
                simpleViewEnabled = false,
                removeLineIfRepIsZero = false,
                hideLinesWithWeightZero = false,
            )
            val service = dbProvider.createFenPositionService()
            val oldest = service.create(
                fen = OldestPositionFen,
                name = "Oldest position",
                theme = "Strategy",
                description = "Oldest description",
            ) as CreateFenPositionResult.Success
            val middle = service.create(
                fen = MiddlePositionFen,
                name = "Middle position",
                theme = "Strategy",
                description = "Middle description",
            ) as CreateFenPositionResult.Success
            val newest = service.create(
                fen = NewestPositionFen,
                name = "Newest position",
                theme = "Strategy",
                description = "Newest description",
            ) as CreateFenPositionResult.Success

            TestPositionIds(
                oldest = oldest.id,
                middle = middle.id,
                newest = newest.id,
            )
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

    private fun waitForNodeExists(testTag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag(testTag).assertExists()
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

    private data class TestPositionIds(
        val oldest: Long,
        val middle: Long,
        val newest: Long,
    )

    private companion object {
        const val InitialPositionFen =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -"
        const val OldestPositionFen = "4k3/8/8/8/8/8/8/4K3 w - -"
        const val MiddlePositionFen = "4k3/8/8/8/8/8/8/4K3 b - -"
        const val NewestPositionFen = "4k3/8/8/8/8/8/P7/4K3 w - -"
    }
}
