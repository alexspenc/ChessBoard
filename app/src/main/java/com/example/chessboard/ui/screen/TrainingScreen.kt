package com.example.chessboard.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.app.Activity
import androidx.lifecycle.LifecycleOwner

// TrainingScreenContainer - Manages all backend logic
@Composable
fun TrainingScreenContainer(
    activity: Activity,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onNavigate: (String) -> Unit = {}
) {
    val gameController = remember { GameController() }
    val dataBaseController = remember { DatabaseProvider.createInstance(context = activity.applicationContext) }
    var isDatabaseBusy by remember { mutableStateOf(false) }

    val saveGame: () -> Unit = {
        println("Save game clicked")

        if (!isDatabaseBusy) {
            isDatabaseBusy = true

            val localPgn = gameController.generatePgn()
            val gameEntity = GameEntity(
                white = "Biba",
                black = "Buba",
                result = null,
                event = null,
                site = null,
                date = 0,
                round = null,
                eco = null,
                pgn = localPgn,
                initialFen = "",
            )

            (activity as? LifecycleOwner)?.let { lifecycleOwner ->
                lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    dataBaseController.addGame(
                        gameEntity,
                        gameController.getMovesCopy())
                    withContext(Dispatchers.Main) {
                        isDatabaseBusy = false
                    }
                }
            }
        }
    }

    val clearDatabase: () -> Unit = {
        if (!isDatabaseBusy) {
            isDatabaseBusy = true

            (activity as? LifecycleOwner)?.let { lifecycleOwner ->
                lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    dataBaseController.clearAllData()
                    withContext(Dispatchers.Main) {
                        isDatabaseBusy = false
                    }
                }
            }
        }
    }

    TrainingScreen(
        gameController = gameController,
        modifier = modifier,
        onBackClick = onBackClick,
        onNavigate = onNavigate,
        onSaveGame = saveGame,
        onDatabaseClear = clearDatabase
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(
    gameController: GameController,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
    onSaveGame: () -> Unit = {},
    onDatabaseClear: () -> Unit = {}
) {
    var selectedNavItem by remember { mutableStateOf("Training") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = TrainingBackgroundDark,
        topBar = {
            TrainingTopBar(onBackClick = onBackClick)
        },
        bottomBar = {
            TrainingBottomNavigation(
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            TrainingStatsRow(
                correctCount = 0,
                incorrectCount = 0,
                streakCount = 0
            )

            Spacer(modifier = Modifier.height(20.dp))

            ChessBoardSection(gameController = gameController)

            Spacer(modifier = Modifier.height(20.dp))

            TrainingActionButtons(
                onSaveGame = onSaveGame,
                onDatabaseClear = onDatabaseClear,
                gameController = gameController
            )

            Spacer(modifier = Modifier.height(20.dp))

            TrainingStatusCard()

            Spacer(modifier = Modifier.height(16.dp))

            ResetTrainingButton(onResetClick = {
                // Handle reset logic
            })

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainingTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = TrainingBackgroundDark,
            navigationIconContentColor = TrainingTextPrimary,
            titleContentColor = TrainingTextPrimary
        ),
        navigationIcon = {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(40.dp)
                    .background(
                        color = TrainingSurfaceDark,
                        shape = RoundedCornerShape(10.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TrainingTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        title = {
            Text(
                text = "Training: Italian Game",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TrainingTextPrimary
            )
        }
    )
}

@Composable
private fun TrainingStatsRow(
    correctCount: Int,
    incorrectCount: Int,
    streakCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatChip(
            icon = "✓",
            count = correctCount,
            color = TrainingSuccessGreen,
            modifier = Modifier.weight(1f)
        )
        StatChip(
            icon = "✕",
            count = incorrectCount,
            color = TrainingErrorRed,
            modifier = Modifier.weight(1f)
        )
        StatChip(
            icon = "🔥",
            count = streakCount,
            color = TrainingWarningOrange,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatChip(
    icon: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = TrainingCardDark
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = icon, fontSize = 16.sp, color = color)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = count.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TrainingTextPrimary
            )
        }
    }
}

@Composable
private fun TrainingStatusCard(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = TrainingCardDark,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp),
                color = TrainingAccentTeal.copy(alpha = 0.15f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(text = "♟", fontSize = 28.sp, color = TrainingAccentTeal)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Your Turn",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TrainingTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Make the correct move to continue the opening",
                    fontSize = 13.sp,
                    color = TrainingTextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun TrainingBottomNavigation(
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomNavItem("Home", Icons.Outlined.Home, Icons.Filled.Home),
        BottomNavItem("Training", Icons.Outlined.AccountBox, Icons.Filled.AccountBox),
        BottomNavItem("Stats", Icons.Outlined.Info, Icons.Filled.Info),
        BottomNavItem("Profile", Icons.Outlined.Person, Icons.Filled.Person)
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
                    BottomNavItemView(
                        item = item,
                        isSelected = selectedItem == item.label,
                        onClick = { onItemSelected(item.label) }
                    )
                }
            }
        }
    }
}

private data class BottomNavItem(
    val label: String,
    val iconUnselected: ImageVector,
    val iconSelected: ImageVector
)

@Composable
private fun BottomNavItemView(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (isSelected) TrainingAccentTeal else TrainingIconInactive

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (isSelected) item.iconSelected else item.iconUnselected,
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TrainingScreenPreview() {
    ChessBoardTheme {
        TrainingScreen(gameController = GameController())
    }
}
