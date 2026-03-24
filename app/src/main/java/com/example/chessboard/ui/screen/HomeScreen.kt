package com.example.chessboard.ui.screen

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessboard.ui.theme.*

private data class ChessOpening(
    val name: String,
    val ecoCode: String,
    val difficulty: String,
    val whiteWin: Int,
    val draw: Int,
    val blackWin: Int
)

private enum class FilterTab { ALL, AS_WHITE, AS_BLACK }

private val sampleOpenings = listOf(
    ChessOpening("Italian Game", "C50", "Beginner", 38, 34, 28),
    ChessOpening("Sicilian Defense", "B20", "Intermediate", 35, 33, 32),
    ChessOpening("French Defense", "C00", "Intermediate", 34, 33, 33),
    ChessOpening("Queen's Gambit", "D06", "Intermediate", 37, 35, 28),
    ChessOpening("King's Indian", "E60", "Advanced", 33, 31, 36),
    ChessOpening("Ruy Lopez", "C60", "Intermediate", 39, 33, 28),
    ChessOpening("Caro-Kann", "B10", "Beginner", 36, 35, 29),
    ChessOpening("English Opening", "A10", "Advanced", 35, 36, 29),
    ChessOpening("Dutch Defense", "A80", "Advanced", 34, 32, 34),
    ChessOpening("Nimzo-Indian", "E20", "Advanced", 33, 35, 32),
)

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(FilterTab.ALL) }

    val filteredOpenings = sampleOpenings.filter { it.name.contains(searchQuery, ignoreCase = true) }

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
                            text = "Master ${sampleOpenings.size} classic openings",
                            fontSize = 14.sp,
                            color = TrainingTextSecondary
                        )
                    }
                    Button(
                        onClick = {},
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

            items(filteredOpenings) { opening ->
                OpeningCard(
                    opening = opening,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
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
private fun OpeningCard(opening: ChessOpening, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = TrainingCardDark
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = opening.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TrainingTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EcoChip(opening.ecoCode)
                DifficultyChip(opening.difficulty)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBox(label = "White", value = "${opening.whiteWin}%", modifier = Modifier.weight(1f))
                StatBox(label = "Draw", value = "${opening.draw}%", modifier = Modifier.weight(1f))
                StatBox(label = "Black", value = "${opening.blackWin}%", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EcoChip(code: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = TrainingBackgroundDark
    ) {
        Text(
            text = code,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = TrainingTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DifficultyChip(difficulty: String) {
    val color = when (difficulty) {
        "Beginner" -> TrainingAccentTeal
        "Intermediate" -> TrainingWarningOrange
        else -> TrainingErrorRed
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color
    ) {
        Text(
            text = difficulty,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = TrainingBackgroundDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 12.sp, color = TrainingTextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TrainingTextPrimary
            )
        }
    }
}

@Composable
private fun HomeBottomNavigation(
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    data class NavItem(val label: String, val outlinedIcon: ImageVector, val filledIcon: ImageVector)
    val items = listOf(
        NavItem("Home", Icons.Outlined.Home, Icons.Filled.Home),
        NavItem("Training", Icons.Outlined.AccountBox, Icons.Filled.AccountBox),
        NavItem("Stats", Icons.Outlined.Info, Icons.Filled.Info),
        NavItem("Profile", Icons.Outlined.Person, Icons.Filled.Person),
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
                    val isSelected = item.label == "Home"
                    val color = if (isSelected) TrainingAccentTeal else TrainingIconInactive
                    Column(
                        modifier = Modifier
                            .clickable { onItemSelected(item.label) }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.filledIcon else item.outlinedIcon,
                            contentDescription = item.label,
                            tint = color,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.label,
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
