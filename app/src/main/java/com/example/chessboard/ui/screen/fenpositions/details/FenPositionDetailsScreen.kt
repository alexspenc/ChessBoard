package com.example.chessboard.ui.screen.fenpositions.details

/*
 * File role: renders the FEN position details screen and its local expansion state.
 * Allowed here:
 * - loading/error/content presentation, read-only board, description, and continuation sections
 * - forwarding required back, adjacent-position navigation, edit, delete, add-continuation, and FEN-copy actions
 * Not allowed here:
 * - service calls, persistence mutations, app-wide navigation, or continuation storage
 * Validation date: 2026-09-02
 */

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.ChessBoardSection
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.components.ScreenTitleText
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.screen.fenpositions.resolveFenPositionBoardOrientation
import com.example.chessboard.ui.screen.fenpositions.toLoadableFenPosition
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsBoardTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsContentTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsContinuationsHeaderTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsDescriptionBodyTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsDescriptionCollapseTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsDescriptionHeaderTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsLoadFailedTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsLoadingTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsNextPositionTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsNotFoundTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsPreviousPositionTestTag
import com.example.chessboard.ui.testtags.fenpositions.FenPositionDetailsScreenTestTag
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.MutedContentColor
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal

internal sealed interface FenPositionDetailsUiState {
    data object Loading : FenPositionDetailsUiState
    data object NotFound : FenPositionDetailsUiState
    data object LoadFailed : FenPositionDetailsUiState
    data class Content(val position: FenPositionDetailsItem) : FenPositionDetailsUiState
}

internal data class FenPositionDetailsItem(
    val id: Long,
    val fen: String,
    val name: String,
    val theme: String,
    val description: String?,
    val continuationSanLines: List<String>,
    val catalogIndex: Int,
    val previousPositionId: Long?,
    val nextPositionId: Long?,
)

@Composable
internal fun FenPositionDetailsScreen(
    uiState: FenPositionDetailsUiState,
    onBackClick: () -> Unit,
    onPreviousPositionClick: () -> Unit,
    onNextPositionClick: () -> Unit,
    onEditPositionClick: () -> Unit,
    onDeletePositionClick: () -> Unit,
    onAddContinuationClick: () -> Unit,
    onCopyFenClick: () -> Unit,
    canCopyFen: Boolean,
    modifier: Modifier = Modifier,
) {
    val strings = fenPositionDetailsStrings()
    val contentState = uiState as? FenPositionDetailsUiState.Content

    @Composable
    fun DetailsTopBar() {
        fun resolveArrowTint(isEnabled: Boolean): Color {
            if (!isEnabled) {
                return MutedContentColor
            }

            return TextColor.Primary
        }

        val canOpenPreviousPosition = contentState?.position?.previousPositionId != null
        val canOpenNextPosition = contentState?.position?.nextPositionId != null
        AppTopBar(
            title = strings.screenTitle,
            onBackClick = onBackClick,
            handleSystemBack = true,
            filledBackButton = true,
            actions = {
                IconButton(
                    onClick = onPreviousPositionClick,
                    enabled = canOpenPreviousPosition,
                    modifier = Modifier.testTag(FenPositionDetailsPreviousPositionTestTag),
                ) {
                    IconMd(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = strings.previousPositionContentDescription,
                        tint = resolveArrowTint(canOpenPreviousPosition),
                    )
                }
                IconButton(
                    onClick = onNextPositionClick,
                    enabled = canOpenNextPosition,
                    modifier = Modifier.testTag(FenPositionDetailsNextPositionTestTag),
                ) {
                    IconMd(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = strings.nextPositionContentDescription,
                        tint = resolveArrowTint(canOpenNextPosition),
                    )
                }
            },
        )
    }

    AppScreenScaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag(FenPositionDetailsScreenTestTag),
        topBar = {
            DetailsTopBar()
        },
        bottomBar = {
            if (contentState != null) {
                FenPositionDetailsBottomBar(
                    editContentDescription = strings.editPositionContentDescription,
                    deleteContentDescription = strings.deletePositionContentDescription,
                    addContinuationContentDescription = strings.addContinuationContentDescription,
                    copyFenContentDescription = strings.copyFenContentDescription,
                    canCopyFen = canCopyFen,
                    onEditClick = onEditPositionClick,
                    onDeleteClick = onDeletePositionClick,
                    onAddContinuationClick = onAddContinuationClick,
                    onCopyFenClick = onCopyFenClick,
                )
            }
        },
    ) { paddingValues ->
        when (uiState) {
            FenPositionDetailsUiState.Loading -> FenPositionDetailsLoadingContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )

            FenPositionDetailsUiState.NotFound -> FenPositionDetailsStateMessage(
                text = strings.notFound,
                testTag = FenPositionDetailsNotFoundTestTag,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )

            FenPositionDetailsUiState.LoadFailed -> FenPositionDetailsStateMessage(
                text = strings.loadFailed,
                testTag = FenPositionDetailsLoadFailedTestTag,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )

            is FenPositionDetailsUiState.Content -> key(uiState.position.id) {
                FenPositionDetailsContent(
                    position = uiState.position,
                    strings = strings,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }
        }
    }
}

