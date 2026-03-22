package com.example.chessboard.ui

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.database.DatabaseProvider
import com.example.chessboard.database.GameEntity
import com.example.chessboard.ui.theme.ChessBoardTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.app.Activity

// TrainingScreenContainer - Manages all backend logic
@Composable
fun TrainingScreenContainer(
    activity: Activity,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
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

            (activity as? androidx.lifecycle.LifecycleOwner)?.let { lifecycleOwner ->
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

            (activity as? androidx.lifecycle.LifecycleOwner)?.let { lifecycleOwner ->
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
        onSaveGame = saveGame,
        onDatabaseClear = clearDatabase
    )
}

// Color Palette for Training Screen
private object TrainingColors {
    val BackgroundDark = Color(0xFF0D0D0D)
    val SurfaceDark = Color(0xFF1A1A1A)
    val CardDark = Color(0xFF252525)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF9E9E9E)
    val AccentTeal = Color(0xFF1DB584)
    val SuccessGreen = Color(0xFF4CAF50)
    val ErrorRed = Color(0xFFF44336)
    val WarningOrange = Color(0xFFFF9800)
    val IconInactive = Color(0xFF666666)
    val DividerColor = Color(0xFF2A2A2A)
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
        containerColor = TrainingColors.BackgroundDark,
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

            // Stats Row
            TrainingStatsRow(
                correctCount = 0,
                incorrectCount = 0,
                streakCount = 0
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Chess Board Section
            // ============================================================
            // PLUG YOUR EXISTING CHESSBOARD HERE
            // Replace this Box with: ChessBoardWithCoordinates(gameController)
            // ============================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                ChessBoardWithCoordinates(
                    gameController = gameController,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            TrainingActionButtons(
                onSaveGame = onSaveGame,
                onDatabaseClear = onDatabaseClear,
                gameController = gameController
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Status Card
            TrainingStatusCard()

            Spacer(modifier = Modifier.height(16.dp))

            // Reset Button
            ResetTrainingButton(onResetClick = {
                // Handle reset logic
            })

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TrainingActionButtons(
    onSaveGame: () -> Unit,
    onDatabaseClear: () -> Unit,
    gameController: GameController,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Primary actions: Save game, Clear database
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onSaveGame,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrainingColors.AccentTeal,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                )
            ) {
                Text(
                    text = "Save game",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            Button(
                onClick = onDatabaseClear,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrainingColors.SurfaceDark,
                    contentColor = TrainingColors.TextPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                )
            ) {
                Text(
                    text = "Clear database",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Secondary actions: Back, Forward, Reset
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { gameController.undoMove() },
                enabled = gameController.canUndo,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrainingColors.SurfaceDark,
                    contentColor = TrainingColors.TextPrimary,
                    disabledContainerColor = TrainingColors.CardDark,
                    disabledContentColor = TrainingColors.TextSecondary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                )
            ) {
                Text(
                    text = "Back",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            Button(
                onClick = { gameController.redoMove() },
                enabled = gameController.canRedo,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrainingColors.SurfaceDark,
                    contentColor = TrainingColors.TextPrimary,
                    disabledContainerColor = TrainingColors.CardDark,
                    disabledContentColor = TrainingColors.TextSecondary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                )
            ) {
                Text(
                    text = "Forward",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            Button(
                onClick = { gameController.resetToStartPosition() },
                enabled = true,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrainingColors.SurfaceDark,
                    contentColor = TrainingColors.TextPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                )
            ) {
                Text(
                    text = "Reset",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
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
            containerColor = TrainingColors.BackgroundDark,
            navigationIconContentColor = TrainingColors.TextPrimary,
            titleContentColor = TrainingColors.TextPrimary
        ),
        navigationIcon = {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(40.dp)
                    .background(
                        color = TrainingColors.SurfaceDark,
                        shape = RoundedCornerShape(10.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TrainingColors.TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        title = {
            Text(
                text = "Training: Italian Game",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TrainingColors.TextPrimary
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
        // Correct moves
        StatChip(
            icon = "✓",
            count = correctCount,
            color = TrainingColors.SuccessGreen,
            modifier = Modifier.weight(1f)
        )
        
        // Incorrect moves
        StatChip(
            icon = "✕",
            count = incorrectCount,
            color = TrainingColors.ErrorRed,
            modifier = Modifier.weight(1f)
        )
        
        // Streak
        StatChip(
            icon = "🔥",
            count = streakCount,
            color = TrainingColors.WarningOrange,
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
        color = TrainingColors.CardDark
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = icon,
                fontSize = 16.sp,
                color = color
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = count.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TrainingColors.TextPrimary
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
        color = TrainingColors.CardDark,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Container
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp),
                color = TrainingColors.AccentTeal.copy(alpha = 0.15f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "♟",
                        fontSize = 28.sp,
                        color = TrainingColors.AccentTeal
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Text Content
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Your Turn",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TrainingColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Make the correct move to continue the opening",
                    fontSize = 13.sp,
                    color = TrainingColors.TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun ResetTrainingButton(
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onResetClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = TrainingColors.SurfaceDark,
            contentColor = TrainingColors.TextPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp
        )
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Reset",
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Reset Training",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
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
        color = TrainingColors.SurfaceDark,
        tonalElevation = 8.dp
    ) {
        Column {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = TrainingColors.DividerColor
            )
            
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
    val color = if (isSelected) TrainingColors.AccentTeal else TrainingColors.IconInactive
    
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
        TrainingScreen(
            gameController = GameController()
        )
    }
}