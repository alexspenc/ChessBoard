package com.example.chessboard.analysis

/**
 * Tests pure building of UI-facing opening deviation items.
 * Keep Android UI rendering and navigation coverage out of this file.
 */
import com.example.chessboard.boardmodel.InitialBoardFen
import com.example.chessboard.entity.GameEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpeningDeviationItemBuilderTest {

    private val builder = OpeningDeviationItemBuilder()

    @Test
    fun `build creates one start position with two branches`() {
        val nf3Game = game(
            id = 1,
            pgn = storedPgn("e2e4", "e7e5", "g1f3"),
        )
        val bc4Game = game(
            id = 2,
            pgn = storedPgn("e2e4", "e7e5", "f1c4"),
        )

        val items = builder.build(
            games = listOf(nf3Game, bc4Game),
            selectedSide = OpeningSide.WHITE,
        )

        assertEquals(1, items.size)
        assertEquals(
            "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6",
            items.single().positionFen,
        )
        assertEquals(
            listOf(
                branch(
                    moveUci = "g1f3",
                    resultFen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq -",
                    gamesCount = 1,
                ),
                branch(
                    moveUci = "f1c4",
                    resultFen = "rnbqkbnr/pppp1ppp/8/4p3/2B1P3/8/PPPP1PPP/RNBQK1NR b KQkq -",
                    gamesCount = 1,
                ),
            ),
            items.single().branches,
        )
    }

    @Test
    fun `build creates multiple deviation start positions`() {
        val nf3Game = game(
            id = 1,
            pgn = storedPgn("e2e4", "e7e5", "g1f3"),
        )
        val bc4Game = game(
            id = 2,
            pgn = storedPgn("e2e4", "e7e5", "f1c4"),
        )
        val c4Game = game(
            id = 3,
            pgn = storedPgn("d2d4", "d7d5", "c2c4"),
        )
        val g3Game = game(
            id = 4,
            pgn = storedPgn("d2d4", "d7d5", "g2g3"),
        )

        val items = builder.build(
            games = listOf(nf3Game, bc4Game, c4Game, g3Game),
            selectedSide = OpeningSide.WHITE,
        )

        assertEquals(3, items.size)
        assertEquals(
            listOf(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -",
                "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6",
                "rnbqkbnr/ppp1pppp/8/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq d6",
            ),
            items.map { item -> item.positionFen },
        )
    }

    @Test
    fun `build returns empty list when no deviations exist`() {
        val firstGame = game(
            id = 1,
            pgn = storedPgn("e2e4", "e7e5", "g1f3"),
        )
        val secondGame = game(
            id = 2,
            pgn = storedPgn("e2e4", "e7e5", "g1f3"),
        )

        val items = builder.build(
            games = listOf(firstGame, secondGame),
            selectedSide = OpeningSide.WHITE,
        )

        assertTrue(items.isEmpty())
    }

    @Test
    fun `build merges repeated final positions into one branch with games count`() {
        val firstNf3Game = game(
            id = 1,
            pgn = storedPgn("e2e4", "e7e5", "g1f3"),
        )
        val secondNf3Game = game(
            id = 2,
            pgn = storedPgn("e2e4", "e7e5", "g1f3"),
        )
        val bc4Game = game(
            id = 3,
            pgn = storedPgn("e2e4", "e7e5", "f1c4"),
        )

        val items = builder.build(
            games = listOf(firstNf3Game, secondNf3Game, bc4Game),
            selectedSide = OpeningSide.WHITE,
        )

        assertEquals(1, items.size)
        assertEquals(2, items.single().branches.size)
        assertEquals(
            listOf(2, 1),
            items.single().branches.map { branch -> branch.gamesCount },
        )
        assertEquals(
            listOf("g1f3", "f1c4"),
            items.single().branches.map { branch -> branch.moveUci },
        )
    }

    private fun game(
        id: Long,
        pgn: String,
        initialFen: String = InitialBoardFen,
    ): GameEntity {
        return GameEntity(
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

    private fun branch(
        moveUci: String,
        resultFen: String,
        gamesCount: Int,
    ) = com.example.chessboard.ui.screen.openingDeviation.OpeningDeviationBranch(
        moveUci = moveUci,
        resultFen = resultFen,
        gamesCount = gamesCount,
    )
}
