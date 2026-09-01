package com.example.chessboard.ui.screen.fenpositions.catalog

/*
 * File role: connects the FEN position catalog UI to its service and runtime screen state.
 * Allowed here:
 * - loading one catalog page, correcting stale offsets, and mapping entities to UI items
 * - coordinating create/delete dialogs with the service and refreshing the catalog
 * - forwarding screen actions and storing selection in runtime context
 * Not allowed here:
 * - app-wide navigation registration, Room queries, or card presentation details
 * Validation date: 2026-09-01
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
import com.example.chessboard.ui.screen.fenpositions.FenPositionDeletionFlow
import com.example.chessboard.ui.screen.fenpositions.fenPositionDeletionStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FenPositionCatalogScreenContainer(
    fenPositionService: FenPositionService,
    runtimeContext: FenPositionCatalogRuntimeContext,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onOpenPosition: (Long) -> Unit,
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
    var isDeleteDialogVisible by remember { mutableStateOf(false) }
    val strings = fenPositionCatalogStrings()
    val deletionStrings = fenPositionDeletionStrings()
    val pageOffset = runtimeContext.offset
    val selectedPositionId = runtimeContext.selectedPositionId

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
        selectedPositionId = selectedPositionId,
        onBackClick = onBackClick,
        onHomeClick = onHomeClick,
        onPositionSelected = runtimeContext::selectPosition,
        onAddPositionClick = {
            isCreateDialogVisible = true
        },
        onOpenPositionClick = onOpenPosition,
        onDeletePositionClick = {
            if (selectedPositionId != null) {
                isDeleteDialogVisible = true
            }
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

    if (isDeleteDialogVisible && selectedPositionId != null) {
        FenPositionDeletionFlow(
            positionId = selectedPositionId,
            fenPositionService = fenPositionService,
            strings = deletionStrings,
            onDismiss = {
                isDeleteDialogVisible = false
            },
            onDeleted = {
                isDeleteDialogVisible = false
                runtimeContext.clearPositionSelection()
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
    var saveErrorDialogMessage by remember { mutableStateOf<String?>(null) }
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
        saveErrorDialogMessage = null
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
            when (result) {
                null -> saveErrorDialogMessage = strings.saveFailed
                CreateFenPositionResult.DuplicateFen -> {
                    saveErrorDialogMessage = strings.duplicateFen
                }
                CreateFenPositionResult.InvalidFen -> {
                    createErrorMessage = strings.invalidFen
                }
                CreateFenPositionResult.BlankTheme -> {
                    createErrorMessage = strings.themeRequired
                }
                is CreateFenPositionResult.Success -> onCreated()
            }
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

    val currentSaveErrorDialogMessage = saveErrorDialogMessage
    if (currentSaveErrorDialogMessage != null) {
        AppMessageDialog(
            title = strings.saveFailedTitle,
            message = currentSaveErrorDialogMessage,
            onDismiss = {
                saveErrorDialogMessage = null
            },
        )
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
