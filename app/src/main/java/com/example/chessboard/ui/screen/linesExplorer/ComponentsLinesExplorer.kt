package com.example.chessboard.ui.screen.linesExplorer

/**
 * Visual blocks and search helpers for the lines-explorer package.
 *
 * Keep in this file:
 * - render-only components used by the lines explorer screen
 * - search dialog UI and filter matching helpers for lines explorer
 * - small package-local models that support the explorer UI
 *
 * It is acceptable to add here:
 * - new reusable UI blocks for the lines explorer package
 * - search-related helpers used only by files in this package
 *
 * Do not add here:
 * - database calls, coroutine orchestration, or navigation decisions
 * - logic for unrelated screens
 * - broad app-wide UI utilities
 */
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.chessboard.R
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.entity.SideMask
import com.example.chessboard.service.ParsedLine
import com.example.chessboard.ui.components.AppIconSizes
import com.example.chessboard.ui.components.AppTextField
import com.example.chessboard.ui.components.BoardActionNavigationBar
import com.example.chessboard.ui.components.BoardActionNavigationItem
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.components.LineMoveTreeSection
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.BottomBarContentColor
import com.example.chessboard.ui.theme.MutedContentColor
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingErrorRed

internal enum class LinesExplorerSideFilter(
    val sideMask: Int?,
) {
    ANY(sideMask = null),
    WHITE(sideMask = SideMask.WHITE),
    BLACK(sideMask = SideMask.BLACK);

    companion object {
        fun fromSideMask(sideMask: Int?): LinesExplorerSideFilter {
            if (sideMask == SideMask.WHITE) {
                return WHITE
            }

            if (sideMask == SideMask.BLACK) {
                return BLACK
            }

            return ANY
        }
    }
}

internal data class LinesExplorerFilterState(
    val query: String = "",
    val isCaseSensitive: Boolean = false,
    val dubiousOnly: Boolean = false,
    val sideFilter: LinesExplorerSideFilter = LinesExplorerSideFilter.ANY,
)

internal data class CallbackWithCfg(
    val canUse: Boolean,
    val onClick: () -> Unit,
)

@Composable
internal fun LineBlock(
    parsedLine: ParsedLine,
    isSelected: Boolean,
    lineController: LineController,
    onSelectClick: () -> Unit,
    onMovePlyClick: (ply: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isSelected) {
        LineMoveTreeSection(
            importedUciLines = listOf(parsedLine.uciMoves),
            lineController = lineController,
            modifier = modifier.fillMaxWidth(),
            onMoveSelected = { _, targetPly -> onMovePlyClick(targetPly) },
        )
        return
    }

    CardSurface(
        modifier = modifier.fillMaxWidth(),
        color = Background.SurfaceDark,
        border = null,
        onClick = onSelectClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SectionTitleText(
                    text = parsedLine.line.event ?: stringResource(R.string.lines_explorer_default_line_name)
                )
                LineBlockMetaRow(
                    eco = parsedLine.line.eco,
                    lineId = parsedLine.line.id
                )
            }
            CardMetaText(
                text = pluralStringResource(
                    R.plurals.lines_explorer_moves_count,
                    parsedLine.moveLabels.size,
                    parsedLine.moveLabels.size,
                )
            )
        }
    }
}

