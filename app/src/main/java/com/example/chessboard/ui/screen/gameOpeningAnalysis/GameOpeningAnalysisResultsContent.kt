@file:Suppress("FunctionName")

package com.example.chessboard.ui.screen.gameOpeningAnalysis

/*
 * File role: renders the game-opening analysis results list for the analysis screen.
 * Allowed here:
 * - screen-specific result list cards, short result summaries, paging controls, and result selection callbacks
 * Not allowed here:
 * - analyzer execution, result mutation, board/detail preview rendering, database access, or PGN parsing
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R
import com.example.chessboard.analysis.GameOpeningAnalysisResult
import com.example.chessboard.analysis.GameOpeningBookTooShort
import com.example.chessboard.analysis.GameOpeningDeviation
import com.example.chessboard.analysis.GameOpeningInvalidGameMove
import com.example.chessboard.analysis.GameOpeningInvalidInitialPosition
import com.example.chessboard.analysis.GameOpeningMatchesKnownOpening
import com.example.chessboard.analysis.GameOpeningNoMatchingOpening
import com.example.chessboard.analysis.GameOpeningOpponentLeftBook
import com.example.chessboard.runtimecontext.GameOpeningAnalysisRuntimeContext
import com.example.chessboard.runtimecontext.ImportedGameAnalysisResult
import com.example.chessboard.runtimecontext.ImportedGameItem
import com.example.chessboard.ui.GameOpeningAnalysisNextResultsPageTestTag
import com.example.chessboard.ui.GameOpeningAnalysisPreviousResultsPageTestTag
import com.example.chessboard.ui.GameOpeningAnalysisResultListTestTag
import com.example.chessboard.ui.GameOpeningAnalysisResultsContentTestTag
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.IconMd
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
            GameOpeningAnalysisResultCard(
                analysisResult = analysisResult,
                selected = analysisResult.gameId == runtimeContext.selectedResultGameId,
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
    selected: Boolean,
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
        color = resolveResultCardColor(selected),
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

@Composable
private fun resultTypeLabel(result: GameOpeningAnalysisResult): String =
    when (result) {
        is GameOpeningDeviation -> stringResource(R.string.game_opening_analysis_result_deviation)
        is GameOpeningOpponentLeftBook -> stringResource(R.string.game_opening_analysis_result_opponent_left_book)
        is GameOpeningBookTooShort -> stringResource(R.string.game_opening_analysis_result_book_too_short)
        is GameOpeningMatchesKnownOpening -> stringResource(R.string.game_opening_analysis_result_matches_known_opening)
        is GameOpeningNoMatchingOpening -> stringResource(R.string.game_opening_analysis_result_no_matching_opening)
        is GameOpeningInvalidGameMove -> stringResource(R.string.game_opening_analysis_result_invalid_game_move)
        is GameOpeningInvalidInitialPosition -> stringResource(R.string.game_opening_analysis_result_invalid_initial_position)
    }

@Composable
private fun resultDetailText(result: GameOpeningAnalysisResult): String =
    when (result) {
        is GameOpeningDeviation -> {
            stringResource(R.string.game_opening_analysis_result_ply, result.ply)
        }

        is GameOpeningOpponentLeftBook -> {
            stringResource(R.string.game_opening_analysis_result_ply, result.ply)
        }

        is GameOpeningBookTooShort -> {
            stringResource(R.string.game_opening_analysis_result_matched_ply, result.matchedPly)
        }

        is GameOpeningMatchesKnownOpening -> {
            stringResource(R.string.game_opening_analysis_result_matched_ply, result.matchedPly)
        }

        is GameOpeningNoMatchingOpening -> {
            stringResource(R.string.game_opening_analysis_result_ply, result.ply)
        }

        is GameOpeningInvalidGameMove -> {
            stringResource(
                R.string.game_opening_analysis_result_invalid_move_detail,
                result.moveUci,
                result.reason.name,
            )
        }

        is GameOpeningInvalidInitialPosition -> {
            stringResource(R.string.game_opening_analysis_result_invalid_initial_position_detail)
        }
    }

private fun ImportedGameItem.displayEvent(unknownEvent: String): String {
    val event = headers[EVENT_HEADER]
    if (!event.isNullOrBlank()) {
        return event
    }

    return unknownEvent
}

private fun ImportedGameItem.displayPlayers(unknownPlayer: String): String {
    val white = headers[WHITE_HEADER]?.takeIf { value -> value.isNotBlank() } ?: unknownPlayer
    val black = headers[BLACK_HEADER]?.takeIf { value -> value.isNotBlank() } ?: unknownPlayer
    return "$white - $black"
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

private const val EVENT_HEADER = "Event"
private const val WHITE_HEADER = "White"
private const val BLACK_HEADER = "Black"
