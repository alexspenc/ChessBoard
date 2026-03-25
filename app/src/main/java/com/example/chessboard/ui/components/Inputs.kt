package com.example.chessboard.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingErrorRed
import com.example.chessboard.ui.theme.TrainingIconInactive
import com.example.chessboard.ui.theme.TrainingSurfaceDark
import com.example.chessboard.ui.theme.TrainingTextPrimary
import com.example.chessboard.ui.theme.TrainingTextSecondary

/** Displays the standard text field used for labeled form input across the app. */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    minLines: Int = 1
) {
    Column(modifier = modifier) {
        FieldLabelText(
            text = label,
            color = if (isError) TrainingErrorRed else TrainingTextSecondary,
            modifier = Modifier.padding(bottom = AppDimens.radiusXs)
        )
        Surface(
            shape = RoundedCornerShape(AppDimens.radiusMd),
            color = TrainingSurfaceDark,
            border = if (isError) BorderStroke(1.dp, TrainingErrorRed) else null,
            modifier = Modifier.fillMaxWidth()
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = AppDimens.spaceMd),
                textStyle = MaterialTheme.typography.bodyMedium.merge(
                    TextStyle(color = TrainingTextPrimary)
                ),
                cursorBrush = SolidColor(TrainingAccentTeal),
                minLines = minLines,
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        BodySecondaryText(
                            text = placeholder,
                            color = TrainingIconInactive
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

/** Displays the standard pill-shaped search field used for filtering lists. */
@Composable
fun AppSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    PillSurface(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = AppDimens.spaceLg,
            vertical = 14.dp
        )
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = TrainingTextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.size(AppDimens.spaceMd))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterVertically),
            textStyle = MaterialTheme.typography.bodyMedium.merge(
                TextStyle(color = TrainingTextPrimary)
            ),
            cursorBrush = SolidColor(TrainingAccentTeal),
            singleLine = true,
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    BodySecondaryText(
                        text = placeholder,
                        color = TrainingTextSecondary
                    )
                }
                innerTextField()
            }
        )
    }
}
