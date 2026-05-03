package com.example.chessboard.ui.screen.home

/**
 * File role: holds only UI blocks that are reused by both home-screen branches.
 * Allowed here:
 * - composables used by both regular home and SimpleView home
 * Not allowed here:
 * - branch-specific models or styling
 * - UI used by only one home branch
 * Validation date: 2026-05-03
 */
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.theme.TrainingAccentTeal

@Composable
internal fun AddOpeningButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(TrainingAccentTeal)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        IconMd(
            imageVector = Icons.Default.Add,
            contentDescription = "Add opening",
            tint = Color.White,
        )
    }
}

@Composable
internal fun HomeBottomNavigation(
    onItemSelected: (ScreenType) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppBottomNavigation(
        items = defaultAppBottomNavigationItems(),
        selectedItem = ScreenType.Home,
        onItemSelected = onItemSelected,
        modifier = modifier,
    )
}
