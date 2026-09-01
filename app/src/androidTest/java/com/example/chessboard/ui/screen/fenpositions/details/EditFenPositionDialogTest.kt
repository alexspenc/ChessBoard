package com.example.chessboard.ui.screen.fenpositions.details

/*
 * File role: verifies the pure Compose form behavior of the FEN position edit dialog.
 * Allowed here:
 * - initial field values, input replacement, theme validation, and emitted edit requests
 * Not allowed here:
 * - Room persistence, details-screen orchestration, or app navigation
 * Validation date: 2026-09-01
 */

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.example.chessboard.ui.testtags.fenpositions.FenPositionEditConfirmTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionEditDescriptionInputTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionEditDialogTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionEditNameInputTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionEditThemeInputTestTag
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EditFenPositionDialogTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun currentValuesArePrefilled() {
        setDialog(onSave = ::recordIgnoredSave)

        composeRule.onNodeWithTag(FenPositionEditDialogTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(FenPositionEditNameInputTestTag)
            .assertTextEquals("Original name")
        composeRule.onNodeWithTag(FenPositionEditThemeInputTestTag)
            .assertTextEquals("Original theme")
        composeRule.onNodeWithTag(FenPositionEditDescriptionInputTestTag)
            .assertTextEquals("Original description")
    }

    @Test
    fun editedValuesAreEmittedInOneRequest() {
        val requests = mutableListOf<EditFenPositionRequest>()
        setDialog(onSave = requests::add)

        composeRule.onNodeWithTag(FenPositionEditNameInputTestTag)
            .performTextReplacement("Updated name")
        composeRule.onNodeWithTag(FenPositionEditThemeInputTestTag)
            .performTextReplacement("Updated theme")
        composeRule.onNodeWithTag(FenPositionEditDescriptionInputTestTag)
            .performTextReplacement("Updated description")
        composeRule.onNodeWithTag(FenPositionEditConfirmTestTag).performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    EditFenPositionRequest(
                        name = "Updated name",
                        theme = "Updated theme",
                        description = "Updated description",
                    ),
                ),
                requests,
            )
        }
    }

    @Test
    fun blankThemeDoesNotEmitRequestAndCanBeCorrected() {
        val requests = mutableListOf<EditFenPositionRequest>()
        setDialog(
            initialTheme = "",
            onSave = requests::add,
        )

        composeRule.onNodeWithTag(FenPositionEditConfirmTestTag).performClick()
        composeRule.runOnIdle {
            assertEquals(emptyList<EditFenPositionRequest>(), requests)
        }

        composeRule.onNodeWithTag(FenPositionEditThemeInputTestTag)
            .performTextInput("Strategy")
        composeRule.onNodeWithTag(FenPositionEditConfirmTestTag).performClick()
        composeRule.runOnIdle {
            assertEquals("Strategy", requests.single().theme)
        }
    }

    @Test
    fun savingDisablesSaveAction() {
        setDialog(
            isSaving = true,
            onSave = ::recordIgnoredSave,
        )

        composeRule.onNodeWithTag(FenPositionEditConfirmTestTag).assertIsNotEnabled()
    }

    private fun setDialog(
        initialTheme: String = "Original theme",
        isSaving: Boolean = false,
        onSave: (EditFenPositionRequest) -> Unit,
    ) {
        composeRule.setContent {
            ChessBoardTheme {
                EditFenPositionDialog(
                    initialName = "Original name",
                    initialTheme = initialTheme,
                    initialDescription = "Original description",
                    strings = dialogStrings(),
                    isSaving = isSaving,
                    onDismiss = ::recordIgnoredDismiss,
                    onSave = onSave,
                )
            }
        }
    }

    private fun dialogStrings(): FenPositionEditDialogStrings {
        return FenPositionEditDialogStrings(
            title = "Edit position",
            nameLabel = "Name",
            namePlaceholder = "Position name",
            themeLabel = "Theme *",
            themePlaceholder = "Strategy",
            descriptionLabel = "Description",
            descriptionPlaceholder = "Notes",
            cancel = "Cancel",
            save = "Save",
        )
    }

    private fun recordIgnoredDismiss() = Unit

    private fun recordIgnoredSave(
        @Suppress("UNUSED_PARAMETER") request: EditFenPositionRequest,
    ) = Unit
}
