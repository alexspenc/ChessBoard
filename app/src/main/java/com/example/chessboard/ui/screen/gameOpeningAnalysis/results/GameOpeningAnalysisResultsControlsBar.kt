@file:Suppress("FunctionName")

package com.example.chessboard.ui.screen.gameOpeningAnalysis.results

/*
 * File role: renders bottom action controls for the game-opening analysis results screen.
 * Allowed here:
 * - result-screen bottom action bar items and their local visual state
 * Not allowed here:
 * - imported-game board navigation controls, export execution, dialogs, or runtime-context mutation
 * Validation date: 2026-07-02
 */

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R
import com.example.chessboard.ui.GameOpeningAnalysisDeleteResultGamesTestTag
import com.example.chessboard.ui.GameOpeningAnalysisSaveResultGamesTestTag
import com.example.chessboard.ui.components.BoardActionNavigationBar
import com.example.chessboard.ui.components.BoardActionNavigationItem
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.theme.BottomBarContentColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingErrorRed

@Composable
internal fun GameOpeningAnalysisResultsControlsBar(
    canSaveResultGames: Boolean,
    canDeleteResultGames: Boolean,
    onSaveResultGamesClick: () -> Unit,
    onDeleteResultGamesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoardActionNavigationBar(
        modifier = modifier,
        maxVisibleItems = 2,
        items =
            listOf(
                BoardActionNavigationItem(
                    label = stringResource(R.string.common_save),
                    selected = true,
                    enabled = canSaveResultGames,
                    modifier = Modifier.testTag(GameOpeningAnalysisSaveResultGamesTestTag),
                    onClick = onSaveResultGamesClick,
                ) {
                    IconMd(
                        imageVector = Icons.Default.Save,
                        contentDescription =
                            stringResource(
                                R.string.game_opening_analysis_save_result_games_content_description,
                            ),
                        tint = resolveSaveResultGamesTint(canSaveResultGames),
                    )
                },
                BoardActionNavigationItem(
                    label = stringResource(R.string.common_delete),
                    enabled = canDeleteResultGames,
                    modifier = Modifier.testTag(GameOpeningAnalysisDeleteResultGamesTestTag),
                    onClick = onDeleteResultGamesClick,
                ) {
                    IconMd(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription =
                            stringResource(
                                R.string.game_opening_analysis_delete_result_games_content_description,
                            ),
                        tint = resolveDeleteResultGamesTint(canDeleteResultGames),
                    )
                },
            ),
    )
}

private fun resolveSaveResultGamesTint(enabled: Boolean): Color {
    if (enabled) {
        return TrainingAccentTeal
    }

    return BottomBarContentColor.copy(alpha = 0.5f)
}

private fun resolveDeleteResultGamesTint(enabled: Boolean): Color {
    if (enabled) {
        return TrainingErrorRed
    }

    return TrainingErrorRed.copy(alpha = 0.5f)
}
