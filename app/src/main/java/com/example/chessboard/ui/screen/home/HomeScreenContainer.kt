package com.example.chessboard.ui.screen.home

/**
 * File role: owns home-screen loading and chooses which home-screen branch to render.
 * Allowed here:
 * - screen container state for home data
 * - the branch chooser between regular and SimpleView home UIs
 * Not allowed here:
 * - large chunks of regular-home layout markup
 * - large chunks of SimpleView home layout markup
 * - persistence rules that belong in services
 * Validation date: 2026-05-03
 */
import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R
import com.example.chessboard.entity.SideMask
import com.example.chessboard.service.OneLineTrainingData
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class HomeTrainingItem(
    val trainingId: Long,
    val name: String,
    val linesCount: Int,
    val supportsWhite: Boolean,
    val supportsBlack: Boolean,
)

@Composable
fun HomeScreenContainer(
    activity: Activity,
    screenContext: ScreenContainerContext,
    simpleViewEnabled: Boolean,
    onCreateOpeningClick: () -> Unit = { screenContext.onNavigate(ScreenType.CreateOpening) },
    onCreateTrainingClick: () -> Unit = {},
    onSmartTrainingClick: () -> Unit = { screenContext.onNavigate(ScreenType.SmartTraining) },
    onOpenPositionSearchClick: () -> Unit = {},
    onOpenSavedPositionsClick: () -> Unit = { screenContext.onNavigate(ScreenType.SavedPositions) },
    modifier: Modifier = Modifier,
) {
    var trainings by remember { mutableStateOf<List<HomeTrainingItem>>(emptyList()) }
    val trainingService = remember(screenContext.inDbProvider) {
        screenContext.inDbProvider.createTrainingService()
    }
    val lineListService = remember(screenContext.inDbProvider) {
        screenContext.inDbProvider.createLineListService()
    }
    val unnamedTrainingName = stringResource(R.string.home_unnamed_training)

    LaunchedEffect(simpleViewEnabled, unnamedTrainingName) {
        if (!simpleViewEnabled) {
            trainings = emptyList()
            return@LaunchedEffect
        }
        trainings = withContext(Dispatchers.IO) {
            val allLines = screenContext.inDbProvider.getAllLines().associateBy { it.id }
            trainingService.getAllTrainings().map { training ->
                val trainingLines = OneLineTrainingData.fromJson(training.linesJson)
                val includedLines = trainingLines.mapNotNull { allLines[it.lineId] }
                HomeTrainingItem(
                    trainingId = training.id,
                    name = training.name.ifBlank { unnamedTrainingName },
                    linesCount = trainingLines.size,
                    supportsWhite = includedLines.any { line ->
                        (line.sideMask and SideMask.WHITE) != 0
                    },
                    supportsBlack = includedLines.any { line ->
                        (line.sideMask and SideMask.BLACK) != 0
                    },
                )
            }
        }
    }

    HomeSmartTrainingNavigationHost(
        lineListService = lineListService,
        trainingService = trainingService,
        errorReporter = screenContext.errorReporter,
        onSmartTrainingClick = onSmartTrainingClick,
        onCreateOpeningClick = onCreateOpeningClick,
        onCreateTrainingClick = onCreateTrainingClick,
    ) { preparedSmartTrainingClick ->
        HomeTrainingNavigationHost(
            lineListService = lineListService,
            errorReporter = screenContext.errorReporter,
            onTrainingClick = { screenContext.onNavigate(ScreenType.Training) },
            onCreateOpeningClick = onCreateOpeningClick,
        ) { preparedTrainingClick ->
            HomeScreen(
                simpleViewEnabled = simpleViewEnabled,
                trainings = trainings,
                onNavigate = screenContext.onNavigate,
                onCreateOpeningClick = onCreateOpeningClick,
                onCreateTrainingClick = onCreateTrainingClick,
                onOpenTrainingsClick = preparedTrainingClick,
                onSmartTrainingClick = preparedSmartTrainingClick,
                onOpenPositionSearchClick = onOpenPositionSearchClick,
                onOpenSavedPositionsClick = onOpenSavedPositionsClick,
                onOpenBackupClick = { screenContext.onNavigate(ScreenType.Backup) },
                onExitClick = { activity.finishAffinity() },
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun HomeScreen(
    simpleViewEnabled: Boolean,
    trainings: List<HomeTrainingItem>,
    onNavigate: (ScreenType) -> Unit = {},
    onCreateOpeningClick: () -> Unit = { onNavigate(ScreenType.CreateOpening) },
    onCreateTrainingClick: () -> Unit = {},
    onOpenTrainingsClick: () -> Unit = { onNavigate(ScreenType.Training) },
    onSmartTrainingClick: () -> Unit = {},
    onOpenPositionSearchClick: () -> Unit = {},
    onOpenSavedPositionsClick: () -> Unit = {},
    onOpenBackupClick: () -> Unit = {},
    onExitClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (simpleViewEnabled) {
        SimpleHomeScreen(
            trainings = trainings,
            onCreateOpeningClick = onCreateOpeningClick,
            onOpenTraining = { trainingId ->
                onNavigate(ScreenType.EditTraining(trainingId))
            },
            onNavigate = onNavigate,
            onOpenTrainingsClick = onOpenTrainingsClick,
            onSmartTrainingClick = onSmartTrainingClick,
            onOpenSavedPositionsClick = onOpenSavedPositionsClick,
            modifier = modifier,
        )
        return
    }

    RegularHomeScreen(
        onNavigate = onNavigate,
        onCreateOpeningClick = onCreateOpeningClick,
        onCreateTrainingClick = onCreateTrainingClick,
        onOpenTrainingsClick = onOpenTrainingsClick,
        onOpenPositionSearchClick = onOpenPositionSearchClick,
        onOpenSavedPositionsClick = onOpenSavedPositionsClick,
        onOpenBackupClick = onOpenBackupClick,
        onExitClick = onExitClick,
        modifier = modifier,
    )
}
