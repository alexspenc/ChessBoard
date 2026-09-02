package com.example.chessboard.ui.screen.fenpositions.continuations

/*
 * File role: renders the pure add-FEN-position-continuations screen from prepared UI state.
 * Allowed here:
 * - screen layout, board/input availability, SAN previews, and forwarding required actions
 * Not allowed here:
 * - PGN parsing, debounce orchestration, persistence, or app navigation decisions
 * Validation date: 2026-09-02
 */

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.ui.components.AppDivider
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.ChessBoardSection
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.ScreenTitleText
import com.example.chessboard.ui.components.SecondaryButton
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationAddBoardTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationAddManualBackTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationAddManualClearTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationAddSaveTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationAddTextClearTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionContinuationAddTextInputTestTag
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.MutedContentColor
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal

internal data class AddFenPositionContinuationsScreenState(
    val positionName: String,
    val theme: String,
    val manualSanLine: String,
    val text: String,
    val newContinuationSanLines: List<String>,
    val canUndoManualLine: Boolean,
    val canClearManualLine: Boolean,
    val canSave: Boolean,
    val dialogState: AddFenPositionContinuationsDialogState?,
)

internal data class AddFenPositionContinuationsScreenActions(
    val onBackClick: () -> Unit,
    val onManualBackClick: () -> Unit,
    val onManualClearClick: () -> Unit,
    val onTextChange: (String) -> Unit,
    val onTextClearClick: () -> Unit,
    val onSaveClick: () -> Unit,
    val dialogActions: AddFenPositionContinuationsDialogActions,
)

@Composable
internal fun AddFenPositionContinuationsScreen(
    lineController: LineController,
    state: AddFenPositionContinuationsScreenState,
    actions: AddFenPositionContinuationsScreenActions,
    modifier: Modifier = Modifier,
) {
    val strings = addFenPositionContinuationsStrings()
    val isProcessing = state.dialogState is AddFenPositionContinuationsDialogState.Processing
    val hasManualLine = state.manualSanLine.isNotBlank()
    val hasText = state.text.isNotBlank()
    val boardInputEnabled = !hasText && !isProcessing
    val textInputEnabled = !hasManualLine && !isProcessing

    SideEffect {
        lineController.setUserMovesEnabled(boardInputEnabled)
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = strings.screenTitle,
                onBackClick = actions.onBackClick,
                handleSystemBack = true,
                filledBackButton = true,
            )
        },
        bottomBar = {
            SaveBottomBar(
                text = strings.save,
                enabled = state.canSave && !isProcessing,
                onClick = actions.onSaveClick,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = AppDimens.spaceLg,
                    vertical = AppDimens.spaceLg,
                ),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
        ) {
            PositionHeader(
                name = strings.positionName(state.positionName),
                theme = strings.theme(state.theme),
            )
            // TODO: Add a promotion-piece picker to the shared interactive board. Until then a
            // manually entered continuation cannot advance past a pawn-promotion move; the
            // planned pasted PGN/SAN input will be the way to add such a continuation.
            ChessBoardSection(
                lineController = lineController,
                modifier = Modifier
                    .testTag(FenPositionContinuationAddBoardTestTag)
                    .semantics {
                        if (!boardInputEnabled) {
                            disabled()
                        }
                    },
            )
            if (hasText) {
                BodySecondaryText(text = strings.boardDisabledMessage)
            }
            ManualLineSection(
                sanLine = state.manualSanLine,
                canUndo = state.canUndoManualLine && !isProcessing,
                canClear = state.canClearManualLine && !isProcessing,
                strings = strings,
                onBackClick = actions.onManualBackClick,
                onClearClick = actions.onManualClearClick,
            )
            ContinuationTextSection(
                text = state.text,
                enabled = textInputEnabled,
                blockedByManualLine = hasManualLine,
                strings = strings,
                onTextChange = actions.onTextChange,
                onClearClick = actions.onTextClearClick,
            )
            NewContinuationsSection(
                sanLines = state.newContinuationSanLines,
                strings = strings,
            )
        }
    }

    AddFenPositionContinuationsDialogs(
        dialogState = state.dialogState,
        strings = strings,
        actions = actions.dialogActions,
    )
}

