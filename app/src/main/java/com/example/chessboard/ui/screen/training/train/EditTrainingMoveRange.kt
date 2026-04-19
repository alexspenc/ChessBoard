package com.example.chessboard.ui.screen.training.train

/*
 * Training-only move range state and UI for EditTrainingScreen.
 *
 * Keep the transient "from / to" training launch controls here so the shared
 * training editor pieces do not depend on training-specific launch settings.
 * Do not add save/load helpers or generic editor UI to this file.
 */

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal

internal data class TrainingMoveRange(
    val from: Int = 1,
    val to: Int = 0,
) {
    fun decreaseFrom(): TrainingMoveRange {
        if (from <= 1) {
            return this
        }

        return copy(from = from - 1)
    }

    fun increaseFrom(): TrainingMoveRange {
        if (to != 0 && from >= to) {
            return this
        }

        return copy(from = from + 1)
    }

    fun decreaseTo(): TrainingMoveRange {
        if (to <= 0) {
            return this
        }

        val nextTo = to - 1
        if (nextTo == 0) {
            return copy(to = 0)
        }

        return copy(
            from = minOf(from, nextTo),
            to = nextTo,
        )
    }

    fun increaseTo(): TrainingMoveRange {
        if (to == 0) {
            return copy(to = from)
        }

        return copy(to = to + 1)
    }
}

@Composable
internal fun EditTrainingMoveRangeSection(
    moveRange: TrainingMoveRange,
    onMoveRangeChange: (TrainingMoveRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
    ) {
        MoveRangeControl(
            label = "From:",
            displayText = "${moveRange.from}",
            onDecrement = {
                onMoveRangeChange(moveRange.decreaseFrom())
            },
            onIncrement = {
                onMoveRangeChange(moveRange.increaseFrom())
            }
        )
        MoveRangeControl(
            label = "To:",
            displayText = if (moveRange.to == 0) "All" else "${moveRange.to}",
            onDecrement = {
                onMoveRangeChange(moveRange.decreaseTo())
            },
            onIncrement = {
                onMoveRangeChange(moveRange.increaseTo())
            }
        )
    }
}

@Composable
private fun MoveRangeControl(
    label: String,
    displayText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)
    ) {
        BodySecondaryText(text = label)
        IconButton(onClick = onDecrement, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Decrease $label",
                tint = TrainingAccentTeal,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(min = 36.dp)
        ) {
            Text(
                text = displayText,
                color = TextColor.Primary,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = "move",
                color = TextColor.Secondary,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
        IconButton(onClick = onIncrement, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Increase $label",
                tint = TrainingAccentTeal,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
