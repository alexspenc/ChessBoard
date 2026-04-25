package com.example.chessboard.ui.components

/**
 * Shared board section wrapper for screens that need a standard interactive chess board.
 *
 * Keep generic board framing and sizing here. Do not add screen-specific controls,
 * training workflow logic, or persistence behavior to this file.
 */
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.ui.ChessBoardWithCoordinates
import com.example.chessboard.ui.theme.AppDimens

@Composable
fun ChessBoardSection(
    gameController: GameController,
    modifier: Modifier = Modifier,
    boardModifier: Modifier = Modifier,
) {
    val boardState = gameController.boardState

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(AppDimens.radiusXl))
            .pointerInput(Unit) {
                // Consume all pointer events in the Initial pass (leaf→root) so that any
                // ancestor ScrollableNode (e.g. LazyColumn) never enters dragging state
                // while the user is touching the board. Without this, the LazyColumn's
                // DragGestureNode calls positionOnScreen on a detached coordinator → SIGSEGV.
                // ChessBoardWithCoordinates uses requireUnconsumed=false so it still fires.
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        key(boardState) {
            ChessBoardWithCoordinates(
                gameController = gameController,
                modifier = boardModifier.fillMaxSize(),
            )
        }
    }
}
