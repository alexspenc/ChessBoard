package com.example.chessboard.ui.screen.fenpositions.details

/*
 * File role: verifies pure Compose presentation and expansion behavior of FEN position details.
 * Allowed here:
 * - loading/terminal states, board content, description, continuations, and back callback assertions
 * Not allowed here:
 * - Room/service integration, app routing, or persistence mutations
 * Validation date: 2026-09-01
 */

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsBoardTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsContinuationsHeaderTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsDescriptionBodyTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsDescriptionCollapseTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsDescriptionHeaderTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsLoadFailedTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsLoadingTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsNotFoundTestTag
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FenPositionDetailsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun detailsScreenShowsLoadingState() {
        setDetailsScreen(FenPositionDetailsUiState.Loading)

        composeRule.onNodeWithTag(FenPositionDetailsLoadingTestTag).assertIsDisplayed()
    }

    @Test
    fun detailsScreenShowsNotFoundState() {
        setDetailsScreen(FenPositionDetailsUiState.NotFound)

        composeRule.onNodeWithTag(FenPositionDetailsNotFoundTestTag).assertIsDisplayed()
        composeRule.onNodeWithText("Position not found").assertIsDisplayed()
    }

    @Test
    fun detailsScreenShowsLoadFailedState() {
        setDetailsScreen(FenPositionDetailsUiState.LoadFailed)

        composeRule.onNodeWithTag(FenPositionDetailsLoadFailedTestTag).assertIsDisplayed()
        composeRule.onNodeWithText("Could not load the position").assertIsDisplayed()
    }

    @Test
    fun detailsScreenShowsPositionBoardAndCollapsedSections() {
        setDetailsScreen(contentState(description = "Activate the king"))

        composeRule.onNodeWithText("Isolated Pawn").assertIsDisplayed()
        composeRule.onNodeWithText("Theme: Strategy").assertIsDisplayed()
        composeRule.onNodeWithTag(
            testTag = FenPositionDetailsBoardTestTag,
            useUnmergedTree = true,
        ).assertIsDisplayed()
        composeRule.onNodeWithTag(FenPositionDetailsDescriptionHeaderTestTag)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(FenPositionDetailsContinuationsHeaderTestTag)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(FenPositionDetailsDescriptionBodyTestTag).assertDoesNotExist()
    }

    @Test
    fun descriptionCanExpandAndCollapseWithoutTextClickCollapsingIt() {
        setDetailsScreen(contentState(description = "Activate the king"))

        composeRule.onNodeWithTag(FenPositionDetailsDescriptionHeaderTestTag)
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag(FenPositionDetailsDescriptionBodyTestTag)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Activate the king").assertExists()

        composeRule.onNodeWithTag(FenPositionDetailsDescriptionBodyTestTag)
            .performTouchInput { click(center) }
        composeRule.onNodeWithText("Activate the king").assertIsDisplayed()

        composeRule.onNodeWithTag(FenPositionDetailsContinuationsHeaderTestTag)
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithContentDescription("Collapse continuations").assertExists()
        composeRule.onNodeWithText("Activate the king").assertExists()

        composeRule.onNodeWithTag(FenPositionDetailsDescriptionCollapseTestTag)
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag(FenPositionDetailsDescriptionBodyTestTag).assertDoesNotExist()
    }

    @Test
    fun expandedDescriptionShowsAbsentMessageWhenNoDescriptionExists() {
        setDetailsScreen(contentState(description = null))

        composeRule.onNodeWithTag(FenPositionDetailsDescriptionHeaderTestTag)
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithTag(FenPositionDetailsDescriptionBodyTestTag)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Description is absent").assertExists()
    }

    @Test
    fun topBarBackButtonCallsRequiredCallback() {
        var backClicks = 0
        setDetailsScreen(
            uiState = contentState(description = null),
            onBackClick = {
                backClicks += 1
            },
        )

        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.runOnIdle {
            assertEquals(1, backClicks)
        }
    }

    private fun setDetailsScreen(
        uiState: FenPositionDetailsUiState,
    ) {
        setDetailsScreen(
            uiState = uiState,
            onBackClick = ::recordIgnoredBackClick,
        )
    }

    private fun setDetailsScreen(
        uiState: FenPositionDetailsUiState,
        onBackClick: () -> Unit,
    ) {
        composeRule.setContent {
            ChessBoardTheme {
                FenPositionDetailsScreen(
                    uiState = uiState,
                    onBackClick = onBackClick,
                )
            }
        }
    }

    private fun contentState(description: String?): FenPositionDetailsUiState.Content {
        return FenPositionDetailsUiState.Content(
            FenPositionDetailsItem(
                id = 7L,
                fen = InitialPositionFen,
                name = "Isolated Pawn",
                theme = "Strategy",
                description = description,
            ),
        )
    }

    private fun recordIgnoredBackClick() = Unit

    private companion object {
        const val InitialPositionFen =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -"
    }
}
