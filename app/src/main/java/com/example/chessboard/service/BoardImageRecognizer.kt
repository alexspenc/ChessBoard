package com.example.chessboard.service

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pure ARGB pixel grid — the recognizer's working type. A [Bitmap] is converted once at
 * the boundary via [toPixelGrid]; everything past that point is plain Kotlin, so threshold
 * calibration can run as JVM unit tests that load the PNGs in `test positions/` with
 * javax.imageio instead of needing a device or Robolectric.
 */
class PixelGrid(
    val width: Int,
    val height: Int,
    val pixels: IntArray
) {
    init {
        require(pixels.size == width * height) {
            "Expected ${width * height} pixels, got ${pixels.size}"
        }
    }
}

/** The only Bitmap-dependent step of the recognition pipeline. */
fun Bitmap.toPixelGrid(): PixelGrid {
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    return PixelGrid(width, height, pixels)
}

/**
 * Background-independent silhouette of one piece TYPE (not colour).
 * White and black pieces converge to the same **filled** silhouette (see
 * [BoardImageRecognizer.buildGlyphMask]), so a template stores the uppercase type letter
 * (P, N, B, R, Q, K); the piece colour is decided separately from luminance.
 * [mask] is a [BoardImageRecognizer.MASK_SIZE] square row-major grid, already
 * bounding-box-normalised by [BoardImageRecognizer.normalizeMask]. [heightFraction] is
 * the glyph's bounding-box height relative to the cell — piece height is the strongest
 * cross-piece-set type signal (pawns are the shortest piece in every set), and it is lost
 * by bbox normalisation, so it is kept as a separate matching feature.
 */
data class PieceTemplate(
    val type: Char,
    val mask: BooleanArray,
    val heightFraction: Double
) {
    /** Horizontal asymmetry of [mask] — see [BoardImageRecognizer.maskAsymmetry]. */
    val asymmetry: Double = BoardImageRecognizer.maskAsymmetry(mask)

    override fun equals(other: Any?): Boolean =
        other is PieceTemplate && other.type == type &&
            other.heightFraction == heightFraction && other.mask.contentEquals(mask)

    override fun hashCode(): Int =
        (31 * type.hashCode() + mask.contentHashCode()) * 31 + heightFraction.hashCode()
}

/**
 * Result of recognising one square, already mapped to board coordinates:
 * [row] 0 = rank 8, [col] 0 = file a. [symbol] is the FEN letter
 * (uppercase = white, lowercase = black), or null when the square is empty.
 * [confidence] is 1 − the best template distance (1.0 for empty squares); the position
 * editor can highlight low-confidence squares for manual correction.
 */
data class RecognizedSquare(
    val row: Int,
    val col: Int,
    val symbol: Char?,
    val confidence: Double = 1.0
)

/**
 * Turns a cropped 2D board screenshot into a FEN piece-placement string. No ML.
 *
 * Pipeline (pure Kotlin after [toPixelGrid]):
 * 1. [cellLuma] slices the grid into 64 cells with drift-free boundaries and area-averages
 *    each down to a [MASK_SIZE] luma grid.
 * 2. [buildForegroundMask] + [centralCoverage] decide which squares are empty (the outer
 *    ring is ignored — coordinate labels are drawn inside corner cells).
 * 3. [sampleThemeBackgrounds] reads the theme's two square colours off the empty squares.
 * 4. For occupied squares, [buildGlyphMask] builds a filled glyph silhouette and
 *    [normalizeMask] crops it to its bounding box, so matching survives different board
 *    themes, highlight squares, and piece sets rendered at different scales.
 * 5. Nearest template by [compareMasks] (1 − IoU) plus a glyph-height term gives the
 *    type; [pieceIsWhite] the colour.
 * 6. [buildPiecePlacement] folds the squares into the FEN piece-placement field.
 *
 * Orientation is a caller-supplied flag (a UI toggle); [flipOrientation] re-orients an
 * already recognised board without re-running recognition.
 */
object BoardImageRecognizer {

    const val BOARD_SIZE = 8

    /** Side length the cell luma grids and glyph masks are normalised to. */
    const val MASK_SIZE = 64

    /** Convenience wrapper for callers that still hold a [Bitmap]. */
    fun recognize(
        board: Bitmap,
        templates: List<PieceTemplate>,
        whiteAtBottom: Boolean = true,
        emptyCoverage: Double = DEFAULT_EMPTY_COVERAGE
    ): List<RecognizedSquare> =
        recognize(board.toPixelGrid(), templates, whiteAtBottom, emptyCoverage)

