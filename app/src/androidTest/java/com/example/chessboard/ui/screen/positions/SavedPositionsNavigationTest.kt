package com.example.chessboard.ui.screen.positions

/**
 * Navigation coverage for the saved positions entry point.
 *
 * Keep tests here focused on opening the screen and top-level saved-position flows.
 * Board interactions here should only verify screen wiring, not low-level board rules.
 */
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.example.chessboard.MainActivity
import com.example.chessboard.boardmodel.InitialBoardFen
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.service.SaveSavedSearchPositionResult
import com.example.chessboard.testing.fenStateDescriptionMatcher
import com.example.chessboard.ui.InteractiveChessBoardTestTag
import com.example.chessboard.ui.SavedPositionsContentTestTag
import com.example.chessboard.ui.SavedPositionsOpenSelectedTestTag
import com.example.chessboard.ui.SavedPositionsSearchActionTestTag
import com.example.chessboard.ui.SavedPositionsSearchNameFieldTestTag
import com.example.chessboard.ui.savedPositionCardTestTag
import com.example.chessboard.ui.savedPositionDeleteButtonTestTag
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
    fun savedPositionsScreen_openSelectedPositionRequiresExplicitAction() {
        val positionId = savePosition(name = "Editor Position")

        waitForTextDisplayed("Saved Positions")
        composeRule.onNodeWithText("Saved Positions").performClick()

        waitForTextDisplayed("Editor Position")
        composeRule.onNodeWithTag(savedPositionCardTestTag(positionId)).performClick()
        composeRule.onNodeWithText("Position Editor").assertDoesNotExist()

        composeRule.onNodeWithTag(SavedPositionsOpenSelectedTestTag).assertIsEnabled()
        composeRule.onNodeWithTag(SavedPositionsOpenSelectedTestTag).performClick()

        waitForTextDisplayed("Position Editor")
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
