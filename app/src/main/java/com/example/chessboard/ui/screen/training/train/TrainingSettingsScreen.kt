package com.example.chessboard.ui.screen.training.train

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.service.OneGameTrainingData
import com.example.chessboard.service.parsePgnMoves
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.theme.AppDimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun TrainingSettingsScreenContainer(
    trainingId: Long,
    screenContext: ScreenContainerContext,
    initialMoveFrom: Int,
    initialMoveTo: Int,
    onMoveRangeChange: (Int, Int) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var maxMove by remember { mutableIntStateOf(DefaultMaxMove) }

    LaunchedEffect(trainingId) {
        val computed = withContext(Dispatchers.IO) {
            resolveTrainingMaxMove(screenContext.inDbProvider, trainingId)
        }
        if (computed != null && computed > 1) {
            maxMove = computed
        }
    }

    TrainingSettingsScreen(
        initialMoveFrom = initialMoveFrom,
        initialMoveTo = initialMoveTo,
        maxMove = maxMove,
        onMoveRangeChange = onMoveRangeChange,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

private suspend fun resolveTrainingMaxMove(dbProvider: DatabaseProvider, trainingId: Long): Int? {
    val training = dbProvider.createTrainingService().getTrainingById(trainingId) ?: return null
    val gameIds = OneGameTrainingData.fromJson(training.gamesJson).map { it.gameId }
    if (gameIds.isEmpty()) return null

    val allGamesById = dbProvider.getAllGames().associateBy { it.id }
    return gameIds
        .mapNotNull { allGamesById[it] }
        .mapNotNull { game ->
            val plies = parsePgnMoves(game.pgn).size
            if (plies > 0) (plies + 1) / 2 else null
        }
        .maxOrNull()
}

@Composable
fun TrainingSettingsScreen(
    initialMoveFrom: Int,
    initialMoveTo: Int,
    maxMove: Int = DefaultMaxMove,
    onMoveRangeChange: (Int, Int) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var moveRange by remember {
        mutableStateOf(TrainingMoveRange(from = initialMoveFrom, to = initialMoveTo))
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = "Training Settings",
                onBackClick = onBackClick,
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(AppDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
        ) {
            SectionTitleText(text = "Move range")
            EditTrainingMoveRangeSection(
                moveRange = moveRange,
                maxMove = maxMove,
                onMoveRangeChange = { newRange ->
                    moveRange = newRange
                    onMoveRangeChange(newRange.from, newRange.to)
                }
            )
        }
    }
}
