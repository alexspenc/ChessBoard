package com.example.chessboard.ui.screen.openingDeviation

/**
 * Lets the user choose one deviation start position before opening the final display screen.
 * Keep selection-screen layout, preview rendering, and top-bar launch action here.
 * Do not add runtime-context wiring, database lookups, or Saved Positions integration to this file.
 */
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.chessboard.ui.OpeningDeviationSelectionContentTestTag
import com.example.chessboard.ui.OpeningDeviationSelectionEmptyStateTestTag
import com.example.chessboard.ui.OpeningDeviationSelectionPreviewBoardCardTestTag
import com.example.chessboard.ui.OpeningDeviationSelectionPreviewBoardTestTag
import com.example.chessboard.ui.OpeningDeviationSelectionStartTestTag
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.HomeIconButton
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.components.ScreenTitleText
import com.example.chessboard.ui.openingDeviationSelectionCardTestTag
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.MutedContentColor

@Composable
fun OpeningDeviationSelectionScreenContainer(
    deviationItems: List<OpeningDeviationItem>,
    selectedDeviationIndex: Int?,
    onDeviationSelected: (Int) -> Unit,
    onStartClick: (Int) -> Unit,
    onHomeClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OpeningDeviationSelectionScreen(
        deviationItems = deviationItems,
        selectedDeviationIndex = selectedDeviationIndex,
        onDeviationSelected = onDeviationSelected,
        onStartClick = onStartClick,
        onHomeClick = onHomeClick,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@Composable
internal fun OpeningDeviationSelectionScreen(
    deviationItems: List<OpeningDeviationItem>,
    selectedDeviationIndex: Int?,
    onDeviationSelected: (Int) -> Unit,
    onStartClick: (Int) -> Unit,
    onHomeClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = openingDeviationStrings()
    val selectedDeviationItem = deviationItems.getOrNull(selectedDeviationIndex ?: -1)

    fun resolveStartTint(): Color {
        if (selectedDeviationItem == null) {
            return MutedContentColor
        }

        return TrainingAccentTeal
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = strings.selectionTitle,
                subtitleLines = listOf(strings.positions(deviationItems.size)),
                onBackClick = onBackClick,
                handleSystemBack = true,
                filledBackButton = true,
                actions = {
                    HomeIconButton(onClick = onHomeClick)
                    IconButton(
                        onClick = {
                            val currentSelectedDeviationIndex = selectedDeviationIndex
                                ?: return@IconButton
                            onStartClick(currentSelectedDeviationIndex)
                        },
                        enabled = selectedDeviationItem != null,
                        modifier = Modifier.testTag(OpeningDeviationSelectionStartTestTag),
                    ) {
                        IconMd(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = strings.startDisplayContentDescription,
                            tint = resolveStartTint(),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag(OpeningDeviationSelectionContentTestTag),
            contentPadding = PaddingValues(
                horizontal = AppDimens.spaceLg,
                vertical = AppDimens.spaceLg,
            ),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
        ) {
            if (deviationItems.isEmpty()) {
                item {
                    OpeningDeviationSelectionEmptyState(strings)
                }
                return@LazyColumn
            }

            itemsIndexed(deviationItems) { index, deviationItem ->
                if (index == selectedDeviationIndex) {
                    OpeningDeviationBoardCard(
                        title = strings.selectedDeviationPositionTitle,
                        fen = deviationItem.positionFen,
                        subtitle = resolveDeviationSelectionSideToMoveLabel(
                            deviationItem.positionFen,
                            strings,
                        ),
                        metaText = strings.branches(deviationItem.branches.size),
                        modifier = Modifier.testTag(OpeningDeviationSelectionPreviewBoardCardTestTag),
                        boardTestTag = OpeningDeviationSelectionPreviewBoardTestTag,
                    )
                }

                OpeningDeviationSelectionCard(
                    index = index,
                    deviationItem = deviationItem,
                    isSelected = index == selectedDeviationIndex,
                    onClick = { onDeviationSelected(index) },
                    strings = strings,
                )

                if (index != deviationItems.lastIndex) {
                    Spacer(modifier = Modifier.height(AppDimens.spaceXs))
                }
            }
        }
    }
}

@Composable
private fun OpeningDeviationSelectionCard(
    index: Int,
    deviationItem: OpeningDeviationItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    strings: OpeningDeviationStrings,
) {
    CardSurface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(openingDeviationSelectionCardTestTag(index))
            .semantics { selected = isSelected },
        color = if (isSelected) Background.CardDark else Background.SurfaceDark,
        border = if (isSelected) BorderStroke(1.dp, TrainingAccentTeal) else null,
        onClick = onClick,
    ) {
        ScreenTitleText(text = strings.deviationPosition(index))
        if (isSelected) {
            CardMetaText(
                text = strings.selectedLabel,
                color = TrainingAccentTeal,
            )
        }
        CardMetaText(text = strings.branches(deviationItem.branches.size))
        CardMetaText(text = strings.fen(deviationItem.positionFen))
    }
}

@Composable
private fun OpeningDeviationSelectionEmptyState(strings: OpeningDeviationStrings) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .testTag(OpeningDeviationSelectionEmptyStateTestTag),
        contentAlignment = Alignment.Center,
    ) {
        BodySecondaryText(
            text = strings.selectionEmptyState,
            color = TextColor.Secondary,
            textAlign = TextAlign.Center,
        )
    }
}

private fun resolveDeviationSelectionSideToMoveLabel(
    fen: String,
    strings: OpeningDeviationStrings,
): String {
    if (fen.trim().split(Regex("\\s+")).getOrNull(1) == "b") {
        return strings.blackToMove
    }

    return strings.whiteToMove
}