@Composable
private fun FenPositionDetailsLoadingContent(modifier: Modifier) {
    Box(
        modifier = modifier.testTag(FenPositionDetailsLoadingTestTag),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = TrainingAccentTeal)
    }
}

@Composable
private fun FenPositionDetailsStateMessage(
    text: String,
    testTag: String,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .padding(AppDimens.spaceLg)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        BodySecondaryText(text = text)
    }
}

@Composable
private fun FenPositionDetailsContent(
    position: FenPositionDetailsItem,
    strings: FenPositionDetailsStrings,
    modifier: Modifier,
) {
    val lineController = remember(position.id, position.fen) {
        LineController().also { controller ->
            controller.loadPreviewFen(toLoadableFenPosition(position.fen))
            controller.setOrientation(resolveFenPositionBoardOrientation(position.fen))
            controller.setUserMovesEnabled(false)
        }
    }
    var descriptionExpanded by rememberSaveable(position.id) { mutableStateOf(false) }
    var continuationsExpanded by rememberSaveable(position.id) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = AppDimens.spaceLg,
                vertical = AppDimens.spaceLg,
            )
            .testTag(FenPositionDetailsContentTestTag),
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)) {
            ScreenTitleText(text = strings.name(position.name))
            CardMetaText(text = strings.theme(position.theme))
        }

        ChessBoardSection(
            lineController = lineController,
            modifier = Modifier.testTag(FenPositionDetailsBoardTestTag),
        )

        FenPositionDescriptionSection(
            description = position.description,
            expanded = descriptionExpanded,
            strings = strings,
            onToggle = {
                descriptionExpanded = !descriptionExpanded
            },
            onCollapse = {
                descriptionExpanded = false
            },
        )

        FenPositionContinuationsSection(
            sanLines = position.continuationSanLines,
            expanded = continuationsExpanded,
            strings = strings,
            onToggle = {
                continuationsExpanded = !continuationsExpanded
            },
        )
    }
}

@Composable
private fun FenPositionDescriptionSection(
    description: String?,
    expanded: Boolean,
    strings: FenPositionDetailsStrings,
    onToggle: () -> Unit,
    onCollapse: () -> Unit,
) {
    CardSurface(modifier = Modifier.fillMaxWidth()) {
        FenPositionExpandableHeader(
            title = strings.descriptionTitle,
            expanded = expanded,
            expandContentDescription = strings.expandDescriptionContentDescription,
            collapseContentDescription = strings.collapseDescriptionContentDescription,
            testTag = FenPositionDetailsDescriptionHeaderTestTag,
            onToggle = onToggle,
        )
        if (!expanded) {
            return@CardSurface
        }

        Spacer(modifier = Modifier.height(AppDimens.spaceMd))
        Text(
            text = description?.takeIf { text -> text.isNotBlank() }
                ?: strings.descriptionAbsent,
            modifier = Modifier.testTag(FenPositionDetailsDescriptionBodyTestTag),
            style = MaterialTheme.typography.bodyMedium,
            color = TextColor.Primary,
        )
        TextButton(
            onClick = onCollapse,
            modifier = Modifier
                .align(Alignment.End)
                .testTag(FenPositionDetailsDescriptionCollapseTestTag),
        ) {
            Text(text = strings.collapse)
        }
    }
}

@Composable
private fun FenPositionContinuationsSection(
    sanLines: List<String>,
    expanded: Boolean,
    strings: FenPositionDetailsStrings,
    onToggle: () -> Unit,
) {
    CardSurface(modifier = Modifier.fillMaxWidth()) {
        FenPositionExpandableHeader(
            title = strings.continuations(sanLines.size),
            expanded = expanded,
            expandContentDescription = strings.expandContinuationsContentDescription,
            collapseContentDescription = strings.collapseContinuationsContentDescription,
            testTag = FenPositionDetailsContinuationsHeaderTestTag,
            onToggle = onToggle,
        )
        if (!expanded) {
            return@CardSurface
        }

        sanLines.forEach { sanLine ->
            MonospaceContinuationText(text = sanLine)
        }
    }
}

@Composable
private fun MonospaceContinuationText(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(AppDimens.spaceMd),
        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        color = TextColor.Primary,
    )
}

@Composable
private fun FenPositionExpandableHeader(
    title: String,
    expanded: Boolean,
    expandContentDescription: String,
    collapseContentDescription: String,
    testTag: String,
    onToggle: () -> Unit,
) {
    var icon: ImageVector = Icons.Default.KeyboardArrowDown
    var iconContentDescription = expandContentDescription
    if (expanded) {
        icon = Icons.Default.KeyboardArrowUp
        iconContentDescription = collapseContentDescription
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionTitleText(
            text = title,
            modifier = Modifier.weight(1f),
        )
        IconMd(
            imageVector = icon,
            contentDescription = iconContentDescription,
            tint = TextColor.Primary,
        )
    }
}
