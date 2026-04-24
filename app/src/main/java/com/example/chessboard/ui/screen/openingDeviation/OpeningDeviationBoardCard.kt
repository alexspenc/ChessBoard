package com.example.chessboard.ui.screen.openingDeviation

/**
 * Renders a read-only chess board card for opening deviation screens.
 *
 * Keep board presentation, labels, and local FEN loading helpers here.
 * Do not add screen-level navigation, list orchestration, or persistence logic to this file.
 */
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.ChessBoardSection
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TrainingAccentTeal

@Composable
internal fun OpeningDeviationBoardCard(
    title: String,
    fen: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    metaText: String? = null,
    boardTestTag: String? = null,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val gameController = remember { GameController() }

    LaunchedEffect(fen) {
        gameController.loadPreviewFen(toLoadableDeviationFen(fen))
        gameController.setOrientation(resolveDeviationBoardOrientation(fen))
        gameController.setUserMovesEnabled(false)
    }

    CardSurface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                if (onClick != null) {
                    selected = isSelected
                }
            },
        color = resolveDeviationBoardCardColor(
            isSelected = isSelected,
            onClick = onClick,
        ),
        border = if (isSelected) BorderStroke(1.dp, TrainingAccentTeal) else null,
        onClick = onClick,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)) {
            SectionTitleText(text = title)
            if (!subtitle.isNullOrBlank()) {
                BodySecondaryText(text = subtitle)
            }
            if (!metaText.isNullOrBlank()) {
                CardMetaText(text = metaText)
            }
            Spacer(modifier = Modifier.height(AppDimens.spaceXs))
            ChessBoardSection(
                gameController = gameController,
                boardModifier = resolveDeviationBoardModifier(boardTestTag),
            )
        }
    }
}

private fun resolveDeviationBoardCardColor(
    isSelected: Boolean,
    onClick: (() -> Unit)?,
) = when {
    isSelected -> Background.CardDark
    onClick != null -> Background.SurfaceDark
    else -> Background.CardDark
}

private fun resolveDeviationBoardModifier(boardTestTag: String?): Modifier {
    if (boardTestTag.isNullOrBlank()) {
        return Modifier
    }

    return Modifier.testTag(boardTestTag)
}

private fun resolveDeviationBoardOrientation(fen: String): BoardOrientation {
    if (resolveDeviationSideToMove(fen) == "b") {
        return BoardOrientation.BLACK
    }

    return BoardOrientation.WHITE
}

private fun resolveDeviationSideToMove(fen: String): String {
    return fen.trim().split(Regex("\\s+")).getOrNull(1) ?: "w"
}

private fun toLoadableDeviationFen(fen: String): String {
    val normalizedFen = fen.trim()
    if (normalizedFen.isBlank()) {
        return "8/8/8/8/8/8/8/8 w - - 0 1"
    }

    val fenParts = normalizedFen.split(Regex("\\s+"))

    if (fenParts.size >= 6) {
        return normalizedFen
    }

    if (fenParts.size == 5) {
        return "$normalizedFen 1"
    }

    if (fenParts.size == 4) {
        return "$normalizedFen 0 1"
    }

    if (fenParts.size == 3) {
        return "$normalizedFen - 0 1"
    }

    if (fenParts.size == 2) {
        return "$normalizedFen - - 0 1"
    }

    return "$normalizedFen w - - 0 1"
}
