package com.example.chessboard.ui.screen

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.entity.SideMask
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.ChessBoardWithCoordinates
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TrainingBackgroundDark
import com.example.chessboard.ui.theme.TrainingTextPrimary
import com.example.chessboard.ui.theme.TrainingTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class TrainSingleGameData(
    val game: GameEntity,
    val uciMoves: List<String>
)

private enum class TrainSingleGamePhase {
    Idle
}

@Composable
fun TrainSingleGameScreenContainer(
    activity: Activity,
    gameId: Long,
    trainingId: Long,
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    modifier: Modifier = Modifier,
    inDbProvider: DatabaseProvider,
) {
    var trainingGameData by remember { mutableStateOf<TrainSingleGameData?>(null) }

    LaunchedEffect(gameId) {
        trainingGameData = withContext(Dispatchers.IO) {
            val game = inDbProvider.getGameById(gameId) ?: return@withContext null
            TrainSingleGameData(
                game = game,
                uciMoves = parsePgnMoves(game.pgn)
            )
        }
    }

    TrainSingleGameScreen(
        gameId = gameId,
        trainingId = trainingId,
        trainingGameData = trainingGameData,
        onBackClick = onBackClick,
        onNavigate = onNavigate,
        modifier = modifier
    )
}

@Composable
private fun TrainSingleGameScreen(
    gameId: Long,
    trainingId: Long,
    trainingGameData: TrainSingleGameData? = null,
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedNavItem by remember { mutableStateOf<ScreenType>(ScreenType.Home) }
    var currentSideIndex by remember(trainingGameData?.game?.id) { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(TrainSingleGamePhase.Idle) }
    val trainingSides = remember(trainingGameData?.game?.sideMask) {
        trainingGameData?.game?.sideMask?.let(::resolveTrainingOrientations).orEmpty()
    }
    val currentOrientation = trainingSides.getOrNull(currentSideIndex) ?: BoardOrientation.WHITE
    val gameController = remember(currentOrientation) { GameController(currentOrientation) }

    LaunchedEffect(gameController, trainingGameData?.game?.id) {
        gameController.resetToStartPosition()
        phase = TrainSingleGamePhase.Idle
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = TrainingBackgroundDark,
        topBar = {
            AppTopBar(
                title = "Train Game",
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            AppBottomNavigation(
                items = defaultAppBottomNavigationItems(),
                selectedItem = selectedNavItem,
                onItemSelected = {
                    selectedNavItem = it
                    onNavigate(it)
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            TrainSingleGameContent(
                gameId = gameId,
                trainingId = trainingId,
                trainingGameData = trainingGameData,
                gameController = gameController,
                currentOrientation = currentOrientation,
                currentSideIndex = currentSideIndex,
                sidesCount = trainingSides.size,
                phase = phase
            )
        }
    }
}

@Composable
private fun TrainSingleGameContent(
    gameId: Long,
    trainingId: Long,
    trainingGameData: TrainSingleGameData?,
    gameController: GameController,
    currentOrientation: BoardOrientation,
    currentSideIndex: Int,
    sidesCount: Int,
    phase: TrainSingleGamePhase,
) {
    ScreenSection {
        if (trainingGameData == null) {
            BodySecondaryText(
                text = "Loading training session for gameId=$gameId, trainingId=$trainingId",
                color = TrainingTextSecondary
            )
            return@ScreenSection
        }

        Column {
            SectionTitleText(
                text = trainingGameData.game.event ?: "Unnamed Opening",
                color = TrainingTextPrimary
            )
            Spacer(modifier = Modifier.height(AppDimens.spaceSm))
            TrainingBoardSection(gameController = gameController)
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            BodySecondaryText(
                text = "Training ID: $trainingId",
                color = TrainingTextSecondary
            )
            BodySecondaryText(
                text = "Game ID: $gameId",
                color = TrainingTextSecondary
            )
            BodySecondaryText(
                text = "Moves loaded: ${trainingGameData.uciMoves.size}",
                color = TrainingTextSecondary
            )
            BodySecondaryText(
                text = "Training side: ${orientationLabel(currentOrientation)}",
                color = TrainingTextSecondary
            )
            if (sidesCount > 1) {
                CardMetaText(
                    text = "Side ${currentSideIndex + 1} of $sidesCount",
                    color = TrainingTextSecondary
                )
            }
            BodySecondaryText(
                text = "Session state: ${phase.name}",
                color = TrainingTextSecondary
            )
        }
    }
}

@Composable
private fun TrainingBoardSection(
    gameController: GameController,
    modifier: Modifier = Modifier
) {
    CardSurface(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(AppDimens.spaceMd)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppDimens.radiusLg))
        ) {
            ChessBoardWithCoordinates(
                gameController = gameController,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun resolveTrainingOrientations(sideMask: Int): List<BoardOrientation> {
    if (sideMask == SideMask.WHITE) {
        return listOf(BoardOrientation.WHITE)
    }

    if (sideMask == SideMask.BLACK) {
        return listOf(BoardOrientation.BLACK)
    }

    if (sideMask == SideMask.BOTH) {
        return listOf(BoardOrientation.WHITE, BoardOrientation.BLACK)
    }

    return listOf(BoardOrientation.WHITE)
}

private fun orientationLabel(orientation: BoardOrientation): String =
    when (orientation) {
        BoardOrientation.WHITE -> "White"
        BoardOrientation.BLACK -> "Black"
    }
