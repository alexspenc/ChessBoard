package com.example.chessboard.entity

/*
 * File role: defines Room storage for one description attached to a four-field FEN position.
 * Allowed here:
 * - persisted description identity and canonical position FEN
 * - database-level uniqueness of the described position FEN
 * Not allowed here:
 * - catalog-row foreign keys, FEN normalization, or UI state
 * Validation date: 2026-08-31
 */

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "fen_position_descriptions",
    indices = [
        Index(value = ["fen"], unique = true),
    ],
)
data class FenPositionDescriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fen: String,
    val description: String,
)
