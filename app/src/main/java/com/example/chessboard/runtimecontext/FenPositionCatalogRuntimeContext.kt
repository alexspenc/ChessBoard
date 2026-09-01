package com.example.chessboard.runtimecontext

/*
 * File role: stores in-memory pagination and selection state for the FEN position catalog.
 * Allowed here:
 * - current catalog offset, selected position id, and page-navigation rules
 * - keeping the offset and selection valid when catalog data changes
 * Not allowed here:
 * - Room access, persisted position data, UI rendering, or navigation
 * Validation date: 2026-08-31
 */

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class FenPositionCatalogRuntimeContext(
    val pageLimit: Int,
) {
    init {
        require(pageLimit > 0) { "Page limit must be positive" }
    }

    var offset by mutableStateOf(0)
        private set

    var selectedPositionId by mutableStateOf<Long?>(null)
        private set

    fun canOpenPreviousPage(): Boolean {
        return offset > 0
    }

    fun canOpenNextPage(totalCount: Int): Boolean {
        return offset + pageLimit < totalCount
    }

    fun openFirstPage() {
        if (offset == 0) {
            return
        }

        clearPositionSelection()
        offset = 0
    }

    fun openPreviousPage() {
        if (!canOpenPreviousPage()) {
            return
        }

        clearPositionSelection()
        offset = (offset - pageLimit).coerceAtLeast(0)
    }

    fun openNextPage(totalCount: Int) {
        if (!canOpenNextPage(totalCount)) {
            return
        }

        clearPositionSelection()
        offset += pageLimit
    }

    fun ensureValidOffset(totalCount: Int) {
        if (totalCount <= 0) {
            clearPositionSelection()
            offset = 0
            return
        }

        if (offset < totalCount) {
            return
        }

        clearPositionSelection()
        offset = (totalCount - 1) / pageLimit * pageLimit
    }

    fun selectPosition(positionId: Long) {
        selectedPositionId = positionId
    }

    fun clearPositionSelection() {
        selectedPositionId = null
    }

    fun ensureSelectedPositionIsVisible(visiblePositionIds: Collection<Long>) {
        val selectedId = selectedPositionId ?: return
        if (selectedId in visiblePositionIds) {
            return
        }

        clearPositionSelection()
    }
}
