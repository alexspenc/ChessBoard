@file:Suppress("FunctionName")

package com.example.chessboard.ui.screen.gameOpeningAnalysis

/*
 * File role: renders the filter dialog for imported games on the game-opening analysis screen.
 * Allowed here:
 * - screen-specific filter controls for imported PGN games
 * - conversion between text input and GameOpeningAnalysisFilter values
 * Not allowed here:
 * - PGN parsing, runtime list mutation, database access, or analyzer execution
 * Validation date: 2026-06-27
 */

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chessboard.R
import com.example.chessboard.analysis.OpeningSide
import com.example.chessboard.runtimecontext.GameOpeningAnalysisFilter
import com.example.chessboard.ui.GameOpeningAnalysisFilterBlackSideTestTag
import com.example.chessboard.ui.GameOpeningAnalysisFilterCaseSensitiveTestTag
import com.example.chessboard.ui.GameOpeningAnalysisFilterDialogTestTag
import com.example.chessboard.ui.GameOpeningAnalysisFilterExactMatchTestTag
import com.example.chessboard.ui.GameOpeningAnalysisFilterMinPlyTestTag
import com.example.chessboard.ui.GameOpeningAnalysisFilterPlayerNameTestTag
import com.example.chessboard.ui.components.AppTextField
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.KingSideFilterMode
import com.example.chessboard.ui.components.KingSideFilterOption
import com.example.chessboard.ui.components.KingSideFilterSelector
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal

