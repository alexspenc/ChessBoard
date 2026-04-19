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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppDivider
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppSearchField
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.service.OneGameTrainingData
import com.example.chessboard.service.SmartGamePair
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TextColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingErrorRed
import com.example.chessboard.ui.theme.TrainingSuccessGreen
import com.example.chessboard.ui.theme.TrainingWarningOrange

private val SmartTrainingCardBg = Color(0xFF0D2318)
private val SmartTrainingIconBg = Color(0xFF179A6F)
private val SmartTrainingItemBg = Color(0xFF141414)
private val SmartTrainingItemBorder = Color(0xFF222222)

data class SmartTrainingItem(
    val trainingId: Long,
    val name: String,
    val gamesCount: Int,
    val masteredCount: Int,
)

@Composable
fun SmartTrainingScreenContainer(
    screenContext: ScreenContainerContext,
    onStartTraining: (List<SmartGamePair>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val inDbProvider = screenContext.inDbProvider
    val userProfileService = remember(inDbProvider) { inDbProvider.createUserProfileService() }
    val trainingService = remember(inDbProvider) { inDbProvider.createTrainingService() }
    val smartTrainingService = remember(inDbProvider) { inDbProvider.createSmartTrainingService() }

    var infoCardHidden by remember { mutableStateOf(false) }
    var trainings by remember { mutableStateOf<List<SmartTrainingItem>>(emptyList()) }
    var noGamesFound by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val profile = withContext(Dispatchers.IO) { userProfileService.getProfile() }
        infoCardHidden = profile.hideSmartTrainingInfoCard

        trainings = withContext(Dispatchers.IO) {
            trainingService.getAllTrainings().map { entity ->
                val gameIds = OneGameTrainingData.fromJson(entity.gamesJson).map { it.gameId }
                SmartTrainingItem(
                    trainingId = entity.id,
                    name = entity.name.ifBlank { "Unnamed Training" },
                    gamesCount = gameIds.size,
                    masteredCount = smartTrainingService.countMasteredGames(gameIds),
                )
            }
        }
    }

    SmartTrainingScreen(
        infoCardHidden = infoCardHidden,
        trainings = trainings,
        noGamesFound = noGamesFound,
        onDismissNoGamesFound = { noGamesFound = false },
        onDismissInfoCard = {
            infoCardHidden = true
            scope.launch(Dispatchers.IO) { userProfileService.setHideSmartTrainingInfoCard(true) }
        },
        onStartTraining = { selectedIds, maxLines, onlyWithMistakes ->
            scope.launch {
                val queue = withContext(Dispatchers.IO) {
                    smartTrainingService.resolveSmartQueue(selectedIds, onlyWithMistakes).take(maxLines)
                }
                if (queue.isEmpty()) {
                    noGamesFound = true
                } else {
                    onStartTraining(queue)
                }
            }
        },
        onNavigate = screenContext.onNavigate,
        modifier = modifier,
    )
}

private const val MaxLinesMin = 1
private const val MaxLinesMax = 50
private const val MaxLinesDefault = 10

