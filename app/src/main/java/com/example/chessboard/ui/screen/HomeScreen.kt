package com.example.chessboard.ui.screen

import android.app.Activity
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.components.ScreenTitleText
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.ButtonColor
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal

@Composable
fun HomeScreenContainer(
    activity: Activity,
    onNavigate: (ScreenType) -> Unit = {},
    onCreateTrainingClick: () -> Unit = {},
    onStartFirstTrainingClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    HomeScreen(
        onNavigate = onNavigate,
        onCreateTrainingClick = onCreateTrainingClick,
        onStartFirstTrainingClick = onStartFirstTrainingClick,
        modifier = modifier
    )
}

@Composable
fun HomeScreen(
    onNavigate: (ScreenType) -> Unit = {},
    onCreateTrainingClick: () -> Unit = {},
    onStartFirstTrainingClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            HomeBottomNavigation(onItemSelected = onNavigate)
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = AppDimens.spaceLg,
                top = 20.dp,
                end = AppDimens.spaceLg,
                bottom = AppDimens.spaceLg
            ),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("♛", fontSize = 26.sp, color = TrainingAccentTeal)
                            Spacer(modifier = Modifier.width(AppDimens.spaceSm))
                            Text(
                                text = "Chess Openings",
                                style = MaterialTheme.typography.displaySmall,
                                color = TextColor.Primary
                            )
                        }
                        SectionTitleText(
                            text = "Home",
                            color = TextColor.Secondary
                        )
                    }
                    AddOpeningButton(
                        onClick = { onNavigate(ScreenType.CreateOpening) }
                    )
                }
            }

            item {
                ScreenSection {
                    BodySecondaryText(
                        text = "Choose what you want to do.",
                        color = TextColor.Secondary
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
                ) {
                    HomeActionCard(
                        title = "Trainings",
                        subtitle = "Open saved training plans",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(ScreenType.Training) }
                    )
                    HomeActionCard(
                        title = "Games",
                        subtitle = "Browse saved openings",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(ScreenType.GamesExplorer) }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
                ) {
                    HomeActionCard(
                        title = "Create Training",
                        subtitle = "Build a training from saved games",
                        modifier = Modifier.weight(1f),
                        onClick = onCreateTrainingClick
                    )
                    HomeActionCard(
                        title = "Start First",
                        subtitle = "Launch the first available line",
                        modifier = Modifier.weight(1f),
                        onClick = onStartFirstTrainingClick
                    )
                }
            }

            item {
                ScreenSection {
                    PrimaryButton(
                        text = "Create Opening",
                        onClick = { onNavigate(ScreenType.CreateOpening) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeActionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CardSurface(
        modifier = modifier,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)
            ) {
                ScreenTitleText(text = title)
                CardMetaText(text = subtitle)
            }
        }
    }
}

@Composable
private fun AddOpeningButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.size(AppDimens.buttonHeight),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(AppDimens.radiusLg),
        colors = ButtonDefaults.buttonColors(containerColor = ButtonColor.PrimaryContainer),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add opening",
            tint = ButtonColor.Content,
            modifier = Modifier.size(AppDimens.navIconSize)
        )
    }
}

@Composable
private fun HomeBottomNavigation(
    onItemSelected: (ScreenType) -> Unit,
    modifier: Modifier = Modifier
) {
    AppBottomNavigation(
        items = defaultAppBottomNavigationItems(),
        selectedItem = ScreenType.Home,
        onItemSelected = onItemSelected,
        modifier = modifier
    )
}
