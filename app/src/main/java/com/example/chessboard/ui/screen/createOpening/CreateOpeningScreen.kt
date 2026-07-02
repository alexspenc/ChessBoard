package com.example.chessboard.ui.screen.createOpening

/**
 * Pure UI for the create-opening screen.
 *
 * Keep in this file:
 * - composable layout and visual subcomponents for the create-opening screen
 * - small UI-only helpers used only by this screen
 * - rendering logic that depends only on parameters already prepared by the container
 *
 * It is acceptable to add here:
 * - new visual blocks of this screen
 * - small private UI helper composables
 * - UI-only formatting helpers
 *
 * Do not add here:
 * - database calls, service orchestration, or coroutine-based save flows
 * - navigation decisions beyond invoking callbacks passed from the container
 * - post-save business flow for creating trainings or templates
 */
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.chessboard.R
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.ui.CreateOpeningContentTestTag
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTextField
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.ChessBoardSection
import com.example.chessboard.ui.components.HomeIconButton
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.components.PasteInputBlock
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.screen.EditableLineSide
import com.example.chessboard.ui.components.LineMoveTreeSection
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TrainingAccentTeal
import kotlinx.coroutines.launch

internal data class CreateOpeningScreenState(
    val selectedSide: EditableLineSide,
    val openingName: String,
    val ecoCode: String,
    val showOpeningNameError: Boolean,
    val pgnText: String,
    val importedUciLines: List<List<String>>,
    val importedChapterCount: Int,
    val pgnImportError: String?,
    val saveError: String?,
)

internal data class CreateOpeningScreenActions(
    val onSideSelected: (EditableLineSide) -> Unit,
    val onBackClick: () -> Unit,
    val onHomeClick: () -> Unit,
    val onOpeningNameChange: (String) -> Unit,
    val onEcoCodeChange: (String) -> Unit,
    val onPgnTextChange: (String) -> Unit,
    val onPgnImportErrorDismiss: () -> Unit,
    val onSaveErrorDismiss: () -> Unit,
    val onImportFromFileClick: () -> Unit,
    val onSave: (scrollToNameField: () -> Unit) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateOpeningScreen(
    lineController: LineController,
    state: CreateOpeningScreenState,
    actions: CreateOpeningScreenActions,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val nameFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    if (state.pgnImportError != null) {
        AppMessageDialog(
            title = stringResource(R.string.create_opening_import_failed_title),
            message = state.pgnImportError,
            onDismiss = actions.onPgnImportErrorDismiss
        )
    }
    if (state.saveError != null) {
        AppMessageDialog(
            title = stringResource(R.string.create_opening_save_failed_title),
            message = state.saveError,
            onDismiss = actions.onSaveErrorDismiss
        )
    }

    AppScreenScaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag(CreateOpeningContentTestTag),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.create_opening_title),
                subtitleLines = listOf(stringResource(R.string.create_opening_subtitle)),
                onBackClick = actions.onBackClick,
                handleSystemBack = true,
                actions = {
                    HomeIconButton(onClick = actions.onHomeClick)
                    IconButton(
                        onClick = {
                            actions.onSave {
                                coroutineScope.launch {
                                    scrollState.animateScrollTo(0)
                                    nameFocusRequester.requestFocus()
                                }
                            }
                        },
                    ) {
                        IconMd(
                            imageVector = Icons.Default.Save,
                            contentDescription = stringResource(R.string.create_opening_save_content_description),
                            tint = TrainingAccentTeal,
                        )
                    }
                }
            )
        },
        bottomBar = {
            CreateOpeningBoardControlsBar(
                selectedSide = state.selectedSide,
                onSideSelected = actions.onSideSelected,
                canUndo = lineController.canUndo,
                canRedo = lineController.canRedo,
                onUndoClick = { lineController.undoMove() },
                onResetClick = { lineController.resetToStartPosition() },
                onRedoClick = { lineController.redoMove() },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))

            ScreenSection {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
                    verticalAlignment = Alignment.Bottom
                ) {
                    AppTextField(
                        value = state.openingName,
                        onValueChange = actions.onOpeningNameChange,
                        placeholder = stringResource(R.string.create_opening_name_placeholder),
                        label = stringResource(R.string.create_opening_name_label),
                        isError = state.showOpeningNameError,
                        focusRequester = nameFocusRequester,
                        modifier = Modifier.weight(1f)
                    )
                    AppTextField(
                        value = state.ecoCode,
                        onValueChange = actions.onEcoCodeChange,
                        placeholder = stringResource(R.string.create_opening_eco_placeholder),
                        label = stringResource(R.string.create_opening_eco_label),
                        modifier = Modifier.width(96.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.spaceLg))

            ScreenSection {
                PasteInputBlock(
                    title = stringResource(R.string.create_opening_import_pgn_title),
                    text = state.pgnText,
                    onTextChange = actions.onPgnTextChange,
                    placeholder = stringResource(R.string.create_opening_import_pgn_placeholder),
                    badge = if (state.importedChapterCount > 1) {
                        pluralStringResource(
                            R.plurals.create_opening_chapters_count,
                            state.importedChapterCount,
                            state.importedChapterCount,
                        )
                    } else {
                        null
                    },
                    onImportFromFileClick = actions.onImportFromFileClick,
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.spaceLg))

            ScreenSection {
                ChessBoardSection(lineController = lineController)
            }

            Spacer(modifier = Modifier.height(AppDimens.spaceXs))

            ScreenSection {
                LineMoveTreeSection(
                    importedUciLines = state.importedUciLines,
                    lineController = lineController
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
        }
    }
}
