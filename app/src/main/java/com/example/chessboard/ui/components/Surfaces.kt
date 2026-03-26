package com.example.chessboard.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TrainingCardDark
import com.example.chessboard.ui.theme.TrainingDividerColor
import com.example.chessboard.ui.theme.TrainingSurfaceDark

/** Wraps content in the app's default card container used for standalone content blocks. */
@Composable
fun CardSurface(
    modifier: Modifier = Modifier,
    color: Color = TrainingCardDark,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(AppDimens.spaceLg),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickableModifier = if (onClick == null) {
        modifier
    } else {
        modifier.clickable(onClick = onClick)
    }

    Surface(
        modifier = clickableModifier,
        shape = RoundedCornerShape(AppDimens.radiusXl),
        color = color,
        border = border
    ) {
        Column(
            modifier = Modifier.padding(contentPadding)
        ) {
            content()
        }
    }
}

/** Wraps compact horizontal controls in a pill-shaped surface. */
@Composable
fun PillSurface(
    modifier: Modifier = Modifier,
    color: Color = TrainingSurfaceDark,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = AppDimens.spaceLg,
        vertical = AppDimens.spaceMd
    ),
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AppDimens.radiusPill),
        color = color
    ) {
        Row(
            modifier = Modifier.padding(contentPadding)
        ) {
            content()
        }
    }
}

/** Applies the standard horizontal screen padding for a content section. */
@Composable
fun ScreenSection(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = AppDimens.spaceLg),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        content = content
    )
}

/** Draws the app's standard divider between screen sections. */
@Composable
fun AppDivider(
    modifier: Modifier = Modifier,
    color: Color = TrainingDividerColor
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = AppDimens.dividerThickness,
        color = color
    )
}
