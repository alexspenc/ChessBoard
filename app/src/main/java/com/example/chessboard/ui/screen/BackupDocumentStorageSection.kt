package com.example.chessboard.ui.screen

/*
 * File role: renders backup-screen status and controls for the configured app document root.
 * Keep screen-facing storage state mapping and folder-selection UI here.
 * Do not add SAF permission calls, picker contracts, backup file I/O, or persistence logic.
 * Validation date: 2026-07-24
 */

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R
import com.example.chessboard.service.AppDocumentStorage
import com.example.chessboard.ui.BackupStorageSectionTestTag
import com.example.chessboard.ui.BackupStorageSelectTestTag
import com.example.chessboard.ui.BackupStorageStatusTestTag
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.ScreenTitleText

internal sealed interface BackupDocumentStorageUiState {
    data object Loading : BackupDocumentStorageUiState

    data object Configuring : BackupDocumentStorageUiState

    data class Loaded(
        val storageState: AppDocumentStorage.State,
    ) : BackupDocumentStorageUiState

    data object Error : BackupDocumentStorageUiState
}

internal fun resolveReadyDocumentStorageState(
    uiState: BackupDocumentStorageUiState,
): AppDocumentStorage.State.Ready? {
    if (uiState !is BackupDocumentStorageUiState.Loaded) {
        return null
    }

    val storageState = uiState.storageState
    if (storageState !is AppDocumentStorage.State.Ready) {
        return null
    }

    return storageState
}

internal fun isDocumentStorageStateResolved(
    uiState: BackupDocumentStorageUiState,
): Boolean {
    if (uiState is BackupDocumentStorageUiState.Loading) {
        return false
    }
    if (uiState is BackupDocumentStorageUiState.Configuring) {
        return false
    }

    return true
}

@Composable
internal fun BackupDocumentStorageSection(
    uiState: BackupDocumentStorageUiState,
    onSelectFolderClick: () -> Unit,
) {
    val readyState = resolveReadyDocumentStorageState(uiState)
    val actionEnabled = isDocumentStorageStateResolved(uiState)
    val actionTextResource = resolveDocumentStorageActionTextResource(readyState)

    BackupOptionSection(
        modifier = Modifier.testTag(BackupStorageSectionTestTag),
    ) {
        ScreenTitleText(text = stringResource(R.string.backup_storage_title))
        BodySecondaryText(
            text = resolveDocumentStorageStatusText(uiState),
            modifier = Modifier.testTag(BackupStorageStatusTestTag),
        )
        PrimaryButton(
            text = stringResource(actionTextResource),
            onClick = onSelectFolderClick,
            enabled = actionEnabled,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(BackupStorageSelectTestTag),
        )
    }
}

private fun resolveDocumentStorageActionTextResource(
    readyState: AppDocumentStorage.State.Ready?,
): Int {
    if (readyState == null) {
        return R.string.backup_storage_select_action
    }

    return R.string.backup_storage_change_action
}

@Composable
private fun resolveDocumentStorageStatusText(
    uiState: BackupDocumentStorageUiState,
): String {
    if (uiState is BackupDocumentStorageUiState.Loading) {
        return stringResource(R.string.backup_storage_loading)
    }
    if (uiState is BackupDocumentStorageUiState.Configuring) {
        return stringResource(R.string.backup_storage_configuring)
    }
    if (uiState is BackupDocumentStorageUiState.Error) {
        return stringResource(R.string.backup_storage_unavailable)
    }

    val loadedState = uiState as BackupDocumentStorageUiState.Loaded
    if (loadedState.storageState is AppDocumentStorage.State.Ready) {
        return stringResource(R.string.backup_storage_ready)
    }
    if (loadedState.storageState is AppDocumentStorage.State.PermissionLost) {
        return stringResource(R.string.backup_storage_permission_lost)
    }

    return stringResource(R.string.backup_storage_not_configured)
}
