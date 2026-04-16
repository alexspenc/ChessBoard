package com.example.chessboard.ui.screen

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.chessboard.service.GameBackupRestoreProgress
import com.example.chessboard.service.GameBackupRestoreResult
import com.example.chessboard.ui.BackupRestoreCancelTestTag
import com.example.chessboard.ui.BackupRestoreProgressDialogTestTag
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppConfirmDialog
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTextField
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.components.ScreenTitleText
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

typealias BackupRestoreRunner = suspend (
    uri: Uri,
    onProgress: suspend (GameBackupRestoreProgress) -> Unit,
) -> GameBackupRestoreResult

@Composable
fun BackupScreenContainer(
    activity: Activity,
    screenContext: ScreenContainerContext,
    modifier: Modifier = Modifier,
    testRestoreUri: Uri? = null,
    restoreBackupRunner: BackupRestoreRunner? = null,
) {
    val gameBackupService = remember { screenContext.inDbProvider.createGameBackupService() }

    fun resolveDefaultBackupFileName(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd-HH-mm", Locale.US)
        val timestamp = formatter.format(Date())
        return "games-backup-$timestamp.pgn"
    }

    fun ensureBackupFileName(fileName: String): String {
        val trimmed = fileName.trim().ifBlank { resolveDefaultBackupFileName() }
        if (trimmed.endsWith(".pgn", ignoreCase = true)) {
            return trimmed
        }

        return "$trimmed.pgn"
    }

    fun resolveRestoreMessage(result: GameBackupRestoreResult): String {
        if (result.restoredGamesCount == 0 && result.skippedGamesCount == 0) {
            return "No games were found in the selected backup."
        }

        return buildString {
            appendLine("Restored games: ${result.restoredGamesCount}")
            append("Skipped games: ${result.skippedGamesCount}")
        }
    }

    fun resolveRestoreCanceledMessage(progress: GameBackupRestoreProgress?): String {
        val currentProgress = progress ?: return "Restore canceled."

        return buildString {
            appendLine("Restore canceled.")
            appendLine("Processed games: ${currentProgress.processedGamesCount}/${currentProgress.totalGames}")
            appendLine("Restored games: ${currentProgress.restoredGamesCount}")
            append("Skipped games: ${currentProgress.skippedGamesCount}")
        }
    }

    suspend fun runRestoreBackup(
        restoreUri: Uri,
        onProgress: suspend (GameBackupRestoreProgress) -> Unit,
    ): GameBackupRestoreResult {
        if (restoreBackupRunner != null) {
            return restoreBackupRunner(restoreUri, onProgress)
        }

        val inputStream = activity.contentResolver.openInputStream(restoreUri)
        if (inputStream == null) {
            throw IllegalStateException("Failed to open the selected backup file")
        }

        return inputStream.use { stream ->
            gameBackupService.restoreBackup(stream, onProgress)
        }
    }

    var showBackupDialog by remember { mutableStateOf(false) }
    var backupFileName by remember { mutableStateOf(resolveDefaultBackupFileName()) }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var backupError by remember { mutableStateOf<String?>(null) }
    var restoreMessage by remember { mutableStateOf<String?>(null) }
    var restoreError by remember { mutableStateOf<String?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var restoreProgress by remember { mutableStateOf<GameBackupRestoreProgress?>(null) }
    var restoreJob by remember { mutableStateOf<Job?>(null) }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-chess-pgn")
    ) { uri: Uri? ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }

        (activity as? LifecycleOwner)?.lifecycleScope?.launch(Dispatchers.IO) {
            try {
                val outputStream = activity.contentResolver.openOutputStream(uri)
                if (outputStream == null) {
                    withContext(Dispatchers.Main) {
                        backupError = "Failed to open the selected destination"
                    }
                    return@launch
                }

                outputStream.use { stream ->
                    gameBackupService.writeBackup(stream)
                }

                withContext(Dispatchers.Main) {
                    backupMessage = "Backup saved as ${ensureBackupFileName(backupFileName)}"
                }
            } catch (error: Exception) {
                withContext(Dispatchers.Main) {
                    backupError = error.message ?: "Failed to save backup"
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }

        pendingRestoreUri = uri
    }

    if (backupMessage != null) {
        AppMessageDialog(
            title = "Backup Saved",
            message = backupMessage!!,
            onDismiss = { backupMessage = null }
        )
    }

    if (backupError != null) {
        AppMessageDialog(
            title = "Backup Failed",
            message = backupError!!,
            onDismiss = { backupError = null }
        )
    }

    if (restoreMessage != null) {
        AppMessageDialog(
            title = "Restore Games",
            message = restoreMessage!!,
            onDismiss = { restoreMessage = null }
        )
    }

    if (restoreError != null) {
        AppMessageDialog(
            title = "Restore Failed",
            message = restoreError!!,
            onDismiss = { restoreError = null }
        )
    }

    if (restoreProgress != null) {
        BackupRestoreProgressDialog(
            progress = restoreProgress!!,
            onCancel = {
                restoreJob?.cancel()
            }
        )
    }

    if (pendingRestoreUri != null) {
        AppConfirmDialog(
            title = "Restore Games",
            message = "Restore games from the selected backup file?",
            onDismiss = { pendingRestoreUri = null },
            onConfirm = {
                val restoreUri = pendingRestoreUri!!
                pendingRestoreUri = null

                val lifecycleOwner = activity as? LifecycleOwner ?: return@AppConfirmDialog
                restoreJob = lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val result = runRestoreBackup(restoreUri) { progress ->
                            withContext(Dispatchers.Main) {
                                restoreProgress = progress
                            }
                        }

                        withContext(Dispatchers.Main) {
                            restoreProgress = null
                            restoreJob = null
                            restoreMessage = resolveRestoreMessage(result)
                        }
                    } catch (_: CancellationException) {
                        withContext(NonCancellable + Dispatchers.Main) {
                            restoreJob = null
                            restoreMessage = resolveRestoreCanceledMessage(restoreProgress)
                            restoreProgress = null
                        }
                    } catch (error: Exception) {
                        withContext(Dispatchers.Main) {
                            restoreProgress = null
                            restoreJob = null
                            restoreError = error.message ?: "Failed to restore games"
                        }
                    }
                }
            },
            confirmText = "Restore",
            isDestructive = true
        )
    }

    if (showBackupDialog) {
        BackupFileNameDialog(
            fileName = backupFileName,
            onFileNameChange = { backupFileName = it },
            onDismiss = { showBackupDialog = false },
            onConfirm = {
                val resolvedName = ensureBackupFileName(backupFileName)
                backupFileName = resolvedName
                showBackupDialog = false
                backupLauncher.launch(resolvedName)
            }
        )
    }

    BackupScreen(
        onBackClick = screenContext.onBackClick,
        onNavigate = screenContext.onNavigate,
        onCreateBackupClick = {
            backupFileName = resolveDefaultBackupFileName()
            showBackupDialog = true
        },
        onRestoreGamesClick = {
            if (testRestoreUri != null) {
                pendingRestoreUri = testRestoreUri
                return@BackupScreen
            }

            restoreLauncher.launch(arrayOf("application/x-chess-pgn", "text/plain", "*/*"))
        },
        modifier = modifier
    )
}

