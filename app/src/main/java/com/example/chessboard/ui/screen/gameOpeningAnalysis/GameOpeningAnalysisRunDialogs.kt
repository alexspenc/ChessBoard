@file:Suppress("FunctionName")

package com.example.chessboard.ui.screen.gameOpeningAnalysis

/*
 * File role: renders game-opening analysis run dialogs for imported games.
 * Allowed here:
 * - screen-specific option controls for batch analysis
 * - blocking progress UI and cancel action for an active analysis run
 * Not allowed here:
 * - analyzer execution, database access, PGN parsing, or runtime result mutation
 * Validation date: 2026-06-29
 */

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R
import com.example.chessboard.runtimecontext.GameOpeningAnalysisOptions
import com.example.chessboard.runtimecontext.GameOpeningAnalysisProgress
import com.example.chessboard.ui.GameOpeningAnalysisCancelAnalysisTestTag
import com.example.chessboard.ui.GameOpeningAnalysisCancelImportTestTag
import com.example.chessboard.ui.GameOpeningAnalysisImportProgressDialogTestTag
import com.example.chessboard.ui.GameOpeningAnalysisMinimumKnownPrefixTestTag
import com.example.chessboard.ui.GameOpeningAnalysisOptionsAnalyzeTestTag
import com.example.chessboard.ui.GameOpeningAnalysisOptionsDialogTestTag
import com.example.chessboard.ui.GameOpeningAnalysisProgressDialogTestTag
import com.example.chessboard.ui.components.AppTextField
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal

internal data class GameOpeningAnalysisImportProgress(
    val processedCount: Int,
    val totalCount: Int,
    val parallelism: Int,
)

