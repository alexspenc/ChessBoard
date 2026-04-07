package com.example.chessboard

class RuntimeContext {
    val orderGamesInTraining = OrderGamesInTraining()

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
            getGameId: (T) -> Long
        ): List<T> {
            // Zero means "not trained in this runtime session", so those stay ahead of completed games.
            return games.withIndex()
                .sortedWith(
                    compareBy<IndexedValue<T>> { indexedGame ->
                        if (lastCompletedOrderByGameId[getGameId(indexedGame.value)] == null) 0 else 1
                    }.thenByDescending { indexedGame ->
                        lastCompletedOrderByGameId[getGameId(indexedGame.value)] ?: 0L
                    }.thenBy { indexedGame ->
                        indexedGame.index
                    }
                )
                .map { indexedGame -> indexedGame.value }
        }
    }
}
