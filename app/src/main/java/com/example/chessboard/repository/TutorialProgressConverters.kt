package com.example.chessboard.repository

/**
 * File role: converts tutorial progress enums to and from Room-compatible strings.
 * Allowed here:
 * - Room type converters for tutorial persistence enums
 * Not allowed here:
 * - database queries
 * - tutorial business rules or UI orchestration
 * Validation date: 2026-05-02
 */
import androidx.room.TypeConverter
import com.example.chessboard.entity.TutorialRunStatus
import com.example.chessboard.entity.TutorialStage
import com.example.chessboard.entity.TutorialType

class TutorialProgressConverters {

    @TypeConverter
    fun fromTutorialType(value: TutorialType): String {
        return value.name
    }

    @TypeConverter
    fun toTutorialType(value: String): TutorialType {
        return TutorialType.valueOf(value)
    }

    @TypeConverter
    fun fromTutorialStage(value: TutorialStage): String {
        return value.name
    }

    @TypeConverter
    fun toTutorialStage(value: String): TutorialStage {
        return TutorialStage.valueOf(value)
    }

    @TypeConverter
    fun fromTutorialRunStatus(value: TutorialRunStatus): String {
        return value.name
    }

    @TypeConverter
    fun toTutorialRunStatus(value: String): TutorialRunStatus {
        return TutorialRunStatus.valueOf(value)
    }
}
