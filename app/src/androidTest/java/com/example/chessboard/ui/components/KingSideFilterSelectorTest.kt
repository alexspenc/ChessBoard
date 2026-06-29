package com.example.chessboard.ui.components

/*
 * UI tests for the reusable king-icon side filter selector.
 *
 * Keep tests here focused on option rendering, selection semantics, and click callbacks.
 * Do not add screen-specific filter behavior or database-backed scenarios here.
 * Validation date: 2026-06-28
 */
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Rule
import org.junit.Test

class KingSideFilterSelectorTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun kingSideFilterSelector_rendersOptionsAndUpdatesSelection() {
        composeRule.setContent {
            ChessBoardTheme {
                var selectedSide by remember { mutableStateOf(TestSide.ANY) }

                KingSideFilterSelector(
                    options =
                        listOf(
                            KingSideFilterOption(
                                value = TestSide.ANY,
                                label = "Any",
                                mode = KingSideFilterMode.ANY,
                                testTag = AnySideTestTag,
                            ),
                            KingSideFilterOption(
                                value = TestSide.WHITE,
                                label = "White",
                                mode = KingSideFilterMode.WHITE,
                                testTag = WhiteSideTestTag,
                            ),
                            KingSideFilterOption(
                                value = TestSide.BLACK,
                                label = "Black",
                                mode = KingSideFilterMode.BLACK,
                                testTag = BlackSideTestTag,
                            ),
                        ),
                    selectedValue = selectedSide,
                    onValueSelected = { selectedSide = it },
                )
                androidx.compose.material3.Text("Selected: ${selectedSide.name}")
            }
        }

        composeRule.onNodeWithText("Any").assertTextEquals("Any")
        composeRule.onNodeWithText("White").assertTextEquals("White")
        composeRule.onNodeWithText("Black").assertTextEquals("Black")
        composeRule.onNodeWithTag(AnySideTestTag).assertIsSelected()

        composeRule.onNodeWithTag(BlackSideTestTag).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(BlackSideTestTag).assertIsSelected()
        composeRule.onNodeWithText("Selected: BLACK").assertTextEquals("Selected: BLACK")
    }

    private enum class TestSide {
        ANY,
        WHITE,
        BLACK,
    }

    private companion object {
        const val AnySideTestTag = "king-side-filter-any"
        const val WhiteSideTestTag = "king-side-filter-white"
        const val BlackSideTestTag = "king-side-filter-black"
    }
}
