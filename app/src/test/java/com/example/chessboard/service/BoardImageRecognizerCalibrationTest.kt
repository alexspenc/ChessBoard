package com.example.chessboard.service

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Calibration tests over the real screenshots in `test positions/`. They run on the plain
 * JVM: PNGs are loaded with ImageIO and fed into the pure [PixelGrid] pipeline — no device
 * and no Robolectric. On mismatch the failure message lists every misrecognised square
 * with its confidence, which is the feedback loop for tuning the recognizer thresholds.
 */
class BoardImageRecognizerCalibrationTest {

    @Test
    fun recognize_referenceStartBoard_reproducesStartPosition() {
        val squares = BoardImageRecognizer.recognize(
            grid = loadGrid(REFERENCE_IMAGE),
            templates = referenceTemplates()
        )

        assertBoardEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR", squares)
    }

    @Test
    fun recognize_blueThemeWithHighlights_matchesPosition() {
        // suc.png: chess.com-style blue theme, green last-move squares (a1, f1), red check
        // glow around the f7 king, coordinate labels inside the edge cells, and a piece
        // set different from the reference board's.
        val squares = BoardImageRecognizer.recognize(
            grid = loadGrid("suc.png"),
            templates = referenceTemplates()
        )

        assertBoardEquals("r1qr3b/pp2pk2/1np3pB/4P1Q1/2PP4/1P6/P3B1P1/5R1K", squares)
    }

    @Test
    fun recognize_flippedBrownTheme_matchesPosition() {
        // 8_2.png: brown theme, black at the bottom (whiteAtBottom = false), a hand cursor
        // over the board, and 800x796 pixels — not a multiple of 8, so this also covers
        // the drift-free cell slicing.
        val squares = BoardImageRecognizer.recognize(
            grid = loadGrid("8_2.png"),
            templates = referenceTemplates(),
            whiteAtBottom = false
        )

        assertBoardEquals("r6k/p3p3/7P/n2pq1Q1/2p5/2P1P3/P1B2r2/2K3RR", squares)
    }

    @Test
    fun bundledAsset_buildsFullTemplateSet_andRecognisesItself() {
        // Guards `assets/reference_board.png` — the image BoardImageTemplates builds the
        // production templates from. If it is ever replaced with an image the pipeline
        // cannot read cleanly, this fails before any on-device import does.
        val grid = loadGrid(bundledAssetImage())
        val templates = BoardImageRecognizer.buildTemplatesFromReferenceBoard(grid)

        val exemplarsPerType = templates.groupingBy { it.type }.eachCount()
        assertEquals(setOf('P', 'N', 'B', 'R', 'Q', 'K'), exemplarsPerType.keys)
        exemplarsPerType.forEach { (type, count) ->
            assertEquals("Expected 2 exemplars (one per colour) for type $type", 2, count)
        }

        assertBoardEquals(
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR",
            BoardImageRecognizer.recognize(grid, templates)
        )
    }

    @Test
    fun recognize_sucBoard_toleratesImperfectManualCrop() {
        // A hand-drawn crop frame is never pixel-perfect on the board edges. A few pixels
        // of drift pushes the bottom-rank coordinate labels (a-h) out of the edge ring and
        // into the central region, where they used to be misread as low-confidence knights
        // on the empty c1/e1 squares. The MIN_PIECE_CONFIDENCE floor keeps those empty.
        val full = loadGrid("suc.png")
        val templates = referenceTemplates()
        for (inset in listOf(1, 2, 3, 4, 6)) {
            val grid = BoardImageImportService.cropPixelGrid(
                full, inset, inset, full.width - 2 * inset, full.height - 2 * inset
            )
            val bottomRank = BoardImageRecognizer.recognize(grid, templates)
                .filter { it.row == 7 }
                .sortedBy { it.col }
            val phantom = bottomRank.filter { it.col in 0..4 && it.symbol != null }
            assertEquals(
                "inset=$inset put phantom pieces on empty a1-e1: " +
                    phantom.joinToString { "${'a' + it.col}1=${it.symbol}" },
                emptyList<Char>(),
                phantom.map { it.symbol }
            )
            // The two real bottom-rank pieces must survive as pieces (their exact type can
            // drift a little under an imperfect crop; only their presence is asserted here).
            assertEquals("inset=$inset dropped the f1 piece", true, bottomRank[5].symbol != null)
            assertEquals("inset=$inset dropped the h1 king", 'K', bottomRank[7].symbol)
        }
    }

