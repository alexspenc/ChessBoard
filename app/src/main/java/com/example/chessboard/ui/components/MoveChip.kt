package com.example.chessboard.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chessboard.ui.moveChipTestTag
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal

@Composable
fun MoveChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    unselectedBackground: Color = Background.SurfaceDark,
    unselectedTextColor: Color = TextColor.Secondary,
    textStyle: TextStyle = MaterialTheme.typography.labelMedium,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .testTag(moveChipTestTag(label)),
        shape = RoundedCornerShape(AppDimens.radiusSm),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) TrainingAccentTeal else unselectedBackground,
            contentColor = if (isSelected) Color.White else unselectedTextColor
        ),
        contentPadding = contentPadding,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        Text(
            text = label,
            style = textStyle,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else unselectedTextColor
        )
    }
}
