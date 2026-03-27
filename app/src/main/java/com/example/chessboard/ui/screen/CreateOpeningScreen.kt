package com.example.chessboard.ui.screen

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TextColor
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
    var selectedSide by remember { mutableStateOf(EditableGameSide.AS_BOTH) }

    LaunchedEffect(selectedSide) {
        gameController.setOrientation(selectedSide.orientation)
    }

    CreateOpeningScreen(
        gameController = gameController,
        selectedSide = selectedSide,
        onSideSelected = { selectedSide = it },
        onBackClick = onBackClick,
        onSave = { name, eco, sideMask ->
            val entity = GameEntity(
                event = name.ifBlank { null },
                eco = eco.ifBlank { null },
                pgn = gameController.generatePgn(),
                initialFen = "",
                sideMask = sideMask
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
private fun CreateOpeningScreen(
    gameController: GameController,
    selectedSide: EditableGameSide,
    onSideSelected: (EditableGameSide) -> Unit = {},
    onBackClick: () -> Unit = {},
    onSave: (name: String, eco: String, sideMask: Int) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var openingName by remember { mutableStateOf("") }
    var ecoCode by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
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
                            onSave(openingName, ecoCode, selectedSide.sideMask)
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
                GameSideSelector(
                    selectedSide = selectedSide,
                    onSideSelected = onSideSelected
                )
            }

            ScreenSection {
                SectionTitleText(text = "Drag pieces to add moves", color = TextColor.Secondary)
            }

            ScreenSection {
                ChessBoardSection(gameController = gameController)
            }

            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
        }
    }
}
