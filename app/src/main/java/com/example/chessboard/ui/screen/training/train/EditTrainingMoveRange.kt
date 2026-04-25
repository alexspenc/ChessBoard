package com.example.chessboard.ui.screen.training.train

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import kotlin.math.roundToInt

private const val MinMove = 1
internal const val DefaultMaxMove = 40

internal data class TrainingMoveRange(
    val from: Int = 1,
    val to: Int = 0,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditTrainingMoveRangeSection(
    moveRange: TrainingMoveRange,
    onMoveRangeChange: (TrainingMoveRange) -> Unit,
    maxMove: Int = DefaultMaxMove,
    modifier: Modifier = Modifier,
) {
    val clampedFrom = moveRange.from.coerceIn(MinMove, maxMove - 1)
    val toEffective = if (moveRange.to == 0) maxMove
                      else moveRange.to.coerceIn(clampedFrom + 1, maxMove)
    val sliderValue = clampedFrom.toFloat()..toEffective.toFloat()
    val fromLabel = "$clampedFrom"
    val toLabel = if (moveRange.to == 0) "Last" else "${moveRange.to}"
    val fromFraction = (clampedFrom - MinMove).toFloat() / (maxMove - MinMove).coerceAtLeast(1)
    val toFraction = (toEffective - MinMove).toFloat() / (maxMove - MinMove).coerceAtLeast(1)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "From: $fromLabel",
                color = TextColor.Primary,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "To: $toLabel",
                color = TextColor.Primary,
                style = MaterialTheme.typography.titleSmall,
            )
        }

        RangeSlider(
            modifier = Modifier.fillMaxWidth(),
            value = sliderValue,
            onValueChange = { range ->
                val newFrom = range.start.roundToInt().coerceIn(MinMove, maxMove - 1)
                val newToRaw = range.endInclusive.roundToInt().coerceIn(newFrom + 1, maxMove)
                val newTo = if (newToRaw >= maxMove) 0 else newToRaw
                onMoveRangeChange(TrainingMoveRange(from = newFrom, to = newTo))
            },
            valueRange = MinMove.toFloat()..maxMove.toFloat(),
            steps = (maxMove - MinMove - 1).coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = TrainingAccentTeal,
                activeTrackColor = TrainingAccentTeal,
                inactiveTrackColor = TrainingAccentTeal.copy(alpha = 0.24f),
            )
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = fromLabel,
                color = TextColor.Secondary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.centeredAtFraction(fromFraction),
            )
            Text(
                text = toLabel,
                color = TextColor.Secondary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.centeredAtFraction(toFraction),
            )
        }
    }
}

private fun Modifier.centeredAtFraction(fraction: Float): Modifier =
    this.layout { measurable, constraints ->
        val placeable = measurable.measure(
            constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity)
        )
        val thumbRadiusPx = 10.dp.toPx()
        val trackWidthPx = constraints.maxWidth - 2f * thumbRadiusPx
        val centerPx = thumbRadiusPx + trackWidthPx * fraction
        layout(constraints.maxWidth, placeable.height) {
            placeable.placeRelative(
                x = (centerPx - placeable.width / 2f).roundToInt(),
                y = 0,
            )
        }
    }
