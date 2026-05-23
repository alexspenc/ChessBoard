package com.example.chessboard.runtimecontext

/**
 * File role: groups unit tests for LineCollectionRuntimeContext selection behavior.
 * Allowed here:
 * - pure selection-state tests for ordered ids, fallback selection, and session cleanup
 * - assertions about collection session isolation and next-line resolution
 * Not allowed here:
 * - training-progress tests, Compose UI checks, or persistence-backed behavior
 * - tests that depend on TrainingRuntimeContext internals
 * Validation date: 2026-05-23
 */

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LineCollectionRuntimeContextTest {

    @Test
    fun `empty session returns null selected line`() {
        val runtimeContext = LineCollectionRuntimeContext()

        assertNull(runtimeContext.resolveSelectedLineId(1L))
    }

    @Test
    fun `setOrderedLineIds stores ordered line ids`() {
        val runtimeContext = LineCollectionRuntimeContext()

        runtimeContext.setOrderedLineIds(1L, listOf(10L, 20L, 30L))

        assertEquals(listOf(10L, 20L, 30L), runtimeContext.orderedLineIds(1L))
    }

    @Test
    fun `resolveSelectedLineId returns explicitly selected line`() {
        val runtimeContext = LineCollectionRuntimeContext()
        runtimeContext.setOrderedLineIds(1L, listOf(10L, 20L, 30L))
        runtimeContext.setSelectedLineId(1L, 20L)

        assertEquals(20L, runtimeContext.resolveSelectedLineId(1L))
    }

    @Test
    fun `resolveSelectedLineId falls back to first ordered line when selected line missing`() {
        val runtimeContext = LineCollectionRuntimeContext()
        runtimeContext.setOrderedLineIds(1L, listOf(10L, 20L, 30L))
        runtimeContext.setSelectedLineId(1L, 99L)

        assertEquals(10L, runtimeContext.resolveSelectedLineId(1L))
    }

    @Test
    fun `resolveSelectedLineId falls back to first ordered line when selection reset to null`() {
        val runtimeContext = LineCollectionRuntimeContext()
        runtimeContext.setOrderedLineIds(1L, listOf(10L, 20L, 30L))
        runtimeContext.setSelectedLineId(1L, 20L)

        runtimeContext.setSelectedLineId(1L, null)

        assertEquals(10L, runtimeContext.resolveSelectedLineId(1L))
    }

    @Test
    fun `resolveNextLineId returns next ordered line or null`() {
        val runtimeContext = LineCollectionRuntimeContext()
        runtimeContext.setOrderedLineIds(1L, listOf(10L, 20L, 30L))

        assertEquals(30L, runtimeContext.resolveNextLineId(1L, 20L))
        assertNull(runtimeContext.resolveNextLineId(1L, 30L))
        assertNull(runtimeContext.resolveNextLineId(1L, 99L))
    }

    @Test
    fun `clearSession removes selection and ordered lines`() {
        val runtimeContext = LineCollectionRuntimeContext()
        runtimeContext.setOrderedLineIds(1L, listOf(10L, 20L, 30L))
        runtimeContext.setSelectedLineId(1L, 20L)

        runtimeContext.clearSession(1L)

        assertEquals(emptyList<Long>(), runtimeContext.orderedLineIds(1L))
        assertNull(runtimeContext.selectedLineId(1L))
        assertNull(runtimeContext.resolveSelectedLineId(1L))
    }
}
