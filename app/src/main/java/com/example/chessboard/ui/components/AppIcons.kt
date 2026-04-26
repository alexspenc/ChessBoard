package com.example.chessboard.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