@Composable
internal fun GameOpeningAnalysisOptionsDialog(
    visible: Boolean,
    options: GameOpeningAnalysisOptions,
    onOptionsChange: (GameOpeningAnalysisOptions) -> Unit,
    onDismiss: () -> Unit,
    onAnalyzeClick: () -> Unit,
) {
    if (!visible) {
        return
    }

    fun updateMinimumKnownPrefix(input: String) {
        val digits = input.filter { character -> character.isDigit() }
        onOptionsChange(options.copy(minimumKnownPrefixPly = digits.toIntOrNull() ?: 0))
    }

    fun updateResultFilter(
        filter: GameOpeningAnalysisOptions.ResultFilter,
        checked: Boolean,
    ) {
        val nextFilters = options.resultTypes.toMutableSet()
        if (checked) {
            nextFilters.add(filter)
        } else {
            nextFilters.remove(filter)
        }
        onOptionsChange(options.copy(resultTypes = nextFilters))
    }

    AlertDialog(
        modifier = Modifier.testTag(GameOpeningAnalysisOptionsDialogTestTag),
        onDismissRequest = onDismiss,
        containerColor = Background.ScreenDark,
        title = {
            SectionTitleText(text = stringResource(R.string.game_opening_analysis_options_title))
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
            ) {
                AppTextField(
                    value = options.minimumKnownPrefixPly.toString(),
                    onValueChange = ::updateMinimumKnownPrefix,
                    label = stringResource(R.string.game_opening_analysis_minimum_known_prefix),
                    placeholder = stringResource(R.string.game_opening_analysis_minimum_known_prefix_placeholder),
                    inputTestTag = GameOpeningAnalysisMinimumKnownPrefixTestTag,
                )
                BodySecondaryText(
                    text = stringResource(R.string.game_opening_analysis_result_types_title),
                    color = TextColor.Primary,
                )
                GameOpeningAnalysisOptions.ResultFilter.entries.forEach { filter ->
                    ResultFilterRow(
                        label = resultFilterLabel(filter),
                        checked = filter in options.resultTypes,
                        onCheckedChange = { checked -> updateResultFilter(filter, checked) },
                    )
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = stringResource(R.string.game_opening_analysis_analyze_action),
                onClick = onAnalyzeClick,
                modifier = Modifier.testTag(GameOpeningAnalysisOptionsAnalyzeTestTag),
                enabled = options.resultTypes.isNotEmpty(),
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                CardMetaText(text = stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
internal fun GameOpeningAnalysisImportProgressDialog(
    progress: GameOpeningAnalysisImportProgress?,
    onCancel: () -> Unit,
) {
    val currentProgress = progress ?: return
    var progressText = stringResource(R.string.game_opening_analysis_import_preparing_message)
    if (currentProgress.totalCount != 0) {
        progressText =
            stringResource(
                R.string.game_opening_analysis_import_progress_message,
                currentProgress.processedCount,
                currentProgress.totalCount,
            )
    }

    AlertDialog(
        modifier = Modifier.testTag(GameOpeningAnalysisImportProgressDialogTestTag),
        onDismissRequest = {},
        containerColor = Background.ScreenDark,
        title = {
            SectionTitleText(text = stringResource(R.string.game_opening_analysis_import_progress_title))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(color = TrainingAccentTeal)
                BodySecondaryText(
                    text = progressText,
                    color = TextColor.Primary,
                )
                CardMetaText(
                    text =
                        stringResource(
                            R.string.game_opening_analysis_import_parallelism,
                            currentProgress.parallelism,
                        ),
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.testTag(GameOpeningAnalysisCancelImportTestTag),
            ) {
                CardMetaText(text = stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
internal fun GameOpeningAnalysisProgressDialog(
    progress: GameOpeningAnalysisProgress?,
    onCancel: () -> Unit,
) {
    val currentProgress = progress ?: return
    val progressMessage =
        when (currentProgress.stage) {
            GameOpeningAnalysisProgress.Stage.BUILDING_BOOK -> {
                stringResource(R.string.game_opening_analysis_building_book_message)
            }
            GameOpeningAnalysisProgress.Stage.ANALYZING_GAMES -> {
                stringResource(
                    R.string.game_opening_analysis_progress_message,
                    currentProgress.analyzedCount,
                    currentProgress.totalCount,
                )
            }
        }

    AlertDialog(
        modifier = Modifier.testTag(GameOpeningAnalysisProgressDialogTestTag),
        onDismissRequest = {},
        containerColor = Background.ScreenDark,
        title = {
            SectionTitleText(text = stringResource(R.string.game_opening_analysis_progress_title))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(color = TrainingAccentTeal)
                BodySecondaryText(
                    text = progressMessage,
                    color = TextColor.Primary,
                )
                AnalysisParallelismText(currentProgress)
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.testTag(GameOpeningAnalysisCancelAnalysisTestTag),
            ) {
                CardMetaText(text = stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun AnalysisParallelismText(progress: GameOpeningAnalysisProgress) {
    val parallelism = progress.parallelism ?: return
    if (progress.stage != GameOpeningAnalysisProgress.Stage.ANALYZING_GAMES) {
        return
    }

    CardMetaText(
        text =
            stringResource(
                R.string.game_opening_analysis_parallelism,
                parallelism,
            ),
    )
}

@Composable
private fun ResultFilterRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BodySecondaryText(
            text = label,
            color = TextColor.Primary,
            modifier = Modifier.weight(1f),
        )
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun resultFilterLabel(filter: GameOpeningAnalysisOptions.ResultFilter): String =
    when (filter) {
        GameOpeningAnalysisOptions.ResultFilter.DEVIATION -> {
            stringResource(R.string.game_opening_analysis_result_deviation)
        }

        GameOpeningAnalysisOptions.ResultFilter.OPPONENT_LEFT_BOOK -> {
            stringResource(R.string.game_opening_analysis_result_opponent_left_book)
        }

        GameOpeningAnalysisOptions.ResultFilter.BOOK_TOO_SHORT -> {
            stringResource(R.string.game_opening_analysis_result_book_too_short)
        }

        GameOpeningAnalysisOptions.ResultFilter.MATCHES_KNOWN_OPENING -> {
            stringResource(R.string.game_opening_analysis_result_matches_known_opening)
        }

        GameOpeningAnalysisOptions.ResultFilter.NO_MATCHING_OPENING -> {
            stringResource(R.string.game_opening_analysis_result_no_matching_opening)
        }

        GameOpeningAnalysisOptions.ResultFilter.INVALID_GAMES -> {
            stringResource(R.string.game_opening_analysis_result_invalid_games)
        }
    }
