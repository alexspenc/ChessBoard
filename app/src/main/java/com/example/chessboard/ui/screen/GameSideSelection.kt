package com.example.chessboard.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessboard.entity.SideMask
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingIconInactive
import com.example.chessboard.ui.theme.TrainingTextPrimary

internal val SideButtonSelectedBg = Color(0xFF2C2C2C)

enum class EditableGameSide(
    val sideMask: Int,
    val orientation: BoardOrientation,
    private val displayLabel: String,
    private val symbol: String
) {
    AS_WHITE(
        sideMask = SideMask.WHITE,
        orientation = BoardOrientation.WHITE,
        displayLabel = "As White",
        symbol = "♔"
    ),
    AS_BLACK(
        sideMask = SideMask.BLACK,
        orientation = BoardOrientation.BLACK,
        displayLabel = "As Black",
        symbol = "♚"
    );

    fun toDisplayText(): String = displayLabel
    fun toDisplaySymbol(): String = symbol

    companion object {
        fun fromSideMask(sideMask: Int): EditableGameSide {
            if (sideMask == SideMask.BLACK) {
                return AS_BLACK
            }

            return AS_WHITE
        }
    }
}

private data class SideSelectionVisualState(
    val side: EditableGameSide,
    val selectedSide: EditableGameSide
)

@Composable
internal fun GameSideSelector(
    selectedSide: EditableGameSide,
    onSideSelected: (EditableGameSide) -> Unit,
    showTitle: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (showTitle) {
            SectionTitleText(text = "Selected side", color = TextColor.Secondary)
            Spacer(modifier = Modifier.height(AppDimens.spaceSm))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            EditableGameSide.entries.forEach { side ->
                val visualState = SideSelectionVisualState(
                    side = side,
                    selectedSide = selectedSide
                )

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = resolveSideSelectionColor(visualState),
                            shape = RoundedCornerShape(50)
                        )
                        .clickable { onSideSelected(side) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = side.toDisplaySymbol(),
                        fontSize = 20.sp,
                        color = resolveSideSelectionContentColor(visualState)
                    )
                }
            }
        }
    }
}

private fun resolveSideSelectionColor(
    visualState: SideSelectionVisualState
): Color {
    if (visualState.side == visualState.selectedSide) {
        return SideButtonSelectedBg
    }

    return Color.Transparent
}

private fun resolveSideSelectionContentColor(
    visualState: SideSelectionVisualState
): Color {
    if (visualState.side == visualState.selectedSide) {
        return TrainingTextPrimary
    }

    return TrainingIconInactive
}
