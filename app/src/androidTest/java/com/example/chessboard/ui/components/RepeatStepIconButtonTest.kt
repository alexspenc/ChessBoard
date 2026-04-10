package com.example.chessboard.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.center
import com.example.chessboard.ui.theme.ChessBoardTheme
import org.junit.Rule
import org.junit.Test

class RepeatStepIconButtonTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun repeatStepIconButton_singleTapTriggersSingleStep() {
        composeRule.setContent {
            ChessBoardTheme {
                var value by remember { mutableIntStateOf(0) }

                Column {
                    androidx.compose.material3.Text(value.toString())
                    RepeatStepIconButton(
                        icon = Icons.Default.Add,
                        contentDescription = "Increase value",
                        onStep = { value += 1 }
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription("Increase value").performTouchInput {
            click(center)
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("1").assertTextEquals("1")
    }
}