@Composable
private fun BackupScreen(
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    onCreateBackupClick: () -> Unit = {},
    onRestoreGamesClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = "Backup",
                subtitle = "Export and restore saved games",
                onBackClick = onBackClick,
                filledBackButton = true,
            )
        },
        bottomBar = {
            AppBottomNavigation(
                items = defaultAppBottomNavigationItems(),
                selectedItem = ScreenType.Home,
                onItemSelected = onNavigate,
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(AppDimens.spaceLg),
            verticalArrangement = Arrangement.Center,
        ) {
            ScreenSection {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
                ) {
                    ScreenTitleText(text = "Game Backup")
                    BodySecondaryText(text = "Create a PGN backup or restore games later.")
                    PrimaryButton(
                        text = "Create Backup",
                        onClick = onCreateBackupClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                    PrimaryButton(
                        text = "Restore Games",
                        onClick = onRestoreGamesClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun BackupFileNameDialog(
    fileName: String,
    onFileNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            ScreenTitleText(text = "Backup Games")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                BodySecondaryText(
                    text = "Choose a file name. Android will then ask where to save the backup."
                )
                AppTextField(
                    value = fileName,
                    onValueChange = onFileNameChange,
                    label = "File name",
                    placeholder = "games-backup.pgn"
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Choose Location",
                onClick = onConfirm
            )
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Background.SurfaceDark)
            ) {
                CardMetaText(text = "Cancel")
            }
        },
        containerColor = Background.ScreenDark,
    )
}

@Composable
private fun BackupRestoreProgressDialog(
    progress: GameBackupRestoreProgress,
    onCancel: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.testTag(BackupRestoreProgressDialogTestTag),
        onDismissRequest = {},
        title = {
            ScreenTitleText(text = "Restoring Games")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                BodySecondaryText(text = "Total games: ${progress.totalGames}")
                BodySecondaryText(
                    text = "Processed games: ${progress.processedGamesCount}/${progress.totalGames}"
                )
                BodySecondaryText(text = "Restored games: ${progress.restoredGamesCount}")
                BodySecondaryText(text = "Skipped games: ${progress.skippedGamesCount}")
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Cancel Restore",
                onClick = onCancel,
                modifier = Modifier.testTag(BackupRestoreCancelTestTag)
            )
        },
        containerColor = Background.ScreenDark,
    )
}
