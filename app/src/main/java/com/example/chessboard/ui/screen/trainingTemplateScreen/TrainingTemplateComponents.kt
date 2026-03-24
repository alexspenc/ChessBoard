package com.example.chessboard.ui.screen.trainingTemplateScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment

// ----------------------
// SEARCH SETTINGS
// ----------------------

@Composable
fun SearchSettingsSection(
    selectedSide: String,
    onSideChange: (String) -> Unit,
    rangeStart: Int,
    onRangeChange: (Int) -> Unit
) {
    Column {

        Text("Сторона")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip("WHITE", selectedSide, onSideChange)
            FilterChip("BLACK", selectedSide, onSideChange)
            FilterChip("ANY", selectedSide, onSideChange)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Диапазон партий")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val newValue = (rangeStart - 20).coerceAtLeast(0)
                onRangeChange(newValue)
            }) {
                Text("<")
            }

            Text("${rangeStart + 1}-${rangeStart + 20}")

            Button(onClick = {
                onRangeChange(rangeStart + 20)
            }) {
                Text(">")
            }
        }
    }
}

@Composable
private fun FilterChip(
    value: String,
    selected: String,
    onClick: (String) -> Unit
) {
    AssistChip(
        onClick = { onClick(value) },
        label = { Text(value) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (value == selected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surface
        )
    )
}

// ----------------------
// CANDIDATES
// ----------------------

@Composable
fun CandidatesList(
    games: List<GameUiModel>,
    onAddClick: (GameUiModel) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        items(games) { game ->
            CandidateItem(game, onAddClick)
        }
    }
}

@Composable
fun CandidateItem(
    game: GameUiModel,
    onAddClick: (GameUiModel) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(game.name)
            Text("ID: ${game.id}", style = MaterialTheme.typography.bodySmall)
        }

        Button(onClick = { onAddClick(game) }) {
            Text("+")
        }
    }
}

// ----------------------
// SELECTED
// ----------------------

@Composable
fun SelectedGamesList(
    games: List<TemplateGameUiModel>,
    onRemove: (TemplateGameUiModel) -> Unit,
    onWeightChange: (TemplateGameUiModel, Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        items(games) { game ->
            SelectedGameItem(game, onRemove, onWeightChange)
        }
    }
}

@Composable
fun SelectedGameItem(
    game: TemplateGameUiModel,
    onRemove: (TemplateGameUiModel) -> Unit,
    onWeightChange: (TemplateGameUiModel, Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(game.name)
            Text("ID: ${game.id}", style = MaterialTheme.typography.bodySmall)

            Row(verticalAlignment = Alignment.CenterVertically) {

                IconButton(
                    onClick = {
                        val newWeight = (game.weight - 1).coerceAtLeast(1)
                        onWeightChange(game, newWeight)
                    }
                ) {
                    Text("-")
                }

                Text("${game.weight}")

                IconButton(
                    onClick = {
                        onWeightChange(game, game.weight + 1)
                    }
                ) {
                    Text("+")
                }
            }
        }

        IconButton(onClick = { onRemove(game) }) {
            Text("❌")
        }
    }
}