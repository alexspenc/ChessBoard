package com.example.chessboard.service

import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.move.Move
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import com.example.chessboard.ui.screen.createOpening.TreeSegment
import com.example.chessboard.ui.screen.createOpening.buildMoveTreeData

class PgnServiceTest {

    // ──────────────────────────────────────────────────────────────────────
    // parsePgnToUciLines — variation expansion
    // ──────────────────────────────────────────────────────────────────────

    /**
     * PGN with one nested variation level:
     *
     *   1. d4 d5 2. Nf3 Nf6 (2... Bg4 3. Nbd2 e6 4. e3 (4. Ne5 Nf6 5. h3) 4... Nf6 5. h3) 3. e3 e6 4. Bd3 Be7
     *
     * Expected lines (main line first):
     *   1. d4 d5 Nf3 Nf6 e3 e6 Bd3 Be7
     *   2. d4 d5 Nf3 Bg4 Nbd2 e6 e3 Nf6 h3
     *   3. d4 d5 Nf3 Bg4 Nbd2 e6 Ne5 Nf6 h3
     */
    @Test
    fun `parsePgnToUciLines returns main line then variation lines`() {
        val pgn = """
            [Event "?"]
            [Site "?"]
            [Date "????.??.??"]
            [Round "?"]
            [White "?"]
            [Black "?"]
            [Result "*"]
            [Link "https://www.chess.com/analysis/game/pgn/2MAF42tFxz/analysis"]

            1. d4 d5 2. Nf3 Nf6 (2... Bg4 3. Nbd2 e6 4. e3 (4. Ne5 Nf6 5. h3) 4... Nf6 5.
            h3) 3. e3 e6 4. Bd3 Be7 *
        """.trimIndent()

        val lines = parsePgnToUciLines(pgn)

        assertEquals(3, lines.size)

        assertEquals(
            listOf("d2d4", "d7d5", "g1f3", "g8f6", "e2e3", "e7e6", "f1d3", "f8e7"),
            lines[0]
        )
        assertEquals(
            listOf("d2d4", "d7d5", "g1f3", "c8g4", "b1d2", "e7e6", "e2e3", "g8f6", "h2h3"),
            lines[1]
        )
        assertEquals(
            listOf("d2d4", "d7d5", "g1f3", "c8g4", "b1d2", "e7e6", "f3e5", "g8f6", "h2h3"),
            lines[2]
        )
    }

    @Test
    fun `parsePgnToUciLines Ne5 sub-variation is not lost`() {
        // Regression test: the deeply nested Ne5 sub-variation was reported missing.
        // PGN structure: main-line Nf6 / outer-var (Bg4 / inner-var (Ne5) / e3) / main continues
        val pgn = """
            [Event "?"]
            [Site "?"]
            [Date "????.??.??"]
            [Round "?"]
            [White "?"]
            [Black "?"]
            [Result "*"]
            [Link "https://www.chess.com/analysis/game/pgn/2MAF42tFxz/analysis"]

            1. d4 d5 2. Nf3 Nf6 (2... Bg4 3. Nbd2 e6 4. e3 (4. Ne5 Nf6 5. h3) 4... Nf6 5.
            h3) 3. e3 e6 4. Bd3 Be7 *
        """.trimIndent()

        val lines = parsePgnToUciLines(pgn)

        val ne5Line = listOf("d2d4", "d7d5", "g1f3", "c8g4", "b1d2", "e7e6", "f3e5", "g8f6", "h2h3")
        assertTrue(
            "Ne5 sub-variation not found. Got ${lines.size} line(s):\n${lines.joinToString("\n")}",
            lines.contains(ne5Line)
        )
    }

    @Test
    fun `parsePgnToUciLines keeps nested variation that starts from current position`() {
        val pgn = """
            1. d4 d5 2. Nf3 Nf6 (2... Bg4 3. Nbd2 e6 (4. Ne5 Nf6 5. h3) 4. e3 Nf6 5. h3) 3. e3 e6 4. Bd3 Be7 *
        """.trimIndent()

        val lines = parsePgnToUciLines(pgn)

        assertEquals(3, lines.size)
        assertTrue(
            "Nested Ne5 line not found. Got ${lines.size} line(s):\n${lines.joinToString("\n")}",
            lines.contains(
                listOf("d2d4", "d7d5", "g1f3", "c8g4", "b1d2", "e7e6", "f3e5", "g8f6", "h2h3")
            )
        )
    }

