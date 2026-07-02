@file:Suppress("FunctionName")

package com.example.chessboard.ui.screen.gameOpeningAnalysis

/*
 * File role: renders the bottom board-controls bar for the game-opening analysis screen.
 * Allowed here:
 * - bottom action bar items for import, delete, analyze, menu, and move navigation
 * - visual helpers used only by that board-controls bar
 * Not allowed here:
 * - top-bar pagination, dialogs, import/export/analysis execution, or runtime-context mutation
 * Validation date: 2026-06-30
 */

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R
import com.example.chessboard.ui.GameOpeningAnalysisAddGamesTestTag
import com.example.chessboard.ui.GameOpeningAnalysisAnalyzeActionTestTag
import com.example.chessboard.ui.GameOpeningAnalysisDeleteGameTestTag
import com.example.chessboard.ui.GameOpeningAnalysisGameActionsTestTag
import com.example.chessboard.ui.GameOpeningAnalysisNextMoveTestTag
import com.example.chessboard.ui.GameOpeningAnalysisPreviousMoveTestTag
import com.example.chessboard.ui.components.BoardActionNavigationBar
import com.example.chessboard.ui.components.BoardActionNavigationItem
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.theme.BottomBarContentColor
import com.example.chessboard.ui.theme.TrainingAccentTeal

internal data class GameOpeningAnalysisBoardControls(
    val state: GameOpeningAnalysisBoardControlsState,
    val actions: GameOpeningAnalysisBoardControlsActions,
)

internal data class GameOpeningAnalysisBoardControlsState(
    val hasImportedGames: Boolean,
    val canUndo: Boolean,
    val canRedo: Boolean,
    val canAnalyze: Boolean,
    val canDeleteGame: Boolean,
    val hasGameActions: Boolean,
)

internal data class GameOpeningAnalysisBoardControlsActions(
    val onPreviousMoveClick: () -> Unit,
    val onNextMoveClick: () -> Unit,
    val onAddGamesClick: () -> Unit,
    val onDeleteGameClick: () -> Unit,
    val onGameActionsClick: () -> Unit,
    val onAnalyzeClick: () -> Unit,
)

internal fun gameOpeningAnalysisBoardControls(
    hasImportedGames: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    canAnalyze: Boolean,
    canDeleteGame: Boolean,
    hasGameActions: Boolean,
    onPreviousMoveClick: () -> Unit,
    onNextMoveClick: () -> Unit,
    onAddGamesClick: () -> Unit,
    onDeleteGameClick: () -> Unit,
    onGameActionsClick: () -> Unit,
    onAnalyzeClick: () -> Unit,
): GameOpeningAnalysisBoardControls {
    return GameOpeningAnalysisBoardControls(
        state =
            GameOpeningAnalysisBoardControlsState(
                hasImportedGames = hasImportedGames,
                canUndo = canUndo,
                canRedo = canRedo,
                canAnalyze = canAnalyze,
                canDeleteGame = canDeleteGame,
                hasGameActions = hasGameActions,
            ),
        actions =
            GameOpeningAnalysisBoardControlsActions(
                onPreviousMoveClick = onPreviousMoveClick,
                onNextMoveClick = onNextMoveClick,
                onAddGamesClick = onAddGamesClick,
                onDeleteGameClick = onDeleteGameClick,
                onGameActionsClick = onGameActionsClick,
                onAnalyzeClick = onAnalyzeClick,
            ),
    )
}

@Composable
internal fun GameOpeningAnalysisBoardControlsBar(
    controls: GameOpeningAnalysisBoardControls,
    modifier: Modifier = Modifier,
) {
    val state = controls.state
    val actions = controls.actions
    val items =
        buildList {
            add(
                BoardActionNavigationItem(
                    label = stringResource(R.string.game_opening_analysis_add_games_action),
                    selected = true,
                    modifier = Modifier.testTag(GameOpeningAnalysisAddGamesTestTag),
                    onClick = actions.onAddGamesClick,
                ) {
                    IconMd(
                        imageVector = Icons.Default.Add,
                        contentDescription =
                            stringResource(
                                R.string.game_opening_analysis_add_games_content_description,
                            ),
                        tint = TrainingAccentTeal,
                    )
                },
            )
            if (!state.hasImportedGames) {
                return@buildList
            }

            add(
                BoardActionNavigationItem(
                    label = stringResource(R.string.common_delete),
                    enabled = state.canDeleteGame,
                    modifier = Modifier.testTag(GameOpeningAnalysisDeleteGameTestTag),
                    onClick = actions.onDeleteGameClick,
                ) {
                    IconMd(
                        imageVector = Icons.Default.Delete,
                        contentDescription =
                            stringResource(
                                R.string.game_opening_analysis_delete_game_content_description,
                            ),
                        tint = resolveMoveControlTint(state.canDeleteGame),
                    )
                },
            )
            add(
                BoardActionNavigationItem(
                    label = stringResource(R.string.game_opening_analysis_analyze_action),
                    selected = true,
                    enabled = state.canAnalyze,
                    modifier = Modifier.testTag(GameOpeningAnalysisAnalyzeActionTestTag),
                    onClick = actions.onAnalyzeClick,
                ) {
                    IconMd(
                        imageVector = Icons.Default.Biotech,
                        contentDescription = stringResource(R.string.game_opening_analysis_analyze_action),
                        tint = resolveMoveControlTint(state.canAnalyze),
                    )
                },
            )
            add(
                BoardActionNavigationItem(
                    label = stringResource(R.string.common_menu),
                    enabled = state.hasGameActions,
                    modifier = Modifier.testTag(GameOpeningAnalysisGameActionsTestTag),
                    onClick = actions.onGameActionsClick,
                ) {
                    IconMd(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(R.string.game_opening_analysis_game_actions),
                        tint = resolveMoveControlTint(state.hasGameActions),
                    )
                },
            )
            add(
                BoardActionNavigationItem(
                    label = stringResource(R.string.common_back),
                    enabled = state.canUndo,
                    modifier = Modifier.testTag(GameOpeningAnalysisPreviousMoveTestTag),
                    onClick = actions.onPreviousMoveClick,
                ) {
                    IconMd(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription =
                            stringResource(
                                R.string.game_opening_analysis_previous_move_content_description,
                            ),
                        tint = resolveMoveControlTint(state.canUndo),
                    )
                },
            )
            add(
                BoardActionNavigationItem(
                    label = stringResource(R.string.common_forward),
                    enabled = state.canRedo,
                    modifier = Modifier.testTag(GameOpeningAnalysisNextMoveTestTag),
                    onClick = actions.onNextMoveClick,
                ) {
                    IconMd(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription =
                            stringResource(
                                R.string.game_opening_analysis_next_move_content_description,
                            ),
                        tint = resolveMoveControlTint(state.canRedo),
                    )
                },
            )
        }
    BoardActionNavigationBar(
        modifier = modifier,
        maxVisibleItems = 6,
        items = items,
    )
}

private fun resolveMoveControlTint(enabled: Boolean): Color {
    if (enabled) {
        return BottomBarContentColor
    }

    return BottomBarContentColor.copy(alpha = 0.5f)
}
