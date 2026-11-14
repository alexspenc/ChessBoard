package com.example.chessboard.chesscore

/**
 * Tests valid coordinates for the chesscore Square type.
 * Does not contain the chesslib adapter or application workflows.
 * Validation date: 2026-09-08.
 */

import com.example.chessboard.chesscore.model.Square
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SquareTest {
    @Test
    fun acceptsAllBoardSquares() {
        val squares = ('a'..'h').flatMap { file -> (1..8).map { rank -> Square(file, rank) } }
        assertEquals(64, squares.toSet().size)
    }

    @Test
    fun rejectsCoordinatesOutsideBoard() {
        for (file in listOf('A', '`', 'i')) {
            assertThrows(IllegalArgumentException::class.java) { Square(file, 1) }
        }
        for (rank in listOf(0, 9)) {
            assertThrows(IllegalArgumentException::class.java) { Square('a', rank) }
        }
    }
}
