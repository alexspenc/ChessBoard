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
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.service.OneLineTrainingData
import com.example.chessboard.service.parsePgnMoves
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.HomeIconButton
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
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
        onHomeClick = { screenContext.onNavigate(ScreenType.Home) },
        modifier = modifier,
    )
}

private suspend fun resolveTrainingMaxMove(dbProvider: DatabaseProvider, trainingId: Long): Int? {
    val training = dbProvider.createTrainingService().getTrainingById(trainingId) ?: return null
    val lineIds = OneLineTrainingData.fromJson(training.linesJson).map { it.lineId }
    if (lineIds.isEmpty()) return null

    val allLinesById = dbProvider.getAllLines().associateBy { it.id }
    return lineIds
        .mapNotNull { allLinesById[it] }
        .mapNotNull { line ->
            val plies = parsePgnMoves(line.pgn).size
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
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var moveRange by remember {
        mutableStateOf(TrainingMoveRange(from = initialMoveFrom, to = initialMoveTo))
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.training_settings_title),
                onBackClick = onBackClick,
                handleSystemBack = true,
                actions = {
                    HomeIconButton(onClick = onHomeClick)
                },
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
            SectionTitleText(text = stringResource(R.string.training_settings_move_range_title))
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