    @Test
    fun `parsePgnToUciLines throws when nested variation starts from illegal position`() {
        val pgn = """
            1. d4 d5 2. Nf3 Nf6 (2... Bg4 3. Nbd2 e6 (4. Qa5) 4. e3 Nf6 5. h3) 3. e3 e6 4. Bd3 Be7 *
        """.trimIndent()

        val error = assertThrows(IllegalArgumentException::class.java) {
            parsePgnToUciLines(pgn)
        }

        assertTrue(error.message?.contains("Can't play Qa5") == true)
    }

    @Test
    fun `buildMoveTreeData emits separate sub-variation segment`() {
        val uciLines = listOf(
            listOf("d2d4", "d7d5", "g1f3", "g8f6", "e2e3", "e7e6", "f1d3", "f8e7"),
            listOf("d2d4", "d7d5", "g1f3", "c8g4", "b1d2", "e7e6", "e2e3", "g8f6", "h2h3"),
            listOf("d2d4", "d7d5", "g1f3", "c8g4", "b1d2", "e7e6", "f3e5", "g8f6", "h2h3")
        )

        val segments = buildMoveTreeData(uciLines)
        val variationSegments = segments.filterIsInstance<TreeSegment.Variation>()

        assertEquals(2, variationSegments.size)

        val moveLabelsByVariation = variationSegments.map { variation ->
            variation.moves.map { move -> move.label }
        }

        assertEquals(listOf("Bg4", "Nd2", "e6", "e3", "Nf6", "h3"), moveLabelsByVariation[0])
        assertEquals(listOf("Ne5", "Nf6", "h3"), moveLabelsByVariation[1])
    }

    @Test
    fun `parsed PGN builds move tree with main line first and two variation rows`() {
        val pgn = """
            1. d4 d5 2. Nf3 Nf6 (2... Bg4 3. Nbd2 e6 4. e3 (4. Ne5 Nf6 5. h3) 4... Nf6 5. h3) 3. e3 e6 4. Bd3 Be7 *
        """.trimIndent()

        val uciLines = parsePgnToUciLines(pgn)
        val segments = buildMoveTreeData(uciLines)
        val labelsBySegment = segments.map { segment ->
            when (segment) {
                is TreeSegment.MainMoves -> "MainMoves" to segment.moves.map { move -> move.label }
                is TreeSegment.Variation -> "Variation" to segment.moves.map { move -> move.label }
            }
        }

        assertEquals("MainMoves", labelsBySegment[0].first)
        assertEquals(listOf("d4", "d5", "Nf3", "Nf6"), labelsBySegment[0].second)
        assertEquals("Variation", labelsBySegment[1].first)
        assertEquals(listOf("Bg4", "Nd2", "e6", "e3", "Nf6", "h3"), labelsBySegment[1].second)
        assertEquals("Variation", labelsBySegment[2].first)
        assertEquals(listOf("Ne5", "Nf6", "h3"), labelsBySegment[2].second)
        assertEquals("MainMoves", labelsBySegment[3].first)
        assertEquals(listOf("e3", "e6", "Bd3", "Be7"), labelsBySegment[3].second)
    }

    @Test
    fun `parsePgnToUci returns first line only`() {
        val pgn = """
            1. d4 d5 2. Nf3 Nf6 (2... Bg4 3. Nbd2 e6 4. e3 (4. Ne5 Nf6 5. h3) 4... Nf6 5.
            h3) 3. e3 e6 4. Bd3 Be7 *
        """.trimIndent()

        val uci = parsePgnToUci(pgn)

        assertEquals(listOf("d2d4", "d7d5", "g1f3", "g8f6", "e2e3", "e7e6", "f1d3", "f8e7"), uci)
    }