@Composable
fun SmartTrainingScreen(
    infoCardHidden: Boolean = false,
    trainings: List<SmartTrainingItem> = emptyList(),
    noGamesFound: Boolean = false,
    onDismissNoGamesFound: () -> Unit = {},
    onDismissInfoCard: () -> Unit = {},
    onStartTraining: (Set<Long>, Int, Boolean) -> Unit = { _, _, _ -> },
    onNavigate: (ScreenType) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }
    val selectedIds = remember { mutableStateOf(setOf<Long>()) }
    var maxLines by remember { mutableStateOf(MaxLinesDefault) }
    var onlyWithMistakes by remember { mutableStateOf(false) }

    if (noGamesFound) {
        AlertDialog(
            onDismissRequest = onDismissNoGamesFound,
            title = { Text("No Games Found") },
            text = { Text("No games are due right now.\n\nGames with no mistakes are skipped until 3 days have passed. If nothing is due at 3 days, games from 5+ days ago are shown instead.\n\nTry turning on \"Only Games with Mistakes\" to train lines you haven't perfected yet.") },
            confirmButton = {
                TextButton(onClick = onDismissNoGamesFound) { Text("OK") }
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
        val total = source.sumOf { it.gamesCount }
        val mastered = source.sumOf { it.masteredCount }
        total to mastered
    }
    val (progressTotal, progressMastered) = progressSource

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { SmartTrainingTopBar() },
        bottomBar = {
            Column {
                PrimaryButton(
                    text = "Start Training",
                    onClick = { onStartTraining(selectedIds.value, maxLines, onlyWithMistakes) },
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
                    SmartTrainingProgressCard(
                        mastered = progressMastered,
                        total = progressTotal,
                        label = if (selectedIds.value.isEmpty()) "All lines" else "Selected openings",
                    )
                }
            }

            item {
                AppSearchField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Search trainings...",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                MaxLinesStepper(
                    value = maxLines,
                    onDecrement = { if (maxLines > MaxLinesMin) maxLines-- },
                    onIncrement = { if (maxLines < MaxLinesMax) maxLines++ },
                )
            }

            item {
                OnlyWithMistakesToggle(
                    checked = onlyWithMistakes,
                    onCheckedChange = { onlyWithMistakes = it },
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
private fun SmartTrainingTopBar(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SmartTrainingIconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Psychology,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(modifier = Modifier.width(AppDimens.spaceLg))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Smart Training",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextColor.Primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = TrainingAccentTeal,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(
                    text = "Select openings to practice",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextColor.Secondary,
                )
            }
        }
        AppDivider()
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
                Icon(
                    imageVector = Icons.Filled.Psychology,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(AppDimens.spaceMd))
            Text(
                text = "How Your Training Works",
                style = MaterialTheme.typography.titleMedium,
                color = TextColor.Primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Dismiss",
                tint = TextColor.Secondary,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onDismiss),
            )
        }

        Text(
            text = "We use a smart repetition system to help you remember openings more effectively:",
            style = MaterialTheme.typography.bodyMedium,
            color = TextColor.Secondary,
        )

        HowItWorksRow(
            icon = Icons.Filled.Close,
            iconTint = TrainingErrorRed,
            boldPart = "2+ mistakes",
            rest = " — highest priority, always included first",
        )
        HowItWorksRow(
            icon = Icons.Filled.Warning,
            iconTint = TrainingWarningOrange,
            boldPart = "1 mistake or new",
            rest = " — included next, until you nail it",
        )
        HowItWorksRow(
            icon = Icons.Filled.CheckBox,
            iconTint = TrainingSuccessGreen,
            boldPart = "No mistakes",
            rest = " — reviewed after 3 days; if nothing is due at 3 days, after 5 days",
        )

        Spacer(modifier = Modifier.height(AppDimens.spaceXs))

        Text(
            text = "This way, you focus more on what you struggle with, while still reinforcing what you already know — so nothing gets forgotten.",
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
                .size(18.dp),
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
                text = "Select Openings",
                style = MaterialTheme.typography.headlineSmall,
                color = TextColor.Primary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "$selectedCount openings selected",
                style = MaterialTheme.typography.bodySmall,
                color = TextColor.Secondary,
            )
        }
        Text(
            text = "Select All",
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
            text = "Clear",
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
private fun MaxLinesStepper(
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.radiusMd))
            .background(SmartTrainingItemBg)
            .border(1.dp, SmartTrainingItemBorder, RoundedCornerShape(AppDimens.radiusMd))
            .padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Max Lines",
                style = MaterialTheme.typography.titleSmall,
                color = TextColor.Primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Lines to train per session",
                style = MaterialTheme.typography.bodySmall,
                color = TextColor.Secondary,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(AppDimens.radiusSm))
                    .background(SmartTrainingIconBg.copy(alpha = if (value > MaxLinesMin) 1f else 0.3f))
                    .clickable(enabled = value > MaxLinesMin, onClick = onDecrement),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "−",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            }
            Text(
                text = "$value",
                style = MaterialTheme.typography.titleMedium,
                color = TextColor.Primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(40.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(AppDimens.radiusSm))
                    .background(SmartTrainingIconBg.copy(alpha = if (value < MaxLinesMax) 1f else 0.3f))
                    .clickable(enabled = value < MaxLinesMax, onClick = onIncrement),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun SmartTrainingProgressCard(
    mastered: Int,
    total: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    val fraction = if (total > 0) mastered.toFloat() / total else 0f
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.radiusMd))
            .background(SmartTrainingItemBg)
            .border(1.dp, SmartTrainingItemBorder, RoundedCornerShape(AppDimens.radiusMd))
            .padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceMd),
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = TextColor.Primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "$mastered / $total mastered",
                style = MaterialTheme.typography.bodySmall,
                color = TextColor.Secondary,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(AppDimens.radiusPill))
                .background(SmartTrainingItemBorder),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(AppDimens.radiusPill))
                    .background(TrainingAccentTeal),
            )
        }
    }
}

@Composable
private fun OnlyWithMistakesToggle(
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
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = AppDimens.spaceLg, vertical = AppDimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Only Games with Mistakes",
                style = MaterialTheme.typography.titleSmall,
                color = TextColor.Primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Skip well-known lines, focus on errors",
                style = MaterialTheme.typography.bodySmall,
                color = TextColor.Secondary,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = TrainingAccentTeal,
                uncheckedThumbColor = TextColor.Secondary,
                uncheckedTrackColor = SmartTrainingItemBorder,
            ),
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
                text = "${item.gamesCount} ${if (item.gamesCount == 1) "game" else "games"}",
                style = MaterialTheme.typography.bodySmall,
                color = TextColor.Secondary,
            )
        }
    }
}
