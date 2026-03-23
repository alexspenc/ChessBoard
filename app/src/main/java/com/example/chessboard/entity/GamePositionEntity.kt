package com.example.chessboard.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "game_positions",
    primaryKeys = ["gameId", "ply"],
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PositionEntity::class,
            parentColumns = ["id"],
            childColumns = ["positionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["positionId"]),
        Index(value = ["gameId"])
    ]
)
data class GamePositionEntity(
    val gameId: Long,
    val positionId: Long,
    val ply: Int,
    val sideMask: Int
)
