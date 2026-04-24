package com.example.chessboard.analysis

/**
 * Builds UI-facing opening deviation items from pure deviation results.
 * Keep branch aggregation and one-ply replay logic here.
 * Do not add screen wiring, navigation, or database queries to this file.
 */
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.service.parsePgnMoves
import com.example.chessboard.ui.screen.openingDeviation.OpeningDeviationBranch
import com.example.chessboard.ui.screen.openingDeviation.OpeningDeviationItem

class OpeningDeviationItemBuilder(
    private val finder: OpeningDeviationFinder = OpeningDeviationFinder(),
) {

    fun build(
        games: List<GameEntity>,
        selectedSide: OpeningSide,
    ): List<OpeningDeviationItem> {
        return finder.findDeviations(
            games = games,
            selectedSide = selectedSide,
        ).map { deviation ->
            OpeningDeviationItem(
                positionFen = deviation.positionFen,
                branches = buildBranches(
                    deviation = deviation,
                    selectedSide = selectedSide,
                ),
            )
        }
    }

    private fun buildBranches(
        deviation: OpeningDeviation,
        selectedSide: OpeningSide,
    ): List<OpeningDeviationBranch> {
        val branchesByResultFen = linkedMapOf<String, BranchBucket>()

        deviation.games.forEach { game ->
            recordGameBranch(
                game = game,
                deviationPositionFen = deviation.positionFen,
                selectedSide = selectedSide,
                branchesByResultFen = branchesByResultFen,
            )
        }

        return branchesByResultFen.values.map { bucket ->
            OpeningDeviationBranch(
                moveUci = bucket.moveUci,
                resultFen = bucket.resultFen,
                gamesCount = bucket.gamesCount,
            )
        }
    }

    private fun recordGameBranch(
        game: GameEntity,
        deviationPositionFen: String,
        selectedSide: OpeningSide,
        branchesByResultFen: MutableMap<String, BranchBucket>,
    ) {
        val board = OpeningDeviationReplay.buildInitialBoard(game.initialFen)
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
                !OpeningDeviationReplay.isSelectedSideToMove(board, selectedSide) ||
                positionKey != deviationPositionFen
            ) {
                board.doMove(move)
                continue
            }

            board.doMove(move)
            recordBranch(
                moveUci = uciMove.lowercase(),
                resultFen = OpeningDeviationReplay.buildPositionKey(board),
                branchesByResultFen = branchesByResultFen,
            )
            return
        }
    }

    private fun recordBranch(
        moveUci: String,
        resultFen: String,
        branchesByResultFen: MutableMap<String, BranchBucket>,
    ) {
        val existingBucket = branchesByResultFen[resultFen]
        if (existingBucket != null) {
            existingBucket.gamesCount += 1
            return
        }

        branchesByResultFen[resultFen] = BranchBucket(
            moveUci = moveUci,
            resultFen = resultFen,
            gamesCount = 1,
        )
    }

    private data class BranchBucket(
        val moveUci: String,
        val resultFen: String,
        var gamesCount: Int,
    )
}