@Composable
private fun PositionHeader(
    name: String,
    theme: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)) {
        ScreenTitleText(text = name)
        CardMetaText(text = theme)
    }
}

@Composable
private fun ManualLineSection(
    sanLine: String,
    canUndo: Boolean,
    canClear: Boolean,
    strings: AddFenPositionContinuationsStrings,
    onBackClick: () -> Unit,
    onClearClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
    ) {
        SmallSectionTitle(text = strings.manualLineTitle)
        if (sanLine.isNotBlank()) {
            MonospaceSanText(text = sanLine)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
        ) {
            SecondaryButton(
                text = strings.manualBack,
                onClick = onBackClick,
                enabled = canUndo,
                modifier = Modifier
                    .weight(1f)
                    .testTag(FenPositionContinuationAddManualBackTestTag),
            )
            SecondaryButton(
                text = strings.manualClear,
                onClick = onClearClick,
                enabled = canClear,
                modifier = Modifier
                    .weight(1f)
                    .testTag(FenPositionContinuationAddManualClearTestTag),
            )
        }
    }
}

@Composable
private fun ContinuationTextSection(
    text: String,
    enabled: Boolean,
    blockedByManualLine: Boolean,
    strings: AddFenPositionContinuationsStrings,
    onTextChange: (String) -> Unit,
    onClearClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
        SmallSectionTitle(text = strings.textTitle)
        ContinuationTextInput(
            text = text,
            enabled = enabled,
            placeholder = strings.textPlaceholder,
            onTextChange = onTextChange,
        )
        if (blockedByManualLine) {
            BodySecondaryText(text = strings.textDisabledMessage)
        }
        if (text.isNotBlank()) {
            TextButton(
                onClick = onClearClick,
                modifier = Modifier.testTag(FenPositionContinuationAddTextClearTestTag),
            ) {
                Text(text = strings.textClear)
            }
        }
    }
}

@Composable
private fun ContinuationTextInput(
    text: String,
    enabled: Boolean,
    placeholder: String,
    onTextChange: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.radiusMd),
        color = Background.SurfaceDark,
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) TrainingAccentTeal else MutedContentColor,
        ),
    ) {
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(FenPositionContinuationAddTextInputTestTag)
                .padding(
                    horizontal = 14.dp,
                    vertical = AppDimens.spaceMd,
                ),
            textStyle = MaterialTheme.typography.bodyMedium.merge(
                TextStyle(
                    color = if (enabled) TextColor.Primary else MutedContentColor,
                    fontFamily = FontFamily.Monospace,
                ),
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
            ),
            cursorBrush = SolidColor(TrainingAccentTeal),
            minLines = 4,
            maxLines = 8,
            decorationBox = { innerTextField ->
                if (text.isEmpty()) {
                    BodySecondaryText(
                        text = placeholder,
                        color = MutedContentColor,
                    )
                }
                innerTextField()
            },
        )
    }
}

@Composable
private fun NewContinuationsSection(
    sanLines: List<String>,
    strings: AddFenPositionContinuationsStrings,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
    ) {
        SmallSectionTitle(text = strings.newContinuationsTitle)
        sanLines.forEachIndexed { index, sanLine ->
            CardSurface(
                modifier = Modifier.fillMaxWidth(),
            ) {
                CardMetaText(
                    text = strings.continuationTitle(index),
                    color = TrainingAccentTeal,
                    fontWeight = FontWeight.SemiBold,
                )
                MonospaceSanText(
                    text = sanLine,
                    modifier = Modifier.padding(top = AppDimens.spaceSm),
                )
            }
        }
    }
}

@Composable
private fun SmallSectionTitle(text: String) {
    CardMetaText(
        text = text,
        color = TrainingAccentTeal,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun MonospaceSanText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        color = TextColor.Primary,
    )
}

@Composable
private fun SaveBottomBar(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Background.SurfaceDark,
        tonalElevation = 8.dp,
    ) {
        Column {
            AppDivider()
            PrimaryButton(
                text = text,
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimens.spaceMd)
                    .testTag(FenPositionContinuationAddSaveTestTag),
            )
        }
    }
}
