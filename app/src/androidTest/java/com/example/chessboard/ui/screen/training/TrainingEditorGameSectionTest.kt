package com.example.chessboard.ui.screen.training
import com.example.chessboard.ui.screen.training.common.ParsedTrainingEditorGame
import com.example.chessboard.ui.screen.training.common.TrainingEditorGameSection
import com.example.chessboard.ui.screen.training.common.TrainingEditorGameSectionActions
import com.example.chessboard.ui.screen.training.common.TrainingEditorGameSectionState
import com.example.chessboard.ui.screen.training.common.TrainingEditorPrimaryAction
import com.example.chessboard.ui.screen.training.common.TrainingGameEditorItem

import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.service.buildMoveLabels
import com.example.chessboard.service.parsePgnMoves
import com.example.chessboard.ui.TrainingEditorGameCardTestTag
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TrainingEditorGameSectionTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun trainingEditorGameSection_rendersGameMetaAndParsedMoves() {
        val game = createSampleGame()
        val parsedGame = createParsedSampleGame(game)

        setTrainingEditorGameSectionContent(
            state = createSectionState(
                game = game,
                parsedGame = parsedGame,
                isSelected = false
            )
        )

        composeRule.onNodeWithText("Italian Game").assertIsDisplayed()
        composeRule.onNodeWithText("C50").assertIsDisplayed()
        composeRule.onNodeWithText("3").assertIsDisplayed()
        composeRule.onNodeWithText("Move Sequence").assertIsDisplayed()
    }

    @Test
    fun trainingEditorGameSection_weightButtonsInvokeCallbacks() {
        var decreaseClicks = 0
        var increaseClicks = 0

        setTrainingEditorGameSectionContent(
            actions = createSectionActions(
                onDecreaseWeightClick = { decreaseClicks += 1 },
                onIncreaseWeightClick = { increaseClicks += 1 }
            )
        )

        composeRule.onNodeWithContentDescription("Decrease").performClick()
        composeRule.onNodeWithContentDescription("Increase").performClick()

        composeRule.runOnIdle {
            assertEquals(1, decreaseClicks)
            assertEquals(1, increaseClicks)
        }
    }

    @Test
    fun trainingEditorGameSection_primaryActionVisibleAndInvokesCallback() {
        var primaryActionClicks = 0

        setTrainingEditorGameSectionContent(
            primaryAction = TrainingEditorPrimaryAction(
                onClick = { primaryActionClicks += 1 },
                icon = Icons.Default.Add,
                contentDescription = "Start training"
            )
        )

        composeRule.onNodeWithContentDescription("Start training").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Start training").performClick()

        composeRule.runOnIdle {
            assertEquals(1, primaryActionClicks)
        }
    }

    @Test
    fun trainingEditorGameSection_hidesOptionalSectionsWhenDataIsMissing() {
        setTrainingEditorGameSectionContent(
            state = createSectionState(parsedGame = null, isSelected = false),
            primaryAction = null
        )

        composeRule.onAllNodesWithText("Move Sequence").assertCountEquals(0)
        composeRule.onAllNodesWithContentDescription("Start training").assertCountEquals(0)
    }

    @Test
    fun trainingEditorGameSection_clickingUnselectedCardInvokesOnSelect() {
        var selectClicks = 0

        setTrainingEditorGameSectionContent(
            state = createSectionState(isSelected = false),
            actions = createSectionActions(
                onSelect = { selectClicks += 1 }
            )
        )

        composeRule.onNodeWithTag(TrainingEditorGameCardTestTag).performClick()

        composeRule.runOnIdle {
            assertEquals(1, selectClicks)
        }
    }

    private fun setTrainingEditorGameSectionContent(
        state: TrainingEditorGameSectionState = createSectionState(),
        actions: TrainingEditorGameSectionActions = createSectionActions(),
        primaryAction: TrainingEditorPrimaryAction? = createPrimaryAction(),
    ) {
        composeRule.setContent {
            ChessBoardTheme {
                TrainingEditorGameSection(
                    state = state,
                    actions = actions,
                    primaryAction = primaryAction
                )
            }
        }
    }

    private fun createSectionState(
        game: TrainingGameEditorItem = createSampleGame(),
        parsedGame: ParsedTrainingEditorGame? = createParsedSampleGame(game),
        isSelected: Boolean = false,
        currentPly: Int = 0,
    ): TrainingEditorGameSectionState {
        return TrainingEditorGameSectionState(
            game = game,
            parsedGame = parsedGame,
            isSelected = isSelected,
            gameController = GameController(),
            currentPly = currentPly
        )
    }

    private fun createSectionActions(
        onDecreaseWeightClick: () -> Unit = {},
        onIncreaseWeightClick: () -> Unit = {},
        onSelect: () -> Unit = {},
        onPrevClick: () -> Unit = {},
        onNextClick: () -> Unit = {},
        onResetClick: () -> Unit = {},
        onEditGameClick: () -> Unit = {},
        onMovePlyClick: (Int) -> Unit = {},
    ): TrainingEditorGameSectionActions {
        return TrainingEditorGameSectionActions(
            onDecreaseWeightClick = onDecreaseWeightClick,
            onIncreaseWeightClick = onIncreaseWeightClick,
            onSelect = onSelect,
            onPrevClick = onPrevClick,
            onNextClick = onNextClick,
            onResetClick = onResetClick,
            onEditGameClick = onEditGameClick,
            onMovePlyClick = onMovePlyClick
        )
    }

    private fun createPrimaryAction(): TrainingEditorPrimaryAction {
        return TrainingEditorPrimaryAction(
            onClick = {},
            icon = Icons.Default.Add,
            contentDescription = "Start training"
        )
    }

    private fun createSampleGame(): TrainingGameEditorItem {
        return TrainingGameEditorItem(
            gameId = 1L,
            title = "Italian Game",
            weight = 3,
            eco = "C50",
            pgn = "1. e4 e5 2. Nf3 *"
        )
    }

    private fun createParsedSampleGame(
        game: TrainingGameEditorItem
    ): ParsedTrainingEditorGame {
        val uciMoves = parsePgnMoves(game.pgn)
        val moveLabels = buildMoveLabels(uciMoves)

        return ParsedTrainingEditorGame(
            uciMoves = uciMoves,
            moveLabels = moveLabels
        )
    }
}
