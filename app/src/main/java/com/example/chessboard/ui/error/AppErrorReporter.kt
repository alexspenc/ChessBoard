package com.example.chessboard.ui.error

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val AppErrorLogTag = "ChessBoardAppError"

data class AppErrorUiState(
    val title: String,
    val message: String,
)

class AppErrorReporter(
    private val defaultTitle: String,
    private val defaultMessage: String,
    private val onReport: (AppErrorUiState) -> Unit,
) {
    fun report(
        error: Throwable,
        message: String = defaultMessage,
        title: String = defaultTitle,
    ) {
        if (error is CancellationException) {
            throw error
        }

        Log.e(AppErrorLogTag, message, error)
        onReport(AppErrorUiState(title = title, message = message))
    }

    companion object {
        val NoOp = AppErrorReporter(
            defaultTitle = "",
            defaultMessage = "",
            onReport = {},
        )
    }
}

fun CoroutineScope.launchAppCatching(
    errorReporter: AppErrorReporter,
    message: String,
    block: suspend CoroutineScope.() -> Unit,
): Job {
    return launch {
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            errorReporter.report(error, message)
        }
    }
}
