package com.example.chessboard.runtimecontext

/**
 * File role: groups app-level runtime contexts and cross-screen in-memory session state.
 * Allowed here:
 * - screen runtime-context holders and app-process session state
 * - runtime-only helpers that preserve navigation or editor/training context
 * Not allowed here:
 * - composable UI, screen rendering, or layout code
 * - database access, persistence helpers, or repository/business logic
 * Validation date: 2026-04-25
 */

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.chessboard.boardmodel.InitialBoardFenWithoutMoveNumbers
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.service.SmartLinePair
import com.example.chessboard.ui.screen.openingDeviation.OpeningDeviationItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RuntimeContext {
    val linesExplorer = ObservableLinesPage(LinesExplorerPageLimit)
    val openingDeviation = OpeningDeviation()
    val orderLinesInTraining = OrderLinesInTraining()
    val templateLineSelection = LineCollectionRuntimeContext()
    internal val trainingSession = TrainingRuntimeContext()
    val statisticsTraining = StatisticsTrainingRuntimeContext()
    val positionSearch = PositionSearch()
    var trainingMoveFrom: Int = 1
    var trainingMoveTo: Int = 0
    var smartTrainingQueue: List<SmartLinePair> = emptyList()

    fun resolveNextSmartLinePair(currentLineId: Long): SmartLinePair? {
        val index = smartTrainingQueue.indexOfFirst { it.lineId == currentLineId }
        if (index < 0) return null
        return smartTrainingQueue.getOrNull(index + 1)
    }

    fun resolveNextTrainingLineId(trainingId: Long, currentLineId: Long): Long? {
        return trainingSession.resolveNextLineId(trainingId, currentLineId)
    }

    companion object {
        const val LinesExplorerPageLimit = 20
    }

    class OrderLinesInTraining {
        private val lastCompletedOrderByLineId = mutableMapOf<Long, Long>()
        private var nextOrder = 0L

        fun markLineCompleted(lineId: Long) {
            nextOrder += 1L
            lastCompletedOrderByLineId[lineId] = nextOrder
        }

        fun reset() {
            lastCompletedOrderByLineId.clear()
            nextOrder = 0L
        }

        fun <T> orderLines(
            lines: List<T>,
            getLineId: (T) -> Long,
            getWeight: (T) -> Int
        ): List<T> {
            return lines.withIndex()
                .sortedWith { leftLine, rightLine ->
                    val leftCompletedOrder = lastCompletedOrderByLineId[getLineId(leftLine.value)]
                    val rightCompletedOrder = lastCompletedOrderByLineId[getLineId(rightLine.value)]

                    if (leftCompletedOrder == null && rightCompletedOrder != null) {
                        return@sortedWith -1
                    }

                    if (leftCompletedOrder != null && rightCompletedOrder == null) {
                        return@sortedWith 1
                    }

                    if (leftCompletedOrder == null && rightCompletedOrder == null) {
                        val weightCompare = rightLine.value.let(getWeight).compareTo(
                            leftLine.value.let(getWeight)
                        )
                        if (weightCompare != 0) {
                            return@sortedWith weightCompare
                        }

                        return@sortedWith leftLine.index.compareTo(rightLine.index)
                    }

                    val completedOrderCompare = rightCompletedOrder!!.compareTo(leftCompletedOrder!!)
                    if (completedOrderCompare != 0) {
                        return@sortedWith completedOrderCompare
                    }

                    val weightCompare = rightLine.value.let(getWeight).compareTo(
                        leftLine.value.let(getWeight)
                    )
                    if (weightCompare != 0) {
                        return@sortedWith weightCompare
                    }

                    leftLine.index.compareTo(rightLine.index)
                }
                .map { indexedLine -> indexedLine.value }
        }
    }

    class PositionSearch {
        private val defaultInitialFen = InitialBoardFenWithoutMoveNumbers

        var initialFen by mutableStateOf(defaultInitialFen)
        var onBackClick: () -> Unit = {}

        fun resetToInitialPosition() {
            initialFen = defaultInitialFen
        }
    }

    class OpeningDeviation {
        var deviationItems by mutableStateOf<List<OpeningDeviationItem>>(emptyList())
            private set
        var selectedDeviationIndex by mutableStateOf<Int?>(null)
            private set
        var selectedBranchIndex by mutableStateOf<Int?>(null)
            private set
        var sourcePositionFen by mutableStateOf<String?>(null)
            private set

        fun setDeviationItems(
            sourcePositionFen: String?,
            deviationItems: List<OpeningDeviationItem>,
        ) {
            this.sourcePositionFen = sourcePositionFen
            this.deviationItems = deviationItems
            selectedDeviationIndex = null
            selectedBranchIndex = null
        }

        fun selectDeviation(index: Int) {
            selectedDeviationIndex = index
            selectedBranchIndex = null
        }

        fun selectBranch(index: Int) {
            selectedBranchIndex = index
        }

        fun selectedDeviationItem(): OpeningDeviationItem? {
            val selectedIndex = selectedDeviationIndex ?: return null
            return deviationItems.getOrNull(selectedIndex)
        }

        fun selectedBranch(): com.example.chessboard.ui.screen.openingDeviation.OpeningDeviationBranch? {
            val selectedIndex = selectedBranchIndex ?: return null
            return selectedDeviationItem()?.branches?.getOrNull(selectedIndex)
        }

        fun clear() {
            sourcePositionFen = null
            deviationItems = emptyList()
            selectedDeviationIndex = null
            selectedBranchIndex = null
        }
    }

    class ObservableLinesPage(
        private val limit: Int
    ) {
        data class State(
            val lineIds: List<Long> = emptyList(),
            val offset: Int = 0
        )

        var state by mutableStateOf(State())
            private set

        suspend fun loadAllLineIds(inDbProvider: DatabaseProvider) {
            val lineListService = inDbProvider.createLineListService()
            val linesCount = withContext(Dispatchers.IO) {
                lineListService.getLinesCount()
            }
            val lineIds = withContext(Dispatchers.IO) {
                lineListService.getLineIdsPage(
                    limit = linesCount.coerceAtLeast(1),
                    offset = 0
                )
            }

            state = State(lineIds = lineIds)
        }

        fun setLineIds(lineIds: List<Long>) {
            state = State(lineIds = lineIds)
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
    }
}
