package com.example.chessboard.ui.screen.trainingTemplateScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.example.chessboard.ui.theme.TrainingBackgroundDark
import com.example.chessboard.ui.theme.TrainingCardDark
import com.example.chessboard.ui.theme.TrainingTextPrimary
import com.example.chessboard.ui.theme.TrainingTextSecondary
import com.example.chessboard.ui.theme.TrainingSuccessGreen
import com.example.chessboard.ui.theme.TrainingAccentTeal

@Composable
fun CreateTrainingTemplateScreen(
    modifier: Modifier = Modifier
) {
    // ----------------------
    // STATE (пока локально)
    // TODO: перенести во ViewModel
    // ----------------------

    var templateName by remember { mutableStateOf("") }

    var selectedSide by remember { mutableStateOf("WHITE") }
    var rangeStart by remember { mutableStateOf(0) }

    val candidateGames = remember { mutableStateListOf<GameUiModel>() }
    val selectedGames = remember { mutableStateListOf<TemplateGameUiModel>() }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TrainingBackgroundDark)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {

        // ----------------------
        // TEMPLATE NAME
        // ----------------------

        Text(
            text = "Название шаблона",
            color = TrainingTextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = templateName,
            onValueChange = { templateName = it },
            modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.ui.text.TextStyle(color = TrainingTextPrimary),
            label = {
                Text(
                    text = "Введите название",
                    color = TrainingTextSecondary
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ----------------------
        // SEARCH SETTINGS
        // ----------------------

        SearchSettingsSection(
            selectedSide = selectedSide,
            onSideChange = {
                selectedSide = it
                // TODO: обновить кандидатов через DAO
            },
            rangeStart = rangeStart,
            onRangeChange = {
                rangeStart = it
                // TODO: обновить кандидатов через DAO
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ----------------------
        // CANDIDATES
        // ----------------------

        Text(
            text = "Кандидаты",
            color = TrainingTextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        CandidatesList(
            games = candidateGames,
            onAddClick = { game ->
                if (selectedGames.none { it.id == game.id }) {
                    selectedGames.add(
                        TemplateGameUiModel(
                            id = game.id,
                            name = game.name,
                            weight = 1
                        )
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ----------------------
        // SELECTED
        // ----------------------

        Text(
            text = "Выбранные партии",
            color = TrainingTextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        SelectedGamesList(
            games = selectedGames,
            onRemove = { game ->
                selectedGames.remove(game)
            },
            onWeightChange = { game, newWeight ->
                val index = selectedGames.indexOfFirst { it.id == game.id }
                if (index != -1) {
                    selectedGames[index] = game.copy(weight = newWeight)
                }
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        // ----------------------
        // BUTTONS
        // ----------------------

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Button(
                onClick = {
                    // TODO: сохранить шаблон
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrainingSuccessGreen,
                    contentColor = TrainingTextPrimary
                )
            ) {
                Text("Сохранить")
            }

            OutlinedButton(
                onClick = {
                    // TODO: клонировать
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Клонировать",
                    color = TrainingAccentTeal
                )
            }
        }
    }
}