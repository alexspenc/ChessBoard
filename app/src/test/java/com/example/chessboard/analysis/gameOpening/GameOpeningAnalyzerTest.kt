package com.example.chessboard.analysis.gameOpening

/**
 * Tests game-vs-opening analysis behavior without database or UI dependencies.
 * Keep tests here focused on GameOpeningAnalyzer result semantics and matching modes.
 * Validation date: 2026-06-25
 */
import com.example.chessboard.analysis.GameOpeningAnalyzer
import com.example.chessboard.analysis.GameOpeningBookTooShort
import com.example.chessboard.analysis.GameOpeningDeviation
import com.example.chessboard.analysis.GameOpeningInvalidGameMove
import com.example.chessboard.analysis.GameOpeningInvalidInitialPosition
import com.example.chessboard.analysis.GameOpeningMatchesKnownOpening
import com.example.chessboard.analysis.GameOpeningNoMatchingOpening
import com.example.chessboard.analysis.GameOpeningOpponentLeftBook
import com.example.chessboard.analysis.OpeningMatchMode
import com.example.chessboard.analysis.OpeningSide
import com.example.chessboard.boardmodel.InitialBoardFen
import com.example.chessboard.entity.LineEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GameOpeningAnalyzerTest {

    private val analyzer = GameOpeningAnalyzer()

    /**
     * Verifies that a game with a different first move is not treated as a deviation.
     * The analyzer should report that no saved opening matched from the start.
     */
    @Test
    fun `returns no matching opening when first move differs from book`() {
        val result = analyze(
            gameMoves = listOf("d2d4"),
            bookLines = listOf(line(id = 1, moves = listOf("e2e4"))),
            selectedSide = OpeningSide.WHITE,
            matchMode = OpeningMatchMode.MOVE_SEQUENCE,
        )

        val noMatch = assertResult<GameOpeningNoMatchingOpening>(result)
        assertEquals(OpeningSide.WHITE, noMatch.selectedSide)
        assertEquals(OpeningMatchMode.MOVE_SEQUENCE, noMatch.matchMode)
        assertEquals(InitialPositionKey, noMatch.positionFen)
        assertEquals(0, noMatch.ply)
        assertEquals("d2d4", noMatch.playedMoveUci)
        assertEquals(listOf("e2e4"), noMatch.knownMoves.map { move -> move.moveUci })
    }

    /**
     * Verifies that selected-side divergence after a known prefix is a real deviation.
     * The result should include the played move and the book continuations expected instead.
     */
    @Test
    fun `returns deviation when selected side leaves book after matching prefix`() {
        val result = analyze(
            gameMoves = listOf("e2e4", "e7e5", "f1c4"),
            bookLines = listOf(line(id = 1, moves = listOf("e2e4", "e7e5", "g1f3"))),
            selectedSide = OpeningSide.WHITE,
            matchMode = OpeningMatchMode.MOVE_SEQUENCE,
        )

        val deviation = assertResult<GameOpeningDeviation>(result)
        assertEquals(OpeningSide.WHITE, deviation.selectedSide)
        assertEquals(OpeningMatchMode.MOVE_SEQUENCE, deviation.matchMode)
        assertEquals(AfterE4E5Key, deviation.positionFen)
        assertEquals(2, deviation.ply)
        assertEquals("f1c4", deviation.playedMoveUci)
        assertEquals(AfterE4E5Bc4Key, deviation.playedResultFen)
        assertEquals(listOf("g1f3"), deviation.expectedMoves.map { move -> move.moveUci })
        assertEquals(listOf(0), deviation.matchingLineRefs.map { ref -> ref.lineIndex })
    }

    /**
     * Verifies that position mode reports selected-side divergence after a known position.
     * This covers the same result semantics as move-sequence mode using the position index path.
     */
    @Test
    fun `position mode returns deviation when selected side leaves book after matching prefix`() {
        val result = analyze(
            gameMoves = listOf("e2e4", "e7e5", "f1c4"),
            bookLines = listOf(line(id = 1, moves = listOf("e2e4", "e7e5", "g1f3"))),
            selectedSide = OpeningSide.WHITE,
            matchMode = OpeningMatchMode.POSITION,
        )

        val deviation = assertResult<GameOpeningDeviation>(result)
        assertEquals(OpeningSide.WHITE, deviation.selectedSide)
        assertEquals(OpeningMatchMode.POSITION, deviation.matchMode)
        assertEquals(AfterE4E5Key, deviation.positionFen)
        assertEquals(2, deviation.ply)
        assertEquals("f1c4", deviation.playedMoveUci)
        assertEquals(AfterE4E5Bc4Key, deviation.playedResultFen)
        assertEquals(listOf("g1f3"), deviation.expectedMoves.map { move -> move.moveUci })
        assertEquals(listOf(0), deviation.matchingLineRefs.map { ref -> ref.lineIndex })
    }

    /**
     * Verifies that opponent divergence is reported separately from selected-side deviation.
     * This keeps the user-facing analysis focused on the chosen side's opening choices.
     */
    @Test
    fun `returns opponent left book when opponent leaves book`() {
        val result = analyze(
            gameMoves = listOf("e2e4", "c7c5"),
            bookLines = listOf(line(id = 1, moves = listOf("e2e4", "e7e5", "g1f3"))),
            selectedSide = OpeningSide.WHITE,
            matchMode = OpeningMatchMode.MOVE_SEQUENCE,
        )

        val opponentLeftBook = assertResult<GameOpeningOpponentLeftBook>(result)
        assertEquals(OpeningSide.WHITE, opponentLeftBook.selectedSide)
        assertEquals(OpeningMatchMode.MOVE_SEQUENCE, opponentLeftBook.matchMode)
        assertEquals(AfterE4Key, opponentLeftBook.positionFen)
        assertEquals(1, opponentLeftBook.ply)
        assertEquals("c7c5", opponentLeftBook.playedMoveUci)
        assertEquals(AfterE4C5Key, opponentLeftBook.playedResultFen)
        assertEquals(listOf("e7e5"), opponentLeftBook.expectedMoves.map { move -> move.moveUci })
        assertEquals(listOf(0), opponentLeftBook.matchingLineRefs.map { ref -> ref.lineIndex })
    }

    /**
     * Verifies that position mode also separates opponent divergence from selected-side deviation.
     * This protects the branch that decides the result type after a known indexed position.
     */
    @Test
    fun `position mode returns opponent left book when opponent leaves book`() {
        val result = analyze(
            gameMoves = listOf("e2e4", "c7c5"),
            bookLines = listOf(line(id = 1, moves = listOf("e2e4", "e7e5", "g1f3"))),
            selectedSide = OpeningSide.WHITE,
            matchMode = OpeningMatchMode.POSITION,
        )

        val opponentLeftBook = assertResult<GameOpeningOpponentLeftBook>(result)
        assertEquals(OpeningSide.WHITE, opponentLeftBook.selectedSide)
        assertEquals(OpeningMatchMode.POSITION, opponentLeftBook.matchMode)
        assertEquals(AfterE4Key, opponentLeftBook.positionFen)
        assertEquals(1, opponentLeftBook.ply)
        assertEquals("c7c5", opponentLeftBook.playedMoveUci)
        assertEquals(AfterE4C5Key, opponentLeftBook.playedResultFen)
        assertEquals(listOf("e7e5"), opponentLeftBook.expectedMoves.map { move -> move.moveUci })
        assertEquals(listOf(0), opponentLeftBook.matchingLineRefs.map { ref -> ref.lineIndex })
    }

    /**
     * Verifies that matching the full saved line is not enough when the game continues past it.
     * The analyzer should report that the saved opening is too short and identify the next game move.
     */
    @Test
    fun `returns book too short when book ends before game continues`() {
        val result = analyze(
            gameMoves = listOf("e2e4", "e7e5", "g1f3"),
            bookLines = listOf(line(id = 1, moves = listOf("e2e4", "e7e5"))),
            selectedSide = OpeningSide.WHITE,
            matchMode = OpeningMatchMode.MOVE_SEQUENCE,
            minimumKnownPrefixPly = 8,
        )

        val tooShort = assertResult<GameOpeningBookTooShort>(result)
        assertEquals(OpeningSide.WHITE, tooShort.selectedSide)
        assertEquals(OpeningMatchMode.MOVE_SEQUENCE, tooShort.matchMode)
        assertEquals(AfterE4E5Key, tooShort.lastKnownPositionFen)
        assertEquals(2, tooShort.matchedPly)
        assertEquals(8, tooShort.minimumKnownPrefixPly)
        assertEquals("g1f3", tooShort.nextGameMoveUci)
        assertEquals(listOf(0), tooShort.endedLineRefs.map { ref -> ref.lineIndex })
    }

    /**
     * Verifies that position mode reports short book coverage when an indexed line ends.
     * This covers the path that reads ended refs from the position index.
     */
    @Test
    fun `position mode returns book too short when book ends before game continues`() {
        val result = analyze(
            gameMoves = listOf("e2e4", "e7e5", "g1f3"),
            bookLines = listOf(line(id = 1, moves = listOf("e2e4", "e7e5"))),
            selectedSide = OpeningSide.WHITE,
            matchMode = OpeningMatchMode.POSITION,
            minimumKnownPrefixPly = 8,
        )

        val tooShort = assertResult<GameOpeningBookTooShort>(result)
        assertEquals(OpeningSide.WHITE, tooShort.selectedSide)
        assertEquals(OpeningMatchMode.POSITION, tooShort.matchMode)
        assertEquals(AfterE4E5Key, tooShort.lastKnownPositionFen)
        assertEquals(2, tooShort.matchedPly)
        assertEquals(8, tooShort.minimumKnownPrefixPly)
        assertEquals("g1f3", tooShort.nextGameMoveUci)
        assertEquals(listOf(0), tooShort.endedLineRefs.map { ref -> ref.lineIndex })
    }

    /**
     * Verifies that a game ending inside a longer saved opening is considered a successful match.
     * The result should expose the final matched position and the book lines that still cover it.
     */
    @Test
    fun `returns matches known opening when game ends inside book`() {
        val result = analyze(
            gameMoves = listOf("e2e4", "e7e5", "g1f3"),
            bookLines = listOf(line(id = 1, moves = listOf("e2e4", "e7e5", "g1f3", "b8c6"))),
            selectedSide = OpeningSide.WHITE,
            matchMode = OpeningMatchMode.MOVE_SEQUENCE,
        )

        val match = assertResult<GameOpeningMatchesKnownOpening>(result)
        assertEquals(OpeningSide.WHITE, match.selectedSide)
        assertEquals(OpeningMatchMode.MOVE_SEQUENCE, match.matchMode)
        assertEquals(3, match.matchedPly)
        assertEquals(AfterE4E5Nf3Key, match.finalPositionFen)
        assertEquals(listOf(0), match.matchingLineRefs.map { ref -> ref.lineIndex })
    }

    /**
     * Verifies that a short game is not penalized for ending before the requested minimum depth.
     * If every played move is covered by the book, the result should still be a known-opening match.
     */
    @Test
    fun `returns matches known opening when short game ends before minimum`() {
        val result = analyze(
            gameMoves = listOf("e2e4"),
            bookLines = listOf(line(id = 1, moves = listOf("e2e4", "e7e5", "g1f3", "b8c6"))),
            selectedSide = OpeningSide.WHITE,
            matchMode = OpeningMatchMode.MOVE_SEQUENCE,
            minimumKnownPrefixPly = 8,
        )

        val match = assertResult<GameOpeningMatchesKnownOpening>(result)
        assertEquals(OpeningSide.WHITE, match.selectedSide)
        assertEquals(OpeningMatchMode.MOVE_SEQUENCE, match.matchMode)
        assertEquals(1, match.matchedPly)
        assertEquals(AfterE4Key, match.finalPositionFen)
        assertEquals(listOf(0), match.matchingLineRefs.map { ref -> ref.lineIndex })
    }

    /**
     * Verifies that malformed UCI text is returned as an analysis result instead of throwing.
     * This keeps user-entered game data errors separate from invalid analyzer parameters.
     */
    @Test
    fun `returns invalid game move for malformed uci`() {
        val result = analyze(
            gameMoves = listOf("not-uci"),
            bookLines = listOf(line(id = 1, moves = listOf("e2e4"))),
            selectedSide = OpeningSide.WHITE,
            matchMode = OpeningMatchMode.MOVE_SEQUENCE,
        )

        val invalidMove = assertResult<GameOpeningInvalidGameMove>(result)
        assertEquals(OpeningSide.WHITE, invalidMove.selectedSide)
        assertEquals(OpeningMatchMode.MOVE_SEQUENCE, invalidMove.matchMode)
        assertEquals(InitialPositionKey, invalidMove.positionFen)
        assertEquals(0, invalidMove.ply)
        assertEquals("not-uci", invalidMove.moveUci)
        assertEquals(GameOpeningInvalidGameMove.Reason.INVALID_UCI, invalidMove.reason)
    }

    /**
     * Verifies that syntactically valid UCI that is illegal in the position is reported as invalid.
     * The analyzer should not throw for a bad game move supplied by the caller.
     */
    @Test
    fun `returns invalid game move for illegal move`() {
        val result = analyze(
            gameMoves = listOf("e2e5"),
            bookLines = listOf(line(id = 1, moves = listOf("e2e4"))),
            selectedSide = OpeningSide.WHITE,
            matchMode = OpeningMatchMode.MOVE_SEQUENCE,
        )

        val invalidMove = assertResult<GameOpeningInvalidGameMove>(result)
        assertEquals(OpeningSide.WHITE, invalidMove.selectedSide)
        assertEquals(OpeningMatchMode.MOVE_SEQUENCE, invalidMove.matchMode)
        assertEquals(InitialPositionKey, invalidMove.positionFen)
        assertEquals(0, invalidMove.ply)
        assertEquals("e2e5", invalidMove.moveUci)
        assertEquals(GameOpeningInvalidGameMove.Reason.ILLEGAL_MOVE, invalidMove.reason)
    }

    /**
     * Verifies that an invalid game initial FEN is represented by a dedicated result.
     * Book-line data errors may still throw, but the analyzed game's start position is user input.
     */
    @Test
    fun `returns invalid initial position for bad game initial fen`() {
        val result = analyze(
            gameMoves = listOf("e2e4"),
            gameInitialFen = "not a fen",
            bookLines = listOf(line(id = 1, moves = listOf("e2e4"))),
            selectedSide = OpeningSide.WHITE,
            matchMode = OpeningMatchMode.MOVE_SEQUENCE,
        )

        val invalidInitialPosition = assertResult<GameOpeningInvalidInitialPosition>(result)
        assertEquals(OpeningSide.WHITE, invalidInitialPosition.selectedSide)
        assertEquals(OpeningMatchMode.MOVE_SEQUENCE, invalidInitialPosition.matchMode)
        assertEquals("not a fen", invalidInitialPosition.initialFen)
    }

    /**
     * Verifies that position mode includes book lines that reached the final position by transposition.
     * The game follows the first line exactly, while the second line reaches the same position in another order.
     */
    @Test
    fun `position mode includes transposed matching line refs`() {
        val result = analyze(
            gameMoves = listOf("g1f3", "g8f6", "b1c3", "b8c6"),
            bookLines = listOf(
                line(id = 1, moves = listOf("g1f3", "g8f6", "b1c3", "b8c6")),
                line(id = 2, moves = listOf("b1c3", "g8f6", "g1f3", "b8c6")),
            ),
            selectedSide = OpeningSide.WHITE,
            matchMode = OpeningMatchMode.POSITION,
        )

        val match = assertResult<GameOpeningMatchesKnownOpening>(result)
        assertEquals(OpeningSide.WHITE, match.selectedSide)
        assertEquals(OpeningMatchMode.POSITION, match.matchMode)
        assertEquals(4, match.matchedPly)
        assertEquals(AfterFourKnightsDevelopedKey, match.finalPositionFen)
        assertEquals(listOf(0, 1), match.matchingLineRefs.map { ref -> ref.lineIndex })
    }

    /**
     * Verifies that move-sequence mode does not include lines that only match by transposition.
     * Exact move order should keep the recognized opening lines limited to the matching sequence prefix.
     */
    @Test
    fun `move sequence mode excludes transposed matching line refs`() {
        val result = analyze(
            gameMoves = listOf("g1f3", "g8f6", "b1c3", "b8c6"),
            bookLines = listOf(
                line(id = 1, moves = listOf("g1f3", "g8f6", "b1c3", "b8c6")),
                line(id = 2, moves = listOf("b1c3", "g8f6", "g1f3", "b8c6")),
            ),
            selectedSide = OpeningSide.WHITE,
            matchMode = OpeningMatchMode.MOVE_SEQUENCE,
        )

        val match = assertResult<GameOpeningMatchesKnownOpening>(result)
        assertEquals(OpeningSide.WHITE, match.selectedSide)
        assertEquals(OpeningMatchMode.MOVE_SEQUENCE, match.matchMode)
        assertEquals(4, match.matchedPly)
        assertEquals(AfterFourKnightsDevelopedKey, match.finalPositionFen)
        assertEquals(listOf(0), match.matchingLineRefs.map { ref -> ref.lineIndex })
    }

    /**
     * Verifies that impossible analyzer configuration is rejected with an exception.
     * Negative minimum depth is a programming/configuration error, not an analysis result.
     */
    @Test
    fun `throws for negative minimumKnownPrefixPly`() {
        try {
            analyze(
                gameMoves = listOf("e2e4"),
                bookLines = listOf(line(id = 1, moves = listOf("e2e4"))),
                selectedSide = OpeningSide.WHITE,
                matchMode = OpeningMatchMode.MOVE_SEQUENCE,
                minimumKnownPrefixPly = -1,
            )
            fail("Expected IllegalArgumentException for negative minimumKnownPrefixPly")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    private fun analyze(
        gameMoves: List<String>,
        gameInitialFen: String = InitialBoardFen,
        bookLines: List<LineEntity>,
        selectedSide: OpeningSide,
        matchMode: OpeningMatchMode,
        minimumKnownPrefixPly: Int = 0,
    ) = analyzer.analyze(
        gameMoves = gameMoves,
        gameInitialFen = gameInitialFen,
        bookLines = bookLines,
        selectedSide = selectedSide,
        matchMode = matchMode,
        minimumKnownPrefixPly = minimumKnownPrefixPly,
    )

    private inline fun <reified T> assertResult(result: Any): T {
        assertTrue(
            "Expected ${T::class.java.simpleName}, got ${result::class.java.simpleName}",
            result is T,
        )
        return result as T
    }

    private fun line(
        id: Long,
        moves: List<String>,
        initialFen: String = InitialBoardFen,
    ): LineEntity {
        return LineEntity(
            id = id,
            pgn = storedPgn(moves),
            initialFen = initialFen,
        )
    }

    private fun storedPgn(moves: List<String>): String {
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
        const val AfterE4Key =
            "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3"
        const val AfterE4C5Key =
            "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6"
        const val AfterE4E5Key =
            "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6"
        const val AfterE4E5Bc4Key =
            "rnbqkbnr/pppp1ppp/8/4p3/2B1P3/8/PPPP1PPP/RNBQK1NR b KQkq -"
        const val AfterE4E5Nf3Key =
            "rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq -"
        const val AfterFourKnightsDevelopedKey =
            "r1bqkb1r/pppppppp/2n2n2/8/8/2N2N2/PPPPPPPP/R1BQKB1R w KQkq -"
    }
}
