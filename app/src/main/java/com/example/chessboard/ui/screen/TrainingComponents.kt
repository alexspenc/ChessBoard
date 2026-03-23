package com.example.chessboard.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.ui.ChessBoardWithCoordinates

internal object TrainingColors {
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

@Composable
fun ChessBoardSection(
    gameController: GameController,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
    ) {
        ChessBoardWithCoordinates(
            gameController = gameController,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun TrainingActionButtons(
    onSaveGame: () -> Unit,
    onDatabaseClear: () -> Unit,
    gameController: GameController,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Primary actions: Save game, Clear database
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onSaveGame,
                modifier = Modifier.weight(1f).height(48.dp),
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
                Text(text = "Save game", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }

            Button(
                onClick = onDatabaseClear,
                modifier = Modifier.weight(1f).height(48.dp),
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
                Text(text = "Clear database", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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
                modifier = Modifier.weight(1f).height(48.dp),
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
                Text(text = "Back", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }

            Button(
                onClick = { gameController.redoMove() },
                enabled = gameController.canRedo,
                modifier = Modifier.weight(1f).height(48.dp),
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
                Text(text = "Forward", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }

            Button(
                onClick = { gameController.resetToStartPosition() },
                modifier = Modifier.weight(1f).height(48.dp),
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
                Text(text = "Reset", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun ResetTrainingButton(
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onResetClick,
        modifier = modifier.fillMaxWidth().height(56.dp),
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
        Text(text = "Reset Training", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}
