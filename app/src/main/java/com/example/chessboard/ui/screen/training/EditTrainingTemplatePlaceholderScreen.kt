package com.example.chessboard.ui.screen.training

/**
 * Placeholder screen for training template editing.
 *
 * Replace this screen with the real template editor once the editing flow is implemented.
 */

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.screen.ScreenContainerContext

@Composable
fun EditTrainingTemplatePlaceholderScreenContainer(
    templateId: Long,
    screenContext: ScreenContainerContext,
    modifier: Modifier = Modifier,
) {
    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = "Edit Template",
                onBackClick = screenContext.onBackClick,
                filledBackButton = true,
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            ScreenSection {
                BodySecondaryText(
                    text = "Template editor is not implemented yet.\nTemplate ID: $templateId",
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
