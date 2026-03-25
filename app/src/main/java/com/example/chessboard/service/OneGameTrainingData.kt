package com.example.chessboard.service

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class OneGameTrainingData(
    val gameId: Long,
    val weight: Int
) {
    companion object {
        fun fromJson(jsonString: String): List<OneGameTrainingData> {
            return try {
                Json.decodeFromString(jsonString)
            } catch (e: Exception) {
                val method = Throwable().stackTrace[1].methodName
                println("Error [${e}] on [${method}]")
                emptyList()
            }
        }

        fun toJson(games: List<OneGameTrainingData>): String {
            return Json.encodeToString(games)
        }
    }
}
