package com.example.chessboard.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.ColorFilter
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
import com.example.chessboard.R
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
import com.example.chessboard.ui.components.ScreenSection
import com.example.chessboard.ui.components.SecondaryButton
import com.example.chessboard.ui.components.defaultAppBottomNavigationItems
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TextColor
import com.example.chessboard.ui.theme.TrainingAccentTeal
import com.example.chessboard.ui.theme.ChessPieceDark

private const val EmptyBoardFen = "8/8/8/8/8/8/8/8 w - - 0 1"

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

private data class PositionEditorUiState(
        val fenText: String = EmptyBoardFen,
        val selectedSide: EditableGameSide = EditableGameSide.AS_WHITE,
        val selectedPiece: PositionEditorPieceOption = PositionEditorPieceOptions.first(),
        val fenError: String? = null
    )

@Composable
fun PositionEditorScreenContainer(
    screenContext: ScreenContainerContext,
    modifier: Modifier = Modifier
) {


    val gameController = remember { GameController() }
    var uiState by remember { mutableStateOf(PositionEditorUiState()) }

    LaunchedEffect(Unit) {
        gameController.loadFromFen(EmptyBoardFen)
    }

    LaunchedEffect(uiState.selectedSide) {
        gameController.setOrientation(uiState.selectedSide.orientation)
    }

    PositionEditorScreen(
        gameController = gameController,
        uiState = uiState,
        onFenTextChange = { uiState = uiState.copy(fenText = it) },
        onSideSelected = { uiState = uiState.copy(selectedSide = it) },
        onPieceSelected = { uiState = uiState.copy(selectedPiece = it) },
        onFenErrorDismiss = { uiState = uiState.copy(fenError = null) },
        onApplyFenClick = {
            val normalizedFen = normalizePositionEditorFen(uiState.fenText)

            try {
                gameController.loadFromFen(normalizedFen)
                uiState = uiState.copy(
                    fenText = gameController.getFen(),
                    fenError = null
                )
            } catch (error: Exception) {
                uiState = uiState.copy(
                    fenError = error.message ?: "Failed to apply FEN"
                )
            }
        },
        onClearBoardClick = {
            gameController.loadFromFen(EmptyBoardFen)
            uiState = uiState.copy(fenText = gameController.getFen())
        },
        onSetInitialPositionClick = {
            gameController.resetToStartPosition()
            uiState = uiState.copy(fenText = gameController.getFen())
        },
        onBoardSquareClick = { square ->
            val updatedFen = placePieceOnFen(
                fen = gameController.getFen(),
                square = square,
                pieceLetter = uiState.selectedPiece.letter
            )
            uiState = uiState.copy(fenText = updatedFen)
            gameController.loadFromFen(updatedFen)
        },
        onBackClick = screenContext.onBackClick,
        onNavigate = screenContext.onNavigate,
        modifier = modifier
    )
}

