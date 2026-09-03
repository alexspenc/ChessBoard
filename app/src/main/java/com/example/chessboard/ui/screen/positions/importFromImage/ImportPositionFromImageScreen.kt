package com.example.chessboard.ui.screen.positions.importFromImage

/**
 * Import a position from a board screenshot: pick an image, frame the board inside a square
 * viewport, recognise it via BoardImageRecognizer, preview the result, and hand the FEN over
 * to the position-search editor. Recognition logic lives in BoardImageImportService; this
 * file holds only screen state and the framing viewport UI.
 */

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.ui.BoardOrientation
import com.example.chessboard.service.BoardImageImportService
import com.example.chessboard.service.BoardImageTemplates
import com.example.chessboard.service.toPixelGrid
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppToggleRow
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.ChessBoardSection
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.Background
import com.example.chessboard.ui.theme.TrainingAccentTeal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val CropGridColor = Color.White.copy(alpha = 0.35f)

/** How far the framing viewport may zoom in past the fully zoomed-out square. */
private const val MaxZoomFactor = 8f

/** Index of the result item in the screen list, scrolled to once recognition finishes. */
private const val ResultItemIndex = 3

private data class ImportPositionFromImageUiState(
    val image: Bitmap? = null,
    val cropRect: Rect? = null,
    val whiteAtBottom: Boolean = true,
    val isRecognizing: Boolean = false,
    val outcome: BoardImageImportService.RecognitionOutcome? = null,
    val errorMessage: String? = null,
)

private data class ImportPositionFromImageActions(
    val onPickImageClick: () -> Unit,
    val onCropRectChange: (Rect) -> Unit,
    val onWhiteAtBottomChange: (Boolean) -> Unit,
    val onRecognizeClick: () -> Unit,
    val onOpenPositionSearchClick: () -> Unit,
    val onErrorDismiss: () -> Unit,
)

@Composable
fun ImportPositionFromImageScreenContainer(
    screenContext: ScreenContainerContext,
    onOpenPositionSearch: (fen: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = importPositionFromImageStrings()
    val previewController = remember {
        LineController().also { it.setUserMovesEnabled(false) }
    }
    var uiState by remember { mutableStateOf(ImportPositionFromImageUiState()) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        val selectedUri = uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val pickedImage = withContext(Dispatchers.IO) {
                BoardImageImportService.decodeBoardImage(context.contentResolver, selectedUri)
            }
            if (pickedImage == null) {
                uiState = uiState.copy(errorMessage = strings.decodeFailedMessage)
                return@launch
            }
            uiState = ImportPositionFromImageUiState(
                image = pickedImage,
                cropRect = defaultCropRect(pickedImage.width, pickedImage.height),
                whiteAtBottom = uiState.whiteAtBottom,
            )
        }
    }

    fun recognizeBoard() {
        val image = uiState.image ?: return
        val crop = uiState.cropRect ?: return
        val whiteAtBottom = uiState.whiteAtBottom
        uiState = uiState.copy(isRecognizing = true, outcome = null)
        scope.launch {
            val outcome = try {
                val templates = withContext(Dispatchers.IO) { BoardImageTemplates.get(context) }
                withContext(Dispatchers.Default) {
                    BoardImageImportService.recognizeBoard(
                        image = image.toPixelGrid(),
                        cropLeft = crop.left.roundToInt(),
                        cropTop = crop.top.roundToInt(),
                        cropWidth = crop.width.roundToInt(),
                        cropHeight = crop.height.roundToInt(),
                        templates = templates,
                        whiteAtBottom = whiteAtBottom,
                    )
                }
            } catch (_: Exception) {
                null
            }
            if (outcome == null) {
                uiState = uiState.copy(
                    isRecognizing = false,
                    errorMessage = strings.decodeFailedMessage,
                )
                return@launch
            }
            previewController.setOrientation(orientationFor(whiteAtBottom))
            previewController.loadPreviewFen("${outcome.positionFen} 0 1")
            uiState = uiState.copy(isRecognizing = false, outcome = outcome)
        }
    }

    fun setWhiteAtBottom(whiteAtBottom: Boolean) {
        if (whiteAtBottom == uiState.whiteAtBottom) {
            return
        }

        val flippedOutcome = uiState.outcome?.let { BoardImageImportService.flipOutcome(it) }
        if (flippedOutcome != null) {
            previewController.setOrientation(orientationFor(whiteAtBottom))
            previewController.loadPreviewFen("${flippedOutcome.positionFen} 0 1")
        }
        uiState = uiState.copy(whiteAtBottom = whiteAtBottom, outcome = flippedOutcome)
    }

    ImportPositionFromImageScreen(
        state = uiState,
        previewController = previewController,
        actions = ImportPositionFromImageActions(
            onPickImageClick = { imagePickerLauncher.launch(arrayOf("image/*")) },
            onCropRectChange = { updatedRect ->
                uiState = uiState.copy(cropRect = updatedRect)
            },
            onWhiteAtBottomChange = ::setWhiteAtBottom,
            onRecognizeClick = ::recognizeBoard,
            onOpenPositionSearchClick = openPositionSearch@{
                val outcome = uiState.outcome ?: return@openPositionSearch
                onOpenPositionSearch(outcome.positionFen)
            },
            onErrorDismiss = { uiState = uiState.copy(errorMessage = null) },
        ),
        onBackClick = screenContext.onBackClick,
        modifier = modifier,
    )
}

