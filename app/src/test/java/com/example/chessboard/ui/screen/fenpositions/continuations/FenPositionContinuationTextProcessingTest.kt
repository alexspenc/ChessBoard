package com.example.chessboard.ui.screen.fenpositions.continuations

/*
 * File role: verifies pasted continuation parsing and the add-screen text state contract.
 * Allowed here:
 * - source-FEN parsing, filtering statistics, stored coverage, SAN previews, and stale-result tests
 * Not allowed here:
 * - Compose rendering, real debounce timing, Room integration, or continuation insertion
 * Validation date: 2026-09-02
 */

import com.example.chessboard.service.PgnParseErrorStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FenPositionContinuationTextProcessingTest {

    /*
     * This is the representative copied-from-position PGN supplied for the feature. It protects
     * the complete path from header trimming and variation expansion through full SAN previews;
     * all three branches must survive in parser order.
     */
    @Test
    fun `from-position PGN produces three ordered continuation previews`() {
        val headerFen = "rnbqkbnr/ppp1pppp/8/3p4/8/5NP1/PPPPPP1P/RNBQKB1R b KQkq"
        val startFen = "$headerFen -"
        val text = """
            [Variant "From Position"]
            [FEN "$headerFen"]

            1... Nf6 2. Bg2 g6 (2... c5 3. O-O Nc6 4. d4 e6 5. c4) 3. O-O Bg7
            (3... c6 4. c4 Bg7 5. cxd5 cxd5 6. d4) 4. c4 c6
        """.trimIndent()

        val parsedLines = parseFenPositionContinuationText(
            text = text,
            startFen = startFen,
            errorStrings = ErrorStrings,
            noValidLinesMessage = NoValidLinesMessage,
        )
        val preparation = prepareFenPositionContinuationText(parsedLines)
        val result = buildFenPositionContinuationTextProcessingResult(
            preparation = preparation,
            storedUciLines = emptyList(),
            startFen = startFen,
        )

        assertEquals(3, result.recognizedLinesCount)
        assertEquals(0, result.exactDuplicateLinesCount)
        assertEquals(0, result.coveredPrefixLinesCount)
        assertEquals(0, result.coveredByStoredLinesCount)
        assertEquals(
            listOf(
                "1... Nf6 2. Bg2 g6 3. O-O Bg7 4. c4 c6",
                "1... Nf6 2. Bg2 g6 3. O-O c6 4. c4 Bg7 5. cxd5 cxd5 6. d4",
                "1... Nf6 2. Bg2 c5 3. O-O Nc6 4. d4 e6 5. c4",
            ),
            result.newSanLines,
        )
    }

    /*
     * The shared parser normally removes equal branches. This feature deliberately preserves
     * them until batch preparation so the result dialog can report the actual duplicate count.
     */
    @Test
    fun `exact duplicate branches are counted before removal`() {
        val parsedLines = parseFenPositionContinuationText(
            text = "1. e4 (1. e4 e5) e5",
            startFen = InitialFen,
            errorStrings = ErrorStrings,
            noValidLinesMessage = NoValidLinesMessage,
        )

        val preparation = prepareFenPositionContinuationText(parsedLines)

        assertEquals(2, preparation.sourceLinesCount)
        assertEquals(1, preparation.exactDuplicateLinesCount)
        assertEquals(0, preparation.coveredPrefixLinesCount)
        assertEquals(listOf(listOf("e2e4", "e7e5")), preparation.preparedUciLines)
    }

    /*
     * Stored coverage is directional: a saved long line covers a new short prefix, while a saved
     * short line must not prevent adding its longer continuation. This test keeps both cases in
     * one result so their count and surviving order are checked together.
     */
    @Test
    fun `stored comparison removes only candidates covered by equal or longer lines`() {
        val longerCandidate = listOf("e2e4", "e7e5", "g1f3")
        val coveredShortCandidate = listOf("d2d4")
        val preparation = prepareFenPositionContinuationText(
            parsedUciLines = listOf(longerCandidate, coveredShortCandidate),
        )

        val result = buildFenPositionContinuationTextProcessingResult(
            preparation = preparation,
            storedUciLines = listOf(
                listOf("e2e4", "e7e5"),
                listOf("d2d4", "d7d5"),
            ),
            startFen = InitialFen,
        )

        assertEquals(listOf(longerCandidate), result.newUciLines)
        assertEquals(listOf("1. e4 e5 2. Nf3"), result.newSanLines)
        assertEquals(1, result.coveredByStoredLinesCount)
    }

    /*
     * A non-empty block without moves must be an error rather than a successful zero-line result;
     * otherwise the UI could offer a meaningless save after accepting only PGN metadata.
     */
    @Test
    fun `header-only text reports no valid lines`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            parseFenPositionContinuationText(
                text = "[Event \"No moves\"]",
                startFen = InitialFen,
                errorStrings = ErrorStrings,
                noValidLinesMessage = NoValidLinesMessage,
            )
        }

        assertEquals(NoValidLinesMessage, error.message)
    }

    /*
     * Parsing is atomic for the pasted tree. One illegal branch invalidates the whole input, so
     * no partial set of otherwise valid lines may reach filtering or later persistence.
     */
    @Test
    fun `one invalid variation rejects the complete pasted tree`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            parseFenPositionContinuationText(
                text = "1. e4 e5 (1... Qa5) 2. Nf3",
                startFen = InitialFen,
                errorStrings = ErrorStrings,
                noValidLinesMessage = NoValidLinesMessage,
            )
        }

        assertTrue(error.message?.contains("Qa5") == true)
    }

    /*
     * Debounced work may finish after a newer edit has replaced its source text. The state holder
     * must ignore that stale completion, and dismissing the current result dialog must retain the
     * processed lines that are still shown on the screen.
     */
    @Test
    fun `text state ignores stale completion and retains accepted result after dialog dismissal`() {
        val state = AddFenPositionContinuationsTextState()
        val staleResult = processingResult(listOf("e2e4"))
        val currentResult = processingResult(listOf("d2d4"))

        state.updateText("1. e4")
        state.updateProcessingStage(
            sourceText = "1. e4",
            stage = AddFenPositionContinuationsProcessingStage.ParsingContinuations,
        )
        state.updateText("1. d4")
        state.completeProcessing(sourceText = "1. e4", result = staleResult)

        assertNull(state.processingStage)
        assertNull(state.latestResult)
        assertNull(state.pendingResult)

        state.completeProcessing(sourceText = "1. d4", result = currentResult)
        state.dismissResult()

        assertEquals(currentResult, state.latestResult)
        assertNull(state.pendingResult)
    }

    private fun processingResult(uciLine: List<String>): FenPositionContinuationTextProcessingResult {
        return FenPositionContinuationTextProcessingResult(
            newUciLines = listOf(uciLine),
            newSanLines = listOf("preview"),
            recognizedLinesCount = 1,
            exactDuplicateLinesCount = 0,
            coveredPrefixLinesCount = 0,
            coveredByStoredLinesCount = 0,
        )
    }

    private companion object {
        const val InitialFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -"
        const val NoValidLinesMessage = "No valid continuation lines"

        val ErrorStrings = PgnParseErrorStrings(
            mainLine = "main line",
            variation = "variation %1\$d",
            whiteSide = "White",
            blackSide = "Black",
            lineParseFailed = "%1\$s in the %2\$s",
            unrecognizedNotation = "Can't play %1\$s (move %2\$d, %3\$s): unrecognized notation",
            illegalMove = "Can't play %1\$s (move %2\$d, %3\$s): illegal move",
        )
    }
}
