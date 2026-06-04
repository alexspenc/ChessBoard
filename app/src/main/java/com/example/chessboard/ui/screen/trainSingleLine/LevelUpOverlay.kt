package com.example.chessboard.ui.screen.trainSingleLine

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessboard.R
import com.example.chessboard.ui.theme.TrainingAccentTeal

private val CardBg = Color(0xFF121418)
private val BadgeBg = Color(0xFF1A3028)
private val DotColor = Color(0xFF1DB584).copy(alpha = 0.28f)

@Composable
internal fun LevelUpDialog(
    tierSymbol: String,
    levelNumber: Int,
    rankTitle: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "levelUp")
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -18f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bounce",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(300.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CardBg),
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(DotColor, 5.dp.toPx(), Offset(22.dp.toPx(), 22.dp.toPx()))
                drawCircle(DotColor, 4.dp.toPx(), Offset(size.width - 18.dp.toPx(), 30.dp.toPx()))
                drawCircle(DotColor, 6.dp.toPx(), Offset(size.width - 32.dp.toPx(), 72.dp.toPx()))
                drawCircle(DotColor, 4.dp.toPx(), Offset(18.dp.toPx(), size.height - 44.dp.toPx()))
                drawCircle(DotColor, 5.dp.toPx(), Offset(size.width - 22.dp.toPx(), size.height - 26.dp.toPx()))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = tierSymbol,
                    fontSize = 72.sp,
                    color = TrainingAccentTeal,
                    modifier = Modifier.graphicsLayer { translationY = bounceOffset },
                )

                Text(
                    text = stringResource(R.string.train_single_line_level_up_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 3.sp,
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(BadgeBg)
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.train_single_line_level_number, levelNumber),
                        color = TrainingAccentTeal,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                Text(
                    text = rankTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TrainingAccentTeal,
                    fontWeight = FontWeight.Medium,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.train_single_line_level_up_continue),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.35f),
                )
            }
        }
    }
}
