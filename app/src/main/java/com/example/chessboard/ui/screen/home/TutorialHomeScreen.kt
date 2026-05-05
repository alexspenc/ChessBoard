package com.example.chessboard.ui.screen.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.chessboard.entity.tutorial.TutorialProgressEntity
import com.example.chessboard.entity.tutorial.TutorialStage
import com.example.chessboard.service.tutorial.TutorialService
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class HomeTutorialState(
    val activeTutorial: TutorialProgressEntity? = null,
    val shouldOfferManualTutorial: Boolean = false,
)

@Composable
internal fun TutorialHomeScreen(
    screenContext: ScreenContainerContext,
    trainings: List<HomeTrainingItem>,
    onCreateOpeningClick: () -> Unit,
    onOpenTraining: (Long) -> Unit,
    onNavigate: (ScreenType) -> Unit,
    onSmartTrainingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tutorialState by remember { mutableStateOf(HomeTutorialState()) }
    var showTutorialDialog by remember { mutableStateOf(false) }
    val tutorialService = remember(screenContext.inDbProvider) {
        screenContext.inDbProvider.createTutorialService()
    }
    val scope = rememberCoroutineScope()

    LaunchedEffect(screenContext.inDbProvider) {
        tutorialState = loadHomeTutorialState(tutorialService)
    }

    SimpleHomeScreen(
        trainings = trainings,
        tutorialHelpContentDescription = resolveHomeTutorialHelpContentDescription(tutorialState),
        onTutorialHelpClick = { showTutorialDialog = true },
        onCreateOpeningClick = onCreateOpeningClick,
        onOpenTraining = onOpenTraining,
        onNavigate = onNavigate,
        onSmartTrainingClick = onSmartTrainingClick,
        modifier = modifier,
    )

    if (showTutorialDialog) {
        RenderHomeTutorialDialog(
            tutorialState = tutorialState,
            onDismiss = { showTutorialDialog = false },
            onStartTutorialClick = {
                scope.launch(Dispatchers.IO) {
                    tutorialService.startManualFirstFlowTutorial()
                    val updatedTutorialState = loadHomeTutorialState(tutorialService)
                    withContext(Dispatchers.Main) {
                        tutorialState = updatedTutorialState
                        showTutorialDialog = false
                    }
                }
            },
        )
    }
}

private suspend fun loadHomeTutorialState(
    tutorialService: TutorialService
): HomeTutorialState {
    return withContext(Dispatchers.IO) {
        HomeTutorialState(
            activeTutorial = tutorialService.getActiveTutorial(),
            shouldOfferManualTutorial = tutorialService.shouldOfferManualTutorial(),
        )
    }
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
