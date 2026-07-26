package com.example.chessboard.analysis

/**
 * File role: analyzes one game against saved opening lines.
 * Allowed here:
 * - pure game-vs-opening analysis using loaded line records and opening-book indexes
 * - move replay and result selection for game-opening analysis scenarios
 * Not allowed here:
 * - database access, Compose UI, screen navigation, or persistence workflows
 * Validation date: 2026-06-29
 */
import com.example.chessboard.boardmodel.buildChesslibMoveFromUci
import com.example.chessboard.entity.LineEntity
import com.example.chessboard.service.parsePgnMoves
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.move.Move

class GameOpeningAnalyzer(
    private val indexBuilder: OpeningBookIndexBuilder = OpeningBookIndexBuilder(),
) {
    sealed interface PreparedGameOpeningBook {
        val matchMode: OpeningMatchMode

        fun analyze(
            gameMoves: List<String>,
            gameInitialFen: String,
            selectedSide: OpeningSide,
            minimumKnownPrefixPly: Int,
        ): GameOpeningAnalysisResult
    }

    fun prepareBook(
        bookLines: List<LineEntity>,
        matchMode: OpeningMatchMode,
    ): PreparedGameOpeningBook {
        val bookLinesSnapshot = bookLines.toList()
        return when (matchMode) {
            OpeningMatchMode.MOVE_SEQUENCE -> {
                PreparedSequenceBook(
                    analyzer = this,
                    sequenceLines = buildSequenceLines(bookLinesSnapshot),
                )
            }
            OpeningMatchMode.POSITION -> {
                PreparedPositionBook(
                    analyzer = this,
                    index = indexBuilder.build(bookLinesSnapshot),
                )
            }
        }
    }

    fun analyze(
        gameMoves: List<String>,
        gameInitialFen: String,
        bookLines: List<LineEntity>,
        selectedSide: OpeningSide,
        matchMode: OpeningMatchMode,
        minimumKnownPrefixPly: Int,
    ): GameOpeningAnalysisResult {
        val preparedBook =
            prepareBook(
                bookLines = bookLines,
                matchMode = matchMode,
            )
        return preparedBook.analyze(
            gameMoves = gameMoves,
            gameInitialFen = gameInitialFen,
            selectedSide = selectedSide,
            minimumKnownPrefixPly = minimumKnownPrefixPly,
        )
    }

    private fun analyzePrepared(
        gameMoves: List<String>,
        gameInitialFen: String,
        preparedBook: PreparedGameOpeningBook,
        selectedSide: OpeningSide,
        minimumKnownPrefixPly: Int,
    ): GameOpeningAnalysisResult {
        require(minimumKnownPrefixPly >= 0) {
            "minimumKnownPrefixPly must be non-negative"
        }

        val board = buildGameInitialBoard(gameInitialFen)
        if (board == null) {
            return GameOpeningInvalidInitialPosition(
                selectedSide = selectedSide,
                matchMode = preparedBook.matchMode,
                initialFen = gameInitialFen,
            )
        }

        return when (preparedBook) {
            is PreparedSequenceBook -> {
                analyzeMoveSequence(
                    gameMoves = gameMoves,
                    board = board,
                    gameInitialFen = gameInitialFen,
                    sequenceLines = preparedBook.sequenceLines,
                    selectedSide = selectedSide,
                    matchMode = preparedBook.matchMode,
                    minimumKnownPrefixPly = minimumKnownPrefixPly,
                )
            }
            is PreparedPositionBook -> {
                analyzePosition(
                    gameMoves = gameMoves,
                    board = board,
                    index = preparedBook.index,
                    selectedSide = selectedSide,
                    matchMode = preparedBook.matchMode,
                    minimumKnownPrefixPly = minimumKnownPrefixPly,
                )
            }
        }
    }

    private data class PreparedSequenceBook(
        val analyzer: GameOpeningAnalyzer,
        val sequenceLines: List<SequenceLine>,
    ) : PreparedGameOpeningBook {
        override val matchMode: OpeningMatchMode = OpeningMatchMode.MOVE_SEQUENCE

        override fun analyze(
            gameMoves: List<String>,
            gameInitialFen: String,
            selectedSide: OpeningSide,
            minimumKnownPrefixPly: Int,
        ): GameOpeningAnalysisResult =
            analyzer.analyzePrepared(
                gameMoves = gameMoves,
                gameInitialFen = gameInitialFen,
                preparedBook = this,
                selectedSide = selectedSide,
                minimumKnownPrefixPly = minimumKnownPrefixPly,
            )
    }

    private data class PreparedPositionBook(
        val analyzer: GameOpeningAnalyzer,
        val index: OpeningBookIndex,
    ) : PreparedGameOpeningBook {
        override val matchMode: OpeningMatchMode = OpeningMatchMode.POSITION

        override fun analyze(
            gameMoves: List<String>,
            gameInitialFen: String,
            selectedSide: OpeningSide,
            minimumKnownPrefixPly: Int,
        ): GameOpeningAnalysisResult =
            analyzer.analyzePrepared(
                gameMoves = gameMoves,
                gameInitialFen = gameInitialFen,
                preparedBook = this,
                selectedSide = selectedSide,
                minimumKnownPrefixPly = minimumKnownPrefixPly,
            )
    }

    /**
     * Analyzes the game by exact move-order prefix.
     * Candidate lines are narrowed after every matched ply, so transposed lines do not remain candidates.
     */
    private fun analyzeMoveSequence(
        gameMoves: List<String>,
        board: Board,
        gameInitialFen: String,
        sequenceLines: List<SequenceLine>,
        selectedSide: OpeningSide,
        matchMode: OpeningMatchMode,
        minimumKnownPrefixPly: Int,
    ): GameOpeningAnalysisResult {
        var candidates = sequenceLines.filter { line -> line.initialPositionFen == buildPositionKey(board) }
        var matchedPly = 0
        for ((ply, rawMoveUci) in gameMoves.withIndex()) {
            val positionFen = buildPositionKey(board)
            val sideToMove = resolveSideToMove(board)
            val moveUci = rawMoveUci.lowercase()
            val moveResult = buildGameMove(
                moveUci = moveUci,
                board = board,
                positionFen = positionFen,
                ply = ply,
                selectedSide = selectedSide,
                matchMode = matchMode,
            )
            val gameMove = moveResult.move ?: return moveResult.invalidMove!!

            val nextMoveGroups = buildSequenceNextMoveGroups(candidates, ply)
            val endedLineRefs = candidates
                .filter { candidate -> candidate.steps.size == ply }
                .map { candidate -> candidate.refAt(ply) }
                .sortedLineRefs()
            val matchingGroup = nextMoveGroups[moveUci]
            val playedResultFen = buildResultFen(board, gameMove)

            if (matchingGroup != null) {
                board.doMove(gameMove)
                candidates = matchingGroup.candidates
                matchedPly = ply + 1
                continue
            }

            if (matchedPly == 0) {
                return GameOpeningNoMatchingOpening(
                    selectedSide = selectedSide,
                    matchMode = matchMode,
                    positionFen = positionFen,
                    ply = ply,
                    playedMoveUci = moveUci,
                    knownMoves = nextMoveGroups.toExpectedMoves(ply),
                )
            }

            if (nextMoveGroups.isEmpty() && endedLineRefs.isNotEmpty()) {
                return buildEndedBookCoverageResult(
                    selectedSide = selectedSide,
                    matchMode = matchMode,
                    lastKnownPositionFen = positionFen,
                    matchedPly = matchedPly,
                    minimumKnownPrefixPly = minimumKnownPrefixPly,
                    nextGameMoveUci = moveUci,
                    endedLineRefs = endedLineRefs,
                )
            }

            val expectedMoves = nextMoveGroups.toExpectedMoves(ply)
            val matchingLineRefs = expectedMoves
                .flatMap { move -> move.lineRefs }
                .sortedLineRefs()
            if (sideToMove == selectedSide) {
                return GameOpeningDeviation(
                    selectedSide = selectedSide,
                    matchMode = matchMode,
                    positionFen = positionFen,
                    ply = ply,
                    playedMoveUci = moveUci,
                    playedResultFen = playedResultFen,
                    expectedMoves = expectedMoves,
                    matchingLineRefs = matchingLineRefs,
                )
            }

            return GameOpeningOpponentLeftBook(
                selectedSide = selectedSide,
                matchMode = matchMode,
                positionFen = positionFen,
                ply = ply,
                playedMoveUci = moveUci,
                playedResultFen = playedResultFen,
                expectedMoves = expectedMoves,
                matchingLineRefs = matchingLineRefs,
            )
        }

        return GameOpeningMatchesKnownOpening(
            selectedSide = selectedSide,
            matchMode = matchMode,
            matchedPly = matchedPly,
            finalPositionFen = buildPositionKey(board),
            matchingLineRefs = buildSequenceCoverageRefs(candidates, matchedPly).ifEmpty {
                refsFromInitialPosition(gameInitialFen, sequenceLines)
            },
        )
    }

    /**
     * Analyzes the game by normalized board position.
     * Each ply is still checked step by step, but matching refs come from all book lines at that position.
     */
    private fun analyzePosition(
        gameMoves: List<String>,
        board: Board,
        index: OpeningBookIndex,
        selectedSide: OpeningSide,
        matchMode: OpeningMatchMode,
        minimumKnownPrefixPly: Int,
    ): GameOpeningAnalysisResult {
        var matchedPly = 0
        var lastKnownPositionFen = buildPositionKey(board)
        var lastEndedLineRefs = endedRefs(index.positions[lastKnownPositionFen])

        for ((ply, rawMoveUci) in gameMoves.withIndex()) {
            val positionFen = buildPositionKey(board)
            val sideToMove = resolveSideToMove(board)
            val moveUci = rawMoveUci.lowercase()
            val moveResult = buildGameMove(
                moveUci = moveUci,
                board = board,
                positionFen = positionFen,
                ply = ply,
                selectedSide = selectedSide,
                matchMode = matchMode,
            )
            val gameMove = moveResult.move ?: return moveResult.invalidMove!!
            val position = index.positions[positionFen]
            val expectedMoves = position?.nextMoves.toExpectedMoves()
            val matchingMove = position?.nextMoves?.firstOrNull { move -> move.moveUci == moveUci }
            val playedResultFen = buildResultFen(board, gameMove)

            if (matchingMove != null) {
                board.doMove(gameMove)
                matchedPly = ply + 1
                lastKnownPositionFen = playedResultFen
                lastEndedLineRefs = endedRefs(index.positions[lastKnownPositionFen])
                continue
            }

            if (matchedPly == 0) {
                return GameOpeningNoMatchingOpening(
                    selectedSide = selectedSide,
                    matchMode = matchMode,
                    positionFen = positionFen,
                    ply = ply,
                    playedMoveUci = moveUci,
                    knownMoves = expectedMoves,
                )
            }

            if (position == null || position.nextMoves.isEmpty()) {
                return buildEndedBookCoverageResult(
                    selectedSide = selectedSide,
                    matchMode = matchMode,
                    lastKnownPositionFen = lastKnownPositionFen,
                    matchedPly = matchedPly,
                    minimumKnownPrefixPly = minimumKnownPrefixPly,
                    nextGameMoveUci = moveUci,
                    endedLineRefs = lastEndedLineRefs,
                )
            }

            val matchingLineRefs = expectedMoves
                .flatMap { move -> move.lineRefs }
                .sortedLineRefs()
            if (sideToMove == selectedSide) {
                return GameOpeningDeviation(
                    selectedSide = selectedSide,
                    matchMode = matchMode,
                    positionFen = positionFen,
                    ply = ply,
                    playedMoveUci = moveUci,
                    playedResultFen = playedResultFen,
                    expectedMoves = expectedMoves,
                    matchingLineRefs = matchingLineRefs,
                )
            }

            return GameOpeningOpponentLeftBook(
                selectedSide = selectedSide,
                matchMode = matchMode,
                positionFen = positionFen,
                ply = ply,
                playedMoveUci = moveUci,
                playedResultFen = playedResultFen,
                expectedMoves = expectedMoves,
                matchingLineRefs = matchingLineRefs,
            )
        }

        return GameOpeningMatchesKnownOpening(
            selectedSide = selectedSide,
            matchMode = matchMode,
            matchedPly = matchedPly,
            finalPositionFen = buildPositionKey(board),
            matchingLineRefs = coverageRefs(index.positions[buildPositionKey(board)]),
        )
    }

    /**
     * Classifies a game that continued after every matching book line ended.
     * Zero disables the minimum-depth requirement and preserves the legacy too-short result.
     */
    private fun buildEndedBookCoverageResult(
        selectedSide: OpeningSide,
        matchMode: OpeningMatchMode,
        lastKnownPositionFen: String,
        matchedPly: Int,
        minimumKnownPrefixPly: Int,
        nextGameMoveUci: String,
        endedLineRefs: List<OpeningBookLineRef>,
    ): GameOpeningAnalysisResult {
        if (minimumKnownPrefixPly == 0 || matchedPly < minimumKnownPrefixPly) {
            return GameOpeningBookTooShort(
                selectedSide = selectedSide,
                matchMode = matchMode,
                lastKnownPositionFen = lastKnownPositionFen,
                matchedPly = matchedPly,
                minimumKnownPrefixPly = minimumKnownPrefixPly,
                nextGameMoveUci = nextGameMoveUci,
                endedLineRefs = endedLineRefs,
            )
        }

        return GameOpeningMatchesKnownOpening(
            selectedSide = selectedSide,
            matchMode = matchMode,
            matchedPly = matchedPly,
            finalPositionFen = lastKnownPositionFen,
            matchingLineRefs = endedLineRefs,
        )
    }

    /**
     * Converts a game UCI string into a legal chesslib move or a typed invalid-move result.
     * User-provided game move errors are reported as analysis results instead of exceptions.
     */
    private fun buildGameMove(
        moveUci: String,
        board: Board,
        positionFen: String,
        ply: Int,
        selectedSide: OpeningSide,
        matchMode: OpeningMatchMode,
    ): MoveBuildResult {
        if (!isValidUci(moveUci)) {
            return MoveBuildResult.invalid(
                selectedSide = selectedSide,
                matchMode = matchMode,
                positionFen = positionFen,
                ply = ply,
                moveUci = moveUci,
                reason = GameOpeningInvalidGameMove.Reason.INVALID_UCI,
            )
        }

        val move = runCatching { buildChesslibMoveFromUci(uci = moveUci, board = board) }
            .getOrNull()
        if (move == null) {
            return MoveBuildResult.invalid(
                selectedSide = selectedSide,
                matchMode = matchMode,
                positionFen = positionFen,
                ply = ply,
                moveUci = moveUci,
                reason = GameOpeningInvalidGameMove.Reason.INVALID_UCI,
            )
        }

        if (board.legalMoves().contains(move)) {
            return MoveBuildResult(move = move, invalidMove = null)
        }

        return MoveBuildResult.invalid(
            selectedSide = selectedSide,
            matchMode = matchMode,
            positionFen = positionFen,
            ply = ply,
            moveUci = moveUci,
            reason = GameOpeningInvalidGameMove.Reason.ILLEGAL_MOVE,
        )
    }

    /**
     * Creates the game starting board.
     * Blank FEN means the normal starting position; malformed explicit FEN returns null.
     */
    private fun buildGameInitialBoard(gameInitialFen: String): Board? {
        val normalizedFen = gameInitialFen.trim()
        if (normalizedFen.isBlank()) {
            return Board()
        }

        if (normalizedFen.split(Regex("\\s+")).size < 4) {
            return null
        }

        return runCatching {
            Board().also { board -> board.loadFromFen(normalizedFen) }
        }.getOrNull()
    }

    /**
     * Converts saved book lines into replayable move-sequence candidates for MOVE_SEQUENCE mode.
     */
    private fun buildSequenceLines(bookLines: List<LineEntity>): List<SequenceLine> {
        return bookLines.mapIndexed { lineIndex, line -> buildSequenceLine(lineIndex, line) }
    }

    /**
     * Replays one saved line into ordered steps with result positions for each stored move.
     */
    private fun buildSequenceLine(
        lineIndex: Int,
        line: LineEntity,
    ): SequenceLine {
        val board = OpeningDeviationReplay.buildInitialBoard(line.initialFen)
        val initialPositionFen = buildPositionKey(board)
        val steps = parsePgnMoves(line.pgn).mapIndexed { moveIndex, uciMove ->
            val move = OpeningDeviationReplay.buildMoveFromUci(
                uci = uciMove,
                board = board,
                line = line,
                moveIndex = moveIndex,
            )
            board.doMove(move)
            SequenceStep(
                moveUci = uciMove.lowercase(),
                resultFen = buildPositionKey(board),
            )
        }

        return SequenceLine(
            line = line,
            lineIndex = lineIndex,
            initialPositionFen = initialPositionFen,
            steps = steps,
        )
    }

    /**
     * Groups the next available candidate moves at [ply] while preserving first-seen book order.
     */
    private fun buildSequenceNextMoveGroups(
        candidates: List<SequenceLine>,
        ply: Int,
    ): LinkedHashMap<String, SequenceMoveGroup> {
        val groups = linkedMapOf<String, SequenceMoveGroup>()
        candidates.forEach { candidate ->
            val step = candidate.steps.getOrNull(ply) ?: return@forEach
            val group = groups.getOrPut(step.moveUci) {
                SequenceMoveGroup(
                    moveUci = step.moveUci,
                    resultFen = step.resultFen,
                    candidates = emptyList(),
                )
            }
            groups[step.moveUci] = group.copy(candidates = group.candidates + candidate)
        }
        return groups
    }

    /**
     * Returns line refs that still cover the exact matched move-sequence prefix at [ply].
     */
    private fun buildSequenceCoverageRefs(
        candidates: List<SequenceLine>,
        ply: Int,
    ): List<OpeningBookLineRef> {
        return candidates
            .filter { candidate -> candidate.steps.size >= ply }
            .map { candidate -> candidate.refAt(ply) }
            .sortedLineRefs()
    }

    /**
     * Returns sequence refs that start from the analyzed game's initial position.
     * This is used for the empty-game successful match case.
     */
    private fun refsFromInitialPosition(
        gameInitialFen: String,
        sequenceLines: List<SequenceLine>,
    ): List<OpeningBookLineRef> {
        val board = buildGameInitialBoard(gameInitialFen) ?: return emptyList()
        val initialPositionFen = buildPositionKey(board)
        return sequenceLines
            .filter { line -> line.initialPositionFen == initialPositionFen }
            .map { line -> line.refAt(0) }
            .sortedLineRefs()
    }

    /**
     * Converts MOVE_SEQUENCE grouped candidate moves into public expected-move results for [ply].
     */
    private fun Map<String, SequenceMoveGroup>.toExpectedMoves(ply: Int): List<GameOpeningExpectedMove> {
        return values.map { group ->
            GameOpeningExpectedMove(
                moveUci = group.moveUci,
                resultFen = group.resultFen,
                lineRefs = group.candidates.map { candidate -> candidate.refAt(ply) }
                    .sortedLineRefs(),
            )
        }
    }

    /**
     * Converts POSITION-mode indexed book moves into public expected-move results.
     */
    private fun List<OpeningBookMove>?.toExpectedMoves(): List<GameOpeningExpectedMove> {
        return orEmpty().map { move ->
            GameOpeningExpectedMove(
                moveUci = move.moveUci,
                resultFen = move.resultFen,
                lineRefs = move.lineRefs.sortedLineRefs(),
            )
        }
    }

    /**
     * Returns every line ref that covers a book position, including continuations and ended lines.
     */
    private fun coverageRefs(position: OpeningBookPosition?): List<OpeningBookLineRef> {
        if (position == null) {
            return emptyList()
        }

        return (position.nextMoves.flatMap { move -> move.lineRefs } + position.endedLineRefs)
            .sortedLineRefs()
    }

    /**
     * Returns only line refs that ended at a book position, used for BookTooShort results.
     */
    private fun endedRefs(position: OpeningBookPosition?): List<OpeningBookLineRef> {
        if (position == null) {
            return emptyList()
        }

        return position.endedLineRefs.sortedLineRefs()
    }

    /**
     * Sorts refs by book order and deduplicates them using the same stable-id/input-index rule as the index.
     */
    private fun List<OpeningBookLineRef>.sortedLineRefs(): List<OpeningBookLineRef> {
        val refsByKey = linkedMapOf<LineRefKey, OpeningBookLineRef>()
        sortedWith(compareBy({ ref -> ref.lineIndex }, { ref -> ref.ply })).forEach { ref ->
            refsByKey[LineRefKey.from(ref)] = ref
        }
        return refsByKey.values.toList()
    }

    /**
     * Applies [move] on a copied board and returns the normalized resulting position key.
     */
    private fun buildResultFen(board: Board, move: Move): String {
        val copy = Board()
        copy.loadFromFen(board.fen)
        copy.doMove(move)
        return buildPositionKey(copy)
    }

    /**
     * Builds the normalized FEN key used by opening-book analysis comparisons.
     */
    private fun buildPositionKey(board: Board): String {
        return OpeningDeviationReplay.buildPositionKey(board)
    }

    /**
     * Converts chesslib's current side-to-move value into this package's opening side enum.
     */
    private fun resolveSideToMove(board: Board): OpeningSide {
        if (board.sideToMove.name == OpeningSide.BLACK.name) {
            return OpeningSide.BLACK
        }

        return OpeningSide.WHITE
    }

    /**
     * Performs a lightweight UCI shape check before asking chesslib to build a move.
     */
    private fun isValidUci(moveUci: String): Boolean {
        return UciPattern.matches(moveUci)
    }

    private data class MoveBuildResult(
        val move: Move?,
        val invalidMove: GameOpeningInvalidGameMove?,
    ) {
        companion object {
            fun invalid(
                selectedSide: OpeningSide,
                matchMode: OpeningMatchMode,
                positionFen: String,
                ply: Int,
                moveUci: String,
                reason: GameOpeningInvalidGameMove.Reason,
            ): MoveBuildResult {
                return MoveBuildResult(
                    move = null,
                    invalidMove = GameOpeningInvalidGameMove(
                        selectedSide = selectedSide,
                        matchMode = matchMode,
                        positionFen = positionFen,
                        ply = ply,
                        moveUci = moveUci,
                        reason = reason,
                    ),
                )
            }
        }
    }

    private data class SequenceLine(
        val line: LineEntity,
        val lineIndex: Int,
        val initialPositionFen: String,
        val steps: List<SequenceStep>,
    ) {
        fun refAt(ply: Int): OpeningBookLineRef {
            return OpeningBookLineRef.from(
                line = line,
                lineIndex = lineIndex,
                ply = ply,
            )
        }
    }

    private data class SequenceStep(
        val moveUci: String,
        val resultFen: String,
    )

    private data class SequenceMoveGroup(
        val moveUci: String,
        val resultFen: String,
        val candidates: List<SequenceLine>,
    )

    private data class LineRefKey(
        val stableLineId: Long?,
        val inputIndex: Int?,
    ) {
        companion object {
            fun from(ref: OpeningBookLineRef): LineRefKey {
                return LineRefKey(
                    stableLineId = ref.stableLineId,
                    inputIndex = ref.inputIndex,
                )
            }
        }
    }

    private companion object {
        val UciPattern = Regex("^[a-h][1-8][a-h][1-8][qrbn]?$")
    }
}
