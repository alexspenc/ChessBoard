package com.example.chessboard.ui.screen.training.common

/*
 * Shared top and bottom action-bar factory for training collection editors.
 *
 * Keep the common editor actions and optional training-specific add-ons here so
 * edit-training and edit-template screens can assemble near-identical bars
 * without duplicating button lists. Do not add screen state or persistence.
 */

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R
import com.example.chessboard.ui.components.BoardActionNavigationBar
import com.example.chessboard.ui.components.BoardActionNavigationItem
import com.example.chessboard.ui.components.HomeIconButton
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.theme.BottomBarContentColor


internal data class TrainingCollectionEditorBarStrings(
    val homeContentDescription: String,
    val editLabel: String,
    val editContentDescription: String,
    val deleteLabel: String,
    val deleteContentDescription: String,
    val backLabel: String,
    val previousMoveContentDescription: String,
    val forwardLabel: String,
    val nextMoveContentDescription: String,
)

@Composable
internal fun trainingCollectionEditorBarStrings(
    deleteContentDescription: String,
): TrainingCollectionEditorBarStrings {
    return TrainingCollectionEditorBarStrings(
        homeContentDescription = stringResource(R.string.common_home),
        editLabel = stringResource(R.string.common_edit),
        editContentDescription = stringResource(R.string.training_collection_editor_edit_line),
        deleteLabel = stringResource(R.string.common_delete),
        deleteContentDescription = deleteContentDescription,
        backLabel = stringResource(R.string.common_back),
        previousMoveContentDescription = stringResource(
            R.string.training_collection_editor_previous_move,
        ),
        forwardLabel = stringResource(R.string.common_forward),
        nextMoveContentDescription = stringResource(
            R.string.training_collection_editor_next_move,
        ),
    )
}

private data class TrainingCollectionEditorTopBarAction(
    val content: @Composable () -> Unit,
)

private data class TrainingCollectionEditorBottomBarAction(
    val label: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
    val content: @Composable (Boolean) -> Unit,
)

internal class TrainingCollectionEditorBarsFactory(
    onHomeClick: () -> Unit,
    hasSelection: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    strings: TrainingCollectionEditorBarStrings,
    canUndo: Boolean,
    onPrevClick: () -> Unit,
    canRedo: Boolean,
    onNextClick: () -> Unit,
) {
    private val topBarActions = mutableListOf(
        createHomeTopBarAction(
            onClick = onHomeClick,
            contentDescription = strings.homeContentDescription,
        ),
    )
    private val bottomBarActions = mutableListOf(
        createEditBottomBarAction(
            label = strings.editLabel,
            contentDescription = strings.editContentDescription,
            enabled = hasSelection,
            onClick = onEditClick,
        ),
        createDeleteBottomBarAction(
            label = strings.deleteLabel,
            enabled = hasSelection,
            onClick = onDeleteClick,
            contentDescription = strings.deleteContentDescription,
        ),
        createPrevBottomBarAction(
            label = strings.backLabel,
            contentDescription = strings.previousMoveContentDescription,
            enabled = canUndo,
            onClick = onPrevClick,
        ),
        createNextBottomBarAction(
            label = strings.forwardLabel,
            contentDescription = strings.nextMoveContentDescription,
            enabled = canRedo,
            onClick = onNextClick,
        ),
    )

    fun addTopBarAction(
        index: Int = -1,
        content: @Composable () -> Unit,
    ): TrainingCollectionEditorBarsFactory {
        topBarActions.insertAt(
            index = index,
            value = TrainingCollectionEditorTopBarAction(content = content),
        )

        return this
    }

    fun addBottomBarAction(
        label: String,
        enabled: Boolean,
        onClick: () -> Unit,
        index: Int = -1,
        content: @Composable (Boolean) -> Unit,
    ): TrainingCollectionEditorBarsFactory {
        bottomBarActions.insertAt(
            index = index,
            value = TrainingCollectionEditorBottomBarAction(
                label = label,
                enabled = enabled,
                onClick = onClick,
                content = content,
            ),
        )

        return this
    }

    fun buildTopBarActions(): @Composable () -> Unit {
        val actions = topBarActions.toList()
        return {
            TrainingCollectionEditorTopBarActions(actions = actions)
        }
    }

    fun buildBottomBar(): @Composable () -> Unit {
        val actions = bottomBarActions.toList()
        return {
            TrainingCollectionEditorBottomBar(actions = actions)
        }
    }
}

private fun <T> MutableList<T>.insertAt(index: Int, value: T) {
    if (index in 0..size) {
        add(index, value)
        return
    }

    add(value)
}

@Composable
private fun TrainingCollectionEditorTopBarActions(
    actions: List<TrainingCollectionEditorTopBarAction>,
) {
    actions.forEach { action ->
        action.content()
    }
}

@Composable
private fun TrainingCollectionEditorBottomBar(
    actions: List<TrainingCollectionEditorBottomBarAction>,
    modifier: Modifier = Modifier,
    maxVisibleItems: Int = 5,
) {
    BoardActionNavigationBar(
        modifier = modifier,
        maxVisibleItems = maxVisibleItems,
        items = actions.map { action ->
            BoardActionNavigationItem(
                label = action.label,
                enabled = action.enabled,
                onClick = action.onClick,
            ) {
                action.content(action.enabled)
            }
        },
    )
}

private fun createHomeTopBarAction(
    onClick: () -> Unit,
    contentDescription: String,
): TrainingCollectionEditorTopBarAction {
    return TrainingCollectionEditorTopBarAction {
        HomeIconButton(
            onClick = onClick,
            contentDescription = contentDescription,
        )
    }
}

private fun createEditBottomBarAction(
    label: String,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
): TrainingCollectionEditorBottomBarAction {
    return TrainingCollectionEditorBottomBarAction(
        label = label,
        enabled = enabled,
        onClick = onClick,
    ) { isEnabled ->
        IconMd(
            imageVector = Icons.Default.Edit,
            contentDescription = contentDescription,
            tint = resolveTrainingCollectionEditorActionTint(isEnabled),
        )
    }
}

private fun createDeleteBottomBarAction(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
): TrainingCollectionEditorBottomBarAction {
    return TrainingCollectionEditorBottomBarAction(
        label = label,
        enabled = enabled,
        onClick = onClick,
    ) { isEnabled ->
        IconMd(
            imageVector = Icons.Default.Delete,
            contentDescription = contentDescription,
            tint = resolveTrainingCollectionEditorActionTint(isEnabled),
        )
    }
}

private fun createPrevBottomBarAction(
    label: String,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
): TrainingCollectionEditorBottomBarAction {
    return TrainingCollectionEditorBottomBarAction(
        label = label,
        enabled = enabled,
        onClick = onClick,
    ) { isEnabled ->
        IconMd(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = contentDescription,
            tint = resolveTrainingCollectionEditorActionTint(isEnabled),
        )
    }
}

private fun createNextBottomBarAction(
    label: String,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
): TrainingCollectionEditorBottomBarAction {
    return TrainingCollectionEditorBottomBarAction(
        label = label,
        enabled = enabled,
        onClick = onClick,
    ) { isEnabled ->
        IconMd(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = contentDescription,
            tint = resolveTrainingCollectionEditorActionTint(isEnabled),
        )
    }
}

private fun resolveTrainingCollectionEditorActionTint(enabled: Boolean): Color {
    if (enabled) {
        return BottomBarContentColor
    }

    return BottomBarContentColor.copy(alpha = 0.5f)
}
