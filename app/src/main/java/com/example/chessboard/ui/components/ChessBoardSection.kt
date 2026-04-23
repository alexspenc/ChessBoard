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
    ) {
        key(boardState) {
            ChessBoardWithCoordinates(
                gameController = gameController,
                modifier = boardModifier.fillMaxSize(),
            )
        }
    }
}
