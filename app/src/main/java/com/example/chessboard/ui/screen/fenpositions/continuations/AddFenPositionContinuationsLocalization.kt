package com.example.chessboard.ui.screen.fenpositions.continuations

/*
 * File role: groups localized text used by the add-FEN-position-continuations screen.
 * Allowed here:
 * - resource reads and small formatting helpers for continuation-addition UI text
 * Not allowed here:
 * - Compose layout, parsing, persistence, or screen workflow state
 * Validation date: 2026-09-02
 */

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R

internal data class AddFenPositionContinuationsStrings(
    val screenTitle: String,
    val unnamedPosition: String,
    private val themeFormat: String,
    val manualLineTitle: String,
    val manualBack: String,
    val manualClear: String,
    val boardDisabledMessage: String,
    val textTitle: String,
    val textPlaceholder: String,
    val textClear: String,
    val textDisabledMessage: String,
    val newContinuationsTitle: String,
    private val continuationTitleFormat: String,
    val save: String,
    val loadingPositionTitle: String,
    val loadingPositionMessage: String,
    val parsingTitle: String,
    val parsingMessage: String,
    val checkingLinesTitle: String,
    val checkingLinesMessage: String,
    val checkingStoredTitle: String,
    val checkingStoredMessage: String,
    val savingTitle: String,
    val savingMessage: String,
    val resultTitle: String,
    private val recognizedLinesFormat: String,
    private val exactDuplicatesFormat: String,
    private val coveredPrefixLinesFormat: String,
    private val coveredByStoredLinesFormat: String,
    private val newLinesFormat: String,
    val noNewContinuations: String,
    val discardTitle: String,
    val discardMessage: String,
    val stay: String,
    val exit: String,
) {
    fun positionName(name: String): String {
        if (name.isBlank()) {
            return unnamedPosition
        }

        return name
    }

    fun theme(theme: String): String {
        return themeFormat.format(theme)
    }

    fun continuationTitle(index: Int): String {
        return continuationTitleFormat.format(index + 1)
    }

    fun processingTitle(stage: AddFenPositionContinuationsProcessingStage): String {
        return when (stage) {
            AddFenPositionContinuationsProcessingStage.LoadingPosition -> loadingPositionTitle
            AddFenPositionContinuationsProcessingStage.ParsingContinuations -> parsingTitle
            AddFenPositionContinuationsProcessingStage.CheckingLines -> checkingLinesTitle
            AddFenPositionContinuationsProcessingStage.CheckingStoredContinuations -> checkingStoredTitle
            AddFenPositionContinuationsProcessingStage.SavingContinuations -> savingTitle
        }
    }

    fun processingMessage(stage: AddFenPositionContinuationsProcessingStage): String {
        return when (stage) {
            AddFenPositionContinuationsProcessingStage.LoadingPosition -> loadingPositionMessage
            AddFenPositionContinuationsProcessingStage.ParsingContinuations -> parsingMessage
            AddFenPositionContinuationsProcessingStage.CheckingLines -> checkingLinesMessage
            AddFenPositionContinuationsProcessingStage.CheckingStoredContinuations -> checkingStoredMessage
            AddFenPositionContinuationsProcessingStage.SavingContinuations -> savingMessage
        }
    }

    fun resultMessage(result: AddFenPositionContinuationsProcessingResult): String {
        val lines = mutableListOf(recognizedLinesFormat.format(result.recognizedLinesCount))
        if (result.exactDuplicateLinesCount > 0) {
            lines += exactDuplicatesFormat.format(result.exactDuplicateLinesCount)
        }
        if (result.coveredPrefixLinesCount > 0) {
            lines += coveredPrefixLinesFormat.format(result.coveredPrefixLinesCount)
        }
        if (result.coveredByStoredLinesCount > 0) {
            lines += coveredByStoredLinesFormat.format(result.coveredByStoredLinesCount)
        }
        if (result.newLinesCount == 0) {
            lines += noNewContinuations
        } else {
            lines += newLinesFormat.format(result.newLinesCount)
        }

        return lines.joinToString(separator = "\n")
    }
}

@Composable
internal fun addFenPositionContinuationsStrings(): AddFenPositionContinuationsStrings {
    return AddFenPositionContinuationsStrings(
        screenTitle = stringResource(R.string.fen_position_continuation_add_title),
        unnamedPosition = stringResource(R.string.fen_position_continuation_add_unnamed_position),
        themeFormat = stringResource(R.string.fen_position_continuation_add_theme),
        manualLineTitle = stringResource(R.string.fen_position_continuation_add_manual_line_title),
        manualBack = stringResource(R.string.fen_position_continuation_add_manual_back),
        manualClear = stringResource(R.string.fen_position_continuation_add_manual_clear),
        boardDisabledMessage = stringResource(
            R.string.fen_position_continuation_add_board_disabled_message,
        ),
        textTitle = stringResource(R.string.fen_position_continuation_add_text_title),
        textPlaceholder = stringResource(R.string.fen_position_continuation_add_text_placeholder),
        textClear = stringResource(R.string.fen_position_continuation_add_text_clear),
        textDisabledMessage = stringResource(
            R.string.fen_position_continuation_add_text_disabled_message,
        ),
        newContinuationsTitle = stringResource(
            R.string.fen_position_continuation_add_new_continuations_title,
        ),
        continuationTitleFormat = stringResource(
            R.string.fen_position_continuation_add_continuation_title,
        ),
        save = stringResource(R.string.common_save),
        loadingPositionTitle = stringResource(
            R.string.fen_position_continuation_add_loading_position_title,
        ),
        loadingPositionMessage = stringResource(
            R.string.fen_position_continuation_add_loading_position_message,
        ),
        parsingTitle = stringResource(R.string.fen_position_continuation_add_parsing_title),
        parsingMessage = stringResource(R.string.fen_position_continuation_add_parsing_message),
        checkingLinesTitle = stringResource(
            R.string.fen_position_continuation_add_checking_lines_title,
        ),
        checkingLinesMessage = stringResource(
            R.string.fen_position_continuation_add_checking_lines_message,
        ),
        checkingStoredTitle = stringResource(
            R.string.fen_position_continuation_add_checking_stored_title,
        ),
        checkingStoredMessage = stringResource(
            R.string.fen_position_continuation_add_checking_stored_message,
        ),
        savingTitle = stringResource(R.string.fen_position_continuation_add_saving_title),
        savingMessage = stringResource(R.string.fen_position_continuation_add_saving_message),
        resultTitle = stringResource(R.string.fen_position_continuation_add_result_title),
        recognizedLinesFormat = stringResource(
            R.string.fen_position_continuation_add_recognized_lines,
        ),
        exactDuplicatesFormat = stringResource(
            R.string.fen_position_continuation_add_exact_duplicates,
        ),
        coveredPrefixLinesFormat = stringResource(
            R.string.fen_position_continuation_add_covered_prefix_lines,
        ),
        coveredByStoredLinesFormat = stringResource(
            R.string.fen_position_continuation_add_covered_by_stored_lines,
        ),
        newLinesFormat = stringResource(R.string.fen_position_continuation_add_new_lines),
        noNewContinuations = stringResource(
            R.string.fen_position_continuation_add_no_new_continuations,
        ),
        discardTitle = stringResource(R.string.fen_position_continuation_add_discard_title),
        discardMessage = stringResource(R.string.fen_position_continuation_add_discard_message),
        stay = stringResource(R.string.fen_position_continuation_add_stay),
        exit = stringResource(R.string.fen_position_continuation_add_exit),
    )
}
