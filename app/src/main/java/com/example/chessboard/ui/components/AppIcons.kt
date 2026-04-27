package com.example.chessboard.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.size
import com.example.chessboard.ui.theme.TrainingAccentTeal

@Composable
fun SettingsIconButton(
    onClick: () -> Unit,
    contentDescription: String = "Settings",
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = contentDescription,
            tint = TrainingAccentTeal,
        )
    }
}

@Composable
fun HintIconButton(
    onClick: () -> Unit,
    iconSize: Dp = 22.dp,
    buttonSize: Dp = 40.dp,
    tint: Color = TrainingAccentTeal,
    contentDescription: String = "Hint",
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier.size(buttonSize)) {
        Icon(
            imageVector = Icons.Default.Lightbulb,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}
