package com.example.chessboard.ui.screen.training

/*
 * Unit tests for shared training editor unsaved-changes helpers.
 *
 * Keep pure dirty-state and name-normalization regression tests here. Do not
 * add Compose UI dialog tests or broader screen flow tests to this file.
 */

import com.example.chessboard.ui.screen.training.common.TrainingGameEditorItem

import com.example.chessboard.ui.screen.training.loadsave.hasUnsavedTrainingEditorChanges
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingEditorUnsavedChangesTest {

    @Test
    fun `hasUnsavedTrainingEditorChanges returns false when name and games are unchanged`() {
        val initialGames = listOf(
            TrainingGameEditorItem(gameId = 1L, title = "Game 1", weight = 2)
        )
        val editorState = CreateTrainingEditorState(
            trainingName = "My Training",
            editableGamesForTraining = initialGames,
        )

        val hasChanges = hasUnsavedTrainingEditorChanges(
            editorState = editorState,
            initialTrainingName = "My Training",
            initialGamesForTraining = initialGames,
        )

        assertFalse(hasChanges)
    }

    @Test
    fun `hasUnsavedTrainingEditorChanges returns true when name changes`() {
        val initialGames = listOf(
            TrainingGameEditorItem(gameId = 1L, title = "Game 1", weight = 2)
        )
        val editorState = CreateTrainingEditorState(
            trainingName = "Updated Training",
            editableGamesForTraining = initialGames,
        )

        val hasChanges = hasUnsavedTrainingEditorChanges(
            editorState = editorState,
            initialTrainingName = "Initial Training",
            initialGamesForTraining = initialGames,
        )

        assertTrue(hasChanges)
    }

    @Test
    fun `hasUnsavedTrainingEditorChanges treats blank name as default name`() {
        val initialGames = listOf(
            TrainingGameEditorItem(gameId = 1L, title = "Game 1", weight = 2)
        )
        val editorState = CreateTrainingEditorState(
            trainingName = "",
            editableGamesForTraining = initialGames,
        )

        val hasChanges = hasUnsavedTrainingEditorChanges(
            editorState = editorState,
            initialTrainingName = DEFAULT_TRAINING_NAME,
            initialGamesForTraining = initialGames,
        )

        assertFalse(hasChanges)
    }

    @Test
    fun `hasUnsavedTrainingEditorChanges returns true when game list changes`() {
        val initialGames = listOf(
            TrainingGameEditorItem(gameId = 1L, title = "Game 1", weight = 2)
        )
        val editorState = CreateTrainingEditorState(
            trainingName = "My Training",
            editableGamesForTraining = initialGames + TrainingGameEditorItem(
                gameId = 2L,
                title = "Game 2",
                weight = 1,
            ),
        )

        val hasChanges = hasUnsavedTrainingEditorChanges(
            editorState = editorState,
            initialTrainingName = "My Training",
            initialGamesForTraining = initialGames,
        )

        assertTrue(hasChanges)
    }

    @Test
    fun `hasUnsavedTrainingEditorChanges returns true when game weight changes`() {
        val initialGames = listOf(
            TrainingGameEditorItem(gameId = 1L, title = "Game 1", weight = 2)
        )
        val editorState = CreateTrainingEditorState(
            trainingName = "My Training",
            editableGamesForTraining = listOf(
                TrainingGameEditorItem(gameId = 1L, title = "Game 1", weight = 3)
            ),
        )

        val hasChanges = hasUnsavedTrainingEditorChanges(
            editorState = editorState,
            initialTrainingName = "My Training",
            initialGamesForTraining = initialGames,
        )

        assertTrue(hasChanges)
    }
}