    /**
     * Recognises the whole board into board-coordinate [RecognizedSquare]s.
     * [whiteAtBottom] handles orientation: when false the image is read as a board
     * seen from Black, so rows and files are reversed. [templates] are the type
     * silhouettes; several exemplars may share a type (see
     * [buildTemplatesFromReferenceBoard]).
     */
    fun recognize(
        grid: PixelGrid,
        templates: List<PieceTemplate>,
        whiteAtBottom: Boolean = true,
        emptyCoverage: Double = DEFAULT_EMPTY_COVERAGE
    ): List<RecognizedSquare> {
        val lumas = Array(BOARD_SIZE * BOARD_SIZE) { index ->
            cellLuma(grid, index / BOARD_SIZE, index % BOARD_SIZE)
        }
        val rawMasks = Array(lumas.size) { buildForegroundMask(lumas[it]) }
        val (lightBg, darkBg) = sampleThemeBackgrounds(lumas, rawMasks, emptyCoverage)

        val result = ArrayList<RecognizedSquare>(lumas.size)
        for (imageRow in 0 until BOARD_SIZE) {
            for (imageCol in 0 until BOARD_SIZE) {
                val index = imageRow * BOARD_SIZE + imageCol
                val match = classifyCell(
                    luma = lumas[index],
                    rawMask = rawMasks[index],
                    templates = templates,
                    lightBg = lightBg,
                    darkBg = darkBg,
                    emptyCoverage = emptyCoverage
                )
                val boardRow = if (whiteAtBottom) imageRow else BOARD_SIZE - 1 - imageRow
                val boardCol = if (whiteAtBottom) imageCol else BOARD_SIZE - 1 - imageCol
                result.add(RecognizedSquare(boardRow, boardCol, match.symbol, match.confidence))
            }
        }
        return result
    }

    /**
     * Re-orients an already recognised board (white-at-bottom <-> black-at-bottom) without
     * re-running recognition: a 180° flip only remaps coordinates. Backs the UI orientation
     * toggle, which replaces automatic orientation detection.
     */
    fun flipOrientation(squares: List<RecognizedSquare>): List<RecognizedSquare> =
        squares.map {
            it.copy(row = BOARD_SIZE - 1 - it.row, col = BOARD_SIZE - 1 - it.col)
        }

    /**
     * Cheap sanity check of a recognised position before handing it to the editor.
     * Catches the mistakes recognition is most likely to make: wrong king count,
     * pawns on the back ranks, more than 16 pieces of one colour.
     */
    fun findPositionIssues(squares: List<RecognizedSquare>): List<String> {
        val issues = ArrayList<String>()
        val whiteKings = squares.count { it.symbol == 'K' }
        val blackKings = squares.count { it.symbol == 'k' }
        if (whiteKings != 1) issues.add("Expected 1 white king, found $whiteKings")
        if (blackKings != 1) issues.add("Expected 1 black king, found $blackKings")
        val backRankPawns = squares.count {
            (it.symbol == 'P' || it.symbol == 'p') && (it.row == 0 || it.row == BOARD_SIZE - 1)
        }
        if (backRankPawns > 0) issues.add("Found $backRankPawns pawn(s) on rank 1 or 8")
        val whitePieces = squares.count { it.symbol?.isUpperCase() == true }
        val blackPieces = squares.count { it.symbol?.isLowerCase() == true }
        if (whitePieces > 16) issues.add("Found $whitePieces white pieces (max 16)")
        if (blackPieces > 16) issues.add("Found $blackPieces black pieces (max 16)")
        return issues
    }

    /**
     * True when the square at image [row]/[col] is a light square. Pure geometry: the
     * bottom-left corner (row 7, col 0) is always dark on a correctly oriented board,
     * so colours follow the checkerboard parity — no luminance needed. Parity is the
     * same in both orientations, so image coordinates are fine.
     */
    fun isLightSquare(row: Int, col: Int): Boolean = (row + col) % 2 == 0

    /** Convenience wrapper for callers that still hold a [Bitmap]. */
    fun buildTemplatesFromReferenceBoard(referenceBoard: Bitmap): List<PieceTemplate> =
        buildTemplatesFromReferenceBoard(referenceBoard.toPixelGrid())

