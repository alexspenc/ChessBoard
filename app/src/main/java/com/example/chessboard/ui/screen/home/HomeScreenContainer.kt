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
import com.example.chessboard.entity.SideMask
import com.example.chessboard.service.OneGameTrainingData
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class HomeTrainingItem(
    val trainingId: Long,
    val name: String,
    val gamesCount: Int,
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
    onOpenPositionEditorClick: () -> Unit = {},
    onOpenSavedPositionsClick: () -> Unit = { screenContext.onNavigate(ScreenType.SavedPositions) },
    modifier: Modifier = Modifier,
) {
    var trainings by remember { mutableStateOf<List<HomeTrainingItem>>(emptyList()) }
    val trainingService = remember(screenContext.inDbProvider) {
        screenContext.inDbProvider.createTrainingService()
    }

    LaunchedEffect(simpleViewEnabled) {
        if (!simpleViewEnabled) {
            trainings = emptyList()
            return@LaunchedEffect
        }
        trainings = withContext(Dispatchers.IO) {
            val allGames = screenContext.inDbProvider.getAllGames().associateBy { it.id }
            trainingService.getAllTrainings().map { training ->
                val trainingGames = OneGameTrainingData.fromJson(training.gamesJson)
                val includedGames = trainingGames.mapNotNull { allGames[it.gameId] }
                HomeTrainingItem(
                    trainingId = training.id,
                    name = training.name.ifBlank { "Unnamed Training" },
                    gamesCount = trainingGames.size,
                    supportsWhite = includedGames.any { game ->
                        (game.sideMask and SideMask.WHITE) != 0
                    },
                    supportsBlack = includedGames.any { game ->
                        (game.sideMask and SideMask.BLACK) != 0
                    },
                )
            }
        }
    }

    HomeScreen(
        simpleViewEnabled = simpleViewEnabled,
        trainings = trainings,
        onNavigate = screenContext.onNavigate,
        onCreateOpeningClick = onCreateOpeningClick,
        onCreateTrainingClick = onCreateTrainingClick,
        onSmartTrainingClick = onSmartTrainingClick,
        onOpenPositionEditorClick = onOpenPositionEditorClick,
        onOpenSavedPositionsClick = onOpenSavedPositionsClick,
        onOpenBackupClick = { screenContext.onNavigate(ScreenType.Backup) },
        onExitClick = { activity.finishAffinity() },
        modifier = modifier,
    )
}

@Composable
private fun HomeScreen(
    simpleViewEnabled: Boolean,
    trainings: List<HomeTrainingItem>,
    onNavigate: (ScreenType) -> Unit = {},
    onCreateOpeningClick: () -> Unit = { onNavigate(ScreenType.CreateOpening) },
    onCreateTrainingClick: () -> Unit = {},
    onSmartTrainingClick: () -> Unit = {},
    onOpenPositionEditorClick: () -> Unit = {},
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
            onSmartTrainingClick = onSmartTrainingClick,
            modifier = modifier,
        )
        return
    }

    RegularHomeScreen(
        onNavigate = onNavigate,
        onCreateOpeningClick = onCreateOpeningClick,
        onCreateTrainingClick = onCreateTrainingClick,
        onOpenPositionEditorClick = onOpenPositionEditorClick,
        onOpenSavedPositionsClick = onOpenSavedPositionsClick,
        onOpenBackupClick = onOpenBackupClick,
        onExitClick = onExitClick,
        modifier = modifier,
    )
}
