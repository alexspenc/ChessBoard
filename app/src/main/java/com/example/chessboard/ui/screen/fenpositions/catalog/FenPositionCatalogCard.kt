package com.example.chessboard.ui.screen.fenpositions.catalog

/*
 * File role: renders one read-only position card in the FEN catalog.
 * Allowed here:
 * - position title and theme presentation
 * - local board-controller setup for a four-field position FEN
 * Not allowed here:
 * - catalog loading, pagination, navigation routing, or persistence operations
 * Validation date: 2026-08-30
 */

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.ChessBoardSection
import com.example.chessboard.ui.components.ScreenTitleText
import com.example.chessboard.ui.theme.AppDimens

@Composable
internal fun FenPositionCatalogCard(
    position: FenPositionCatalogItem,
    themeText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lineController = remember(position.id, position.fen) {
        LineController().also { controller ->
            controller.loadPreviewFen(toLoadableFenPosition(position.fen))
            controller.setOrientation(resolveFenPositionBoardOrientation(position.fen))
            controller.setUserMovesEnabled(false)
        }
    }

    CardSurface(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(
                start = AppDimens.spaceLg,
                top = AppDimens.spaceLg,
                end = AppDimens.spaceLg,
            ),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
        ) {
            ScreenTitleText(text = position.name)
            CardMetaText(text = themeText)
        }
        Spacer(modifier = Modifier.height(AppDimens.spaceMd))
        ChessBoardSection(lineController = lineController)
    }
}

internal fun resolveFenPositionBoardOrientation(fen: String): BoardOrientation {
    val sideToMove = fen.trim().split(Regex("\\s+")).getOrNull(1)
    if (sideToMove == "b") {
        return BoardOrientation.BLACK
    }

    return BoardOrientation.WHITE
}

internal fun toLoadableFenPosition(fen: String): String {
    return "${fen.trim()} 0 1"
}
