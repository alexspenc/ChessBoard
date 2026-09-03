package com.example.chessboard.service

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

/**
 * Import-position-from-screenshot flow logic on top of [BoardImageRecognizer]:
 * decoding the picked image, cropping the board region out of the full screenshot,
 * and turning recognised squares into the FEN + warnings the import screen shows.
 * See PLAN_BOARD_IMAGE_RECOGNITION.md.
 */
object BoardImageImportService {

    /**
     * Squares recognised with confidence below this are reported as uncertain. Calibrated
     * against the screenshots in `test positions/`: it flags every square the recognizer
     * actually gets wrong on them, and 2 correct squares alongside, while flagging nothing
     * at all on the boards that recognise cleanly. Raising it drowns the warning — at 0.45
     * a correctly recognised board flags a third of its own pieces.
     */
    const val LOW_CONFIDENCE = 0.30

    /** Largest decode dimension; screenshots above it are downsampled by power-of-two. */
    private const val MAX_DECODE_DIMENSION = 2048

    /**
     * Everything the import screen needs from one recognition run. [positionFen] is in the
     * position-search format (`placement <side> - -`); castling and en passant cannot be
     * read off a screenshot, so they default to "none" and are edited in the position
     * editor. The side-to-move token carries the viewing orientation so the editor keeps
     * the board oriented as in the screenshot: `w` when [whiteAtBottom], `b` otherwise
     * (position search derives board orientation from that token). [uncertainSquares] are
     * algebraic names of occupied squares below [LOW_CONFIDENCE].
     */
    data class RecognitionOutcome(
        val squares: List<RecognizedSquare>,
        val piecePlacement: String,
        val positionFen: String,
        val issues: List<String>,
        val uncertainSquares: List<String>,
        val whiteAtBottom: Boolean,
    )

    /**
     * Decodes a picked image into a [Bitmap], downsampled so its largest side stays at or
     * under [MAX_DECODE_DIMENSION]. Returns null when the uri cannot be read or decoded.
     */
    fun decodeBoardImage(contentResolver: ContentResolver, uri: Uri): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            val options = BitmapFactory.Options().apply {
                inSampleSize = resolveInSampleSize(maxOf(bounds.outWidth, bounds.outHeight))
            }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Crops the user-framed board region and recognises it into a [RecognitionOutcome]. */
    fun recognizeBoard(
        image: PixelGrid,
        cropLeft: Int,
        cropTop: Int,
        cropWidth: Int,
        cropHeight: Int,
        templates: List<PieceTemplate>,
        whiteAtBottom: Boolean,
    ): RecognitionOutcome {
        val board = cropPixelGrid(image, cropLeft, cropTop, cropWidth, cropHeight)
        return buildOutcome(
            BoardImageRecognizer.recognize(board, templates, whiteAtBottom),
            whiteAtBottom,
        )
    }

    /**
     * Re-orients an existing outcome when the user flips the "white at bottom" toggle —
     * no re-recognition, just [BoardImageRecognizer.flipOrientation] plus rebuilt FEN
     * and warnings.
     */
    fun flipOutcome(outcome: RecognitionOutcome): RecognitionOutcome =
        buildOutcome(
            BoardImageRecognizer.flipOrientation(outcome.squares),
            !outcome.whiteAtBottom,
        )

    /** Copies a sub-rectangle out of [grid], clamped to the grid bounds. Pure. */
    fun cropPixelGrid(grid: PixelGrid, left: Int, top: Int, width: Int, height: Int): PixelGrid {
        val clampedLeft = left.coerceIn(0, grid.width - 1)
        val clampedTop = top.coerceIn(0, grid.height - 1)
        val clampedWidth = width.coerceIn(1, grid.width - clampedLeft)
        val clampedHeight = height.coerceIn(1, grid.height - clampedTop)

        val pixels = IntArray(clampedWidth * clampedHeight)
        for (y in 0 until clampedHeight) {
            System.arraycopy(
                grid.pixels,
                (clampedTop + y) * grid.width + clampedLeft,
                pixels,
                y * clampedWidth,
                clampedWidth,
            )
        }
        return PixelGrid(clampedWidth, clampedHeight, pixels)
    }

    private fun buildOutcome(
        squares: List<RecognizedSquare>,
        whiteAtBottom: Boolean,
    ): RecognitionOutcome {
        val placement = BoardImageRecognizer.buildPiecePlacement(squares)
        val side = if (whiteAtBottom) "w" else "b"
        return RecognitionOutcome(
            squares = squares,
            piecePlacement = placement,
            positionFen = "$placement $side - -",
            issues = BoardImageRecognizer.findPositionIssues(squares),
            uncertainSquares = squares
                .filter { it.symbol != null && it.confidence < LOW_CONFIDENCE }
                .sortedWith(compareBy({ it.row }, { it.col }))
                .map { squareName(it) },
            whiteAtBottom = whiteAtBottom,
        )
    }

    private fun squareName(square: RecognizedSquare): String =
        "${'a' + square.col}${BoardImageRecognizer.BOARD_SIZE - square.row}"

    private fun resolveInSampleSize(largestDimension: Int): Int {
        var sampleSize = 1
        while (largestDimension / sampleSize > MAX_DECODE_DIMENSION) {
            sampleSize *= 2
        }
        return sampleSize
    }
}
