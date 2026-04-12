package com.example.chessboard.ui.screen

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import com.example.chessboard.R
import com.example.chessboard.entity.SideMask
import com.example.chessboard.service.OneGameTrainingData
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppSearchField
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.components.ScreenTitleText
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val HomeSegmentBackground = Color(0xFF151515)
private val HomeSegmentSelected = Color(0xFF202020)
private val HomeBadgeBackground = Color(0xFF242428)
private val HomeMetricCard = Color(0xFF202024)
private val HomeDivider = Color(0xFF202020)

enum class HomeSideFilter {
    ALL,
    WHITE,
    BLACK
}

data class HomeTrainingItem(
    val trainingId: Long,
    val name: String,
    val gamesCount: Int,
    val supportsWhite: Boolean,
    val supportsBlack: Boolean,
)

@Composable
fun HomeScreenContainer(
    activity: Activity,
    screenContext: ScreenContainerContext,
    simpleViewEnabled: Boolean,
    onCreateOpeningClick: () -> Unit = { screenContext.onNavigate(ScreenType.CreateOpening) },
    onCreateTrainingClick: () -> Unit = {},
    onOpenPositionEditorClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var trainings by remember { mutableStateOf<List<HomeTrainingItem>>(emptyList()) }
    val trainingService = remember(screenContext.inDbProvider) { screenContext.inDbProvider.createTrainingService() }

    LaunchedEffect(simpleViewEnabled) {
        trainings = if (!simpleViewEnabled) {
            emptyList()
        } else {
            withContext(Dispatchers.IO) {
                val allGames = screenContext.inDbProvider.getAllGames().associateBy { it.id }
                trainingService.getAllTrainings().map { training ->
                    val trainingGames = OneGameTrainingData.fromJson(training.gamesJson)
                    val includedGames = trainingGames.mapNotNull { allGames[it.gameId] }
                    HomeTrainingItem(
                        trainingId = training.id,
                        name = training.name.ifBlank { "Unnamed Training" },
                        gamesCount = trainingGames.size,
                        supportsWhite = includedGames.any { game ->
                            (game.sideMask and SideMask.WHITE) != 0
                        },
                        supportsBlack = includedGames.any { game ->
                            (game.sideMask and SideMask.BLACK) != 0
                        },
                    )
                }
            }
        }
    }

    HomeScreen(
        simpleViewEnabled = simpleViewEnabled,
        trainings = trainings,
        onNavigate = screenContext.onNavigate,
        onCreateOpeningClick = onCreateOpeningClick,
        onCreateTrainingClick = onCreateTrainingClick,
        onOpenPositionEditorClick = onOpenPositionEditorClick,
        onOpenBackupClick = { screenContext.onNavigate(ScreenType.Backup) },
        onExitClick = { activity.finishAffinity() },
        modifier = modifier
    )
}

@Composable
fun HomeScreen(
    simpleViewEnabled: Boolean,
    trainings: List<HomeTrainingItem>,
    onNavigate: (ScreenType) -> Unit = {},
    onCreateOpeningClick: () -> Unit = { onNavigate(ScreenType.CreateOpening) },
    onCreateTrainingClick: () -> Unit = {},
    onOpenPositionEditorClick: () -> Unit = {},
    onOpenBackupClick: () -> Unit = {},
    onExitClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (simpleViewEnabled) {
        SimpleHomeScreen(
            trainings = trainings,
            onCreateOpeningClick = onCreateOpeningClick,
            onOpenTraining = { trainingId ->
                onNavigate(ScreenType.EditTraining(trainingId))
            },
            onNavigate = onNavigate,
            modifier = modifier
        )
        return
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
            contentPadding = PaddingValues(
                start = AppDimens.spaceLg,
                top = 20.dp,
                end = AppDimens.spaceLg,
                bottom = AppDimens.spaceLg
            ),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        BodySecondaryText(
                            text = "Home",
                            color = TextColor.Secondary
                        )
                    }
                    AddOpeningButton(
                        onClick = onCreateOpeningClick
                    )
                }
            }

            item {
                ScreenSection {
                    BodySecondaryText(
                        text = "Choose what you want to do.",
                        color = TextColor.Secondary
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
                ) {
                    HomeActionCard(
                        title = "Trainings",
                        subtitle = "Open saved training plans",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(ScreenType.Training) }
                    )
                    HomeActionCard(
                        title = "Games",
                        subtitle = "Browse saved openings",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(ScreenType.GamesExplorer) }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
                ) {
                    HomeActionCard(
                        title = "Create Training",
                        subtitle = "Build a training from saved games",
                        modifier = Modifier.weight(1f),
                        onClick = onCreateTrainingClick
                    )
                    HomeActionCard(
                        title = "Templates",
                        subtitle = "Browse and edit training templates",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(ScreenType.TrainingTemplates) }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
                ) {
                    HomeActionCard(
                        title = "Create Opening",
                        subtitle = "Save a new opening line",
                        modifier = Modifier.weight(1f),
                        onClick = onCreateOpeningClick
                    )
                    HomeActionCard(
                        title = "Position Editor",
                        subtitle = "Set up a custom board position",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenPositionEditorClick
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
                ) {
                    HomeActionCard(
                        title = "Backup Games",
                        subtitle = "Export all games to a PGN file",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenBackupClick
                    )
                    HomeActionCard(
                        title = "Exit",
                        subtitle = "Close the application",
                        modifier = Modifier.weight(1f),
                        onClick = onExitClick
                    )
                }
            }
        }
    }
}

