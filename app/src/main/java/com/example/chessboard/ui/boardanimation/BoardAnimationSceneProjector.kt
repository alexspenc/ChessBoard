package com.example.chessboard.ui.boardanimation

/**
 * Projects temporary render scenes while a board animation is in flight.
 * Keep interpolation and scene transformation for active animation commands here.
 * Do not add queue orchestration, controller ownership, or screen-specific UI to this file.
 * Validation date: 2026-07-10
 */

import androidx.compose.ui.geometry.Offset
import com.example.chessboard.ui.boardrender.BoardRenderAnimatedPiece
import com.example.chessboard.ui.boardrender.BoardRenderScene
import com.example.chessboard.ui.boardrender.calculateSquareCenterOffset

fun buildAnimatedBoardRenderScene(
    baseScene: BoardRenderScene,
    activeAction: AnimatedBoardMoveAction,
    progress: Float,
    squareSizePx: Float,
): BoardRenderScene {
    return when (activeAction) {
        is AnimateSimpleMoveAction -> buildAnimatedSimpleMoveBoardRenderScene(
            baseScene = baseScene,
            activeAction = activeAction,
            progress = progress,
            squareSizePx = squareSizePx,
        )
        is AnimateCaptureMoveAction -> buildAnimatedCaptureBoardRenderScene(
            baseScene = baseScene,
            activeAction = activeAction,
            progress = progress,
            squareSizePx = squareSizePx,
        )
        is AnimateCastlingMoveAction -> buildAnimatedCastlingBoardRenderScene(
            baseScene = baseScene,
            activeAction = activeAction,
            progress = progress,
            squareSizePx = squareSizePx,
        )
    }
}

fun applyAnimatedBoardMove(
    scene: BoardRenderScene,
    action: AnimatedBoardMoveAction,
): BoardRenderScene {
    return when (action) {
        is AnimateSimpleMoveAction -> applyAnimatedSimpleMove(scene = scene, action = action)
        is AnimateCaptureMoveAction -> applyAnimatedCaptureMove(scene = scene, action = action)
        is AnimateCastlingMoveAction -> applyAnimatedCastlingMove(scene = scene, action = action)
    }
}

private fun buildAnimatedCastlingBoardRenderScene(
    baseScene: BoardRenderScene,
    activeAction: AnimateCastlingMoveAction,
    progress: Float,
    squareSizePx: Float,
): BoardRenderScene {
    val king = baseScene.pieces.find { piece -> piece.square == activeAction.from }
    val rook = baseScene.pieces.find { piece -> piece.square == activeAction.rookFrom }
    if (king == null || rook == null) {
        return baseScene
    }

    return baseScene.copy(
        lastMoveHighlight = activeAction.lastMoveHighlight,
        animatedPieces = listOf(
            BoardRenderAnimatedPiece(
                fromSquare = activeAction.from,
                centerOffset = calculateAnimatedMoveOffset(
                    scene = baseScene,
                    from = activeAction.from,
                    to = activeAction.to,
                    progress = progress,
                    squareSizePx = squareSizePx,
                ),
            ),
            BoardRenderAnimatedPiece(
                fromSquare = activeAction.rookFrom,
                centerOffset = calculateAnimatedMoveOffset(
                    scene = baseScene,
                    from = activeAction.rookFrom,
                    to = activeAction.rookTo,
                    progress = progress,
                    squareSizePx = squareSizePx,
                ),
            ),
        ),
    )
}

private fun buildAnimatedSimpleMoveBoardRenderScene(
    baseScene: BoardRenderScene,
    activeAction: AnimateSimpleMoveAction,
    progress: Float,
    squareSizePx: Float,
): BoardRenderScene {
    val movingPiece = baseScene.pieces.find { piece -> piece.square == activeAction.from }
    if (movingPiece == null) {
        return baseScene
    }

    return baseScene.copy(
        lastMoveHighlight = activeAction.lastMoveHighlight,
        dragFromSquare = activeAction.from,
        dragOffset = calculateAnimatedMoveOffset(
            scene = baseScene,
            from = activeAction.from,
            to = activeAction.to,
            progress = progress,
            squareSizePx = squareSizePx,
        ),
    )
}

