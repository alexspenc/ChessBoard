package com.example.chessboard

import com.example.chessboard.boardmodel.InitialBoardFenWithoutMoveNumbers

class RuntimeContext {
    val orderGamesInTraining = OrderGamesInTraining()
    val positionEditor = PositionEditor()

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
}
