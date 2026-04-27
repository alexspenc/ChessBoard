package com.example.chessboard.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.ButtonColor
import kotlinx.coroutines.delay

@Composable
private fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    heighDp: Dp,
    shape: Dp,
    elevation: ButtonElevation,
    containerColor: Color,
    contentColor: Color,
    disabledContainerColor: Color,
    disabledContentColor: Color,
    fontWeight: FontWeight,
    fontSize: Int,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier.height(height = heighDp),
        shape = RoundedCornerShape(size = shape),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        ),
        elevation = elevation,
    ) {
        Text(
            text = text,
            fontWeight = fontWeight,
            fontSize = fontSize.sp
        )
    }
}

/** Renders the main call-to-action button with the app's default visual style. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    heighDp : Dp = AppDimens.buttonHeight,
    shape : Dp = AppDimens.radiusLg,
    elevation : ButtonElevation = ButtonDefaults.buttonElevation(
        defaultElevation = 2.dp,
        pressedElevation = 4.dp
    ),
    containerColor : Color = ButtonColor.PrimaryContainer,
    contentColor : Color = ButtonColor.Content,
    fontWeight : FontWeight = FontWeight.SemiBold,
    fontSize : Int = 14,
) {
    AppButton(
        text = text,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        interactionSource = interactionSource,
        heighDp = heighDp,
        shape = shape,
        elevation = elevation,
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = containerColor,
        disabledContentColor = contentColor,
        fontWeight = fontWeight,
        fontSize = fontSize
    )
}

/** Renders a compact secondary action button for dense controls and pagination. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    heighDp: Dp = 30.dp,
    shape: Dp = AppDimens.radiusMd,
    elevation: ButtonElevation = ButtonDefaults.buttonElevation(
        defaultElevation = 0.dp,
        pressedElevation = 1.dp
    ),
    containerColor: Color = ButtonColor.PrimaryContainer,
    contentColor: Color = ButtonColor.Content,
    fontWeight: FontWeight = FontWeight.SemiBold,
    fontSize: Int = 12,
) {
    AppButton(
        text = text,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        interactionSource = interactionSource,
        heighDp = heighDp,
        shape = shape,
        elevation = elevation,
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = containerColor,
        disabledContentColor = contentColor,
        fontWeight = fontWeight,
        fontSize = fontSize
    )
}

/** Renders a button that fires immediately on press and repeats while held. */
@Composable
fun RepeatStepButton(
    text: String,
    onStep: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    repeatIntervalMillis: Long = 200L,
    heighDp: Dp = 30.dp,
    shape: Dp = AppDimens.radiusMd,
    elevation: ButtonElevation = ButtonDefaults.buttonElevation(
        defaultElevation = 0.dp,
        pressedElevation = 1.dp
    ),
    containerColor: Color = ButtonColor.PrimaryContainer,
    contentColor: Color = ButtonColor.Content,
    fontWeight: FontWeight = FontWeight.SemiBold,
    fontSize: Int = 12,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val currentOnStep by rememberUpdatedState(onStep)

    LaunchedEffect(enabled, isPressed, repeatIntervalMillis) {
        if (!enabled || !isPressed) {
            return@LaunchedEffect
        }

        delay(repeatIntervalMillis)
        while (true) {
            currentOnStep()
            delay(repeatIntervalMillis)
        }
    }

    SecondaryButton(
        text = text,
        onClick = {},
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource,
        heighDp = heighDp,
        shape = shape,
        elevation = elevation,
        containerColor = containerColor,
        contentColor = contentColor,
        fontWeight = fontWeight,
        fontSize = fontSize,
    )
}

@Composable
fun RepeatStepIconButton(
    icon: ImageVector,
    contentDescription: String,
    onStep: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    repeatIntervalMillis: Long = 200L,
    buttonSize: Dp = AppIconSizes.Md,
    shape: Dp = AppDimens.radiusMd,
    containerColor: Color = ButtonColor.PrimaryContainer,
    contentColor: Color = ButtonColor.Content,
) {
    fun resolveContainerColor(): Color {
        if (enabled) {
            return containerColor
        }

        return containerColor.copy(alpha = 0.5f)
    }

    fun resolveContentColor(): Color {
        if (enabled) {
            return contentColor
        }

        return contentColor.copy(alpha = 0.5f)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val currentOnStep by rememberUpdatedState(onStep)

    LaunchedEffect(enabled, isPressed, repeatIntervalMillis) {
        if (!enabled || !isPressed) {
            return@LaunchedEffect
        }

        delay(repeatIntervalMillis)
        while (true) {
            currentOnStep()
            delay(repeatIntervalMillis)
        }
    }

    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = resolveContentColor(),
        modifier = modifier
            .size(buttonSize)
            .clip(RoundedCornerShape(size = shape))
            .background(resolveContainerColor())
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onStep
            )
            .padding(6.dp)
    )
}
