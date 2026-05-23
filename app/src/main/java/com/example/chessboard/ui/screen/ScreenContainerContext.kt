package com.example.chessboard.ui.screen

import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.runtimecontext.RuntimeContext
import com.example.chessboard.ui.error.AppErrorReporter

data class ScreenContainerContext(
    val onBackClick: () -> Unit = {},
    val onNavigate: (ScreenType) -> Unit = {},
    val inDbProvider: DatabaseProvider,
    val runtimeContext: RuntimeContext = RuntimeContext(),
    val errorReporter: AppErrorReporter = AppErrorReporter.NoOp,
)
