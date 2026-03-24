package com.example.chessboard.ui.screen.trainingTemplateScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.example.chessboard.ui.theme.TrainingBackgroundDark
import com.example.chessboard.ui.theme.TrainingCardDark
import com.example.chessboard.ui.theme.TrainingTextPrimary
import com.example.chessboard.ui.theme.TrainingTextSecondary
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingSuccessGreen
import com.example.chessboard.ui.theme.TrainingErrorRed

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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TrainingBackgroundDark)
            .padding(16.dp)
    ) {

        Text(
            text = "Сторона",
            color = TrainingTextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip("WHITE", selectedSide, onSideChange)
            FilterChip("BLACK", selectedSide, onSideChange)
            FilterChip("ANY", selectedSide, onSideChange)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Диапазон партий",
            color = TrainingTextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Button(
                onClick = {
                    val newValue = (rangeStart - 20).coerceAtLeast(0)
                    onRangeChange(newValue)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrainingCardDark,
                    contentColor = TrainingTextPrimary
                )
            ) {
                Text("<")
            }

            Text(
                text = "${rangeStart + 1}-${rangeStart + 20}",
                color = TrainingTextSecondary
            )

            Button(
                onClick = {
                    onRangeChange(rangeStart + 20)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrainingCardDark,
                    contentColor = TrainingTextPrimary
                )
            ) {
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
    val isSelected = value == selected

    AssistChip(
        onClick = { onClick(value) },
        label = {
            Text(
                value,
                color = TrainingTextPrimary
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (isSelected)
                TrainingAccentTeal
            else
                TrainingCardDark
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
            .heightIn(max = 200.dp)
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
    Card(
        colors = CardDefaults.cardColors(
            containerColor = TrainingCardDark
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Column {
                Text(
                    text = game.name,
                    color = TrainingTextPrimary
                )

                Text(
                    text = "ID: ${game.id}",
                    color = TrainingTextSecondary
                )
            }

            Button(
                onClick = { onAddClick(game) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrainingSuccessGreen,
                    contentColor = TrainingTextPrimary
                )
            ) {
                Text("+")
            }
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
            .heightIn(max = 200.dp)
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
    Card(
        colors = CardDefaults.cardColors(
            containerColor = TrainingCardDark
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Column {

                Text(
                    text = game.name,
                    color = TrainingTextPrimary
                )

                Text(
                    text = "ID: ${game.id}",
                    color = TrainingTextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {

                    IconButton(
                        onClick = {
                            val newWeight = (game.weight - 1).coerceAtLeast(1)
                            onWeightChange(game, newWeight)
                        }
                    ) {
                        Text("-", color = TrainingTextPrimary)
                    }

                    Text(
                        text = "${game.weight}",
                        color = TrainingTextPrimary
                    )

                    IconButton(
                        onClick = {
                            onWeightChange(game, game.weight + 1)
                        }
                    ) {
                        Text("+", color = TrainingTextPrimary)
                    }
                }
            }

            IconButton(
                onClick = { onRemove(game) }
            ) {
                Text(
                    text = "✕",
                    color = TrainingErrorRed
                )
            }
        }
    }
}