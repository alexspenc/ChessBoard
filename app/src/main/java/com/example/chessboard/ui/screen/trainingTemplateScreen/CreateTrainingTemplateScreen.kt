package com.example.chessboard.ui.screen.trainingTemplateScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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

    // TODO: загрузка кандидатов из БД при старте и изменении фильтров

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // ----------------------
        // TEMPLATE NAME
        // ----------------------

        OutlinedTextField(
            value = templateName,
            onValueChange = { templateName = it },
            label = { Text("Название шаблона") },
            modifier = Modifier.fillMaxWidth()
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

        Text("Кандидаты", style = MaterialTheme.typography.titleMedium)

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

                // TODO: обновить список кандидатов (исключить добавленные)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ----------------------
        // SELECTED
        // ----------------------

        Text("Выбранные партии", style = MaterialTheme.typography.titleMedium)

        SelectedGamesList(
            games = selectedGames,
            onRemove = { game ->
                selectedGames.remove(game)

                // TODO: возможно вернуть в кандидаты
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
                    // TODO: сохранить шаблон в БД
                    // данные:
                    // templateName
                    // selectedSide
                    // rangeStart
                    // selectedGames
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Сохранить")
            }

            OutlinedButton(
                onClick = {
                    // TODO: клонировать шаблон
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Клонировать")
            }
        }
    }
}