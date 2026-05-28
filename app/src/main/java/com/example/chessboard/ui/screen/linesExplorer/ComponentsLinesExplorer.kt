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
    val label: String,
) {
    ANY(sideMask = null, label = "Any"),
    WHITE(sideMask = SideMask.WHITE, label = "White"),
    BLACK(sideMask = SideMask.BLACK, label = "Black");

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
                    text = parsedLine.line.event ?: "Opening"
                )
                LineBlockMetaRow(
                    eco = parsedLine.line.eco,
                    lineId = parsedLine.line.id
                )
            }
            CardMetaText(text = "${parsedLine.moveLabels.size} moves")
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
                    label = "Edit",
                    enabled = hasSelection,
                    onClick = onEditClick,
                ) {
                    IconMd(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit line",
                        tint = resolveLinesExplorerActionTint(hasSelection),
                    )
                },
                BoardActionNavigationItem(
                    label = "Delete",
                    enabled = hasSelection,
                    onClick = onDeleteClick,
                ) {
                    IconMd(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete line",
                        tint = resolveLinesExplorerActionTint(hasSelection),
                    )
                },
                BoardActionNavigationItem(
                    label = "Back",
                    enabled = canUndo,
                    onClick = onPrevClick,
                ) {
                    IconMd(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous",
                        tint = resolveLinesExplorerActionTint(canUndo),
                    )
                },
                BoardActionNavigationItem(
                    label = "Forward",
                    enabled = canRedo,
                    onClick = onNextClick,
                ) {
                    IconMd(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next",
                        tint = resolveLinesExplorerActionTint(canRedo),
                    )
                },
            )
        } else listOf(
            BoardActionNavigationItem(
                label = "Menu",
                enabled = hasLineActions,
                onClick = onLineActionsClick,
            ) {
                IconMd(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Line actions",
                    tint = resolveLinesExplorerActionTint(hasLineActions),
                )
            },
            BoardActionNavigationItem(
                label = "Edit",
                enabled = hasSelection,
                onClick = onEditClick,
            ) {
                IconMd(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit line",
                    tint = resolveLinesExplorerActionTint(hasSelection),
                )
            },
            BoardActionNavigationItem(
                label = "Delete",
                enabled = hasSelection,
                onClick = onDeleteClick,
            ) {
                IconMd(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete line",
                    tint = resolveLinesExplorerActionTint(hasSelection),
                )
            },
            BoardActionNavigationItem(
                label = "Back",
                enabled = canUndo,
                onClick = onPrevClick,
            ) {
                IconMd(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous",
                    tint = resolveLinesExplorerActionTint(canUndo),
                )
            },
            BoardActionNavigationItem(
                label = "Forward",
                enabled = canRedo,
                onClick = onNextClick,
            ) {
                IconMd(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next",
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
            SectionTitleText(text = "Line Actions")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)
            ) {
                LinesExplorerDialogAction(
                    label = "Export PGN",
                    action = copyLinesPgnAction,
                ) { tint ->
                    IconMd(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Export lines PGN",
                        tint = tint,
                    )
                }
                LinesExplorerDialogAction(
                    label = "Create Training",
                    action = createTrainingAction,
                ) { tint ->
                    IconMd(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = "Create training",
                        tint = tint,
                    )
                }
                LinesExplorerDialogAction(
                    label = "Reset",
                    action = resetAction,
                ) { tint ->
                    IconMd(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = tint,
                    )
                }
                LinesExplorerDialogAction(
                    label = "Analyze",
                    action = analyzeAction,
                ) { tint ->
                    IconMd(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Analyze line",
                        tint = tint,
                    )
                }
                LinesExplorerDialogAction(
                    label = "Clone",
                    action = cloneAction,
                ) { tint ->
                    IconMd(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Clone line",
                        tint = tint,
                    )
                }
                LinesExplorerDialogAction(
                    label = "Delete Lines",
                    action = deleteExplorerLinesAction,
                    isDestructive = true,
                ) { tint ->
                    IconMd(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Delete explorer lines",
                        tint = tint,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                CardMetaText(text = "Cancel")
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

        CardMetaText(text = "ID: $lineId")
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
            SectionTitleText(text = "Search Lines")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
            ) {
                AppTextField(
                    value = filterState.query,
                    onValueChange = ::updateQuery,
                    label = "Line name",
                    placeholder = "Enter part of the title"
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
                            text = "Case sensitive",
                            color = TextColor.Primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        CardMetaText(
                            text = "Match uppercase and lowercase exactly"
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
                            text = "Dubious lines",
                            color = TextColor.Primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        CardMetaText(
                            text = "Search only lines marked as dubious"
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
                text = "Apply",
                onClick = onApplyClick
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                CardMetaText(text = "Cancel")
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
            text = "Side",
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
                text = sideFilter.label,
                color = tint,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
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
