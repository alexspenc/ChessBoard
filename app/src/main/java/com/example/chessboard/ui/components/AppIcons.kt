package com.example.chessboard.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingErrorRed

object AppIconSizes {
    val Xs = 16.dp
    val Sm = 20.dp
    val Md = 24.dp
    val Lg = 32.dp
}

@Composable
private fun AppSizedIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size),
    )
}

@Composable
fun IconXs(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    AppSizedIcon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        size = AppIconSizes.Xs,
        modifier = modifier,
        tint = tint,
    )
}

@Composable
fun IconSm(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    AppSizedIcon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        size = AppIconSizes.Sm,
        modifier = modifier,
        tint = tint,
    )
}

@Composable
fun IconMd(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    AppSizedIcon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        size = AppIconSizes.Md,
        modifier = modifier,
        tint = tint,
    )
}

@Composable
fun IconLg(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    AppSizedIcon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        size = AppIconSizes.Lg,
        modifier = modifier,
        tint = tint,
    )
}

@Composable
fun DeleteIconButton(
    onClick: () -> Unit,
    contentDescription: String = "Delete",
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        IconMd(
            imageVector = Icons.Default.Delete,
            contentDescription = contentDescription,
            tint = TrainingErrorRed,
        )
    }
}

@Composable
fun SettingsIconButton(
    onClick: () -> Unit,
    contentDescription: String = "Settings",
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        IconMd(
            imageVector = Icons.Default.Settings,
            contentDescription = contentDescription,
            tint = TrainingAccentTeal,
        )
    }
}

@Composable
fun HintIconButton(
    onClick: () -> Unit,
    iconSize: Dp = AppIconSizes.Sm,
    buttonSize: Dp = AppDimens.iconButtonSize,
    tint: Color = TrainingAccentTeal,
    contentDescription: String = "Hint",
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier.size(buttonSize)) {
        IconSm(
            imageVector = Icons.Default.Lightbulb,
            contentDescription = contentDescription,
            tint = tint,
            modifier = if (iconSize == AppIconSizes.Sm) Modifier else Modifier.size(iconSize),
        )
    }
}
