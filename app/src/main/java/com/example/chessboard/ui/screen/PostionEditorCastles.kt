package com.example.chessboard.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.chessboard.ui.PositionEditorBlackLongCastleTestTag
import com.example.chessboard.ui.PositionEditorBlackShortCastleTestTag
import com.example.chessboard.ui.PositionEditorWhiteLongCastleTestTag
import com.example.chessboard.ui.PositionEditorWhiteShortCastleTestTag
import com.example.chessboard.ui.components.AppIconSizes
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.resolvePieceGlyph
import com.example.chessboard.ui.resolvePieceTint
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TextColor

internal data class PositionEditorCastlingState(
    val whiteShortCastleAvailable: Boolean = false,
    val whiteLongCastleAvailable: Boolean = false,
    val blackShortCastleAvailable: Boolean = false,
    val blackLongCastleAvailable: Boolean = false
)

internal fun resolvePositionEditorCastlingState(
    fen: String
): PositionEditorCastlingState {
    val castlingPart = fen.trim()
        .split(Regex("\\s+"))
        .getOrNull(2)
        .orEmpty()

    return PositionEditorCastlingState(
        whiteShortCastleAvailable = 'K' in castlingPart,
        whiteLongCastleAvailable = 'Q' in castlingPart,
        blackShortCastleAvailable = 'k' in castlingPart,
        blackLongCastleAvailable = 'q' in castlingPart
    )
}

internal fun replacePositionEditorFenCastlingPart(
    fen: String,
    castlingState: PositionEditorCastlingState
): String {
    val fenParts = fen.trim().split(Regex("\\s+"))
    if (fenParts.isEmpty()) {
        return fen
    }

    val normalizedFenParts = fenParts.take(4).toMutableList()
    while (normalizedFenParts.size < 4) {
        normalizedFenParts += if (normalizedFenParts.size == 1) "w" else "-"
    }
    normalizedFenParts[2] = castlingState.toFenToken()
    return normalizedFenParts.joinToString(separator = " ")
}

@Composable
internal fun PositionEditorCastlesSection(
    castlingState: PositionEditorCastlingState,
    onCastlingStateChange: (PositionEditorCastlingState) -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenSection(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SectionTitleText(text = "Castling", color = TextColor.Secondary)
            Spacer(modifier = Modifier.height(AppDimens.spaceMd))

            PositionEditorCastlingRow(
                kingLetter = 'K',
                shortCastleEnabled = castlingState.whiteShortCastleAvailable,
                longCastleEnabled = castlingState.whiteLongCastleAvailable,
                onShortCastleChange = { isEnabled ->
                    onCastlingStateChange(
                        castlingState.copy(whiteShortCastleAvailable = isEnabled)
                    )
                },
                onLongCastleChange = { isEnabled ->
                    onCastlingStateChange(
                        castlingState.copy(whiteLongCastleAvailable = isEnabled)
                    )
                },
                shortCastleTestTag = PositionEditorWhiteShortCastleTestTag,
                longCastleTestTag = PositionEditorWhiteLongCastleTestTag
            )

            Spacer(modifier = Modifier.height(AppDimens.spaceSm))

            PositionEditorCastlingRow(
                kingLetter = 'k',
                shortCastleEnabled = castlingState.blackShortCastleAvailable,
                longCastleEnabled = castlingState.blackLongCastleAvailable,
                onShortCastleChange = { isEnabled ->
                    onCastlingStateChange(
                        castlingState.copy(blackShortCastleAvailable = isEnabled)
                    )
                },
                onLongCastleChange = { isEnabled ->
                    onCastlingStateChange(
                        castlingState.copy(blackLongCastleAvailable = isEnabled)
                    )
                },
                shortCastleTestTag = PositionEditorBlackShortCastleTestTag,
                longCastleTestTag = PositionEditorBlackLongCastleTestTag
            )
        }
    }
}

@Composable
private fun PositionEditorCastlingRow(
    kingLetter: Char,
    shortCastleEnabled: Boolean,
    longCastleEnabled: Boolean,
    onShortCastleChange: (Boolean) -> Unit,
    onLongCastleChange: (Boolean) -> Unit,
    shortCastleTestTag: String,
    longCastleTestTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
    ) {
        Text(
            text = resolvePieceGlyph(kingLetter) ?: kingLetter.toString(),
            color = resolvePieceTint(kingLetter),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.size(AppIconSizes.Lg)
        )

        PositionEditorCastleCheckbox(
            label = "0-0",
            checked = shortCastleEnabled,
            onCheckedChange = onShortCastleChange,
            testTag = shortCastleTestTag,
            modifier = Modifier.weight(1f)
        )

        PositionEditorCastleCheckbox(
            label = "0-0-0",
            checked = longCastleEnabled,
            onCheckedChange = onLongCastleChange,
            testTag = longCastleTestTag,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PositionEditorCastleCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = TextColor.Primary,
            style = MaterialTheme.typography.bodyLarge
        )
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

private fun PositionEditorCastlingState.toFenToken(): String {
    return buildString {
        if (whiteShortCastleAvailable) {
            append('K')
        }
        if (whiteLongCastleAvailable) {
            append('Q')
        }
        if (blackShortCastleAvailable) {
            append('k')
        }
        if (blackLongCastleAvailable) {
            append('q')
        }
    }.ifBlank { "-" }
}
