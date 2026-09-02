package com.example.chessboard.ui.screen.fenpositions.details

/*
 * File role: renders actions available for the currently opened FEN position.
 * Allowed here:
 * - details bottom-bar layout and edit/delete/add-continuation action presentation
 * Not allowed here:
 * - confirmation dialogs, persistence, loading state, or app navigation
 * Validation date: 2026-09-02
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
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
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsDeleteTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsEditTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsAddContinuationTestTag
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingErrorRed

@Composable
internal fun FenPositionDetailsBottomBar(
    editContentDescription: String,
    deleteContentDescription: String,
    addContinuationContentDescription: String,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onAddContinuationClick: () -> Unit,
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
                    AppDimens.spaceXl,
                    Alignment.CenterHorizontally,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FenPositionDetailsActionButton(
                    imageVector = Icons.Default.Add,
                    contentDescription = addContinuationContentDescription,
                    backgroundColor = TrainingAccentTeal,
                    testTag = FenPositionDetailsAddContinuationTestTag,
                    onClick = onAddContinuationClick,
                )
                FenPositionDetailsActionButton(
                    imageVector = Icons.Default.Edit,
                    contentDescription = editContentDescription,
                    backgroundColor = TrainingAccentTeal,
                    testTag = FenPositionDetailsEditTestTag,
                    onClick = onEditClick,
                )
                FenPositionDetailsActionButton(
                    imageVector = Icons.Default.Delete,
                    contentDescription = deleteContentDescription,
                    backgroundColor = TrainingErrorRed,
                    testTag = FenPositionDetailsDeleteTestTag,
                    onClick = onDeleteClick,
                )
            }
        }
    }
}

@Composable
private fun FenPositionDetailsActionButton(
    imageVector: ImageVector,
    contentDescription: String,
    backgroundColor: Color,
    testTag: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
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
