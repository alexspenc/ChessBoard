package com.example.chessboard.runtimecontext

/*
 * File role: verifies normalized PGN export for imported game-opening analysis games.
 * Allowed here:
 * - tests for ImportedGameItem to PGN serialization
 * - round-trip checks through the existing game-opening analysis PGN parser
 * Not allowed here:
 * - Compose rendering, Android document picker tests, database access, or file I/O tests
 * Validation date: 2026-06-30
 */

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameOpeningAnalysisExportTest {
    @Test
    fun `buildGameOpeningAnalysisGamesPgn exports one game with normalized headers and SAN movetext`() {
        val game =
            importedGame(
                id = 1,
                sourceIndex = 0,
                headers =
                    mapOf(
                        "Event" to "Training block",
                        "White" to "Alice",
                        "Black" to "Bob",
                        "Result" to "1-0",
                        "ECO" to "C50",
                    ),
                moves = listOf("e2e4", "e7e5", "g1f3", "b8c6"),
            )

        val pgn = buildGameOpeningAnalysisGamesPgn(listOf(game))

        assertEquals(
            """
            [Event "Training block"]
            [Site "?"]
            [Date "?"]
            [Round "?"]
            [White "Alice"]
            [Black "Bob"]
            [Result "1-0"]
            [ECO "C50"]

            1. e4 e5 2. Nf3 Nc6 1-0
            """.trimIndent(),
            pgn,
        )
    }

    @Test
    fun `buildGameOpeningAnalysisGamesPgn preserves game order and separates records`() {
        val firstGame =
            importedGame(
                id = 1,
                sourceIndex = 0,
                headers = mapOf("Event" to "First", "Result" to "*"),
                moves = listOf("e2e4", "e7e5"),
            )
        val secondGame =
            importedGame(
                id = 2,
                sourceIndex = 1,
                headers = mapOf("Event" to "Second", "Result" to "0-1"),
                moves = listOf("d2d4", "g8f6"),
            )

        val pgn = buildGameOpeningAnalysisGamesPgn(listOf(firstGame, secondGame))

        assertEquals(
            """
            [Event "First"]
            [Site "?"]
            [Date "?"]
            [Round "?"]
            [White "?"]
            [Black "?"]
            [Result "*"]

            1. e4 e5 *

            [Event "Second"]
            [Site "?"]
            [Date "?"]
            [Round "?"]
            [White "?"]
            [Black "?"]
            [Result "0-1"]

            1. d4 Nf6 0-1
            """.trimIndent(),
            pgn,
        )
    }

    @Test
    fun `buildGameOpeningAnalysisGamesPgn round trips exported games through import parser`() {
        val games =
            listOf(
                importedGame(
                    id = 1,
                    sourceIndex = 0,
                    headers = mapOf("Event" to "First", "White" to "Alice", "Black" to "Bob", "Result" to "*"),
                    moves = listOf("e2e4", "e7e5", "g1f3", "b8c6"),
                ),
                importedGame(
                    id = 2,
                    sourceIndex = 1,
                    headers = mapOf("Event" to "Second", "White" to "Carol", "Black" to "Dave", "Result" to "1/2-1/2"),
                    moves = listOf("d2d4", "d7d5", "c2c4", "e7e6"),
                ),
            )

        val candidates = parseGameOpeningAnalysisPgnCandidates(buildGameOpeningAnalysisGamesPgn(games))

        val parsedGames =
            candidates.map { candidate ->
                val parsed = candidate as ImportedGameCandidate.Parsed
                parsed.game
            }
        assertEquals(listOf("First", "Second"), parsedGames.map { game -> game.headers["Event"] })
        assertEquals(listOf(0, 1), parsedGames.map { game -> game.sourceIndex })
        assertEquals(games.map { game -> game.mainLineMoves }, parsedGames.map { game -> game.mainLineMoves })
        assertEquals(listOf("*", "1/2-1/2"), parsedGames.map { game -> game.headers["Result"] })
    }

    @Test
    fun `buildGameOpeningAnalysisGamesPgn exports SAN disambiguation promotion and check`() {
        val disambiguationGame =
            importedGame(
                id = 1,
                sourceIndex = 0,
                headers = mapOf("Event" to "Disambiguation", "Result" to "*"),
                moves = listOf("d2d4", "d7d5", "g1f3", "c8g4", "b1d2"),
            )
        val promotionGame =
            importedGame(
                id = 2,
                sourceIndex = 1,
                headers = mapOf("Event" to "Promotion", "Result" to "*"),
                moves = listOf("a2a4", "h7h5", "a4a5", "h5h4", "a5a6", "h4h3", "a6b7", "h3g2", "b7a8q"),
            )
        val checkGame =
            importedGame(
                id = 3,
                sourceIndex = 2,
                headers = mapOf("Event" to "Checkmate", "Result" to "*"),
                moves = listOf("f2f4", "e7e5", "g2g4", "d8h4"),
            )

        val pgn = buildGameOpeningAnalysisGamesPgn(listOf(disambiguationGame, promotionGame, checkGame))

        assertTrue(pgn.contains("3. Nbd2"))
        assertTrue(pgn.contains("5. bxa8=Q"))
        assertTrue(pgn.contains("2. g4 Qh4#"))
        val candidates = parseGameOpeningAnalysisPgnCandidates(pgn)
        val reparsedMoves =
            candidates.map { candidate ->
                val parsed = candidate as ImportedGameCandidate.Parsed
                parsed.game.mainLineMoves
            }
        assertEquals(
            listOf(disambiguationGame.mainLineMoves, promotionGame.mainLineMoves, checkGame.mainLineMoves),
            reparsedMoves,
        )
    }

    @Test
    fun `buildGameOpeningAnalysisGamesPgn returns empty text for empty games`() {
        assertEquals("", buildGameOpeningAnalysisGamesPgn(emptyList()))
    }

    @Test
    fun `buildGameOpeningAnalysisGamesPgn normalizes missing invalid and quoted header values`() {
        val game =
            importedGame(
                id = 1,
                sourceIndex = 0,
                headers = mapOf("Event" to "Alice \"training\"", "Result" to "unfinished"),
                moves = listOf("e2e4"),
            )

        val pgn = buildGameOpeningAnalysisGamesPgn(listOf(game))

        assertEquals(
            """
            [Event "Alice 'training'"]
            [Site "?"]
            [Date "?"]
            [Round "?"]
            [White "?"]
            [Black "?"]
            [Result "*"]

            1. e4 *
            """.trimIndent(),
            pgn,
        )
        val parsed = parseGameOpeningAnalysisPgnCandidates(pgn).single() as ImportedGameCandidate.Parsed
        assertEquals("Alice 'training'", parsed.game.headers["Event"])
        assertEquals(listOf("e2e4"), parsed.game.mainLineMoves)
    }

    private fun importedGame(
        id: Long,
        sourceIndex: Int,
        headers: Map<String, String>,
        moves: List<String>,
    ): ImportedGameItem {
        return ImportedGameItem(
            id = id,
            sourceIndex = sourceIndex,
            headers = headers,
            mainLineMoves = moves,
            mainLineHash = moves.hashCode(),
        )
    }
}
