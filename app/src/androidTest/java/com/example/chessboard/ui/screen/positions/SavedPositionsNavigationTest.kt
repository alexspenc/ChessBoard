package com.example.chessboard.ui.screen.positions

/**
 * Navigation coverage for the saved positions entry point.
 *
 * Keep tests here focused on opening the screen and top-level saved-position flows.
 * Do not add low-level persistence or board interaction tests here.
 */
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.chessboard.MainActivity
import com.example.chessboard.boardmodel.InitialBoardFen
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.service.SaveSavedSearchPositionResult
import com.example.chessboard.ui.SavedPositionsContentTestTag
import com.example.chessboard.ui.SavedPositionsOpenSelectedTestTag
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

    private fun savePosition(name: String): Long {
        return runBlocking {
            val result = dbProvider.createSavedSearchPositionService().create(
                name = name,
                fenForSearch = InitialBoardFen,
                fenFull = InitialBoardFen,
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
}
