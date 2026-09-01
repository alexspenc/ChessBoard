package com.example.chessboard.ui.screen.fenpositions.details

/*
 * File role: coordinates editing one loaded FEN position from the details screen.
 * Allowed here:
 * - edit-dialog state, asynchronous service calls, progress, and failure dialogs
 * - reporting successful updates or a concurrently removed position to the container
 * Not allowed here:
 * - app navigation, details layout, FEN changes, or direct Room access
 * Validation date: 2026-09-02
 */

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.chessboard.service.FenPositionService
import com.example.chessboard.service.UpdateFenPositionResult
import com.example.chessboard.ui.components.AppLoadingDialog
import com.example.chessboard.ui.components.AppMessageDialog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun FenPositionEditFlow(
    position: FenPositionDetailsItem,
    fenPositionService: FenPositionService,
    strings: FenPositionEditDialogStrings,
    onDismiss: () -> Unit,
    onUpdated: () -> Unit,
    onPositionMissing: () -> Unit,
) {
    var isSaving by remember(position.id) { mutableStateOf(false) }
    var failureMessage by remember(position.id) { mutableStateOf<String?>(null) }
    var positionMissing by remember(position.id) { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    fun save(request: EditFenPositionRequest) {
        isSaving = true
        failureMessage = null
        positionMissing = false
        coroutineScope.launch {
            val result = try {
                withContext(Dispatchers.IO) {
                    fenPositionService.updateDetails(
                        positionId = position.id,
                        name = request.name,
                        theme = request.theme,
                        description = request.description,
                    )
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                null
            }

            isSaving = false
            when (result) {
                null -> failureMessage = strings.saveFailed
                UpdateFenPositionResult.BlankTheme -> {
                    failureMessage = strings.themeRequired
                }
                UpdateFenPositionResult.PositionNotFound -> {
                    positionMissing = true
                    failureMessage = strings.positionNotFound
                }
                UpdateFenPositionResult.Success -> onUpdated()
            }
        }
    }

    EditFenPositionDialog(
        initialName = position.name,
        initialTheme = position.theme,
        initialDescription = position.description.orEmpty(),
        strings = strings,
        isSaving = isSaving,
        onDismiss = onDismiss,
        onSave = ::save,
    )

    if (isSaving) {
        AppLoadingDialog(
            title = strings.savingTitle,
            message = strings.savingMessage,
        )
    }

    val currentFailureMessage = failureMessage
    if (currentFailureMessage != null) {
        AppMessageDialog(
            title = strings.saveFailedTitle,
            message = currentFailureMessage,
            onDismiss = {
                failureMessage = null
                if (positionMissing) {
                    positionMissing = false
                    onPositionMissing()
                }
            },
        )
    }
}
