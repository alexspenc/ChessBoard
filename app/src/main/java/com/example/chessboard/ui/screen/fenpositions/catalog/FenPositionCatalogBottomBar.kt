package com.example.chessboard.ui.screen.fenpositions.catalog

/*
 * File role: renders catalog actions in the FEN position catalog bottom bar.
 * Allowed here:
 * - bottom-bar layout and the add-position action button
 * Not allowed here:
 * - dialog state, persistence, paging, or navigation
 * Validation date: 2026-08-31
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.chessboard.ui.components.AppDivider
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCatalogAddTestTag
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TrainingAccentTeal

@Composable
internal fun FenPositionCatalogBottomBar(
    addContentDescription: String,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Background.SurfaceDark,
        tonalElevation = 8.dp,
    ) {
        Column {
            AppDivider()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppDimens.spaceSm),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(TrainingAccentTeal)
                        .clickable(onClick = onAddClick)
                        .testTag(FenPositionCatalogAddTestTag),
                    contentAlignment = Alignment.Center,
                ) {
                    IconMd(
                        imageVector = Icons.Default.Add,
                        contentDescription = addContentDescription,
                        tint = Color.White,
                    )
                }
            }
        }
    }
}
