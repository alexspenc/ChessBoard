package com.example.chessboard.ui.screen.training.common

/*
 * Shared collection-editor shell for training-like screens.
 *
 * Keep the reusable scaffold, name field, optional header slot, games count,
 * and list container here so training and template editors can share the same
 * screen shell. Do not add training-only launch logic or persistence helpers.
 */

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.chessboard.ui.EditTrainingListTestTag
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTextField
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TrainingAccentTeal

internal data class TrainingCollectionEditorStrings(
    val screenTitle: String,
    val collectionNameLabel: String,
    val collectionNamePlaceholder: String,
    val gamesCountLabel: String,
)

@Composable
internal fun TrainingCollectionEditorScreen(
    strings: TrainingCollectionEditorStrings,
    collectionName: String,
    onCollectionNameChange: (String) -> Unit,
    games: List<TrainingGameEditorItem>,
    selectedNavItem: ScreenType,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    onNavigate: (ScreenType) -> Unit,
    modifier: Modifier = Modifier,
    simpleViewEnabled: Boolean = false,
    listTestTag: String = EditTrainingListTestTag,
    autoScrollToGameIndex: Int? = null,
    headerContent: (@Composable () -> Unit)? = null,
    topBarActions: @Composable () -> Unit = {},
    gameItemContent: @Composable LazyItemScope.(TrainingGameEditorItem) -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(autoScrollToGameIndex, headerContent) {
        val targetIndex = autoScrollToGameIndex ?: return@LaunchedEffect
        listState.animateScrollToItem(
            targetIndex + resolveTrainingEditorHeaderCount(headerContent)
        )
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = strings.screenTitle,
                onBackClick = onBackClick,
                actions = {
                    topBarActions()
                    if (!simpleViewEnabled) {
                        IconButton(onClick = onSaveClick) {
                            IconMd(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Save",
                                tint = TrainingAccentTeal,
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            AppBottomNavigation(
                items = defaultAppBottomNavigationItems(),
                selectedItem = selectedNavItem,
                onItemSelected = onNavigate
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag(listTestTag),
            contentPadding = PaddingValues(
                start = AppDimens.spaceLg,
                end = AppDimens.spaceLg,
                top = AppDimens.spaceLg,
                bottom = AppDimens.spaceLg
            ),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            item {
                AppTextField(
                    value = collectionName,
                    onValueChange = onCollectionNameChange,
                    label = strings.collectionNameLabel,
                    placeholder = strings.collectionNamePlaceholder
                )
            }

            if (headerContent != null) {
                item {
                    headerContent()
                }
            }

            item {
                BodySecondaryText(text = "${strings.gamesCountLabel}: ${games.size}")
            }

            items(
                items = games,
                key = { game -> game.gameId },
                itemContent = gameItemContent
            )
        }
    }
}

private fun resolveTrainingEditorHeaderCount(
    headerContent: (@Composable () -> Unit)?
): Int {
    if (headerContent == null) {
        return 2
    }

    return 3
}
