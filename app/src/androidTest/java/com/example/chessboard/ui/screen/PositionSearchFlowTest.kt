package com.example.chessboard.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.boardmodel.InitialBoardFen
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.entity.SideMask
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.testing.fenStateDescriptionMatcher
import com.example.chessboard.ui.ChessBoardWithCoordinates
import com.example.chessboard.ui.InteractiveChessBoardTestTag
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.screen.positions.PositionEditorScreenContainer
import com.example.chessboard.ui.screen.trainSingleGame.TrainSingleGamePhase
import com.example.chessboard.ui.screen.trainSingleGame.TrainSingleGameUiState
import com.example.chessboard.ui.screen.trainSingleGame.advanceProgramMoves
import com.example.chessboard.ui.screen.trainSingleGame.buildStartTrainingState
import com.example.chessboard.ui.screen.trainSingleGame.handleTrainingProgress
import com.example.chessboard.ui.screen.trainSingleGame.resolveBoardInteractionEnabled
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PositionSearchFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun trainSingleGame_searchByPositionReturnsCurrentBoardFen() {
        var searchedFen = ""

        composeRule.setContent {
            ChessBoardTheme {
                TrainSingleGameSearchHarness(
                    uciMoves = listOf("e2e4", "e7e5"),
                    onSearchByPositionClick = { searchedFen = it }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Start training").performClick()
        composeRule.waitForIdle()

        val boardNode = composeRule.onNodeWithTag(InteractiveChessBoardTestTag)
        boardNode.assert(fenStateDescriptionMatcher(InitialBoardFen))
        boardNode.performTouchInput {
            val squareSize = width / 8f
            click(squareCenter(file = 4, row = 6, squareSize = squareSize))
        }
        boardNode.performTouchInput {
            val squareSize = width / 8f
            click(squareCenter(file = 4, row = 4, squareSize = squareSize))
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Search by position").performClick()

        composeRule.runOnIdle {
            assertEquals(
                "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2",
                searchedFen
            )
        }
    }

    @Test
    fun positionEditorScreenContainer_usesPassedInitialFen() {
        val dbProvider = DatabaseProvider.createInstance(composeRule.activity)
        val initialFen = "4k3/8/8/8/8/8/8/4K3 b - -"

        composeRule.setContent {
            ChessBoardTheme {
                PositionEditorScreenContainer(
                    initialFen = initialFen,
                    screenContext = ScreenContainerContext(inDbProvider = dbProvider),
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag(InteractiveChessBoardTestTag).assert(
            fenStateDescriptionMatcher("4k3/8/8/8/8/8/8/4K3 b - - 0 1")
        )
    }

    private fun squareCenter(file: Int, row: Int, squareSize: Float): Offset {
        return Offset(
            x = file * squareSize + squareSize / 2f,
            y = row * squareSize + squareSize / 2f,
        )
    }

    @Composable
    private fun TrainSingleGameSearchHarness(
        uciMoves: List<String>,
        onSearchByPositionClick: (String) -> Unit,
    ) {
        var uiState by remember { mutableStateOf(TrainSingleGameUiState()) }
        val gameController = remember { GameController(BoardOrientation.WHITE) }

        SideEffect {
            gameController.setUserMovesEnabled(resolveBoardInteractionEnabled(uiState))
            gameController.setAllowedMoveUci(null)
        }

        LaunchedEffect(uiState.phase, gameController.boardState, uiState.expectedPly, uciMoves) {
            uiState = handleTrainingProgress(
                uiState = uiState,
                gameController = gameController,
                uciMoves = uciMoves,
                currentOrientation = BoardOrientation.WHITE,
                sidesCount = 1,
            )
        }

        Column {
            androidx.compose.material3.IconButton(
                onClick = {
                    gameController.resetToStartPosition()
                    uiState = advanceProgramMoves(
                        uiState = buildStartTrainingState(uiState),
                        gameController = gameController,
                        uciMoves = uciMoves,
                        currentOrientation = BoardOrientation.WHITE,
                        sidesCount = 1,
                    )
                }
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Start training",
                )
            }

            androidx.compose.material3.IconButton(
                onClick = { onSearchByPositionClick(gameController.getFen()) }
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search by position",
                )
            }

            androidx.compose.material3.Text("Phase: ${uiState.phase == TrainSingleGamePhase.Training}")

            ChessBoardWithCoordinates(
                gameController = gameController,
                modifier = Modifier.size(320.dp),
            )
        }
    }
}
