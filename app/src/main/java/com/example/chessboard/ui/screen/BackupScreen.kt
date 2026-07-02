@file:Suppress("FunctionName")

package com.example.chessboard.ui.screen

/*
 * Legacy mixed-responsibility file.
 * Current role: groups backup screen UI with screen-level document picker orchestration for line PGN backups and full database backups.
 * Allowed here:
 * - backup screen state, dialogs, launcher wiring, and calls into backup services
 * - UI-specific restore confirmation and progress handling
 * Prefer not to add here:
 * - database-file copy logic, PGN parsing, or reusable backup algorithms
 * Validation date: 2026-06-28
 */

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.chessboard.R
import com.example.chessboard.service.LineBackupRestoreProgress
import com.example.chessboard.service.LineBackupRestoreResult
import com.example.chessboard.ui.BackupContentTestTag
import com.example.chessboard.ui.BackupFullCreateTestTag
import com.example.chessboard.ui.BackupFullRestoreTestTag
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
import com.example.chessboard.ui.components.HomeIconButton
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
    onProgress: suspend (LineBackupRestoreProgress) -> Unit,
) -> LineBackupRestoreResult

@Composable
fun BackupScreenContainer(
    activity: Activity,
    screenContext: ScreenContainerContext,
    modifier: Modifier = Modifier,
    testRestoreUri: Uri? = null,
    restoreBackupRunner: BackupRestoreRunner? = null,
) {
    var databaseGeneration by remember { mutableStateOf(0) }
    val lineBackupService = remember(databaseGeneration) { screenContext.inDbProvider.createLineBackupService() }
    val fullDatabaseBackupService =
        remember(databaseGeneration) {
            screenContext.inDbProvider.createFullDatabaseBackupService()
        }
    val noLinesFoundMessage = stringResource(R.string.backup_no_lines_found)
    val restoredLinesFormat = stringResource(R.string.backup_restored_lines)
    val skippedLinesFormat = stringResource(R.string.backup_skipped_lines)
    val processedLinesFormat = stringResource(R.string.backup_processed_lines)
    val restoreCanceledMessage = stringResource(R.string.backup_restore_canceled)
    val failedOpenSelectedFileMessage = stringResource(R.string.backup_failed_open_selected_file)
    val failedOpenDestinationMessage = stringResource(R.string.backup_failed_open_destination)
    val backupSavedAsFormat = stringResource(R.string.backup_saved_as)
    val failedSaveBackupMessage = stringResource(R.string.backup_failed_save)
    val failedRestoreLinesMessage = stringResource(R.string.backup_failed_restore)
    val fullBackupSavedAsFormat = stringResource(R.string.backup_full_saved_as)
    val failedSaveFullBackupMessage = stringResource(R.string.backup_full_failed_save)
    val failedRestoreFullBackupMessage = stringResource(R.string.backup_full_failed_restore)

    fun resolveDefaultBackupFileName(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd-HH-mm", Locale.US)
        val timestamp = formatter.format(Date())
        return "lines-backup-$timestamp.pgn"
    }

    fun ensureBackupFileName(fileName: String): String {
        val trimmed = fileName.trim().ifBlank { resolveDefaultBackupFileName() }
        if (trimmed.endsWith(".pgn", ignoreCase = true)) {
            return trimmed
        }

        return "$trimmed.pgn"
    }

    fun resolveDefaultFullBackupFileName(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd-HH-mm", Locale.US)
        val timestamp = formatter.format(Date())
        return "chessboard-full-backup-$timestamp.db"
    }

    fun resolveRestoreMessage(result: LineBackupRestoreResult): String {
        if (result.restoredLinesCount == 0 && result.skippedLinesCount == 0) {
            return noLinesFoundMessage
        }

        return buildString {
            appendLine(restoredLinesFormat.format(result.restoredLinesCount))
            append(skippedLinesFormat.format(result.skippedLinesCount))
        }
    }

    fun resolveRestoreCanceledMessage(progress: LineBackupRestoreProgress?): String {
        val currentProgress = progress ?: return restoreCanceledMessage

        return buildString {
            appendLine(restoreCanceledMessage)
            appendLine(processedLinesFormat.format(currentProgress.processedLinesCount, currentProgress.totalLines))
            appendLine(restoredLinesFormat.format(currentProgress.restoredLinesCount))
            append(skippedLinesFormat.format(currentProgress.skippedLinesCount))
        }
    }

    suspend fun runRestoreBackup(
        restoreUri: Uri,
        onProgress: suspend (LineBackupRestoreProgress) -> Unit,
    ): LineBackupRestoreResult {
        if (restoreBackupRunner != null) {
            return restoreBackupRunner(restoreUri, onProgress)
        }

        val inputStream = activity.contentResolver.openInputStream(restoreUri)
        if (inputStream == null) {
            throw IllegalStateException(failedOpenSelectedFileMessage)
        }

        return inputStream.use { stream ->
            lineBackupService.restoreBackup(stream, onProgress)
        }
    }

    var showBackupDialog by remember { mutableStateOf(false) }
    var backupFileName by remember { mutableStateOf(resolveDefaultBackupFileName()) }
    var fullBackupFileName by remember { mutableStateOf(resolveDefaultFullBackupFileName()) }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var backupError by remember { mutableStateOf<String?>(null) }
    var restoreMessage by remember { mutableStateOf<String?>(null) }
    var restoreError by remember { mutableStateOf<String?>(null) }
    var fullBackupMessage by remember { mutableStateOf<String?>(null) }
    var fullRestoreMessage by remember { mutableStateOf<String?>(null) }
    var fullBackupError by remember { mutableStateOf<String?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var pendingFullRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var restoreProgress by remember { mutableStateOf<LineBackupRestoreProgress?>(null) }
    var restoreJob by remember { mutableStateOf<Job?>(null) }

    val backupLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/x-chess-pgn"),
        ) { uri: Uri? ->
            if (uri == null) {
                return@rememberLauncherForActivityResult
            }

            (activity as? LifecycleOwner)?.lifecycleScope?.launch(Dispatchers.IO) {
                try {
                    val outputStream = activity.contentResolver.openOutputStream(uri)
                    if (outputStream == null) {
                        withContext(Dispatchers.Main) {
                            backupError = failedOpenDestinationMessage
                        }
                        return@launch
                    }

                    outputStream.use { stream ->
                        lineBackupService.writeBackup(stream)
                    }

                    withContext(Dispatchers.Main) {
                        backupMessage = backupSavedAsFormat.format(ensureBackupFileName(backupFileName))
                    }
                } catch (error: Exception) {
                    withContext(Dispatchers.Main) {
                        backupError = error.message ?: failedSaveBackupMessage
                    }
                }
            }
        }

    val fullBackupLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/vnd.sqlite3"),
        ) { uri: Uri? ->
            if (uri == null) {
                return@rememberLauncherForActivityResult
            }

            (activity as? LifecycleOwner)?.lifecycleScope?.launch(Dispatchers.IO) {
                try {
                    val outputStream = activity.contentResolver.openOutputStream(uri)
                    if (outputStream == null) {
                        withContext(Dispatchers.Main) {
                            fullBackupError = failedOpenDestinationMessage
                        }
                        return@launch
                    }

                    outputStream.use { stream ->
                        fullDatabaseBackupService.writeBackup(stream)
                    }

                    withContext(Dispatchers.Main) {
                        fullBackupMessage = fullBackupSavedAsFormat.format(fullBackupFileName)
                    }
                } catch (error: Exception) {
                    withContext(Dispatchers.Main) {
                        fullBackupError = error.message ?: failedSaveFullBackupMessage
                    }
                }
            }
        }

    val restoreLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri: Uri? ->
            if (uri == null) {
                return@rememberLauncherForActivityResult
            }

            pendingRestoreUri = uri
        }

    val fullRestoreLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri: Uri? ->
            if (uri == null) {
                return@rememberLauncherForActivityResult
            }

            pendingFullRestoreUri = uri
        }

    if (backupMessage != null) {
        AppMessageDialog(
            title = stringResource(R.string.backup_saved_title),
            message = backupMessage!!,
            onDismiss = { backupMessage = null },
        )
    }

    if (backupError != null) {
        AppMessageDialog(
            title = stringResource(R.string.backup_failed_title),
            message = backupError!!,
            onDismiss = { backupError = null },
        )
    }

    if (restoreMessage != null) {
        AppMessageDialog(
            title = stringResource(R.string.backup_restore_title),
            message = restoreMessage!!,
            onDismiss = { restoreMessage = null },
        )
    }

    if (restoreError != null) {
        AppMessageDialog(
            title = stringResource(R.string.backup_restore_failed_title),
            message = restoreError!!,
            onDismiss = { restoreError = null },
        )
    }

    if (fullBackupMessage != null) {
        AppMessageDialog(
            title = stringResource(R.string.backup_full_saved_title),
            message = fullBackupMessage!!,
            onDismiss = { fullBackupMessage = null },
        )
    }

    if (fullRestoreMessage != null) {
        AppMessageDialog(
            title = stringResource(R.string.backup_full_restore_title),
            message = fullRestoreMessage!!,
            onDismiss = { fullRestoreMessage = null },
        )
    }

    if (fullBackupError != null) {
        AppMessageDialog(
            title = stringResource(R.string.backup_full_failed_title),
            message = fullBackupError!!,
            onDismiss = { fullBackupError = null },
        )
    }

    if (restoreProgress != null) {
        BackupRestoreProgressDialog(
            progress = restoreProgress!!,
            onCancel = {
                restoreJob?.cancel()
            },
        )
    }

    if (pendingRestoreUri != null) {
        AppConfirmDialog(
            title = stringResource(R.string.backup_restore_title),
            message = stringResource(R.string.backup_restore_confirm_message),
            onDismiss = { pendingRestoreUri = null },
            onConfirm = {
                val restoreUri = pendingRestoreUri!!
                pendingRestoreUri = null

                val lifecycleOwner = activity as? LifecycleOwner ?: return@AppConfirmDialog
                restoreJob =
                    lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val result =
                                runRestoreBackup(restoreUri) { progress ->
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
                                restoreError = error.message ?: failedRestoreLinesMessage
                            }
                        }
                    }
            },
            confirmText = stringResource(R.string.backup_restore_confirm_action),
            isDestructive = true,
        )
    }

    if (pendingFullRestoreUri != null) {
        AppConfirmDialog(
            title = stringResource(R.string.backup_full_restore_title),
            message = stringResource(R.string.backup_full_restore_confirm_message),
            onDismiss = { pendingFullRestoreUri = null },
            onConfirm = {
                val restoreUri = pendingFullRestoreUri!!
                pendingFullRestoreUri = null

                val lifecycleOwner = activity as? LifecycleOwner ?: return@AppConfirmDialog
                lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val inputStream = activity.contentResolver.openInputStream(restoreUri)
                        if (inputStream == null) {
                            withContext(Dispatchers.Main) {
                                fullBackupError = failedOpenSelectedFileMessage
                            }
                            return@launch
                        }

                        fullDatabaseBackupService.restoreBackup(inputStream)

                        withContext(Dispatchers.Main) {
                            databaseGeneration += 1
                            fullRestoreMessage = activity.getString(R.string.backup_full_restored_message)
                        }
                    } catch (error: Exception) {
                        withContext(Dispatchers.Main) {
                            fullBackupError = error.message ?: failedRestoreFullBackupMessage
                        }
                    }
                }
            },
            confirmText = stringResource(R.string.backup_full_restore_confirm_action),
            isDestructive = true,
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
            },
        )
    }

    BackupScreen(
        onBackClick = screenContext.onBackClick,
        onNavigate = screenContext.onNavigate,
        onCreateBackupClick = {
            backupFileName = resolveDefaultBackupFileName()
            showBackupDialog = true
        },
        onRestoreLinesClick = {
            if (testRestoreUri != null) {
                pendingRestoreUri = testRestoreUri
                return@BackupScreen
            }

            restoreLauncher.launch(arrayOf("application/x-chess-pgn", "text/plain", "*/*"))
        },
        onCreateFullBackupClick = {
            val resolvedName = resolveDefaultFullBackupFileName()
            fullBackupFileName = resolvedName
            fullBackupLauncher.launch(resolvedName)
        },
        onRestoreFullBackupClick = {
            fullRestoreLauncher.launch(arrayOf("application/vnd.sqlite3", "application/octet-stream", "*/*"))
        },
        modifier = modifier,
    )
}

