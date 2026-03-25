package com.example.chessboard.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.chessboard.ui.theme.TrainingTextPrimary
import com.example.chessboard.ui.theme.TrainingTextSecondary

/** Displays the primary large title used for screens and other major headings. */
@Composable
fun ScreenTitleText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TrainingTextPrimary
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleLarge,
        color = color
    )
}

/** Displays a medium-emphasis section heading inside screen content. */
@Composable
fun SectionTitleText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TrainingTextPrimary
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        color = color
    )
}

/** Displays the label text shown above input fields. */
@Composable
fun FieldLabelText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TrainingTextSecondary
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = color
    )
}

/** Displays secondary body text for hints, subtitles, and empty states. */
@Composable
fun BodySecondaryText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TrainingTextSecondary,
    textAlign: TextAlign? = null
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        textAlign = textAlign
    )
}

/** Displays compact metadata text inside cards, badges, and lightweight actions. */
@Composable
fun CardMetaText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TrainingTextSecondary,
    fontWeight: FontWeight = FontWeight.Medium
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        fontWeight = fontWeight
    )
}

/** Displays labels for bottom navigation items. */
@Composable
fun NavLabelText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TrainingTextSecondary,
    fontWeight: FontWeight = FontWeight.Medium,
    textAlign: TextAlign? = null
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = fontWeight,
        textAlign = textAlign
    )
}
