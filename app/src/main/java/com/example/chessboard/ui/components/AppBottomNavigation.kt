package com.example.chessboard.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingIconInactive

data class AppBottomNavigationItem<T>(
    val value: T,
    val label: String,
    val iconUnselected: ImageVector,
    val iconSelected: ImageVector
)

fun defaultAppBottomNavigationItems(): List<AppBottomNavigationItem<ScreenType>> {
    return listOf(
        AppBottomNavigationItem(
            value = ScreenType.Home,
            label = ScreenType.Home.toString(),
            iconUnselected = Icons.Outlined.Home,
            iconSelected = Icons.Filled.Home
        ),
        AppBottomNavigationItem(
            value = ScreenType.Training,
            label = ScreenType.Training.toString(),
            iconUnselected = Icons.Outlined.AccountBox,
            iconSelected = Icons.Filled.AccountBox
        ),
        AppBottomNavigationItem(
            value = ScreenType.GamesExplorer,
            label = ScreenType.GamesExplorer.toString(),
            iconUnselected = Icons.Outlined.Info,
            iconSelected = Icons.Filled.Info
        ),
        AppBottomNavigationItem(
            value = ScreenType.Stats,
            label = ScreenType.Stats.toString(),
            iconUnselected = Icons.Outlined.Info,
            iconSelected = Icons.Filled.Info
        ),
        AppBottomNavigationItem(
            value = ScreenType.Profile,
            label = ScreenType.Profile.toString(),
            iconUnselected = Icons.Outlined.Person,
            iconSelected = Icons.Filled.Person
        )
    )
}

/** Displays the app's standard bottom navigation with icons and labels. */
@Composable
fun <T> AppBottomNavigation(
    items: List<AppBottomNavigationItem<T>>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    maxVisibleItems: Int = 4,
    showTopDivider: Boolean = true,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Background.SurfaceDark,
        tonalElevation = 8.dp
    ) {
        Column {
            if (showTopDivider) {
                AppDivider()
            }

            BoxWithConstraints {
                val normalizedMaxVisibleItems = maxVisibleItems.coerceAtLeast(1)
                val itemsCount = items.size.coerceAtLeast(1)
                val visibleItemsCount = itemsCount.coerceAtMost(normalizedMaxVisibleItems)
                val itemWidth = maxWidth / visibleItemsCount

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = AppDimens.spaceSm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEach { item ->
                        val isSelected = selectedItem == item.value
                        val color = if (isSelected) TrainingAccentTeal else TrainingIconInactive

                        Column(
                            modifier = Modifier
                                .width(itemWidth)
                                .clickable { onItemSelected(item.value) }
                                .padding(AppDimens.spaceSm),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.iconSelected else item.iconUnselected,
                                contentDescription = item.label,
                                tint = color,
                                modifier = Modifier.size(AppDimens.navIconSize)
                            )
                            Spacer(modifier = Modifier.height(AppDimens.spaceXs))
                            NavLabelText(
                                text = item.label,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = color,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
