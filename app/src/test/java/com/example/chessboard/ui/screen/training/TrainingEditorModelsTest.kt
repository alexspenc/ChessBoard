package com.example.chessboard.ui.screen.training

/*
 * Unit tests for shared training editor list helpers.
 *
 * Keep pure helper regression tests here. Do not add Compose UI tests or
 * database-backed screen flow tests to this file.
 */

import com.example.chessboard.ui.screen.training.common.TrainingGameEditorItem
import com.example.chessboard.ui.screen.training.common.decreaseTrainingGameWeight
import com.example.chessboard.ui.screen.training.common.increaseTrainingGameWeight
import com.example.chessboard.ui.screen.training.common.removeTrainingGame

import org.junit.Assert.assertEquals
import org.junit.Test

class TrainingEditorModelsTest {

    @Test
    fun `decreaseTrainingGameWeight reduces only matching game`() {
        val games = listOf(
            TrainingGameEditorItem(gameId = 1L, title = "Game 1", weight = 3),
            TrainingGameEditorItem(gameId = 2L, title = "Game 2", weight = 2)
        )

        val updatedGames = decreaseTrainingGameWeight(
            games = games,
            gameId = 1L
        )

        assertEquals(2, updatedGames.first { it.gameId == 1L }.weight)
        assertEquals(2, updatedGames.first { it.gameId == 2L }.weight)
    }

    @Test
    fun `decreaseTrainingGameWeight keeps minimum weight at one`() {
        val games = listOf(
            TrainingGameEditorItem(gameId = 1L, title = "Game 1", weight = 1)
        )

        val updatedGames = decreaseTrainingGameWeight(
            games = games,
            gameId = 1L
        )

        assertEquals(1, updatedGames.single().weight)
    }

    @Test
    fun `increaseTrainingGameWeight increases only matching game`() {
        val games = listOf(
            TrainingGameEditorItem(gameId = 1L, title = "Game 1", weight = 3),
            TrainingGameEditorItem(gameId = 2L, title = "Game 2", weight = 2)
        )

        val updatedGames = increaseTrainingGameWeight(
            games = games,
            gameId = 2L
        )

        assertEquals(3, updatedGames.first { it.gameId == 1L }.weight)
        assertEquals(3, updatedGames.first { it.gameId == 2L }.weight)
    }

    @Test
    fun `removeTrainingGame removes only matching game`() {
        val games = listOf(
            TrainingGameEditorItem(gameId = 1L, title = "Game 1", weight = 3),
            TrainingGameEditorItem(gameId = 2L, title = "Game 2", weight = 2)
        )

        val updatedGames = removeTrainingGame(
            games = games,
            gameId = 1L
        )

        assertEquals(listOf(2L), updatedGames.map { it.gameId })
    }

    @Test
    fun `shared helpers keep list unchanged for unknown game id`() {
        val games = listOf(
            TrainingGameEditorItem(gameId = 1L, title = "Game 1", weight = 3),
            TrainingGameEditorItem(gameId = 2L, title = "Game 2", weight = 2)
        )

        assertEquals(
            games,
            decreaseTrainingGameWeight(games = games, gameId = 99L)
        )
        assertEquals(
            games,
            increaseTrainingGameWeight(games = games, gameId = 99L)
        )
        assertEquals(
            games,
            removeTrainingGame(games = games, gameId = 99L)
        )
    }
}
