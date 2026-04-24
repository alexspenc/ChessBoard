package com.example.chessboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.chessboard.boardmodel.InitialBoardFenWithoutMoveNumbers
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.service.SmartGamePair
import com.example.chessboard.ui.screen.openingDeviation.OpeningDeviationItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RuntimeContext {
    val gamesExplorer = ObservableGamesPage(GamesExplorerPageLimit)
    val openingDeviation = OpeningDeviation()
    val orderGamesInTraining = OrderGamesInTraining()
    val positionEditor = PositionEditor()
    var trainingMoveFrom: Int = 1
    var trainingMoveTo: Int = 0
    var trainingOrderedGameIds: List<Long> = emptyList()
    var smartTrainingQueue: List<SmartGamePair> = emptyList()

    fun resolveNextSmartGamePair(currentGameId: Long): SmartGamePair? {
        val index = smartTrainingQueue.indexOfFirst { it.gameId == currentGameId }
        if (index < 0) return null
        return smartTrainingQueue.getOrNull(index + 1)
    }

    fun resolveNextTrainingGameId(currentGameId: Long): Long? {
        val currentIndex = trainingOrderedGameIds.indexOf(currentGameId)
        if (currentIndex < 0) {
            return null
        }

        return trainingOrderedGameIds.getOrNull(currentIndex + 1)
    }

    companion object {
        const val GamesExplorerPageLimit = 20
    }

    class OrderGamesInTraining {
        private val lastCompletedOrderByGameId = mutableMapOf<Long, Long>()
        private var nextOrder = 0L

        fun markGameCompleted(gameId: Long) {
            nextOrder += 1L
            lastCompletedOrderByGameId[gameId] = nextOrder
        }

        fun reset() {
            lastCompletedOrderByGameId.clear()
            nextOrder = 0L
        }

        fun <T> orderGames(
            games: List<T>,
            getGameId: (T) -> Long,
            getWeight: (T) -> Int
        ): List<T> {
            return games.withIndex()
                .sortedWith { leftGame, rightGame ->
                    val leftCompletedOrder = lastCompletedOrderByGameId[getGameId(leftGame.value)]
                    val rightCompletedOrder = lastCompletedOrderByGameId[getGameId(rightGame.value)]

                    if (leftCompletedOrder == null && rightCompletedOrder != null) {
                        return@sortedWith -1
                    }

                    if (leftCompletedOrder != null && rightCompletedOrder == null) {
                        return@sortedWith 1
                    }

                    if (leftCompletedOrder == null && rightCompletedOrder == null) {
                        val weightCompare = rightGame.value.let(getWeight).compareTo(
                            leftGame.value.let(getWeight)
                        )
                        if (weightCompare != 0) {
                            return@sortedWith weightCompare
                        }

                        return@sortedWith leftGame.index.compareTo(rightGame.index)
                    }

                    val completedOrderCompare = rightCompletedOrder!!.compareTo(leftCompletedOrder!!)
                    if (completedOrderCompare != 0) {
                        return@sortedWith completedOrderCompare
                    }

                    val weightCompare = rightGame.value.let(getWeight).compareTo(
                        leftGame.value.let(getWeight)
                    )
                    if (weightCompare != 0) {
                        return@sortedWith weightCompare
                    }

                    leftGame.index.compareTo(rightGame.index)
                }
                .map { indexedGame -> indexedGame.value }
        }
    }

    class PositionEditor {
        private val defaultInitialFen = InitialBoardFenWithoutMoveNumbers

        var initialFen: String = defaultInitialFen
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

    class ObservableGamesPage(
        private val limit: Int
    ) {
        data class State(
            val gameIds: List<Long> = emptyList(),
            val offset: Int = 0
        )

        var state by mutableStateOf(State())
            private set

        suspend fun loadAllGameIds(inDbProvider: DatabaseProvider) {
            val gameListService = inDbProvider.createGameListService()
            val gamesCount = withContext(Dispatchers.IO) {
                gameListService.getGamesCount()
            }
            val gameIds = withContext(Dispatchers.IO) {
                gameListService.getGameIdsPage(
                    limit = gamesCount.coerceAtLeast(1),
                    offset = 0
                )
            }

            state = State(gameIds = gameIds)
        }

        fun setGameIds(gameIds: List<Long>) {
            state = State(gameIds = gameIds)
        }

        fun ensureVisible(gameId: Long?) {
            if (gameId == null) {
                return
            }

            val gameIndex = state.gameIds.indexOf(gameId)
            if (gameIndex < 0) {
                return
            }

            val nextOffset = gameIndex / limit * limit
            if (nextOffset == state.offset) {
                return
            }

            state = state.copy(offset = nextOffset)
        }

        fun visibleGameIds(): List<Long> {
            return state.gameIds.drop(state.offset).take(limit)
        }

        fun canOpenPreviousPage(): Boolean {
            return state.offset > 0
        }

        fun canOpenNextPage(): Boolean {
            return state.offset + limit < state.gameIds.size
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

        fun removeGameId(gameId: Long) {
            val mutableGameIds = state.gameIds.toMutableList()
            if (!mutableGameIds.remove(gameId)) {
                return
            }

            state = state.copy(
                gameIds = mutableGameIds,
                offset = resolveOffsetAfterRemove(mutableGameIds.size)
            )
        }

        private fun resolveOffsetAfterRemove(
            nextGameCount: Int
        ): Int {
            if (nextGameCount <= 0) {
                return 0
            }

            if (state.offset < nextGameCount) {
                return state.offset
            }

            return ((nextGameCount - 1) / limit) * limit
        }
    }
}