@Composable
private fun ImportPositionFromImageScreen(
    state: ImportPositionFromImageUiState,
    previewController: LineController,
    actions: ImportPositionFromImageActions,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = importPositionFromImageStrings()
    val listState = rememberLazyListState()

    state.errorMessage?.let { errorMessage ->
        AppMessageDialog(
            title = strings.screenTitle,
            message = errorMessage,
            onDismiss = actions.onErrorDismiss,
        )
    }

    LaunchedEffect(state.outcome) {
        if (state.outcome != null) {
            listState.animateScrollToItem(ResultItemIndex)
        }
    }

    AppScreenScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = strings.screenTitle,
                onBackClick = onBackClick,
                handleSystemBack = true,
            )
        },
        bottomBar = {
            RecognizeActionBar(
                text = if (state.isRecognizing) {
                    strings.recognizingLabel
                } else {
                    strings.recognizeAction
                },
                enabled = state.image != null && !state.isRecognizing,
                onClick = actions.onRecognizeClick,
            )
        },
    ) { paddingValues ->
        // Every section is its own item and stays in the list even while empty, so
        // ResultItemIndex keeps pointing at the result once recognition finishes.
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = AppDimens.spaceLg,
                end = AppDimens.spaceLg,
                top = AppDimens.spaceSm,
                bottom = AppDimens.spaceLg,
            ),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd),
        ) {
            item {
                PrimaryButton(
                    text = strings.pickImageAction,
                    onClick = actions.onPickImageClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                AppToggleRow(
                    title = strings.whiteAtBottomTitle,
                    subtitle = strings.whiteAtBottomSubtitle,
                    checked = state.whiteAtBottom,
                    onCheckedChange = actions.onWhiteAtBottomChange,
                )
            }

            item {
                val image = state.image
                val cropRect = state.cropRect
                if (image != null && cropRect != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                        ImportImageCropSection(
                            image = image,
                            cropRect = cropRect,
                            onCropRectChange = actions.onCropRectChange,
                        )
                        BodySecondaryText(text = strings.cropHint)
                    }
                }
            }

            item {
                val outcome = state.outcome
                if (outcome != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceMd)) {
                        SectionTitleText(text = strings.resultSectionTitle)
                        ChessBoardSection(lineController = previewController)

                        if (outcome.issues.isNotEmpty() || outcome.uncertainSquares.isNotEmpty()) {
                            CardSurface(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
                                ) {
                                    SectionTitleText(text = strings.warningsTitle)
                                    outcome.issues.forEach { issue ->
                                        BodySecondaryText(text = issue)
                                    }
                                    if (outcome.uncertainSquares.isNotEmpty()) {
                                        BodySecondaryText(
                                            text = strings.uncertainSquaresMessage(
                                                outcome.uncertainSquares
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        PrimaryButton(
                            text = strings.openInPositionSearchAction,
                            onClick = actions.onOpenPositionSearchClick,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecognizeActionBar(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Background.SurfaceDark,
        tonalElevation = 8.dp,
    ) {
        PrimaryButton(
            text = text,
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.spaceLg),
        )
    }
}

/**
 * Square viewport the user frames the board in. [cropRect] is the part of the image currently
 * visible in it, kept in image pixel coordinates, and is what gets recognised. Two fingers
 * zoom, one finger moves the image behind the viewport, and an 8×8 grid is drawn on top so
 * the board squares can be lined up with it.
 */
@Composable
private fun ImportImageCropSection(
    image: Bitmap,
    cropRect: Rect,
    onCropRectChange: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageBitmap = remember(image) { image.asImageBitmap() }
    val currentCropRect by rememberUpdatedState(cropRect)
    val currentOnCropRectChange by rememberUpdatedState(onCropRectChange)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(AppDimens.radiusLg))
            .pointerInput(image) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val viewportSide = size.width.toFloat()
                    if (viewportSide <= 0f) {
                        return@detectTransformGestures
                    }
                    currentOnCropRectChange(
                        applyViewportGesture(
                            crop = currentCropRect,
                            viewportSide = viewportSide,
                            centroid = centroid,
                            pan = pan,
                            zoom = zoom,
                            imageWidth = image.width.toFloat(),
                            imageHeight = image.height.toFloat(),
                        )
                    )
                }
            }
    ) {
        val scale = size.width / cropRect.width

        drawImage(
            image = imageBitmap,
            dstOffset = IntOffset(
                (-cropRect.left * scale).roundToInt(),
                (-cropRect.top * scale).roundToInt(),
            ),
            dstSize = IntSize(
                (image.width * scale).roundToInt(),
                (image.height * scale).roundToInt(),
            ),
        )

        for (line in 1..7) {
            val step = size.width * line / 8f
            drawLine(
                color = CropGridColor,
                start = Offset(step, 0f),
                end = Offset(step, size.height),
                strokeWidth = 1.dp.toPx(),
            )
            drawLine(
                color = CropGridColor,
                start = Offset(0f, step),
                end = Offset(size.width, step),
                strokeWidth = 1.dp.toPx(),
            )
        }

        drawRect(
            color = TrainingAccentTeal,
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

private fun orientationFor(whiteAtBottom: Boolean): BoardOrientation =
    if (whiteAtBottom) BoardOrientation.WHITE else BoardOrientation.BLACK

private fun defaultCropRect(imageWidth: Int, imageHeight: Int): Rect {
    val side = minOf(imageWidth, imageHeight).toFloat()
    val left = (imageWidth - side) / 2f
    val top = (imageHeight - side) / 2f
    return Rect(left, top, left + side, top + side)
}

/**
 * Maps one pinch/drag step of the framing viewport onto the crop square, in image pixels.
 * Zooming keeps the image point under [centroid] in place; the crop square never grows past
 * the shorter image side and never leaves the image, so it always holds real pixels.
 */
private fun applyViewportGesture(
    crop: Rect,
    viewportSide: Float,
    centroid: Offset,
    pan: Offset,
    zoom: Float,
    imageWidth: Float,
    imageHeight: Float,
): Rect {
    val maxSide = minOf(imageWidth, imageHeight)
    val side = (crop.width / zoom).coerceIn(maxSide / MaxZoomFactor, maxSide)
    val focus = crop.topLeft + centroid * (crop.width / viewportSide)
    val topLeft = focus - (centroid + pan) * (side / viewportSide)
    val left = topLeft.x.coerceIn(0f, imageWidth - side)
    val top = topLeft.y.coerceIn(0f, imageHeight - side)
    return Rect(left, top, left + side, top + side)
}
