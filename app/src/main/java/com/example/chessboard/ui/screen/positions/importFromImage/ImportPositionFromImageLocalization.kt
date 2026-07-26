package com.example.chessboard.ui.screen.positions.importFromImage

/*
 * Localization holder for the import-position-from-image screen strings.
 * Keep grouped resource reads and small formatting helpers used by this screen here.
 * Do not add persistence, navigation, or UI layout logic to this file.
 */

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R

internal class ImportPositionFromImageStrings(
    val screenTitle: String,
    val pickImageAction: String,
    val cropSectionTitle: String,
    val cropHint: String,
    val whiteAtBottomTitle: String,
    val whiteAtBottomSubtitle: String,
    val recognizeAction: String,
    val recognizingLabel: String,
    val resultSectionTitle: String,
    val warningsTitle: String,
    private val uncertainSquaresFormat: String,
    val openInPositionSearchAction: String,
    val decodeFailedMessage: String,
) {
    fun uncertainSquaresMessage(squares: List<String>): String {
        return uncertainSquaresFormat.format(squares.joinToString(separator = ", "))
    }
}

@Composable
internal fun importPositionFromImageStrings(): ImportPositionFromImageStrings {
    return ImportPositionFromImageStrings(
        screenTitle = stringResource(R.string.import_position_title),
        pickImageAction = stringResource(R.string.import_position_pick_image),
        cropSectionTitle = stringResource(R.string.import_position_crop_title),
        cropHint = stringResource(R.string.import_position_crop_hint),
        whiteAtBottomTitle = stringResource(R.string.import_position_white_at_bottom_title),
        whiteAtBottomSubtitle = stringResource(R.string.import_position_white_at_bottom_subtitle),
        recognizeAction = stringResource(R.string.import_position_recognize),
        recognizingLabel = stringResource(R.string.import_position_recognizing),
        resultSectionTitle = stringResource(R.string.import_position_result_title),
        warningsTitle = stringResource(R.string.import_position_warnings_title),
        uncertainSquaresFormat = stringResource(R.string.import_position_uncertain_squares),
        openInPositionSearchAction = stringResource(R.string.import_position_open_in_search),
        decodeFailedMessage = stringResource(R.string.import_position_decode_failed),
    )
}