    /**
     * Builds type silhouettes from a clean start-position board, already cropped to the
     * board edges. Orientation-agnostic: the back rank (image row 0) is R N B Q K B N R
     * by file in either orientation, and image row 1 is pawns — and the same holds for
     * rows 7/6 with the other colour. Both colours are extracted (up to 12 templates,
     * 6 types × 2 exemplars): the filled silhouettes are close but outline thickness
     * differs, and [recognize] simply takes the nearest exemplar of any type.
     */
    fun buildTemplatesFromReferenceBoard(grid: PixelGrid): List<PieceTemplate> {
        val lumas = Array(BOARD_SIZE * BOARD_SIZE) { index ->
            cellLuma(grid, index / BOARD_SIZE, index % BOARD_SIZE)
        }
        val rawMasks = Array(lumas.size) { buildForegroundMask(lumas[it]) }
        val (lightBg, darkBg) = sampleThemeBackgrounds(lumas, rawMasks, DEFAULT_EMPTY_COVERAGE)

        val backRankTypes = charArrayOf('R', 'N', 'B', 'Q', 'K', 'B', 'N', 'R')
        val templates = ArrayList<PieceTemplate>()
        for ((backRow, pawnRow) in listOf(0 to 1, BOARD_SIZE - 1 to BOARD_SIZE - 2)) {
            val seen = HashSet<Char>()
            for (col in 0 until BOARD_SIZE) {
                val type = backRankTypes[col]
                if (!seen.add(type)) continue
                val shape = glyphShape(lumas[backRow * BOARD_SIZE + col], lightBg, darkBg)
                if (shape != null) templates.add(PieceTemplate(type, shape.mask, shape.heightFraction))
            }
            val pawn = glyphShape(lumas[pawnRow * BOARD_SIZE], lightBg, darkBg)
            if (pawn != null) templates.add(PieceTemplate('P', pawn.mask, pawn.heightFraction))
        }
        return templates
    }

    /**
     * Luma grid of one cell, downscaled to [MASK_SIZE] by area averaging. Cell boundaries
     * are rounded per index rather than stepped by `size / 8`, so boards whose pixel size
     * is not a multiple of 8 (e.g. 800×796) don't accumulate drift towards the last
     * rank/file. Assumes [grid] is already cropped to the board edges.
     */
    fun cellLuma(grid: PixelGrid, row: Int, col: Int): IntArray {
        val left = cellStart(grid.width, col)
        val right = cellStart(grid.width, col + 1)
        val top = cellStart(grid.height, row)
        val bottom = cellStart(grid.height, row + 1)

        val luma = IntArray(MASK_SIZE * MASK_SIZE)
        for (maskY in 0 until MASK_SIZE) {
            val yStart = top + (maskY * (bottom - top)) / MASK_SIZE
            val yEnd = maxOf(top + ((maskY + 1) * (bottom - top)) / MASK_SIZE, yStart + 1)
            for (maskX in 0 until MASK_SIZE) {
                val xStart = left + (maskX * (right - left)) / MASK_SIZE
                val xEnd = maxOf(left + ((maskX + 1) * (right - left)) / MASK_SIZE, xStart + 1)
                var sum = 0
                var count = 0
                for (y in yStart until yEnd) {
                    for (x in xStart until xEnd) {
                        val p = grid.pixels[y * grid.width + x]
                        val r = (p shr 16) and 0xFF
                        val g = (p shr 8) and 0xFF
                        val b = p and 0xFF
                        sum += (r * 299 + g * 587 + b * 114) / 1000
                        count++
                    }
                }
                luma[maskY * MASK_SIZE + maskX] = sum / count
            }
        }
        return luma
    }

    private fun cellStart(total: Int, index: Int): Int =
        (index * total.toFloat() / BOARD_SIZE).roundToInt()

