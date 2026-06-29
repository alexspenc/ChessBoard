package com.example.chessboard.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingErrorRed
import com.example.chessboard.ui.theme.MutedContentColor
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
    minLines: Int = 1,
    focusRequester: FocusRequester? = null,
    inputTestTag: String? = null,
) {
    Column(modifier = modifier) {
        if (label.isNotBlank()) {
            FieldLabelText(
                text = label,
                color = if (isError) TrainingErrorRed else TextColor.Secondary,
                modifier = Modifier.padding(bottom = AppDimens.radiusXs)
            )
        }
        Surface(
            shape = RoundedCornerShape(AppDimens.radiusMd),
            color = Background.SurfaceDark,
            border = if (isError) BorderStroke(1.dp, TrainingErrorRed) else null,
            modifier = Modifier.fillMaxWidth()
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                    .then(if (inputTestTag != null) Modifier.testTag(inputTestTag) else Modifier)
                    .padding(horizontal = 14.dp, vertical = AppDimens.spaceMd),
                textStyle = MaterialTheme.typography.bodyMedium.merge(
                    TextStyle(color = TextColor.Primary)
                ),
                cursorBrush = SolidColor(TrainingAccentTeal),
                minLines = minLines,
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        BodySecondaryText(
                            text = placeholder,
                            color = MutedContentColor
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

private val SettingsToggleRowIconBg = Color(0xFF1A3A28)
private val SettingsToggleRowUncheckedTrack = Color(0xFF3A3A3A)

@Composable
fun AppSettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    iconBackground: Color = SettingsToggleRowIconBg,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(AppDimens.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(AppDimens.radiusLg))
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            IconSm(
                imageVector = icon,
                contentDescription = null,
                tint = TrainingAccentTeal,
            )
        }
        Spacer(modifier = Modifier.width(AppDimens.spaceLg))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextColor.Primary,
                fontWeight = FontWeight.SemiBold,
            )
            if (subtitle.isNotEmpty()) {
                CardMetaText(text = subtitle, color = TextColor.Secondary)
            }
        }
        Spacer(modifier = Modifier.width(AppDimens.spaceMd))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = TrainingAccentTeal,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = SettingsToggleRowUncheckedTrack,
            ),
        )
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
        IconSm(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = TrainingTextSecondary,
        )
        Spacer(modifier = Modifier.size(AppDimens.spaceMd))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterVertically),
            textStyle = MaterialTheme.typography.bodyMedium.merge(
                TextStyle(color = TextColor.Primary)
            ),
            cursorBrush = SolidColor(TrainingAccentTeal),
            singleLine = true,
            decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        BodySecondaryText(text = placeholder)
                    }
                    innerTextField()
                }
        )
    }
}
