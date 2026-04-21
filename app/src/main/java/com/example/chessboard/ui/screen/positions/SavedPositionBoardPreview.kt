package com.example.chessboard.ui.screen.positions

/**
 * Board preview rendering for the selected saved position.
 *
 * Keep read-only selected-position board display here. Do not add persistence or list state orchestration.
 */
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.screen.training.ChessBoardSection
import com.example.chessboard.ui.theme.AppDimens

@Composable
internal fun SavedPositionBoardPreview(
    position: SavedPositionListItem,
    gameController: GameController,
    modifier: Modifier = Modifier,
) {
    CardSurface(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)) {
            SectionTitleText(text = "Position Preview")
            BodySecondaryText(text = resolveSavedPositionSideToMoveLabel(position))
            Spacer(modifier = Modifier.height(AppDimens.spaceXs))
            ChessBoardSection(gameController = gameController)
        }
    }
}

internal fun resolveSavedPositionBoardOrientation(
    position: SavedPositionListItem
): BoardOrientation {
    if (resolveSavedPositionSideToMove(position) == "b") {
        return BoardOrientation.BLACK
    }

    return BoardOrientation.WHITE
}

private fun resolveSavedPositionSideToMoveLabel(position: SavedPositionListItem): String {
    if (resolveSavedPositionSideToMove(position) == "b") {
        return "Black to move"
    }

    return "White to move"
}

private fun resolveSavedPositionSideToMove(position: SavedPositionListItem): String {
    return resolveDisplayedFen(position).trim().split(Regex("\\s+")).getOrNull(1) ?: "w"
}