    @Test
    fun `parsePgnToUciLines deduplicates identical lines`() {
        // Two variations that collapse to the same move sequence
        val pgn = "1. e4 e5 (1... e5 2. Nf3) 2. Nf3 *"
        val lines = parsePgnToUciLines(pgn)
        val asStrings = lines.map { it.joinToString(" ") }
        assertEquals(asStrings.distinct(), asStrings)
    }

    @Test
    fun `parsePgnToUciLines handles PGN with no variations`() {
        val pgn = "1. e4 e5 2. Nf3 Nc6 *"
        val lines = parsePgnToUciLines(pgn)
        assertEquals(1, lines.size)
        assertEquals(listOf("e2e4", "e7e5", "g1f3", "b8c6"), lines[0])
    }

    // ──────────────────────────────────────────────────────────────────────
    // extractPgnHeaders
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun `extractPgnHeaders returns all tag values`() {
        val pgn = """
            [Event "London System"]
            [White "Magnus"]
            [Black "Hikaru"]
            [ECO "D02"]
            [Result "1-0"]
        """.trimIndent()

        val headers = extractPgnHeaders(pgn)

        assertEquals("London System", headers["Event"])
        assertEquals("Magnus", headers["White"])
        assertEquals("Hikaru", headers["Black"])
        assertEquals("D02", headers["ECO"])
        assertEquals("1-0", headers["Result"])
    }

    @Test
    fun `extractPgnHeaders returns empty map for PGN with no headers`() {
        val headers = extractPgnHeaders("1. e4 e5 *")
        assertTrue(headers.isEmpty())
    }

    // ──────────────────────────────────────────────────────────────────────
    // parsePgnMoves — stored-PGN format (UCI notation)
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun `parsePgnMoves extracts UCI tokens ignoring headers and move numbers`() {
        val stored = """
            [Event "Test"]
            [White "White"]
            [Black "Black"]
            [Result "*"]

            1. d2d4 d7d5 2. g1f3 g8f6 3. e2e3 e7e6 4. f1d3 f8e7 *
        """.trimIndent()

        val moves = parsePgnMoves(stored)

        assertEquals(listOf("d2d4", "d7d5", "g1f3", "g8f6", "e2e3", "e7e6", "f1d3", "f8e7"), moves)
    }

    @Test
    fun `parsePgnMoves handles promotion tokens`() {
        val stored = "1. e2e4 d7d5 2. e4e5 e7e6 3. e5e6 f7f6 4. e6e7 g8f6 5. e7e8q *"
        val moves = parsePgnMoves(stored)
        assertTrue(moves.last() == "e7e8q")
    }

    // ──────────────────────────────────────────────────────────────────────
    // computeLabel
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun `computeLabel returns correct label for pawn push`() {
        val startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        val move = Move(Square.E2, Square.E4)
        assertEquals("e4", computeLabel(move, startFen))
    }

    @Test
    fun `computeLabel returns correct label for knight move`() {
        val startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        val move = Move(Square.G1, Square.F3)
        assertEquals("Nf3", computeLabel(move, startFen))
    }

    @Test
    fun `computeLabel appends capture marker`() {
        // After 1.e4 d5 — white pawn captures on d5
        val fenAfter1e4d5 = "rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2"
        val move = Move(Square.E4, Square.D5)
        assertEquals("exd5", computeLabel(move, fenAfter1e4d5))
    }

    @Test
    fun `computeLabel returns O-O for kingside castling`() {
        // Position where white can castle kingside
        val fen = "r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R w KQkq - 0 1"
        val move = Move(Square.E1, Square.G1)
        assertEquals("O-O", computeLabel(move, fen))
    }

    @Test
    fun `computeLabel returns O-O-O for queenside castling`() {
        val fen = "r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R w KQkq - 0 1"
        val move = Move(Square.E1, Square.C1)
        assertEquals("O-O-O", computeLabel(move, fen))
    }

