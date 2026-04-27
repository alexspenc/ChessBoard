package com.example.chessboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TrainingTextPrimary

/** Displays the app's standard top bar with an optional subtitle, back action, and action slot. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    filledBackButton: Boolean = false,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Background.ScreenDark,
            navigationIconContentColor = TrainingTextPrimary,
            titleContentColor = TrainingTextPrimary
        ),
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .padding(start = AppDimens.spaceSm)
                        .size(AppDimens.iconButtonSize)
                        .background(
                            color = if (filledBackButton) Background.SurfaceDark else Color.Transparent,
                            shape = RoundedCornerShape(AppDimens.radiusMd)
                        )
                ) {
                    IconSm(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TrainingTextPrimary,
                    )
                }
            }
        },
        title = {
            Column {
                ScreenTitleText(text = title)
                if (!subtitle.isNullOrBlank()) {
                    BodySecondaryText(text = subtitle)
                }
            }
        },
        actions = {
            actions()
        }
    )
}
