package com.example.chessboard.runtimecontext

/*
 * File role: stores in-memory pagination state for the FEN position catalog.
 * Allowed here:
 * - current catalog offset and page-navigation rules
 * - keeping the offset valid when the catalog size changes
 * Not allowed here:
 * - Room access, persisted position data, UI rendering, or navigation
 * Validation date: 2026-08-30
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

    fun canOpenPreviousPage(): Boolean {
        return offset > 0
    }

    fun canOpenNextPage(totalCount: Int): Boolean {
        return offset + pageLimit < totalCount
    }

    fun openFirstPage() {
        offset = 0
    }

    fun openPreviousPage() {
        if (!canOpenPreviousPage()) {
            return
        }

        offset = (offset - pageLimit).coerceAtLeast(0)
    }

    fun openNextPage(totalCount: Int) {
        if (!canOpenNextPage(totalCount)) {
            return
        }

        offset += pageLimit
    }

    fun ensureValidOffset(totalCount: Int) {
        if (totalCount <= 0) {
            offset = 0
            return
        }

        if (offset < totalCount) {
            return
        }

        offset = (totalCount - 1) / pageLimit * pageLimit
    }
}
