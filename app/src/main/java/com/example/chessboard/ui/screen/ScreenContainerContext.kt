package com.example.chessboard.ui.screen

import com.example.chessboard.repository.DatabaseProvider

data class ScreenContainerContext(
    val onBackClick: () -> Unit = {},
    val onNavigate: (ScreenType) -> Unit = {},
    val inDbProvider: DatabaseProvider,
)
