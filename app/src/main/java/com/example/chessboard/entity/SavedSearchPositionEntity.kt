package com.example.chessboard.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saved_search_positions",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["hashForSearch", "fenForSearch"], unique = true),
        Index(value = ["hashFull", "fenFull"], unique = true),
    ]
)
data class SavedSearchPositionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val fenForSearch: String,
    val hashForSearch: Long,
    val fenFull: String? = null,
    val hashFull: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
