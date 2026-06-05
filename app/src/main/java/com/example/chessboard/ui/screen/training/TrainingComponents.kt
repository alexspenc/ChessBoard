package com.example.chessboard.ui.screen.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.chessboard.R
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.ui.components.AppTextField
import com.example.chessboard.ui.components.IconSm
import com.example.chessboard.ui.components.MoveSequenceSection
import com.example.chessboard.ui.components.MoveSequenceSectionStrings
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.ButtonColor
import com.example.chessboard.ui.theme.TextColor

// ──────────────────────────────────────────────────────────────────────────────
// Shared composables
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun DarkInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    minLines: Int = 1,
    focusRequester: FocusRequester? = null,
) {
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        modifier = modifier,
        isError = isError,
        minLines = minLines,
        focusRequester = focusRequester,
    )
}


@Composable
fun MoveLegendSection(
    moveLabels: List<String>,
    currentPly: Int,
    isSelectionEnabled: Boolean,
    onMovePlyClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    strings: MoveSequenceSectionStrings? = null,
    showNavControls: Boolean = isSelectionEnabled,
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    onPrevMoveClick: () -> Unit = {},
    onNextMoveClick: () -> Unit = {},
    onResetMovesClick: () -> Unit = {},
) {
    val resolvedStrings = strings ?: MoveSequenceSectionStrings(
        title = stringResource(R.string.move_legend_title),
        emptyText = stringResource(R.string.move_legend_empty),
        previousMoveContentDescription = stringResource(
            R.string.move_sequence_previous_move_content_description
        ),
        resetLabel = stringResource(R.string.common_reset),
        nextMoveContentDescription = stringResource(
            R.string.move_sequence_next_move_content_description
        ),
    )

    MoveSequenceSection(
        moveLabels = moveLabels,
        currentPly = currentPly,
        isSelectionEnabled = isSelectionEnabled,
        showNavControls = showNavControls,
        canUndo = canUndo,
        canRedo = canRedo,
        onMovePlyClick = onMovePlyClick,
        onPrevMoveClick = onPrevMoveClick,
        onNextMoveClick = onNextMoveClick,
        onResetMovesClick = onResetMovesClick,
        modifier = modifier,
        strings = resolvedStrings,
    )
}

@Composable
fun TrainingActionButtons(
    onSaveLine: () -> Unit,
    onDatabaseClear: () -> Unit,
    lineController: LineController,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Primary actions: Save line, Clear database
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrimaryButton("Save line", onClick = onSaveLine)
            PrimaryButton("Clear database", onClick = onDatabaseClear)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Secondary actions: Back, Forward, Reset
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrimaryButton("Back", onClick = { lineController.undoMove() })
            PrimaryButton("Forward", onClick = { lineController.redoMove() })
            PrimaryButton("Reset", onClick = { lineController.resetToStartPosition() })
        }
    }
}

@Composable
fun ResetTrainingButton(
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onResetClick,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(AppDimens.radiusLg),
        colors = ButtonDefaults.buttonColors(
            containerColor = Background.SurfaceDark,
            contentColor = ButtonColor.Content
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp
        )
    ) {
        IconSm(
            imageVector = Icons.Default.Refresh,
            contentDescription = stringResource(R.string.common_reset),
        )
        Spacer(modifier = Modifier.width(8.dp))
        SectionTitleText(text = stringResource(R.string.training_reset_title), color = TextColor.Primary)
    }
}
