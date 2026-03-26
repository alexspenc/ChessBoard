package com.example.chessboard.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingSurfaceDark

@Composable
private fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
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
    heighDp : Dp = AppDimens.buttonHeight,
    shape : Dp = AppDimens.radiusLg,
    elevation : ButtonElevation = ButtonDefaults.buttonElevation(
        defaultElevation = 2.dp,
        pressedElevation = 4.dp
    ),
    containerColor : Color = TrainingAccentTeal,
    contentColor : Color = Color.White,
    fontWeight : FontWeight = FontWeight.SemiBold,
    fontSize : Int = 14,
) {
    AppButton(
        text = text,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
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
    heighDp: Dp = 30.dp,
    shape: Dp = AppDimens.radiusMd,
    elevation: ButtonElevation = ButtonDefaults.buttonElevation(
        defaultElevation = 0.dp,
        pressedElevation = 1.dp
    ),
    containerColor: Color = TrainingAccentTeal,
    contentColor: Color = Color.White,
    fontWeight: FontWeight = FontWeight.SemiBold,
    fontSize: Int = 12,
) {
    AppButton(
        text = text,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
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
