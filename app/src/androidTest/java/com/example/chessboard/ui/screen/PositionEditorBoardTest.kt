package com.example.chessboard.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.ui.InteractiveChessBoardTestTag
import com.example.chessboard.ui.PositionEditorBoardWithCoordinates
import com.example.chessboard.ui.components.SecondaryButton
import com.example.chessboard.ui.theme.ChessBoardTheme
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class PositionEditorBoardTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun positionEditorBoard_updatesVisibleFenWhenPreviewPositionChanges() {
        val gameController = GameController()

        composeRule.setContent {
            ChessBoardTheme {
                PositionEditorBoardHost(gameController = gameController)
            }
        }

        composeRule.runOnIdle {
            gameController.loadPreviewFen("8/8/8/8/8/8/8/4K3 w - - 0 1")
        }

        assertBoardFen("8/8/8/8/8/8/8/4K3 w - - 0 1")

        composeRule.runOnIdle {
            gameController.loadPreviewFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1")
        }

        assertBoardFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1")
    }


    @Test
    fun positionEditorBoard_dragAndDropUpdatesVisibleFen() {
        val gameController = GameController()

        composeRule.setContent {
            ChessBoardTheme {
                PositionEditorInteractiveBoardHost(gameController = gameController)
            }
        }

        composeRule.runOnIdle {
            gameController.loadPreviewFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1")
        }

        val boardNode = composeRule.onNodeWithTag(InteractiveChessBoardTestTag)
        val squareSize = with(composeRule.density) { 320.dp.toPx() } / 8f

        boardNode.performTouchInput {
            val from = squareCenter(square = "e1", squareSize = squareSize)
            val to = squareCenter(square = "e2", squareSize = squareSize)
            down(from)
            moveTo(to)
            up()
        }

        assertBoardFen("4k3/8/8/8/8/8/4K3/8 w - - 0 1")
    }


    @Test
    fun positionEditorBoard_tapPlacesPieceAndUpdatesVisibleFen() {
        val gameController = GameController()

        composeRule.setContent {
            ChessBoardTheme {
                PositionEditorPlacementBoardHost(gameController = gameController)
            }
        }

        composeRule.runOnIdle {
            gameController.loadPreviewFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1")
        }

        val boardNode = composeRule.onNodeWithTag(InteractiveChessBoardTestTag)
        val squareSize = with(composeRule.density) { 320.dp.toPx() } / 8f

        boardNode.performTouchInput {
            click(squareCenter(square = "d4", squareSize = squareSize))
        }

        assertBoardFen("4k3/8/8/8/3Q4/8/8/4K3 w - - 0 1")
    }


    @Test
    fun positionEditorBoard_clearBoardButtonUpdatesVisibleFen() {
        val gameController = GameController()

        composeRule.setContent {
            ChessBoardTheme {
                PositionEditorClearBoardHost(gameController = gameController)
            }
        }

        composeRule.onNodeWithText("Clear board").performClick()

        assertBoardFen("8/8/8/8/8/8/8/8 w - - 0 1")
    }


    @Test
    fun positionEditorBoard_initialPositionButtonUpdatesVisibleFen() {
        val gameController = GameController()

        composeRule.setContent {
            ChessBoardTheme {
                PositionEditorInitialPositionHost(gameController = gameController)
            }
        }

        composeRule.onNodeWithText("Initial position").performClick()

        assertBoardFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
    }

    private fun assertBoardFen(expectedFen: String) {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(InteractiveChessBoardTestTag).assert(
            SemanticsMatcher.expectValue(
                androidx.compose.ui.semantics.SemanticsProperties.StateDescription,
                expectedFen
            )
        )
    }
}

@Composable
private fun PositionEditorBoardHost(gameController: GameController) {
    val boardState = gameController.boardState

    key(boardState) {
        PositionEditorBoardWithCoordinates(
            gameController = gameController,
            onSquareClick = {},
            onPieceMove = { _, _ -> },
            modifier = Modifier.size(320.dp)
        )
    }
}

@Composable
private fun PositionEditorInteractiveBoardHost(gameController: GameController) {
    val boardState = gameController.boardState

    key(boardState) {
        PositionEditorBoardWithCoordinates(
            gameController = gameController,
            onSquareClick = {},
            onPieceMove = { fromSquare, toSquare ->
                val updatedFen = movePieceInPreviewFen(
                    fen = gameController.getFen(),
                    fromSquare = fromSquare,
                    toSquare = toSquare
                )
                gameController.loadPreviewFen(updatedFen)
            },
            modifier = Modifier.size(320.dp)
        )
    }
}

