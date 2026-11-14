package com.example.chessboard.chesscore.fixtures

/**
 * File role: supplies the user-provided opening and a recorded PositionFactory for usage examples.
 * Contains test-only FEN snapshots, explicit moves, and snapshot lookup through chesscore interfaces.
 * Does not generate legal moves, validate arbitrary FEN, or implement general chess rules.
 * Validation date: 2026-11-14.
 */

import com.example.chessboard.chesscore.Position
import com.example.chessboard.chesscore.PositionFactory
import com.example.chessboard.chesscore.model.Move
import com.example.chessboard.chesscore.model.Piece
import com.example.chessboard.chesscore.model.PieceType
import com.example.chessboard.chesscore.model.Side
import com.example.chessboard.chesscore.model.Square

/** Records the seven supplied positions and six moves of 1. Nf3 Nf6 2. g3 d5 3. Bg2 c6. */
internal object OpeningSequence {
    val fens: List<String> = listOf(
        "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
        "rnbqkbnr/pppppppp/8/8/8/5N2/PPPPPPPP/RNBQKB1R b KQkq - 1 1",
        "rnbqkb1r/pppppppp/5n2/8/8/5N2/PPPPPPPP/RNBQKB1R w KQkq - 2 2",
        "rnbqkb1r/pppppppp/5n2/8/8/5NP1/PPPPPP1P/RNBQKB1R b KQkq - 0 2",
        "rnbqkb1r/ppp1pppp/5n2/3p4/8/5NP1/PPPPPP1P/RNBQKB1R w KQkq - 0 3",
        "rnbqkb1r/ppp1pppp/5n2/3p4/8/5NP1/PPPPPPBP/RNBQK2R b KQkq - 1 3",
        "rnbqkb1r/pp2pppp/2p2n2/3p4/8/5NP1/PPPPPPBP/RNBQK2R w KQkq - 0 4",
    )

    val moves: List<Move> = listOf(
        Move(Square('g', 1), Square('f', 3)), // Nf3
        Move(Square('g', 8), Square('f', 6)), // Nf6
        Move(Square('g', 2), Square('g', 3)), // g3
        Move(Square('d', 7), Square('d', 5)), // d5
        Move(Square('f', 1), Square('g', 2)), // Bg2
        Move(Square('c', 7), Square('c', 6)), // c6
    )
}

/**
 * Creates independent cursors into [OpeningSequence] for examples of the public chesscore API.
 * This test double supplies only the next recorded move, not every legal move of each position.
 * Unknown positions and moves are fixture errors, rather than claims of chess illegality.
 */
internal class OpeningSequencePositionFactory : PositionFactory {
    /**
     * @param fen an exact FEN from [OpeningSequence], or null for its standard initial position.
     * @return an independent position at the selected snapshot.
     * @throws IllegalStateException if the fixture does not contain the supplied position.
     */
    override fun create(fen: String?): Position {
        var positionIndex = 0
        if (fen != null) {
            positionIndex = OpeningSequence.fens.indexOf(fen)
        }
        check(positionIndex >= 0) { "FEN is not recorded in OpeningSequence: $fen" }

        return OpeningSequencePosition(positionIndex)
    }
}

private class OpeningSequencePosition(private var positionIndex: Int) : Position {
    private var pieces = readRecordedPieces(getFen())

    override fun getFen(): String {
        return OpeningSequence.fens[positionIndex]
    }

    override fun getSideToMove(): Side {
        val sideToken = getFen().split(' ')[1]
        if (sideToken == "w") {
            return Side.WHITE
        }

        return Side.BLACK
    }

    override fun getPiece(square: Square): Piece? {
        return pieces[square]
    }

    override fun getLegalMoves(): List<Move> {
        check(positionIndex < OpeningSequence.moves.size) {
            "No moves are recorded after the final OpeningSequence position"
        }

        return listOf(OpeningSequence.moves[positionIndex])
    }

    override fun applyMove(move: Move): Boolean {
        val recordedMove = getLegalMoves().single()
        check(move == recordedMove) {
            "Move $move is not recorded for OpeningSequence position $positionIndex"
        }

        positionIndex++
        pieces = readRecordedPieces(getFen())
        return true
    }

    private fun readRecordedPieces(fen: String): Map<Square, Piece> {
        val pieces = mutableMapOf<Square, Piece>()
        val ranks = fen.substringBefore(' ').split('/')

        for ((rankIndex, rankText) in ranks.withIndex()) {
            var fileIndex = 0
            for (token in rankText) {
                if (token in '1'..'8') {
                    fileIndex += token.digitToInt()
                    continue
                }

                var side = Side.BLACK
                if (token.isUpperCase()) {
                    side = Side.WHITE
                }
                val type = when (token.lowercaseChar()) {
                    'p' -> PieceType.PAWN
                    'n' -> PieceType.KNIGHT
                    'b' -> PieceType.BISHOP
                    'r' -> PieceType.ROOK
                    'q' -> PieceType.QUEEN
                    'k' -> PieceType.KING
                    else -> error("Unknown piece in recorded FEN: $token")
                }
                pieces[Square('a' + fileIndex, 8 - rankIndex)] = Piece(side, type)
                fileIndex++
            }
        }

        return pieces
    }
}