    @Test
    fun `computeLabel appends check suffix`() {
        // Queen on a1, black king on e8 — Qa8 attacks along the 8th rank (check, not mate)
        val fen = "4k3/8/8/8/8/8/8/Q3K3 w - - 0 1"
        val move = Move(Square.A1, Square.A8)
        val label = computeLabel(move, fen)
        assertTrue("Expected check suffix, got: $label", label.endsWith("+"))
    }

    @Test
    fun `computeLabel appends checkmate suffix`() {
        // Scholar's mate: after 1.e4 e5 2.Bc4 Nc6 3.Qh5, white plays Qxf7#
        // Queen on h5 captures pawn on f7; black king on e8 has no escape
        val fen = "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4"
        val move = Move(Square.H5, Square.F7)
        val label = computeLabel(move, fen)
        assertTrue("Expected checkmate suffix, got: $label", label.endsWith("#"))
    }

    @Test
    fun `computeLabel includes promotion piece`() {
        // White pawn on a7, black king on h8 (not on promotion square) — a7a8q gives check
        val fen = "7k/P7/8/8/8/8/8/7K w - - 0 1"
        val move = Move(Square.A7, Square.A8, Piece.WHITE_QUEEN)
        val label = computeLabel(move, fen)
        assertTrue("Expected =Q in label, got: $label", label.contains("=Q"))
    }

    // ──────────────────────────────────────────────────────────────────────
    // buildMoveLabels
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun `buildMoveLabels returns correct algebraic labels for main line`() {
        val uci = listOf("d2d4", "d7d5", "g1f3", "g8f6", "e2e3", "e7e6", "f1d3", "f8e7")
        val labels = buildMoveLabels(uci)
        assertEquals(listOf("d4", "d5", "Nf3", "Nf6", "e3", "e6", "Bd3", "Be7"), labels)
    }

    @Test
    fun `buildMoveLabels Ne5 line produces correct algebraic labels`() {
        val uci = listOf("d2d4", "d7d5", "g1f3", "c8g4", "b1d2", "e7e6", "f3e5", "g8f6", "h2h3")
        val labels = buildMoveLabels(uci)
        assertEquals(
            "Label count must match move count",
            uci.size, labels.size
        )
        assertEquals("d4",  labels[0])
        assertEquals("d5",  labels[1])
        assertEquals("Nf3", labels[2])
        assertEquals("Bg4", labels[3])
        // Move 5: Nd2 — two knights can reach d2, computeLabel emits "Nd2" (no disambiguation)
        assertEquals("Nd2", labels[4])
        assertEquals("e6",  labels[5])
        assertEquals("Ne5", labels[6])
        assertEquals("Nf6", labels[7])
        assertEquals("h3",  labels[8])
    }

    @Test
    fun `buildMoveLabels count matches input when all moves are legal`() {
        val uci = listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4")
        val labels = buildMoveLabels(uci)
        assertEquals(uci.size, labels.size)
    }

    @Test
    fun `buildMoveLabels includes promotion piece in label`() {
        // White a-pawn races to promotion: a2-a5-a6xb7xc8=Q
        // Black only moves the h-pawn so it stays out of the way
        val uci = listOf("a2a4", "h7h6", "a4a5", "h6h5", "a5a6", "h5h4", "a6b7", "h4h3", "b7c8q")
        val labels = buildMoveLabels(uci)
        assertTrue(
            "Expected promotion label containing =Q, labels: $labels",
            labels.last().contains("=Q")
        )
    }

    // ──────────────────────────────────────────────────────────────────────
    // buildStoredPgnFromUci
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun `buildStoredPgnFromUci round-trips through parsePgnMoves`() {
        val uci = listOf("d2d4", "d7d5", "g1f3", "g8f6", "e2e3", "e7e6", "f1d3", "f8e7")
        val stored = buildStoredPgnFromUci(uci, event = "Test")
        val parsed = parsePgnMoves(stored)
        assertEquals(uci, parsed)
    }

    @Test
    fun `buildStoredPgnFromUci includes correct move numbers`() {
        val uci = listOf("e2e4", "e7e5", "g1f3")
        val stored = buildStoredPgnFromUci(uci, event = "Test")
        assertTrue(stored.contains("1."))
        assertTrue(stored.contains("2."))
    }
}
