package com.example.chessboard.ui.screen.createOpening

/**
 * File role: groups unit tests for create-opening save-planning helpers.
 * Allowed here:
 * - pure tests for create-opening line metadata mapping and imported line save plans
 * - assertions about naming and fallback behavior used before persistence calls
 * Not allowed here:
 * - Room integration tests or Android activity tests
 * - compose rendering tests for the create-opening screen
 * Validation date: 2026-05-05
 */

import com.example.chessboard.entity.SideMask
import com.example.chessboard.ui.screen.EditableLineSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CreateOpeningSaveTest {

    @Test
    fun `buildImportedLineSavePlans copies metadata and appends line suffixes`() {
        val importedChapter = ImportedChapter(
            headers = mapOf(
                "White" to "Kasparov",
                "Black" to "Karpov",
                "Result" to "1-0",
                "Site" to "Moscow",
                "Round" to "8",
                "ECO" to "B90",
            ),
            uciLines = listOf(
                listOf("e2e4", "c7c5", "g1f3"),
                listOf("e2e4", "c7c5", "b1c3"),
            ),
        )

        val savePlans = buildImportedLineSavePlans(
            baseName = "Sicilian Defense",
            importedChapter = importedChapter,
            ecoCode = "",
            selectedSide = EditableLineSide.AS_WHITE,
            strings = testSaveStrings(),
        )

        assertEquals(2, savePlans.size)
        assertEquals("Sicilian Defense", savePlans[0].entity.event)
        assertEquals("Sicilian Defense (Line 2)", savePlans[1].entity.event)
        assertEquals("Kasparov", savePlans[0].entity.white)
        assertEquals("Karpov", savePlans[0].entity.black)
        assertEquals("1-0", savePlans[0].entity.result)
        assertEquals("Moscow", savePlans[0].entity.site)
        assertEquals("8", savePlans[0].entity.round)
        assertEquals("B90", savePlans[0].entity.eco)
        assertEquals(SideMask.WHITE, savePlans[0].entity.sideMask)
    }

    @Test
    fun `buildImportedLineSavePlans prefers explicit eco code over imported header`() {
        val importedChapter = ImportedChapter(
            headers = mapOf("ECO" to "B90"),
            uciLines = listOf(listOf("d2d4", "d7d5")),
        )

        val savePlans = buildImportedLineSavePlans(
            baseName = "Queen's Pawn Line",
            importedChapter = importedChapter,
            ecoCode = "D00",
            selectedSide = EditableLineSide.AS_BLACK,
            strings = testSaveStrings(),
        )

        assertEquals("D00", savePlans.single().entity.eco)
        assertEquals(SideMask.BLACK, savePlans.single().entity.sideMask)
    }

    @Test
    fun `buildManualOpeningEntity normalizes blank metadata to null`() {
        val entity = buildManualOpeningEntity(
            openingName = "",
            ecoCode = "",
            generatedPgn = "1. e4 e5 *",
            selectedSide = EditableLineSide.AS_WHITE,
        )

        assertNull(entity.event)
        assertNull(entity.eco)
        assertEquals("1. e4 e5 *", entity.pgn)
        assertEquals(SideMask.WHITE, entity.sideMask)
    }

    @Test
    fun `countCreateOpeningSaveTargets returns one for manual opening`() {
        val snapshot = createSaveSnapshot(importedChapters = emptyList())

        assertEquals(1, countCreateOpeningSaveTargets(snapshot))
    }

    @Test
    fun `countCreateOpeningSaveTargets sums imported chapter lines`() {
        val snapshot = createSaveSnapshot(
            importedChapters = listOf(
                importedChapter(
                    uciLines = listOf(
                        listOf("e2e4", "e7e5"),
                        listOf("d2d4", "d7d5"),
                    ),
                ),
                importedChapter(
                    uciLines = listOf(
                        listOf("c2c4", "g8f6"),
                    ),
                ),
            )
        )

        assertEquals(3, countCreateOpeningSaveTargets(snapshot))
    }

    private fun testSaveStrings(): CreateOpeningSaveStrings {
        return CreateOpeningSaveStrings(
            defaultOpeningName = "Opening",
            defaultChapterNameFormat = "Chapter %1\$d",
            lineEventNameFormat = "%1\$s (Line %2\$d)",
            noImportedChaptersSaved = "None of the imported chapters could be saved",
            noImportedLinesSaved = "None of the imported lines could be saved",
            failedSaveOpening = "Failed to save opening",
            runtime = CreateOpeningSaveRuntimeStrings(
                saveCanceled = "Save canceled.",
                processedLines = "Processed lines: %1\$d/%2\$d",
                savedLines = "Saved lines: %1\$d",
                skippedLines = "Skipped lines: %1\$d",
            ),
        )
    }

    private fun createSaveSnapshot(
        importedChapters: List<ImportedChapter>,
    ): CreateOpeningSaveSnapshot {
        return CreateOpeningSaveSnapshot(
            openingName = "Opening",
            ecoCode = "",
            selectedSide = EditableLineSide.AS_WHITE,
            importedChapters = importedChapters,
            movesSnapshot = emptyList(),
            generatedPgn = "",
            simpleViewEnabled = false,
        )
    }

    private fun importedChapter(
        uciLines: List<List<String>>,
    ): ImportedChapter {
        return ImportedChapter(
            headers = emptyMap(),
            uciLines = uciLines,
        )
    }
}
