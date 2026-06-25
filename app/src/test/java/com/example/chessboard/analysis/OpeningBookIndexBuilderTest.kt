package com.example.chessboard.analysis

/**
 * Tests pure opening-book indexing used by opening analysis features.
 * Keep UI rendering, navigation, and database integration coverage outside this file.
 * Validation date: 2026-06-25
 */
import com.example.chessboard.boardmodel.InitialBoardFen
import com.example.chessboard.entity.LineEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class OpeningBookIndexBuilderTest {

    private val builder = OpeningBookIndexBuilder()

    /**
     * Verifies that the index records every known book move from the initial position.
     * This is the base contract used by both existing deviation search and future game-vs-book analysis.
     */
    @Test
    fun `build records next moves from starting position`() {
        val e4Line = line(
            id = 1,
            pgn = storedPgn("e2e4"),
        )
        val d4Line = line(
            id = 2,
            pgn = storedPgn("d2d4"),
        )

        val index = builder.build(listOf(e4Line, d4Line))
        val startPosition = requirePosition(index, InitialPositionKey)

        assertEquals(OpeningSide.WHITE, startPosition.sideToMove)
        assertEquals(listOf("e2e4", "d2d4"), startPosition.nextMoves.map { move -> move.moveUci })
    }

    /**
     * Verifies that identical continuations are stored as one book move with multiple line refs.
     * This preserves branch counts without duplicating the same move in deviation displays.
     */
    @Test
    fun `build groups same next move from multiple lines`() {
        val shortNf3Line = line(
            id = 1,
            pgn = storedPgn("e2e4", "e7e5", "g1f3"),
        )
        val longNf3Line = line(
            id = 2,
            pgn = storedPgn("e2e4", "e7e5", "g1f3", "b8c6"),
        )

        val index = builder.build(listOf(shortNf3Line, longNf3Line))
        val position = requirePosition(index, AfterE4E5Key)
        val nf3Move = position.nextMoves.single { move -> move.moveUci == "g1f3" }

        assertEquals(listOf(0, 1), nf3Move.lineRefs.map { ref -> ref.lineIndex })
        assertEquals(listOf(2, 2), nf3Move.lineRefs.map { ref -> ref.ply })
    }

    /**
     * Verifies that a line ending at a position is indexed separately from available next moves.
     * This keeps current deviation detection unchanged and enables the future "book too short" result.
     */
    @Test
    fun `build records ended lines at final position`() {
        val shorterLine = line(
            id = 1,
            pgn = storedPgn("e2e4", "e7e5"),
        )
        val longerLine = line(
            id = 2,
            pgn = storedPgn("e2e4", "e7e5", "g1f3"),
        )

        val index = builder.build(listOf(shorterLine, longerLine))
        val position = requirePosition(index, AfterE4E5Key)

        assertEquals(listOf(0), position.endedLineRefs.map { ref -> ref.lineIndex })
        assertEquals(listOf("g1f3"), position.nextMoves.map { move -> move.moveUci })
    }

    /**
     * Verifies that one line contributes only once when it reaches the same position again.
     * This preserves the previous finder behavior and prevents inflated branch counts.
     */
    @Test
    fun `build treats repeated position in same line once`() {
        val repeatedLine = line(
            id = 1,
            pgn = storedPgn(
                "g1f3",
                "g8f6",
                "f3g1",
                "f6g8",
                "e2e4",
            ),
        )
        val nf3Line = line(
            id = 2,
            pgn = storedPgn("g1f3"),
        )

        val index = builder.build(listOf(repeatedLine, nf3Line))
        val startPosition = requirePosition(index, InitialPositionKey)
        val nf3Move = startPosition.nextMoves.single { move -> move.moveUci == "g1f3" }

        assertEquals(listOf(0, 1), nf3Move.lineRefs.map { ref -> ref.lineIndex })
        assertEquals(listOf("g1f3"), startPosition.nextMoves.map { move -> move.moveUci })
    }

    /**
     * Verifies that indexing starts from each line's stored initial FEN instead of always using the normal start.
     * This protects saved lines that begin from imported or manually selected positions.
     */
    @Test
    fun `build replays custom initial fen`() {
        val nf3Line = line(
            id = 1,
            pgn = storedPgn("g1f3"),
            initialFen = AfterE4E5Fen,
        )
        val bc4Line = line(
            id = 2,
            pgn = storedPgn("f1c4"),
            initialFen = AfterE4E5Fen,
        )

        val index = builder.build(listOf(nf3Line, bc4Line))
        val position = requirePosition(index, AfterE4E5Key)

        assertEquals(OpeningSide.WHITE, position.sideToMove)
        assertEquals(listOf("g1f3", "f1c4"), position.nextMoves.map { move -> move.moveUci })
    }

    private fun requirePosition(
        index: OpeningBookIndex,
        positionFen: String,
    ): OpeningBookPosition {
        val position = index.positions[positionFen]
        assertNotNull("Expected indexed position $positionFen", position)
        return position!!
    }

    private fun line(
        id: Long,
        pgn: String,
        initialFen: String = InitialBoardFen,
    ): LineEntity {
        return LineEntity(
            id = id,
            pgn = pgn,
            initialFen = initialFen,
        )
    }

    private fun storedPgn(vararg moves: String): String {
        return buildString {
            append("[Event \"Test\"]\n")
            append("[White \"White\"]\n")
            append("[Black \"Black\"]\n")
            append("[Result \"*\"]\n\n")

            moves.forEachIndexed { index, move ->
                if (index % 2 == 0) {
                    append("${index / 2 + 1}. ")
                }
                append(move)
                append(" ")
            }

            append("*")
        }
    }

    private companion object {
        const val InitialPositionKey =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -"
        const val AfterE4E5Key =
            "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6"
        const val AfterE4E5Fen =
            "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2"
    }
}
