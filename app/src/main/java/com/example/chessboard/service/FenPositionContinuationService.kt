package com.example.chessboard.service

/*
 * File role: coordinates persistence for continuations owned by FEN catalog positions.
 * Allowed here:
 * - validating complete move sequences from their stored parent FEN
 * - canonical UCI serialization and single or atomic batch continuation operations
 * Not allowed here:
 * - SAN presentation, Compose UI, screen navigation, or continuation editing state
 * Validation date: 2026-09-02
 */

import androidx.room.withTransaction
import com.example.chessboard.boardmodel.buildChesslibMoveFromUci
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

sealed interface CreateFenPositionContinuationBatchResult {
    data class Success(
        val insertedIds: List<Long>,
        val coveredByStoredLinesCount: Int,
    ) : CreateFenPositionContinuationBatchResult

    data object PositionNotFound : CreateFenPositionContinuationBatchResult
    data object EmptyBatch : CreateFenPositionContinuationBatchResult
    data class EmptyContinuation(val lineIndex: Int) : CreateFenPositionContinuationBatchResult
    data class InvalidMove(
        val lineIndex: Int,
        val plyIndex: Int,
    ) : CreateFenPositionContinuationBatchResult
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

    suspend fun createBatch(
        positionId: Long,
        preparation: FenPositionContinuationBatchPreparation,
    ): CreateFenPositionContinuationBatchResult {
        if (preparation.preparedUciLines.isEmpty()) {
            return CreateFenPositionContinuationBatchResult.EmptyBatch
        }

        return database.withTransaction {
            val position = positionDao.getById(positionId)
                ?: return@withTransaction CreateFenPositionContinuationBatchResult.PositionNotFound
            val canonicalLines = mutableListOf<List<String>>()
            val canonicalUciPattern = Regex("^[a-h][1-8][a-h][1-8][qrbn]?$")

            for ((lineIndex, line) in preparation.preparedUciLines.withIndex()) {
                if (line.isEmpty()) {
                    return@withTransaction CreateFenPositionContinuationBatchResult.EmptyContinuation(
                        lineIndex = lineIndex,
                    )
                }

                val board = Board().also { board ->
                    board.loadFromFen("${position.fen} 0 1")
                }
                val canonicalMoves = mutableListOf<String>()

                for ((plyIndex, uciMove) in line.withIndex()) {
                    if (!canonicalUciPattern.matches(uciMove)) {
                        return@withTransaction CreateFenPositionContinuationBatchResult.InvalidMove(
                            lineIndex = lineIndex,
                            plyIndex = plyIndex,
                        )
                    }

                    val move = buildChesslibMoveFromUci(uci = uciMove, board = board)
                    val legalMove = board.legalMoves().firstOrNull { candidate -> candidate == move }
                        ?: return@withTransaction CreateFenPositionContinuationBatchResult.InvalidMove(
                            lineIndex = lineIndex,
                            plyIndex = plyIndex,
                        )
                    canonicalMoves += buildUciFromChesslibMove(legalMove)
                    board.doMove(legalMove)
                }

                canonicalLines += canonicalMoves
            }

            val storedUciLines = continuationDao.getByPositionId(positionId)
                .map(FenPositionContinuationEntity::toUciMoves)
            val comparison = compareFenPositionContinuationBatchWithStoredLines(
                preparation = preparation.copy(preparedUciLines = canonicalLines),
                storedUciLines = storedUciLines,
            )
            val entitiesToInsert = comparison.uciLinesToInsert.map { line ->
                FenPositionContinuationEntity(
                    positionId = positionId,
                    uciMoves = line.joinToString(separator = " "),
                )
            }
            var insertedIds = emptyList<Long>()
            if (entitiesToInsert.isNotEmpty()) {
                insertedIds = continuationDao.insertAll(entitiesToInsert)
            }

            CreateFenPositionContinuationBatchResult.Success(
                insertedIds = insertedIds,
                coveredByStoredLinesCount = comparison.coveredByStoredLinesCount,
            )
        }
    }

    suspend fun getById(id: Long): FenPositionContinuationEntity? {
        return continuationDao.getById(id)
    }

    suspend fun getByPositionId(positionId: Long): List<FenPositionContinuationEntity> {
        return continuationDao.getByPositionId(positionId)
    }

    suspend fun getUciLinesByPositionId(positionId: Long): List<List<String>> {
        return continuationDao.getByPositionId(positionId)
            .map(FenPositionContinuationEntity::toUciMoves)
    }

    suspend fun deleteById(id: Long): Boolean {
        return continuationDao.deleteById(id) > 0
    }
}

private fun FenPositionContinuationEntity.toUciMoves(): List<String> {
    return uciMoves.split(' ').filter { move -> move.isNotBlank() }
}
