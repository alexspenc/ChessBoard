package com.example.chessboard.ui.screen.fenpositions.details

/*
 * File role: verifies pure Compose presentation and expansion behavior of FEN position details.
 * Allowed here:
 * - loading/terminal states, board content, expandable sections, and action callback assertions
 * Not allowed here:
 * - Room/service integration, app routing, or persistence mutations
 * Validation date: 2026-09-02
 */

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsDeleteTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsAddContinuationTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsCopyFenTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsEditTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsLoadFailedTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsLoadingTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsNextPositionTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsNotFoundTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsPreviousPositionTestTag
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
        composeRule.onNodeWithTag(FenPositionDetailsPreviousPositionTestTag).assertIsNotEnabled()
        composeRule.onNodeWithTag(FenPositionDetailsNextPositionTestTag).assertIsNotEnabled()
        composeRule.onNodeWithTag(FenPositionDetailsEditTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(FenPositionDetailsDeleteTestTag).assertDoesNotExist()
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
    fun expandedContinuationsShowTheirSanLines() {
        setDetailsScreen(
            contentState(
                description = null,
                continuationSanLines = listOf("1. e4 e5 2. Nf3"),
            ),
        )

        composeRule.onNodeWithTag(FenPositionDetailsContinuationsHeaderTestTag)
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("1. e4 e5 2. Nf3").assertIsDisplayed()
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

    @Test
    fun availablePositionNavigationCallsRequiredCallbacks() {
        var previousClicks = 0
        var nextClicks = 0
        setDetailsScreen(
            uiState = contentState(
                description = null,
                previousPositionId = 8L,
                nextPositionId = 6L,
            ),
            onBackClick = ::recordIgnoredBackClick,
            onPreviousPositionClick = {
                previousClicks += 1
            },
            onNextPositionClick = {
                nextClicks += 1
            },
            onEditPositionClick = ::recordIgnoredEditPositionClick,
            onDeletePositionClick = ::recordIgnoredDeletePositionClick,
            onAddContinuationClick = ::recordIgnoredAddContinuationClick,
            onCopyFenClick = ::recordIgnoredCopyFenClick,
        )

        composeRule.onNodeWithTag(FenPositionDetailsPreviousPositionTestTag)
            .assertIsEnabled()
            .performClick()
        composeRule.onNodeWithTag(FenPositionDetailsNextPositionTestTag)
            .assertIsEnabled()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, previousClicks)
            assertEquals(1, nextClicks)
        }
    }

    @Test
    fun catalogEdgesDisableUnavailablePositionNavigation() {
        setDetailsScreen(
            contentState(
                description = null,
                previousPositionId = null,
                nextPositionId = 6L,
            ),
        )

        composeRule.onNodeWithTag(FenPositionDetailsPreviousPositionTestTag).assertIsNotEnabled()
        composeRule.onNodeWithTag(FenPositionDetailsNextPositionTestTag).assertIsEnabled()
    }

    @Test
    fun contentDeleteButtonCallsRequiredCallback() {
        var deleteClicks = 0
        setDetailsScreen(
            uiState = contentState(description = null),
            onBackClick = ::recordIgnoredBackClick,
            onPreviousPositionClick = ::recordIgnoredPositionNavigationClick,
            onNextPositionClick = ::recordIgnoredPositionNavigationClick,
            onEditPositionClick = ::recordIgnoredEditPositionClick,
            onDeletePositionClick = {
                deleteClicks += 1
            },
            onAddContinuationClick = ::recordIgnoredAddContinuationClick,
            onCopyFenClick = ::recordIgnoredCopyFenClick,
        )

        composeRule.onNodeWithTag(FenPositionDetailsDeleteTestTag)
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, deleteClicks)
        }
    }

    @Test
    fun contentEditButtonCallsRequiredCallback() {
        var editClicks = 0
        setDetailsScreen(
            uiState = contentState(description = null),
            onBackClick = ::recordIgnoredBackClick,
            onPreviousPositionClick = ::recordIgnoredPositionNavigationClick,
            onNextPositionClick = ::recordIgnoredPositionNavigationClick,
            onEditPositionClick = {
                editClicks += 1
            },
            onDeletePositionClick = ::recordIgnoredDeletePositionClick,
            onAddContinuationClick = ::recordIgnoredAddContinuationClick,
            onCopyFenClick = ::recordIgnoredCopyFenClick,
        )

        composeRule.onNodeWithTag(FenPositionDetailsEditTestTag)
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, editClicks)
        }
    }

    @Test
    fun contentAddContinuationButtonCallsRequiredCallback() {
        var addClicks = 0
        setDetailsScreen(
            uiState = contentState(description = null),
            onBackClick = ::recordIgnoredBackClick,
            onPreviousPositionClick = ::recordIgnoredPositionNavigationClick,
            onNextPositionClick = ::recordIgnoredPositionNavigationClick,
            onEditPositionClick = ::recordIgnoredEditPositionClick,
            onDeletePositionClick = ::recordIgnoredDeletePositionClick,
            onAddContinuationClick = {
                addClicks += 1
            },
            onCopyFenClick = ::recordIgnoredCopyFenClick,
        )

        composeRule.onNodeWithTag(FenPositionDetailsAddContinuationTestTag)
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, addClicks)
        }
    }

    @Test
    fun contentCopyFenButtonCallsRequiredCallback() {
        var copyClicks = 0
        setDetailsScreen(
            uiState = contentState(description = null),
            onBackClick = ::recordIgnoredBackClick,
            onPreviousPositionClick = ::recordIgnoredPositionNavigationClick,
            onNextPositionClick = ::recordIgnoredPositionNavigationClick,
            onEditPositionClick = ::recordIgnoredEditPositionClick,
            onDeletePositionClick = ::recordIgnoredDeletePositionClick,
            onAddContinuationClick = ::recordIgnoredAddContinuationClick,
            onCopyFenClick = {
                copyClicks += 1
            },
        )

        composeRule.onNodeWithTag(FenPositionDetailsCopyFenTestTag)
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, copyClicks)
        }
    }

    private fun setDetailsScreen(
        uiState: FenPositionDetailsUiState,
    ) {
        setDetailsScreen(
            uiState = uiState,
            onBackClick = ::recordIgnoredBackClick,
            onPreviousPositionClick = ::recordIgnoredPositionNavigationClick,
            onNextPositionClick = ::recordIgnoredPositionNavigationClick,
            onEditPositionClick = ::recordIgnoredEditPositionClick,
            onDeletePositionClick = ::recordIgnoredDeletePositionClick,
            onAddContinuationClick = ::recordIgnoredAddContinuationClick,
            onCopyFenClick = ::recordIgnoredCopyFenClick,
        )
    }

    private fun setDetailsScreen(
        uiState: FenPositionDetailsUiState,
        onBackClick: () -> Unit,
    ) {
        setDetailsScreen(
            uiState = uiState,
            onBackClick = onBackClick,
            onPreviousPositionClick = ::recordIgnoredPositionNavigationClick,
            onNextPositionClick = ::recordIgnoredPositionNavigationClick,
            onEditPositionClick = ::recordIgnoredEditPositionClick,
            onDeletePositionClick = ::recordIgnoredDeletePositionClick,
            onAddContinuationClick = ::recordIgnoredAddContinuationClick,
            onCopyFenClick = ::recordIgnoredCopyFenClick,
        )
    }

    private fun setDetailsScreen(
        uiState: FenPositionDetailsUiState,
        onBackClick: () -> Unit,
        onPreviousPositionClick: () -> Unit,
        onNextPositionClick: () -> Unit,
        onEditPositionClick: () -> Unit,
        onDeletePositionClick: () -> Unit,
        onAddContinuationClick: () -> Unit,
        onCopyFenClick: () -> Unit,
    ) {
        composeRule.setContent {
            ChessBoardTheme {
                FenPositionDetailsScreen(
                    uiState = uiState,
                    onBackClick = onBackClick,
                    onPreviousPositionClick = onPreviousPositionClick,
                    onNextPositionClick = onNextPositionClick,
                    onEditPositionClick = onEditPositionClick,
                    onDeletePositionClick = onDeletePositionClick,
                    onAddContinuationClick = onAddContinuationClick,
                    onCopyFenClick = onCopyFenClick,
                    canCopyFen = true,
                )
            }
        }
    }

    private fun contentState(
        description: String?,
        previousPositionId: Long? = null,
        nextPositionId: Long? = null,
        continuationSanLines: List<String> = emptyList(),
    ): FenPositionDetailsUiState.Content {
        return FenPositionDetailsUiState.Content(
            FenPositionDetailsItem(
                id = 7L,
                fen = InitialPositionFen,
                name = "Isolated Pawn",
                theme = "Strategy",
                description = description,
                continuationSanLines = continuationSanLines,
                catalogIndex = 0,
                previousPositionId = previousPositionId,
                nextPositionId = nextPositionId,
            ),
        )
    }

    private fun recordIgnoredBackClick() = Unit

    private fun recordIgnoredPositionNavigationClick() = Unit

    private fun recordIgnoredEditPositionClick() = Unit

    private fun recordIgnoredDeletePositionClick() = Unit

    private fun recordIgnoredAddContinuationClick() = Unit

    private fun recordIgnoredCopyFenClick() = Unit

    private companion object {
        const val InitialPositionFen =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -"
    }
}
