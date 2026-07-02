package com.example.chessboard.ui.screen

import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.components.IconSm
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessboard.R
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppConfirmDialog
import com.example.chessboard.ui.components.AppDivider
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.HomeIconButton
import com.example.chessboard.ui.components.ScreenTitleText
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.ChessBoardTheme
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingErrorRed
import com.example.chessboard.ui.theme.TrainingSuccessGreen
import com.example.chessboard.ui.theme.TrainingWarningOrange

private val ProfileHeroCardColor = Color(0xFF0F2A1B)
private val ProfileAvatarContainerColor = Color(0xFF1A4A2A)
private val ProfileAchievementBlue = Color(0xFF5C6BC0)

@Composable
fun ProfileScreenContainer(
    screenContext: ScreenContainerContext,
    modifier: Modifier = Modifier,
) {
    val viewModel = remember { ProfileViewModel() }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.loadStats(screenContext.inDbProvider)
    }

    ProfileScreen(
        state = state,
        onBackClick = screenContext.onBackClick,
        onNavigate = screenContext.onNavigate,
        onClearAllDataClick = {
            viewModel.clearAllData(screenContext.inDbProvider)
        },
        modifier = modifier,
    )
}

@Composable
private fun ProfileScreen(
    state: ProfileState,
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    onClearAllDataClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showClearAllDataDialog by remember { mutableStateOf(false) }

    val clearAllDataTitle = stringResource(R.string.profile_clear_all_data_title)

    if (showClearAllDataDialog) {
        AppConfirmDialog(
            title = clearAllDataTitle,
            message = stringResource(R.string.profile_clear_all_data_message),
            onDismiss = { showClearAllDataDialog = false },
            onConfirm = {
                showClearAllDataDialog = false
                onClearAllDataClick()
            },
            confirmText = stringResource(R.string.profile_clear_all_data_confirm),
            isDestructive = true,
        )
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.profile_title),
                subtitleLines = listOf(stringResource(R.string.profile_subtitle)),
                onBackClick = onBackClick,
                handleSystemBack = true,
                filledBackButton = true,
                actions = {
                    HomeIconButton(onClick = { onNavigate(ScreenType.Home) })
                },
            )
        },
        bottomBar = {
            AppBottomNavigation(
                items = defaultAppBottomNavigationItems(),
                selectedItem = ScreenType.Profile,
                onItemSelected = onNavigate,
            )
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
            item {
                ProfileHeroCard(state = state)
            }
            item {
                QuickStatsCard(state = state)
            }
            item {
                AchievementsSection(achievements = state.achievements)
            }
            item {
                ActionMenuCard(
                    clearAllDataTitle = clearAllDataTitle,
                    onFeedbackClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/alexspenc/ChessBoard/issues"))
                        context.startActivity(intent)
                    },
                    onClearAllDataClick = { showClearAllDataDialog = true },
                )
            }
        }
    }
}

@Composable
private fun ProfileHeroCard(
    state: ProfileState,
    modifier: Modifier = Modifier,
) {
    CardSurface(
        modifier = modifier.fillMaxWidth(),
        color = ProfileHeroCardColor,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(AppDimens.radiusMd))
                    .background(ProfileAvatarContainerColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = state.tier.symbol,
                    fontSize = 30.sp,
                    color = TrainingAccentTeal,
                )
            }
            Spacer(modifier = Modifier.width(AppDimens.spaceLg))
            Column {
                ScreenTitleText(
                    text = stringResource(ProfileLocalization.rankTitleResId(state.rankTitleId)),
                )
                Spacer(modifier = Modifier.height(AppDimens.spaceXs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LevelBadge(level = state.level)
                    Spacer(modifier = Modifier.width(AppDimens.spaceMd))
                    CardMetaText(
                        text = stringResource(R.string.profile_total_trainings, state.totalTrainings)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(AppDimens.spaceLg))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CardMetaText(text = stringResource(R.string.profile_progress_to_level, state.level + 1))
            CardMetaText(text = "${state.totalTrainings}/${state.levelTrainingThreshold}")
        }
        Spacer(modifier = Modifier.height(AppDimens.spaceSm))
        LevelProgressBar(
            current = state.totalTrainings,
            max = state.levelTrainingThreshold,
        )
    }
}

@Composable
private fun LevelBadge(
    level: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AppDimens.radiusPill),
        color = TrainingAccentTeal,
    ) {
        Text(
            text = stringResource(R.string.profile_level, level),
            modifier = Modifier.padding(horizontal = AppDimens.spaceMd, vertical = AppDimens.spaceXs),
            color = TextColor.Primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun LevelProgressBar(
    current: Int,
    max: Int,
    modifier: Modifier = Modifier,
) {
    val fraction = if (max > 0) (current.toFloat() / max).coerceIn(0f, 1f) else 0f
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(AppDimens.radiusPill))
            .background(ProfileAvatarContainerColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(6.dp)
                .clip(RoundedCornerShape(AppDimens.radiusPill))
                .background(TrainingSuccessGreen),
        )
    }
}

