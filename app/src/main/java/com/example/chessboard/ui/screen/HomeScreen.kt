package com.example.chessboard.ui.screen

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.entity.SideMask
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class FilterTab { ALL, AS_WHITE, AS_BLACK }

private data class NavItem(val label: ScreenType, val outlinedIcon: ImageVector, val filledIcon: ImageVector)

@Composable
fun HomeScreenContainer(
    activity: Activity,
    onNavigate: (ScreenType) -> Unit = {},
    onOpenGame: (GameEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dbProvider = remember { DatabaseProvider.createInstance(activity.applicationContext) }
    var games by remember { mutableStateOf<List<GameEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) { dbProvider.getAllGames() }
        games = loaded
    }

    HomeScreen(
        games = games,
        onNavigate = onNavigate,
        onOpenGame = onOpenGame,
        modifier = modifier
    )
}

@Composable
fun HomeScreen(
    games: List<GameEntity>,
    onNavigate: (ScreenType) -> Unit = {},
    onOpenGame: (GameEntity) -> Unit = {},
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
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("♛", fontSize = 26.sp, color = TrainingAccentTeal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Chess Openings",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = TrainingTextPrimary
                            )
                        }
                        Text(
                            text = "${games.size} opening${if (games.size == 1) "" else "s"}",
                            fontSize = 14.sp,
                            color = TrainingTextSecondary
                        )
                    }
                    Button(
                        onClick = { onNavigate(ScreenType.CreateOpening) },
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TrainingAccentTeal),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add opening",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(50.dp),
                    color = TrainingSurfaceDark
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TrainingTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = TrainingTextPrimary, fontSize = 15.sp),
                            cursorBrush = SolidColor(TrainingAccentTeal),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search openings...",
                                        color = TrainingTextSecondary,
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(50.dp),
                    color = TrainingSurfaceDark
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
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
                HorizontalDivider(
                    modifier = Modifier.padding(top = 16.dp),
                    thickness = 0.5.dp,
                    color = TrainingDividerColor
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (filteredGames.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (games.isEmpty()) "No openings yet.\nTap + to create one." else "No results found.",
                            color = TrainingTextSecondary,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(filteredGames, key = { it.id }) { game ->
                    GameEntityCard(
                        game = game,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onClick = { onOpenGame(game) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun GameEntityCard(game: GameEntity, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = TrainingCardDark
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = game.event ?: "Unnamed Opening",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TrainingTextPrimary
            )
            if (!game.eco.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = TrainingBackgroundDark
                ) {
                    Text(
                        text = game.eco,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = TrainingTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
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
            .clip(RoundedCornerShape(50.dp))
            .background(if (isSelected) Color.White else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.Black else TrainingTextSecondary,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            fontSize = 14.sp
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
            HorizontalDivider(thickness = 0.5.dp, color = TrainingDividerColor)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                items.forEach { item ->
                    val isSelected = item.label == ScreenType.Home
                    val color = if (isSelected) TrainingAccentTeal else TrainingIconInactive
                    Column(
                        modifier = Modifier
                            .clickable { onItemSelected(item.label) }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.filledIcon else item.outlinedIcon,
                            contentDescription = item.label.toString(),
                            tint = color,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.label.toString(),
                            fontSize = 11.sp,
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
