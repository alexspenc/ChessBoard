package com.example.chessboard.ui.screen

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.entity.SideMask
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.ui.components.AppDivider
import com.example.chessboard.ui.components.AppSearchField
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.NavLabelText
import com.example.chessboard.ui.components.PillSurface
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.components.ScreenTitleText
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.theme.AppDimens

import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingBackgroundDark
import com.example.chessboard.ui.theme.TrainingDividerColor
import com.example.chessboard.ui.theme.TrainingIconInactive
import com.example.chessboard.ui.theme.TrainingTextPrimary
import com.example.chessboard.ui.theme.TrainingTextSecondary
import com.example.chessboard.ui.theme.TrainingSurfaceDark
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class FilterTab { ALL, AS_WHITE, AS_BLACK }
private const val FULL_TRAINING_NAME = "FullTraining"

private data class NavItem(val label: ScreenType, val outlinedIcon: ImageVector, val filledIcon: ImageVector)
private data class TrainingCreationSuccess(
    val trainingId: Long,
    val trainingName: String,
    val gamesCount: Int
)

@Composable
fun HomeScreenContainer(
    activity: Activity,
    onNavigate: (ScreenType) -> Unit = {},
    onOpenGame: (GameEntity) -> Unit = {},
    onCreateTrainingClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    inDbProvider : DatabaseProvider,
) {
    val dbProvider = inDbProvider
    var games by remember { mutableStateOf<List<GameEntity>>(emptyList()) }
    var showNoGamesError by remember { mutableStateOf(false) }
    var trainingCreationSuccess by remember { mutableStateOf<TrainingCreationSuccess?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) { dbProvider.getAllGames() }
        games = loaded
    }

    if (showNoGamesError) {
        TrainingCreationErrorDialog(
            onDismiss = { showNoGamesError = false }
        )
    }

    trainingCreationSuccess?.let { success ->
        TrainingCreationSuccessDialog(
            success = success,
            onDismiss = { trainingCreationSuccess = null }
        )
    }

    HomeScreen(
        games = games,
        onNavigate = onNavigate,
        onOpenGame = onOpenGame,
        onCreateTrainingClick = {
            handleCreateTrainingClick(
                scope = scope,
                dbProvider = dbProvider,
                gamesCount = games.size,
                onBeforeCreate = onCreateTrainingClick,
                onEmptyGames = { showNoGamesError = true },
                onTrainingCreated = { trainingCreationSuccess = it }
            )
        },
        modifier = modifier
    )
}

private fun handleCreateTrainingClick(
    scope: CoroutineScope,
    dbProvider: DatabaseProvider,
    gamesCount: Int,
    onBeforeCreate: () -> Unit,
    onEmptyGames: () -> Unit,
    onTrainingCreated: (TrainingCreationSuccess) -> Unit
) {
    onBeforeCreate()
    scope.launch {
        val success = withContext(Dispatchers.IO) {
            createFullTraining(
                dbProvider = dbProvider,
                gamesCount = gamesCount
            )
        }

        if (success == null) {
            onEmptyGames()
        } else {
            onTrainingCreated(success)
        }
    }
}

@Composable
fun HomeScreen(
    games: List<GameEntity>,
    onNavigate: (ScreenType) -> Unit = {},
    onOpenGame: (GameEntity) -> Unit = {},
    onCreateTrainingClick: () -> Unit = {},
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = TrainingBackgroundDark,
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
                                color = TrainingTextPrimary
                            )
                        }
                        SectionTitleText(
                            text = "${games.size} opening${if (games.size == 1) "" else "s"}",
                            color = TrainingTextSecondary
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                AppDivider(modifier = Modifier.padding(top = AppDimens.spaceLg), color = TrainingDividerColor)
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
                            color = TrainingTextSecondary,
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

private suspend fun createFullTraining(
    dbProvider: DatabaseProvider,
    gamesCount: Int
): TrainingCreationSuccess? {
    val trainingId = dbProvider.createTrainingFromAllGames(name = FULL_TRAINING_NAME) ?: return null

    return TrainingCreationSuccess(
        trainingId = trainingId,
        trainingName = FULL_TRAINING_NAME,
        gamesCount = gamesCount
    )
}

@Composable
private fun TrainingCreationErrorDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TrainingBackgroundDark,
        title = {
            ScreenTitleText(
                text = "Training Creation Failed",
                color = TrainingTextPrimary
            )
        },
        text = {
            BodySecondaryText(
                text = "There are no games available to create a training.",
                color = TrainingTextSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                CardMetaText(
                    text = "OK",
                    color = TrainingAccentTeal
                )
            }
        }
    )
}

@Composable
private fun TrainingCreationSuccessDialog(
    success: TrainingCreationSuccess,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TrainingBackgroundDark,
        title = {
            ScreenTitleText(
                text = "Training Created",
                color = TrainingTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)) {
                BodySecondaryText(
                    text = "ID: ${success.trainingId}",
                    color = TrainingTextSecondary
                )
                BodySecondaryText(
                    text = "Name: ${success.trainingName}",
                    color = TrainingTextSecondary
                )
                BodySecondaryText(
                    text = "Games added: ${success.gamesCount}",
                    color = TrainingTextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                CardMetaText(
                    text = "OK",
                    color = TrainingAccentTeal
                )
            }
        }
    )
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
            text = game.event ?: "Unnamed Opening",
            color = TrainingTextPrimary
        )
        if (!game.eco.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(AppDimens.spaceSm))
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(AppDimens.radiusXs),
                color = TrainingBackgroundDark
            ) {
                CardMetaText(
                    text = game.eco,
                    modifier = Modifier.padding(horizontal = AppDimens.spaceSm, vertical = AppDimens.spaceXs),
                    color = TrainingTextSecondary,
                    fontWeight = FontWeight.Medium
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
            color = if (isSelected) Color.Black else TrainingTextSecondary,
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
    val items = listOf(
        NavItem(ScreenType.Home, Icons.Outlined.Home, Icons.Filled.Home),
        NavItem(ScreenType.Training, Icons.Outlined.AccountBox, Icons.Filled.AccountBox),
        NavItem(ScreenType.Stats , Icons.Outlined.Info, Icons.Filled.Info),
        NavItem(ScreenType.Profile, Icons.Outlined.Person, Icons.Filled.Person),
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = TrainingSurfaceDark,
        tonalElevation = 8.dp
    ) {
        Column {
            HorizontalDivider(thickness = AppDimens.dividerThickness, color = TrainingDividerColor)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppDimens.spaceSm),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                items.forEach { item ->
                    val isSelected = item.label == ScreenType.Home
                    val color = if (isSelected) TrainingAccentTeal else TrainingIconInactive
                    Column(
                        modifier = Modifier
                            .clickable { onItemSelected(item.label) }
                            .padding(AppDimens.spaceSm),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.filledIcon else item.outlinedIcon,
                            contentDescription = item.label.toString(),
                            tint = color,
                            modifier = Modifier.size(AppDimens.navIconSize)
                        )
                        Spacer(modifier = Modifier.height(AppDimens.spaceXs))
                        NavLabelText(
                            text = item.label.toString(),
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = color,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
