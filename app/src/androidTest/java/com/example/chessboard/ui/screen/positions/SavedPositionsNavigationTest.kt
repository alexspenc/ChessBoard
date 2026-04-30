package com.example.chessboard.ui.screen.positions

/**
 * Navigation coverage for the saved positions entry point.
 *
 * Keep tests here focused on opening the screen and top-level saved-position flows.
 * Board interactions here should only verify screen wiring, not low-level board rules.
 */
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.example.chessboard.MainActivity
import com.example.chessboard.boardmodel.InitialBoardFen
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.entity.SideMask
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.service.SaveSavedSearchPositionResult
import com.example.chessboard.service.uciMovesToMoves
import com.example.chessboard.testing.fenStateDescriptionMatcher
import com.example.chessboard.testing.normalizeFenForAssertion
import com.example.chessboard.ui.InteractiveChessBoardTestTag
import com.example.chessboard.ui.OpeningDeviationDisplayContentTestTag
import com.example.chessboard.ui.OpeningDeviationSelectionContentTestTag
import com.example.chessboard.ui.OpeningDeviationSelectionStartTestTag
import com.example.chessboard.ui.OpeningDeviationSourceBoardTestTag
import com.example.chessboard.ui.PositionSearchResultTemplateActionTestTag
import com.example.chessboard.ui.PositionSearchResultTrainingActionTestTag
import com.example.chessboard.ui.PositionTemplateNameConfirmTestTag
import com.example.chessboard.ui.SavedPositionsContentTestTag
import com.example.chessboard.ui.SavedPositionsDeviationDialogActionTestTag
import com.example.chessboard.ui.SavedPositionsNextPageTestTag
import com.example.chessboard.ui.SavedPositionsPreviousPageTestTag
import com.example.chessboard.ui.SavedPositionsSearchActionTestTag
import com.example.chessboard.ui.SavedPositionsSearchNameFieldTestTag
import com.example.chessboard.ui.openingDeviationBranchCardTestTag
import com.example.chessboard.ui.openingDeviationSelectionCardTestTag
import com.example.chessboard.ui.savedPositionCardTestTag
import com.example.chessboard.ui.savedPositionCreateButtonTestTag
import com.example.chessboard.ui.savedPositionDeviationButtonTestTag
import com.example.chessboard.ui.savedPositionDeleteButtonTestTag
import com.example.chessboard.ui.savedPositionOpenButtonTestTag
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SavedPositionsNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val dbProvider: DatabaseProvider
        get() = DatabaseProvider.createInstance(composeRule.activity)

    @Before
    fun setUp() {
        dbProvider.clearAllData()
    }

    @Test
    fun home_savedPositionsCardOpensSavedPositionsScreen() {
        waitForTextDisplayed("Saved Positions")

        composeRule.onNodeWithText("Saved Positions").performClick()

        waitForNodeDisplayed(SavedPositionsContentTestTag)
        composeRule.onNodeWithText("No saved positions available.").assertIsDisplayed()
    }

    @Test
    fun savedPositionsScreen_displaysPersistedPositions() {
        savePosition(name = "Italian Position")

        waitForTextDisplayed("Saved Positions")
        composeRule.onNodeWithText("Saved Positions").performClick()

        waitForTextDisplayed("Italian Position")
        composeRule.onNodeWithText("FEN: $InitialBoardFen").assertIsDisplayed()
    }

    @Test
    fun savedPositionsScreen_paginatesPositionsTwentyAtATime() {
        val firstPositionId = savePagedPositions(count = 21)

        waitForTextDisplayed("Saved Positions")
        composeRule.onNodeWithText("Saved Positions").performClick()

        waitForTextDisplayed("Positions: 21 • Page 1/2")
        waitForTextDisplayed("Paged Position 01")
        composeRule.onNodeWithText("Paged Position 21").assertDoesNotExist()
        composeRule.onNodeWithTag(SavedPositionsPreviousPageTestTag).assertIsNotEnabled()
        composeRule.onNodeWithTag(SavedPositionsNextPageTestTag).assertIsEnabled()

        composeRule.onNodeWithTag(savedPositionCardTestTag(firstPositionId)).performClick()
        composeRule.onNodeWithTag(SavedPositionsNextPageTestTag).performClick()

        waitForTextDisplayed("Positions: 21 • Page 2/2")
        waitForTextDisplayed("Paged Position 21")
        composeRule.onNodeWithText("Paged Position 01").assertDoesNotExist()
        composeRule.onNodeWithTag(SavedPositionsPreviousPageTestTag).assertIsEnabled()
        composeRule.onNodeWithTag(SavedPositionsNextPageTestTag).assertIsNotEnabled()
    }

    @Test
    fun savedPositionsScreen_clickingPositionSelectsCard() {
        val positionId = savePosition(name = "Selected Position")

        waitForTextDisplayed("Saved Positions")
        composeRule.onNodeWithText("Saved Positions").performClick()

        waitForTextDisplayed("Selected Position")
        composeRule.onNodeWithTag(savedPositionCardTestTag(positionId)).performClick()

        composeRule.onNodeWithTag(savedPositionCardTestTag(positionId)).assertIsSelected()
    }

    @Test
    fun savedPositionsScreen_selectingPositionShowsReadOnlyBoardPreview() {
        val positionId = savePosition(name = "Preview Position", fen = InitialBoardFen)

        waitForTextDisplayed("Saved Positions")
        composeRule.onNodeWithText("Saved Positions").performClick()

        waitForTextDisplayed("Preview Position")
        composeRule.onNodeWithText("Position Preview").assertDoesNotExist()
        composeRule.onNodeWithTag(savedPositionCardTestTag(positionId)).performClick()

        waitForTextDisplayed("Position Preview")
        composeRule.onNodeWithText("White to move").assertIsDisplayed()
        composeRule.onNodeWithTag(InteractiveChessBoardTestTag).assert(
            fenStateDescriptionMatcher(InitialBoardFen)
        )
    }

    @Test
    fun savedPositionsScreen_selectingBlackToMovePositionShowsBlackPreviewLabel() {
        val positionId = savePosition(name = "Black Preview", fen = BlackToMoveFen)

        waitForTextDisplayed("Saved Positions")
        composeRule.onNodeWithText("Saved Positions").performClick()

        waitForTextDisplayed("Black Preview")
        composeRule.onNodeWithTag(savedPositionCardTestTag(positionId)).performClick()

        waitForTextDisplayed("Position Preview")
        composeRule.onNodeWithText("Black to move").assertIsDisplayed()
        composeRule.onNodeWithTag(InteractiveChessBoardTestTag).assert(
            fenStateDescriptionMatcher(BlackToMoveFen)
        )
    }

    @Test
    fun savedPositionsScreen_boardPreviewDoesNotAllowMoves() {
        val positionId = savePosition(name = "Read Only Preview", fen = InitialBoardFen)

        waitForTextDisplayed("Saved Positions")
        composeRule.onNodeWithText("Saved Positions").performClick()

        waitForTextDisplayed("Read Only Preview")
        composeRule.onNodeWithTag(savedPositionCardTestTag(positionId)).performClick()

        val boardNode = composeRule.onNodeWithTag(InteractiveChessBoardTestTag)
        waitForTextDisplayed("Position Preview")
        boardNode.performTouchInput {
            val squareSize = width / 8f
            val from = squareCenter(file = 4, row = 6, squareSize = squareSize)
            val to = squareCenter(file = 4, row = 4, squareSize = squareSize)
            down(from)
            moveTo(to)
            up()
        }

        boardNode.assert(fenStateDescriptionMatcher(InitialBoardFen))
    }

    @Test
    fun savedPositionsScreen_openPositionUsesCardAction() {
        val positionId = savePosition(name = "Editor Position")

        waitForTextDisplayed("Saved Positions")
        composeRule.onNodeWithText("Saved Positions").performClick()

        waitForTextDisplayed("Editor Position")
        composeRule.onNodeWithText("Position Editor").assertDoesNotExist()

        composeRule.onNodeWithTag(savedPositionOpenButtonTestTag(positionId)).performClick()

        waitForTextDisplayed("Position Editor")
    }

    @Test
    fun savedPositionsScreen_createFromPositionCreatesTemplateFromFoundGames() {
        saveGameContainingInitialPosition()
        val positionId = savePosition(name = "Create From Position", fen = InitialBoardFen)

        waitForTextDisplayed("Saved Positions")
        composeRule.onNodeWithText("Saved Positions").performClick()

        waitForTextDisplayed("Create From Position")
        composeRule.onNodeWithTag(savedPositionCreateButtonTestTag(positionId)).performClick()

        waitForTextDisplayed("Games Found")
        composeRule.onNodeWithText(
            "saved games match this position",
            substring = true,
        ).assertIsDisplayed()
        composeRule.onNodeWithTag(PositionSearchResultTrainingActionTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(PositionSearchResultTemplateActionTestTag).performClick()
        waitForTextDisplayed("New Template")
        composeRule.onNodeWithTag(PositionTemplateNameConfirmTestTag).performClick()

        waitForTextDisplayed("Template Created")
        val templates = runBlocking {
            dbProvider.createTrainingTemplateService().getAllTemplates()
        }
        check(templates.size == 1) {
            "Expected one template after create-from-position, got ${templates.size}"
        }

        val templateGames = runBlocking {
            dbProvider.createTrainingTemplateService().getGames(templates.first().id)
        }
        check(templateGames.size == 1) {
            "Expected one template game after create-from-position, got ${templateGames.size}"
        }
    }

    @Test
    fun savedPositionsScreen_createFromPositionOpensTrainingCreationFromFoundGames() {
        saveGameContainingInitialPosition()
        val positionId = savePosition(name = "Training From Position", fen = InitialBoardFen)

        waitForTextDisplayed("Saved Positions")
        composeRule.onNodeWithText("Saved Positions").performClick()

        waitForTextDisplayed("Training From Position")
        composeRule.onNodeWithTag(savedPositionCreateButtonTestTag(positionId)).performClick()

        waitForTextDisplayed("Games Found")
        composeRule.onNodeWithTag(PositionSearchResultTrainingActionTestTag).performClick()

        waitForTextDisplayed("Create Training From Position")
        waitForTextDisplayed("Games found for position: 1")
    }

    @Test
    fun savedPositionsScreen_findDeviationsShowsNoDeviationsDialog() {
        saveGameContainingInitialPosition()
        val positionId = savePosition(name = "No Deviations Position", fen = InitialBoardFen)

        waitForTextDisplayed("Saved Positions")
        composeRule.onNodeWithText("Saved Positions").performClick()

        waitForTextDisplayed("No Deviations Position")
        composeRule.onNodeWithTag(savedPositionDeviationButtonTestTag(positionId)).performClick()

        waitForTextDisplayed("No Deviations")
        composeRule.onNodeWithText(
            "No opening deviations found for this saved position."
        ).assertIsDisplayed()
    }

    @Test
    fun savedPositionsScreen_findDeviationsOpensDeviationSelectionFlow() {
        saveGame(
            event = "Deviation Source A",
            uciMoves = listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4"),
        )
        saveGame(
            event = "Deviation Source B",
            uciMoves = listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1b5"),
        )
        val positionId = savePosition(name = "Deviation Flow Position", fen = InitialBoardFen)

        waitForTextDisplayed("Saved Positions")
        composeRule.onNodeWithText("Saved Positions").performClick()

        waitForTextDisplayed("Deviation Flow Position")
        composeRule.onNodeWithTag(savedPositionDeviationButtonTestTag(positionId)).performClick()

        waitForTextDisplayed("Opening Deviations")
        composeRule.onNodeWithText(
            "deviation positions were found",
            substring = true,
        ).assertIsDisplayed()
        composeRule.onNodeWithTag(SavedPositionsDeviationDialogActionTestTag).performClick()

        waitForTextDisplayed("Deviation Positions")
        composeRule.onNodeWithText("Positions: 1").assertIsDisplayed()
    }

    @Test
    fun savedPositionsScreen_findDeviationsSelectionStartsDisplayFlow() {
        val positionId = saveDeviationSourcePosition()

        openDeviationSelectionFromSavedPosition(positionId)

        composeRule.onNodeWithTag(openingDeviationSelectionCardTestTag(0)).performClick()
        composeRule.onNodeWithTag(OpeningDeviationSelectionStartTestTag).assertIsEnabled()
        composeRule.onNodeWithTag(OpeningDeviationSelectionStartTestTag).performClick()

        waitForNodeDisplayed(OpeningDeviationDisplayContentTestTag)
        composeRule.onNodeWithText("Opening Deviations").assertIsDisplayed()
        assertBoardFenEventually(
            boardTag = OpeningDeviationSourceBoardTestTag,
            expectedFen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 0 1",
        )
        scrollToDeviationBranchCard(0)
        composeRule.onNodeWithText("Move: f1c4").assertIsDisplayed()
    }

    @Test
    fun savedPositionsScreen_openingDeviationBackFlowReturnsToSavedPositions() {
        val positionId = saveDeviationSourcePosition()

        openDeviationDisplayFromSavedPosition(positionId)

        composeRule.onAllNodesWithContentDescription("Back")[0].performClick()
        waitForNodeDisplayed(OpeningDeviationSelectionContentTestTag)

        composeRule.onAllNodesWithContentDescription("Back")[0].performClick()
        waitForNodeDisplayed(SavedPositionsContentTestTag)
        composeRule.onNodeWithText("Deviation Flow Position").assertIsDisplayed()
    }

    @Test
    fun savedPositionsScreen_deletePositionRemovesPersistedPosition() {
        val positionId = savePosition(name = "Delete Me")

        waitForTextDisplayed("Saved Positions")
        composeRule.onNodeWithText("Saved Positions").performClick()

        waitForTextDisplayed("Delete Me")
        composeRule.onNodeWithTag(savedPositionDeleteButtonTestTag(positionId)).performClick()

        waitForTextDisplayed("Delete Position")
        composeRule.onNodeWithText("Delete").performClick()

        waitForTextDisplayed("No saved positions available.")
        composeRule.onNodeWithText("Delete Me").assertDoesNotExist()

        val savedPositions = runBlocking {
            dbProvider.createSavedSearchPositionService().getAll()
        }
        check(savedPositions.isEmpty()) {
            "Expected saved positions to be empty after delete, got ${savedPositions.size}"
        }
    }

    @Test
    fun savedPositionsScreen_searchApplyFiltersByPositionName() {
        savePosition(name = "Italian Position", fen = InitialBoardFen)
        savePosition(name = "French Structure", fen = EmptyBoardFen)

        waitForTextDisplayed("Saved Positions")
        composeRule.onNodeWithText("Saved Positions").performClick()

        waitForTextDisplayed("Italian Position")
        waitForTextDisplayed("French Structure")
        composeRule.onNodeWithTag(SavedPositionsSearchActionTestTag).performClick()
        waitForTextDisplayed("Search Positions")
        composeRule.onNodeWithTag(SavedPositionsSearchNameFieldTestTag)
            .performTextInput("ita")
        composeRule.onNodeWithText("Apply").performClick()

        waitForTextDisplayed("Italian Position")
        composeRule.onNodeWithText("French Structure").assertDoesNotExist()
    }

    @Test
    fun savedPositionsScreen_searchCancelDoesNotApplyDraftFilter() {
        savePosition(name = "Italian Position", fen = InitialBoardFen)
        savePosition(name = "French Structure", fen = EmptyBoardFen)

        waitForTextDisplayed("Saved Positions")
        composeRule.onNodeWithText("Saved Positions").performClick()

        waitForTextDisplayed("Italian Position")
        waitForTextDisplayed("French Structure")
        composeRule.onNodeWithTag(SavedPositionsSearchActionTestTag).performClick()
        waitForTextDisplayed("Search Positions")
        composeRule.onNodeWithTag(SavedPositionsSearchNameFieldTestTag)
            .performTextInput("Italian")
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithText("Italian Position").assertIsDisplayed()
        composeRule.onNodeWithText("French Structure").assertIsDisplayed()
    }

    private fun savePosition(
        name: String,
        fen: String = InitialBoardFen,
    ): Long {
        return runBlocking {
            val result = dbProvider.createSavedSearchPositionService().create(
                name = name,
                fenForSearch = fen,
                fenFull = fen,
            )
            check(result is SaveSavedSearchPositionResult.Success) {
                "Expected saved position success, got $result"
            }
            result.id
        }
    }

    private fun saveDeviationSourcePosition(): Long {
        saveGame(
            event = "Deviation Source A",
            uciMoves = listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4"),
        )
        saveGame(
            event = "Deviation Source B",
            uciMoves = listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1b5"),
        )

        return savePosition(
            name = "Deviation Flow Position",
            fen = InitialBoardFen,
        )
    }

    private fun openDeviationSelectionFromSavedPosition(positionId: Long) {
        waitForTextDisplayed("Saved Positions")
        composeRule.onNodeWithText("Saved Positions").performClick()

        waitForTextDisplayed("Deviation Flow Position")
        composeRule.onNodeWithTag(savedPositionDeviationButtonTestTag(positionId)).performClick()

        waitForTextDisplayed("Opening Deviations")
        composeRule.onNodeWithTag(SavedPositionsDeviationDialogActionTestTag).performClick()
        waitForNodeDisplayed(OpeningDeviationSelectionContentTestTag)
    }

    private fun openDeviationDisplayFromSavedPosition(positionId: Long) {
        openDeviationSelectionFromSavedPosition(positionId)
        composeRule.onNodeWithTag(openingDeviationSelectionCardTestTag(0)).performClick()
        composeRule.onNodeWithTag(OpeningDeviationSelectionStartTestTag).performClick()
        waitForNodeDisplayed(OpeningDeviationDisplayContentTestTag)
    }

    private fun saveGameContainingInitialPosition(): Long {
        return saveGame(
            event = "Saved Position Source",
            uciMoves = listOf("e2e4", "e7e5"),
        )
    }

    private fun saveGame(
        event: String,
        uciMoves: List<String>,
    ): Long {
        return runBlocking {
            val game = GameEntity(
                event = event,
                pgn = storedPgn(uciMoves),
                initialFen = "",
                sideMask = SideMask.BOTH,
            )
            val gameId = dbProvider.createGameSaver().saveGame(
                game = game,
                moves = uciMovesToMoves(uciMoves),
                sideMask = game.sideMask,
            )

            checkNotNull(gameId) {
                "Expected source game to be saved"
            }
        }
    }

    private fun storedPgn(moves: List<String>): String {
        return buildString {
            append("[Event \"Saved Position Source\"]\n")
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

    private fun savePagedPositions(count: Int): Long {
        check(count > 0) {
            "Paged positions count must be positive"
        }

        return (0 until count).map { index ->
            savePosition(
                name = "Paged Position ${(index + 1).toString().padStart(2, '0')}",
                fen = uniquePagedPositionFen(index),
            )
        }.first()
    }

    private fun uniquePagedPositionFen(index: Int): String {
        val rows = (0 until 8).map { row ->
            buildFenRankWithKing(
                targetSquareIndex = index % 64,
                row = row,
            )
        }

        return "${rows.joinToString(separator = "/")} w - - 0 1"
    }

    private fun buildFenRankWithKing(
        targetSquareIndex: Int,
        row: Int,
    ): String {
        val rankStartIndex = row * 8
        val targetFile = targetSquareIndex - rankStartIndex

        if (targetFile !in 0..7) {
            return "8"
        }

        val prefix = targetFile.takeIf { it > 0 }?.toString().orEmpty()
        val suffix = (7 - targetFile).takeIf { it > 0 }?.toString().orEmpty()
        return "${prefix}K$suffix"
    }

    private fun waitForTextDisplayed(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText(text).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    private fun waitForNodeDisplayed(testTag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag(testTag).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    private fun scrollToDeviationBranchCard(index: Int) {
        composeRule.onNodeWithTag(OpeningDeviationDisplayContentTestTag)
            .performScrollToNode(hasTestTag(openingDeviationBranchCardTestTag(index)))
        composeRule.waitForIdle()
    }

    private fun assertBoardFenEventually(boardTag: String, expectedFen: String) {
        val normalizedExpectedFen = normalizeFenForAssertion(expectedFen)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            currentBoardFen(boardTag)?.let(::normalizeFenForAssertion) == normalizedExpectedFen
        }
        composeRule.onNodeWithTag(boardTag).assert(
            fenStateDescriptionMatcher(expectedFen)
        )
    }

    private fun currentBoardFen(boardTag: String): String? {
        return runCatching {
            composeRule.onNodeWithTag(boardTag)
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.StateDescription)
        }.getOrNull()
    }

    private fun squareCenter(file: Int, row: Int, squareSize: Float): Offset {
        return Offset(
            x = file * squareSize + squareSize / 2f,
            y = row * squareSize + squareSize / 2f
        )
    }

    private companion object {
        const val EmptyBoardFen = "8/8/8/8/8/8/8/8 w - - 0 1"
        const val BlackToMoveFen = "8/8/8/8/8/8/8/8 b - - 0 1"
    }
}