@Composable
private fun BackupScreen(
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    onCreateBackupClick: () -> Unit = {},
    onRestoreLinesClick: () -> Unit = {},
    onCreateFullBackupClick: () -> Unit = {},
    onRestoreFullBackupClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    AppScreenScaffold(
        modifier =
            modifier
                .fillMaxSize()
                .testTag(BackupContentTestTag),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.backup_title),
                subtitleLines = listOf(stringResource(R.string.backup_subtitle)),
                onBackClick = onBackClick,
                handleSystemBack = true,
                filledBackButton = true,
                actions = {
                    HomeIconButton(onClick = { onNavigate(ScreenType.Home) })
                },
            )
        },
        bottomBar = {
            AppBottomNavigation(
                items = defaultAppBottomNavigationItems(),
                selectedItem = ScreenType.Home,
                onItemSelected = onNavigate,
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(AppDimens.spaceLg)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
        ) {
            ScreenSection {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
                ) {
                    ScreenTitleText(text = stringResource(R.string.backup_content_title))
                    BodySecondaryText(text = stringResource(R.string.backup_content_subtitle))
                    PrimaryButton(
                        text = stringResource(R.string.backup_create_action),
                        onClick = onCreateBackupClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PrimaryButton(
                        text = stringResource(R.string.backup_restore_action),
                        onClick = onRestoreLinesClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            ScreenSection {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
                ) {
                    ScreenTitleText(text = stringResource(R.string.backup_full_content_title))
                    BodySecondaryText(text = stringResource(R.string.backup_full_content_subtitle))
                    PrimaryButton(
                        text = stringResource(R.string.backup_full_create_action),
                        onClick = onCreateFullBackupClick,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .testTag(BackupFullCreateTestTag),
                    )
                    PrimaryButton(
                        text = stringResource(R.string.backup_full_restore_action),
                        onClick = onRestoreFullBackupClick,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .testTag(BackupFullRestoreTestTag),
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
            ScreenTitleText(text = stringResource(R.string.backup_file_dialog_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                BodySecondaryText(
                    text = stringResource(R.string.backup_file_dialog_message),
                )
                AppTextField(
                    value = fileName,
                    onValueChange = onFileNameChange,
                    label = stringResource(R.string.backup_file_name_label),
                    placeholder = stringResource(R.string.backup_file_name_placeholder),
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = stringResource(R.string.backup_location_action),
                onClick = onConfirm,
            )
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Background.SurfaceDark),
            ) {
                CardMetaText(text = stringResource(R.string.common_cancel))
            }
        },
        containerColor = Background.ScreenDark,
    )
}

@Composable
private fun BackupRestoreProgressDialog(
    progress: LineBackupRestoreProgress,
    onCancel: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.testTag(BackupRestoreProgressDialogTestTag),
        onDismissRequest = {},
        title = {
            ScreenTitleText(text = stringResource(R.string.backup_restoring_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                BodySecondaryText(text = stringResource(R.string.backup_total_lines, progress.totalLines))
                BodySecondaryText(
                    text =
                        stringResource(
                            R.string.backup_processed_lines,
                            progress.processedLinesCount,
                            progress.totalLines,
                        ),
                )
                BodySecondaryText(text = stringResource(R.string.backup_restored_lines, progress.restoredLinesCount))
                BodySecondaryText(text = stringResource(R.string.backup_skipped_lines, progress.skippedLinesCount))
            }
        },
        confirmButton = {
            PrimaryButton(
                text = stringResource(R.string.backup_stop_action),
                onClick = onCancel,
                modifier = Modifier.testTag(BackupRestoreCancelTestTag),
            )
        },
        containerColor = Background.ScreenDark,
    )
}
