@file:Suppress("FunctionName")

package com.example.chessboard.ui.screen.gameOpeningAnalysis

/*
 * File role: renders the game-opening analysis screen entry point and imported-game list shell.
 * Allowed here:
 * - screen-level UI for imported game opening analysis
 * - summary, empty state, and imported-game list rendering backed by runtime context
 * Not allowed here:
 * - PGN parsing, database access, analyzer orchestration, or reusable generic components
 * Validation date: 2026-06-26
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
import com.example.chessboard.runtimecontext.GameOpeningAnalysisRuntimeContext
import com.example.chessboard.runtimecontext.ImportedGameItem
import com.example.chessboard.ui.GameOpeningAnalysisContentTestTag
import com.example.chessboard.ui.GameOpeningAnalysisEmptyStateTestTag
import com.example.chessboard.ui.GameOpeningAnalysisGameListTestTag
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TextColor

@Composable
fun GameOpeningAnalysisScreenContainer(
    screenContext: ScreenContainerContext,
    modifier: Modifier = Modifier,
) {
    GameOpeningAnalysisScreen(
        runtimeContext = screenContext.runtimeContext.gameOpeningAnalysis,
        onBackClick = screenContext.onBackClick,
        modifier = modifier,
    )
}

@Composable
internal fun GameOpeningAnalysisScreen(
    runtimeContext: GameOpeningAnalysisRuntimeContext,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val importedGames = runtimeContext.importedGames
    val visibleGames = runtimeContext.visibleGames()

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.game_opening_analysis_title),
                subtitle =
                    stringResource(
                        R.string.game_opening_analysis_subtitle,
                        importedGames.size,
                        visibleGames.size,
                    ),
                onBackClick = onBackClick,
                handleSystemBack = true,
                filledBackButton = true,
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(AppDimens.spaceLg)
                    .testTag(GameOpeningAnalysisContentTestTag),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
        ) {
            if (visibleGames.isEmpty()) {
                GameOpeningAnalysisEmptyState()
                return@Column
            }

            BodySecondaryText(
                text = stringResource(R.string.game_opening_analysis_list_hint),
                color = TextColor.Secondary,
            )

            visibleGames.forEach { game ->
                ImportedGameCard(game = game)
            }
        }
    }
}

@Composable
private fun GameOpeningAnalysisEmptyState() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .testTag(GameOpeningAnalysisEmptyStateTestTag),
        contentAlignment = Alignment.Center,
    ) {
        BodySecondaryText(
            text = stringResource(R.string.game_opening_analysis_empty),
            color = TextColor.Secondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ImportedGameCard(game: ImportedGameItem) {
    val unknownEvent = stringResource(R.string.game_opening_analysis_unknown_event)
    val unknownPlayer = stringResource(R.string.game_opening_analysis_unknown_player)

    CardSurface(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(GameOpeningAnalysisGameListTestTag),
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
}

private fun ImportedGameItem.eventTitle(unknownEvent: String): String {
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
