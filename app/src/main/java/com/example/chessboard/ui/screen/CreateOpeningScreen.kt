package com.example.chessboard.ui.screen

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TrainingBackgroundDark
import com.example.chessboard.ui.theme.TrainingTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CreateOpeningScreenContainer(
    activity: Activity,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    inDbProvider : DatabaseProvider,
) {
    val dbProvider = inDbProvider
    val gameController = remember { GameController() }

    CreateOpeningScreen(
        gameController = gameController,
        onBackClick = onBackClick,
        onSave = { name, eco ->
            val entity = GameEntity(
                event = name.ifBlank { null },
                eco = eco.ifBlank { null },
                pgn = gameController.generatePgn(),
                initialFen = "",
            )
            (activity as? LifecycleOwner)?.lifecycleScope?.launch(Dispatchers.IO) {
                dbProvider.addGame(entity, gameController.getMovesCopy())
                withContext(Dispatchers.Main) { onBackClick() }
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOpeningScreen(
    gameController: GameController,
    onBackClick: () -> Unit = {},
    onSave: (name: String, eco: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var openingName by remember { mutableStateOf("") }
    var ecoCode by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = TrainingBackgroundDark,
        topBar = {
            AppTopBar(
                title = "Create Opening",
                subtitle = "Build your custom opening",
                onBackClick = onBackClick,
                actions = {
                    PrimaryButton(
                        "Save",
                        onClick = {
                            if (openingName.isBlank()) {
                                nameError = true
                                return@PrimaryButton
                            }
                            onSave(openingName, ecoCode)
                        })
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            Spacer(modifier = Modifier.height(AppDimens.spaceXs))

            ScreenSection {
                DarkInputField(
                    value = openingName,
                    onValueChange = { openingName = it; nameError = false },
                    placeholder = "e.g., Sicilian Defense",
                    label = "Opening Name *",
                    isError = nameError
                )
            }

            ScreenSection {
                DarkInputField(
                    value = ecoCode,
                    onValueChange = { ecoCode = it },
                    placeholder = "e.g., B20",
                    label = "ECO Code",
                    modifier = Modifier.fillMaxWidth(0.5f)
                )
            }

            ScreenSection {
                SectionTitleText(
                    text = "Drag pieces to add moves",
                    color = TrainingTextSecondary
                )
            }

            ScreenSection {
                ChessBoardSection(gameController = gameController)
            }

            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
        }
    }
}