@Composable
internal fun GameOpeningAnalysisFilterDialog(
    visible: Boolean,
    filter: GameOpeningAnalysisFilter,
    onFilterChange: (GameOpeningAnalysisFilter) -> Unit,
    onDismiss: () -> Unit,
    onApplyClick: () -> Unit,
) {
    if (!visible) {
        return
    }

    fun updateSide(side: OpeningSide) {
        onFilterChange(filter.copy(side = side))
    }

    fun updatePlayerName(playerNameQuery: String) {
        onFilterChange(filter.copy(playerNameQuery = playerNameQuery))
    }

    fun updateMatchMode(matchMode: GameOpeningAnalysisFilter.PlayerNameMatchMode) {
        onFilterChange(filter.copy(playerNameMatchMode = matchMode))
    }

    fun updateCaseSensitive(isCaseSensitive: Boolean) {
        onFilterChange(filter.copy(isCaseSensitive = isCaseSensitive))
    }

    fun updateMinPly(input: String) {
        val digits = input.filter { character -> character.isDigit() }
        onFilterChange(filter.copy(minPly = digits.toIntOrNull()))
    }

    AlertDialog(
        modifier = Modifier.testTag(GameOpeningAnalysisFilterDialogTestTag),
        onDismissRequest = onDismiss,
        containerColor = Background.ScreenDark,
        title = {
            SectionTitleText(text = stringResource(R.string.game_opening_analysis_filter_title))
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
            ) {
                GameOpeningAnalysisPlayerFilterSection(
                    filter = filter,
                    onSideChange = ::updateSide,
                    onPlayerNameChange = ::updatePlayerName,
                    onMatchModeChange = ::updateMatchMode,
                    onCaseSensitiveChange = ::updateCaseSensitive,
                )
                AppTextField(
                    value = filter.minPly?.toString() ?: "",
                    onValueChange = ::updateMinPly,
                    label = stringResource(R.string.game_opening_analysis_filter_min_ply),
                    placeholder = stringResource(R.string.game_opening_analysis_filter_min_ply_placeholder),
                    inputTestTag = GameOpeningAnalysisFilterMinPlyTestTag,
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = stringResource(R.string.game_opening_analysis_filter_apply),
                onClick = onApplyClick,
                enabled = filter.playerNameQuery.isNotBlank(),
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                CardMetaText(text = stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun GameOpeningAnalysisPlayerFilterSection(
    filter: GameOpeningAnalysisFilter,
    onSideChange: (OpeningSide) -> Unit,
    onPlayerNameChange: (String) -> Unit,
    onMatchModeChange: (GameOpeningAnalysisFilter.PlayerNameMatchMode) -> Unit,
    onCaseSensitiveChange: (Boolean) -> Unit,
) {
    CardSurface(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, TrainingAccentTeal),
        contentPadding = PaddingValues(AppDimens.spaceMd),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
        ) {
            FilterSectionTitle(text = stringResource(R.string.game_opening_analysis_filter_player_section))
            GameOpeningAnalysisSideSelector(
                selectedSide = filter.side,
                onSideChange = onSideChange,
            )
            AppTextField(
                value = filter.playerNameQuery,
                onValueChange = onPlayerNameChange,
                label = stringResource(R.string.game_opening_analysis_filter_player_name_label),
                placeholder = stringResource(R.string.game_opening_analysis_filter_player_name_placeholder),
                isError = filter.playerNameQuery.isBlank(),
                inputTestTag = GameOpeningAnalysisFilterPlayerNameTestTag,
            )
            CardMetaText(text = stringResource(R.string.game_opening_analysis_filter_player_name_required))
            FilterSectionTitle(text = stringResource(R.string.game_opening_analysis_filter_match_mode))
            GameOpeningAnalysisNameMatchSelector(
                selectedMatchMode = filter.playerNameMatchMode,
                onMatchModeChange = onMatchModeChange,
            )
            GameOpeningAnalysisCaseSensitiveRow(
                checked = filter.isCaseSensitive,
                onCheckedChange = onCaseSensitiveChange,
            )
        }
    }
}

@Composable
private fun GameOpeningAnalysisSideSelector(
    selectedSide: OpeningSide,
    onSideChange: (OpeningSide) -> Unit,
) {
    KingSideFilterSelector(
        options =
            listOf(
                KingSideFilterOption(
                    value = OpeningSide.WHITE,
                    label = stringResource(R.string.game_opening_analysis_filter_white),
                    mode = KingSideFilterMode.WHITE,
                ),
                KingSideFilterOption(
                    value = OpeningSide.BLACK,
                    label = stringResource(R.string.game_opening_analysis_filter_black),
                    mode = KingSideFilterMode.BLACK,
                    testTag = GameOpeningAnalysisFilterBlackSideTestTag,
                ),
            ),
        selectedValue = selectedSide,
        onValueSelected = onSideChange,
    )
}

@Composable
private fun GameOpeningAnalysisNameMatchSelector(
    selectedMatchMode: GameOpeningAnalysisFilter.PlayerNameMatchMode,
    onMatchModeChange: (GameOpeningAnalysisFilter.PlayerNameMatchMode) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
    ) {
        GameOpeningAnalysisNameMatchOption(
            text = stringResource(R.string.game_opening_analysis_filter_contains),
            selectedMatchMode = selectedMatchMode,
            matchMode = GameOpeningAnalysisFilter.PlayerNameMatchMode.CONTAINS,
            onMatchModeChange = onMatchModeChange,
        )
        GameOpeningAnalysisNameMatchOption(
            text = stringResource(R.string.game_opening_analysis_filter_exact),
            selectedMatchMode = selectedMatchMode,
            matchMode = GameOpeningAnalysisFilter.PlayerNameMatchMode.EXACT,
            onMatchModeChange = onMatchModeChange,
            modifier = Modifier.testTag(GameOpeningAnalysisFilterExactMatchTestTag),
        )
    }
}

@Composable
private fun GameOpeningAnalysisNameMatchOption(
    text: String,
    selectedMatchMode: GameOpeningAnalysisFilter.PlayerNameMatchMode,
    matchMode: GameOpeningAnalysisFilter.PlayerNameMatchMode,
    onMatchModeChange: (GameOpeningAnalysisFilter.PlayerNameMatchMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = selectedMatchMode == matchMode

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onMatchModeChange(matchMode) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Text(
            text = text,
            color = TextColor.Primary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun GameOpeningAnalysisCaseSensitiveRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.game_opening_analysis_filter_case_sensitive),
                color = TextColor.Primary,
                fontWeight = FontWeight.SemiBold,
            )
            CardMetaText(text = stringResource(R.string.game_opening_analysis_filter_case_sensitive_subtitle))
        }
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(GameOpeningAnalysisFilterCaseSensitiveTestTag),
        )
    }
}

@Composable
private fun FilterSectionTitle(text: String) {
    BodySecondaryText(
        text = text,
        color = TextColor.Primary,
    )
}
