@file:Suppress("FunctionName")

package com.example.chessboard.ui.screen.gameOpeningAnalysis

/*
 * File role: renders the game-opening analysis screen entry point and imported-game list shell.
 * Allowed here:
 * - screen-level UI for imported game opening analysis
 * - summary, empty state, imported-game list rendering, and selected-game preview
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.chessboard.R
import com.example.chessboard.analysis.OpeningSide
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.runtimecontext.GameOpeningAnalysisRuntimeContext
import com.example.chessboard.runtimecontext.ImportedGameItem
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.GameOpeningAnalysisContentTestTag
import com.example.chessboard.ui.GameOpeningAnalysisEmptyStateTestTag
import com.example.chessboard.ui.GameOpeningAnalysisGameListTestTag
import com.example.chessboard.ui.GameOpeningAnalysisNextMoveTestTag
import com.example.chessboard.ui.GameOpeningAnalysisPreviewTestTag
import com.example.chessboard.ui.GameOpeningAnalysisPreviousMoveTestTag
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BoardActionNavigationBar
import com.example.chessboard.ui.components.BoardActionNavigationItem
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.ChessBoardSection
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.components.LineMoveTreeSection
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.BottomBarContentColor
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
    val selectedGame = visibleGames.firstOrNull { game -> game.id == runtimeContext.selectedGameId }
    val lineController = remember { LineController(resolveBoardOrientation(runtimeContext.filter.side)) }

    LaunchedEffect(selectedGame?.id, runtimeContext.filter.side) {
        val orientation = resolveBoardOrientation(runtimeContext.filter.side)
        lineController.setOrientation(orientation)
        if (selectedGame == null) {
            lineController.resetToStartPosition()
            lineController.setUserMovesEnabled(false)
            return@LaunchedEffect
        }

        lineController.loadFromUciMoves(selectedGame.mainLineMoves, targetPly = 0)
        lineController.setUserMovesEnabled(false)
    }

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
        bottomBar = {
            GameOpeningAnalysisBoardControlsBar(
                canUndo = selectedGame != null && lineController.canUndo,
                canRedo = selectedGame != null && lineController.canRedo,
                onPreviousMoveClick = { lineController.undoMove() },
                onNextMoveClick = { lineController.redoMove() },
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
                if (game.id == selectedGame?.id) {
                    ImportedGamePreview(
                        game = game,
                        lineController = lineController,
                        onMovePlyClick = { targetPly ->
                            lineController.loadFromUciMoves(game.mainLineMoves, targetPly = targetPly)
                            lineController.setUserMovesEnabled(false)
                        },
                    )
                    return@forEach
                }

                ImportedGameCard(
                    game = game,
                    onClick = {
                        runtimeContext.selectGame(game.id)
                        lineController.setOrientation(resolveBoardOrientation(runtimeContext.filter.side))
                        lineController.loadFromUciMoves(game.mainLineMoves, targetPly = 0)
                        lineController.setUserMovesEnabled(false)
                    },
                )
            }
        }
    }
}

@Composable
private fun GameOpeningAnalysisBoardControlsBar(
    canUndo: Boolean,
    canRedo: Boolean,
    onPreviousMoveClick: () -> Unit,
    onNextMoveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoardActionNavigationBar(
        modifier = modifier,
        maxVisibleItems = 2,
        items =
            listOf(
                BoardActionNavigationItem(
                    label = stringResource(R.string.common_back),
                    enabled = canUndo,
                    modifier = Modifier.testTag(GameOpeningAnalysisPreviousMoveTestTag),
                    onClick = onPreviousMoveClick,
                ) {
                    IconMd(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription =
                            stringResource(
                                R.string.game_opening_analysis_previous_move_content_description,
                            ),
                        tint = resolveMoveControlTint(canUndo),
                    )
                },
                BoardActionNavigationItem(
                    label = stringResource(R.string.common_forward),
                    enabled = canRedo,
                    modifier = Modifier.testTag(GameOpeningAnalysisNextMoveTestTag),
                    onClick = onNextMoveClick,
                ) {
                    IconMd(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription =
                            stringResource(
                                R.string.game_opening_analysis_next_move_content_description,
                            ),
                        tint = resolveMoveControlTint(canRedo),
                    )
                },
            ),
    )
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

private fun resolveBoardOrientation(side: OpeningSide): BoardOrientation {
    if (side == OpeningSide.BLACK) {
        return BoardOrientation.BLACK
    }

    return BoardOrientation.WHITE
}

private fun resolveMoveControlTint(enabled: Boolean): Color {
    if (enabled) {
        return BottomBarContentColor
    }

    return BottomBarContentColor.copy(alpha = 0.5f)
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