@Composable
internal fun LinesExplorerBoardControlsBar(
    canUndo: Boolean,
    canRedo: Boolean,
    hasSelection: Boolean,
    hasLineActions: Boolean,
    simpleViewEnabled: Boolean = false,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onLineActionsClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    BoardActionNavigationBar(
        maxVisibleItems = if (simpleViewEnabled) 4 else 5,
        items = if (simpleViewEnabled) {
            listOf(
                BoardActionNavigationItem(
                    label = stringResource(R.string.lines_explorer_action_edit),
                    enabled = hasSelection,
                    onClick = onEditClick,
                ) {
                    IconMd(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.lines_explorer_edit_line),
                        tint = resolveLinesExplorerActionTint(hasSelection),
                    )
                },
                BoardActionNavigationItem(
                    label = stringResource(R.string.common_delete),
                    enabled = hasSelection,
                    onClick = onDeleteClick,
                ) {
                    IconMd(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.lines_explorer_delete_line),
                        tint = resolveLinesExplorerActionTint(hasSelection),
                    )
                },
                BoardActionNavigationItem(
                    label = stringResource(R.string.common_back),
                    enabled = canUndo,
                    onClick = onPrevClick,
                ) {
                    IconMd(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.common_previous),
                        tint = resolveLinesExplorerActionTint(canUndo),
                    )
                },
                BoardActionNavigationItem(
                    label = stringResource(R.string.common_forward),
                    enabled = canRedo,
                    onClick = onNextClick,
                ) {
                    IconMd(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.common_next),
                        tint = resolveLinesExplorerActionTint(canRedo),
                    )
                },
            )
        } else listOf(
            BoardActionNavigationItem(
                label = stringResource(R.string.common_menu),
                enabled = hasLineActions,
                onClick = onLineActionsClick,
            ) {
                IconMd(
                    imageVector = Icons.Default.Menu,
                    contentDescription = stringResource(R.string.lines_explorer_line_actions),
                    tint = resolveLinesExplorerActionTint(hasLineActions),
                )
            },
            BoardActionNavigationItem(
                label = stringResource(R.string.lines_explorer_action_edit),
                enabled = hasSelection,
                onClick = onEditClick,
            ) {
                IconMd(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.lines_explorer_edit_line),
                    tint = resolveLinesExplorerActionTint(hasSelection),
                )
            },
            BoardActionNavigationItem(
                label = stringResource(R.string.common_delete),
                enabled = hasSelection,
                onClick = onDeleteClick,
            ) {
                IconMd(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.lines_explorer_delete_line),
                    tint = resolveLinesExplorerActionTint(hasSelection),
                )
            },
            BoardActionNavigationItem(
                label = stringResource(R.string.common_back),
                enabled = canUndo,
                onClick = onPrevClick,
            ) {
                IconMd(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.common_previous),
                    tint = resolveLinesExplorerActionTint(canUndo),
                )
            },
            BoardActionNavigationItem(
                label = stringResource(R.string.common_forward),
                enabled = canRedo,
                onClick = onNextClick,
            ) {
                IconMd(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.common_next),
                    tint = resolveLinesExplorerActionTint(canRedo),
                )
            },
        ),
    )
}

