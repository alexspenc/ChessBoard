package com.example.chessboard.ui.boardanimation

/**
 * Projects temporary render scenes while a board animation is in flight.
 * Keep application scene transformation and adaptation to library animation geometry here.
 * Do not add queue orchestration, controller ownership, or screen-specific UI to this file.
 * Validation date: 2026-11-14.
 */

import androidx.compose.ui.geometry.Offset
import com.example.chessboard.chessboardui.animation.calculateAnimatedPieceCenter
import com.example.chessboard.chesscore.model.Square
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.boardrender.BoardRenderAnimatedPiece
import com.example.chessboard.ui.boardrender.BoardRenderScene
import com.example.chessboard.chessboardui.model.BoardOrientation as LibraryBoardOrientation

/**
 * Projects an application animation frame using chessboardui to calculate moving centers.
 * Keeps the source references, capture removal, last-move highlight, and castling piece order.
 * Returns baseScene unchanged if a required source piece is missing, without calculating centers.
 *
 * @param baseScene the application's placement and current display metadata.
 * @param activeAction the simple move, capture, or castling command; squares must use a1..h8.
 * @param progress the finite movement fraction; values outside 0..1 are clamped by the library.
 * @param squareSizePx the positive finite size of one square in pixels.
 * @return the temporary application scene, retaining the existing moving-piece representation.
 * @throws IllegalArgumentException during center calculation if a movement square, progress, or
 * square size is invalid. Conversion and library validation exceptions propagate unchanged.
 */
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

/**
 * Projects a capture frame with the captured piece removed and the attacker drawn at its current center.
 * Returns baseScene unchanged if the attacker's source piece is missing.
 *
 * @param baseScene the application's source placement and display metadata.
 * @param activeAction the capture command; movement squares must use a1..h8.
 * @param progress the finite movement fraction, clamped to 0..1 by the library.
 * @param squareSizePx the positive finite size of one square in pixels.
 * @return the temporary scene using the existing drag fields for the animated attacker.
 * @throws IllegalArgumentException during center calculation for an invalid movement square,
 * non-finite progress, or invalid square size. Collaborator exceptions propagate unchanged.
 */
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
    fun parseSquare(value: String, field: String): Square {
        require(value.length == 2 && value[0] in 'a'..'h' && value[1] in '1'..'8') {
            "Invalid animation square in $field: '$value'; expected a1..h8"
        }
        return Square(value[0], value[1] - '0')
    }

    val orientation = when (scene.orientation) {
        BoardOrientation.WHITE -> LibraryBoardOrientation.WHITE
        BoardOrientation.BLACK -> LibraryBoardOrientation.BLACK
    }
    return calculateAnimatedPieceCenter(
        fromSquare = parseSquare(from, "from"),
        toSquare = parseSquare(to, "to"),
        progress = progress,
        orientation = orientation,
        squareSizePx = squareSizePx,
    )
}
