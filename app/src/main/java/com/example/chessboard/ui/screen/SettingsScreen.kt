package com.example.chessboard.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessboard.R
import com.example.chessboard.localization.AppLanguage
import com.example.chessboard.service.SimpleViewUpgradePromptIntervalDefault
import com.example.chessboard.service.SimpleViewUpgradePromptIntervalMax
import com.example.chessboard.service.SimpleViewUpgradePromptIntervalMin
import com.example.chessboard.service.clampSimpleViewUpgradePromptInterval
import com.example.chessboard.ui.components.AppNumberSlider
import com.example.chessboard.ui.components.AppSettingsScaffold
import com.example.chessboard.ui.components.AppSettingsToggleRow
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.IconSm
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.ChessBoardTheme
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingDividerColor
import com.example.chessboard.ui.theme.TextColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val SettingsLanguageIconBg = Color(0xFF1A3A28)

@Composable
fun SettingsScreenContainer(
    screenContext: ScreenContainerContext,
    simpleViewEnabled: Boolean,
    onSimpleViewToggle: (Boolean) -> Unit,
    removeLineIfRepIsZero: Boolean,
    onRemoveLineIfRepIsZeroToggle: (Boolean) -> Unit,
    hideLinesWithWeightZero: Boolean,
    onHideLinesWithWeightZeroToggle: (Boolean) -> Unit,
    appLanguage: AppLanguage,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val userProfileService = remember(screenContext.inDbProvider) {
        screenContext.inDbProvider.createUserProfileService()
    }
    val scope = rememberCoroutineScope()
    var autoNextLine by remember { mutableStateOf(false) }
    var disableSimpleViewUpgradePrompt by remember { mutableStateOf(false) }
    var simpleViewUpgradePromptInterval by remember {
        mutableStateOf(SimpleViewUpgradePromptIntervalDefault)
    }

    LaunchedEffect(Unit) {
        val profile = withContext(Dispatchers.IO) { userProfileService.getProfile() }
        autoNextLine = profile.autoNextLine
        disableSimpleViewUpgradePrompt = profile.disableSimpleViewUpgradePrompt
        simpleViewUpgradePromptInterval = clampSimpleViewUpgradePromptInterval(
            profile.simpleViewUpgradePromptInterval,
        )
    }

    SettingsScreen(
        simpleViewEnabled = simpleViewEnabled,
        onSimpleViewToggle = onSimpleViewToggle,
        removeLineIfRepIsZero = removeLineIfRepIsZero,
        onRemoveLineIfRepIsZeroToggle = onRemoveLineIfRepIsZeroToggle,
        hideLinesWithWeightZero = hideLinesWithWeightZero,
        onHideLinesWithWeightZeroToggle = onHideLinesWithWeightZeroToggle,
        appLanguage = appLanguage,
        onAppLanguageChange = onAppLanguageChange,
        disableSimpleViewUpgradePrompt = disableSimpleViewUpgradePrompt,
        onDisableSimpleViewUpgradePromptChange = { newValue ->
            disableSimpleViewUpgradePrompt = newValue
            scope.launch(Dispatchers.IO) {
                userProfileService.updateSimpleViewUpgradePromptSettings(
                    disabled = newValue,
                    interval = simpleViewUpgradePromptInterval,
                )
            }
        },
        simpleViewUpgradePromptInterval = simpleViewUpgradePromptInterval,
        onSimpleViewUpgradePromptIntervalChange = { newValue ->
            val interval = clampSimpleViewUpgradePromptInterval(newValue)
            simpleViewUpgradePromptInterval = interval
            scope.launch(Dispatchers.IO) {
                userProfileService.updateSimpleViewUpgradePromptSettings(
                    disabled = disableSimpleViewUpgradePrompt,
                    interval = interval,
                )
            }
        },
        autoNextLine = autoNextLine,
        onAutoNextLineChange = { newValue ->
            autoNextLine = newValue
            scope.launch(Dispatchers.IO) { userProfileService.updateAutoNextLine(newValue) }
        },
        onBackClick = screenContext.onBackClick,
        onNavigate = screenContext.onNavigate,
        modifier = modifier,
    )
}

