package com.example.chessboard.analysis

/**
 * Contains pure opening-line analysis that works with loaded game records.
 * Keep database access, UI state, and screen workflow code outside this file.
 */
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.service.parsePgnMoves

enum class OpeningSide {
    WHITE,
    BLACK,
}

data class OpeningDeviation(
    val positionFen: String,
    val games: List<GameEntity>,
)

class OpeningDeviationFinder {

    /**
     * Finds opening-line deviations for one selected side.
     *
     * Algorithm:
     * 1. Iterate over every input game and replay its stored UCI moves on a chesslib [Board].
     * 2. Before each move, inspect the current board position. A position is considered only when
     *    the side to move is [selectedSide]; opponent turns are only replayed to advance the board.
     * 3. Convert the current board to a comparison key using FEN without halfmove/fullmove counters,
     *    while keeping piece placement, side to move, castling rights, and en passant target.
     * 4. Track position keys already seen inside the current game. If the same position appears
     *    again in that game, ignore the repeated occurrence so one game contributes only once per
     *    position.
     * 5. For the first occurrence of a selected-side position in a game, record two facts in the
     *    bucket for that position:
     *    - the game that reached this position;
     *    - the next move played by the selected side from this position.
     * 6. After all games are scanned, a position is a deviation when its bucket contains more than
     *    one unique next move. The public result returns the position FEN and all games that reached
     *    that position; the move set is only an internal detail used to detect the split.
     *
     * Games with a persisted id are deduplicated by that id. Games with id = 0 are treated as
     * distinct input items by their index, which keeps multiple unsaved games separate.
     */
    fun findDeviations(
        games: List<GameEntity>,
        selectedSide: OpeningSide,
    ): List<OpeningDeviation> {
        val bucketsByPosition = linkedMapOf<String, PositionBucket>()

        games.forEachIndexed { gameIndex, game ->
            scanGame(
                game = game,
                gameIndex = gameIndex,
                selectedSide = selectedSide,
                bucketsByPosition = bucketsByPosition,
            )
        }

        return bucketsByPosition.values
            .filter { bucket -> bucket.nextMoves.size > 1 }
            .map { bucket ->
                OpeningDeviation(
                    positionFen = bucket.positionFen,
                    games = bucket.games.values.toList(),
                )
            }
    }

    private fun scanGame(
        game: GameEntity,
        gameIndex: Int,
        selectedSide: OpeningSide,
        bucketsByPosition: MutableMap<String, PositionBucket>,
    ) {
        val board = OpeningDeviationReplay.buildInitialBoard(game.initialFen)
        val seenPositionsInGame = mutableSetOf<String>()
        val moves = parsePgnMoves(game.pgn)

        for ((moveIndex, uciMove) in moves.withIndex()) {
            val positionKey = OpeningDeviationReplay.buildPositionKey(board)
            val move = OpeningDeviationReplay.buildMoveFromUci(
                uci = uciMove,
                board = board,
                game = game,
                moveIndex = moveIndex,
            )

            if (
                OpeningDeviationReplay.isSelectedSideToMove(board, selectedSide) &&
                seenPositionsInGame.add(positionKey)
            ) {
                recordPosition(
                    positionKey = positionKey,
                    game = game,
                    gameIndex = gameIndex,
                    nextMove = uciMove.lowercase(),
                    bucketsByPosition = bucketsByPosition,
                )
            }

            board.doMove(move)
        }
    }

    private fun recordPosition(
        positionKey: String,
        game: GameEntity,
        gameIndex: Int,
        nextMove: String,
        bucketsByPosition: MutableMap<String, PositionBucket>,
    ) {
        val bucket = bucketsByPosition.getOrPut(positionKey) {
            PositionBucket(positionFen = positionKey)
        }

        bucket.nextMoves.add(nextMove)
        bucket.games[GameKey.from(game = game, gameIndex = gameIndex)] = game
    }

    private data class PositionBucket(
        val positionFen: String,
        val games: LinkedHashMap<GameKey, GameEntity> = linkedMapOf(),
        val nextMoves: MutableSet<String> = linkedSetOf(),
    )

    private data class GameKey(
        val stableId: Long?,
        val inputIndex: Int?,
    ) {
        companion object {
            fun from(game: GameEntity, gameIndex: Int): GameKey {
                if (game.id != 0L) {
                    return GameKey(stableId = game.id, inputIndex = null)
                }

                return GameKey(stableId = null, inputIndex = gameIndex)
            }
        }
    }
}
