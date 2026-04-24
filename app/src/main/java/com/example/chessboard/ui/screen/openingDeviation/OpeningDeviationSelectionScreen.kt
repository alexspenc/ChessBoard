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
import androidx.compose.material3.Icon
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
import com.example.chessboard.ui.components.ScreenTitleText
import com.example.chessboard.ui.openingDeviationSelectionCardTestTag
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingIconInactive

@Composable
fun OpeningDeviationSelectionScreenContainer(
    deviationItems: List<OpeningDeviationItem>,
    selectedDeviationIndex: Int?,
    onDeviationSelected: (Int) -> Unit,
    onStartClick: (OpeningDeviationItem) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OpeningDeviationSelectionScreen(
        deviationItems = deviationItems,
        selectedDeviationIndex = selectedDeviationIndex,
        onDeviationSelected = onDeviationSelected,
        onStartClick = onStartClick,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@Composable
internal fun OpeningDeviationSelectionScreen(
    deviationItems: List<OpeningDeviationItem>,
    selectedDeviationIndex: Int?,
    onDeviationSelected: (Int) -> Unit,
    onStartClick: (OpeningDeviationItem) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedDeviationItem = deviationItems.getOrNull(selectedDeviationIndex ?: -1)

    fun resolveStartTint(): Color {
        if (selectedDeviationItem == null) {
            return TrainingIconInactive
        }

        return TrainingAccentTeal
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = "Deviation Positions",
                subtitle = "Positions: ${deviationItems.size}",
                onBackClick = onBackClick,
                filledBackButton = true,
                actions = {
                    IconButton(
                        onClick = {
                            val deviationItem = selectedDeviationItem ?: return@IconButton
                            onStartClick(deviationItem)
                        },
                        enabled = selectedDeviationItem != null,
                        modifier = Modifier.testTag(OpeningDeviationSelectionStartTestTag),
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start deviation display",
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
                    OpeningDeviationSelectionEmptyState()
                }
                return@LazyColumn
            }

            itemsIndexed(deviationItems) { index, deviationItem ->
                if (index == selectedDeviationIndex) {
                    OpeningDeviationBoardCard(
                        title = "Selected Deviation Position",
                        fen = deviationItem.positionFen,
                        subtitle = resolveDeviationSelectionSideToMoveLabel(deviationItem.positionFen),
                        metaText = "Branches: ${deviationItem.branches.size}",
                        modifier = Modifier.testTag(OpeningDeviationSelectionPreviewBoardCardTestTag),
                        boardTestTag = OpeningDeviationSelectionPreviewBoardTestTag,
                    )
                }

                OpeningDeviationSelectionCard(
                    index = index,
                    deviationItem = deviationItem,
                    isSelected = index == selectedDeviationIndex,
                    onClick = { onDeviationSelected(index) },
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
        ScreenTitleText(text = resolveDeviationSelectionTitle(index))
        if (isSelected) {
            CardMetaText(
                text = "Selected",
                color = TrainingAccentTeal,
            )
        }
        CardMetaText(text = "Branches: ${deviationItem.branches.size}")
        CardMetaText(text = "FEN: ${deviationItem.positionFen}")
    }
}

@Composable
private fun OpeningDeviationSelectionEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .testTag(OpeningDeviationSelectionEmptyStateTestTag),
        contentAlignment = Alignment.Center,
    ) {
        BodySecondaryText(
            text = "No deviation positions available.",
            color = TextColor.Secondary,
            textAlign = TextAlign.Center,
        )
    }
}

private fun resolveDeviationSelectionTitle(index: Int): String {
    return "Deviation Position ${index + 1}"
}

private fun resolveDeviationSelectionSideToMoveLabel(fen: String): String {
    if (fen.trim().split(Regex("\\s+")).getOrNull(1) == "b") {
        return "Black to move"
    }

    return "White to move"
}
