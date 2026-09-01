package com.example.chessboard.ui.screen.fenpositions

/*
 * File role: coordinates the confirmation, progress, and failure UI for deleting a FEN position.
 * Allowed here:
 * - calling FenPositionService.deleteById from a coroutine
 * - shared deletion dialogs used by catalog and details screens
 * Not allowed here:
 * - choosing which screen opens after deletion, catalog paging, or position presentation
 * Validation date: 2026-09-01
 */

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.chessboard.service.FenPositionService
import com.example.chessboard.ui.components.AppConfirmDialog
import com.example.chessboard.ui.components.AppLoadingDialog
import com.example.chessboard.ui.components.AppMessageDialog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun FenPositionDeletionFlow(
    positionId: Long,
    fenPositionService: FenPositionService,
    strings: FenPositionDeletionStrings,
    onDismiss: () -> Unit,
    onDeleted: () -> Unit,
) {
    var isDeleting by remember { mutableStateOf(false) }
    var deleteFailureDialogMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun dismiss() {
        if (isDeleting) {
            return
        }

        onDismiss()
    }

    fun delete() {
        isDeleting = true
        deleteFailureDialogMessage = null
        coroutineScope.launch {
            val deletionCompleted = try {
                withContext(Dispatchers.IO) {
                    fenPositionService.deleteById(positionId)
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                false
            }

            isDeleting = false
            if (!deletionCompleted) {
                deleteFailureDialogMessage = strings.failedMessage
                return@launch
            }

            onDeleted()
        }
    }

    AppConfirmDialog(
        title = strings.title,
        message = strings.message,
        confirmText = strings.confirm,
        isDestructive = true,
        onDismiss = ::dismiss,
        onConfirm = ::delete,
    )

    if (isDeleting) {
        AppLoadingDialog(
            title = strings.deletingTitle,
            message = strings.deletingMessage,
        )
    }

    val currentDeleteFailureDialogMessage = deleteFailureDialogMessage
    if (currentDeleteFailureDialogMessage != null) {
        AppMessageDialog(
            title = strings.failedTitle,
            message = currentDeleteFailureDialogMessage,
            onDismiss = {
                deleteFailureDialogMessage = null
            },
        )
    }
}
