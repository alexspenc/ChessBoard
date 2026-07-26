package com.example.chessboard.runtimecontext.linesexplorer

/*
 * File role: stores in-memory runtime state for the lines explorer screen.
 * Allowed here:
 * - line id paging, sorting, filtering criteria, and selection-preserving helpers
 * - runtime-only state that should survive leaving and returning to the lines explorer
 * Not allowed here:
 * - Compose UI rendering, screen navigation, Room DAO definitions, or database writes
 * Validation date: 2026-07-09
 */

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.chessboard.repository.DatabaseProvider

class LinesExplorerRuntimeContext(
    private val limit: Int
) {
    enum class LinesSortMode {
        DEFAULT,
        MISTAKES_DESC,
        MISTAKES_ASC,
    }

    data class State(
        val defaultLineIds: List<Long> = emptyList(),
        val lineIds: List<Long> = emptyList(),
        val lineMistakeTotalsByLineId: Map<Long, Int> = emptyMap(),
        val sortMode: LinesSortMode = LinesSortMode.DEFAULT,
        val offset: Int = 0,
    )

    data class FilterCriteria(
        val query: String = "",
        val isCaseSensitive: Boolean = false,
        val dubiousOnly: Boolean = false,
        val sideMask: Int? = null,
    )

    var state by mutableStateOf(State())
        private set

    var filterCriteria by mutableStateOf(FilterCriteria())
        private set

    var filteredOffset by mutableStateOf(0)
        private set

    suspend fun loadAllLineIds(inDbProvider: DatabaseProvider) {
        val snapshot = inDbProvider.createLinesExplorerDataService().loadAllLinesSnapshot()
        val lineMistakeTotalsByLineId = snapshot.lineMistakeTotals.associate { lineMistakesTotal ->
            lineMistakesTotal.lineId to lineMistakesTotal.totalMistakes
        }

        state = State(
            defaultLineIds = snapshot.lineIds,
            lineIds = sortLineIds(
                lineIds = snapshot.lineIds,
                sortMode = state.sortMode,
                lineMistakeTotalsByLineId = lineMistakeTotalsByLineId,
                defaultLineIds = snapshot.lineIds,
            ),
            lineMistakeTotalsByLineId = lineMistakeTotalsByLineId,
            sortMode = state.sortMode,
        )
        clearFilter()
    }

    fun setLineIds(lineIds: List<Long>) {
        state = State(
            defaultLineIds = lineIds,
            lineIds = sortLineIds(
                lineIds = lineIds,
                sortMode = state.sortMode,
                lineMistakeTotalsByLineId = state.lineMistakeTotalsByLineId,
                defaultLineIds = lineIds,
            ),
            lineMistakeTotalsByLineId = state.lineMistakeTotalsByLineId,
            sortMode = state.sortMode,
        )
        clearFilter()
    }

    fun updateSortMode(sortMode: LinesSortMode) {
        state = state.copy(
            lineIds = sortLineIds(
                lineIds = state.defaultLineIds,
                sortMode = sortMode,
                lineMistakeTotalsByLineId = state.lineMistakeTotalsByLineId,
                defaultLineIds = state.defaultLineIds,
            ),
            sortMode = sortMode,
            offset = 0,
        )
        filteredOffset = 0
    }

    fun sortLineIds(lineIds: List<Long>): List<Long> {
        return sortLineIds(
            lineIds = lineIds,
            sortMode = state.sortMode,
            lineMistakeTotalsByLineId = state.lineMistakeTotalsByLineId,
            defaultLineIds = state.defaultLineIds,
        )
    }

    fun updateFilterCriteria(filterCriteria: FilterCriteria) {
        this.filterCriteria = filterCriteria
        filteredOffset = 0
    }

    fun clearFilter() {
        filterCriteria = FilterCriteria()
        filteredOffset = 0
    }

    fun updateFilteredOffset(offset: Int) {
        filteredOffset = offset.coerceAtLeast(0)
    }

    fun openPreviousFilteredPage() {
        filteredOffset = (filteredOffset - limit).coerceAtLeast(0)
    }

    fun openNextFilteredPage(totalLineCount: Int) {
        if (filteredOffset + limit >= totalLineCount) {
            return
        }

        filteredOffset += limit
    }

    fun ensureVisible(lineId: Long?) {
        if (lineId == null) {
            return
        }

        val lineIndex = state.lineIds.indexOf(lineId)
        if (lineIndex < 0) {
            return
        }

        val nextOffset = lineIndex / limit * limit
        if (nextOffset == state.offset) {
            return
        }

        state = state.copy(offset = nextOffset)
    }

    fun visibleLineIds(): List<Long> {
        return state.lineIds.drop(state.offset).take(limit)
    }

    fun canOpenPreviousPage(): Boolean {
        return state.offset > 0
    }

    fun canOpenNextPage(): Boolean {
        return state.offset + limit < state.lineIds.size
    }

    fun openPreviousPage() {
        if (!canOpenPreviousPage()) {
            return
        }

        state = state.copy(
            offset = (state.offset - limit).coerceAtLeast(0)
        )
    }

    fun openNextPage() {
        if (!canOpenNextPage()) {
            return
        }

        state = state.copy(offset = state.offset + limit)
    }

    fun removeLineId(lineId: Long) {
        val mutableLineIds = state.lineIds.toMutableList()
        if (!mutableLineIds.remove(lineId)) {
            return
        }

        state = state.copy(
            defaultLineIds = state.defaultLineIds.filterNot { currentLineId -> currentLineId == lineId },
            lineIds = mutableLineIds,
            offset = resolveOffsetAfterRemove(mutableLineIds.size)
        )
    }

    fun removeLineIds(lineIds: Collection<Long>) {
        if (lineIds.isEmpty()) {
            return
        }

        val lineIdsToRemove = lineIds.toSet()
        val nextLineIds = state.lineIds.filterNot { lineId -> lineId in lineIdsToRemove }
        if (nextLineIds.size == state.lineIds.size) {
            return
        }

        state = state.copy(
            defaultLineIds = state.defaultLineIds.filterNot { lineId -> lineId in lineIdsToRemove },
            lineIds = nextLineIds,
            offset = resolveOffsetAfterRemove(nextLineIds.size)
        )
    }

    private fun resolveOffsetAfterRemove(
        nextLineCount: Int
    ): Int {
        if (nextLineCount <= 0) {
            return 0
        }

        if (state.offset < nextLineCount) {
            return state.offset
        }

        return ((nextLineCount - 1) / limit) * limit
    }

    private fun sortLineIds(
        lineIds: List<Long>,
        sortMode: LinesSortMode,
        lineMistakeTotalsByLineId: Map<Long, Int>,
        defaultLineIds: List<Long>,
    ): List<Long> {
        if (sortMode == LinesSortMode.DEFAULT) {
            return lineIds
        }

        val defaultOrderByLineId = defaultLineIds.withIndex().associate { indexedLineId ->
            indexedLineId.value to indexedLineId.index
        }
        val currentOrderByLineId = lineIds.withIndex().associate { indexedLineId ->
            indexedLineId.value to indexedLineId.index
        }

        fun resolveOrder(lineId: Long): Int {
            return defaultOrderByLineId[lineId] ?: currentOrderByLineId[lineId] ?: Int.MAX_VALUE
        }

        if (sortMode == LinesSortMode.MISTAKES_DESC) {
            return lineIds.sortedWith(
                compareByDescending<Long> { lineId -> lineMistakeTotalsByLineId[lineId] ?: 0 }
                    .thenBy { lineId -> resolveOrder(lineId) }
            )
        }

        return lineIds.sortedWith(
            compareBy<Long> { lineId -> lineMistakeTotalsByLineId[lineId] ?: 0 }
                .thenBy { lineId -> resolveOrder(lineId) }
        )
    }
}
