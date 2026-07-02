package com.example.chessboard.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.chessboard.R
import com.example.chessboard.service.OneLineTrainingData
import com.example.chessboard.service.SmartLinePair
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppIconSizes
import com.example.chessboard.ui.components.AppProgressCard
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppSearchField
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.HomeIconButton
import com.example.chessboard.ui.components.IconSm
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.SettingsIconButton
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingErrorRed
import com.example.chessboard.ui.theme.TrainingSuccessGreen
import com.example.chessboard.ui.theme.TrainingWarningOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val SmartTrainingCardBg = Color(0xFF0D2318)
private val SmartTrainingIconBg = Color(0xFF179A6F)
private val SmartTrainingItemBg = Color(0xFF141414)
private val SmartTrainingItemBorder = Color(0xFF222222)

data class SmartTrainingItem(
    val trainingId: Long,
    val name: String,
    val linesCount: Int,
    val masteredCount: Int,
)

@Composable
fun SmartTrainingScreenContainer(
    screenContext: ScreenContainerContext,
    onStartTraining: (List<SmartLinePair>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val inDbProvider = screenContext.inDbProvider
    val userProfileService = remember(inDbProvider) { inDbProvider.createUserProfileService() }
    val trainingService = remember(inDbProvider) { inDbProvider.createTrainingService() }
    val smartTrainingService = remember(inDbProvider) { inDbProvider.createSmartTrainingService() }
    val unnamedTraining = stringResource(R.string.training_list_unnamed_training)

    var infoCardHidden by remember { mutableStateOf(true) }
    var smartMaxLines by remember { mutableStateOf(10) }
    var smartOnlyWithMistakes by remember { mutableStateOf(false) }
    var trainings by remember { mutableStateOf<List<SmartTrainingItem>>(emptyList()) }
    var noLinesFound by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val profile = withContext(Dispatchers.IO) { userProfileService.getProfile() }
        infoCardHidden = profile.hideSmartTrainingInfoCard
        smartMaxLines = profile.smartMaxLines
        smartOnlyWithMistakes = profile.smartOnlyWithMistakes

        trainings = withContext(Dispatchers.IO) {
            trainingService.getAllTrainings().map { entity ->
                val lineIds = OneLineTrainingData.fromJson(entity.linesJson).map { it.lineId }
                SmartTrainingItem(
                    trainingId = entity.id,
                    name = entity.name.ifBlank { unnamedTraining },
                    linesCount = lineIds.size,
                    masteredCount = smartTrainingService.countMasteredLines(lineIds),
                )
            }
        }
    }

    SmartTrainingScreen(
        infoCardHidden = infoCardHidden,
        trainings = trainings,
        noLinesFound = noLinesFound,
        onDismissNoLinesFound = { noLinesFound = false },
        onDismissInfoCard = {
            infoCardHidden = true
            scope.launch(Dispatchers.IO) { userProfileService.setHideSmartTrainingInfoCard(true) }
        },
        onStartTraining = { selectedIds ->
            scope.launch {
                val queue = withContext(Dispatchers.IO) {
                    smartTrainingService.resolveSmartQueue(selectedIds, smartOnlyWithMistakes).take(smartMaxLines)
                }
                if (queue.isEmpty()) {
                    noLinesFound = true
                } else {
                    onStartTraining(queue)
                }
            }
        },
        onSettingsClick = { screenContext.onNavigate(ScreenType.SmartSettings) },
        onBackClick = screenContext.onBackClick,
        onHomeClick = { screenContext.onNavigate(ScreenType.Home) },
        onNavigate = screenContext.onNavigate,
        modifier = modifier,
    )
}

