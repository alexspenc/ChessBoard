package com.example.chessboard.ui.screen.home

/**
 * File role: renders the regular full home screen outside SimpleView.
 * Allowed here:
 * - regular home layout and only the UI pieces used by that layout
 * Not allowed here:
 * - SimpleView-specific layout
 * - home container loading logic
 * Validation date: 2026-05-03
 */
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.components.ScreenTitleText
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal

@Composable
internal fun RegularHomeScreen(
    onNavigate: (ScreenType) -> Unit = {},
    onCreateOpeningClick: () -> Unit = { onNavigate(ScreenType.CreateOpening) },
    onCreateTrainingClick: () -> Unit = {},
    onOpenPositionEditorClick: () -> Unit = {},
    onOpenSavedPositionsClick: () -> Unit = {},
    onOpenBackupClick: () -> Unit = {},
    onExitClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            HomeBottomNavigation(onItemSelected = onNavigate)
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = AppDimens.spaceLg,
                top = 20.dp,
                end = AppDimens.spaceLg,
                bottom = AppDimens.spaceLg,
            ),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("♛", fontSize = 26.sp, color = TrainingAccentTeal)
                            Spacer(modifier = Modifier.width(AppDimens.spaceSm))
                            Text(
                                text = "Chess Openings",
                                style = MaterialTheme.typography.displaySmall,
                                color = TextColor.Primary,
                            )
                        }
                        BodySecondaryText(
                            text = "Home",
                            color = TextColor.Secondary,
                        )
                    }
                    AddOpeningButton(
                        onClick = onCreateOpeningClick,
                    )
                }
            }

            item {
                ScreenSection {
                    BodySecondaryText(
                        text = "Choose what you want to do.",
                        color = TextColor.Secondary,
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
                ) {
                    HomeActionCard(
                        title = "Trainings",
                        subtitle = "Open saved training plans",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(ScreenType.Training) },
                    )
                    HomeActionCard(
                        title = "Games",
                        subtitle = "Browse saved openings",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(ScreenType.GamesExplorer) },
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
                ) {
                    HomeActionCard(
                        title = "Create Training",
                        subtitle = "Build a training from saved games",
                        modifier = Modifier.weight(1f),
                        onClick = onCreateTrainingClick,
                    )
                    HomeActionCard(
                        title = "Templates",
                        subtitle = "Browse and edit training templates",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(ScreenType.TrainingTemplates) },
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
                ) {
                    HomeActionCard(
                        title = "Create Opening",
                        subtitle = "Save a new opening line",
                        modifier = Modifier.weight(1f),
                        onClick = onCreateOpeningClick,
                    )
                    HomeActionCard(
                        title = "Position Editor",
                        subtitle = "Set up a custom board position",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenPositionEditorClick,
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
                ) {
                    HomeActionCard(
                        title = "Saved Positions",
                        subtitle = "Open saved board positions",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenSavedPositionsClick,
                    )
                    HomeActionCard(
                        title = "Backup Games",
                        subtitle = "Export all games to a PGN file",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenBackupClick,
                    )
                }
            }

            item {
                HomeActionCard(
                    title = "Exit",
                    subtitle = "Close the application",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onExitClick,
                )
            }
        }
    }
}

@Composable
private fun HomeActionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CardSurface(
        modifier = modifier,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp),
            contentAlignment = Alignment.TopStart,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
            ) {
                ScreenTitleText(text = title)
                CardMetaText(text = subtitle)
            }
        }
    }
}
