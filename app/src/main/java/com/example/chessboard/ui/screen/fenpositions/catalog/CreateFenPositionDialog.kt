package com.example.chessboard.ui.screen.fenpositions.catalog

/*
 * File role: renders and validates the add-position dialog for the FEN catalog.
 * Allowed here:
 * - dialog-local form state, debounced FEN validation, and read-only board preview
 * - emitting one validated create request to the container
 * Not allowed here:
 * - Room/service calls, catalog reloads, or app navigation
 * Validation date: 2026-08-31
 */

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.chessboard.boardmodel.InitialBoardFenWithoutMoveNumbers
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.service.normalizeValidFenPosition
import com.example.chessboard.ui.components.AppTextField
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.ChessBoardSection
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.ScreenTitleText
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCreateConfirmTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCreateDescriptionInputTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCreateDialogTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCreateFenInputTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCreateNameInputTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCreatePreviewBoardTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCreateThemeInputTestTag
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TrainingErrorRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val FenValidationDebounceMs = 600L
private const val PreviewBoardScreenFraction = 0.7f
private const val DialogScreenHeightFraction = 0.92f

internal data class CreateFenPositionRequest(
    val fen: String,
    val name: String,
    val theme: String,
    val description: String,
)

private data class EvaluatedFen(
    val source: String = "",
    val normalizedFen: String? = null,
)

@Composable
internal fun CreateFenPositionDialog(
    strings: FenPositionCreateDialogStrings,
    isSaving: Boolean,
    saveErrorMessage: String?,
    onInputChanged: () -> Unit,
    onDismiss: () -> Unit,
    onCreate: (CreateFenPositionRequest) -> Unit,
) {
    var fen by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var theme by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var evaluatedFen by remember { mutableStateOf(EvaluatedFen()) }
    var hasAttemptedCreate by remember { mutableStateOf(false) }

    LaunchedEffect(fen) {
        if (fen.isBlank()) {
            evaluatedFen = EvaluatedFen()
            return@LaunchedEffect
        }

        delay(FenValidationDebounceMs)
        val source = fen
        val normalizedFen = withContext(Dispatchers.Default) {
            normalizeValidFenPosition(source)
        }
        evaluatedFen = EvaluatedFen(
            source = source,
            normalizedFen = normalizedFen,
        )
    }

    val isFenPending = fen.isNotBlank() && evaluatedFen.source != fen
    val isFenInvalid = fen.isNotBlank() && !isFenPending && evaluatedFen.normalizedFen == null
    val showFenError = isFenInvalid || (hasAttemptedCreate && fen.isBlank())
    val showThemeError = hasAttemptedCreate && theme.isBlank()
    val canAttemptCreate = !isSaving && !isFenPending

    fun submit() {
        hasAttemptedCreate = true
        val normalizedFen = evaluatedFen.normalizedFen ?: return
        if (theme.isBlank()) {
            return
        }

        onCreate(
            CreateFenPositionRequest(
                fen = normalizedFen,
                name = name,
                theme = theme,
                description = description,
            ),
        )
    }

    val configuration = LocalConfiguration.current
    val smallestScreenSideDp = minOf(
        configuration.screenWidthDp,
        configuration.screenHeightDp,
    )
    val previewBoardSize = (smallestScreenSideDp * PreviewBoardScreenFraction).dp
    val dialogMaxHeight = (configuration.screenHeightDp * DialogScreenHeightFraction).dp

    Dialog(
        onDismissRequest = {
            if (!isSaving) {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = dialogMaxHeight)
                .testTag(FenPositionCreateDialogTestTag),
            shape = RoundedCornerShape(AppDimens.radiusXl),
            color = Background.ScreenDark,
        ) {
            Column(
                modifier = Modifier.padding(AppDimens.spaceLg),
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
            ) {
                ScreenTitleText(text = strings.title)

                Column(
                    modifier = Modifier
                        .weight(weight = 1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
                ) {
                    AppTextField(
                        value = fen,
                        onValueChange = { value ->
                            fen = value
                            onInputChanged()
                        },
                        label = strings.fenLabel,
                        placeholder = strings.fenPlaceholder,
                        isError = showFenError,
                        minLines = 2,
                        inputTestTag = FenPositionCreateFenInputTestTag,
                    )

                    FenPositionPreview(
                        normalizedFen = evaluatedFen.normalizedFen,
                        isPending = isFenPending,
                        isInvalid = isFenInvalid,
                        strings = strings,
                        boardSize = previewBoardSize,
                    )

                    AppTextField(
                        value = name,
                        onValueChange = { value ->
                            name = value
                            onInputChanged()
                        },
                        label = strings.nameLabel,
                        placeholder = strings.namePlaceholder,
                        inputTestTag = FenPositionCreateNameInputTestTag,
                    )
                    AppTextField(
                        value = theme,
                        onValueChange = { value ->
                            theme = value
                            onInputChanged()
                        },
                        label = strings.themeLabel,
                        placeholder = strings.themePlaceholder,
                        isError = showThemeError,
                        inputTestTag = FenPositionCreateThemeInputTestTag,
                    )
                    AppTextField(
                        value = description,
                        onValueChange = { value ->
                            description = value
                            onInputChanged()
                        },
                        label = strings.descriptionLabel,
                        placeholder = strings.descriptionPlaceholder,
                        minLines = 3,
                        inputTestTag = FenPositionCreateDescriptionInputTestTag,
                    )

                    if (saveErrorMessage != null) {
                        BodySecondaryText(
                            text = saveErrorMessage,
                            color = TrainingErrorRed,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isSaving,
                    ) {
                        CardMetaText(text = strings.cancel)
                    }
                    PrimaryButton(
                        text = strings.add,
                        onClick = ::submit,
                        enabled = canAttemptCreate,
                        modifier = Modifier.testTag(FenPositionCreateConfirmTestTag),
                    )
                }
            }
        }
    }
}

@Composable
private fun FenPositionPreview(
    normalizedFen: String?,
    isPending: Boolean,
    isInvalid: Boolean,
    strings: FenPositionCreateDialogStrings,
    boardSize: Dp,
) {
    val previewFen = normalizedFen ?: InitialBoardFenWithoutMoveNumbers
    val lineController = remember(previewFen) {
        LineController().also { controller ->
            controller.loadPreviewFen(toLoadableFenPosition(previewFen))
            controller.setOrientation(resolveFenPositionBoardOrientation(previewFen))
            controller.setUserMovesEnabled(false)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
    ) {
        Box(modifier = Modifier.size(boardSize)) {
            ChessBoardSection(
                lineController = lineController,
                modifier = Modifier.testTag(FenPositionCreatePreviewBoardTestTag),
            )
        }

        FenPositionPreviewStatus(
            normalizedFen = normalizedFen,
            isPending = isPending,
            isInvalid = isInvalid,
            strings = strings,
        )
    }
}

@Composable
private fun FenPositionPreviewStatus(
    normalizedFen: String?,
    isPending: Boolean,
    isInvalid: Boolean,
    strings: FenPositionCreateDialogStrings,
) {
    val statusText = when {
        isPending -> strings.checkingFen
        isInvalid -> strings.invalidFen
        normalizedFen == null -> strings.previewPrompt
        else -> return
    }
    if (isInvalid) {
        BodySecondaryText(
            text = statusText,
            color = TrainingErrorRed,
        )
        return
    }

    BodySecondaryText(text = statusText)
}
