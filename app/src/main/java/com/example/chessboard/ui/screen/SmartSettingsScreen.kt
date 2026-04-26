package com.example.chessboard.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SmartMaxLinesMin = 1
private const val SmartMaxLinesMax = 50

private val SmartSettingsIconBg = Color(0xFF1A3A28)
private val SmartSettingsStepperBg = Color(0xFF179A6F)

@Composable
fun SmartSettingsScreenContainer(
    screenContext: ScreenContainerContext,
    modifier: Modifier = Modifier,
) {
    val userProfileService = remember(screenContext.inDbProvider) {
        screenContext.inDbProvider.createUserProfileService()
    }
    val scope = rememberCoroutineScope()

    var maxLines by remember { mutableStateOf(10) }
    var onlyWithMistakes by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val profile = withContext(Dispatchers.IO) { userProfileService.getProfile() }
        maxLines = profile.smartMaxLines
        onlyWithMistakes = profile.smartOnlyWithMistakes
    }

    SmartSettingsScreen(
        maxLines = maxLines,
        onlyWithMistakes = onlyWithMistakes,
        onMaxLinesDecrement = {
            if (maxLines > SmartMaxLinesMin) {
                maxLines--
                scope.launch(Dispatchers.IO) { userProfileService.updateSmartSettings(maxLines, onlyWithMistakes) }
            }
        },
        onMaxLinesIncrement = {
            if (maxLines < SmartMaxLinesMax) {
                maxLines++
                scope.launch(Dispatchers.IO) { userProfileService.updateSmartSettings(maxLines, onlyWithMistakes) }
            }
        },
        onOnlyWithMistakesChange = { newValue ->
            onlyWithMistakes = newValue
            scope.launch(Dispatchers.IO) { userProfileService.updateSmartSettings(maxLines, newValue) }
        },
        onBackClick = screenContext.onBackClick,
        onNavigate = screenContext.onNavigate,
        modifier = modifier,
    )
}

@Composable
fun SmartSettingsScreen(
    maxLines: Int = 10,
    onlyWithMistakes: Boolean = false,
    onMaxLinesDecrement: () -> Unit = {},
    onMaxLinesIncrement: () -> Unit = {},
    onOnlyWithMistakesChange: (Boolean) -> Unit = {},
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = "Smart Training Settings",
                subtitle = "Configure your session defaults",
                onBackClick = onBackClick,
                filledBackButton = true,
            )
        },
        bottomBar = {
            AppBottomNavigation(
                items = defaultAppBottomNavigationItems(),
                selectedItem = ScreenType.SmartTraining,
                onItemSelected = onNavigate,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(AppDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
        ) {
            SmartSessionSection(
                maxLines = maxLines,
                onlyWithMistakes = onlyWithMistakes,
                onMaxLinesDecrement = onMaxLinesDecrement,
                onMaxLinesIncrement = onMaxLinesIncrement,
                onOnlyWithMistakesChange = onOnlyWithMistakesChange,
            )
        }
    }
}

@Composable
private fun SmartSessionSection(
    maxLines: Int,
    onlyWithMistakes: Boolean,
    onMaxLinesDecrement: () -> Unit,
    onMaxLinesIncrement: () -> Unit,
    onOnlyWithMistakesChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    CardSurface(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(
            text = "SESSION",
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
        MaxLinesRow(
            value = maxLines,
            onDecrement = onMaxLinesDecrement,
            onIncrement = onMaxLinesIncrement,
        )
        OnlyWithMistakesRow(
            checked = onlyWithMistakes,
            onCheckedChange = onOnlyWithMistakesChange,
        )
    }
}

@Composable
private fun MaxLinesRow(
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
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
                .background(SmartSettingsIconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.FormatListNumbered,
                contentDescription = null,
                tint = TrainingAccentTeal,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.width(AppDimens.spaceLg))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Max Lines",
                style = MaterialTheme.typography.bodyMedium,
                color = TextColor.Primary,
                fontWeight = FontWeight.SemiBold,
            )
            CardMetaText(
                text = "Lines to train per session",
                color = TextColor.Secondary,
            )
        }
        Spacer(modifier = Modifier.width(AppDimens.spaceMd))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepperButton(
                label = "−",
                enabled = value > SmartMaxLinesMin,
                onClick = onDecrement,
            )
            Text(
                text = "$value",
                style = MaterialTheme.typography.titleMedium,
                color = TextColor.Primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(40.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            StepperButton(
                label = "+",
                enabled = value < SmartMaxLinesMax,
                onClick = onIncrement,
            )
        }
    }
}

@Composable
private fun StepperButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(RoundedCornerShape(AppDimens.radiusSm))
            .background(SmartSettingsStepperBg.copy(alpha = if (enabled) 1f else 0.3f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )
    }
}

@Composable
private fun OnlyWithMistakesRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
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
                .background(SmartSettingsIconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
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
                text = "Only Games with Mistakes",
                style = MaterialTheme.typography.bodyMedium,
                color = TextColor.Primary,
                fontWeight = FontWeight.SemiBold,
            )
            CardMetaText(
                text = "Skip well-known lines, focus on errors",
                color = TextColor.Secondary,
            )
        }
        Spacer(modifier = Modifier.width(AppDimens.spaceMd))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = TrainingAccentTeal,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF3A3A3A),
            ),
        )
    }
}
