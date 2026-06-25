package com.example.chessboard.analysis

/**
 * File role: analyzes one game against saved opening lines.
 * Allowed here:
 * - pure game-vs-opening analysis using loaded line records and opening-book indexes
 * - move replay and result selection for game-opening analysis scenarios
 * Not allowed here:
 * - database access, Compose UI, screen navigation, or persistence workflows
 * Validation date: 2026-06-25
 */
import com.example.chessboard.entity.LineEntity

class GameOpeningAnalyzer(
    private val indexBuilder: OpeningBookIndexBuilder = OpeningBookIndexBuilder(),
) {

    fun analyze(
        gameMoves: List<String>,
        gameInitialFen: String,
        bookLines: List<LineEntity>,
        selectedSide: OpeningSide,
        matchMode: OpeningMatchMode,
        minimumKnownPrefixPly: Int,
    ): GameOpeningAnalysisResult {
        TODO("Game opening analysis implementation will be added in the next commit")
    }
}
