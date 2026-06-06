package com.example.chessboard.ui.screen.home

/**
 * File role: covers HomeScreenContainer navigation behavior that depends on app data.
 * Allowed here:
 * - integration-style Compose tests for home container callbacks and dialogs
 * - small database fixtures needed to drive home navigation decisions
 * Not allowed here:
 * - broad MainActivity navigation coverage
 * - low-level line-saving or training-service assertions
 * Validation date: 2026-05-14
 */
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.chessboard.entity.LineEntity
import com.example.chessboard.entity.SideMask
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.service.OneLineTrainingData
import com.example.chessboard.service.uciMovesToMoves
import com.example.chessboard.ui.HomeNoLinesCreateOpeningTestTag
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.screen.ScreenType
import com.example.chessboard.ui.theme.ChessBoardTheme
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HomeScreenContainerTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val dbProvider: DatabaseProvider
        get() = DatabaseProvider.createInstance(composeRule.activity)

    @Before
    fun setUp() {
        dbProvider.clearAllData()
    }

    @Test
    fun simpleHome_smartTrainingClickWithoutLinesShowsCreateOpeningDialog() {
        var createOpeningClicks = 0

        setHomeContent(
            onCreateOpeningClick = {
                createOpeningClicks += 1
            },
        )

        waitForTextDisplayed("Smart Training")
        composeRule.onNodeWithText("Smart Training").performClick()

        waitForTextDisplayed("No openings yet")
        composeRule.onNodeWithText(
            "Create at least one opening or line before starting Smart Training."
        ).assertIsDisplayed()

        composeRule.onNodeWithTag(HomeNoLinesCreateOpeningTestTag).performClick()

        composeRule.runOnIdle {
            check(createOpeningClicks == 1) {
                "Expected Create Opening callback to be called once, got $createOpeningClicks"
            }
        }
    }

    @Test
    fun simpleHome_smartTrainingClickWithLineWithoutTrainingShowsCreateTrainingDialog() {
        saveLine()
        var createTrainingClicks = 0
        var navigatedScreen: ScreenType? = null

        setHomeContent(
            onNavigate = { screen ->
                navigatedScreen = screen
            },
            onCreateTrainingClick = {
                createTrainingClicks += 1
            },
        )

        waitForTextDisplayed("Smart Training")
        composeRule.onNodeWithText("Smart Training").performClick()

        waitForTextDisplayed("No training yet")
        composeRule.onNodeWithText(
            "Create at least one training before starting Smart Training."
        ).assertIsDisplayed()

        composeRule.onNodeWithText("Create Training").performClick()

        composeRule.runOnIdle {
            check(createTrainingClicks == 1) {
                "Expected Create Training callback to be called once, got $createTrainingClicks"
            }
            check(navigatedScreen == null) {
                "Expected Smart Training navigation to be blocked, got $navigatedScreen"
            }
        }
    }

    @Test
    fun simpleHome_smartTrainingClickWithLineAndTrainingNavigatesToSmartTraining() {
        val lineId = saveLine()
        saveTraining(lineId)
        var navigatedScreen: ScreenType? = null

        setHomeContent(
            onNavigate = { screen ->
                navigatedScreen = screen
            },
        )

        waitForTextDisplayed("Smart Training")
        composeRule.onNodeWithText("Smart Training").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            navigatedScreen == ScreenType.SmartTraining
        }
        composeRule.onAllNodesWithText("No openings yet").fetchSemanticsNodes()
            .let { nodes ->
                check(nodes.isEmpty()) {
                    "Expected no missing-openings dialog after Smart Training navigation"
                }
            }
        composeRule.onAllNodesWithText("No training yet").fetchSemanticsNodes()
            .let { nodes ->
                check(nodes.isEmpty()) {
                    "Expected no missing-training dialog after Smart Training navigation"
                }
            }
    }

    @Test
    fun regularHome_trainingsCardWithoutLinesShowsCreateOpeningDialog() {
        var createOpeningClicks = 0
        var navigatedScreen: ScreenType? = null

        setHomeContent(
            simpleViewEnabled = false,
            onNavigate = { screen ->
                navigatedScreen = screen
            },
            onCreateOpeningClick = {
                createOpeningClicks += 1
            },
        )

        waitForTextDisplayed("Trainings")
        composeRule.onNode(hasText("Trainings") and hasClickAction()).performClick()

        assertNoLinesDialogAndCreateOpening(
            expectedMessage = "Create at least one opening or line before opening Trainings.",
            createOpeningClicks = { createOpeningClicks },
        )
        composeRule.runOnIdle {
            check(navigatedScreen == null) {
                "Expected Training navigation to be blocked, got $navigatedScreen"
            }
        }
    }

    @Test
    fun regularHome_trainingBottomBarWithoutLinesShowsCreateOpeningDialog() {
        var createOpeningClicks = 0
        var navigatedScreen: ScreenType? = null

        setHomeContent(
            simpleViewEnabled = false,
            onNavigate = { screen ->
                navigatedScreen = screen
            },
            onCreateOpeningClick = {
                createOpeningClicks += 1
            },
        )

        waitForTextDisplayed("Training")
        composeRule.onNode(hasText("Training") and hasClickAction()).performClick()

        assertNoLinesDialogAndCreateOpening(
            expectedMessage = "Create at least one opening or line before opening Trainings.",
            createOpeningClicks = { createOpeningClicks },
        )
        composeRule.runOnIdle {
            check(navigatedScreen == null) {
                "Expected Training navigation to be blocked, got $navigatedScreen"
            }
        }
    }

    @Test
    fun simpleHome_trainingBottomBarWithoutLinesShowsCreateOpeningDialog() {
        var createOpeningClicks = 0
        var navigatedScreen: ScreenType? = null

        setHomeContent(
            onNavigate = { screen ->
                navigatedScreen = screen
            },
            onCreateOpeningClick = {
                createOpeningClicks += 1
            },
        )

        waitForTextDisplayed("Training")
        composeRule.onNode(hasText("Training") and hasClickAction()).performClick()

        assertNoLinesDialogAndCreateOpening(
            expectedMessage = "Create at least one opening or line before opening Trainings.",
            createOpeningClicks = { createOpeningClicks },
        )
        composeRule.runOnIdle {
            check(navigatedScreen == null) {
                "Expected Training navigation to be blocked, got $navigatedScreen"
            }
        }
    }

    @Test
    fun regularHome_trainingsCardWithLineNavigatesToTraining() {
        saveLine()
        var navigatedScreen: ScreenType? = null

        setHomeContent(
            simpleViewEnabled = false,
            onNavigate = { screen ->
                navigatedScreen = screen
            },
        )

        waitForTextDisplayed("Trainings")
        composeRule.onNode(hasText("Trainings") and hasClickAction()).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            navigatedScreen == ScreenType.Training
        }
        composeRule.onAllNodesWithText("No openings yet").fetchSemanticsNodes()
            .let { nodes ->
                check(nodes.isEmpty()) {
                    "Expected no missing-openings dialog after Training navigation"
                }
            }
    }

    private fun setHomeContent(
        simpleViewEnabled: Boolean = true,
        onNavigate: (ScreenType) -> Unit = {},
        onCreateOpeningClick: () -> Unit = {},
        onCreateTrainingClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            ChessBoardTheme {
                HomeScreenContainer(
                    activity = composeRule.activity,
                    screenContext = ScreenContainerContext(
                        onNavigate = onNavigate,
                        inDbProvider = dbProvider,
                    ),
                    simpleViewEnabled = simpleViewEnabled,
                    onCreateOpeningClick = onCreateOpeningClick,
                    onCreateTrainingClick = onCreateTrainingClick,
                )
            }
        }
    }

    private fun waitForTextDisplayed(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }

    private fun assertNoLinesDialogAndCreateOpening(
        expectedMessage: String,
        createOpeningClicks: () -> Int,
    ) {
        waitForTextDisplayed("No openings yet")
        composeRule.onNodeWithText(expectedMessage).assertIsDisplayed()

        composeRule.onNodeWithTag(HomeNoLinesCreateOpeningTestTag).performClick()

        composeRule.runOnIdle {
            check(createOpeningClicks() == 1) {
                "Expected Create Opening callback to be called once, got ${createOpeningClicks()}"
            }
        }
    }

    private fun saveLine(): Long {
        return runBlocking {
            val line = LineEntity(
                event = "Smart Training Source",
                pgn = storedPgn(listOf("e2e4", "e7e5")),
                initialFen = "",
                sideMask = SideMask.BOTH,
            )

            val lineId = dbProvider.createLineSaver().saveLine(
                line = line,
                moves = uciMovesToMoves(listOf("e2e4", "e7e5")),
                sideMask = line.sideMask,
            )
            checkNotNull(lineId) {
                "Expected test line to be saved"
            }
        }
    }

    private fun saveTraining(lineId: Long) {
        runBlocking {
            val trainingId = dbProvider.createTrainingService().createTrainingFromLines(
                name = "Training",
                lines = listOf(OneLineTrainingData(lineId = lineId, weight = 1)),
            )
            checkNotNull(trainingId) {
                "Expected test training to be saved"
            }
        }
    }

    private fun storedPgn(moves: List<String>): String {
        return buildString {
            append("[Event \"Smart Training Source\"]\n")
            append("[White \"White\"]\n")
            append("[Black \"Black\"]\n")
            append("[Result \"*\"]\n\n")

            moves.forEachIndexed { index, move ->
                if (index % 2 == 0) {
                    append("${index / 2 + 1}. ")
                }

                append(move)
                append(" ")
            }

            append("*")
        }
    }
}
