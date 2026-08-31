package com.example.chessboard.entity

/*
 * File role: defines the Room storage model for positions in the FEN catalog.
 * Allowed here:
 * - persisted position identity, title, and theme
 * - database-level uniqueness of the canonical position FEN
 * Not allowed here:
 * - description text, continuations, FEN normalization, or UI state
 * Validation date: 2026-08-31
 */

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "fen_positions",
    indices = [
        Index(value = ["fen"], unique = true),
    ],
)
data class FenPositionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fen: String,
    val name: String,
    val theme: String,
)
