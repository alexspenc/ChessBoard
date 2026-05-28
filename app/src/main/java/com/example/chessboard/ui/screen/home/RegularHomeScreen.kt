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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.chessboard.R
import com.example.chessboard.ui.components.AppIconSizes
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
    onOpenTrainingsClick: () -> Unit = { onNavigate(ScreenType.Training) },
    onOpenPositionSearchClick: () -> Unit = {},
    onOpenSavedPositionsClick: () -> Unit = {},
    onOpenBackupClick: () -> Unit = {},
    onExitClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    fun navigateFromHome(screen: ScreenType) {
        if (screen == ScreenType.Training) {
            onOpenTrainingsClick()
            return
        }

        onNavigate(screen)
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            HomeBottomNavigation(onItemSelected = ::navigateFromHome)
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
                            Icon(
                                painter = painterResource(R.drawable.ic_launcher_foreground),
                                contentDescription = null,
                                tint = TrainingAccentTeal,
                                modifier = Modifier.size(AppIconSizes.Xl),
                            )
                            Spacer(modifier = Modifier.width(AppDimens.spaceSm))
                            Text(
                                text = stringResource(R.string.home_title),
                                style = MaterialTheme.typography.displaySmall,
                                color = TextColor.Primary,
                            )
                        }
                        BodySecondaryText(
                            text = stringResource(R.string.home_subtitle),
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
                        text = stringResource(R.string.home_regular_prompt),
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
                        title = stringResource(R.string.home_trainings_title),
                        subtitle = stringResource(R.string.home_trainings_subtitle),
                        modifier = Modifier.weight(1f),
                        onClick = onOpenTrainingsClick,
                    )
                    HomeActionCard(
                        title = stringResource(R.string.home_lines_title),
                        subtitle = stringResource(R.string.home_lines_subtitle),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(ScreenType.LinesExplorer) },
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
                ) {
                    HomeActionCard(
                        title = stringResource(R.string.home_create_training_title),
                        subtitle = stringResource(R.string.home_create_training_subtitle),
                        modifier = Modifier.weight(1f),
                        onClick = onCreateTrainingClick,
                    )
                    HomeActionCard(
                        title = stringResource(R.string.home_templates_title),
                        subtitle = stringResource(R.string.home_templates_subtitle),
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
                        title = stringResource(R.string.home_create_opening_title),
                        subtitle = stringResource(R.string.home_create_opening_subtitle),
                        modifier = Modifier.weight(1f),
                        onClick = onCreateOpeningClick,
                    )
                    HomeActionCard(
                        title = stringResource(R.string.home_position_search_title),
                        subtitle = stringResource(R.string.home_position_search_subtitle),
                        modifier = Modifier.weight(1f),
                        onClick = onOpenPositionSearchClick,
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
                ) {
                    HomeActionCard(
                        title = stringResource(R.string.home_saved_positions_title),
                        subtitle = stringResource(R.string.home_saved_positions_subtitle),
                        modifier = Modifier.weight(1f),
                        onClick = onOpenSavedPositionsClick,
                    )
                    HomeActionCard(
                        title = stringResource(R.string.home_backup_lines_title),
                        subtitle = stringResource(R.string.home_backup_lines_subtitle),
                        modifier = Modifier.weight(1f),
                        onClick = onOpenBackupClick,
                    )
                }
            }

            item {
                HomeActionCard(
                    title = stringResource(R.string.home_exit_title),
                    subtitle = stringResource(R.string.home_exit_subtitle),
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