@Composable
fun SettingsScreen(
    simpleViewEnabled: Boolean,
    onSimpleViewToggle: (Boolean) -> Unit,
    removeLineIfRepIsZero: Boolean,
    onRemoveLineIfRepIsZeroToggle: (Boolean) -> Unit,
    hideLinesWithWeightZero: Boolean,
    onHideLinesWithWeightZeroToggle: (Boolean) -> Unit,
    appLanguage: AppLanguage,
    onAppLanguageChange: (AppLanguage) -> Unit,
    disableSimpleViewUpgradePrompt: Boolean,
    onDisableSimpleViewUpgradePromptChange: (Boolean) -> Unit,
    simpleViewUpgradePromptInterval: Int,
    onSimpleViewUpgradePromptIntervalChange: (Int) -> Unit,
    autoNextLine: Boolean = false,
    onAutoNextLineChange: (Boolean) -> Unit = {},
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    AppSettingsScaffold(
        title = stringResource(R.string.settings_title),
        subtitleLines = listOf(stringResource(R.string.settings_subtitle)),
        selectedNavItem = ScreenType.Settings,
        onBackClick = onBackClick,
        onNavigate = onNavigate,
        filledBackButton = true,
        modifier = modifier,
    ) {
        LanguageSection(
            appLanguage = appLanguage,
            onAppLanguageChange = onAppLanguageChange,
        )
        DisplaySection(
            simpleViewEnabled = simpleViewEnabled,
            onSimpleViewToggle = onSimpleViewToggle,
            disableSimpleViewUpgradePrompt = disableSimpleViewUpgradePrompt,
            onDisableSimpleViewUpgradePromptChange = onDisableSimpleViewUpgradePromptChange,
            simpleViewUpgradePromptInterval = simpleViewUpgradePromptInterval,
            onSimpleViewUpgradePromptIntervalChange = onSimpleViewUpgradePromptIntervalChange,
        )
        TrainingSection(
            removeLineIfRepIsZero = removeLineIfRepIsZero,
            onRemoveLineIfRepIsZeroToggle = onRemoveLineIfRepIsZeroToggle,
            hideLinesWithWeightZero = hideLinesWithWeightZero,
            onHideLinesWithWeightZeroToggle = onHideLinesWithWeightZeroToggle,
            autoNextLine = autoNextLine,
            onAutoNextLineChange = onAutoNextLineChange,
        )
    }
}

@Composable
private fun LanguageSection(
    appLanguage: AppLanguage,
    onAppLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    CardSurface(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_language_section),
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
        LanguageRow(
            appLanguage = appLanguage,
            onAppLanguageChange = onAppLanguageChange,
        )
    }
}

@Composable
private fun LanguageRow(
    appLanguage: AppLanguage,
    onAppLanguageChange: (AppLanguage) -> Unit,
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
                .background(SettingsLanguageIconBg),
            contentAlignment = Alignment.Center,
        ) {
            IconSm(
                imageVector = Icons.Filled.Language,
                contentDescription = null,
                tint = TrainingAccentTeal,
            )
        }
        Spacer(modifier = Modifier.width(AppDimens.spaceLg))
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = stringResource(R.string.settings_language_title),
                style = MaterialTheme.typography.bodyMedium,
                color = TextColor.Primary,
                fontWeight = FontWeight.SemiBold,
            )
            CardMetaText(
                text = stringResource(R.string.settings_language_subtitle),
                color = TextColor.Secondary,
            )
            Spacer(modifier = Modifier.size(AppDimens.spaceMd))
            LanguageChoiceRow(
                appLanguage = appLanguage,
                onAppLanguageChange = onAppLanguageChange,
            )
        }
    }
}

