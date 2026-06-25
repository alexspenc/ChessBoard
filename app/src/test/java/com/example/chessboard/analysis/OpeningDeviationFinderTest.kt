package com.example.chessboard.analysis

/**
 * Tests pure opening deviation detection without database or UI dependencies.
 * Keep screen behavior and persistence integration tests in their own packages.
 */
import com.example.chessboard.boardmodel.InitialBoardFen
import com.example.chessboard.entity.LineEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpeningDeviationFinderTest {

    private val finder = OpeningDeviationFinder()

    @Test
    fun `findDeviations detects white move split from same position`() {
        val nf3Line = line(
            id = 1,
            pgn = storedPgn("e2e4", "e7e5", "g1f3"),
        )
        val bc4Line = line(
            id = 2,
            pgn = storedPgn("e2e4", "e7e5", "f1c4"),
        )

        val deviations = finder.findDeviations(
            lines = listOf(nf3Line, bc4Line),
            selectedSide = OpeningSide.WHITE,
        )

        assertEquals(1, deviations.size)
        assertEquals(
            "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6",
            deviations.single().positionFen,
        )
        assertEquals(listOf(1L, 2L), deviations.single().lines.map { line -> line.id })
    }

    @Test
    fun `findDeviations ignores opponent move split`() {
        val e5Line = line(
            id = 1,
            pgn = storedPgn("e2e4", "e7e5"),
        )
        val c5Line = line(
            id = 2,
            pgn = storedPgn("e2e4", "c7c5"),
        )

        val deviations = finder.findDeviations(
            lines = listOf(e5Line, c5Line),
            selectedSide = OpeningSide.WHITE,
        )

        assertTrue(deviations.isEmpty())
    }

    @Test
    fun `findDeviations detects black move split`() {
        val e5Line = line(
            id = 1,
            pgn = storedPgn("e2e4", "e7e5"),
        )
        val c5Line = line(
            id = 2,
            pgn = storedPgn("e2e4", "c7c5"),
        )

        val deviations = finder.findDeviations(
            lines = listOf(e5Line, c5Line),
            selectedSide = OpeningSide.BLACK,
        )

        assertEquals(1, deviations.size)
        assertEquals(
            "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3",
            deviations.single().positionFen,
        )
        assertEquals(listOf(1L, 2L), deviations.single().lines.map { line -> line.id })
    }

    @Test
    fun `findDeviations treats repeated position in same line once`() {
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

        val deviations = finder.findDeviations(
            lines = listOf(repeatedLine, nf3Line),
            selectedSide = OpeningSide.WHITE,
        )

        assertTrue(deviations.isEmpty())
    }

    @Test
    fun `findDeviations keeps en passant target in position key`() {
        val e5Line = line(
            id = 1,
            pgn = storedPgn("e2e4", "d7d5", "e4e5"),
        )
        val exd5Line = line(
            id = 2,
            pgn = storedPgn("e2e4", "d7d5", "e4d5"),
        )

        val deviations = finder.findDeviations(
            lines = listOf(e5Line, exd5Line),
            selectedSide = OpeningSide.WHITE,
        )

        assertEquals(1, deviations.size)
        assertEquals(
            "rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6",
            deviations.single().positionFen,
        )
    }

    @Test
    fun `findDeviations keeps unsaved lines distinct by input position`() {
        val nf3Line = line(
            id = 0,
            pgn = storedPgn("e2e4", "e7e5", "g1f3"),
        )
        val bc4Line = line(
            id = 0,
            pgn = storedPgn("e2e4", "e7e5", "f1c4"),
        )

        val deviations = finder.findDeviations(
            lines = listOf(nf3Line, bc4Line),
            selectedSide = OpeningSide.WHITE,
        )

        assertEquals(1, deviations.size)
        assertEquals(2, deviations.single().lines.size)
    }

    @Test
    fun `findDeviations does not treat shorter line as deviation`() {
        val shorterLine = line(
            id = 1,
            pgn = storedPgn("e2e4", "e7e5"),
        )
        val longerLine = line(
            id = 2,
            pgn = storedPgn("e2e4", "e7e5", "g1f3"),
        )

        val deviations = finder.findDeviations(
            lines = listOf(shorterLine, longerLine),
            selectedSide = OpeningSide.WHITE,
        )

        assertTrue(deviations.isEmpty())
    }

    @Test
    fun `findDeviations replays lines from custom initial fen`() {
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

        val deviations = finder.findDeviations(
            lines = listOf(nf3Line, bc4Line),
            selectedSide = OpeningSide.WHITE,
        )

        assertEquals(1, deviations.size)
        assertEquals(
            "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6",
            deviations.single().positionFen,
        )
        assertEquals(listOf(1L, 2L), deviations.single().lines.map { line -> line.id })
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
        const val AfterE4E5Fen =
            "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2"
    }
}
