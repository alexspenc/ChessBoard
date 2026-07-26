@file:Suppress("FunctionName")

package com.example.chessboard.ui.screen.gameOpeningAnalysis.results

/*
 * File role: renders the detail view for one selected game-opening analysis result.
 * Allowed here:
 * - selected-result detail layout, read-only board preview, result metadata, detail actions, and known continuation summaries
 * - UI-only extraction of expected continuations already stored in the analysis result model
 * Not allowed here:
 * - analyzer execution, database access, result mutation, PGN parsing, or imported-game list rendering
 * Validation date: 2026-06-28
 */

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R
import com.example.chessboard.analysis.GameOpeningAnalysisResult
import com.example.chessboard.analysis.GameOpeningBookTooShort
import com.example.chessboard.analysis.GameOpeningDeviation
import com.example.chessboard.analysis.GameOpeningExpectedMove
import com.example.chessboard.analysis.GameOpeningInvalidGameMove
import com.example.chessboard.analysis.GameOpeningInvalidInitialPosition
import com.example.chessboard.analysis.GameOpeningMatchesKnownOpening
import com.example.chessboard.analysis.GameOpeningNoMatchingOpening
import com.example.chessboard.analysis.GameOpeningOpponentLeftBook
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.runtimecontext.ImportedGameAnalysisResult
import com.example.chessboard.runtimecontext.ImportedGameItem
import com.example.chessboard.ui.GameOpeningAnalysisRecordDeviationMistakeTestTag
import com.example.chessboard.ui.GameOpeningAnalysisResultDetailBoardTestTag
import com.example.chessboard.ui.GameOpeningAnalysisResultDetailContentTestTag
import com.example.chessboard.ui.GameOpeningAnalysisResultCopyPgnActionTestTag
import com.example.chessboard.ui.GameOpeningAnalysisResultDeleteActionTestTag
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.ChessBoardSection
import com.example.chessboard.ui.components.DeleteIconButton
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.components.SecondaryButton
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TextColor

@Composable
internal fun GameOpeningAnalysisResultDetailContent(
    analysisResult: ImportedGameAnalysisResult?,
    onRecordDeviationMistakeClick: (Long, List<Long>) -> Unit,
    onCopyGamePgnClick: (ImportedGameItem) -> Unit,
    onDeleteClick: (Long) -> Unit,
    copyGamePgnEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(AppDimens.spaceLg)
                .testTag(GameOpeningAnalysisResultDetailContentTestTag),
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
    ) {
        if (analysisResult == null) {
            BodySecondaryText(
                text = stringResource(R.string.game_opening_analysis_result_detail_missing),
                color = TextColor.Secondary,
            )
            return@Column
        }

        GameOpeningAnalysisResultGameCard(
            analysisResult = analysisResult,
            onCopyGamePgnClick = onCopyGamePgnClick,
            onDeleteClick = onDeleteClick,
            copyGamePgnEnabled = copyGamePgnEnabled,
        )
        GameOpeningAnalysisResultBoardCard(analysisResult)
        GameOpeningAnalysisResultSummaryCard(
            gameId = analysisResult.gameId,
            result = analysisResult.result,
            onRecordDeviationMistakeClick = onRecordDeviationMistakeClick,
        )
        GameOpeningAnalysisContinuationCard(analysisResult.result)
    }
}

@Composable
private fun GameOpeningAnalysisResultGameCard(
    analysisResult: ImportedGameAnalysisResult,
    onCopyGamePgnClick: (ImportedGameItem) -> Unit,
    onDeleteClick: (Long) -> Unit,
    copyGamePgnEnabled: Boolean,
) {
    val unknownEvent = stringResource(R.string.game_opening_analysis_unknown_event)
    val unknownPlayer = stringResource(R.string.game_opening_analysis_unknown_player)

    CardSurface(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitleText(
                text = stringResource(R.string.game_opening_analysis_result_detail_game_title),
                modifier = Modifier.weight(1f),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onCopyGamePgnClick(analysisResult.game) },
                    enabled = copyGamePgnEnabled,
                    modifier = Modifier.testTag(GameOpeningAnalysisResultCopyPgnActionTestTag),
                ) {
                    IconMd(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription =
                            stringResource(R.string.game_opening_analysis_copy_game_pgn_content_description),
                    )
                }
                DeleteIconButton(
                    onClick = { onDeleteClick(analysisResult.gameId) },
                    modifier = Modifier.testTag(GameOpeningAnalysisResultDeleteActionTestTag),
                )
            }
        }
        CardMetaText(text = analysisResult.game.displayEvent(unknownEvent))
        BodySecondaryText(
            text = analysisResult.game.displayPlayers(unknownPlayer),
            color = TextColor.Secondary,
        )
    }
}

