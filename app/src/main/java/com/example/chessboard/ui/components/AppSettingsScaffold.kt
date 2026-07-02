package com.example.chessboard.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.theme.AppDimens

@Composable
fun AppSettingsScaffold(
    title: String,
    selectedNavItem: ScreenType,
    onBackClick: () -> Unit,
    onNavigate: (ScreenType) -> Unit,
    modifier: Modifier = Modifier,
    subtitleLines: List<String> = emptyList(),
    filledBackButton: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = title,
                subtitleLines = subtitleLines,
                onBackClick = onBackClick,
                handleSystemBack = true,
                filledBackButton = filledBackButton,
                actions = {
                    HomeIconButton(onClick = { onNavigate(ScreenType.Home) })
                },
            )
        },
        bottomBar = {
            AppBottomNavigation(
                items = defaultAppBottomNavigationItems(),
                selectedItem = selectedNavItem,
                onItemSelected = onNavigate,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(AppDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
            content = content,
        )
    }
}