    private fun referenceTemplates(): List<PieceTemplate> =
        BoardImageRecognizer.buildTemplatesFromReferenceBoard(loadGrid(REFERENCE_IMAGE))

    /**
     * Loads a PNG into a [PixelGrid] with javax.imageio via reflection: unit tests run on
     * the desktop JVM where ImageIO exists, but they compile against android.jar, which
     * does not have it — a direct import would not compile.
     */
    private fun loadGrid(name: String): PixelGrid = loadGrid(testImage(name))

    private fun loadGrid(file: File): PixelGrid {
        val image = Class.forName("javax.imageio.ImageIO")
            .getMethod("read", File::class.java)
            .invoke(null, file)
            ?: error("ImageIO could not decode ${file.path}")
        val width = image.javaClass.getMethod("getWidth").invoke(image) as Int
        val height = image.javaClass.getMethod("getHeight").invoke(image) as Int
        val pixels = IntArray(width * height)
        val int = Int::class.javaPrimitiveType
        image.javaClass
            .getMethod("getRGB", int, int, int, int, IntArray::class.java, int, int)
            .invoke(image, 0, 0, width, height, pixels, 0, width)
        return PixelGrid(width, height, pixels)
    }

    private fun testImage(name: String): File {
        // Unit tests run with the module directory as the working dir; the screenshots
        // live in `test positions/` at the repo root.
        val candidates = listOf(File("../test positions/$name"), File("test positions/$name"))
        return candidates.firstOrNull { it.exists() }
            ?: error("Test image not found: $name (looked in ${candidates.map { it.absolutePath }})")
    }

    private fun bundledAssetImage(): File {
        val candidates = listOf(
            File("src/main/assets/reference_board.png"),
            File("app/src/main/assets/reference_board.png")
        )
        return candidates.firstOrNull { it.exists() }
            ?: error(
                "Bundled reference board not found " +
                    "(looked in ${candidates.map { it.absolutePath }})"
            )
    }

    /** Asserts the recognised placement, printing a per-square diff on mismatch. */
    private fun assertBoardEquals(expectedPlacement: String, squares: List<RecognizedSquare>) {
        val actual = BoardImageRecognizer.buildPiecePlacement(squares)
        if (actual == expectedPlacement) return

        val expectedGrid = parsePlacement(expectedPlacement)
        val differences = squares
            .sortedWith(compareBy({ it.row }, { it.col }))
            .mapNotNull { square ->
                val expected = expectedGrid[square.row * 8 + square.col]
                if (expected == square.symbol) return@mapNotNull null
                val file = 'a' + square.col
                val rank = 8 - square.row
                "$file$rank: expected ${expected ?: "empty"}, got ${square.symbol ?: "empty"}" +
                    " (confidence %.2f)".format(square.confidence)
            }
        fail(
            "Recognition mismatch (${differences.size} squares):\n" +
                "expected: $expectedPlacement\n" +
                "actual:   $actual\n" +
                differences.joinToString("\n")
        )
    }

    /** Expands a FEN piece-placement string into a row-major 64-slot grid. */
    private fun parsePlacement(placement: String): Array<Char?> {
        val grid = arrayOfNulls<Char>(64)
        placement.split("/").forEachIndexed { row, rank ->
            var col = 0
            for (symbol in rank) {
                if (symbol.isDigit()) {
                    col += symbol.digitToInt()
                } else {
                    grid[row * 8 + col] = symbol
                    col++
                }
            }
        }
        return grid
    }

    private companion object {
        const val REFERENCE_IMAGE = "start position cropped.png"
    }
}
