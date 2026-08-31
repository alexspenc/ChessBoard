package com.example.chessboard.ui.screen.fenpositions.catalog

/*
 * File role: connects the FEN position catalog UI to its service and runtime screen state.
 * Allowed here:
 * - loading one catalog page, correcting stale offsets, and mapping entities to UI items
 * - coordinating the create dialog with the service and refreshing the catalog
 * - forwarding screen actions and storing selection in runtime context
 * Not allowed here:
 * - app-wide navigation registration, Room queries, or card presentation details
 * Validation date: 2026-08-31
 */

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.chessboard.entity.FenPositionEntity
import com.example.chessboard.runtimecontext.FenPositionCatalogRuntimeContext
import com.example.chessboard.service.CreateFenPositionResult
import com.example.chessboard.service.FenPositionService
import com.example.chessboard.ui.components.AppLoadingDialog
import com.example.chessboard.ui.components.AppMessageDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FenPositionCatalogScreenContainer(
    fenPositionService: FenPositionService,
    runtimeContext: FenPositionCatalogRuntimeContext,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var uiState by remember(fenPositionService, runtimeContext) {
        mutableStateOf(FenPositionCatalogUiState())
    }
    var totalPositionsCount by remember(fenPositionService, runtimeContext) {
        mutableStateOf(0)
    }
    var reloadRevision by remember(fenPositionService, runtimeContext) {
        mutableIntStateOf(0)
    }
    var isCreateDialogVisible by remember { mutableStateOf(false) }
    val strings = fenPositionCatalogStrings()
    val pageOffset = runtimeContext.offset

    LaunchedEffect(fenPositionService, runtimeContext, pageOffset, reloadRevision) {
        uiState = uiState.copy(isLoading = true)

        val page = withContext(Dispatchers.IO) {
            fenPositionService.getCatalogPage(
                limit = runtimeContext.pageLimit,
                offset = pageOffset,
            )
        }

        runtimeContext.ensureValidOffset(page.totalCount)
        if (runtimeContext.offset != pageOffset) {
            return@LaunchedEffect
        }

        runtimeContext.ensureSelectedPositionIsVisible(
            visiblePositionIds = page.positions.map { position -> position.id },
        )
        totalPositionsCount = page.totalCount
        uiState = FenPositionCatalogUiState(
            isLoading = false,
            positions = page.positions.map { position -> position.toCatalogItem() },
        )
    }

    FenPositionCatalogScreen(
        uiState = uiState,
        paginationState = resolveFenPositionCatalogPaginationState(
            runtimeContext = runtimeContext,
            totalPositionsCount = totalPositionsCount,
        ),
        selectedPositionId = runtimeContext.selectedPositionId,
        onBackClick = onBackClick,
        onHomeClick = onHomeClick,
        onPositionSelected = runtimeContext::selectPosition,
        onAddPositionClick = {
            isCreateDialogVisible = true
        },
        onOpenPreviousPageClick = runtimeContext::openPreviousPage,
        onOpenNextPageClick = {
            runtimeContext.openNextPage(totalCount = totalPositionsCount)
        },
        modifier = modifier,
    )

    if (isCreateDialogVisible) {
        FenPositionCreationFlow(
            fenPositionService = fenPositionService,
            strings = strings.createDialog,
            onDismiss = {
                isCreateDialogVisible = false
            },
            onCreated = {
                isCreateDialogVisible = false
                runtimeContext.openFirstPage()
                reloadRevision += 1
            },
        )
    }
}

@Composable
private fun FenPositionCreationFlow(
    fenPositionService: FenPositionService,
    strings: FenPositionCreateDialogStrings,
    onDismiss: () -> Unit,
    onCreated: () -> Unit,
) {
    var isCreating by remember { mutableStateOf(false) }
    var createErrorMessage by remember { mutableStateOf<String?>(null) }
    var saveFailureDialogMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun dismiss() {
        if (isCreating) {
            return
        }

        onDismiss()
    }

    fun create(request: CreateFenPositionRequest) {
        isCreating = true
        createErrorMessage = null
        saveFailureDialogMessage = null
        coroutineScope.launch {
            val result = try {
                withContext(Dispatchers.IO) {
                    fenPositionService.create(
                        fen = request.fen,
                        name = request.name,
                        theme = request.theme,
                        description = request.description,
                    )
                }
            } catch (_: Exception) {
                null
            }

            isCreating = false
            if (result == null) {
                saveFailureDialogMessage = strings.saveFailed
                return@launch
            }

            if (result is CreateFenPositionResult.Success) {
                onCreated()
                return@launch
            }

            createErrorMessage = resolveCreateFenPositionError(
                result = result,
                strings = strings,
            )
        }
    }

    CreateFenPositionDialog(
        strings = strings,
        isSaving = isCreating,
        saveErrorMessage = createErrorMessage,
        onInputChanged = {
            createErrorMessage = null
        },
        onDismiss = ::dismiss,
        onCreate = ::create,
    )

    if (isCreating) {
        AppLoadingDialog(
            title = strings.savingTitle,
            message = strings.savingMessage,
        )
    }

    val currentSaveFailureDialogMessage = saveFailureDialogMessage
    if (currentSaveFailureDialogMessage != null) {
        AppMessageDialog(
            title = strings.saveFailedTitle,
            message = currentSaveFailureDialogMessage,
            onDismiss = {
                saveFailureDialogMessage = null
            },
        )
    }
}

private fun resolveCreateFenPositionError(
    result: CreateFenPositionResult,
    strings: FenPositionCreateDialogStrings,
): String {
    return when (result) {
        CreateFenPositionResult.DuplicateFen -> strings.duplicateFen
        CreateFenPositionResult.InvalidFen -> strings.invalidFen
        CreateFenPositionResult.BlankTheme -> strings.themeRequired
        is CreateFenPositionResult.Success -> strings.saveFailed
    }
}

private fun FenPositionEntity.toCatalogItem(): FenPositionCatalogItem {
    return FenPositionCatalogItem(
        id = id,
        fen = fen,
        name = name,
        theme = theme,
    )
}

private fun resolveFenPositionCatalogPaginationState(
    runtimeContext: FenPositionCatalogRuntimeContext,
    totalPositionsCount: Int,
): FenPositionCatalogPaginationState {
    val totalPages = if (totalPositionsCount == 0) {
        1
    } else {
        (totalPositionsCount - 1) / runtimeContext.pageLimit + 1
    }
    val currentPage = if (totalPositionsCount == 0) {
        1
    } else {
        runtimeContext.offset / runtimeContext.pageLimit + 1
    }

    return FenPositionCatalogPaginationState(
        totalPositionsCount = totalPositionsCount,
        currentPage = currentPage,
        totalPages = totalPages,
        canOpenPreviousPage = runtimeContext.canOpenPreviousPage(),
        canOpenNextPage = runtimeContext.canOpenNextPage(totalPositionsCount),
    )
}
