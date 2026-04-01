package com.example.chessboard.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.ChessBoardTheme
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal

private val SettingsIconBg = Color(0xFF1A3A28)

@Composable
fun SettingsScreenContainer(
    screenContext: ScreenContainerContext,
    modifier: Modifier = Modifier,
) {
    var simpleViewEnabled by remember { mutableStateOf(false) }

    SettingsScreen(
        simpleViewEnabled = simpleViewEnabled,
        onSimpleViewToggle = { simpleViewEnabled = it },
        onBackClick = screenContext.onBackClick,
        modifier = modifier,
    )
}

@Composable
fun SettingsScreen(
    simpleViewEnabled: Boolean,
    onSimpleViewToggle: (Boolean) -> Unit,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = "Settings",
                subtitle = "Customize your experience",
                onBackClick = onBackClick,
                filledBackButton = true,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(AppDimens.spaceLg),
        ) {
            DisplaySection(
                simpleViewEnabled = simpleViewEnabled,
                onSimpleViewToggle = onSimpleViewToggle,
            )
        }
    }
}

@Composable
private fun DisplaySection(
    simpleViewEnabled: Boolean,
    onSimpleViewToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    CardSurface(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(
            text = "DISPLAY",
            modifier = Modifier.padding(
                start = AppDimens.spaceLg,
                top = AppDimens.spaceLg,
                end = AppDimens.spaceLg,
                bottom = AppDimens.spaceMd,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = TextColor.Secondary,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
        )
        SimpleViewRow(
            enabled = simpleViewEnabled,
            onToggle = onSimpleViewToggle,
        )
    }
}

@Composable
private fun SimpleViewRow(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppDimens.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(AppDimens.radiusLg))
                .background(SettingsIconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Visibility,
                contentDescription = null,
                tint = TrainingAccentTeal,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(modifier = Modifier.width(AppDimens.spaceLg))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
        ) {
            Text(
                text = "Simple View",
                style = MaterialTheme.typography.bodyMedium,
                color = TextColor.Primary,
                fontWeight = FontWeight.SemiBold,
            )
            CardMetaText(
                text = "Simplified interface with minimal distractions",
                color = TextColor.Secondary,
            )
        }

        Spacer(modifier = Modifier.width(AppDimens.spaceMd))

        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = TrainingAccentTeal,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF3A3A3A),
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    ChessBoardTheme {
        SettingsScreen(
            simpleViewEnabled = false,
            onSimpleViewToggle = {},
        )
    }
}
