package com.example.chessboard.ui.screen.fenpositions.catalog

/*
 * File role: verifies the pure Compose UI behavior of the FEN position catalog screen.
 * Allowed here:
 * - loading, empty, card selection, pagination callbacks, and board-originated scroll tests
 * Not allowed here:
 * - Room/service integration, app navigation routing, or other FEN feature screens
 * Validation date: 2026-08-31
 */

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCatalogEmptyTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCatalogLoadingTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCatalogNextPageTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCatalogPreviousPageTestTag
import com.example.chessboard.ui.testtags.fenpositions.fenPositionCatalogBoardTestTag
import com.example.chessboard.ui.testtags.fenpositions.fenPositionCatalogCardTestTag
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FenPositionCatalogScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun catalogScreen_showsLoadingState() {
        val callbacks = CatalogCallbackRecorder()

        setCatalogScreen(
            uiState = FenPositionCatalogUiState(),
            paginationState = firstPagePagination(),
            selectedPositionId = null,
            callbacks = callbacks,
        )

        composeRule.onNodeWithTag(FenPositionCatalogLoadingTestTag).assertIsDisplayed()
    }

    @Test
    fun catalogScreen_showsEmptyStateAndDisabledPagination() {
        val callbacks = CatalogCallbackRecorder()

        setCatalogScreen(
            uiState = FenPositionCatalogUiState(isLoading = false),
            paginationState = firstPagePagination(),
            selectedPositionId = null,
            callbacks = callbacks,
        )

        composeRule.onNodeWithTag(FenPositionCatalogEmptyTestTag).assertIsDisplayed()
        composeRule.onNodeWithText("No positions").assertIsDisplayed()
        composeRule.onNodeWithTag(FenPositionCatalogPreviousPageTestTag).assertIsNotEnabled()
        composeRule.onNodeWithTag(FenPositionCatalogNextPageTestTag).assertIsNotEnabled()
    }

    @Test
    fun catalogScreen_displaysPositionNameThemeAndBoard() {
        val position = catalogPosition(
            id = 7L,
            name = "Isolated Pawn",
            theme = "Strategy",
        )

        setCatalogScreen(
            uiState = FenPositionCatalogUiState(
                isLoading = false,
                positions = listOf(position),
            ),
            paginationState = firstPagePagination(totalPositionsCount = 1),
            selectedPositionId = null,
            callbacks = CatalogCallbackRecorder(),
        )

        composeRule.onNodeWithText("Isolated Pawn").assertIsDisplayed()
        composeRule.onNodeWithText("Theme: Strategy").assertIsDisplayed()
        composeRule.onNodeWithTag(
            testTag = fenPositionCatalogBoardTestTag(7L),
            useUnmergedTree = true,
        ).assertIsDisplayed()
    }

    @Test
    fun catalogScreen_clickingCardReportsDatabaseIdAndSelectsCard() {
        val callbacks = CatalogCallbackRecorder()
        var selectedPositionId by mutableStateOf<Long?>(null)

        composeRule.setContent {
            ChessBoardTheme {
                FenPositionCatalogScreen(
                    uiState = FenPositionCatalogUiState(
                        isLoading = false,
                        positions = listOf(
                            catalogPosition(id = 42L, name = "Second"),
                            catalogPosition(id = 11L, name = "First"),
                        ),
                    ),
                    paginationState = firstPagePagination(totalPositionsCount = 2),
                    selectedPositionId = selectedPositionId,
                    onBackClick = callbacks::recordBackClick,
                    onHomeClick = callbacks::recordHomeClick,
                    onPositionSelected = { positionId ->
                        callbacks.recordSelectedPosition(positionId)
                        selectedPositionId = positionId
                    },
                    onOpenPreviousPageClick = callbacks::recordPreviousPageClick,
                    onOpenNextPageClick = callbacks::recordNextPageClick,
                )
            }
        }

        composeRule.onNodeWithTag(fenPositionCatalogCardTestTag(42L)).performClick()

        composeRule.onNodeWithTag(fenPositionCatalogCardTestTag(11L)).assertIsNotSelected()
        composeRule.onNodeWithTag(fenPositionCatalogCardTestTag(42L)).assertIsSelected()
        composeRule.runOnIdle {
            assertEquals(listOf(42L), callbacks.selectedPositionIds)
        }
    }

    @Test
    fun catalogScreen_pageAndTopBarActionsCallExpectedCallbacks() {
        val callbacks = CatalogCallbackRecorder()

        setCatalogScreen(
            uiState = FenPositionCatalogUiState(
                isLoading = false,
                positions = listOf(catalogPosition(id = 7L)),
            ),
            paginationState = FenPositionCatalogPaginationState(
                totalPositionsCount = 12,
                currentPage = 2,
                totalPages = 3,
                canOpenPreviousPage = true,
                canOpenNextPage = true,
            ),
            selectedPositionId = null,
            callbacks = callbacks,
        )

        composeRule.onNodeWithText("Positions: 12 · Page 2 of 3").assertIsDisplayed()
        composeRule.onNodeWithTag(FenPositionCatalogPreviousPageTestTag).assertIsEnabled().performClick()
        composeRule.onNodeWithTag(FenPositionCatalogNextPageTestTag).assertIsEnabled().performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithContentDescription("Home").performClick()

        composeRule.runOnIdle {
            assertEquals(1, callbacks.previousPageClicks)
            assertEquals(1, callbacks.nextPageClicks)
            assertEquals(1, callbacks.backClicks)
            assertEquals(1, callbacks.homeClicks)
        }
    }

    @Test
    fun catalogScreen_scrollsWhenGestureStartsOnBoard() {
        val positions = listOf(
            catalogPosition(id = 1L, name = "First"),
            catalogPosition(id = 2L, name = "Second"),
            catalogPosition(id = 3L, name = "Third"),
        )

        setCatalogScreen(
            uiState = FenPositionCatalogUiState(
                isLoading = false,
                positions = positions,
            ),
            paginationState = firstPagePagination(totalPositionsCount = positions.size),
            selectedPositionId = null,
            callbacks = CatalogCallbackRecorder(),
        )

        composeRule.onNodeWithTag(fenPositionCatalogCardTestTag(3L)).assertIsNotDisplayed()
        composeRule.onNodeWithTag(
            testTag = fenPositionCatalogBoardTestTag(1L),
            useUnmergedTree = true,
        ).performTouchInput { swipeUp() }

        composeRule.onNodeWithTag(fenPositionCatalogCardTestTag(3L)).assertIsDisplayed()
    }

    private fun setCatalogScreen(
        uiState: FenPositionCatalogUiState,
        paginationState: FenPositionCatalogPaginationState,
        selectedPositionId: Long?,
        callbacks: CatalogCallbackRecorder,
    ) {
        composeRule.setContent {
            ChessBoardTheme {
                FenPositionCatalogScreen(
                    uiState = uiState,
                    paginationState = paginationState,
                    selectedPositionId = selectedPositionId,
                    onBackClick = callbacks::recordBackClick,
                    onHomeClick = callbacks::recordHomeClick,
                    onPositionSelected = callbacks::recordSelectedPosition,
                    onOpenPreviousPageClick = callbacks::recordPreviousPageClick,
                    onOpenNextPageClick = callbacks::recordNextPageClick,
                )
            }
        }
    }

    private fun firstPagePagination(totalPositionsCount: Int = 0): FenPositionCatalogPaginationState {
        return FenPositionCatalogPaginationState(
            totalPositionsCount = totalPositionsCount,
            currentPage = 1,
            totalPages = 1,
            canOpenPreviousPage = false,
            canOpenNextPage = false,
        )
    }

    private fun catalogPosition(
        id: Long,
        name: String = "Position $id",
        theme: String = "Tactics",
    ): FenPositionCatalogItem {
        return FenPositionCatalogItem(
            id = id,
            fen = InitialPositionFen,
            name = name,
            theme = theme,
        )
    }

    private class CatalogCallbackRecorder {
        var backClicks = 0
            private set
        var homeClicks = 0
            private set
        var previousPageClicks = 0
            private set
        var nextPageClicks = 0
            private set
        val selectedPositionIds = mutableListOf<Long>()

        fun recordBackClick() {
            backClicks += 1
        }

        fun recordHomeClick() {
            homeClicks += 1
        }

        fun recordPreviousPageClick() {
            previousPageClicks += 1
        }

        fun recordNextPageClick() {
            nextPageClicks += 1
        }

        fun recordSelectedPosition(positionId: Long) {
            selectedPositionIds += positionId
        }
    }

    private companion object {
        const val InitialPositionFen =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -"
    }
}