@Composable
private fun PositionEditorPlacementBoardHost(gameController: GameController) {
    val boardState = gameController.boardState

    key(boardState) {
        PositionEditorBoardWithCoordinates(
            gameController = gameController,
            onSquareClick = { square ->
                val updatedFen = placePieceInPreviewFen(
                    fen = gameController.getFen(),
                    square = square,
                    pieceLetter = 'Q'
                )
                gameController.loadPreviewFen(updatedFen)
            },
            onPieceMove = { _, _ -> },
            modifier = Modifier.size(320.dp)
        )
    }
}

@Composable
private fun PositionEditorClearBoardHost(gameController: GameController) {
    Column {
        SecondaryButton(
            text = "Clear board",
            onClick = {
                gameController.loadPreviewFen("8/8/8/8/8/8/8/8 w - - 0 1")
            }
        )

        val boardState = gameController.boardState
        key(boardState) {
            PositionEditorBoardWithCoordinates(
                gameController = gameController,
                onSquareClick = {},
                onPieceMove = { _, _ -> },
                modifier = Modifier.size(320.dp)
            )
        }
    }
}

@Composable
private fun PositionEditorInitialPositionHost(gameController: GameController) {
    Column {
        SecondaryButton(
            text = "Initial position",
            onClick = {
                gameController.resetToStartPosition()
            }
        )

        val boardState = gameController.boardState
        key(boardState) {
            PositionEditorBoardWithCoordinates(
                gameController = gameController,
                onSquareClick = {},
                onPieceMove = { _, _ -> },
                modifier = Modifier.size(320.dp)
            )
        }
    }
}

private fun placePieceInPreviewFen(
    fen: String,
    square: String,
    pieceLetter: Char
): String {
    val boardPart = fen.substringBefore(' ')
    val metadata = fen.substringAfter(' ', "w - - 0 1")
    val pieces = parsePieces(boardPart)
    val currentPiece = pieces.find { piece -> piece.second == square }
    val updatedPieces = pieces.filterNot { piece -> piece.second == square }.toMutableList()

    if (currentPiece?.first != pieceLetter) {
        updatedPieces += pieceLetter to square
    }

    return buildFen(updatedPieces, metadata)
}

private fun movePieceInPreviewFen(
    fen: String,
    fromSquare: String,
    toSquare: String
): String {
    if (fromSquare == toSquare) {
        return fen
    }

    val boardPart = fen.substringBefore(' ')
    val metadata = fen.substringAfter(' ', "w - - 0 1")
    val pieces = parsePieces(boardPart).toMutableList()
    val movingPiece = pieces.find { piece -> piece.second == fromSquare } ?: return fen
    pieces.removeAll { piece -> piece.second == fromSquare || piece.second == toSquare }
    pieces += movingPiece.first to toSquare
    return buildFen(pieces, metadata)
}

private fun parsePieces(boardPart: String): List<Pair<Char, String>> {
    val pieces = mutableListOf<Pair<Char, String>>()
    val ranks = boardPart.split('/')

    ranks.forEachIndexed { rowIndex, rank ->
        var fileIndex = 0
        rank.forEach { symbol ->
            if (symbol.isDigit()) {
                fileIndex += symbol.digitToInt()
                return@forEach
            }

            val square = "${'a' + fileIndex}${8 - rowIndex}"
            pieces += symbol to square
            fileIndex += 1
        }
    }

    return pieces
}

private fun buildFen(
    pieces: List<Pair<Char, String>>,
    metadata: String
): String {
    val board = Array(8) { CharArray(8) { ' ' } }

    pieces.forEach { (letter, square) ->
        val row = 8 - square[1].digitToInt()
        val col = square[0] - 'a'
        board[row][col] = letter
    }

    val boardPart = buildString {
        for (row in 0 until 8) {
            var emptySquares = 0
            for (col in 0 until 8) {
                val piece = board[row][col]
                if (piece == ' ') {
                    emptySquares += 1
                    continue
                }

                if (emptySquares > 0) {
                    append(emptySquares)
                    emptySquares = 0
                }
                append(piece)
            }

            if (emptySquares > 0) {
                append(emptySquares)
            }

            if (row < 7) {
                append('/')
            }
        }
    }

    return "$boardPart $metadata"
}

private fun squareCenter(square: String, squareSize: Float): Offset {
    val file = square[0] - 'a'
    val rank = square[1].digitToInt()
    val row = 8 - rank

    return Offset(
        x = file * squareSize + squareSize / 2f,
        y = row * squareSize + squareSize / 2f
    )
}
