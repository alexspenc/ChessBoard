package com.example.chessboard.entity

/*
 * File role: defines Room storage for one description attached to a four-field FEN position.
 * Allowed here:
 * - persisted description identity and canonical position FEN
 * - temporary ownership by a catalog position and cascading deletion with that position
 * - database-level uniqueness of the described position FEN
 * Not allowed here:
 * - FEN normalization or UI state
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
            parentColumns = ["fen"],
            childColumns = ["fen"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["fen"], unique = true),
    ],
)
// TODO: Add a description-reference table covering catalog positions and continuation positions,
// then redesign description ownership so shared descriptions are deleted only when no references remain.
data class FenPositionDescriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fen: String,
    val description: String,
)