@Composable
fun SmartTrainingScreen(
    infoCardHidden: Boolean = false,
    trainings: List<SmartTrainingItem> = emptyList(),
    noLinesFound: Boolean = false,
    onDismissNoLinesFound: () -> Unit = {},
    onDismissInfoCard: () -> Unit = {},
    onStartTraining: (Set<Long>) -> Unit = { _ -> },
    onSettingsClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }
    val selectedIds = remember { mutableStateOf(setOf<Long>()) }
    var selectionInitialized by remember { mutableStateOf(false) }

    if (!selectionInitialized && trainings.isNotEmpty()) {
        selectedIds.value = trainings.map { it.trainingId }.toSet()
        selectionInitialized = true
    }

    if (noLinesFound) {
        AlertDialog(
            onDismissRequest = onDismissNoLinesFound,
            title = { Text(stringResource(R.string.smart_training_no_lines_found_title)) },
            text = { Text(stringResource(R.string.smart_training_no_lines_found_message)) },
            confirmButton = {
                TextButton(onClick = onDismissNoLinesFound) {
                    Text(stringResource(R.string.common_ok))
                }
            },
            containerColor = SmartTrainingItemBg,
            titleContentColor = TextColor.Primary,
            textContentColor = TextColor.Secondary,
        )
    }

    val filtered = remember(trainings, searchQuery) {
        trainings.filter {
            searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)
        }
    }
    val selectedCount = selectedIds.value.size

    val progressSource = remember(trainings, selectedIds.value) {
        val source = if (selectedIds.value.isEmpty()) trainings
                     else trainings.filter { it.trainingId in selectedIds.value }
        val total = source.sumOf { it.linesCount }
        val mastered = source.sumOf { it.masteredCount }
        total to mastered
    }
    val (progressTotal, progressMastered) = progressSource

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.smart_training_title),
                subtitleLines = listOf(stringResource(R.string.smart_training_subtitle)),
                onBackClick = onBackClick,
                handleSystemBack = true,
                actions = {
                    HomeIconButton(onClick = onHomeClick)
                    SettingsIconButton(
                        onClick = onSettingsClick,
                        contentDescription = stringResource(
                            R.string.smart_training_settings_content_description,
                        ),
                    )
                },
            )
        },
        bottomBar = {
            Column {
                PrimaryButton(
                    text = stringResource(R.string.smart_training_start),
                    onClick = { onStartTraining(selectedIds.value) },
                    enabled = selectedCount > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceMd),
                )
                AppBottomNavigation(
                    items = defaultAppBottomNavigationItems(),
                    selectedItem = ScreenType.SmartTraining,
                    onItemSelected = onNavigate,
                )
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                horizontal = AppDimens.spaceLg,
                vertical = AppDimens.spaceLg,
            ),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
        ) {
            if (!infoCardHidden) {
                item {
                    HowItWorksCard(onDismiss = onDismissInfoCard)
                }
            }

            item {
                SelectOpeningsHeader(
                    selectedCount = selectedCount,
                    onSelectAll = { selectedIds.value = trainings.map { it.trainingId }.toSet() },
                    onClear = { selectedIds.value = emptySet() },
                )
            }

            if (progressTotal > 0) {
                item {
                    AppProgressCard(
                        label = if (selectedIds.value.isEmpty()) {
                            stringResource(R.string.smart_training_all_lines)
                        } else {
                            stringResource(R.string.smart_training_selected_openings)
                        },
                        progress = progressMastered,
                        total = progressTotal,
                        progressLabel = stringResource(R.string.smart_training_mastered),
                    )
                }
            }

            item {
                AppSearchField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = stringResource(R.string.smart_training_search_placeholder),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            items(filtered.size) { index ->
                val item = filtered[index]
                TrainingSelectRow(
                    item = item,
                    checked = item.trainingId in selectedIds.value,
                    onCheckedChange = { checked ->
                        selectedIds.value = if (checked) {
                            selectedIds.value + item.trainingId
                        } else {
                            selectedIds.value - item.trainingId
                        }
                    },
                )
            }
        }
    }
}


