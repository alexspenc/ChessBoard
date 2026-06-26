@file:Suppress("FunctionName")

package com.example.chessboard.ui.screen.gameOpeningAnalysis

/*
 * File role: renders the game-opening analysis screen entry point and initial placeholder state.
 * Allowed here:
 * - screen-level UI for imported game opening analysis
 * - placeholder content used before import, filter, and result flows are implemented
 * Not allowed here:
 * - PGN parsing, database access, analyzer orchestration, or reusable generic components
 * Validation date: 2026-06-26
 */

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R
import com.example.chessboard.ui.GameOpeningAnalysisContentTestTag
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TextColor

@Composable
fun GameOpeningAnalysisScreenContainer(
    screenContext: ScreenContainerContext,
    modifier: Modifier = Modifier,
) {
    GameOpeningAnalysisScreen(
        onBackClick = screenContext.onBackClick,
        modifier = modifier,
    )
}

@Composable
internal fun GameOpeningAnalysisScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.game_opening_analysis_title),
                onBackClick = onBackClick,
                handleSystemBack = true,
                filledBackButton = true,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .testTag(GameOpeningAnalysisContentTestTag),
            contentPadding = PaddingValues(AppDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
        ) {
            item {
                Box(
                    modifier = Modifier.fillParentMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    BodySecondaryText(
                        text = stringResource(R.string.game_opening_analysis_placeholder),
                        color = TextColor.Secondary,
                    )
                }
            }
        }
    }
}
