package com.example.chessboard.service

/*
 * File role: coordinates persistence for continuations owned by FEN catalog positions.
 * Allowed here:
 * - validating complete move sequences from their stored parent FEN
 * - canonical UCI serialization and continuation create/read/delete operations
 * Not allowed here:
 * - SAN presentation, Compose UI, screen navigation, or continuation editing state
 * Validation date: 2026-09-02
 */

import androidx.room.withTransaction
import com.example.chessboard.boardmodel.buildUciFromChesslibMove
import com.example.chessboard.entity.FenPositionContinuationEntity
import com.example.chessboard.repository.AppDatabase
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.move.Move

sealed interface CreateFenPositionContinuationResult {
    data class Success(val id: Long) : CreateFenPositionContinuationResult
    data object PositionNotFound : CreateFenPositionContinuationResult
    data object EmptyMoves : CreateFenPositionContinuationResult
    data class InvalidMove(val plyIndex: Int) : CreateFenPositionContinuationResult
    data object DuplicateContinuation : CreateFenPositionContinuationResult
}

class FenPositionContinuationService(
    private val database: AppDatabase,
) {
    private val positionDao = database.fenPositionDao()
    private val continuationDao = database.fenPositionContinuationDao()

    suspend fun create(
        positionId: Long,
        moves: List<Move>,
    ): CreateFenPositionContinuationResult {
        if (moves.isEmpty()) {
            return CreateFenPositionContinuationResult.EmptyMoves
        }

        return database.withTransaction {
            val position = positionDao.getById(positionId)
                ?: return@withTransaction CreateFenPositionContinuationResult.PositionNotFound
            val board = Board().also { board ->
                board.loadFromFen("${position.fen} 0 1")
            }
            val canonicalMoves = mutableListOf<String>()

            for ((plyIndex, move) in moves.withIndex()) {
                val legalMove = board.legalMoves().firstOrNull { candidate -> candidate == move }
                    ?: return@withTransaction CreateFenPositionContinuationResult.InvalidMove(
                        plyIndex = plyIndex,
                    )
                canonicalMoves += buildUciFromChesslibMove(legalMove)
                board.doMove(legalMove)
            }

            val id = continuationDao.insert(
                FenPositionContinuationEntity(
                    positionId = positionId,
                    uciMoves = canonicalMoves.joinToString(separator = " "),
                ),
            )
            if (id == -1L) {
                return@withTransaction CreateFenPositionContinuationResult.DuplicateContinuation
            }

            CreateFenPositionContinuationResult.Success(id)
        }
    }

    suspend fun getById(id: Long): FenPositionContinuationEntity? {
        return continuationDao.getById(id)
    }

    suspend fun getByPositionId(positionId: Long): List<FenPositionContinuationEntity> {
        return continuationDao.getByPositionId(positionId)
    }

    suspend fun deleteById(id: Long): Boolean {
        return continuationDao.deleteById(id) > 0
    }
}
