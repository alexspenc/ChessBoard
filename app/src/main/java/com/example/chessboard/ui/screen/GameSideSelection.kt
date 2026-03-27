package com.example.chessboard.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.chessboard.entity.SideMask
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.PillSurface
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingCardDark
import com.example.chessboard.ui.theme.TrainingDividerColor
import com.example.chessboard.ui.theme.TrainingTextPrimary
import com.example.chessboard.ui.theme.TrainingTextSecondary

internal enum class EditableGameSide(
    val sideMask: Int,
    val orientation: BoardOrientation,
    private val displayLabel: String
) {
    AS_WHITE(
        sideMask = SideMask.WHITE,
        orientation = BoardOrientation.WHITE,
        displayLabel = "As White"
    ),
    AS_BLACK(
        sideMask = SideMask.BLACK,
        orientation = BoardOrientation.BLACK,
        displayLabel = "As Black"
    ),
    AS_BOTH(
        sideMask = SideMask.BOTH,
        orientation = BoardOrientation.WHITE,
        displayLabel = "As Both"
    );

    fun toDisplayText(): String = displayLabel

    companion object {
        fun fromSideMask(sideMask: Int): EditableGameSide {
            if (sideMask == SideMask.WHITE) {
                return AS_WHITE
            }

            if (sideMask == SideMask.BLACK) {
                return AS_BLACK
            }

            return AS_BOTH
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
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier
    ) {
        SectionTitleText(
            text = "Selected side",
            color = TrainingTextSecondary
        )

        androidx.compose.foundation.layout.Spacer(
            modifier = Modifier.height(AppDimens.spaceSm)
        )

        PillSurface(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(AppDimens.radiusXs)
        ) {
            EditableGameSide.entries.forEach { side ->
                val visualState = SideSelectionVisualState(
                    side = side,
                    selectedSide = selectedSide
                )

                CardSurface(
                    modifier = Modifier.weight(1f),
                    color = resolveSideSelectionColor(visualState),
                    border = resolveSideSelectionBorder(visualState),
                    contentPadding = PaddingValues(
                        horizontal = AppDimens.spaceMd,
                        vertical = AppDimens.spaceSm
                    ),
                    onClick = { onSideSelected(side) }
                ) {
                    Text(
                        text = side.toDisplayText(),
                        color = TrainingTextPrimary,
                        fontWeight = resolveSideSelectionFontWeight(visualState)
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
        return TrainingAccentTeal
    }

    return TrainingCardDark
}

private fun resolveSideSelectionBorder(
    visualState: SideSelectionVisualState
): BorderStroke? {
    if (visualState.side == visualState.selectedSide) {
        return null
    }

    return BorderStroke(
        width = AppDimens.dividerThickness,
        color = TrainingDividerColor
    )
}

private fun resolveSideSelectionFontWeight(
    visualState: SideSelectionVisualState
): FontWeight {
    if (visualState.side == visualState.selectedSide) {
        return FontWeight.SemiBold
    }

    return FontWeight.Normal
}
