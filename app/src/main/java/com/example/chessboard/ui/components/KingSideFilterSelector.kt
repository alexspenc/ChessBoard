package com.example.chessboard.ui.components

/**
 * File role: provides a reusable king-icon side filter selector for dialogs and filter panels.
 * Allowed here:
 * - generic side-filter option models and icon selector UI
 * - visual mapping for any/white/black side modes
 * Not allowed here:
 * - screen-specific filter enums, persistence filtering, or navigation behavior
 * Validation date: 2026-06-28
 */
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.example.chessboard.R
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.BottomBarContentColor
import com.example.chessboard.ui.theme.TrainingAccentTeal

enum class KingSideFilterMode {
    ANY,
    WHITE,
    BLACK,
}

data class KingSideFilterOption<T>(
    val value: T,
    val label: String,
    val mode: KingSideFilterMode,
    val testTag: String? = null,
)

@Composable
fun <T> KingSideFilterSelector(
    options: List<KingSideFilterOption<T>>,
    selectedValue: T,
    onValueSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { option ->
            KingSideFilterButton(
                option = option,
                selected = option.value == selectedValue,
                onClick = { onValueSelected(option.value) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun <T> KingSideFilterButton(
    option: KingSideFilterOption<T>,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = resolveKingSideFilterTint(selected)
    var buttonModifier =
        modifier.semantics {
            this.selected = selected
        }
    val testTag = option.testTag
    if (testTag != null) {
        buttonModifier = buttonModifier.testTag(testTag)
    }

    TextButton(
        onClick = onClick,
        modifier = buttonModifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
        ) {
            KingSideFilterIcon(
                mode = option.mode,
                tint = tint,
            )
            Text(
                text = option.label,
                color = tint,
                fontWeight = resolveKingSideFilterFontWeight(selected),
            )
        }
    }
}

@Composable
private fun KingSideFilterIcon(
    mode: KingSideFilterMode,
    tint: Color,
) {
    if (mode == KingSideFilterMode.ANY) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KingIcon(tint = tint)
            KingIcon(tint = tint)
        }
        return
    }

    Box(contentAlignment = Alignment.Center) {
        KingIcon(tint = tint)
    }
}

@Composable
private fun KingIcon(tint: Color) {
    Icon(
        painter = painterResource(R.drawable.ic_king),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(AppIconSizes.Md),
    )
}

private fun resolveKingSideFilterTint(selected: Boolean): Color {
    if (selected) {
        return TrainingAccentTeal
    }

    return BottomBarContentColor
}

private fun resolveKingSideFilterFontWeight(selected: Boolean): FontWeight {
    if (selected) {
        return FontWeight.SemiBold
    }

    return FontWeight.Normal
}
