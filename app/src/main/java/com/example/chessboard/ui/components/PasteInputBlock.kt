@file:Suppress("FunctionName")

package com.example.chessboard.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.chessboard.R
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.MutedContentColor
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal

@Composable
fun PasteInputBlock(
    title: String,
    text: String,
    onTextChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
    minLines: Int = 4,
    inputTestTag: String? = null,
    onImportFromFileClick: (() -> Unit)? = null,
    importFromFileTestTag: String? = null,
) {
    val importFromFileLabel = stringResource(R.string.paste_input_from_file)
    val scrollState = rememberScrollState()
    val maxFieldHeight = LocalConfiguration.current.screenHeightDp.dp / 4

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
        ) {
            SectionTitleText(text = title, color = TrainingAccentTeal)
            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = TrainingAccentTeal.copy(alpha = 0.15f),
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = TrainingAccentTeal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(AppDimens.radiusMd),
            color = Background.SurfaceDark,
            border = BorderStroke(1.dp, TrainingAccentTeal),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxFieldHeight),
        ) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .then(if (inputTestTag != null) Modifier.testTag(inputTestTag) else Modifier)
                        .padding(horizontal = 14.dp, vertical = AppDimens.spaceMd),
                textStyle =
                    MaterialTheme.typography.bodyMedium.merge(
                        TextStyle(color = TextColor.Primary),
                    ),
                cursorBrush = SolidColor(TrainingAccentTeal),
                minLines = minLines,
                decorationBox = { innerTextField ->
                    if (text.isEmpty()) {
                        BodySecondaryText(
                            text = placeholder,
                            color = MutedContentColor,
                        )
                    }
                    innerTextField()
                },
            )
        }

        if (onImportFromFileClick != null) {
            Surface(
                onClick = onImportFromFileClick,
                shape = RoundedCornerShape(AppDimens.radiusMd),
                color = Background.SurfaceDark,
                border = BorderStroke(1.dp, TrainingAccentTeal),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .then(if (importFromFileTestTag != null) Modifier.testTag(importFromFileTestTag) else Modifier),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = AppDimens.spaceMd, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconXs(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = TrainingAccentTeal,
                    )
                    Spacer(modifier = Modifier.width(AppDimens.spaceXs))
                    Text(
                        text = importFromFileLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = TrainingAccentTeal,
                    )
                }
            }
        }
    }
}
