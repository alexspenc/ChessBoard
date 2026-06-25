package com.example.chessboard.analysis

/**
 * File role: builds an in-memory opening-book index from loaded line records.
 * Allowed here:
 * - pure replay of stored line moves into normalized position keys
 * - position-to-next-move indexing for opening analysis features
 * - lightweight line references that avoid duplicating full line records per position
 * Not allowed here:
 * - database access, Compose UI, screen navigation, or persistence workflows
 * Validation date: 2026-06-25
 */
import com.example.chessboard.entity.LineEntity
import com.example.chessboard.service.parsePgnMoves

class OpeningBookIndex(
    val lines: List<LineEntity>,
    val positions: Map<String, OpeningBookPosition>,
) {
    fun lineFor(ref: OpeningBookLineRef): LineEntity {
        return lines[ref.lineIndex]
    }
}

data class OpeningBookPosition(
    val positionFen: String,
    val sideToMove: OpeningSide,
    val nextMoves: List<OpeningBookMove>,
    val endedLineRefs: List<OpeningBookLineRef>,
)

data class OpeningBookMove(
    val moveUci: String,
    val resultFen: String,
    val lineRefs: List<OpeningBookLineRef>,
)

data class OpeningBookLineRef(
    val lineIndex: Int,
    val stableLineId: Long?,
    val inputIndex: Int?,
    val ply: Int,
) {
    companion object {
        fun from(
            line: LineEntity,
            lineIndex: Int,
            ply: Int,
        ): OpeningBookLineRef {
            if (line.id != 0L) {
                return OpeningBookLineRef(
                    lineIndex = lineIndex,
                    stableLineId = line.id,
                    inputIndex = null,
                    ply = ply,
                )
            }

            return OpeningBookLineRef(
                lineIndex = lineIndex,
                stableLineId = null,
                inputIndex = lineIndex,
                ply = ply,
            )
        }
    }
}

class OpeningBookIndexBuilder {

    fun build(lines: List<LineEntity>): OpeningBookIndex {
        val positionsByFen = linkedMapOf<String, PositionBucket>()

        lines.forEachIndexed { lineIndex, line ->
            scanLine(
                line = line,
                lineIndex = lineIndex,
                positionsByFen = positionsByFen,
            )
        }

        return OpeningBookIndex(
            lines = lines,
            positions = positionsByFen.mapValues { entry -> entry.value.toPosition() },
        )
    }

    private fun scanLine(
        line: LineEntity,
        lineIndex: Int,
        positionsByFen: MutableMap<String, PositionBucket>,
    ) {
        val board = OpeningDeviationReplay.buildInitialBoard(line.initialFen)
        val seenPositionsInLine = mutableSetOf<String>()
        val moves = parsePgnMoves(line.pgn)

        for ((moveIndex, uciMove) in moves.withIndex()) {
            val positionFen = OpeningDeviationReplay.buildPositionKey(board)
            val sideToMove = resolveSideToMove(boardSideName = board.sideToMove.name)
            val move = OpeningDeviationReplay.buildMoveFromUci(
                uci = uciMove,
                board = board,
                line = line,
                moveIndex = moveIndex,
            )

            board.doMove(move)
            val resultFen = OpeningDeviationReplay.buildPositionKey(board)

            if (!seenPositionsInLine.add(positionFen)) {
                continue
            }

            recordNextMove(
                positionFen = positionFen,
                sideToMove = sideToMove,
                moveUci = uciMove.lowercase(),
                resultFen = resultFen,
                lineRef = OpeningBookLineRef.from(
                    line = line,
                    lineIndex = lineIndex,
                    ply = moveIndex,
                ),
                positionsByFen = positionsByFen,
            )
        }

        recordEndedLine(
            positionFen = OpeningDeviationReplay.buildPositionKey(board),
            sideToMove = resolveSideToMove(boardSideName = board.sideToMove.name),
            lineRef = OpeningBookLineRef.from(
                line = line,
                lineIndex = lineIndex,
                ply = moves.size,
            ),
            positionsByFen = positionsByFen,
        )
    }

    private fun recordNextMove(
        positionFen: String,
        sideToMove: OpeningSide,
        moveUci: String,
        resultFen: String,
        lineRef: OpeningBookLineRef,
        positionsByFen: MutableMap<String, PositionBucket>,
    ) {
        val position = positionsByFen.getOrPut(positionFen) {
            PositionBucket(
                positionFen = positionFen,
                sideToMove = sideToMove,
            )
        }
        val move = position.nextMovesByUci.getOrPut(moveUci) {
            MoveBucket(
                moveUci = moveUci,
                resultFen = resultFen,
            )
        }

        move.lineRefsByKey[lineRef.key()] = lineRef
    }

    private fun recordEndedLine(
        positionFen: String,
        sideToMove: OpeningSide,
        lineRef: OpeningBookLineRef,
        positionsByFen: MutableMap<String, PositionBucket>,
    ) {
        val position = positionsByFen.getOrPut(positionFen) {
            PositionBucket(
                positionFen = positionFen,
                sideToMove = sideToMove,
            )
        }

        position.endedLineRefsByKey[lineRef.key()] = lineRef
    }

    private fun resolveSideToMove(boardSideName: String): OpeningSide {
        if (boardSideName == OpeningSide.BLACK.name) {
            return OpeningSide.BLACK
        }

        return OpeningSide.WHITE
    }

    private fun OpeningBookLineRef.key(): LineRefKey {
        return LineRefKey(
            stableLineId = stableLineId,
            inputIndex = inputIndex,
        )
    }

    private data class PositionBucket(
        val positionFen: String,
        val sideToMove: OpeningSide,
        val nextMovesByUci: LinkedHashMap<String, MoveBucket> = linkedMapOf(),
        val endedLineRefsByKey: LinkedHashMap<LineRefKey, OpeningBookLineRef> = linkedMapOf(),
    ) {
        fun toPosition(): OpeningBookPosition {
            return OpeningBookPosition(
                positionFen = positionFen,
                sideToMove = sideToMove,
                nextMoves = nextMovesByUci.values.map { move -> move.toMove() },
                endedLineRefs = endedLineRefsByKey.values.toList(),
            )
        }
    }

    private data class MoveBucket(
        val moveUci: String,
        val resultFen: String,
        val lineRefsByKey: LinkedHashMap<LineRefKey, OpeningBookLineRef> = linkedMapOf(),
    ) {
        fun toMove(): OpeningBookMove {
            return OpeningBookMove(
                moveUci = moveUci,
                resultFen = resultFen,
                lineRefs = lineRefsByKey.values.toList(),
            )
        }
    }

    private data class LineRefKey(
        val stableLineId: Long?,
        val inputIndex: Int?,
    )
}