    /**
     * Samples the theme's ink thresholds from the **empty** squares of this screenshot,
     * split by deterministic [isLightSquare] parity. Returns the BRIGHTEST light-square
     * background and the DARKEST dark-square background (each sample is already a robust
     * per-cell median from [estimateBackgroundLuma]): themes often carry a board-wide
     * gradient, and only pixels beyond the extreme backgrounds are guaranteed to be piece
     * ink rather than background. Falls back to defaults if a colour has no empty squares
     * (a nearly full board).
     */
    private fun sampleThemeBackgrounds(
        lumas: Array<IntArray>,
        masks: Array<BooleanArray>,
        emptyCoverage: Double
    ): Pair<Int, Int> {
        var lightBg: Int? = null
        var darkBg: Int? = null
        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                val index = row * BOARD_SIZE + col
                if (centralCoverage(masks[index]) >= emptyCoverage) continue
                val background = estimateBackgroundLuma(lumas[index])
                if (isLightSquare(row, col)) {
                    lightBg = maxOf(lightBg ?: background, background)
                } else {
                    darkBg = minOf(darkBg ?: background, background)
                }
            }
        }
        return (lightBg ?: DEFAULT_LIGHT_BG) to (darkBg ?: DEFAULT_DARK_BG)
    }

    private class CellMatch(val symbol: Char?, val confidence: Double)

    /** Normalised glyph mask plus its pre-normalisation height (see [PieceTemplate]). */
    private class GlyphShape(val mask: BooleanArray, val heightFraction: Double) {
        val asymmetry: Double = maskAsymmetry(mask)
    }

    /**
     * Classifies one cell. Empty is decided by [centralCoverage] of the raw mask; type by
     * the nearest template over the filled, normalised glyph mask plus a glyph-height
     * term; colour by [pieceIsWhite]. Confidence is 1 − the best template distance.
     */
    private fun classifyCell(
        luma: IntArray,
        rawMask: BooleanArray,
        templates: List<PieceTemplate>,
        lightBg: Int,
        darkBg: Int,
        emptyCoverage: Double
    ): CellMatch {
        if (centralCoverage(rawMask) < emptyCoverage) return CellMatch(null, 1.0)

        val shape = glyphShape(luma, lightBg, darkBg) ?: return CellMatch(null, 0.0)

        var bestType: Char? = null
        var bestDistance = Double.MAX_VALUE
        for (template in templates) {
            val distance = compareMasks(shape.mask, template.mask) +
                HEIGHT_WEIGHT * abs(shape.heightFraction - template.heightFraction) +
                ASYMMETRY_WEIGHT * abs(shape.asymmetry - template.asymmetry)
            if (distance < bestDistance) {
                bestDistance = distance
                bestType = template.type
            }
        }
        val type = bestType ?: return CellMatch(null, 0.0)

        // A square that crossed the emptiness threshold but matches no template well is
        // debris, not a piece — most often a coordinate label leaking out of a cell whose
        // crop is a few pixels off. Real pieces score >= 0.69 on the calibration
        // screenshots; label fragments score ~0.00 (their short glyph is killed by the
        // height term). Below the floor, call the square empty.
        val confidence = (1.0 - bestDistance).coerceIn(0.0, 1.0)
        if (confidence < MIN_PIECE_CONFIDENCE) return CellMatch(null, 1.0)

        val symbol = if (pieceIsWhite(luma, lightBg, darkBg)) {
            type.uppercaseChar()
        } else {
            type.lowercaseChar()
        }
        return CellMatch(symbol, confidence)
    }

    /** Glyph mask reduced to matching features, or null when the cell holds no glyph. */
    private fun glyphShape(luma: IntArray, lightBg: Int, darkBg: Int): GlyphShape? {
        val glyph = buildGlyphMask(luma, lightBg, darkBg)
        var minY = MASK_SIZE
        var maxY = -1
        for (index in glyph.indices) {
            if (!glyph[index]) continue
            val y = index / MASK_SIZE
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
        if (maxY < 0) return null
        val heightFraction = (maxY - minY + 1).toDouble() / MASK_SIZE
        return GlyphShape(normalizeMask(glyph), heightFraction)
    }

    /**
     * Raw foreground mask for a cell already reduced to a [MASK_SIZE] luma grid.
     * A pixel is foreground when it differs from the per-cell background luminance by
     * more than [FOREGROUND_LUMA_DELTA]. The background is estimated from the corners
     * (least likely to hold the glyph), which is what makes this robust to theme and
     * to green/red highlight squares. The outer [EDGE_MARGIN] ring is cleared — see
     * [clearEdgeRing]. Used for emptiness detection and theme sampling; classification
     * uses the richer [buildGlyphMask].
     */
    fun buildForegroundMask(luma: IntArray): BooleanArray {
        val background = estimateBackgroundLuma(luma)
        return clearEdgeRing(
            BooleanArray(luma.size) { abs(luma[it] - background) > FOREGROUND_LUMA_DELTA }
        )
    }

    /**
     * Clears the outer [EDGE_MARGIN] ring of a cell mask. Cell boundaries catch the
     * neighbouring squares' edges (a piece standing on the square above bleeds into the
     * top rows) and the screenshot's own border shadow (a dark vignette up to ~8% of the
     * cell wide on edge cells). Left in place, that frame fuses with the glyph through
     * dilation and stretches its bounding box, ruining the IoU against every template.
     */
    private fun clearEdgeRing(mask: BooleanArray): BooleanArray {
        val size = MASK_SIZE
        val out = mask.copyOf()
        for (i in 0 until size) {
            for (m in 0 until EDGE_MARGIN) {
                out[m * size + i] = false
                out[(size - 1 - m) * size + i] = false
                out[i * size + m] = false
                out[i * size + (size - 1 - m)] = false
            }
        }
        return out
    }

    /**
     * Classification mask built from **ink** pixels only: brighter than the theme's light
     * square (white ink) or darker than its dark square (black ink). Mid-luminance pixels
     * — square backgrounds, last-move highlights, the red check glow — are never ink, so
     * they cannot pollute the silhouette. Dark ink additionally has to differ from the
     * per-cell background: a border-shadow vignette on edge cells darkens the background
     * itself below the theme's dark-square threshold, and only the local test tells that
     * gradient from true black ink. Bright ink stands alone — a white body on a light
     * square is exactly where the local test fails (delta ~15). The ink mask is then
     * closed (dilated), reduced to its largest connected component (drops coordinate
     * labels and other debris), hole-filled, and eroded back. Filling matters: a white
     * piece body barely differs from a light square, so its ink is
     * outline-plus-body-fragments — after closing and filling, white and black pieces of
     * the same type converge to the same solid silhouette, which is what allows shared
     * type templates. Falls back to the per-cell [buildForegroundMask] when the theme
     * thresholds catch nothing.
     */
    fun buildGlyphMask(luma: IntArray, lightBg: Int, darkBg: Int): BooleanArray {
        val background = estimateBackgroundLuma(luma)
        var ink = clearEdgeRing(
            BooleanArray(luma.size) {
                luma[it] > lightBg + COLOR_MARGIN ||
                    (
                        luma[it] < darkBg - COLOR_MARGIN &&
                            abs(luma[it] - background) > FOREGROUND_LUMA_DELTA
                        )
            }
        )
        if (ink.none { it }) ink = buildForegroundMask(luma)
        return erode(fillHoles(largestComponent(dilate(ink))))
    }

    // Morphology radius; scales with resolution so anti-aliasing gaps in piece outlines
    // (2-3 px at MASK_SIZE 64) still get closed before hole filling.
    private val CLOSING_RADIUS = maxOf(1, MASK_SIZE / 32)

    /** Chebyshev dilation of a [MASK_SIZE] mask (closes anti-aliasing gaps in outlines). */
    private fun dilate(mask: BooleanArray, radius: Int = CLOSING_RADIUS): BooleanArray {
        val size = MASK_SIZE
        val out = BooleanArray(mask.size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                var value = false
                neighbours@ for (dy in -radius..radius) {
                    for (dx in -radius..radius) {
                        val ny = y + dy
                        val nx = x + dx
                        if (ny in 0 until size && nx in 0 until size && mask[ny * size + nx]) {
                            value = true
                            break@neighbours
                        }
                    }
                }
                out[y * size + x] = value
            }
        }
        return out
    }

    /** Chebyshev erosion of a [MASK_SIZE] mask (undoes [dilate] on the outer boundary). */
    private fun erode(mask: BooleanArray, radius: Int = CLOSING_RADIUS): BooleanArray {
        val size = MASK_SIZE
        val out = BooleanArray(mask.size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                var value = true
                neighbours@ for (dy in -radius..radius) {
                    for (dx in -radius..radius) {
                        val ny = y + dy
                        val nx = x + dx
                        if (ny !in 0 until size || nx !in 0 until size || !mask[ny * size + nx]) {
                            value = false
                            break@neighbours
                        }
                    }
                }
                out[y * size + x] = value
            }
        }
        return out
    }

    /** Keeps only the largest 4-connected foreground component of a [MASK_SIZE] mask. */
    private fun largestComponent(mask: BooleanArray): BooleanArray {
        val size = MASK_SIZE
        val componentOf = IntArray(mask.size)
        val queue = IntArray(mask.size)
        var bestComponent = 0
        var bestCount = 0
        var componentCount = 0
        for (start in mask.indices) {
            if (!mask[start] || componentOf[start] != 0) continue
            componentCount++
            componentOf[start] = componentCount
            var head = 0
            var tail = 0
            queue[tail++] = start
            var count = 0
            while (head < tail) {
                val index = queue[head++]
                count++
                val y = index / size
                val x = index % size
                if (x > 0 && mask[index - 1] && componentOf[index - 1] == 0) {
                    componentOf[index - 1] = componentCount
                    queue[tail++] = index - 1
                }
                if (x < size - 1 && mask[index + 1] && componentOf[index + 1] == 0) {
                    componentOf[index + 1] = componentCount
                    queue[tail++] = index + 1
                }
                if (y > 0 && mask[index - size] && componentOf[index - size] == 0) {
                    componentOf[index - size] = componentCount
                    queue[tail++] = index - size
                }
                if (y < size - 1 && mask[index + size] && componentOf[index + size] == 0) {
                    componentOf[index + size] = componentCount
                    queue[tail++] = index + size
                }
            }
            if (count > bestCount) {
                bestCount = count
                bestComponent = componentCount
            }
        }
        if (bestComponent == 0) return BooleanArray(mask.size)
        return BooleanArray(mask.size) { componentOf[it] == bestComponent }
    }

    /**
     * Fills interior holes of a [MASK_SIZE] mask: background is flood-filled from the
     * border; any background pixel the flood cannot reach is enclosed by the glyph and
     * becomes glyph.
     */
    private fun fillHoles(mask: BooleanArray): BooleanArray {
        val size = MASK_SIZE
        val reachable = BooleanArray(mask.size)
        val queue = IntArray(mask.size)
        var tail = 0
        fun seed(index: Int) {
            if (!mask[index] && !reachable[index]) {
                reachable[index] = true
                queue[tail++] = index
            }
        }
        for (i in 0 until size) {
            seed(i)
            seed((size - 1) * size + i)
            seed(i * size)
            seed(i * size + size - 1)
        }
        var head = 0
        while (head < tail) {
            val index = queue[head++]
            val y = index / size
            val x = index % size
            if (x > 0) seed(index - 1)
            if (x < size - 1) seed(index + 1)
            if (y > 0) seed(index - size)
            if (y < size - 1) seed(index + size)
        }
        return BooleanArray(mask.size) { mask[it] || !reachable[it] }
    }

    /**
     * Crops a [MASK_SIZE] mask to the glyph's bounding box and rescales it back to
     * [MASK_SIZE], preserving aspect ratio (longest box side fills the output, the other
     * is centred). Each output pixel is box-sampled over its source rectangle (≥ half
     * covered = set), which keeps upscaled masks smooth. Different sources render pieces
     * at different scales and offsets inside the cell; normalising removes both so
     * templates from one piece set stay comparable with screenshots from another.
     * An empty mask stays empty.
     */
    fun normalizeMask(mask: BooleanArray): BooleanArray {
        val size = MASK_SIZE
        var minX = size
        var minY = size
        var maxX = -1
        var maxY = -1
        for (index in mask.indices) {
            if (!mask[index]) continue
            val y = index / size
            val x = index % size
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
        if (maxX < 0) return BooleanArray(mask.size)

        val boxWidth = maxX - minX + 1
        val boxHeight = maxY - minY + 1
        val scale = maxOf(boxWidth, boxHeight)
        val offsetX = (scale - boxWidth) / 2
        val offsetY = (scale - boxHeight) / 2
        val out = BooleanArray(mask.size)
        for (y in 0 until size) {
            val srcY0 = minY - offsetY + y * scale / size
            val srcY1 = maxOf(minY - offsetY + (y + 1) * scale / size, srcY0 + 1)
            for (x in 0 until size) {
                val srcX0 = minX - offsetX + x * scale / size
                val srcX1 = maxOf(minX - offsetX + (x + 1) * scale / size, srcX0 + 1)
                var covered = 0
                var total = 0
                for (sy in srcY0 until srcY1) {
                    for (sx in srcX0 until srcX1) {
                        total++
                        if (sy in minY..maxY && sx in minX..maxX && mask[sy * size + sx]) covered++
                    }
                }
                out[y * size + x] = covered * 2 >= total
            }
        }
        return out
    }

    /**
     * Foreground coverage over the central region of the cell, ignoring an outer ring of
     * [MASK_SIZE]/8 pixels per side. Coordinate labels drawn inside corner cells and
     * cursor/highlight edges live near the border; excluding them keeps empty labelled
     * squares empty.
     */
    fun centralCoverage(mask: BooleanArray): Double {
        val size = MASK_SIZE
        val margin = size / 8
        var covered = 0
        var total = 0
        for (y in margin until size - margin) {
            for (x in margin until size - margin) {
                if (mask[y * size + x]) covered++
                total++
            }
        }
        return covered.toDouble() / total
    }

    /**
     * Horizontal asymmetry of a [MASK_SIZE] mask: 1 − IoU with its own mirror image.
     * The knight is the only asymmetric piece in every set (~0.23–0.27 measured, against
     * ≤ 0.07 for the symmetric pieces), so this separates fat knight silhouettes from the
     * kings and queens that raw mask IoU confuses them with.
     */
    fun maskAsymmetry(mask: BooleanArray): Double {
        val mirrored = BooleanArray(mask.size)
        for (y in 0 until MASK_SIZE) {
            for (x in 0 until MASK_SIZE) {
                mirrored[y * MASK_SIZE + x] = mask[y * MASK_SIZE + (MASK_SIZE - 1 - x)]
            }
        }
        return compareMasks(mask, mirrored)
    }

    /**
     * Mask distance as 1 − IoU (intersection over union): 0 = identical, 1 = disjoint.
     * Unlike a plain differing-pixel ratio, this ignores the shared empty background, so
     * two small glyphs of different types are not artificially "close" just because most
     * of the cell is background in both. Two empty masks count as identical.
     * Pure — unit-testable.
     */
    fun compareMasks(a: BooleanArray, b: BooleanArray): Double {
        require(a.size == b.size) { "Mask sizes differ: ${a.size} vs ${b.size}" }
        var intersection = 0
        var union = 0
        for (i in a.indices) {
            if (a[i] && b[i]) intersection++
            if (a[i] || b[i]) union++
        }
        if (union == 0) return 0.0
        return 1.0 - intersection.toDouble() / union
    }

    /**
     * Builds the FEN piece-placement field from board-coordinate squares.
     * Pure — no Bitmap, unit-testable. Order-independent: squares are addressed by
     * their [RecognizedSquare.row]/[RecognizedSquare.col].
     */
    fun buildPiecePlacement(squares: List<RecognizedSquare>): String {
        require(squares.size == BOARD_SIZE * BOARD_SIZE) {
            "Expected ${BOARD_SIZE * BOARD_SIZE} squares, got ${squares.size}"
        }
        val byPosition = arrayOfNulls<RecognizedSquare>(BOARD_SIZE * BOARD_SIZE)
        for (square in squares) {
            byPosition[square.row * BOARD_SIZE + square.col] = square
        }

        val ranks = ArrayList<String>(BOARD_SIZE)
        for (row in 0 until BOARD_SIZE) {
            val rank = StringBuilder()
            var emptyRun = 0
            for (col in 0 until BOARD_SIZE) {
                val square = byPosition[row * BOARD_SIZE + col]
                    ?: error("Missing square at row=$row col=$col")
                val symbol = square.symbol
                if (symbol == null) {
                    emptyRun++
                    continue
                }
                if (emptyRun > 0) {
                    rank.append(emptyRun)
                    emptyRun = 0
                }
                rank.append(symbol)
            }
            if (emptyRun > 0) rank.append(emptyRun)
            ranks.add(rank.toString())
        }
        return ranks.joinToString(separator = "/")
    }

    /**
     * Median luminance of four corner patches — the per-cell background estimate.
     * The patches are inset from the cell edge: the outermost pixels blend with the
     * neighbouring square (grid-line anti-aliasing), which would poison the estimate.
     * The median also survives a coordinate label sitting in one corner.
     */
    private fun estimateBackgroundLuma(luma: IntArray): Int {
        val size = MASK_SIZE
        val inset = size / 32
        val patch = size / 16
        val samples = ArrayList<Int>(patch * patch * 4)
        for (dy in inset until inset + patch) {
            for (dx in inset until inset + patch) {
                samples.add(luma[dy * size + dx])
                samples.add(luma[dy * size + (size - 1 - dx)])
                samples.add(luma[(size - 1 - dy) * size + dx])
                samples.add(luma[(size - 1 - dy) * size + (size - 1 - dx)])
            }
        }
        samples.sort()
        return samples[samples.size / 2]
    }

    /**
     * Decides piece colour from the share of BRIGHT ink among the ink pixels of the
     * glyph's **core** — the filled foreground glyph eroded by [COLOR_CORE_RADIUS], which
     * strips the outline and thin detail and leaves body-interior pixels. Fractions of
     * the whole glyph fail here: a black piece with large white detailing (a king's
     * cross, a queen's coronet) carries ~25% bright pixels, while a white outline-drawn
     * piece can be majority-dark from its own hatching — but among core INK pixels
     * (mid-luminance background trapped in the fill stays neutral) the bright share
     * splits cleanly: ≤ 0.31 for black pieces, ≥ 0.47 for white ones on the calibration
     * screenshots. When the core is empty the whole glyph is used; when the foreground
     * mask sees no ink at all (a white body blending into a light square), falls back to
     * theme-relative voting in the central region: brighter than a light square votes
     * white, darker than a dark square votes black. The defaults are the fallback theme
     * colours, for when nothing sampled the theme.
     */
    fun pieceIsWhite(
        luma: IntArray,
        lightBg: Int = DEFAULT_LIGHT_BG,
        darkBg: Int = DEFAULT_DARK_BG
    ): Boolean {
        val glyph = erode(fillHoles(largestComponent(dilate(buildForegroundMask(luma)))))
        val core = erode(glyph, COLOR_CORE_RADIUS)
        val region = if (core.any { it }) core else glyph
        var bright = 0
        var dark = 0
        for (i in region.indices) {
            if (!region[i]) continue
            if (luma[i] > BRIGHT_INK_LUMA) bright++
            else if (luma[i] < DARK_INK_LUMA) dark++
        }
        if (bright + dark > 0) {
            return bright.toDouble() / (bright + dark) >= WHITE_BRIGHT_SHARE
        }

        val size = MASK_SIZE
        val low = size / 4
        val high = size - size / 4
        var whiteVotes = 0
        var blackVotes = 0
        for (y in low until high) {
            for (x in low until high) {
                val value = luma[y * size + x]
                if (value > lightBg + COLOR_MARGIN) whiteVotes++
                else if (value < darkBg - COLOR_MARGIN) blackVotes++
            }
        }
        return whiteVotes >= blackVotes
    }

    // Low enough that a dark-grey piece fill on a dark square still counts as foreground
    // (a cartoon-set black pawn on a blue square differs from it by only ~43 luma).
    private const val FOREGROUND_LUMA_DELTA = 28

    // Central-region coverage below this = empty square. High enough that a coordinate
    // label poking into the central region (~3-5%) is not mistaken for a piece; the
    // smallest piece (pawn) covers ~15%+.
    private const val DEFAULT_EMPTY_COVERAGE = 0.07

    // Best-template confidence below this = empty, not a piece. Sits in the clean gap
    // between real pieces (>= 0.69 measured) and coordinate-label debris from a slightly
    // misaligned crop (~0.00), while staying under the LOW_CONFIDENCE band so genuine
    // borderline pieces are still surfaced as uncertain rather than dropped.
    private const val MIN_PIECE_CONFIDENCE = 0.35

    // Ink / piece-colour margin over the sampled theme extremes (absorbs anti-aliasing).
    private const val COLOR_MARGIN = 8

    // Piece-colour split: core pixels brighter than BRIGHT_INK_LUMA are white ink, darker
    // than DARK_INK_LUMA black ink; a piece is white when white ink holds at least
    // WHITE_BRIGHT_SHARE of the core's ink. Measured margins on the calibration
    // screenshots: black pieces ≤ 0.31, white pieces ≥ 0.47.
    private const val BRIGHT_INK_LUMA = 180
    private const val DARK_INK_LUMA = 80
    private const val WHITE_BRIGHT_SHARE = 0.40

    // Erosion radius that reduces the filled glyph to its body interior for the colour
    // decision (5 px at MASK_SIZE 64 outlasts outlines and thin detail).
    private val COLOR_CORE_RADIUS = maxOf(1, MASK_SIZE / 12)

    // Width of the cell-edge ring cleared by [clearEdgeRing].
    private val EDGE_MARGIN = maxOf(1, MASK_SIZE / 16)

    // Weight of the glyph-height difference in the type distance, alongside 1 − IoU.
    private const val HEIGHT_WEIGHT = 1.0

    // Weight of the horizontal-asymmetry difference in the type distance. Separates the
    // knight (the only asymmetric piece) from kings/queens when a fat knight silhouette
    // lands between the two by raw IoU.
    private const val ASYMMETRY_WEIGHT = 0.5

    // Fallbacks when a square colour has no empty samples (a nearly full board).
    private const val DEFAULT_LIGHT_BG = 200
    private const val DEFAULT_DARK_BG = 100
}
