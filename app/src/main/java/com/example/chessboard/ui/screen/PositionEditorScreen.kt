package com.example.chessboard.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessboard.boardmodel.BoardPiece
import com.example.chessboard.boardmodel.BoardPosition
import com.example.chessboard.boardmodel.ChesslibMapper
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.ui.PositionEditorBoardWithCoordinates
import com.example.chessboard.ui.components.AppBottomNavigation
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.TrainingDividerColor
import com.example.chessboard.ui.theme.TrainingIconInactive
import com.example.chessboard.ui.theme.TrainingTextPrimary

private const val EmptyBoardFen = "8/8/8/8/8/8/8/8 w - - 0 1"
private val SideButtonSelectedBg = Color(0xFF2C2C2C)

private data class PositionEditorPieceOption(
    val letter: Char,
    val label: String
)

private val PositionEditorPieceOptions = listOf(
    PositionEditorPieceOption('K', "White King"),
    PositionEditorPieceOption('Q', "White Queen"),
    PositionEditorPieceOption('R', "White Rook"),
    PositionEditorPieceOption('B', "White Bishop"),
    PositionEditorPieceOption('N', "White Knight"),
    PositionEditorPieceOption('P', "White Pawn"),
    PositionEditorPieceOption('k', "Black King"),
    PositionEditorPieceOption('q', "Black Queen"),
    PositionEditorPieceOption('r', "Black Rook"),
    PositionEditorPieceOption('b', "Black Bishop"),
    PositionEditorPieceOption('n', "Black Knight"),
    PositionEditorPieceOption('p', "Black Pawn")
)

@Composable
fun PositionEditorScreenContainer(
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val gameController = remember { GameController() }
    var fenText by remember { mutableStateOf(EmptyBoardFen) }
    var selectedSide by remember { mutableStateOf(EditableGameSide.AS_WHITE) }
    var selectedPiece by remember { mutableStateOf(PositionEditorPieceOptions.first()) }
    var fenError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        gameController.loadFromFen(EmptyBoardFen)
    }

    LaunchedEffect(selectedSide) {
        gameController.setOrientation(selectedSide.orientation)
    }

    PositionEditorScreen(
        gameController = gameController,
        fenText = fenText,
        onFenTextChange = { fenText = it },
        selectedSide = selectedSide,
        onSideSelected = { selectedSide = it },
        selectedPiece = selectedPiece,
        onPieceSelected = { selectedPiece = it },
        fenError = fenError,
        onFenErrorDismiss = { fenError = null },
        onApplyFenClick = {
            val normalizedFen = normalizePositionEditorFen(fenText)
            val wasLoaded = gameController.loadFromFen(normalizedFen)
            if (!wasLoaded) {
                fenError = "Failed to parse FEN"
            } else {
                fenText = gameController.getFen()
            }
        },
        onBoardSquareClick = { square ->
            val updatedFen = placePieceOnFen(
                fen = gameController.getFen(),
                square = square,
                pieceLetter = selectedPiece.letter
            )
            fenText = updatedFen
            gameController.loadFromFen(updatedFen)
        },
        onBackClick = onBackClick,
        onNavigate = onNavigate,
        modifier = modifier
    )
}

