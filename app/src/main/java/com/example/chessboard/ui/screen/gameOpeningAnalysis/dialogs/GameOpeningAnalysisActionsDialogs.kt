@file:Suppress("FunctionName")

package com.example.chessboard.ui.screen.gameOpeningAnalysis.dialogs

/*
 * File role: renders game-opening analysis action and delete confirmation dialogs.
 * Allowed here:
 * - screen-specific menu/action dialogs for imported games
 * - delete confirmations for one selected game, current filtered game set, or analysis-result game set
 * Not allowed here:
 * - changing runtime-context state, running import/export/analysis jobs, or rendering main screen content
 * Validation date: 2026-07-02
 */

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.chessboard.R
import com.example.chessboard.runtimecontext.ImportedGameItem
import com.example.chessboard.ui.GameOpeningAnalysisDeleteFilteredGamesConfirmTestTag
import com.example.chessboard.ui.GameOpeningAnalysisDeleteFilteredGamesTestTag
import com.example.chessboard.ui.GameOpeningAnalysisDeleteGameConfirmTestTag
import com.example.chessboard.ui.GameOpeningAnalysisDeleteResultGamesConfirmTestTag
import com.example.chessboard.ui.GameOpeningAnalysisSaveFilteredGamesTestTag
import com.example.chessboard.ui.components.AppConfirmDialog
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.screen.gameOpeningAnalysis.eventTitle
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingErrorRed

internal data class GameOpeningAnalysisDialogAction(
    val canUse: Boolean,
    val onClick: () -> Unit,
)

@Composable
internal fun DeleteImportedGameDialog(
    selectedGame: ImportedGameItem?,
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val game = selectedGame ?: return
    if (!visible) {
        return
    }

    AppConfirmDialog(
        title = stringResource(R.string.game_opening_analysis_delete_game_title),
        message =
            stringResource(
                R.string.game_opening_analysis_delete_game_message,
                game.eventTitle(stringResource(R.string.game_opening_analysis_unknown_event)),
            ),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        confirmText = stringResource(R.string.common_delete),
        confirmButtonModifier = Modifier.testTag(GameOpeningAnalysisDeleteGameConfirmTestTag),
        isDestructive = true,
    )
}

@Composable
internal fun GameOpeningAnalysisActionsDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    saveFilteredGamesAction: GameOpeningAnalysisDialogAction,
    deleteFilteredGamesAction: GameOpeningAnalysisDialogAction,
) {
    if (!visible) {
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Background.ScreenDark,
        title = {
            SectionTitleText(text = stringResource(R.string.game_opening_analysis_game_actions))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
            ) {
                GameOpeningAnalysisDialogActionRow(
                    label = stringResource(R.string.game_opening_analysis_save_filtered_games_action),
                    action = saveFilteredGamesAction,
                    isDestructive = false,
                    testTag = GameOpeningAnalysisSaveFilteredGamesTestTag,
                ) { tint ->
                    IconMd(
                        imageVector = Icons.Default.Save,
                        contentDescription =
                            stringResource(
                                R.string.game_opening_analysis_save_filtered_games_content_description,
                            ),
                        tint = tint,
                    )
                }
                GameOpeningAnalysisDialogActionRow(
                    label = stringResource(R.string.game_opening_analysis_delete_filtered_games_action),
                    action = deleteFilteredGamesAction,
                    isDestructive = true,
                    testTag = GameOpeningAnalysisDeleteFilteredGamesTestTag,
                ) { tint ->
                    IconMd(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription =
                            stringResource(
                                R.string.game_opening_analysis_delete_filtered_games_content_description,
                            ),
                        tint = tint,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                CardMetaText(text = stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun GameOpeningAnalysisDialogActionRow(
    label: String,
    action: GameOpeningAnalysisDialogAction,
    isDestructive: Boolean,
    testTag: String,
    icon: @Composable (Color) -> Unit,
) {
    val actionTint =
        resolveGameOpeningAnalysisDialogActionTint(
            isEnabled = action.canUse,
            isDestructive = isDestructive,
        )

    TextButton(
        onClick = action.onClick,
        enabled = action.canUse,
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(testTag),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon(actionTint)
            Text(
                text = label,
                color = actionTint,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
internal fun DeleteFilteredImportedGamesDialog(
    visible: Boolean,
    gamesCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!visible || gamesCount <= 0) {
        return
    }

    AppConfirmDialog(
        title = stringResource(R.string.game_opening_analysis_delete_filtered_games_title),
        message =
            pluralStringResource(
                R.plurals.game_opening_analysis_delete_filtered_games_message,
                gamesCount,
                gamesCount,
            ),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        confirmText = stringResource(R.string.common_delete),
        confirmButtonModifier = Modifier.testTag(GameOpeningAnalysisDeleteFilteredGamesConfirmTestTag),
        isDestructive = true,
    )
}

@Composable
internal fun DeleteAnalysisResultGamesDialog(
    visible: Boolean,
    gamesCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!visible || gamesCount <= 0) {
        return
    }

    AppConfirmDialog(
        title = stringResource(R.string.game_opening_analysis_delete_result_games_title),
        message =
            pluralStringResource(
                R.plurals.game_opening_analysis_delete_result_games_message,
                gamesCount,
                gamesCount,
            ),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        confirmText = stringResource(R.string.common_delete),
        confirmButtonModifier = Modifier.testTag(GameOpeningAnalysisDeleteResultGamesConfirmTestTag),
        isDestructive = true,
    )
}

private fun resolveGameOpeningAnalysisDialogActionTint(
    isEnabled: Boolean,
    isDestructive: Boolean,
): Color {
    if (!isEnabled) {
        return TextColor.Primary.copy(alpha = 0.5f)
    }

    if (isDestructive) {
        return TrainingErrorRed
    }

    return TextColor.Primary
}
