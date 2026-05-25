package com.example.chessboard.entity

/*
 * File role: stores the optional "dubious line" marker for a persisted line.
 * Allowed here:
 * - Room storage fields for one marker keyed by an existing line id
 * - database-level constraints tying the marker to the line lifecycle
 * Not allowed here:
 * - UI wording, training formulas, or marker editing workflow
 * Validation date: 2026-05-25
 */

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "dubious_lines",
    foreignKeys = [
        ForeignKey(
            entity = LineEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class DubiousLineEntity(
    @PrimaryKey
    @ColumnInfo(name = "gameId")
    val lineId: Long,
    val weight: Int,
)