@Composable
private fun PositionEditorScreen(
    gameController: GameController,
    fenText: String,
    onFenTextChange: (String) -> Unit,
    selectedSide: EditableGameSide,
    onSideSelected: (EditableGameSide) -> Unit,
    selectedPiece: PositionEditorPieceOption,
    onPieceSelected: (PositionEditorPieceOption) -> Unit,
    fenError: String?,
    onFenErrorDismiss: () -> Unit,
    onApplyFenClick: () -> Unit,
    onBoardSquareClick: (String) -> Unit,
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (fenError != null) {
        AppMessageDialog(
            title = "Invalid FEN",
            message = fenError,
            onDismiss = onFenErrorDismiss
        )
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = "Position Editor",
                subtitle = "Build a board position manually or from FEN",
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            AppBottomNavigation(
                items = defaultAppBottomNavigationItems(),
                selectedItem = ScreenType.Home,
                onItemSelected = onNavigate
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = AppDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            item {
                Spacer(modifier = Modifier.height(AppDimens.spaceXs))
            }

            item {
                ScreenSection {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SectionTitleText(text = "Position FEN", color = TrainingAccentTeal)
                        Spacer(modifier = Modifier.height(AppDimens.spaceSm))
                        BasicTextField(
                            value = fenText,
                            onValueChange = onFenTextChange,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = TextColor.Primary
                            ),
                            cursorBrush = SolidColor(TrainingAccentTeal),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = AppDimens.spaceMd,
                                            vertical = AppDimens.spaceMd
                                        )
                                ) {
                                    if (fenText.isBlank()) {
                                        BodySecondaryText(text = "Enter a FEN string")
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(AppDimens.spaceMd))
                        PrimaryButton(
                            text = "Set by FEN",
                            onClick = onApplyFenClick,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                ScreenSection {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    ) {
                        PositionEditorBoardWithCoordinates(
                            gameController = gameController,
                            onSquareClick = onBoardSquareClick,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            item {
                PositionEditorSideSelector(
                    selectedSide = selectedSide,
                    onSideSelected = onSideSelected
                )
            }

            item {
                PositionEditorPiecePalette(
                    selectedPiece = selectedPiece,
                    onPieceSelected = onPieceSelected
                )
            }

            item {
                Spacer(modifier = Modifier.height(AppDimens.spaceXs))
            }
        }
    }
}

@Composable
private fun PositionEditorSideSelector(
    selectedSide: EditableGameSide,
    onSideSelected: (EditableGameSide) -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenSection(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SectionTitleText(text = "Board Orientation")
            Spacer(modifier = Modifier.height(AppDimens.spaceSm))

            Row(verticalAlignment = Alignment.CenterVertically) {
                listOf(
                    EditableGameSide.AS_WHITE to "♔",
                    EditableGameSide.AS_BLACK to "♚"
                ).forEach { (side, symbol) ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = if (selectedSide == side) {
                                    SideButtonSelectedBg
                                } else {
                                    Color.Transparent
                                },
                                shape = RoundedCornerShape(50)
                            )
                            .clickable { onSideSelected(side) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = symbol,
                            fontSize = 20.sp,
                            color = if (selectedSide == side) {
                                TrainingTextPrimary
                            } else {
                                TrainingIconInactive
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PositionEditorPiecePalette(
    selectedPiece: PositionEditorPieceOption,
    onPieceSelected: (PositionEditorPieceOption) -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenSection(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SectionTitleText(text = "Place Piece")
            Spacer(modifier = Modifier.height(AppDimens.spaceSm))

            PositionEditorPieceOptions.chunked(2).forEach { rowOptions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
                ) {
                    rowOptions.forEach { pieceOption ->
                        val isSelected = pieceOption == selectedPiece
                        CardSurface(
                            modifier = Modifier.weight(1f),
                            color = if (isSelected) TrainingAccentTeal else Background.CardDark,
                            border = if (isSelected) {
                                null
                            } else {
                                BorderStroke(
                                    width = AppDimens.dividerThickness,
                                    color = TrainingDividerColor
                                )
                            },
                            contentPadding = PaddingValues(
                                horizontal = AppDimens.spaceMd,
                                vertical = AppDimens.spaceSm
                            ),
                            onClick = { onPieceSelected(pieceOption) }
                        ) {
                            Column {
                                Text(
                                    text = pieceOption.letter.toString(),
                                    color = TextColor.Primary,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.height(AppDimens.spaceXs))
                                BodySecondaryText(text = pieceOption.label)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(AppDimens.spaceMd))
            }
        }
    }
}

private fun normalizePositionEditorFen(fen: String): String {
    val trimmedFen = fen.trim()
    if (trimmedFen.isBlank()) {
        return EmptyBoardFen
    }

    if (trimmedFen.contains(' ')) {
        return trimmedFen
    }

    return "$trimmedFen w - - 0 1"
}

private fun placePieceOnFen(
    fen: String,
    square: String,
    pieceLetter: Char
): String {
    val currentPosition = ChesslibMapper.fromFen(fen)
    val updatedPosition = BoardPosition(
        pieces = currentPosition.pieces
            .filterNot { it.field == square }
            .plus(BoardPiece(pieceLetter, square))
    )
    val metadata = fen.substringAfter(' ', "w - - 0 1")
    return buildFenFromBoardPosition(updatedPosition, metadata)
}

private fun buildFenFromBoardPosition(
    position: BoardPosition,
    metadata: String
): String {
    val board = Array(8) { CharArray(8) { ' ' } }

    position.pieces.forEach { piece ->
        if (piece.field.length != 2) {
            return@forEach
        }

        val file = piece.field[0]
        val rank = piece.field[1]
        if (file !in 'a'..'h' || rank !in '1'..'8') {
            return@forEach
        }

        val row = 8 - rank.digitToInt()
        val col = file - 'a'
        board[row][col] = piece.letter
    }

    val boardPart = buildString {
        for (row in 0 until 8) {
            var emptySquares = 0
            for (col in 0 until 8) {
                val pieceLetter = board[row][col]
                if (pieceLetter == ' ') {
                    emptySquares++
                    continue
                }

                if (emptySquares > 0) {
                    append(emptySquares)
                    emptySquares = 0
                }
                append(pieceLetter)
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