@Composable
private fun SimpleHomeScreen(
    trainings: List<HomeTrainingItem>,
    onCreateOpeningClick: () -> Unit,
    onOpenTraining: (Long) -> Unit,
    onNavigate: (ScreenType) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var sideFilter by remember { mutableStateOf(HomeSideFilter.ALL) }

    val filteredTrainings = remember(trainings, searchQuery, sideFilter) {
        trainings.filter { training ->
            val matchesSearch = searchQuery.isBlank() ||
                training.name.contains(searchQuery, ignoreCase = true)
            val matchesSide = when (sideFilter) {
                HomeSideFilter.ALL -> true
                HomeSideFilter.WHITE -> training.supportsWhite
                HomeSideFilter.BLACK -> training.supportsBlack
            }
            matchesSearch && matchesSide
        }
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
            contentPadding = PaddingValues(
                start = 10.dp,
                top = 8.dp,
                end = 10.dp,
                bottom = AppDimens.spaceLg
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.ic_crown),
                                contentDescription = null,
                                tint = TrainingAccentTeal,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Chess Openings",
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextColor.Primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Master 10 classic openings",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextColor.Secondary,
                            modifier = Modifier.padding(start = 2.dp, top = 4.dp)
                        )
                    }
                    AddOpeningButton(onClick = onCreateOpeningClick)
                }
            }

            item {
                AppSearchField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Search openings...",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                HomeSideFilterRow(
                    selectedFilter = sideFilter,
                    onFilterSelected = { sideFilter = it }
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(HomeDivider)
                )
            }

            if (filteredTrainings.isEmpty()) {
                item {
                    CardSurface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Background.CardDark
                    ) {
                        BodySecondaryText(
                            text = if (trainings.isEmpty()) {
                                "No trainings available."
                            } else {
                                "No trainings match the current filters."
                            },
                            color = TextColor.Secondary
                        )
                    }
                }
            } else {
                items(filteredTrainings.size) { index ->
                    val training = filteredTrainings[index]
                    HomeTrainingCard(
                        training = training,
                        onClick = { onOpenTraining(training.trainingId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeSideFilterRow(
    selectedFilter: HomeSideFilter,
    onFilterSelected: (HomeSideFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = HomeSegmentBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            HomeSideFilter.entries.forEach { filter ->
                val isSelected = filter == selectedFilter
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) HomeSegmentSelected else Color.Transparent)
                        .clickable { onFilterSelected(filter) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (filter) {
                            HomeSideFilter.ALL -> "All"
                            HomeSideFilter.WHITE -> "As White"
                            HomeSideFilter.BLACK -> "As Black"
                        },
                        color = if (isSelected) TextColor.Primary else TextColor.Secondary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeTrainingCard(
    training: HomeTrainingItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CardSurface(
        modifier = modifier.fillMaxWidth(),
        color = Background.CardDark,
        onClick = onClick,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = training.name,
                style = MaterialTheme.typography.headlineSmall,
                color = TextColor.Primary,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeBadge(text = "Training")
                HomeBadge(
                    text = when {
                        training.supportsWhite && training.supportsBlack -> "White + Black"
                        training.supportsWhite -> "As White"
                        training.supportsBlack -> "As Black"
                        else -> "Mixed"
                    },
                    background = Color(0xFF203327),
                    contentColor = Color(0xFF59D98E)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HomeMetricBox(
                    title = "Games",
                    value = training.gamesCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                HomeMetricBox(
                    title = "Training",
                    value = "#${training.trainingId}",
                    modifier = Modifier.weight(1f)
                )
                HomeMetricBox(
                    title = "Sides",
                    value = when {
                        training.supportsWhite && training.supportsBlack -> "Both"
                        training.supportsWhite -> "White"
                        training.supportsBlack -> "Black"
                        else -> "-"
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HomeBadge(
    text: String,
    background: Color = HomeBadgeBackground,
    contentColor: Color = TextColor.Secondary,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        CardMetaText(
            text = text,
            color = contentColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun HomeMetricBox(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(HomeMetricCard)
            .padding(vertical = 14.dp, horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CardMetaText(
            text = title,
            color = Color(0xFF9AA0B3),
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = TextColor.Primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HomeActionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CardSurface(
        modifier = modifier,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
            ) {
                ScreenTitleText(text = title)
                CardMetaText(text = subtitle)
            }
        }
    }
}

@Composable
private fun AddOpeningButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(TrainingAccentTeal)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add opening",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
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
