@file:Suppress("FunctionName")

package com.example.chessboard.ui.screen.gameOpeningAnalysis

/*
 * File role: renders the imported-games list and selected-game preview for game-opening analysis.
 * Allowed here:
 * - list, empty state, card, preview, and imported-game header UI for this screen
 * - small presentation helpers for imported-game event and player names
 * Not allowed here:
 * - import/export orchestration, analysis execution, dialog state, or runtime-context mutation
 * Validation date: 2026-06-30
 */

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.chessboard.R
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.runtimecontext.ImportedGameItem
import com.example.chessboard.ui.GameOpeningAnalysisContentTestTag
import com.example.chessboard.ui.GameOpeningAnalysisEmptyStateTestTag
import com.example.chessboard.ui.GameOpeningAnalysisGameListTestTag
import com.example.chessboard.ui.GameOpeningAnalysisPreviewTestTag
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.ChessBoardSection
import com.example.chessboard.ui.components.LineMoveTreeSection
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TextColor

@Composable
internal fun GameOpeningAnalysisImportedGamesContent(
    importedGames: List<ImportedGameItem>,
    visibleGames: List<ImportedGameItem>,
    selectedGame: ImportedGameItem?,
    lineController: LineController,
    onGameClick: (ImportedGameItem) -> Unit,
    onMovePlyClick: (ImportedGameItem, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(AppDimens.spaceLg)
                .testTag(GameOpeningAnalysisContentTestTag),
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
    ) {
        if (visibleGames.isEmpty()) {
            GameOpeningAnalysisEmptyState(hasImportedGames = importedGames.isNotEmpty())
            return@Column
        }

        BodySecondaryText(
            text = stringResource(R.string.game_opening_analysis_list_hint),
            color = TextColor.Secondary,
        )

        visibleGames.forEach { game ->
            if (game.id == selectedGame?.id) {
                ImportedGamePreview(
                    game = game,
                    lineController = lineController,
                    onMovePlyClick = { targetPly -> onMovePlyClick(game, targetPly) },
                )
                return@forEach
            }

            ImportedGameCard(
                game = game,
                onClick = { onGameClick(game) },
            )
        }
    }
}

@Composable
private fun GameOpeningAnalysisEmptyState(hasImportedGames: Boolean) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .testTag(GameOpeningAnalysisEmptyStateTestTag),
        contentAlignment = Alignment.Center,
    ) {
        val emptyMessage =
            if (hasImportedGames) {
                stringResource(R.string.game_opening_analysis_empty_filtered)
            } else {
                stringResource(R.string.game_opening_analysis_empty)
            }

        BodySecondaryText(
            text = emptyMessage,
            color = TextColor.Secondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ImportedGameCard(
    game: ImportedGameItem,
    onClick: () -> Unit,
) {
    val unknownEvent = stringResource(R.string.game_opening_analysis_unknown_event)
    val unknownPlayer = stringResource(R.string.game_opening_analysis_unknown_player)

    CardSurface(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(GameOpeningAnalysisGameListTestTag),
        onClick = onClick,
    ) {
        ImportedGameHeader(
            game = game,
            unknownEvent = unknownEvent,
            unknownPlayer = unknownPlayer,
        )
    }
}

@Composable
private fun ImportedGamePreview(
    game: ImportedGameItem,
    lineController: LineController,
    onMovePlyClick: (Int) -> Unit,
) {
    val unknownEvent = stringResource(R.string.game_opening_analysis_unknown_event)
    val unknownPlayer = stringResource(R.string.game_opening_analysis_unknown_player)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(GameOpeningAnalysisPreviewTestTag),
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
    ) {
        ChessBoardSection(lineController = lineController)
        ImportedGameHeader(
            game = game,
            unknownEvent = unknownEvent,
            unknownPlayer = unknownPlayer,
        )
        LineMoveTreeSection(
            importedUciLines = listOf(game.mainLineMoves),
            lineController = lineController,
            modifier = Modifier.fillMaxWidth(),
            onMoveSelected = { _, targetPly -> onMovePlyClick(targetPly) },
        )
    }
}

@Composable
private fun ImportedGameHeader(
    game: ImportedGameItem,
    unknownEvent: String,
    unknownPlayer: String,
) {
    SectionTitleText(text = game.eventTitle(unknownEvent))
    Spacer(modifier = Modifier.height(AppDimens.spaceSm))
    BodySecondaryText(
        text =
            stringResource(
                R.string.game_opening_analysis_players,
                game.playerName(WHITE_HEADER, unknownPlayer),
                game.playerName(BLACK_HEADER, unknownPlayer),
            ),
        color = TextColor.Secondary,
    )
    Spacer(modifier = Modifier.height(AppDimens.spaceSm))
    CardMetaText(
        text = stringResource(R.string.game_opening_analysis_game_ply_count, game.mainLineMoves.size),
        color = TextColor.Secondary,
    )
}

internal fun ImportedGameItem.eventTitle(unknownEvent: String): String {
    val event = headers[EVENT_HEADER]
    if (!event.isNullOrBlank()) {
        return event
    }

    return unknownEvent
}

private fun ImportedGameItem.playerName(
    headerName: String,
    unknownPlayer: String,
): String = headers[headerName].orUnknownPlayer(unknownPlayer)

private fun String?.orUnknownPlayer(unknownPlayer: String): String {
    if (!isNullOrBlank()) {
        return this
    }

    return unknownPlayer
}

private const val EVENT_HEADER = "Event"
private const val WHITE_HEADER = "White"
private const val BLACK_HEADER = "Black"
