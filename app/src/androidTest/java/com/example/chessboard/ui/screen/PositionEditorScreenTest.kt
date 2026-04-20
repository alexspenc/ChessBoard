package com.example.chessboard.ui.screen

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollToNode
import com.example.chessboard.MainActivity
import com.example.chessboard.boardmodel.InitialBoardFen
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.testing.fenStateDescriptionMatcher
import com.example.chessboard.ui.InteractiveChessBoardTestTag
import com.example.chessboard.ui.PositionEditorListTestTag
import com.example.chessboard.ui.PositionEditorSaveNameFieldTestTag
import com.example.chessboard.ui.PositionEditorWhiteShortCastleTestTag
import com.example.chessboard.ui.PositionEditorClearBoardTestTag
import com.example.chessboard.ui.PositionEditorInitialPositionTestTag
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PositionEditorScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val dbProvider: DatabaseProvider
        get() = DatabaseProvider.createInstance(composeRule.activity)

    @Before
    fun setUp() {
        dbProvider.clearAllData()
    }

    @Test
    fun positionEditorScreen_clearBoardButtonUpdatesVisibleFen() {
        // Home content depends on async startup work, including reading persisted profile settings.
        // On a slow emulator the card may not exist yet when the test starts, so wait for it
        // before clicking instead of assuming the first frame is already stable.
        waitForTextDisplayed("Position Editor")
        composeRule.onNodeWithText("Position Editor").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(PositionEditorListTestTag)
            .performScrollToNode(hasTestTag(PositionEditorInitialPositionTestTag))
        waitForNodeDisplayed(PositionEditorInitialPositionTestTag)
        composeRule.onNodeWithTag(PositionEditorInitialPositionTestTag).performClick()
        composeRule.waitForIdle()
        assertBoardFen(InitialBoardFen)

        composeRule.onNodeWithTag(PositionEditorListTestTag)
            .performScrollToNode(hasTestTag(PositionEditorClearBoardTestTag))
        waitForNodeDisplayed(PositionEditorClearBoardTestTag)
        composeRule.onNodeWithTag(PositionEditorClearBoardTestTag).performClick()
        composeRule.waitForIdle()
        assertBoardFen("8/8/8/8/8/8/8/8 w - - 0 1")
    }

    @Test
    fun positionEditorScreen_castlingCheckboxUpdatesVisibleFen() {
        waitForTextDisplayed("Position Editor")
        composeRule.onNodeWithText("Position Editor").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(PositionEditorListTestTag)
            .performScrollToNode(hasTestTag(PositionEditorInitialPositionTestTag))
        waitForNodeDisplayed(PositionEditorInitialPositionTestTag)
        composeRule.onNodeWithTag(PositionEditorInitialPositionTestTag).performClick()
        composeRule.waitForIdle()
        assertBoardFen(InitialBoardFen)

        composeRule.onNodeWithTag(PositionEditorListTestTag)
            .performScrollToNode(hasTestTag(PositionEditorWhiteShortCastleTestTag))
        waitForNodeDisplayed(PositionEditorWhiteShortCastleTestTag)
        composeRule.onNodeWithTag(PositionEditorWhiteShortCastleTestTag).performClick()
        composeRule.waitForIdle()
        assertBoardFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w Qkq - 0 1")
    }

    @Test
    fun positionEditorScreen_saveButtonOpensSaveDialog() {
        waitForTextDisplayed("Position Editor")
        composeRule.onNodeWithText("Position Editor").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Save").performClick()

        waitForTextDisplayed("Save Position")
        composeRule.onNodeWithTag(PositionEditorSaveNameFieldTestTag).assertIsDisplayed()
    }

    @Test
    fun positionEditorScreen_saveConfirmWithBlankNameShowsValidationError() {
        openPositionEditorSaveDialog()

        composeRule.onNodeWithText("Save").performClick()

        waitForTextDisplayed("Position name is required.")
        composeRule.onNodeWithTag(PositionEditorSaveNameFieldTestTag).assertIsDisplayed()
    }

    @Test
    fun positionEditorScreen_saveConfirmPersistsPositionAndShowsSuccessDialog() {
        openPositionEditorSaveDialog()

        composeRule.onNodeWithTag(PositionEditorSaveNameFieldTestTag)
            .performTextInput("Test Position")
        composeRule.onNodeWithText("Save").performClick()

        waitForTextDisplayed("Position Saved")
        composeRule.onNodeWithText("Save Position").assertDoesNotExist()

        val savedPositions = runBlocking {
            dbProvider.createSavedSearchPositionService().getAll()
        }
        check(savedPositions.size == 1) {
            "Expected 1 saved position, got ${savedPositions.size}"
        }
        check(savedPositions.first().name == "Test Position") {
            "Expected saved position name to be Test Position, got ${savedPositions.first().name}"
        }
    }

    @Test
    fun positionEditorScreen_saveConfirmWithDuplicateNameShowsDuplicateNameError() {
        runBlocking {
            dbProvider.createSavedSearchPositionService().create(
                name = "Duplicate Position",
                fenForSearch = InitialBoardFen,
                fenFull = InitialBoardFen
            )
        }

        openPositionEditorSaveDialog()
        composeRule.onNodeWithTag(PositionEditorSaveNameFieldTestTag)
            .performTextInput("Duplicate Position")
        composeRule.onNodeWithText("Save").performClick()

        waitForTextDisplayed("Position name already exists.")
        composeRule.onNodeWithTag(PositionEditorSaveNameFieldTestTag).assertIsDisplayed()
    }

    @Test
    fun positionEditorScreen_saveConfirmWithDuplicatePositionShowsDuplicatePositionError() {
        runBlocking {
            dbProvider.createSavedSearchPositionService().create(
                name = "Existing Position",
                fenForSearch = InitialBoardFen,
                fenFull = InitialBoardFen
            )
        }

        openPositionEditorSaveDialog()
        composeRule.onNodeWithTag(PositionEditorSaveNameFieldTestTag)
            .performTextInput("New Position Name")
        composeRule.onNodeWithText("Save").performClick()

        waitForTextDisplayed("This search position has already been saved.")
        composeRule.onNodeWithTag(PositionEditorSaveNameFieldTestTag).assertIsDisplayed()
    }


    private fun waitForTextDisplayed(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText(text).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    private fun waitForNodeDisplayed(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag(tag).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    private fun assertBoardFen(expectedFen: String) {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(InteractiveChessBoardTestTag).assert(
            fenStateDescriptionMatcher(expectedFen)
        )
    }

    private fun openPositionEditorSaveDialog() {
        waitForTextDisplayed("Position Editor")
        composeRule.onNodeWithText("Position Editor").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Save").performClick()

        waitForTextDisplayed("Save Position")
        composeRule.onNodeWithTag(PositionEditorSaveNameFieldTestTag).assertIsDisplayed()
    }
}