@Composable
internal fun RenderLinesExplorerLineActionsDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    resetAction: CallbackWithCfg,
    analyzeAction: CallbackWithCfg,
    cloneAction: CallbackWithCfg,
    createTrainingAction: CallbackWithCfg,
    copyLinesPgnAction: CallbackWithCfg,
    deleteExplorerLinesAction: CallbackWithCfg,
) {
    if (!visible) {
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Background.ScreenDark,
        title = {
            SectionTitleText(text = stringResource(R.string.lines_explorer_line_actions))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)
            ) {
                LinesExplorerDialogAction(
                    label = stringResource(R.string.lines_explorer_export_pgn),
                    action = copyLinesPgnAction,
                ) { tint ->
                    IconMd(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = stringResource(R.string.lines_explorer_export_lines_pgn),
                        tint = tint,
                    )
                }
                LinesExplorerDialogAction(
                    label = stringResource(R.string.home_create_training_title),
                    action = createTrainingAction,
                ) { tint ->
                    IconMd(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = stringResource(R.string.lines_explorer_create_training),
                        tint = tint,
                    )
                }
                LinesExplorerDialogAction(
                    label = stringResource(R.string.common_reset),
                    action = resetAction,
                ) { tint ->
                    IconMd(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.common_reset),
                        tint = tint,
                    )
                }
                LinesExplorerDialogAction(
                    label = stringResource(R.string.lines_explorer_analyze),
                    action = analyzeAction,
                ) { tint ->
                    IconMd(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = stringResource(R.string.lines_explorer_analyze_line),
                        tint = tint,
                    )
                }
                LinesExplorerDialogAction(
                    label = stringResource(R.string.lines_explorer_clone),
                    action = cloneAction,
                ) { tint ->
                    IconMd(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.lines_explorer_clone_line),
                        tint = tint,
                    )
                }
                LinesExplorerDialogAction(
                    label = stringResource(R.string.lines_explorer_delete_lines_title),
                    action = deleteExplorerLinesAction,
                    isDestructive = true,
                ) { tint ->
                    IconMd(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = stringResource(R.string.lines_explorer_delete_explorer_lines),
                        tint = tint,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                CardMetaText(text = stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun LinesExplorerDialogAction(
    label: String,
    action: CallbackWithCfg,
    isDestructive: Boolean = false,
    icon: @Composable (Color) -> Unit
) {
    val actionTint = resolveLinesExplorerDialogActionTint(
        isEnabled = action.canUse,
        isDestructive = isDestructive,
    )

    TextButton(
        onClick = action.onClick,
        enabled = action.canUse,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon(actionTint)
            Text(
                text = label,
                color = actionTint,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun resolveLinesExplorerDialogActionTint(
    isEnabled: Boolean,
    isDestructive: Boolean = false,
): Color {
    if (!isEnabled) {
        return TextColor.Primary.copy(alpha = 0.5f)
    }

    if (isDestructive) {
        return TrainingErrorRed
    }

    return TextColor.Primary
}

private fun resolveLinesExplorerActionTint(isEnabled: Boolean): Color {
    if (isEnabled) {
        return MutedContentColor
    }

    return MutedContentColor.copy(alpha = 0.5f)
}

@Composable
private fun LineBlockMetaRow(
    eco: String?,
    lineId: Long
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!eco.isNullOrBlank()) {
            CardMetaText(text = eco)
        }

        CardMetaText(text = stringResource(R.string.common_id, lineId))
    }
}

@Composable
internal fun RenderLinesExplorerSearchDialog(
    visible: Boolean,
    filterState: LinesExplorerFilterState,
    onDismiss: () -> Unit,
    onFilterStateChange: (LinesExplorerFilterState) -> Unit,
    onApplyClick: () -> Unit
) {
    if (!visible) {
        return
    }

    fun updateQuery(query: String) {
        onFilterStateChange(
            filterState.copy(query = query)
        )
    }

    fun updateCaseSensitive(isCaseSensitive: Boolean) {
        onFilterStateChange(
            filterState.copy(isCaseSensitive = isCaseSensitive)
        )
    }

    fun updateDubiousOnly(dubiousOnly: Boolean) {
        onFilterStateChange(
            filterState.copy(dubiousOnly = dubiousOnly)
        )
    }

    fun updateSideFilter(sideFilter: LinesExplorerSideFilter) {
        onFilterStateChange(
            filterState.copy(sideFilter = sideFilter)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Background.ScreenDark,
        title = {
            SectionTitleText(text = stringResource(R.string.lines_explorer_search_title))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
            ) {
                AppTextField(
                    value = filterState.query,
                    onValueChange = ::updateQuery,
                    label = stringResource(R.string.lines_explorer_line_name),
                    placeholder = stringResource(R.string.lines_explorer_line_name_placeholder)
                )

                LinesExplorerSideFilterSelector(
                    selectedSideFilter = filterState.sideFilter,
                    onSideFilterChange = ::updateSideFilter,
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
                            text = stringResource(R.string.lines_explorer_case_sensitive),
                            color = TextColor.Primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        CardMetaText(
                            text = stringResource(R.string.lines_explorer_case_sensitive_subtitle)
                        )
                    }
                    Checkbox(
                        checked = filterState.isCaseSensitive,
                        onCheckedChange = ::updateCaseSensitive
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.lines_explorer_dubious_lines),
                            color = TextColor.Primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        CardMetaText(
                            text = stringResource(R.string.lines_explorer_dubious_lines_subtitle)
                        )
                    }
                    Checkbox(
                        checked = filterState.dubiousOnly,
                        onCheckedChange = ::updateDubiousOnly
                    )
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = stringResource(R.string.common_apply),
                onClick = onApplyClick
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                CardMetaText(text = stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun LinesExplorerSideFilterSelector(
    selectedSideFilter: LinesExplorerSideFilter,
    onSideFilterChange: (LinesExplorerSideFilter) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
    ) {
        Text(
            text = stringResource(R.string.lines_explorer_side),
            color = TextColor.Primary,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LinesExplorerSideFilter.entries.forEach { sideFilter ->
                LinesExplorerSideFilterButton(
                    sideFilter = sideFilter,
                    selected = sideFilter == selectedSideFilter,
                    onClick = { onSideFilterChange(sideFilter) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun LinesExplorerSideFilterButton(
    sideFilter: LinesExplorerSideFilter,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = resolveLinesExplorerSideFilterTint(selected)

    TextButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
        ) {
            LinesExplorerSideFilterIcon(
                sideFilter = sideFilter,
                tint = tint,
            )
            Text(
                text = resolveLinesExplorerSideFilterLabel(sideFilter),
                color = tint,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun resolveLinesExplorerSideFilterLabel(sideFilter: LinesExplorerSideFilter): String {
    return when (sideFilter) {
        LinesExplorerSideFilter.ANY -> stringResource(R.string.lines_explorer_side_any)
        LinesExplorerSideFilter.WHITE -> stringResource(R.string.lines_explorer_side_white)
        LinesExplorerSideFilter.BLACK -> stringResource(R.string.lines_explorer_side_black)
    }
}

@Composable
private fun LinesExplorerSideFilterIcon(
    sideFilter: LinesExplorerSideFilter,
    tint: Color,
) {
    if (sideFilter == LinesExplorerSideFilter.ANY) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LinesExplorerKingIcon(tint = tint)
            LinesExplorerKingIcon(tint = tint)
        }
        return
    }

    Box(contentAlignment = Alignment.Center) {
        LinesExplorerKingIcon(tint = tint)
    }
}

@Composable
private fun LinesExplorerKingIcon(tint: Color) {
    Icon(
        painter = painterResource(R.drawable.ic_king),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(AppIconSizes.Md),
    )
}

private fun resolveLinesExplorerSideFilterTint(selected: Boolean): Color {
    if (selected) {
        return TrainingAccentTeal
    }

    return BottomBarContentColor
}
