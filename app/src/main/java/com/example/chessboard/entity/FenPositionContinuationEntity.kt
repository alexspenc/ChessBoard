package com.example.chessboard.entity

/*
 * File role: defines Room storage for one continuation owned by a FEN catalog position.
 * Allowed here:
 * - persisted continuation identity, owning position id, and canonical UCI moves
 * - per-position uniqueness and cascading deletion with the owning position
 * Not allowed here:
 * - move validation, SAN formatting, persistence workflows, or UI state
 * Validation date: 2026-09-02
 */

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "fen_position_continuations",
    foreignKeys = [
        ForeignKey(
            entity = FenPositionEntity::class,
            parentColumns = ["id"],
            childColumns = ["positionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["positionId", "uciMoves"], unique = true),
    ],
)
data class FenPositionContinuationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val positionId: Long,
    val uciMoves: String,
)
