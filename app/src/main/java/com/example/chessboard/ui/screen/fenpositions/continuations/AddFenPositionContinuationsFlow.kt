package com.example.chessboard.ui.screen.fenpositions.continuations

/*
 * File role: loads a selected FEN position and coordinates continuation persistence around the add screen.
 * Allowed here:
 * - loading the source position, invoking the continuation service, and forwarding flow results
 * Not allowed here:
 * - detailed screen layout, PGN parsing, or Room DAO access
 * Validation date: 2026-09-02
 */

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.chessboard.entity.FenPositionEntity
import com.example.chessboard.service.CreateFenPositionContinuationBatchResult
import com.example.chessboard.service.FenPositionContinuationBatchPreparation
import com.example.chessboard.service.FenPositionContinuationService
import com.example.chessboard.service.FenPositionService
import com.example.chessboard.ui.components.AppLoadingDialog
import com.example.chessboard.ui.components.AppMessageDialog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AddFenPositionContinuationsFlow(
    positionId: Long,
    fenPositionService: FenPositionService,
    continuationService: FenPositionContinuationService,
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var position by remember(positionId, fenPositionService) {
        mutableStateOf<FenPositionEntity?>(null)
    }
    var loadFailed by remember(positionId, fenPositionService) { mutableStateOf(false) }
    var saveFailed by remember(positionId, continuationService) { mutableStateOf(false) }
    var isSaving by remember(positionId, continuationService) { mutableStateOf(false) }
    val strings = addFenPositionContinuationsStrings()
    val scope = rememberCoroutineScope()

    fun save(uciLines: List<List<String>>) {
        if (isSaving) {
            return
        }

        isSaving = true
        saveFailed = false
        scope.launch {
            val result = try {
                withContext(Dispatchers.IO) {
                    continuationService.createBatch(
                        positionId = positionId,
                        preparation = FenPositionContinuationBatchPreparation(
                            preparedUciLines = uciLines,
                            sourceLinesCount = uciLines.size,
                            exactDuplicateLinesCount = 0,
                            coveredPrefixLinesCount = 0,
                        ),
                    )
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                null
            }
            isSaving = false
            if (result is CreateFenPositionContinuationBatchResult.Success) {
                onSaved()
                return@launch
            }

            saveFailed = true
        }
    }

    LaunchedEffect(positionId, fenPositionService) {
        position = try {
            withContext(Dispatchers.IO) {
                fenPositionService.getById(positionId)
            }
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (_: Exception) {
            loadFailed = true
            null
        }

        if (position == null) {
            loadFailed = true
        }
    }

    val loadedPosition = position
    if (loadedPosition == null) {
        if (loadFailed) {
            AppMessageDialog(
                title = strings.processingErrorTitle,
                message = strings.loadingPositionMessage,
                onDismiss = onBackClick,
            )
        } else {
            AppLoadingDialog(
                title = strings.loadingPositionTitle,
                message = strings.loadingPositionMessage,
            )
        }
        return
    }

    AddFenPositionContinuationsContainer(
        startFen = loadedPosition.fen,
        positionName = loadedPosition.name,
        positionTheme = loadedPosition.theme,
        loadStoredUciLines = {
            continuationService.getUciLinesByPositionId(positionId)
        },
        onBackClick = onBackClick,
        onSaveClick = ::save,
        modifier = modifier,
    )

    if (isSaving) {
        AppLoadingDialog(
            title = strings.savingTitle,
            message = strings.savingMessage,
        )
    }

    if (saveFailed) {
        AppMessageDialog(
            title = strings.processingErrorTitle,
            message = strings.saveFailedMessage,
            onDismiss = { saveFailed = false },
        )
    }
}
