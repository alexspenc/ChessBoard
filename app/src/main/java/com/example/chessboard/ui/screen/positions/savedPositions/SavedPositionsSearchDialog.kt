package com.example.chessboard.ui.screen.positions.savedPositions

/**
 * Search dialog and filter helpers for saved positions.
 *
 * Keep saved-position search UI and name matching here. Do not add database calls or navigation decisions.
 */
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.chessboard.ui.SavedPositionsSearchNameFieldTestTag
import com.example.chessboard.ui.components.AppTextField
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TextColor

internal data class SavedPositionsFilterState(
    val query: String = "",
    val isCaseSensitive: Boolean = false,
)

internal fun matchesSavedPositionsFilter(
    position: SavedPositionListItem,
    filterState: SavedPositionsFilterState,
): Boolean {
    if (filterState.query.isBlank()) {
        return true
    }

    if (filterState.isCaseSensitive) {
        return position.name.contains(filterState.query)
    }

    return position.name.contains(filterState.query, ignoreCase = true)
}

@Composable
internal fun RenderSavedPositionsSearchDialog(
    visible: Boolean,
    filterState: SavedPositionsFilterState,
    actions: SavedPositionsSearchActions,
    strings: SavedPositionsSearchStrings,
) {
    if (!visible) {
        return
    }

    fun updateQuery(query: String) {
        actions.onDraftFilterStateChange(
            filterState.copy(query = query)
        )
    }

    fun updateCaseSensitive(isCaseSensitive: Boolean) {
        actions.onDraftFilterStateChange(
            filterState.copy(isCaseSensitive = isCaseSensitive)
        )
    }

    AlertDialog(
        onDismissRequest = { actions.onDialogVisibilityChange(false) },
        containerColor = Background.ScreenDark,
        title = {
            SectionTitleText(text = strings.searchDialogTitle)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
            ) {
                AppTextField(
                    value = filterState.query,
                    onValueChange = ::updateQuery,
                    label = strings.searchNameLabel,
                    placeholder = strings.searchNamePlaceholder,
                    inputTestTag = SavedPositionsSearchNameFieldTestTag,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = strings.searchCaseSensitiveTitle,
                            color = TextColor.Primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        CardMetaText(
                            text = strings.searchCaseSensitiveSubtitle
                        )
                    }
                    Checkbox(
                        checked = filterState.isCaseSensitive,
                        onCheckedChange = ::updateCaseSensitive
                    )
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = strings.applyAction,
                onClick = actions.onApplyFilter
            )
        },
        dismissButton = {
            TextButton(onClick = { actions.onDialogVisibilityChange(false) }) {
                CardMetaText(text = strings.cancelAction)
            }
        }
    )
}
