package com.example.chessboard.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardImageRecognizerTest {

    // region buildPiecePlacement

    @Test
    fun buildPiecePlacement_startPosition_producesStandardFen() {
        val fen = BoardImageRecognizer.buildPiecePlacement(startPositionSquares())

        assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR", fen)
    }

    @Test
    fun buildPiecePlacement_emptyBoard_producesAllEmptyRanks() {
        val squares = (0 until 8).flatMap { row ->
            (0 until 8).map { col -> RecognizedSquare(row, col, null) }
        }

        val fen = BoardImageRecognizer.buildPiecePlacement(squares)

        assertEquals("8/8/8/8/8/8/8/8", fen)
    }

    @Test
    fun buildPiecePlacement_compactsGapsBetweenPieces() {
        val squares = mutableListOf<RecognizedSquare>()
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val symbol = when {
                    row == 0 && col == 0 -> 'R'
                    row == 0 && col == 7 -> 'k'
                    else -> null
                }
                squares.add(RecognizedSquare(row, col, symbol))
            }
        }

        val fen = BoardImageRecognizer.buildPiecePlacement(squares)

        assertEquals("R6k/8/8/8/8/8/8/8", fen)
    }

    @Test
    fun buildPiecePlacement_isOrderIndependent() {
        val fen = BoardImageRecognizer.buildPiecePlacement(startPositionSquares().shuffled())

        assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR", fen)
    }

    @Test(expected = IllegalArgumentException::class)
    fun buildPiecePlacement_wrongSquareCount_throws() {
        BoardImageRecognizer.buildPiecePlacement(listOf(RecognizedSquare(0, 0, null)))
    }

    // endregion

    // region compareMasks

    @Test
    fun compareMasks_identicalMasks_returnsZero() {
        val mask = booleanArrayOf(true, false, true, false)

        assertEquals(0.0, BoardImageRecognizer.compareMasks(mask, mask.copyOf()), 0.0)
    }

    @Test
    fun compareMasks_oppositeMasks_returnsOne() {
        val a = booleanArrayOf(true, true, false, false)
        val b = booleanArrayOf(false, false, true, true)

        assertEquals(1.0, BoardImageRecognizer.compareMasks(a, b), 0.0)
    }

    @Test
    fun compareMasks_halfDiffering_returnsHalf() {
        val a = booleanArrayOf(true, true, true, true)
        val b = booleanArrayOf(true, true, false, false)

        assertEquals(0.5, BoardImageRecognizer.compareMasks(a, b), 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun compareMasks_differentSizes_throws() {
        BoardImageRecognizer.compareMasks(booleanArrayOf(true), booleanArrayOf(true, false))
    }

    // endregion

    // region buildForegroundMask

    @Test
    fun buildForegroundMask_uniformCell_hasNoForeground() {
        val luma = IntArray(BoardImageRecognizer.MASK_SIZE * BoardImageRecognizer.MASK_SIZE) { 200 }

        val mask = BoardImageRecognizer.buildForegroundMask(luma)

        assertFalse(mask.any { it })
    }

    @Test
    fun buildForegroundMask_darkGlyphOnLightBackground_isForeground() {
        val size = BoardImageRecognizer.MASK_SIZE
        val luma = IntArray(size * size) { 220 }
        // A dark glyph pixel well away from the corners (which seed the background).
        val center = (size / 2) * size + (size / 2)
        luma[center] = 20

        val mask = BoardImageRecognizer.buildForegroundMask(luma)

        assertTrue(mask[center])
        // Corners stay background.
        assertFalse(mask[0])
        assertFalse(mask[size * size - 1])
    }

    // endregion

    // region pieceIsWhite

    @Test
    fun pieceIsWhite_blackBodyOnDarkSquare_isBlack() {
        // Blue square (~110) with a black piece body filling the centre.
        val luma = centeredBody(background = 110, bodyLuma = 30)

        assertFalse(BoardImageRecognizer.pieceIsWhite(luma))
    }

    @Test
    fun pieceIsWhite_whiteBodyOnDarkSquare_isWhite() {
        val luma = centeredBody(background = 110, bodyLuma = 245)

        assertTrue(BoardImageRecognizer.pieceIsWhite(luma))
    }

    @Test
    fun pieceIsWhite_whiteBodyOnLightSquare_isWhite() {
        // Cream square (~230): the old mean-foreground rule misread this as black
        // because the mask was outline-only. Absolute luma keeps it white.
        val luma = centeredBody(background = 230, bodyLuma = 245)

        assertTrue(BoardImageRecognizer.pieceIsWhite(luma))
    }

    @Test
    fun pieceIsWhite_blackBodyOnLightSquare_isBlack() {
        val luma = centeredBody(background = 230, bodyLuma = 30)

        assertFalse(BoardImageRecognizer.pieceIsWhite(luma))
    }

    // endregion

    /** A cell whose central region is filled with a piece body over a flat background. */
    private fun centeredBody(background: Int, bodyLuma: Int): IntArray {
        val size = BoardImageRecognizer.MASK_SIZE
        val luma = IntArray(size * size) { background }
        val low = size / 4
        val high = size - size / 4
        for (y in low until high) {
            for (x in low until high) {
                luma[y * size + x] = bodyLuma
            }
        }
        return luma
    }

    private fun startPositionSquares(): List<RecognizedSquare> {
        val blackBackRank = "rnbqkbnr"
        val whiteBackRank = "RNBQKBNR"
        val squares = mutableListOf<RecognizedSquare>()
        for (col in 0 until 8) squares.add(RecognizedSquare(0, col, blackBackRank[col]))
        for (col in 0 until 8) squares.add(RecognizedSquare(1, col, 'p'))
        for (row in 2..5) {
            for (col in 0 until 8) squares.add(RecognizedSquare(row, col, null))
        }
        for (col in 0 until 8) squares.add(RecognizedSquare(6, col, 'P'))
        for (col in 0 until 8) squares.add(RecognizedSquare(7, col, whiteBackRank[col]))
        return squares
    }
}