@Composable
private fun PositionEditorScreen(
    gameController: GameController,
    uiState: PositionEditorUiState,
    onFenTextChange: (String) -> Unit,
    onSideSelected: (EditableGameSide) -> Unit,
    onPieceSelected: (PositionEditorPieceOption) -> Unit,
    onFenErrorDismiss: () -> Unit,
    onApplyFenClick: () -> Unit,
    onClearBoardClick: () -> Unit,
    onSetInitialPositionClick: () -> Unit,
    onBoardSquareClick: (String) -> Unit,
    onBackClick: () -> Unit = {},
    onNavigate: (ScreenType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    RenderPositionEditorFenError(
        fenError = uiState.fenError,
        onDismiss = onFenErrorDismiss
    )

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = "Position Editor",
                onBackClick = onBackClick,
                actions = {
                    SecondaryButton(
                        text = "Apply FEN",
                        onClick = onApplyFenClick
                    )
                }
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
                PositionEditorFenSection(
                    fenText = uiState.fenText,
                    onFenTextChange = onFenTextChange
                )
            }

            item {
                PositionEditorBoardSection(
                    gameController = gameController,
                    onBoardSquareClick = onBoardSquareClick
                )
            }

            item {
                PositionEditorControlsSection(
                    selectedSide = uiState.selectedSide,
                    onSideSelected = onSideSelected,
                    onClearBoardClick = onClearBoardClick,
                    onSetInitialPositionClick = onSetInitialPositionClick
                )
            }

            item {
                PositionEditorPaletteSection(
                    selectedPiece = uiState.selectedPiece,
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
private fun RenderPositionEditorFenError(
    fenError: String?,
    onDismiss: () -> Unit
) {
    if (fenError == null) {
        return
    }

    AppMessageDialog(
        title = "Invalid FEN",
        message = fenError,
        onDismiss = onDismiss
    )
}

@Composable
private fun PositionEditorFenSection(
    fenText: String,
    onFenTextChange: (String) -> Unit
) {
    ScreenSection {
        Column(modifier = Modifier.fillMaxWidth()) {
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
        }
    }
}

@Composable
private fun PositionEditorBoardSection(
    gameController: GameController,
    onBoardSquareClick: (String) -> Unit
) {
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

@Composable
private fun PositionEditorControlsSection(
    selectedSide: EditableGameSide,
    onSideSelected: (EditableGameSide) -> Unit,
    onClearBoardClick: () -> Unit,
    onSetInitialPositionClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
        verticalAlignment = Alignment.Top
    ) {
        GameSideSelector(
            selectedSide = selectedSide,
            onSideSelected = onSideSelected,
            modifier = Modifier.weight(1f)
        )

        PositionEditorActionButtons(
            onClearBoardClick = onClearBoardClick,
            onSetInitialPositionClick = onSetInitialPositionClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PositionEditorActionButtons(
    onClearBoardClick: () -> Unit,
    onSetInitialPositionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)
    ) {
        SecondaryButton(
            text = "Clear board",
            onClick = onClearBoardClick,
            modifier = Modifier.fillMaxWidth()
        )
        SecondaryButton(
            text = "Initial position",
            onClick = onSetInitialPositionClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PositionEditorPaletteSection(
    selectedPiece: PositionEditorPieceOption,
    onPieceSelected: (PositionEditorPieceOption) -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenSection(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(AppDimens.spaceSm))
            BodySecondaryText(text = selectedPiece.label)
            Spacer(modifier = Modifier.height(AppDimens.spaceMd))

            PositionEditorPieceGrid(
                selectedPiece = selectedPiece,
                onPieceSelected = onPieceSelected
            )
        }
    }
}

@Composable
private fun PositionEditorPieceGrid(
    selectedPiece: PositionEditorPieceOption,
    onPieceSelected: (PositionEditorPieceOption) -> Unit
) {
    PositionEditorPieceOptions.chunked(6).forEach { rowOptions ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            rowOptions.forEach { pieceOption ->
                PositionEditorPieceIcon(
                    pieceOption = pieceOption,
                    isSelected = pieceOption == selectedPiece,
                    onClick = { onPieceSelected(pieceOption) }
                )
            }
        }
        Spacer(modifier = Modifier.height(AppDimens.spaceMd))
    }
}

@Composable
private fun PositionEditorPieceIcon(
    pieceOption: PositionEditorPieceOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                color = if (isSelected) SideButtonSelectedBg else Color.Transparent,
                shape = RoundedCornerShape(50)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = androidx.compose.ui.res.painterResource(id = resolvePositionEditorPieceIcon(pieceOption.letter)),
            contentDescription = pieceOption.label,
            colorFilter = ColorFilter.tint(resolvePositionEditorPieceTint(pieceOption.letter))
        )
    }
}

private fun resolvePositionEditorPieceIcon(pieceLetter: Char): Int {
    return when (pieceLetter.lowercaseChar()) {
        'k' -> R.drawable.ic_king
        'q' -> R.drawable.ic_queen
        'r' -> R.drawable.ic_rook
        'b' -> R.drawable.ic_bishop
        'n' -> R.drawable.ic_knight
        else -> R.drawable.ic_pawn
    }
}

private fun resolvePositionEditorPieceTint(pieceLetter: Char): Color {
    if (pieceLetter.isUpperCase()) {
        return Color.White
    }

    return ChessPieceDark
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
    val currentPiece = currentPosition.pieces.find { it.field == square }
    val updatedPieces = currentPosition.pieces.filterNot { it.field == square }.toMutableList()

    if (currentPiece?.letter != pieceLetter) {
        updatedPieces += BoardPiece(pieceLetter, square)
    }

    val updatedPosition = BoardPosition(pieces = updatedPieces)
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