fun buildAnimatedCaptureBoardRenderScene(
    baseScene: BoardRenderScene,
    activeAction: AnimateCaptureMoveAction,
    progress: Float,
    squareSizePx: Float,
): BoardRenderScene {
    val movingPiece = baseScene.pieces.find { piece -> piece.square == activeAction.from }
    if (movingPiece == null) {
        return baseScene
    }

    return baseScene.copy(
        pieces = baseScene.pieces.filterNot { piece -> piece.square == activeAction.capturedSquare },
        lastMoveHighlight = activeAction.lastMoveHighlight,
        dragFromSquare = activeAction.from,
        dragOffset = calculateAnimatedMoveOffset(
            scene = baseScene,
            from = activeAction.from,
            to = activeAction.to,
            progress = progress,
            squareSizePx = squareSizePx,
        ),
    )
}

fun applyAnimatedSimpleMove(
    scene: BoardRenderScene,
    action: AnimateSimpleMoveAction,
): BoardRenderScene {
    val movedPieceIndex = scene.pieces.indexOfFirst { piece -> piece.square == action.from }
    if (movedPieceIndex == -1) {
        return scene
    }

    val updatedPieces = scene.pieces.toMutableList()
    val movedPiece = updatedPieces[movedPieceIndex]
    updatedPieces[movedPieceIndex] = movedPiece.copy(square = action.to)
    return scene.copy(
        pieces = updatedPieces,
        lastMoveHighlight = action.lastMoveHighlight,
        dragFromSquare = null,
        dragOffset = Offset.Zero,
    )
}

fun applyAnimatedCaptureMove(
    scene: BoardRenderScene,
    action: AnimateCaptureMoveAction,
): BoardRenderScene {
    val movedPieceIndex = scene.pieces.indexOfFirst { piece -> piece.square == action.from }
    if (movedPieceIndex == -1) {
        return scene
    }

    val updatedPieces = scene.pieces
        .filterNot { piece -> piece.square == action.capturedSquare }
        .toMutableList()
    val updatedMovedPieceIndex = updatedPieces.indexOfFirst { piece -> piece.square == action.from }
    if (updatedMovedPieceIndex == -1) {
        return scene
    }

    val movedPiece = updatedPieces[updatedMovedPieceIndex]
    updatedPieces[updatedMovedPieceIndex] = movedPiece.copy(square = action.to)
    return scene.copy(
        pieces = updatedPieces,
        lastMoveHighlight = action.lastMoveHighlight,
        dragFromSquare = null,
        dragOffset = Offset.Zero,
    )
}

fun applyAnimatedCastlingMove(
    scene: BoardRenderScene,
    action: AnimateCastlingMoveAction,
): BoardRenderScene {
    val kingIndex = scene.pieces.indexOfFirst { piece -> piece.square == action.from }
    val rookIndex = scene.pieces.indexOfFirst { piece -> piece.square == action.rookFrom }
    if (kingIndex == -1 || rookIndex == -1) {
        return scene
    }

    val updatedPieces = scene.pieces.toMutableList()
    updatedPieces[kingIndex] = updatedPieces[kingIndex].copy(square = action.to)
    updatedPieces[rookIndex] = updatedPieces[rookIndex].copy(square = action.rookTo)
    return scene.copy(
        pieces = updatedPieces,
        lastMoveHighlight = action.lastMoveHighlight,
        animatedPieces = emptyList(),
        dragFromSquare = null,
        dragOffset = Offset.Zero,
    )
}

private fun calculateAnimatedMoveOffset(
    scene: BoardRenderScene,
    from: String,
    to: String,
    progress: Float,
    squareSizePx: Float,
): Offset {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val fromOffset = calculateSquareCenterOffset(
        square = from,
        orientation = scene.orientation,
        squareSizePx = squareSizePx,
    )
    val toOffset = calculateSquareCenterOffset(
        square = to,
        orientation = scene.orientation,
        squareSizePx = squareSizePx,
    )

    return Offset(
        x = lerp(fromOffset.x, toOffset.x, clampedProgress),
        y = lerp(fromOffset.y, toOffset.y, clampedProgress),
    )
}

private fun lerp(start: Float, end: Float, progress: Float): Float {
    return start + ((end - start) * progress)
}
