@file:Suppress("FunctionName")

package com.example.chessboard.ui.screen.gameOpeningAnalysis.results

/*
 * File role: renders the game-opening analysis results list for the analysis screen.
 * Allowed here:
 * - screen-specific result list cards, selected-result preview, short result summaries, and result selection/deletion callbacks
 * - read-only board preview for the selected analysis result position
 * Not allowed here:
 * - analyzer execution, detail-screen rendering, database access, or PGN parsing
 * Validation date: 2026-07-02
 */

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.runtimecontext.GameOpeningAnalysisRuntimeContext
import com.example.chessboard.runtimecontext.ImportedGameAnalysisResult
import com.example.chessboard.ui.GameOpeningAnalysisResultDetailActionTestTag
import com.example.chessboard.ui.GameOpeningAnalysisResultDeleteActionTestTag
import com.example.chessboard.ui.GameOpeningAnalysisResultListTestTag
import com.example.chessboard.ui.GameOpeningAnalysisResultPreviewBoardTestTag
import com.example.chessboard.ui.GameOpeningAnalysisResultPreviewTestTag
import com.example.chessboard.ui.GameOpeningAnalysisResultsContentTestTag
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.ChessBoardSection
import com.example.chessboard.ui.components.DeleteIconButton
import com.example.chessboard.ui.components.SecondaryButton
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.BottomBarContentColor
import com.example.chessboard.ui.theme.TextColor

@Composable
internal fun GameOpeningAnalysisResultsContent(
    runtimeContext: GameOpeningAnalysisRuntimeContext,
    modifier: Modifier = Modifier,
) {
    val visibleResults = runtimeContext.visibleResults()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(AppDimens.spaceLg)
                .testTag(GameOpeningAnalysisResultsContentTestTag),
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
    ) {
        BodySecondaryText(
            text = stringResource(R.string.game_opening_analysis_results_hint),
            color = TextColor.Secondary,
        )

        visibleResults.forEach { analysisResult ->
            if (analysisResult.gameId == runtimeContext.selectedResultGameId) {
                GameOpeningAnalysisResultPreview(
                    analysisResult = analysisResult,
                    onOpenDetailClick = { runtimeContext.openSelectedResultDetail() },
                    onDeleteClick = {
                        runtimeContext.selectNextResult(analysisResult.gameId)
                        runtimeContext.selectGame(analysisResult.gameId)
                        runtimeContext.deleteSelectedGame()
                    },
                )
                return@forEach
            }

            GameOpeningAnalysisResultCard(
                analysisResult = analysisResult,
                onClick = { runtimeContext.selectResult(analysisResult.gameId) },
            )
        }
    }
}

@Composable
private fun GameOpeningAnalysisResultCard(
    analysisResult: ImportedGameAnalysisResult,
    onClick: () -> Unit,
) {
    val game = analysisResult.game
    val unknownEvent = stringResource(R.string.game_opening_analysis_unknown_event)
    val unknownPlayer = stringResource(R.string.game_opening_analysis_unknown_player)

    CardSurface(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(GameOpeningAnalysisResultListTestTag),
        color = Background.CardDark,
        onClick = onClick,
    ) {
        SectionTitleText(text = game.displayEvent(unknownEvent))
        CardMetaText(text = game.displayPlayers(unknownPlayer))
        CardMetaText(text = resultTypeLabel(analysisResult.result))
        CardMetaText(
            text = resultDetailText(analysisResult.result),
            color = TextColor.Secondary,
        )
    }
}

@Composable
private fun GameOpeningAnalysisResultPreview(
    analysisResult: ImportedGameAnalysisResult,
    onOpenDetailClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val result = analysisResult.result
    val game = analysisResult.game
    val unknownEvent = stringResource(R.string.game_opening_analysis_unknown_event)
    val unknownPlayer = stringResource(R.string.game_opening_analysis_unknown_player)
    val previewFen = resultPreviewFen(result)
    val lineController =
        remember(analysisResult.gameId, previewFen, result.selectedSide) {
            LineController(resolveResultBoardOrientation(result)).also { controller ->
                controller.setUserMovesEnabled(false)
                if (previewFen != null) {
                    controller.loadFromFen(previewFen)
                    controller.setUserMovesEnabled(false)
                }
            }
        }

    CardSurface(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(GameOpeningAnalysisResultPreviewTestTag),
        color = resolveResultCardColor(selected = true),
    ) {
        SectionTitleText(text = game.displayEvent(unknownEvent))
        CardMetaText(text = game.displayPlayers(unknownPlayer))
        CardMetaText(text = resultTypeLabel(result))
        CardMetaText(
            text = resultDetailText(result),
            color = TextColor.Secondary,
        )
        resultExtraDetailTexts(result).forEach { detail ->
            CardMetaText(
                text = detail,
                color = TextColor.Secondary,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SecondaryButton(
                text = stringResource(R.string.game_opening_analysis_result_details_action),
                onClick = onOpenDetailClick,
                modifier = Modifier.testTag(GameOpeningAnalysisResultDetailActionTestTag),
            )
            DeleteIconButton(
                onClick = onDeleteClick,
                modifier = Modifier.testTag(GameOpeningAnalysisResultDeleteActionTestTag),
            )
        }
        if (previewFen == null) {
            CardMetaText(
                text = stringResource(R.string.game_opening_analysis_result_no_board_preview),
                color = TextColor.Secondary,
            )
            return@CardSurface
        }

        ChessBoardSection(
            lineController = lineController,
            modifier = Modifier.padding(top = AppDimens.spaceMd),
            boardModifier = Modifier.testTag(GameOpeningAnalysisResultPreviewBoardTestTag),
        )
    }
}

private fun resolveResultCardColor(selected: Boolean): Color {
    if (selected) {
        return BottomBarContentColor.copy(alpha = 0.18f)
    }

    return Background.CardDark
}
