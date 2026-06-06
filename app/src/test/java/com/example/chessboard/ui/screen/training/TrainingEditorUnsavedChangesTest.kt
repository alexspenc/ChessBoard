package com.example.chessboard.ui.screen.training

import com.example.chessboard.ui.screen.training.common.CreateTrainingEditorState

/*
 * Unit tests for shared training editor unsaved-changes helpers.
 *
 * Keep pure dirty-state and name-normalization regression tests here. Do not
 * add Compose UI dialog tests or broader screen flow tests to this file.
 */

import com.example.chessboard.ui.screen.training.common.TrainingLineEditorItem

import com.example.chessboard.ui.screen.training.loadsave.hasUnsavedTrainingEditorChanges
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingEditorUnsavedChangesTest {

    private val defaultTrainingName = "Training"

    @Test
    fun `hasUnsavedTrainingEditorChanges returns false when name and lines are unchanged`() {
        val initialLines = listOf(
            TrainingLineEditorItem(lineId = 1L, title = "Line 1", weight = 2)
        )
        val editorState = CreateTrainingEditorState(
            trainingName = "My Training",
            editableLinesForTraining = initialLines,
        )

        val hasChanges = hasUnsavedTrainingEditorChanges(
            editorState = editorState,
            initialTrainingName = "My Training",
            initialLinesForTraining = initialLines,
            defaultName = defaultTrainingName,
        )

        assertFalse(hasChanges)
    }

    @Test
    fun `hasUnsavedTrainingEditorChanges returns true when name changes`() {
        val initialLines = listOf(
            TrainingLineEditorItem(lineId = 1L, title = "Line 1", weight = 2)
        )
        val editorState = CreateTrainingEditorState(
            trainingName = "Updated Training",
            editableLinesForTraining = initialLines,
        )

        val hasChanges = hasUnsavedTrainingEditorChanges(
            editorState = editorState,
            initialTrainingName = "Initial Training",
            initialLinesForTraining = initialLines,
            defaultName = defaultTrainingName,
        )

        assertTrue(hasChanges)
    }

    @Test
    fun `hasUnsavedTrainingEditorChanges treats blank name as default name`() {
        val initialLines = listOf(
            TrainingLineEditorItem(lineId = 1L, title = "Line 1", weight = 2)
        )
        val editorState = CreateTrainingEditorState(
            trainingName = "",
            editableLinesForTraining = initialLines,
        )

        val hasChanges = hasUnsavedTrainingEditorChanges(
            editorState = editorState,
            initialTrainingName = defaultTrainingName,
            initialLinesForTraining = initialLines,
            defaultName = defaultTrainingName,
        )

        assertFalse(hasChanges)
    }

    @Test
    fun `hasUnsavedTrainingEditorChanges returns true when line list changes`() {
        val initialLines = listOf(
            TrainingLineEditorItem(lineId = 1L, title = "Line 1", weight = 2)
        )
        val editorState = CreateTrainingEditorState(
            trainingName = "My Training",
            editableLinesForTraining = initialLines + TrainingLineEditorItem(
                lineId = 2L,
                title = "Line 2",
                weight = 1,
            ),
        )

        val hasChanges = hasUnsavedTrainingEditorChanges(
            editorState = editorState,
            initialTrainingName = "My Training",
            initialLinesForTraining = initialLines,
            defaultName = defaultTrainingName,
        )

        assertTrue(hasChanges)
    }

    @Test
    fun `hasUnsavedTrainingEditorChanges returns true when line weight changes`() {
        val initialLines = listOf(
            TrainingLineEditorItem(lineId = 1L, title = "Line 1", weight = 2)
        )
        val editorState = CreateTrainingEditorState(
            trainingName = "My Training",
            editableLinesForTraining = listOf(
                TrainingLineEditorItem(lineId = 1L, title = "Line 1", weight = 3)
            ),
        )

        val hasChanges = hasUnsavedTrainingEditorChanges(
            editorState = editorState,
            initialTrainingName = "My Training",
            initialLinesForTraining = initialLines,
            defaultName = defaultTrainingName,
        )

        assertTrue(hasChanges)
    }
}
