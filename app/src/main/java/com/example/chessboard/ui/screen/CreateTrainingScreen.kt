package com.example.chessboard.ui.screen

import android.app.Activity
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppTextField
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TrainingBackgroundDark
import com.example.chessboard.ui.theme.TrainingTextPrimary
import com.example.chessboard.ui.theme.TrainingTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DEFAULT_TRAINING_NAME = "FullTraining"
private val TrainingGameRowHeight = 92.dp
private val TrainingGamesHeaderHeight = 88.dp
private val TrainingGamesNavigationHeight = 64.dp

data class TrainingGameEditorItem(
    val gameId: Long,
    val title: String,
    val weight: Int = 1
)

@Composable
fun CreateTrainingScreenContainer(
    activity: Activity,
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    modifier: Modifier = Modifier,
    inDbProvider: DatabaseProvider,
) {
    var gamesForTraining by remember { mutableStateOf<List<TrainingGameEditorItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        gamesForTraining = withContext(Dispatchers.IO) {
            inDbProvider.getAllGames().map { game ->
                game.toTrainingGameEditorItem()
            }
        }
    }

    CreateTrainingScreen(
        gamesForTraining = gamesForTraining,
        onBackClick = onBackClick,
        onNavigate = onNavigate,
        modifier = modifier
    )
}

@Composable
fun CreateTrainingScreen(
    gamesForTraining: List<TrainingGameEditorItem> = emptyList(),
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedNavItem by remember { mutableStateOf<ScreenType>(ScreenType.Home) }
    var trainingName by remember { mutableStateOf(DEFAULT_TRAINING_NAME) }
    var currentPage by remember { mutableStateOf(0) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = TrainingBackgroundDark,
        topBar = {
            AppTopBar(
                title = "Create Training",
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            CreateTrainingBottomNavigation(
                selectedItem = selectedNavItem,
                onItemSelected = {
                    selectedNavItem = it
                    onNavigate(it)
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            ScreenSection {
                AppTextField(
                    value = trainingName,
                    onValueChange = { trainingName = it },
                    label = "Training Name",
                    placeholder = DEFAULT_TRAINING_NAME
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.spaceLg))

            ScreenSection {
                BodySecondaryText(
                    text = "Games loaded for training: ${gamesForTraining.size}",
                    color = TrainingTextSecondary
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val availableHeightForRows =
                    (maxHeight - TrainingGamesHeaderHeight - TrainingGamesNavigationHeight)
                        .coerceAtLeast(TrainingGameRowHeight)
                val pageSize = (availableHeightForRows / TrainingGameRowHeight)
                    .toInt()
                    .coerceAtLeast(1)
                val totalPages = ((gamesForTraining.size + pageSize - 1) / pageSize).coerceAtLeast(1)
                val safeCurrentPage = currentPage.coerceIn(0, totalPages - 1)
                val currentPageItems = gamesForTraining
                    .drop(safeCurrentPage * pageSize)
                    .take(pageSize)

                ScreenSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppDimens.spaceLg)
                ) {
                    TrainingGamesPage(
                        games = currentPageItems,
                        currentPage = safeCurrentPage,
                        totalPages = totalPages,
                        onPreviousPageClick = {
                            if (safeCurrentPage > 0) {
                                currentPage = safeCurrentPage - 1
                            }
                        },
                        onNextPageClick = {
                            if (safeCurrentPage + 1 < totalPages) {
                                currentPage = safeCurrentPage + 1
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun GameEntity.toTrainingGameEditorItem(): TrainingGameEditorItem {
    return TrainingGameEditorItem(
        gameId = id,
        title = event ?: "Unnamed Opening",
        weight = 1
    )
}

@Composable
private fun TrainingGamesPage(
    games: List<TrainingGameEditorItem>,
    currentPage: Int,
    totalPages: Int,
    onPreviousPageClick: () -> Unit,
    onNextPageClick: () -> Unit
) {
    CardSurface(modifier = Modifier.fillMaxWidth()) {
        SectionTitleText(
            text = "Games in Training",
            color = TrainingTextPrimary
        )

        Spacer(modifier = Modifier.height(AppDimens.spaceSm))

        CardMetaText(
            text = "Page ${currentPage + 1} of $totalPages",
            color = TrainingTextSecondary
        )

        Spacer(modifier = Modifier.height(AppDimens.spaceLg))

        if (games.isEmpty()) {
            BodySecondaryText(
                text = "No games available.",
                color = TrainingTextSecondary
            )
        } else {
            games.forEach { game ->
                TrainingGamePageRow(game = game)
                Spacer(modifier = Modifier.height(AppDimens.spaceMd))
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.spaceSm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
        ) {
            PrimaryButton(
                text = "Previous",
                onClick = onPreviousPageClick,
                enabled = currentPage > 0,
                modifier = Modifier.weight(1f)
            )
            PrimaryButton(
                text = "Next",
                onClick = onNextPageClick,
                enabled = currentPage + 1 < totalPages,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TrainingGamePageRow(
    game: TrainingGameEditorItem
) {
    CardSurface(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(AppDimens.spaceMd)
    ) {
        SectionTitleText(
            text = game.title,
            color = TrainingTextPrimary
        )
        Spacer(modifier = Modifier.height(AppDimens.spaceXs))
        CardMetaText(
            text = "ID: ${game.gameId}",
            color = TrainingTextSecondary
        )
        CardMetaText(
            text = "Weight: ${game.weight}",
            color = TrainingTextSecondary
        )
    }
}

@Composable
private fun CreateTrainingBottomNavigation(
    selectedItem: ScreenType,
    onItemSelected: (ScreenType) -> Unit,
    modifier: Modifier = Modifier
) {
    AppBottomNavigation(
        items = defaultAppBottomNavigationItems(),
        selectedItem = selectedItem,
        onItemSelected = onItemSelected,
        modifier = modifier
    )
}
