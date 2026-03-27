package com.example.chessboard.ui.screen

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.entity.SideMask
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppDivider
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppSearchField
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.PillSurface
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.components.ScreenTitleText
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class FilterTab { ALL, AS_WHITE, AS_BLACK }

@Composable
fun HomeScreenContainer(
    activity: Activity,
    onNavigate: (ScreenType) -> Unit = {},
    onOpenGame: (GameEntity) -> Unit = {},
    onCreateTrainingClick: () -> Unit = {},
    onStartFirstTrainingClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    inDbProvider : DatabaseProvider,
) {
    val dbProvider = inDbProvider
    var games by remember { mutableStateOf<List<GameEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) { dbProvider.getAllGames() }
        games = loaded
    }

    HomeScreen(
        games = games,
        onNavigate = onNavigate,
        onOpenGame = onOpenGame,
        onCreateTrainingClick = onCreateTrainingClick,
        onStartFirstTrainingClick = onStartFirstTrainingClick,
        modifier = modifier
    )
}

@Composable
fun HomeScreen(
    games: List<GameEntity>,
    onNavigate: (ScreenType) -> Unit = {},
    onOpenGame: (GameEntity) -> Unit = {},
    onCreateTrainingClick: () -> Unit = {},
    onStartFirstTrainingClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(FilterTab.ALL) }

    val filteredGames = games.filter { game ->
        val matchesSearch = game.event?.contains(searchQuery, ignoreCase = true) != false ||
                game.eco?.contains(searchQuery, ignoreCase = true) == true
        val matchesFilter = when (selectedFilter) {
            FilterTab.ALL -> true
            FilterTab.AS_WHITE -> game.sideMask and SideMask.WHITE != 0
            FilterTab.AS_BLACK -> game.sideMask and SideMask.BLACK != 0
        }
        matchesSearch && matchesFilter
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            HomeBottomNavigation(onItemSelected = onNavigate)
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = AppDimens.spaceLg)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppDimens.spaceLg, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("♛", fontSize = 26.sp, color = TrainingAccentTeal)
                            Spacer(modifier = Modifier.width(AppDimens.spaceSm))
                            Text(
                                text = "Chess Openings",
                                style = MaterialTheme.typography.displaySmall,
                                color = TextColor.Primary
                            )
                        }
                        SectionTitleText(
                            text = "${games.size} opening${if (games.size == 1) "" else "s"}",
                            color = TextColor.Secondary
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PrimaryButton(
                            text = "Start First",
                            onClick = onStartFirstTrainingClick
                        )
                        PrimaryButton(
                            text = "Full Train",
                            onClick = onCreateTrainingClick
                        )
                        AddOpeningButton(
                            onClick = { onNavigate(ScreenType.CreateOpening) }
                        )
                    }
                }
            }

            item {
                ScreenSection {
                    AppSearchField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search openings...",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(AppDimens.spaceLg)) }

            item {
                ScreenSection {
                    PillSurface(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(AppDimens.spaceXs)
                    ) {
                        FilterTabOption(
                            label = "All",
                            isSelected = selectedFilter == FilterTab.ALL,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedFilter = FilterTab.ALL }
                        )
                        FilterTabOption(
                            label = "As White",
                            isSelected = selectedFilter == FilterTab.AS_WHITE,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedFilter = FilterTab.AS_WHITE }
                        )
                        FilterTabOption(
                            label = "As Black",
                            isSelected = selectedFilter == FilterTab.AS_BLACK,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedFilter = FilterTab.AS_BLACK }
                        )
                    }
                }
            }

            item {
                AppDivider(modifier = Modifier.padding(top = AppDimens.spaceLg))
                Spacer(modifier = Modifier.height(AppDimens.spaceLg))
            }

            if (filteredGames.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BodySecondaryText(
                            text = if (games.isEmpty()) "No openings yet.\nTap + to create one." else "No results found.",
                            color = TextColor.Secondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(filteredGames, key = { it.id }) { game ->
                    GameEntityCard(
                        game = game,
                        modifier = Modifier.padding(horizontal = AppDimens.spaceLg),
                        onClick = { onOpenGame(game) }
                    )
                    Spacer(modifier = Modifier.height(AppDimens.spaceMd))
                }
            }
        }
    }
}

@Composable
private fun AddOpeningButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.size(AppDimens.buttonHeight),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(AppDimens.radiusLg),
        colors = ButtonDefaults.buttonColors(containerColor = TrainingAccentTeal),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add opening",
            tint = Color.White,
            modifier = Modifier.size(AppDimens.navIconSize)
        )
    }
}

@Composable
private fun GameEntityCard(game: GameEntity, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    CardSurface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        ScreenTitleText(
            text = game.event ?: "Unnamed Opening"
        )
        if (!game.eco.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(AppDimens.spaceSm))
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(AppDimens.radiusXs),
                color = Background.ScreenDark
            ) {
                CardMetaText(
                    text = game.eco,
                    modifier = Modifier.padding(horizontal = AppDimens.spaceSm, vertical = AppDimens.spaceXs),
                    color = TextColor.Secondary
                )
            }
        }
    }
}

@Composable
private fun FilterTabOption(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(AppDimens.radiusPill))
            .background(if (isSelected) Color.White else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = AppDimens.radiusMd),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) Color.Black else TextColor.Secondary,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HomeBottomNavigation(
    onItemSelected: (ScreenType) -> Unit,
    modifier: Modifier = Modifier
) {
    AppBottomNavigation(
        items = defaultAppBottomNavigationItems(),
        selectedItem = ScreenType.Home,
        onItemSelected = onItemSelected,
        modifier = modifier
    )
}
