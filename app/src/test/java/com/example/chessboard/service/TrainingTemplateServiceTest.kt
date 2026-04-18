package com.example.chessboard.service

/*
 * Unit tests for TrainingTemplateService.
 *
 * Keep service-level persistence contract tests here using fake DAOs. Do not
 * add Room integration tests or UI-driven flows to this file.
 */

import com.example.chessboard.entity.TrainingTemplateEntity
import com.example.chessboard.repository.TrainingTemplateDao
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingTemplateServiceTest {

    @Test
    fun `updateTemplateFromGames returns false when template is missing`() = runBlocking {
        val dao = FakeTrainingTemplateDao()
        val service = TrainingTemplateService(dao)

        val wasUpdated = service.updateTemplateFromGames(
            templateId = 1L,
            games = listOf(OneGameTrainingData(gameId = 10L, weight = 2)),
            name = "Updated Template",
        )

        assertFalse(wasUpdated)
    }

    @Test
    fun `updateTemplateFromGames updates template name and games json`() = runBlocking {
        val dao = FakeTrainingTemplateDao(
            templates = mutableMapOf(
                1L to TrainingTemplateEntity(
                    id = 1L,
                    name = "Initial Template",
                    gamesJson = "[]",
                )
            )
        )
        val service = TrainingTemplateService(dao)
        val updatedGames = listOf(
            OneGameTrainingData(gameId = 10L, weight = 2),
            OneGameTrainingData(gameId = 20L, weight = 1),
        )

        val wasUpdated = service.updateTemplateFromGames(
            templateId = 1L,
            games = updatedGames,
            name = "Updated Template",
        )

        assertTrue(wasUpdated)
        assertEquals("Updated Template", dao.templates.getValue(1L).name)
        assertEquals(
            OneGameTrainingData.toJson(updatedGames),
            dao.templates.getValue(1L).gamesJson,
        )
    }

    @Test
    fun `updateTemplateFromGames allows empty games list`() = runBlocking {
        val dao = FakeTrainingTemplateDao(
            templates = mutableMapOf(
                1L to TrainingTemplateEntity(
                    id = 1L,
                    name = "Initial Template",
                    gamesJson = OneGameTrainingData.toJson(
                        listOf(OneGameTrainingData(gameId = 10L, weight = 2))
                    ),
                )
            )
        )
        val service = TrainingTemplateService(dao)

        val wasUpdated = service.updateTemplateFromGames(
            templateId = 1L,
            games = emptyList(),
            name = "Empty Template",
        )

        assertTrue(wasUpdated)
        assertEquals("Empty Template", dao.templates.getValue(1L).name)
        assertEquals("[]", dao.templates.getValue(1L).gamesJson)
    }

    private class FakeTrainingTemplateDao(
        val templates: MutableMap<Long, TrainingTemplateEntity> = mutableMapOf(),
    ) : TrainingTemplateDao {
        override suspend fun insert(template: TrainingTemplateEntity): Long {
            templates[template.id] = template
            return template.id
        }

        override suspend fun getAll(): List<TrainingTemplateEntity> {
            return templates.values.toList()
        }

        override suspend fun getById(id: Long): TrainingTemplateEntity? {
            return templates[id]
        }

        override suspend fun deleteById(id: Long) {
            templates.remove(id)
        }

        override suspend fun update(template: TrainingTemplateEntity) {
            templates[template.id] = template
        }
    }
}
