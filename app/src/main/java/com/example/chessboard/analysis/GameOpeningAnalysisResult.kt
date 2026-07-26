package com.example.chessboard.analysis

/**
 * File role: defines pure result models for analyzing one game against saved opening lines.
 * Allowed here:
 * - immutable result types returned by game-opening analysis
 * - small enums and value models that describe analysis output
 * Not allowed here:
 * - analyzer algorithms, database access, Compose UI, or screen workflow code
 * Validation date: 2026-06-25
 */

enum class OpeningMatchMode {
    /** Match by exact move sequence prefix. Transpositions do not count as the same opening path. */
    MOVE_SEQUENCE,

    /**
     * Match by normalized board position.
     * Transpositions count only when already reached before the checked move.
     */
    POSITION,
}

sealed interface GameOpeningAnalysisResult {
    /** Side whose opening choices are being analyzed. */
    val selectedSide: OpeningSide

    /** Matching strategy used for comparing the analyzed game with saved opening lines. */
    val matchMode: OpeningMatchMode
}

data class GameOpeningDeviation(
    override val selectedSide: OpeningSide,
    override val matchMode: OpeningMatchMode,

    /** Normalized FEN before the move that left the known opening book. */
    val positionFen: String,

    /** Zero-based half-move index of [playedMoveUci] in the analyzed game. */
    val ply: Int,

    /** UCI move from the analyzed game that was not found among known continuations. */
    val playedMoveUci: String,

    /** Normalized FEN after applying [playedMoveUci] from [positionFen]. */
    val playedResultFen: String,

    /** Known book continuations from [positionFen], grouped by move and resulting position. */
    val expectedMoves: List<GameOpeningExpectedMove>,

    /** Book lines that reached [positionFen] and therefore define the expected continuations. */
    val matchingLineRefs: List<OpeningBookLineRef>,
) : GameOpeningAnalysisResult

data class GameOpeningOpponentLeftBook(
    override val selectedSide: OpeningSide,
    override val matchMode: OpeningMatchMode,

    /** Normalized FEN before the opponent move that left the known opening book. */
    val positionFen: String,

    /** Zero-based half-move index of [playedMoveUci] in the analyzed game. */
    val ply: Int,

    /** Opponent UCI move from the analyzed game that was not found among known continuations. */
    val playedMoveUci: String,

    /** Normalized FEN after applying [playedMoveUci] from [positionFen]. */
    val playedResultFen: String,

    /** Known book continuations for the opponent from [positionFen]. */
    val expectedMoves: List<GameOpeningExpectedMove>,

    /** Book lines that reached [positionFen] and define the expected continuations. */
    val matchingLineRefs: List<OpeningBookLineRef>,
) : GameOpeningAnalysisResult

data class GameOpeningBookTooShort(
    override val selectedSide: OpeningSide,
    override val matchMode: OpeningMatchMode,

    /** Normalized FEN of the last position that was still covered by the opening book. */
    val lastKnownPositionFen: String,

    /** Number of half-moves from the analyzed game that matched the opening book. */
    val matchedPly: Int,

    /**
     * Minimum known prefix requested by the caller.
     * Zero disables the depth requirement and preserves the legacy behavior where every ended
     * book line is reported as too short.
     */
    val minimumKnownPrefixPly: Int,

    /**
     * Next game move after [lastKnownPositionFen], when the game continues beyond known book
     * coverage.
     */
    val nextGameMoveUci: String?,

    /** Book lines that ended at [lastKnownPositionFen] and caused the coverage to stop. */
    val endedLineRefs: List<OpeningBookLineRef>,
) : GameOpeningAnalysisResult

data class GameOpeningMatchesKnownOpening(
    override val selectedSide: OpeningSide,
    override val matchMode: OpeningMatchMode,

    /**
     * Number of half-moves that matched before the game ended or before the book ended after
     * satisfying a positive minimum-known-prefix requirement.
     */
    val matchedPly: Int,

    /** Normalized FEN after the last matched game move. */
    val finalPositionFen: String,

    /** Book lines that establish the successful coverage at [finalPositionFen]. */
    val matchingLineRefs: List<OpeningBookLineRef>,
) : GameOpeningAnalysisResult

data class GameOpeningNoMatchingOpening(
    override val selectedSide: OpeningSide,
    override val matchMode: OpeningMatchMode,

    /** Normalized FEN before the first analyzed move that did not match any saved opening. */
    val positionFen: String,

    /** Zero-based half-move index of [playedMoveUci] in the analyzed game. */
    val ply: Int,

    /** First game move that prevented matching any saved opening. */
    val playedMoveUci: String,

    /** Known book continuations from [positionFen], if the position exists in the book. */
    val knownMoves: List<GameOpeningExpectedMove>,
) : GameOpeningAnalysisResult

data class GameOpeningInvalidGameMove(
    override val selectedSide: OpeningSide,
    override val matchMode: OpeningMatchMode,

    /** Normalized FEN before the invalid move was attempted. */
    val positionFen: String,

    /** Zero-based half-move index of [moveUci] in the analyzed game. */
    val ply: Int,

    /** UCI move from the analyzed game that could not be legally applied. */
    val moveUci: String,

    /** Short machine-readable reason why [moveUci] could not be applied. */
    val reason: Reason,
) : GameOpeningAnalysisResult {
    enum class Reason {
        INVALID_UCI,
        ILLEGAL_MOVE,
    }
}

data class GameOpeningInvalidInitialPosition(
    override val selectedSide: OpeningSide,
    override val matchMode: OpeningMatchMode,

    /** Initial FEN passed to the analyzer. */
    val initialFen: String,
) : GameOpeningAnalysisResult

data class GameOpeningExpectedMove(
    /** Known UCI move from the book at the analyzed position. */
    val moveUci: String,

    /** Normalized FEN after applying [moveUci] from the analyzed position. */
    val resultFen: String,

    /** Book lines that contain this continuation. */
    val lineRefs: List<OpeningBookLineRef>,
)