@Composable
private fun HowItWorksCard(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.radiusLg))
            .background(SmartTrainingCardBg)
            .padding(AppDimens.spaceLg),
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SmartTrainingIconBg),
                contentAlignment = Alignment.Center,
            ) {
                IconSm(
                    imageVector = Icons.Filled.Psychology,
                    contentDescription = null,
                    tint = Color.White,
                )
            }
            Spacer(modifier = Modifier.width(AppDimens.spaceMd))
            Text(
                text = stringResource(R.string.smart_training_how_it_works_title),
                style = MaterialTheme.typography.titleMedium,
                color = TextColor.Primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(
                    R.string.smart_training_dismiss_content_description,
                ),
                tint = TextColor.Secondary,
                modifier = Modifier
                    .size(AppIconSizes.Sm)
                    .clickable(onClick = onDismiss),
            )
        }

        Text(
            text = stringResource(R.string.smart_training_how_it_works_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = TextColor.Secondary,
        )

        HowItWorksRow(
            icon = Icons.Filled.Close,
            iconTint = TrainingErrorRed,
            boldPart = stringResource(R.string.smart_training_mistakes_many_label),
            rest = stringResource(R.string.smart_training_mistakes_many_description),
        )
        HowItWorksRow(
            icon = Icons.Filled.Warning,
            iconTint = TrainingWarningOrange,
            boldPart = stringResource(R.string.smart_training_mistakes_one_label),
            rest = stringResource(R.string.smart_training_mistakes_one_description),
        )
        HowItWorksRow(
            icon = Icons.Filled.CheckBox,
            iconTint = TrainingSuccessGreen,
            boldPart = stringResource(R.string.smart_training_no_mistakes_label),
            rest = stringResource(R.string.smart_training_no_mistakes_description),
        )

        Spacer(modifier = Modifier.height(AppDimens.spaceXs))

        Text(
            text = stringResource(R.string.smart_training_how_it_works_summary),
            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
            color = TextColor.Secondary,
        )
    }
}

@Composable
private fun HowItWorksRow(
    icon: ImageVector,
    iconTint: Color,
    boldPart: String,
    rest: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(AppIconSizes.Xs),
        )
        Spacer(modifier = Modifier.width(AppDimens.spaceMd))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextColor.Primary)) {
                    append(boldPart)
                }
                withStyle(SpanStyle(color = TextColor.Primary)) {
                    append(rest)
                }
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SelectOpeningsHeader(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.smart_training_select_openings),
                style = MaterialTheme.typography.headlineSmall,
                color = TextColor.Primary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.smart_training_selected_openings_count,
                    selectedCount,
                    selectedCount,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = TextColor.Secondary,
            )
        }
        Text(
            text = stringResource(R.string.smart_training_select_all),
            style = MaterialTheme.typography.bodyMedium,
            color = TrainingAccentTeal,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(AppDimens.radiusSm))
                .clickable(onClick = onSelectAll)
                .padding(horizontal = AppDimens.spaceSm, vertical = AppDimens.spaceXs),
        )
        Spacer(modifier = Modifier.width(AppDimens.spaceSm))
        Text(
            text = stringResource(R.string.smart_training_clear),
            style = MaterialTheme.typography.bodyMedium,
            color = TextColor.Primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(AppDimens.radiusSm))
                .clickable(onClick = onClear)
                .padding(horizontal = AppDimens.spaceSm, vertical = AppDimens.spaceXs),
        )
    }
}


@Composable
private fun TrainingSelectRow(
    item: SmartTrainingItem,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.radiusMd))
            .background(SmartTrainingItemBg)
            .border(1.dp, SmartTrainingItemBorder, RoundedCornerShape(AppDimens.radiusMd))
            .padding(horizontal = AppDimens.spaceMd, vertical = AppDimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = TrainingAccentTeal,
                uncheckedColor = TextColor.Secondary,
                checkmarkColor = Color.White,
            ),
        )
        Spacer(modifier = Modifier.width(AppDimens.spaceSm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall,
                color = TextColor.Primary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.smart_training_lines_count,
                    item.linesCount,
                    item.linesCount,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = TextColor.Secondary,
            )
        }
    }
}
