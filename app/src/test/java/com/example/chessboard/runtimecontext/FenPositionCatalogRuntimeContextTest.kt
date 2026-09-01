package com.example.chessboard.runtimecontext

/*
 * File role: verifies pagination and selection state rules for the FEN position catalog.
 * Allowed here:
 * - pure unit tests for page navigation, selection, details navigation, and offset correction
 * - catalog-data changes that affect the current page or selected position
 * Not allowed here:
 * - Room, service, Compose rendering, or navigation integration tests
 * Validation date: 2026-09-01
 */

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FenPositionCatalogRuntimeContextTest {
    @Test
    fun `initial offset is zero`() {
        val context = FenPositionCatalogRuntimeContext(pageLimit = 5)

        assertEquals(0, context.offset)
    }

    @Test
    fun `next page increases offset by page limit`() {
        val context = FenPositionCatalogRuntimeContext(pageLimit = 5)

        context.openNextPage(totalCount = 6)

        assertEquals(5, context.offset)
    }

    @Test
    fun `next page is unavailable after the last page`() {
        val context = FenPositionCatalogRuntimeContext(pageLimit = 5)
        context.openNextPage(totalCount = 10)

        assertFalse(context.canOpenNextPage(totalCount = 10))

        context.openNextPage(totalCount = 10)

        assertEquals(5, context.offset)
    }

    @Test
    fun `previous page decreases offset by page limit`() {
        val context = FenPositionCatalogRuntimeContext(pageLimit = 5)
        context.openNextPage(totalCount = 11)
        context.openNextPage(totalCount = 11)

        context.openPreviousPage()

        assertEquals(5, context.offset)
    }

    @Test
    fun `previous page is unavailable on the first page`() {
        val context = FenPositionCatalogRuntimeContext(pageLimit = 5)

        assertFalse(context.canOpenPreviousPage())

        context.openPreviousPage()

        assertEquals(0, context.offset)
    }

    @Test
    fun `first page resets offset`() {
        val context = FenPositionCatalogRuntimeContext(pageLimit = 5)
        context.openNextPage(totalCount = 6)

        context.openFirstPage()

        assertEquals(0, context.offset)
    }

    @Test
    fun `catalog growth keeps current offset`() {
        val context = FenPositionCatalogRuntimeContext(pageLimit = 5)
        context.openNextPage(totalCount = 10)

        context.ensureValidOffset(totalCount = 11)

        assertEquals(5, context.offset)
    }

    @Test
    fun `catalog shrink keeps offset when current page still exists`() {
        val context = FenPositionCatalogRuntimeContext(pageLimit = 5)
        context.openNextPage(totalCount = 11)

        context.ensureValidOffset(totalCount = 9)

        assertEquals(5, context.offset)
    }

    @Test
    fun `catalog shrink moves offset to previous page when last page disappears`() {
        val context = FenPositionCatalogRuntimeContext(pageLimit = 5)
        context.openNextPage(totalCount = 11)
        context.openNextPage(totalCount = 11)

        context.ensureValidOffset(totalCount = 10)

        assertEquals(5, context.offset)
    }

    @Test
    fun `empty catalog resets offset`() {
        val context = FenPositionCatalogRuntimeContext(pageLimit = 5)
        context.openNextPage(totalCount = 6)

        context.ensureValidOffset(totalCount = 0)

        assertEquals(0, context.offset)
    }

    @Test
    fun `page limit must be positive`() {
        listOf(0, -1).forEach { pageLimit ->
            assertThrows(IllegalArgumentException::class.java) {
                FenPositionCatalogRuntimeContext(pageLimit = pageLimit)
            }
        }
    }

    @Test
    fun `next page availability follows total count`() {
        val context = FenPositionCatalogRuntimeContext(pageLimit = 5)

        assertFalse(context.canOpenNextPage(totalCount = 5))
        assertTrue(context.canOpenNextPage(totalCount = 6))
    }

    @Test
    fun `selected position id is stored`() {
        val context = FenPositionCatalogRuntimeContext(pageLimit = 5)

        context.selectPosition(positionId = 42L)

        assertEquals(42L, context.selectedPositionId)
    }

    @Test
    fun `details navigation selects position and opens its catalog page`() {
        val context = FenPositionCatalogRuntimeContext(pageLimit = 20)

        context.showPositionAtCatalogIndex(
            positionId = 42L,
            catalogIndex = 20,
        )

        assertEquals(20, context.offset)
        assertEquals(42L, context.selectedPositionId)
    }

    @Test
    fun `details navigation keeps position on first catalog page`() {
        val context = FenPositionCatalogRuntimeContext(pageLimit = 20)

        context.showPositionAtCatalogIndex(
            positionId = 42L,
            catalogIndex = 19,
        )

        assertEquals(0, context.offset)
        assertEquals(42L, context.selectedPositionId)
    }

    @Test
    fun `details navigation rejects negative catalog index`() {
        val context = FenPositionCatalogRuntimeContext(pageLimit = 20)

        assertThrows(IllegalArgumentException::class.java) {
            context.showPositionAtCatalogIndex(
                positionId = 42L,
                catalogIndex = -1,
            )
        }
    }

    @Test
    fun `opening another page clears selected position`() {
        val context = FenPositionCatalogRuntimeContext(pageLimit = 5)
        context.selectPosition(positionId = 42L)

        context.openNextPage(totalCount = 6)

        assertNull(context.selectedPositionId)
    }

    @Test
    fun `opening previous page clears selected position`() {
        val context = FenPositionCatalogRuntimeContext(pageLimit = 5)
        context.openNextPage(totalCount = 6)
        context.selectPosition(positionId = 42L)

        context.openPreviousPage()

        assertNull(context.selectedPositionId)
    }

    @Test
    fun `opening first page from another page clears selected position`() {
        val context = FenPositionCatalogRuntimeContext(pageLimit = 5)
        context.openNextPage(totalCount = 6)
        context.selectPosition(positionId = 42L)

        context.openFirstPage()

        assertNull(context.selectedPositionId)
    }

    @Test
    fun `unavailable page transition keeps selected position`() {
        val context = FenPositionCatalogRuntimeContext(pageLimit = 5)
        context.selectPosition(positionId = 42L)

        context.openPreviousPage()

        assertEquals(42L, context.selectedPositionId)
    }

    @Test
    fun `offset correction clears selected position`() {
        val context = FenPositionCatalogRuntimeContext(pageLimit = 5)
        context.openNextPage(totalCount = 11)
        context.openNextPage(totalCount = 11)
        context.selectPosition(positionId = 42L)

        context.ensureValidOffset(totalCount = 10)

        assertNull(context.selectedPositionId)
    }

    @Test
    fun `visible selected position remains selected`() {
        val context = FenPositionCatalogRuntimeContext(pageLimit = 5)
        context.selectPosition(positionId = 42L)

        context.ensureSelectedPositionIsVisible(visiblePositionIds = listOf(41L, 42L))

        assertEquals(42L, context.selectedPositionId)
    }

    @Test
    fun `missing selected position is cleared after page reload`() {
        val context = FenPositionCatalogRuntimeContext(pageLimit = 5)
        context.selectPosition(positionId = 42L)

        context.ensureSelectedPositionIsVisible(visiblePositionIds = listOf(41L, 43L))

        assertNull(context.selectedPositionId)
    }
}
