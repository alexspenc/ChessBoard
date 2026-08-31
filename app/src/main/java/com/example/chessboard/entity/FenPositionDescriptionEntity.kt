package com.example.chessboard.entity

/*
 * File role: defines Room storage for one description owned by a FEN catalog position.
 * Allowed here:
 * - persisted description identity and owning catalog-position id
 * - one-to-one ownership and cascading deletion with the owning position
 * Not allowed here:
 * - shared descriptions, FEN normalization, or UI state
 * Validation date: 2026-08-31
 */

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "fen_position_descriptions",
    foreignKeys = [
        ForeignKey(
            entity = FenPositionEntity::class,
            parentColumns = ["id"],
            childColumns = ["positionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["positionId"], unique = true),
    ],
)
data class FenPositionDescriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val positionId: Long,
    val description: String,
)
