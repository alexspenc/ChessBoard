package com.example.chessboard.service

import android.content.Context
import android.graphics.BitmapFactory

/**
 * Piece-type templates for [BoardImageRecognizer], built from the bundled reference board
 * (`assets/reference_board.png` — a clean start position cropped to the board edges) and
 * cached for the process lifetime. Lazy: nothing is decoded until the first recognition
 * asks for templates.
 */
object BoardImageTemplates {

    private const val REFERENCE_BOARD_ASSET = "reference_board.png"

    @Volatile
    private var cached: List<PieceTemplate>? = null

    /** Templates from the bundled reference board; decoded and built on first call only. */
    fun get(context: Context): List<PieceTemplate> {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            return buildFromAsset(context).also { cached = it }
        }
    }

    private fun buildFromAsset(context: Context): List<PieceTemplate> {
        val bitmap = context.assets.open(REFERENCE_BOARD_ASSET).use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: error("Could not decode asset $REFERENCE_BOARD_ASSET")
        try {
            return BoardImageRecognizer.buildTemplatesFromReferenceBoard(bitmap)
        } finally {
            bitmap.recycle()
        }
    }
}
