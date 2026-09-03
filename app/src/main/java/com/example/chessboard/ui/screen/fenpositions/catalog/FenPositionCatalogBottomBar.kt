package com.example.chessboard.ui.screen.fenpositions.catalog

/*
 * File role: renders catalog actions in the FEN position catalog bottom bar.
 * Allowed here:
 * - bottom-bar layout and add/open/delete/analyze position action buttons
 * Not allowed here:
 * - dialog state, persistence, paging, or navigation
 * Validation date: 2026-09-03
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.chessboard.ui.components.AppDivider
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCatalogAddTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCatalogAnalyzeTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCatalogDeleteTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCatalogCopyFenTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionCatalogOpenTestTag
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.MutedContentColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingErrorRed

@Composable
internal fun FenPositionCatalogBottomBar(
    addContentDescription: String,
    openContentDescription: String,
    deleteContentDescription: String,
    copyFenContentDescription: String,
    analyzeContentDescription: String,
    canOpen: Boolean,
    canDelete: Boolean,
    canCopyFen: Boolean,
    canAnalyze: Boolean,
    onAddClick: () -> Unit,
    onOpenClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCopyFenClick: () -> Unit,
    onAnalyzeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Background.SurfaceDark,
        tonalElevation = 8.dp,
    ) {
        Column {
            AppDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppDimens.spaceSm),
                horizontalArrangement = Arrangement.spacedBy(
                    AppDimens.spaceLg,
                    Alignment.CenterHorizontally,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FenPositionCatalogActionButton(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = copyFenContentDescription,
                    backgroundColor = TrainingAccentTeal,
                    enabled = canCopyFen,
                    testTag = FenPositionCatalogCopyFenTestTag,
                    onClick = onCopyFenClick,
                )
                FenPositionCatalogActionButton(
                    imageVector = Icons.Default.Add,
                    contentDescription = addContentDescription,
                    backgroundColor = TrainingAccentTeal,
                    enabled = true,
                    testTag = FenPositionCatalogAddTestTag,
                    onClick = onAddClick,
                )
                FenPositionCatalogActionButton(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = openContentDescription,
                    backgroundColor = TrainingAccentTeal,
                    enabled = canOpen,
                    testTag = FenPositionCatalogOpenTestTag,
                    onClick = onOpenClick,
                )
                FenPositionCatalogActionButton(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = analyzeContentDescription,
                    backgroundColor = TrainingAccentTeal,
                    enabled = canAnalyze,
                    testTag = FenPositionCatalogAnalyzeTestTag,
                    onClick = onAnalyzeClick,
                )
                FenPositionCatalogActionButton(
                    imageVector = Icons.Default.Delete,
                    contentDescription = deleteContentDescription,
                    backgroundColor = TrainingErrorRed,
                    enabled = canDelete,
                    testTag = FenPositionCatalogDeleteTestTag,
                    onClick = onDeleteClick,
                )
            }
        }
    }
}

@Composable
private fun FenPositionCatalogActionButton(
    imageVector: ImageVector,
    contentDescription: String,
    backgroundColor: Color,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    val resolvedBackgroundColor = if (enabled) backgroundColor else MutedContentColor
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(resolvedBackgroundColor)
            .clickable(
                enabled = enabled,
                onClick = onClick,
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        IconMd(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = Color.White,
        )
    }
}
