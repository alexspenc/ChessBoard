package com.example.chessboard.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "games",
    indices = [
        Index(value = ["white"]),
        Index(value = ["black"]),
        Index(value = ["event"]),
        Index(value = ["date"])
    ]
)
data class GameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val white: String? = null,
    val black: String? = null,
    val result: String? = null,
    val event: String? = null,
    val site: String? = null,
    val date: Long = 0,
    val round: String? = null,
    val eco: String? = null,
    val pgn: String,
    val initialFen: String,
    val sideMask: Int = SideMask.BOTH
)
