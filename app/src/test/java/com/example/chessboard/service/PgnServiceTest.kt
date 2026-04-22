package com.example.chessboard.service

import com.example.chessboard.boardmodel.InitialBoardFen
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.move.Move
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import com.example.chessboard.ui.screen.gameNotation.TreeSegment
import com.example.chessboard.ui.screen.gameNotation.buildMoveTreeData

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
        val move = Move(Square.E2, Square.E4)
        assertEquals("e4", computeLabel(move, InitialBoardFen))
    }

    @Test
    fun `computeLabel returns correct label for knight move`() {
        val move = Move(Square.G1, Square.F3)
        assertEquals("Nf3", computeLabel(move, InitialBoardFen))
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
        assertEquals("d4", labels[0])
        assertEquals("d5", labels[1])
        assertEquals("Nf3", labels[2])
        assertEquals("Bg4", labels[3])
        // Move 5: Nd2 — two knights can reach d2, computeLabel emits "Nd2" (no disambiguation)
        assertEquals("Nd2", labels[4])
        assertEquals("e6", labels[5])
        assertEquals("Ne5", labels[6])
        assertEquals("Nf6", labels[7])
        assertEquals("h3", labels[8])
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

    // ──────────────────────────────────────────────────────────────────────
    // splitPgnChapters
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun `splitPgnChapters returns one element for a single-game PGN`() {
        val pgn = """
            [Event "Test"]
            [Result "*"]

            1. e4 e5 2. Nf3 *
        """.trimIndent()
        assertEquals(1, splitPgnChapters(pgn).size)
    }

    @Test
    fun `splitPgnChapters returns one element for Najdorf single-chapter study`() {
        assertEquals(1, splitPgnChapters(najdorfStudyPgn).size)
    }

    @Test
    fun `splitPgnChapters splits two concatenated Lichess chapters`() {
        val twoChapters = """
            [Event "Колле: Глава 1"]
            [ECO "D04"]
            [StudyName "Колле"]
            [Result "*"]

            1. d4 d5 2. Nf3 Nf6 3. e3 c6 *
            [Event "Колле: Глава 2"]
            [ECO "A46"]
            [StudyName "Колле"]
            [Result "*"]

            1. d4 Nf6 2. Nf3 e6 3. e3 b6 *
        """.trimIndent()

        val chapters = splitPgnChapters(twoChapters)

        assertEquals(2, chapters.size)
        assertTrue(chapters[0].contains("Глава 1"))
        assertTrue(chapters[1].contains("Глава 2"))
    }

    @Test
    fun `splitPgnChapters each chapter parses as a valid independent PGN`() {
        val twoChapters = """
            [Event "Колле: Глава 1"]
            [ECO "D04"]
            [Result "*"]

            1. d4 d5 2. Nf3 Nf6 3. e3 c6 *
            [Event "Колле: Глава 2"]
            [ECO "A46"]
            [Result "*"]

            1. d4 Nf6 2. Nf3 e6 3. e3 b6 *
        """.trimIndent()

        val chapters = splitPgnChapters(twoChapters)

        for ((index, chapter) in chapters.withIndex()) {
            val lines = parsePgnToUciLines(chapter)
            assertTrue("Chapter $index yielded no lines", lines.isNotEmpty())
        }
    }

    @Test
    fun `splitPgnChapters preserves headers in each chapter`() {
        val twoChapters = """
            [Event "Колле: Введение - Против нимцо"]
            [Date "????.??.??"]
            [White "Введение"]
            [Black "Против нимцо"]
            [Result "*"]
            [Annotator "kluki"]
            [Variant "Standard"]
            [ECO "D04"]
            [Opening "Yusupov-Rubinstein System"]
            [StudyName "Колле"]
            [ChapterName "Введение - Против нимцо"]
            [ChapterURL "https://lichess.org/study/gpkWbrr1/IUmPicxC"]

            1. d4 d5 {}*
            
            
            [Event "Колле: Введение - Против нимцо"]
            [Date "????.??.??"]
            [White "Введение"]
            [Black "Против нимцо"]
            [Result "*"]
            [Annotator "kluki"]
            [Variant "Standard"]
            [ECO "A46"]
            [Opening "Yusupov-Rubinstein System"]
            [StudyName "Колле"]
            [ChapterName "Введение - Против нимцо"]
            [ChapterURL "https://lichess.org/study/gpkWbrr1/IUmPicxC"]

            1. d4 Nf6 *
        """.trimIndent()

        val chapters = splitPgnChapters(twoChapters)

        assertEquals("D04", extractPgnHeaders(chapters[0])["ECO"])
        assertEquals("A46", extractPgnHeaders(chapters[1])["ECO"])
        assertEquals("Колле: Введение - Против нимцо", extractPgnHeaders(chapters[0])["Event"])
        assertEquals("Колле: Введение - Против нимцо", extractPgnHeaders(chapters[1])["Event"])
    }

    // ──────────────────────────────────────────────────────────────────────
    // Real-world Lichess study: Sicilian Najdorf 6.Be3
    // Tests NAG annotations, Cyrillic headers, deep nesting, truncated input
    // ──────────────────────────────────────────────────────────────────────

    private val najdorfStudyPgn = """
        [Event "Найдорф: 6. Се3"]
        [Date "2022.10.26"]
        [Result "*"]
        [Variant "Standard"]
        [ECO "B90"]
        [Opening "Sicilian Defense: Najdorf Variation, English Attack"]
        [StudyName "Найдорф"]
        [ChapterName "6. Се3"]
        [ChapterURL "https://lichess.org/study/4Zb2FBe5/xaJVXVAF"]
        [Annotator "https://lichess.org/@/UnkOwnChessFloMaster"]
        [UTCDate "2022.10.26"]
        [UTCTime "21:03:57"]
        
        1. e4 c5 2. Nf3 d6 3. d4 (3. b4 cxb4 4. d4 (4. a3 Nf6 5. d3 bxa3 6. Bxa3 g6 $15) 4... Nf6 5. Bd3 g6 6. a3 bxa3 7. O-O Bg7 8. Nxa3 O-O $15) (3. Nc3 Nf6 4. e5 dxe5 5. Nxe5 a6 (5... Nbd7) 6. g3 (6. a4 Qc7 7. Nc4 Nc6) 6... Qd6 7. Nf3 Nc6 8. Bg2 e5 9. O-O Be7 $10) 3... cxd4 4. Nxd4 Nf6 5. Nc3 (5. f3 Nc6) 5... a6 6. Be3 (6. f3 e5 7. Nb3 Be6 8. g4?! (8. Bg5 Nbd7 9. Qd2 Be7 10. O-O-O b5 11. h4 b4 12. Nd5 Bxd5 13. exd5 a5 14. Bb5 a4 (14... h6 15. Be3 a4 16. Na1 Qa5 17. Bc6 O-O 18. g4 a3 19. b3 Rad8 20. g5 Nh5 $13) 15. Nc5 Qa5 16. Bxd7+ Nxd7 17. Nxd7 Kxd7 $10) (8. Be3 h5 { переход к главной линии }) (8. f4 exf4 9. Bxf4 Nc6 10. Qd2 d5 $10) 8... Be7 9. Be3 (9. g5 Nh5 10. h4 Ng3 11. Rg1 Nxf1 12. Rxf1 h6 $17) 9... d5 10. g5 d4 $17) (6. g3 e5 7. Nde2 (7. Nb3 Be7 8. Bg2 O-O 9. O-O b5) 7... Be7 8. Bg2 (8. Be3 O-O 9. Bg2 b5 10. a4 (10. Nd5 Nbd7 11. Nec3 Rb8 (11... Bb7 12. O-O Rc8 13. a4 Nxd5 14. Nxd5 Bxd5 15. Qxd5 Nb6 $10) 12. O-O b4 13. Nxe7+ Qxe7 14. Nd5 Nxd5 15. Qxd5 Bb7 16. Qc4 Nf6 17. Rfd1 Rfc8 18. Qd3 h6 19. Bd2 d5 $10) 10... b4 (10... Bb7 11. Nd5 Nxd5 12. exd5 Nd7 $14) 11. Nd5 Nxd5 12. Qxd5 Qd7 13. Qxa8 Bb7 14. Bh3 (14. Qa7 Nc6 15. Qb6 Bd8 16. Bh3 f5 $19) 14... Bxa8 15. Bxd7 Bxe4 16. f3 Bxf3 17. Rf1 Bxe2 18. Kxe2 Nxd7 19. c3 Rb8 $10) 8... O-O (8... b5 9. a3 (9. Nd5 Nbd7 10. Nec3 Nb6) 9... h5) 9. O-O (9. h3) 9... b5 10. Nd5 (10. h3 h5 (10... Nbd7 11. g4)) 10... Nbd7 11. Nec3 Nxd5 12. Nxd5 Nf6 13. Nxe7+ Qxe7 $10) (6. Qf3 g6 7. Bc4 Bg7 8. Be3) 6... e5 7. Nb3 (7. Nf3!? Be7!? { Интреснее } (7... Qc7!? { не пуская Сс4 } 8. a4 (8. h3 Be6 (8... h6 9. g4 b5) 9. Ng5) 8... Be6 9. a5 (9. Ng5 Bd7 (9... Nc6 10. Nxe6 fxe6 11. Bc4 d5 12. exd5 O-O-O 13. O-O Nb4 14. Bb3 exd5 15. Bg5 d4 $14) (9... Qc6? 10. Nxe6 fxe6 11. g4 h6 12. Bg2 $16 { [%csl Gd5][%cal Gg2d5] }) 10. a5 (10. Nd5 Nxd5 11. Qxd5 Bc6 12. Qc4 h6 13. Nf3 Be7 $15) 10... h6!? { лучший вариант } (10... Be7 11. Be2 O-O 12. O-O h6 13. Nf3 $14) 11. Nf3 (11. Bb6 Qc8 12. Nf3 Be6 13. Nd5 Bxd5 14. exd5 Nbd7 15. Be3 Ng4 16. Bc1 Qc5 17. Qd2 Be7 $10) 11... Be7 12. Nd2 (12. Be2 O-O 13. O-O Nc6 14. Bb6 Qc8 15. Qd2 Be6 16. Rfd1 $10) 12... Be6 13. Be2 (13. Na4 Nbd7 14. c4 Ng4 $14) 13... Nbd7 14. O-O O-O 15. Bf3 b5 $10) 9... Qc6 10. Ng5 d5 $14 11. exd5 Bxd5 12. Ra4 $14 Be7 13. Nxd5 (13. Bd3) 13... Nxd5 14. Rc4 Qd7 $10) (7... b5?! 8. a4 b4 9. Nd5 Nbd7 10. Bc4 $16 Nxe4 11. a5) 8. Bc4 (8. h3 Be6 (8... h6 9. g4 (9. a4 Be6 10. Nd2 d5 $10) 9... b5 10. a4 Bb7 11. axb5 O-O 12. g5 hxg5 13. Nxg5 axb5 $15) 9. g4 h6 10. g5 hxg5 11. Nxg5 Nc6 12. Nxe6 fxe6 13. Bc4 Qd7 14. Rg1 O-O-O $13 15. Na4 Kb8 16. Nb6 Qc7 17. Bxe6 Nd4 18. Bxd4 exd4 19. Qxd4 d5 20. Nxd5 Qxc2 21. Rxg7 Rhe8 22. Qe5+ Ka8) 8... O-O 9. O-O Be6 10. Bb3 (10. Bxe6 fxe6 11. Na4 Ng4 12. Qd3 Nxe3 13. Qxe3 b5 14. Nb6 Ra7 15. Nd5 Rb7 16. Qd2 Qd7 17. Nxe7+ Qxe7 $14) 10... Nc6 (10... Qc7!? 11. Qe2 b5 12. Rfd1 Nbd7 $10) 11. Qe2 (11. Bxe6 fxe6 12. Ng5 Qd7 13. Na4 Bd8 $15) 11... Na5 12. Rfd1 (12. Bxe6 fxe6 13. Ng5 Qc8 14. Na4 Qc4 15. Qxc4 Nxc4 16. Nxe6 Rfc8 $15) 12... Nxb3 13. axb3 Qc7 $14 14. Bg5 Rac8 15. Ne1 { [%cal Gf8d8,Gd6d5] } 15... Ne8 16. Bxe7 Qxe7 17. Nd3 Nf6 $14) (7. Nf5? d5 $15 8. Bg5 d4 9. Nd5?? (9. Bxf6 gxf6 10. Nb1 Bxf5 11. exf5 Qd5 $15) 9... Bxf5 10. Bxf6 gxf6 11. exf5 Qxd5 $19) 7... Be6 (7... Be7 8. Qd2 Ng4 9. g3 O-O 10. O-O-O Nxe3 11. Qxe3 Nd7 $10) 8. f3 (8. Qd2!? Nbd7 9. O-O-O (9. f4 b5 10. f5 Bc4 11. O-O-O Rc8 12. Kb1 Be7 13. g4 Nxg4 14. Rg1 Nxe3 15. Qxe3 Qb6 $10) 9... Be7 (9... Rc8 10. f4) 10. f4 (10. Kb1 Ng4 11. g3 Nxe3 12. Qxe3 Qb6 (12... b5 13. f4 Qb6)) 10... Ng4 (10... b5 11. f5 Bc4 12. g4 (12. Kb1 Rc8 13. g4 (13. h3 d5 14. exd5 (14. Nxd5 Nxe4 15. Qe1 Bxd5 16. Rxd5 Qc7 $15) 14... Bb4 $17) 13... Nxg4 14. Rg1 b4 (14... Nxe3 15. Qxe3 Qb6 16. Qg3 Nf6 17. Bxc4 Rxc4 $13 (17... bxc4 18. Nd2)) 15. Nd5 (15. Bxc4 bxc3! 16. Qd5 O-O 17. Rxg4 Nf6 18. Rxg7+ Kxg7 19. Qd3 cxb2 20. Rg1+ Kh8 $15) 15... Bxd5 16. Rxg4 Nf6 17. Rxg7 Bxe4 18. Rc1 Bxf5 19. Bxa6 Rb8 20. Qe2 $15 Bg6 21. Bb5+ Nd7 22. Rd1 Qc8 23. Ba4 Ra8 $10) 12... Nxg4 13. Rg1 Ndf6 14. Kb1 Bxf1 15. Rdxf1 $15 Nxe3 16. Qxe3 b4 17. Nd5 Nxd5 18. exd5 Bf6 $15 19. Nd2 Rb8 20. Ne4 Qb6 21. Qh3 a5 22. Nxf6+ gxf6 $19) (10... exf4 11. Bxf4 Ne5) 11. g3 Nxe3 12. Qxe3 b5 13. Kb1 Qb6 $10) (8. f4?! exf4 9. Bxf4 Nc6 10. Qd2 d5 $10) (8. h3!? Be7 (8... Nbd7 9. g4 (9. Qf3 Rc8 10. Bd3 Be7 11. O-O Rg8 $15) 9... h6 10. Qd2 b5 11. Bg2 Be7 12. Nd5 Bxd5 13. exd5 Nb6 14. Na5 Nc4 15. Nxc4 bxc4 16. Qb4 Nd7 17. Qxc4 Bg5 18. Bxg5 Qxg5 19. h4 Qf6 $44) (8... h5) 9. Qf3 (9. g4 d5 10. exd5 Nxd5 11. Bg2 Nxe3 12. Qxd8+ Bxd8 13. fxe3 Bh4+ 14. Kf1 Bc4+ 15. Kg1 Nc6 $10) 9... O-O 10. O-O-O b5 11. g4 b4 12. Nd5 Nxd5 13. exd5 Bc8 14. Nd2 (14. Bd3 a5 15. h4 Nd7 16. Nd2 a4) 14... a5 $132) (8. Rg1 Ng4 9. Bd2 Qb6 10. Qe2 Nxh2 11. Be3 Qc6 12. f3 Nxf1 $15) (8. Be2 { переход к главе 3 }) 8... h5 { [%cal Rg2g4] } (8... d5?! 9. exd5 Nxd5 10. Nxd5 Bxd5 11. Qd2 $16) (8... Be7 9. Qd2 Nbd7 (9... O-O 10. O-O-O Nbd7 11. g4 b5 12. g5 $14) 10. g4 Nb6 11. O-O-O Rc8 12. Kb1 Nc4 13. Bxc4 Rxc4 14. g5 Nh5 15. Nd5 $16) 9. Nd5 (9. Qd2 Nbd7 10. O-O-O (10. Be2 Rc8 11. O-O (11. O-O-O b5 12. Nd5 Bxd5 13. exd5 Nb6 14. Bxb6 Qxb6 $15) 11... Be7 $10) (10. Nd5 Bxd5 11. exd5 g6 $10 12. O-O-O (12. Na5 Qc7 13. c4 Bg7 14. Be2 e4 15. O-O exf3 16. gxf3 O-O 17. b4 Rfe8 18. Rac1 Rxe3 19. Qxe3 Re8 20. Qd2 Bh6 21. Qxh6 Rxe2 22. Rf2 Rxf2 23. Kxf2 Ne5 24. Qd2 Qd7 25. Kg2 h4 26. Nb3 h3+ 27. Kh1 Nxf3 28. Qf4 Ne5 29. Qxf6 Qg4 30. Qf2 Qe4+ 31. Kg1 Nd3 32. Qg3 Nxc1 33. Nxc1 Qd4+ 34. Kf1 Qd1+ 35. Qe1 Qf3+ 36. Qf2 $10) (12. Be2 Bg7 13. Na5 Qc7 14. O-O-O (14. O-O e4 15. c4 exf3 16. gxf3 O-O 17. b4 Rfe8 18. Rac1 Rxe3 19. Qxe3 Re8 20. Qd2 Bh6 $10) 14... Nb6 $15) 12... Nb6 13. Qa5 (13. Kb1 Nbxd5 (13... Nfxd5 14. Bf2 Nf6 15. Bh4 Be7 16. Bxf6 Bxf6 17. Qxd6 Qxd6 18. Rxd6 Bd8 $10) 14. Bg5 Be7 15. Bd3 Qc7 16. Rhe1 Nb6 17. a4 O-O-O 18. a5 Nbd7 19. Be3 Kb8 20. Bg1 d5 21. c3 Qc6 22. Qf2 Bd6 $10) 13... Bh6 $10) 10... Be7!? (10... Rc8!? 11. Nd5 (11. Kb1 b5 12. Nd5 (12. h3 h4 13. Bg5 Be7 { [%cal Gd8b6,Gb5b4,Ga6a5,Ga5a4] }) (12. g3 Be7 13. Nd5 (13. h3 Nb6) 13... Bxd5 (13... Nxd5 14. exd5 Bf5 15. Bd3 Bxd3 16. Qxd3 O-O $10) 14. exd5 Nb6 15. Bh3 Nc4 16. Qe2 Nxe3 17. Qxe3 Rc4 18. Qa7 $14) 12... Bxd5 13. exd5 Nb6 14. Bxb6 Qxb6 15. Bd3 (15. Na5 Rc5 $15 16. c4 bxc4 17. Nxc4 Qc7 $15 18. Ne3 g6 19. Bxa6 Bh6 $17) 15... g6 $15 16. Rhe1 Bh6 17. Qa5 (17. f4 O-O 18. g3 Ng4 $19 { [%csl Gf2,Gf4][%cal Gg4f2,Ge5f4] }) 17... Nd7 $10) 11... Bxd5 12. exd5 g6 13. Kb1 Bg7 $14 14. Be2 (14. Na5 Qc7) 14... O-O 15. h3 e4 16. f4 b5 $14 { [%csl Gb6,Gd5,Gc4][%cal Gf6d5,Gd7b6,Gb6d5,Gb6c4] } 17. Na5 (17. g4 Nb6 18. gxh5 Nc4 19. Bxc4 bxc4 20. Nd4 c3 $14) 17... Nxd5 18. Qxd5 Qxa5) 11. Kb1 (11. Nd5 Bxd5 12. exd5 Rc8 13. Kb1 (13. Na5 Nb6 14. Nxb7 Qc7 15. Bxb6 Qxb6 16. Na5 Rc5 $44) 13... Nb6 $15 14. Bxb6 (14. Na5 Nbxd5 15. Nxb7 Qc7 16. Bxa6 O-O $19) 14... Qxb6 15. Bd3 O-O 16. Rhe1 Rfe8 17. g4 g6 18. h4 Bf8 $10 { [%csl Gb2][%cal Gf8g7,Gg7b2,Ge5e4] }) (11. h3 h4 $10) (11. Be2 Rc8 12. Kb1 b5 13. Nd5 Bxd5 14. exd5 Nb6 15. Bxb6 Qxb6 $10) 11... b5 12. Nd5 (12. h3 Qc7 (12... h4!? $13) 13. Nd5 (13. Bd3 b4 $15) 13... Bxd5 14. exd5 Nb6 15. Bxb6 Qxb6 16. Na5 Rc8 17. c4 bxc4 18. Nxc4 Qa7 19. Na5 Rc5 { [%cal Gc5d5] } 20. Nc6 Qd7 $14) 12... Bxd5 13. exd5 Nb6 14. Bxb6 (14. Bd3 Nfxd5 15. Be4 Nxe3 16. Qxe3 Rc8 17. Bb7 Rb8 $17) 14... Qxb6 15. Na5 Rc8 16. Nc6 (16. c4 bxc4 17. Nxc4 (17. Bxc4?? Rc5 $19) 17... Qa7 18. Na5 Rc5 19. Nc6 Qd7 $14 20. Bd3 Rxd5) (16. Bd3 b4 17. Nc4 Qc5 18. Ne3 a5 19. Nf5 g6 20. Nxe7 Kxe7 $15 { [%cal Ge7f8,Gf8g7] }) 16... Nxd5 17. Nxe7 Nxe7 18. Qxd6 Qxd6 19. Rxd6 Nc6 $10 20. a4 Ke7 21. Rd1 Rhd8) (9. Be2 Nbd7 10. O-O (10. Nd5 Bxd5 11. exd5 g6 12. Qd2 Bg7 13. O-O b6 14. c4 $10) 10... Rc8 $14 { [%cal Gh5h4,Gf6h5] } 11. Qd2 h4 12. h3 Be7 $10) 9... Bxd5 10. exd5 Nbd7 11. Qd2 (11. c4?! g6 12. Bd3 Bg7 13. O-O O-O $10 { [%cal Gb7b6,Ga6a5,Ga5a4,Gd7c5] }) 11... g6 12. Be2 (12. O-O-O Nb6 (12... Bg7 13. Kb1 Qc7 (13... b5 14. Na5 Nb6 15. Nc6 Qc7 16. c4 bxc4 17. Bxb6 Qxb6 18. Bxc4 O-O 19. Rhe1 $14) 14. c4 b6 $14) 13. Qa5 (13. Kb1 Nfxd5 $10 14. Bf2 (14. Bg5 Be7 15. Bxe7 Qxe7 16. Qa5 Ne3 17. Qxb6 Nxd1 18. Bc4 Nxb2 19. Kxb2 Rc8 $14) 14... Nf6 15. Bh4 Be7 16. Bxf6 Bxf6 17. Qxd6 Qxd6 18. Rxd6 Bd8 $10) 13... Bh6 14. Bxh6 Rxh6 $10 { [%cal Ge8f8,Gf8g7,Gh6h8] }) (12. Na5 Qc7 13. c4 Bg7 14. Rc1 (14. Be2 e4 15. O-O exf3 16. gxf3 O-O 17. b4 Rfe8 18. Rac1 Rxe3 19. Qxe3 Re8 20. Qd2 Bh6 21. f4 Ne4 22. Qc2 Qb6+ 23. Kh1 Qe3 24. Rf3 Nf2+ 25. Kg1) 14... e4 15. Be2 exf3 16. gxf3 (16. Bxf3 O-O 17. O-O Ne5) 16... O-O 17. O-O Rfe8 18. b4 Rxe3! (18... Ne5 19. c5 Qd7 20. c6 bxc6 21. dxc6 Qe7 $10) 19. Qxe3 Re8 20. Qd2 (20. Qf2 Bh6 21. f4 (21. Rc3 Nxd5 $19) 21... Re4 $15) 20... Bh6 $10) 12... Bg7 13. O-O (13. c4?! a5 14. a4 (14. Nc1 a4 $15) 14... b6 15. O-O $15 Nc5) (13. Na5!? Qc7 14. c4 e4! 15. O-O (15. f4 Ng4 16. Bxg4 hxg4 $15) 15... exf3 16. Bxf3 O-O $10 { [%cal Gg7b2,Gb2b4] }) 13... b6 14. c4 $10 { [%csl Gb2][%cal Ge8g8,Gd7c5,Ga6a5,Ga5a4,Ge5e4,Gg7b2] } (14. a4 Qc7) *
        """.trimIndent()

    @Test
    fun `parsePgnToUciLines handles real-world Najdorf study PGN without throwing`() {
        val lines = parsePgnToUciLines(najdorfStudyPgn)
        assertTrue(
            "Expected multiple lines from Najdorf study, got ${lines.size}",
            lines.size > 1
        )
    }

    @Test
    fun `uciMovesToMoves succeeds for every line parsed from Najdorf study`() {
        // Regression: importing this PGN in-app produced "None of the imported lines could be saved".
        // Root cause lives in the parse→convert pipeline: if any line yields a UCI string that
        // uciMovesToMoves cannot replay on a Board, the save coroutine crashes silently and
        // addGameAndGetId is never called.
        val lines = parsePgnToUciLines(najdorfStudyPgn)
        val failures = mutableListOf<String>()

        for ((index, line) in lines.withIndex()) {
            runCatching { uciMovesToMoves(line) }
                .onSuccess { moves ->
                    if (moves.size != line.size) {
                        failures.add("Line $index: expected ${line.size} moves, got ${moves.size}. UCI: $line")
                    }
                }
                .onFailure { e ->
                    failures.add("Line $index (${line.size} moves) threw: ${e.message}. UCI: $line")
                }
        }

        assertTrue(
            "${failures.size} line(s) failed to convert:\n${failures.joinToString("\n")}",
            failures.isEmpty()
        )
    }

    @Test
    fun `parsePgnToUciLines Najdorf study main line starts with correct Sicilian moves`() {
        val lines = parsePgnToUciLines(najdorfStudyPgn)

        // Main line: 1.e4 c5 2.Nf3 d6 3.d4 cxd4 4.Nxd4 Nf6 5.Nc3 a6 6.Be3
        val expectedPrefix = listOf(
            "e2e4", "c7c5", "g1f3", "d7d6",
            "d2d4", "c5d4", "f3d4", "g8f6",
            "b1c3", "a7a6", "c1e3"
        )

        val mainLine = lines.first()
        assertTrue(
            "Main line too short (${mainLine.size} moves). Got: $mainLine",
            mainLine.size >= expectedPrefix.size
        )
        assertEquals(
            "Main line prefix mismatch",
            expectedPrefix,
            mainLine.take(expectedPrefix.size)
        )
    }
}
