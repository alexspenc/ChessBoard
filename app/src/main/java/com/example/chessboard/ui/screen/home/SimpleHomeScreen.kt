package com.example.chessboard.ui.screen.home

/**
 * File role: renders the SimpleView home screen and only the UI used by that branch.
 * Allowed here:
 * - SimpleView home layout
 * - search/filter state local to SimpleView home
 * Not allowed here:
 * - regular home layout
 * - screen container loading logic
 * Validation date: 2026-05-03
 */
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chessboard.R
import com.example.chessboard.ui.components.AppIconSizes
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppSearchField
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.components.IconSm
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal

private val HomeSegmentBackground = Color(0xFF151515)
private val HomeSegmentSelected = Color(0xFF202020)
private val HomeBadgeBackground = Color(0xFF242428)
private val HomeMetricCard = Color(0xFF202024)
private val HomeDivider = Color(0xFF202020)
private val SmartTrainingIconBg = Color(0xFF179A6F)

private enum class HomeSideFilter {
    ALL,
    WHITE,
    BLACK
}

@Composable
internal fun SimpleHomeScreen(
    trainings: List<HomeTrainingItem>,
    onCreateOpeningClick: () -> Unit,
    onOpenTraining: (Long) -> Unit,
    onNavigate: (ScreenType) -> Unit,
    onSmartTrainingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }
    var sideFilter by remember { mutableStateOf(HomeSideFilter.ALL) }

    val filteredTrainings = remember(trainings, searchQuery, sideFilter) {
        trainings.filter { training ->
            val matchesSearch = searchQuery.isBlank() ||
                training.name.contains(searchQuery, ignoreCase = true)
            val matchesSide = when (sideFilter) {
                HomeSideFilter.ALL -> true
                HomeSideFilter.WHITE -> training.supportsWhite
                HomeSideFilter.BLACK -> training.supportsBlack
            }
            matchesSearch && matchesSide
        }
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            HomeBottomNavigation(onItemSelected = onNavigate)
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 10.dp,
                top = 8.dp,
                end = 10.dp,
                bottom = AppDimens.spaceLg,
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.ic_crown),
                                contentDescription = null,
                                tint = TrainingAccentTeal,
                                modifier = Modifier.size(AppIconSizes.Md),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Chess Openings",
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextColor.Primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            text = "Master 10 classic openings",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextColor.Secondary,
                            modifier = Modifier.padding(start = 2.dp, top = 4.dp),
                        )
                    }
                    AddOpeningButton(onClick = onCreateOpeningClick)
                }
            }

            item {
                AppSearchField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Search openings...",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                HomeSideFilterRow(
                    selectedFilter = sideFilter,
                    onFilterSelected = { sideFilter = it },
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(HomeDivider),
                )
            }

            item {
                SmartTrainingBanner(onClick = onSmartTrainingClick)
            }

            if (filteredTrainings.isEmpty()) {
                item {
                    CardSurface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Background.CardDark,
                    ) {
                        BodySecondaryText(
                            text = if (trainings.isEmpty()) {
                                "No trainings available."
                            } else {
                                "No trainings match the current filters."
                            },
                            color = TextColor.Secondary,
                        )
                    }
                }
            } else {
                items(filteredTrainings) { training ->
                    HomeTrainingCard(
                        training = training,
                        onClick = { onOpenTraining(training.trainingId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeSideFilterRow(
    selectedFilter: HomeSideFilter,
    onFilterSelected: (HomeSideFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = HomeSegmentBackground,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            HomeSideFilter.entries.forEach { filter ->
                val isSelected = filter == selectedFilter
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) HomeSegmentSelected else Color.Transparent)
                        .clickable { onFilterSelected(filter) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = when (filter) {
                            HomeSideFilter.ALL -> "All"
                            HomeSideFilter.WHITE -> "As White"
                            HomeSideFilter.BLACK -> "As Black"
                        },
                        color = if (isSelected) TextColor.Primary else TextColor.Secondary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeTrainingCard(
    training: HomeTrainingItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CardSurface(
        modifier = modifier.fillMaxWidth(),
        color = Background.CardDark,
        onClick = onClick,
        contentPadding = PaddingValues(18.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = training.name,
                style = MaterialTheme.typography.headlineSmall,
                color = TextColor.Primary,
                fontWeight = FontWeight.Bold,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeBadge(text = "Training")
                HomeBadge(
                    text = when {
                        training.supportsWhite && training.supportsBlack -> "White + Black"
                        training.supportsWhite -> "As White"
                        training.supportsBlack -> "As Black"
                        else -> "Mixed"
                    },
                    background = Color(0xFF203327),
                    contentColor = Color(0xFF59D98E),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HomeMetricBox(
                    title = "Games",
                    value = training.gamesCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                HomeMetricBox(
                    title = "Training",
                    value = "#${training.trainingId}",
                    modifier = Modifier.weight(1f),
                )
                HomeMetricBox(
                    title = "Sides",
                    value = when {
                        training.supportsWhite && training.supportsBlack -> "Both"
                        training.supportsWhite -> "White"
                        training.supportsBlack -> "Black"
                        else -> "-"
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HomeBadge(
    text: String,
    background: Color = HomeBadgeBackground,
    contentColor: Color = TextColor.Secondary,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        CardMetaText(
            text = text,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun HomeMetricBox(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(HomeMetricCard)
            .padding(vertical = 14.dp, horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CardMetaText(
            text = title,
            color = Color(0xFF9AA0B3),
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = TextColor.Primary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SmartTrainingBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TrainingAccentTeal)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SmartTrainingIconBg),
            contentAlignment = Alignment.Center,
        ) {
            IconMd(
                imageVector = Icons.Filled.Psychology,
                contentDescription = null,
                tint = Color.White,
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Smart Training",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconSm(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = Color.White,
                )
            }
            Text(
                text = "Create personalized practice sessions",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}