@Composable
private fun GameOpeningAnalysisResultBoardCard(analysisResult: ImportedGameAnalysisResult) {
    val result = analysisResult.result
    val previewFen = resultPreviewFen(result)
    val lineController =
        remember(analysisResult.gameId, previewFen, result.selectedSide) {
            createAnalysisResultBoardController(result)
        }

    CardSurface(modifier = Modifier.fillMaxWidth()) {
        SectionTitleText(text = stringResource(R.string.game_opening_analysis_result_detail_position_title))
        if (previewFen == null) {
            BodySecondaryText(
                text = stringResource(R.string.game_opening_analysis_result_no_board_preview),
                color = TextColor.Secondary,
            )
            return@CardSurface
        }

        ChessBoardSection(
            lineController = lineController,
            modifier = Modifier.padding(top = AppDimens.spaceMd),
            boardModifier = Modifier.testTag(GameOpeningAnalysisResultDetailBoardTestTag),
        )
    }
}

internal fun createAnalysisResultBoardController(result: GameOpeningAnalysisResult): LineController {
    val previewFen = resultPreviewFen(result)
    return LineController(resolveResultBoardOrientation(result)).also { controller ->
        controller.setUserMovesEnabled(false)
        if (previewFen != null) {
            controller.loadFromFen(previewFen)
            controller.setUserMovesEnabled(false)
        }
    }
}

@Composable
private fun GameOpeningAnalysisResultSummaryCard(
    gameId: Long,
    result: GameOpeningAnalysisResult,
    onRecordDeviationMistakeClick: (Long, List<Long>) -> Unit,
) {
    CardSurface(modifier = Modifier.fillMaxWidth()) {
        SectionTitleText(text = stringResource(R.string.game_opening_analysis_result_detail_result_title))
        CardMetaText(text = resultTypeLabel(result))
        BodySecondaryText(
            text = resultDetailText(result),
            color = TextColor.Secondary,
        )
        resultExtraDetailTexts(result).forEach { detail ->
            BodySecondaryText(
                text = detail,
                color = TextColor.Secondary,
            )
        }
        val affectedLineIds = deviationMistakeLineIds(result)
        if (affectedLineIds.isNotEmpty()) {
            SecondaryButton(
                text = stringResource(R.string.game_opening_analysis_record_deviation_mistake_action),
                onClick = { onRecordDeviationMistakeClick(gameId, affectedLineIds) },
                modifier = Modifier.testTag(GameOpeningAnalysisRecordDeviationMistakeTestTag),
            )
        }
    }
}

@Composable
private fun GameOpeningAnalysisContinuationCard(result: GameOpeningAnalysisResult) {
    val continuations = resultExpectedMoves(result)

    CardSurface(modifier = Modifier.fillMaxWidth()) {
        SectionTitleText(text = stringResource(R.string.game_opening_analysis_result_detail_continuations_title))
        if (continuations.isEmpty()) {
            BodySecondaryText(
                text = stringResource(R.string.game_opening_analysis_result_detail_no_continuations),
                color = TextColor.Secondary,
            )
            return@CardSurface
        }

        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
            continuations.forEach { continuation ->
                GameOpeningAnalysisContinuationRow(continuation)
            }
        }
    }
}

@Composable
private fun GameOpeningAnalysisContinuationRow(move: GameOpeningExpectedMove) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BodySecondaryText(
            text = stringResource(R.string.game_opening_analysis_result_detail_continuation_move, move.moveUci),
            color = TextColor.Primary,
        )
        CardMetaText(
            text =
                stringResource(
                    R.string.game_opening_analysis_result_detail_continuation_lines,
                    move.lineRefs.size,
                ),
            color = TextColor.Secondary,
        )
    }
}

private fun resultExpectedMoves(result: GameOpeningAnalysisResult): List<GameOpeningExpectedMove> {
    when (result) {
        is GameOpeningDeviation -> return result.expectedMoves

        is GameOpeningOpponentLeftBook -> return result.expectedMoves

        is GameOpeningNoMatchingOpening -> return result.knownMoves

        is GameOpeningBookTooShort,
        is GameOpeningMatchesKnownOpening,
        is GameOpeningInvalidGameMove,
        is GameOpeningInvalidInitialPosition,
        -> return emptyList()
    }
}

private fun deviationMistakeLineIds(result: GameOpeningAnalysisResult): List<Long> {
    if (result !is GameOpeningDeviation) {
        return emptyList()
    }

    return result.matchingLineRefs.mapNotNull { ref -> ref.stableLineId }.distinct()
}
