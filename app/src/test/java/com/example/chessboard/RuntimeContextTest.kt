package com.example.chessboard

import com.example.chessboard.runtimecontext.RuntimeContext

/*
 * Unit tests for runtime-only training navigation helpers.
 *
 * Keep fixed-order next-training resolution tests here. Do not add UI tests,
 * database-backed behavior, or broader screen flow coverage to this file.
 */

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuntimeContextTest {

    @Test
    fun `resolveNextTrainingGameId returns next id from stored order`() {
        val runtimeContext = RuntimeContext()
        runtimeContext.trainingOrderedGameIds = listOf(10L, 20L, 30L)

        val nextGameId = runtimeContext.resolveNextTrainingGameId(10L)

        assertEquals(20L, nextGameId)
    }

    @Test
    fun `resolveNextTrainingGameId returns null for last game in stored order`() {
        val runtimeContext = RuntimeContext()
        runtimeContext.trainingOrderedGameIds = listOf(10L, 20L, 30L)

        val nextGameId = runtimeContext.resolveNextTrainingGameId(30L)

        assertNull(nextGameId)
    }

    @Test
    fun `resolveNextTrainingGameId returns null when current game is missing`() {
        val runtimeContext = RuntimeContext()
        runtimeContext.trainingOrderedGameIds = listOf(10L, 20L, 30L)

        val nextGameId = runtimeContext.resolveNextTrainingGameId(99L)

        assertNull(nextGameId)
    }

    @Test
    fun `resolveNextTrainingGameId keeps using stored order after completion marks`() {
        val runtimeContext = RuntimeContext()
        runtimeContext.trainingOrderedGameIds = listOf(10L, 20L, 30L)
        runtimeContext.orderGamesInTraining.markGameCompleted(10L)

        val nextGameId = runtimeContext.resolveNextTrainingGameId(10L)

        assertEquals(20L, nextGameId)
    }
}