@Composable
private fun QuickStatsCard(
    state: ProfileState,
    modifier: Modifier = Modifier,
) {
    CardSurface(
        modifier = modifier.fillMaxWidth(),
    ) {
        SectionTitleText(text = stringResource(R.string.profile_quick_stats_title))
        Spacer(modifier = Modifier.height(AppDimens.spaceLg))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatItem(
                icon = Icons.Filled.Star,
                iconColor = TrainingAccentTeal,
                value = "${state.accuracy}%",
                label = stringResource(R.string.profile_accuracy_label),
            )
            StatItem(
                icon = Icons.Filled.Favorite,
                iconColor = TrainingWarningOrange,
                value = "${state.bestStreak}",
                label = stringResource(R.string.profile_best_streak_label),
            )
            StatItem(
                icon = Icons.Filled.Star,
                iconColor = ProfileAchievementBlue,
                value = "${state.achievements.count { it.isUnlocked }}",
                label = stringResource(R.string.profile_achievements_title),
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    iconColor: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            IconMd(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
            )
        }
        ScreenTitleText(text = value)
        CardMetaText(text = label)
    }
}

@Composable
private fun AchievementsSection(
    achievements: List<AchievementItem>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "★",
                color = TrainingWarningOrange,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(AppDimens.spaceSm))
            SectionTitleText(text = stringResource(R.string.profile_achievements_title))
        }
        CardSurface(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp),
        ) {
            achievements.forEachIndexed { index, achievement ->
                AchievementRow(achievement = achievement)
                if (index < achievements.lastIndex) {
                    AppDivider()
                }
            }
        }
    }
}

@Composable
private fun AchievementRow(
    achievement: AchievementItem,
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
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (achievement.isUnlocked) TrainingSuccessGreen.copy(alpha = 0.15f)
                    else TrainingErrorRed.copy(alpha = 0.15f)
                ),
            contentAlignment = Alignment.Center,
        ) {
            IconSm(
                imageVector = if (achievement.isUnlocked) Icons.Filled.Star else Icons.Filled.Lock,
                contentDescription = null,
                tint = if (achievement.isUnlocked) TrainingSuccessGreen else TrainingErrorRed,
            )
        }
        Spacer(modifier = Modifier.width(AppDimens.spaceLg))
        Column {
            CardMetaText(
                text = stringResource(ProfileLocalization.achievementTitleResId(achievement.id)),
                fontWeight = FontWeight.SemiBold,
                color = TextColor.Primary,
            )
            CardMetaText(
                text = stringResource(ProfileLocalization.achievementDescriptionResId(achievement.id)),
            )
        }
    }
}

@Composable
private fun ActionMenuCard(
    clearAllDataTitle: String,
    onFeedbackClick: () -> Unit,
    onClearAllDataClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CardSurface(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
    ) {
        ActionMenuRow(
            icon = Icons.Filled.Feedback,
            iconBackgroundColor = TrainingAccentTeal.copy(alpha = 0.15f),
            iconTint = TrainingAccentTeal,
            title = stringResource(R.string.profile_send_feedback_title),
            titleColor = TextColor.Primary,
            subtitle = stringResource(R.string.profile_send_feedback_subtitle),
            onClick = onFeedbackClick,
        )
        AppDivider()
        ActionMenuRow(
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            iconBackgroundColor = TrainingErrorRed.copy(alpha = 0.15f),
            iconTint = TrainingErrorRed,
            title = clearAllDataTitle,
            titleColor = TrainingErrorRed,
            subtitle = stringResource(R.string.profile_clear_all_data_subtitle),
            subtitleColor = TrainingErrorRed.copy(alpha = 0.7f),
            onClick = onClearAllDataClick,
        )
    }
}

@Composable
private fun ActionMenuRow(
    icon: ImageVector,
    iconBackgroundColor: Color,
    iconTint: Color,
    title: String,
    titleColor: Color,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitleColor: Color = TextColor.Secondary,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(AppDimens.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(AppDimens.radiusLg))
                .background(iconBackgroundColor),
            contentAlignment = Alignment.Center,
        ) {
            IconSm(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
            )
        }
        Spacer(modifier = Modifier.width(AppDimens.spaceLg))
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)) {
            SectionTitleText(text = title, color = titleColor)
            CardMetaText(text = subtitle, color = subtitleColor)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    ChessBoardTheme {
        ProfileScreen(state = ProfileState())
    }
}
