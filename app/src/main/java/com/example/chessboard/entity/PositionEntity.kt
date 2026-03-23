package com.example.chessboard.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// We can have collision on hash.
// So first select all by hash and then in application compare with fen.
//
// sideMask — the side from whose perspective the position is considered:
//   1 — White
//   2 — Black
//   3 — Either side = bitmask White | Black
//
// There may be situations where a player uses the same opening line both as White and as Black.
// Without the ability to mark a position for both sides, such a line would be treated as a
// duplicate and could not be stored.
//
// There are also positions that are favorable for one side only.
// This flag helps to filter and search for positions for a specific side.
@Entity(
    tableName = "positions",
    indices = [
        Index(value = ["hash"], unique = false),
        Index(value = ["hash", "fen"], unique = true),
        Index(value = ["hash", "sideMask"], unique = false)
    ]
)
data class PositionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hash: Long,
    val fen: String,
    val sideMask: Int,
)
