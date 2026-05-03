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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.chessboard.entity.SideMask
import com.example.chessboard.entity.TutorialProgressEntity
import com.example.chessboard.entity.TutorialStage
import com.example.chessboard.service.OneGameTrainingData
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class HomeTrainingItem(
    val trainingId: Long,
    val name: String,
    val gamesCount: Int,
    val supportsWhite: Boolean,
    val supportsBlack: Boolean,
)

private data class HomeTutorialState(
    val activeTutorial: TutorialProgressEntity? = null,
    val shouldOfferManualTutorial: Boolean = false,
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
    var tutorialState by remember { mutableStateOf(HomeTutorialState()) }
    var showTutorialDialog by remember { mutableStateOf(false) }
    val trainingService = remember(screenContext.inDbProvider) {
        screenContext.inDbProvider.createTrainingService()
    }
    val tutorialService = remember(screenContext.inDbProvider) {
        screenContext.inDbProvider.createTutorialService()
    }
    val scope = rememberCoroutineScope()

    LaunchedEffect(simpleViewEnabled) {
        if (!simpleViewEnabled) {
            trainings = emptyList()
            tutorialState = HomeTutorialState()
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
        tutorialState = withContext(Dispatchers.IO) {
            HomeTutorialState(
                activeTutorial = tutorialService.getActiveTutorial(),
                shouldOfferManualTutorial = tutorialService.shouldOfferManualTutorial(),
            )
        }
    }

    HomeScreen(
        simpleViewEnabled = simpleViewEnabled,
        trainings = trainings,
        tutorialHelpContentDescription = resolveHomeTutorialHelpContentDescription(tutorialState),
        onNavigate = screenContext.onNavigate,
        onCreateOpeningClick = onCreateOpeningClick,
        onCreateTrainingClick = onCreateTrainingClick,
        onSmartTrainingClick = onSmartTrainingClick,
        onOpenPositionEditorClick = onOpenPositionEditorClick,
        onOpenSavedPositionsClick = onOpenSavedPositionsClick,
        onOpenBackupClick = { screenContext.onNavigate(ScreenType.Backup) },
        onExitClick = { activity.finishAffinity() },
        onTutorialHelpClick = { showTutorialDialog = true },
        modifier = modifier,
    )

    if (showTutorialDialog) {
        RenderHomeTutorialDialog(
            tutorialState = tutorialState,
            onDismiss = { showTutorialDialog = false },
            onStartTutorialClick = {
                scope.launch(Dispatchers.IO) {
                    val startedTutorial = tutorialService.startManualFirstFlowTutorial()
                    val updatedTutorialState = HomeTutorialState(
                        activeTutorial = startedTutorial,
                        shouldOfferManualTutorial = tutorialService.shouldOfferManualTutorial(),
                    )
                    withContext(Dispatchers.Main) {
                        tutorialState = updatedTutorialState
                        showTutorialDialog = false
                    }
                }
            },
        )
    }
}

@Composable
private fun HomeScreen(
    simpleViewEnabled: Boolean,
    trainings: List<HomeTrainingItem>,
    tutorialHelpContentDescription: String = "Tutorial information",
    onNavigate: (ScreenType) -> Unit = {},
    onCreateOpeningClick: () -> Unit = { onNavigate(ScreenType.CreateOpening) },
    onCreateTrainingClick: () -> Unit = {},
    onSmartTrainingClick: () -> Unit = {},
    onOpenPositionEditorClick: () -> Unit = {},
    onOpenSavedPositionsClick: () -> Unit = {},
    onOpenBackupClick: () -> Unit = {},
    onExitClick: () -> Unit = {},
    onTutorialHelpClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (simpleViewEnabled) {
        SimpleHomeScreen(
            trainings = trainings,
            tutorialHelpContentDescription = tutorialHelpContentDescription,
            onCreateOpeningClick = onCreateOpeningClick,
            onOpenTraining = { trainingId ->
                onNavigate(ScreenType.EditTraining(trainingId))
            },
            onNavigate = onNavigate,
            onSmartTrainingClick = onSmartTrainingClick,
            onTutorialHelpClick = onTutorialHelpClick,
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

@Composable
private fun RenderHomeTutorialDialog(
    tutorialState: HomeTutorialState,
    onDismiss: () -> Unit,
    onStartTutorialClick: () -> Unit,
) {
    val activeTutorial = tutorialState.activeTutorial
    if (activeTutorial != null) {
        val instruction = resolveHomeTutorialInstruction(activeTutorial)
        AppMessageDialog(
            title = instruction.title,
            message = instruction.message,
            onDismiss = onDismiss,
        )
        return
    }

    if (tutorialState.shouldOfferManualTutorial) {
        AppMessageDialog(
            title = "Start tutorial",
            message = "You have no games and no training history. Start the basic tutorial for manual game creation?",
            onDismiss = onDismiss,
            confirmText = "Start tutorial",
            onConfirm = onStartTutorialClick,
            dismissText = "Cancel",
            onDismissClick = onDismiss,
        )
        return
    }

    AppMessageDialog(
        title = "Tutorial unavailable",
        message = "The basic tutorial is only offered when there are no saved games and no training history.",
        onDismiss = onDismiss,
    )
}

private data class HomeTutorialInstruction(
    val title: String,
    val message: String,
)

private fun resolveHomeTutorialHelpContentDescription(
    tutorialState: HomeTutorialState
): String {
    if (tutorialState.activeTutorial != null) {
        return "Tutorial help"
    }

    if (tutorialState.shouldOfferManualTutorial) {
        return "Start tutorial"
    }

    return "Tutorial information"
}

private fun resolveHomeTutorialInstruction(
    tutorial: TutorialProgressEntity
): HomeTutorialInstruction {
    if (tutorial.stage == TutorialStage.START) {
        return HomeTutorialInstruction(
            title = "Step 1 of 5",
            message = "Create a game manually. Use the Create Game button on the home screen.",
        )
    }

    if (tutorial.stage == TutorialStage.GAME_CREATED) {
        return HomeTutorialInstruction(
            title = "Step 2 of 5",
            message = "Your game is saved. Open Smart Training next to build a runtime training from that game.",
        )
    }

    if (tutorial.stage == TutorialStage.TRAINING_CREATED) {
        return HomeTutorialInstruction(
            title = "Step 3 of 5",
            message = "Start the tutorial training you just created.",
        )
    }

    if (tutorial.stage == TutorialStage.TRAINING_STARTED) {
        return HomeTutorialInstruction(
            title = "Step 4 of 5",
            message = "Finish the running tutorial training.",
        )
    }

    return HomeTutorialInstruction(
        title = "Step 5 of 5",
        message = "The tutorial training is complete.",
    )
}
