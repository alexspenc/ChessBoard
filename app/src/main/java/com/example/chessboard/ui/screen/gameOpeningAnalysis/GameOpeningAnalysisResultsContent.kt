@file:Suppress("FunctionName")

package com.example.chessboard.ui.screen.gameOpeningAnalysis

/*
 * File role: renders the game-opening analysis results list for the analysis screen.
 * Allowed here:
 * - screen-specific result list cards, selected-result preview, short result summaries, paging controls, and result selection callbacks
 * - read-only board preview for the selected analysis result position
 * Not allowed here:
 * - analyzer execution, result mutation, detail-screen rendering, database access, or PGN parsing
 * Validation date: 2026-06-27
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.chessboard.ui.GameOpeningAnalysisNextResultsPageTestTag
import com.example.chessboard.ui.GameOpeningAnalysisPreviousResultsPageTestTag
import com.example.chessboard.ui.GameOpeningAnalysisResultDetailActionTestTag
import com.example.chessboard.ui.GameOpeningAnalysisResultListTestTag
import com.example.chessboard.ui.GameOpeningAnalysisResultPreviewBoardTestTag
import com.example.chessboard.ui.GameOpeningAnalysisResultPreviewTestTag
import com.example.chessboard.ui.GameOpeningAnalysisResultsContentTestTag
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.ChessBoardSection
import com.example.chessboard.ui.components.IconMd
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
                )
                return@forEach
            }

            GameOpeningAnalysisResultCard(
                analysisResult = analysisResult,
                onClick = { runtimeContext.selectResult(analysisResult.gameId) },
            )
        }

        GameOpeningAnalysisResultsPagingControls(
            canOpenPreviousPage = runtimeContext.canOpenPreviousResultsPage(),
            canOpenNextPage = runtimeContext.canOpenNextResultsPage(),
            onOpenPreviousPageClick = { runtimeContext.openPreviousResultsPage() },
            onOpenNextPageClick = { runtimeContext.openNextResultsPage() },
        )
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
        BodySecondaryText(
            text = resultDetailText(analysisResult.result),
            color = TextColor.Secondary,
        )
    }
}

@Composable
private fun GameOpeningAnalysisResultPreview(
    analysisResult: ImportedGameAnalysisResult,
    onOpenDetailClick: () -> Unit,
) {
    val result = analysisResult.result
    val game = analysisResult.game
    val unknownEvent = stringResource(R.string.game_opening_analysis_unknown_event)
    val unknownPlayer = stringResource(R.string.game_opening_analysis_unknown_player)
    val previewFen = resultPreviewFen(result)
    val lineController = remember { LineController(resolveResultBoardOrientation(result)) }

    LaunchedEffect(previewFen, result.selectedSide) {
        lineController.setOrientation(resolveResultBoardOrientation(result))
        lineController.setUserMovesEnabled(false)
        if (previewFen == null) {
            return@LaunchedEffect
        }

        lineController.loadFromFen(previewFen)
        lineController.setUserMovesEnabled(false)
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
        SecondaryButton(
            text = stringResource(R.string.game_opening_analysis_result_details_action),
            onClick = onOpenDetailClick,
            modifier = Modifier.testTag(GameOpeningAnalysisResultDetailActionTestTag),
        )
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
            boardModifier = Modifier.testTag(GameOpeningAnalysisResultPreviewBoardTestTag),
        )
    }
}

@Composable
private fun GameOpeningAnalysisResultsPagingControls(
    canOpenPreviousPage: Boolean,
    canOpenNextPage: Boolean,
    onOpenPreviousPageClick: () -> Unit,
    onOpenNextPageClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onOpenPreviousPageClick,
            enabled = canOpenPreviousPage,
            modifier = Modifier.testTag(GameOpeningAnalysisPreviousResultsPageTestTag),
        ) {
            IconMd(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.common_previous),
                tint = resolvePageArrowTint(canOpenPreviousPage),
            )
        }
        IconButton(
            onClick = onOpenNextPageClick,
            enabled = canOpenNextPage,
            modifier = Modifier.testTag(GameOpeningAnalysisNextResultsPageTestTag),
        ) {
            IconMd(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.common_next),
                tint = resolvePageArrowTint(canOpenNextPage),
            )
        }
    }
}

private fun resolveResultCardColor(selected: Boolean): Color {
    if (selected) {
        return BottomBarContentColor.copy(alpha = 0.18f)
    }

    return Background.CardDark
}

private fun resolvePageArrowTint(enabled: Boolean): Color {
    if (enabled) {
        return BottomBarContentColor
    }

    return TextColor.Secondary
}
