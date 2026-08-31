package com.example.chessboard.ui.screen

/*
 * File role: renders the backup-screen dialog for selecting a full database backup.
 * Keep strict backup-list presentation and selection callbacks here.
 * Do not add document-provider queries, system picker launchers, or restore operations.
 * Validation date: 2026-08-31
 */

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.chessboard.R
import com.example.chessboard.service.FullDatabaseBackupDocument
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.ScreenTitleText
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TrainingAccentTeal

@Composable
internal fun FullDatabaseBackupPickerDialog(
    backups: List<FullDatabaseBackupDocument>?,
    onDismiss: () -> Unit,
    onBackupSelected: (FullDatabaseBackupDocument) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Background.ScreenDark,
        title = {
            ScreenTitleText(text = stringResource(R.string.backup_full_picker_title))
        },
        text = {
            FullDatabaseBackupPickerContent(
                backups = backups,
                onBackupSelected = onBackupSelected,
            )
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                CardMetaText(text = stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun FullDatabaseBackupPickerContent(
    backups: List<FullDatabaseBackupDocument>?,
    onBackupSelected: (FullDatabaseBackupDocument) -> Unit,
) {
    if (backups == null) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
        ) {
            CircularProgressIndicator(color = TrainingAccentTeal)
            BodySecondaryText(text = stringResource(R.string.backup_full_picker_loading))
        }
        return
    }

    if (backups.isEmpty()) {
        BodySecondaryText(text = stringResource(R.string.backup_full_picker_empty))
        return
    }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp),
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
    ) {
        items(
            items = backups,
            key = { backup -> backup.uri.toString() },
        ) { backup ->
            TextButton(
                onClick = { onBackupSelected(backup) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                BodySecondaryText(
                    text = backup.displayName,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