@Composable
private fun LanguageChoiceRow(
    appLanguage: AppLanguage,
    onAppLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val englishLabel = stringResource(R.string.settings_language_english)
    val russianLabel = stringResource(R.string.settings_language_russian)
    val serbianLabel = stringResource(R.string.settings_language_serbian)

    Row(modifier = modifier.fillMaxWidth()) {
        LanguageChoiceButton(
            text = englishLabel,
            selected = appLanguage == AppLanguage.ENGLISH,
            onClick = { onAppLanguageChange(AppLanguage.ENGLISH) },
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(AppDimens.spaceMd))
        LanguageChoiceButton(
            text = russianLabel,
            selected = appLanguage == AppLanguage.RUSSIAN,
            onClick = { onAppLanguageChange(AppLanguage.RUSSIAN) },
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(AppDimens.spaceMd))
        LanguageChoiceButton(
            text = serbianLabel,
            selected = appLanguage == AppLanguage.SERBIAN,
            onClick = { onAppLanguageChange(AppLanguage.SERBIAN) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LanguageChoiceButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (selected) TrainingAccentTeal else TrainingDividerColor
    val contentColor = if (selected) Color.White else TextColor.Primary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(AppDimens.radiusMd))
            .background(backgroundColor)
            .clickable { if (!selected) onClick() }
            .padding(horizontal = AppDimens.spaceMd, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DisplaySection(
    simpleViewEnabled: Boolean,
    onSimpleViewToggle: (Boolean) -> Unit,
    disableSimpleViewUpgradePrompt: Boolean,
    onDisableSimpleViewUpgradePromptChange: (Boolean) -> Unit,
    simpleViewUpgradePromptInterval: Int,
    onSimpleViewUpgradePromptIntervalChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    CardSurface(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_display_section),
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
        SimpleViewUpgradePromptSettings(
            disabled = disableSimpleViewUpgradePrompt,
            onDisabledChange = onDisableSimpleViewUpgradePromptChange,
            interval = simpleViewUpgradePromptInterval,
            onIntervalChange = onSimpleViewUpgradePromptIntervalChange,
        )
    }
}

@Composable
private fun TrainingSection(
    removeLineIfRepIsZero: Boolean,
    onRemoveLineIfRepIsZeroToggle: (Boolean) -> Unit,
    hideLinesWithWeightZero: Boolean,
    onHideLinesWithWeightZeroToggle: (Boolean) -> Unit,
    autoNextLine: Boolean,
    onAutoNextLineChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    CardSurface(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_training_section),
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
        AppSettingsToggleRow(
            icon = Icons.Filled.FitnessCenter,
            title = stringResource(R.string.settings_remove_line_if_rep_zero_title),
            subtitle = stringResource(R.string.settings_remove_line_if_rep_zero_subtitle),
            checked = removeLineIfRepIsZero,
            onCheckedChange = onRemoveLineIfRepIsZeroToggle,
        )
        AppSettingsToggleRow(
            icon = Icons.Filled.VisibilityOff,
            title = stringResource(R.string.settings_hide_lines_weight_zero_title),
            subtitle = stringResource(R.string.settings_hide_lines_weight_zero_subtitle),
            checked = hideLinesWithWeightZero,
            onCheckedChange = onHideLinesWithWeightZeroToggle,
        )
        AppSettingsToggleRow(
            icon = Icons.Filled.FastForward,
            title = stringResource(R.string.settings_auto_next_line_title),
            subtitle = stringResource(R.string.settings_auto_next_line_subtitle),
            checked = autoNextLine,
            onCheckedChange = onAutoNextLineChange,
        )
    }
}

@Composable
private fun SimpleViewRow(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppSettingsToggleRow(
        icon = Icons.Filled.Visibility,
        title = stringResource(R.string.settings_simple_view_title),
        subtitle = stringResource(R.string.settings_simple_view_subtitle),
        checked = enabled,
        onCheckedChange = onToggle,
        modifier = modifier,
    )
}

@Composable
private fun SimpleViewUpgradePromptSettings(
    disabled: Boolean,
    onDisabledChange: (Boolean) -> Unit,
    interval: Int,
    onIntervalChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppSettingsToggleRow(
        icon = Icons.Filled.VisibilityOff,
        title = stringResource(R.string.settings_disable_simple_view_upgrade_prompt_title),
        subtitle = stringResource(R.string.settings_disable_simple_view_upgrade_prompt_subtitle),
        checked = disabled,
        onCheckedChange = onDisabledChange,
        modifier = modifier,
    )

    if (disabled) {
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppDimens.spaceLg),
    ) {
        Text(
            text = stringResource(R.string.settings_simple_view_upgrade_prompt_interval_title),
            style = MaterialTheme.typography.bodyMedium,
            color = TextColor.Primary,
            fontWeight = FontWeight.SemiBold,
        )
        CardMetaText(
            text = stringResource(R.string.settings_simple_view_upgrade_prompt_interval_subtitle),
            color = TextColor.Secondary,
        )
        Spacer(modifier = Modifier.size(AppDimens.spaceMd))
        AppNumberSlider(
            value = clampSimpleViewUpgradePromptInterval(interval),
            min = SimpleViewUpgradePromptIntervalMin,
            max = SimpleViewUpgradePromptIntervalMax,
            onValueChange = onIntervalChange,
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
            removeLineIfRepIsZero = false,
            onRemoveLineIfRepIsZeroToggle = {},
            hideLinesWithWeightZero = false,
            onHideLinesWithWeightZeroToggle = {},
            appLanguage = AppLanguage.ENGLISH,
            onAppLanguageChange = {},
            disableSimpleViewUpgradePrompt = false,
            onDisableSimpleViewUpgradePromptChange = {},
            simpleViewUpgradePromptInterval = SimpleViewUpgradePromptIntervalDefault,
            onSimpleViewUpgradePromptIntervalChange = {},
        )
    }
}
