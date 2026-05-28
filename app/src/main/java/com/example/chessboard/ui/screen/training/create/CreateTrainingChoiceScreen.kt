package com.example.chessboard.ui.screen.training.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.chessboard.R
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.HomeIconButton
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.theme.AppDimens

@Composable
fun CreateTrainingChoiceScreenContainer(
    screenContext: ScreenContainerContext,
    modifier: Modifier = Modifier,
) {
    CreateTrainingChoiceScreen(
        onBackClick = screenContext.onBackClick,
        onNavigate = screenContext.onNavigate,
        modifier = modifier
    )
}

@Composable
private fun CreateTrainingChoiceScreen(
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedNavItem by remember { mutableStateOf<ScreenType>(ScreenType.Training) }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.create_training_choice_title),
                onBackClick = onBackClick,
                actions = {
                    HomeIconButton(onClick = { onNavigate(ScreenType.Home) })
                },
            )
        },
        bottomBar = {
            AppBottomNavigation(
                items = defaultAppBottomNavigationItems(),
                selectedItem = selectedNavItem,
                onItemSelected = {
                    selectedNavItem = it
                    onNavigate(it)
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            ScreenSection {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PrimaryButton(
                        text = stringResource(R.string.create_training_choice_all_lines),
                        onClick = { onNavigate(ScreenType.CreateTraining) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(AppDimens.spaceMd))
                    PrimaryButton(
                        text = stringResource(R.string.create_training_choice_statistics),
                        onClick = { onNavigate(ScreenType.CreateTrainingByStatistics) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(AppDimens.spaceMd))
                    PrimaryButton(
                        text = stringResource(R.string.create_training_choice_template),
                        onClick = { onNavigate(ScreenType.TrainingTemplateSelection) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
