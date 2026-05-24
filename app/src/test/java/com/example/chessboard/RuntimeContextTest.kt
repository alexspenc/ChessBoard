package com.example.chessboard

/**
 * File role: groups unit tests for RuntimeContext wrappers around screen/runtime helpers.
 * Allowed here:
 * - small runtime-only tests for RuntimeContext delegation to training session state
 * - assertions about next-training resolution through the public RuntimeContext API
 * - small tests for RuntimeContext nested state holders shared by screens
 * Not allowed here:
 * - UI tests, Compose rendering checks, or database-backed behavior
 * - deep unit coverage for TrainingRuntimeContext internals that belongs in its own test file
 * Validation date: 2026-04-26
 */

import com.example.chessboard.runtimecontext.RuntimeContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuntimeContextTest {

    @Test
    fun `resolveNextTrainingLineId returns next id from stored order`() {
        val runtimeContext = RuntimeContext()
        runtimeContext.trainingSession.rememberLaunch(
            trainingId = 1L,
            lineId = 10L,
            orderedLineIds = listOf(10L, 20L, 30L),
        )

        val nextLineId = runtimeContext.resolveNextTrainingLineId(
            trainingId = 1L,
            currentLineId = 10L,
        )

        assertEquals(20L, nextLineId)
    }

    @Test
    fun `resolveNextTrainingLineId returns null for last line in stored order`() {
        val runtimeContext = RuntimeContext()
        runtimeContext.trainingSession.rememberLaunch(
            trainingId = 1L,
            lineId = 10L,
            orderedLineIds = listOf(10L, 20L, 30L),
        )

        val nextLineId = runtimeContext.resolveNextTrainingLineId(
            trainingId = 1L,
            currentLineId = 30L,
        )

        assertNull(nextLineId)
    }

    @Test
    fun `resolveNextTrainingLineId returns null when current line is missing`() {
        val runtimeContext = RuntimeContext()
        runtimeContext.trainingSession.rememberLaunch(
            trainingId = 1L,
            lineId = 10L,
            orderedLineIds = listOf(10L, 20L, 30L),
        )

        val nextLineId = runtimeContext.resolveNextTrainingLineId(
            trainingId = 1L,
            currentLineId = 99L,
        )

        assertNull(nextLineId)
    }

    @Test
    fun `resolveNextTrainingLineId keeps using stored order after completion marks`() {
        val runtimeContext = RuntimeContext()
        runtimeContext.trainingSession.rememberLaunch(
            trainingId = 1L,
            lineId = 10L,
            orderedLineIds = listOf(10L, 20L, 30L),
        )
        runtimeContext.orderLinesInTraining.markLineCompleted(10L)

        val nextLineId = runtimeContext.resolveNextTrainingLineId(
            trainingId = 1L,
            currentLineId = 10L,
        )

        assertEquals(20L, nextLineId)
    }

    @Test
    fun `observable lines page removes multiple ids and keeps offset on available page`() {
        val page = RuntimeContext.ObservableLinesPage(limit = 2)
        page.setLineIds(listOf(10L, 20L, 30L, 40L, 50L))
        page.openNextPage()
        page.openNextPage()

        page.removeLineIds(listOf(40L, 50L, 99L))

        assertEquals(listOf(10L, 20L, 30L), page.state.lineIds)
        assertEquals(2, page.state.offset)
        assertEquals(listOf(30L), page.visibleLineIds())
    }
}
