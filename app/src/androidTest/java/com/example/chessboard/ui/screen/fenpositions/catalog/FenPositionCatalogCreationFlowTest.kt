package com.example.chessboard.ui.screen.fenpositions.catalog

/*
 * File role: verifies catalog creation-flow behavior backed by the real FEN position service.
 * Allowed here:
 * - create-dialog interaction and persistence-result presentation
 * - checking catalog behavior after successful or rejected creation attempts
 * Not allowed here:
 * - isolated dialog validation, paging, or unrelated app navigation
 * Validation date: 2026-08-31
 */

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.example.chessboard.MainActivity
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.service.CreateFenPositionResult
import com.example.chessboard.ui.HomeRegularContentTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCatalogAddTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCatalogHomeEntryTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCreateConfirmTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCreateDialogTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCreateFenInputTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCreateThemeInputTestTag
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FenPositionCatalogCreationFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val dbProvider: DatabaseProvider
        get() = DatabaseProvider.createInstance(composeRule.activity)

    @Before
    fun setUp() {
        dbProvider.clearAllData()
    }

    @Test
    fun duplicateFenShowsMessageDialogAndKeepsCreateDialogOpen() {
        openRegularCatalogWithExistingPosition()

        composeRule.onNodeWithTag(FenPositionCatalogAddTestTag).performClick()
        composeRule.onNodeWithTag(FenPositionCreateFenInputTestTag)
            .performTextInput(InitialPositionFen)
        composeRule.onNodeWithTag(FenPositionCreateThemeInputTestTag)
            .performScrollTo()
            .performTextInput("Strategy")

        waitForNodeEnabled(FenPositionCreateConfirmTestTag)
        composeRule.onNodeWithTag(FenPositionCreateConfirmTestTag).performClick()

        waitForTextDisplayed("Save failed")
        composeRule.onNodeWithText("A position with this FEN already exists").assertIsDisplayed()
        composeRule.onNodeWithTag(FenPositionCreateDialogTestTag).assertIsDisplayed()
    }

    private fun openRegularCatalogWithExistingPosition() {
        runBlocking {
            dbProvider.createUserProfileService().updateSettings(
                simpleViewEnabled = false,
                removeLineIfRepIsZero = false,
                hideLinesWithWeightZero = false,
            )
            val result = dbProvider.createFenPositionService().create(
                fen = InitialPositionFen,
                name = "Existing position",
                theme = "Strategy",
                description = "",
            )
            assertTrue(result is CreateFenPositionResult.Success)
        }

        composeRule.activityRule.scenario.recreate()
        waitForNodeDisplayed(HomeRegularContentTestTag)
        composeRule
            .onNodeWithTag(HomeRegularContentTestTag)
            .performScrollToNode(hasTestTag(FenPositionCatalogHomeEntryTestTag))
        composeRule.onNodeWithTag(FenPositionCatalogHomeEntryTestTag).performClick()
        waitForNodeDisplayed(FenPositionCatalogAddTestTag)
    }

    private fun waitForNodeDisplayed(testTag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag(testTag).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    private fun waitForNodeEnabled(testTag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag(testTag).assertIsEnabled()
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
