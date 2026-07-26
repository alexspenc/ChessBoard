package com.example.chessboard.ui.components

/**
 * Shared board section wrappers for screens that need a standard chess board.
 *
 * Keep generic interactive and replay board framing and sizing here. Do not add
 * screen-specific controls, training workflow logic, or persistence behavior to this file.
 */
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.ui.ChessBoardWithCoordinates
import com.example.chessboard.ui.boardanimation.AnimatedBoardSceneHost
import com.example.chessboard.ui.boardanimation.AnimatedInteractiveChessBoard
import com.example.chessboard.ui.boardanimation.BoardAnimationQueueController
import com.example.chessboard.ui.theme.AppDimens

private const val ChessBoardCellCount = 8

@Composable
fun ChessBoardSection(
    lineController: LineController,
    modifier: Modifier = Modifier,
    boardModifier: Modifier = Modifier,
) {
    ChessBoardSectionFrame(modifier = modifier) {
        ChessBoardWithCoordinates(
            lineController = lineController,
            modifier = boardModifier.fillMaxSize(),
        )
    }
}

@Composable
internal fun AnimatedChessBoardSection(
    lineController: LineController,
    boardAnimationController: BoardAnimationQueueController,
    interactionEnabled: Boolean,
    modifier: Modifier = Modifier,
    boardModifier: Modifier = Modifier,
) {
    ChessBoardSectionFrame(modifier = modifier) {
        AnimatedInteractiveChessBoard(
            lineController = lineController,
            boardAnimationController = boardAnimationController,
            interactionEnabled = interactionEnabled,
            modifier = boardModifier.fillMaxSize(),
        )
    }
}

@Composable
internal fun AnimatedReplayChessBoardSection(
    boardAnimationController: BoardAnimationQueueController,
    modifier: Modifier = Modifier,
    boardModifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(AppDimens.radiusXl))
    ) {
        BoxWithConstraints(modifier = boardModifier.fillMaxSize()) {
            val squareSizePx = constraints.maxWidth / ChessBoardCellCount.toFloat()
            AnimatedBoardSceneHost(
                controller = boardAnimationController,
                squareSizePx = squareSizePx,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ChessBoardSectionFrame(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    // Pre-consume user-input scroll so a parent scrolling container does not
    // take ownership of a gesture that started on the board.
    val noScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                if (source == NestedScrollSource.UserInput) available else Offset.Zero
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(AppDimens.radiusXl))
            .nestedScroll(noScroll)
    ) {
        content()
    }
}
