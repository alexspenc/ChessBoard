package com.example.chessboard.ui.screen.openingDeviation

/**
 * Displays one opening deviation position and its unique next-position branches.
 *
 * Keep screen layout, empty state, and branch-card rendering here.
 * Do not add navigation routing state, database queries, or deviation-building logic to this file.
 */
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.chessboard.ui.OpeningDeviationDisplayContentTestTag
import com.example.chessboard.ui.OpeningDeviationEmptyStateTestTag
import com.example.chessboard.ui.OpeningDeviationSourceBoardTestTag
import com.example.chessboard.ui.OpeningDeviationSourceBoardCardTestTag
import com.example.chessboard.ui.openingDeviationBranchBoardTestTag
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.openingDeviationBranchCardTestTag
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TextColor

@Composable
fun OpeningDeviationDisplayScreen(
    deviationItem: OpeningDeviationItem,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
) {
    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = "Opening Deviations",
                subtitle = "Branches: ${deviationItem.branches.size}",
                onBackClick = onBackClick,
                filledBackButton = true,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag(OpeningDeviationDisplayContentTestTag),
            contentPadding = PaddingValues(
                horizontal = AppDimens.spaceLg,
                vertical = AppDimens.spaceLg,
            ),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
        ) {
            item {
                OpeningDeviationBoardCard(
                    title = "Deviation Position",
                    fen = deviationItem.positionFen,
                    subtitle = resolveDeviationSideToMoveLabel(deviationItem.positionFen),
                    modifier = Modifier.testTag(OpeningDeviationSourceBoardCardTestTag),
                    boardTestTag = OpeningDeviationSourceBoardTestTag,
                )
            }

            if (deviationItem.branches.isEmpty()) {
                item {
                    OpeningDeviationEmptyState()
                }
                return@LazyColumn
            }

            itemsIndexed(deviationItem.branches) { index, branch ->
                OpeningDeviationBoardCard(
                    title = "Branch ${index + 1}",
                    fen = branch.resultFen,
                    subtitle = "Move: ${branch.moveUci}",
                    metaText = "Games: ${branch.gamesCount}",
                    modifier = Modifier.testTag(openingDeviationBranchCardTestTag(index)),
                    boardTestTag = openingDeviationBranchBoardTestTag(index),
                )
            }
        }
    }
}

@Composable
private fun OpeningDeviationEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .testTag(OpeningDeviationEmptyStateTestTag),
        contentAlignment = Alignment.Center,
    ) {
        BodySecondaryText(
            text = "No deviation branches available.",
            color = TextColor.Secondary,
            textAlign = TextAlign.Center,
        )
    }
}

private fun resolveDeviationSideToMoveLabel(fen: String): String {
    if (fen.trim().split(Regex("\\s+")).getOrNull(1) == "b") {
        return "Black to move"
    }

    return "White to move"
}
