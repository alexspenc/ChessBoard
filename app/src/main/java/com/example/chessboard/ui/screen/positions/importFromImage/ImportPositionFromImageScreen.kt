package com.example.chessboard.ui.screen.positions.importFromImage

/**
 * Import a position from a board screenshot: pick an image, frame the board with a crop
 * rectangle, recognise it via BoardImageRecognizer, preview the result, and hand the FEN
 * over to the position-search editor. Recognition logic lives in BoardImageImportService;
 * this file holds only screen state and the crop-frame UI.
 */

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.chessboard.boardmodel.LineController
import com.example.chessboard.service.BoardImageImportService
import com.example.chessboard.service.BoardImageTemplates
import com.example.chessboard.service.toPixelGrid
import com.example.chessboard.ui.components.AppMessageDialog
import com.example.chessboard.ui.components.AppScreenScaffold
import com.example.chessboard.ui.components.AppSettingsToggleRow
import com.example.chessboard.ui.components.AppTopBar
import com.example.chessboard.ui.components.BodySecondaryText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.ChessBoardSection
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.components.SectionTitleText
import com.example.chessboard.ui.screen.ScreenContainerContext
import com.example.chessboard.ui.theme.AppDimens
import com.example.chessboard.ui.theme.TrainingAccentTeal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val CropHandleTouchRadius = 24.dp
private val CropDimColor = Color.Black.copy(alpha = 0.55f)
private val CropGridColor = Color.White.copy(alpha = 0.35f)

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

    state.errorMessage?.let { errorMessage ->
        AppMessageDialog(
            title = strings.screenTitle,
            message = errorMessage,
            onDismiss = actions.onErrorDismiss,
        )
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
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = AppDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg),
        ) {
            item {
                Spacer(modifier = Modifier.height(AppDimens.spaceXs))
            }

            item {
                PrimaryButton(
                    text = strings.pickImageAction,
                    onClick = actions.onPickImageClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            val image = state.image
            val cropRect = state.cropRect
            if (image != null && cropRect != null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                        SectionTitleText(text = strings.cropSectionTitle)
                        BodySecondaryText(text = strings.cropHint)
                        ImportImageCropSection(
                            image = image,
                            cropRect = cropRect,
                            onCropRectChange = actions.onCropRectChange,
                        )
                    }
                }

                item {
                    CardSurface(modifier = Modifier.fillMaxWidth()) {
                        AppSettingsToggleRow(
                            icon = Icons.Filled.SwapVert,
                            title = strings.whiteAtBottomTitle,
                            subtitle = strings.whiteAtBottomSubtitle,
                            checked = state.whiteAtBottom,
                            onCheckedChange = actions.onWhiteAtBottomChange,
                        )
                    }
                }

                item {
                    PrimaryButton(
                        text = if (state.isRecognizing) {
                            strings.recognizingLabel
                        } else {
                            strings.recognizeAction
                        },
                        onClick = actions.onRecognizeClick,
                        enabled = !state.isRecognizing,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            val outcome = state.outcome
            if (outcome != null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceSm)) {
                        SectionTitleText(text = strings.resultSectionTitle)
                        ChessBoardSection(lineController = previewController)
                    }
                }

                if (outcome.issues.isNotEmpty() || outcome.uncertainSquares.isNotEmpty()) {
                    item {
                        CardSurface(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.spaceXs)) {
                                SectionTitleText(text = strings.warningsTitle)
                                outcome.issues.forEach { issue ->
                                    BodySecondaryText(text = issue)
                                }
                                if (outcome.uncertainSquares.isNotEmpty()) {
                                    BodySecondaryText(
                                        text = strings.uncertainSquaresMessage(outcome.uncertainSquares)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    PrimaryButton(
                        text = strings.openInPositionSearchAction,
                        onClick = actions.onOpenPositionSearchClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(AppDimens.spaceXs))
            }
        }
    }
}

private enum class CropHandle { TopLeft, TopRight, BottomLeft, BottomRight, Inside }

/**
 * The picked screenshot with a draggable crop frame over it. [cropRect] is kept in image
 * pixel coordinates; the composable maps gestures from screen space through the fit-width
 * scale factor. Corners resize the frame, dragging inside moves it, and an 8×8 grid is
 * drawn inside the frame so the user can align it with the board squares.
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
    val handleTouchRadius = with(LocalDensity.current) { CropHandleTouchRadius.toPx() }

    // Same trick as ChessBoardSection: pre-consume user scroll so the parent LazyColumn
    // never starts scrolling while the finger is dragging the crop frame.
    val noScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                if (source == NestedScrollSource.UserInput) available else Offset.Zero
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(image.width.toFloat() / image.height.toFloat())
            .clip(RoundedCornerShape(AppDimens.radiusLg))
            .nestedScroll(noScroll)
            .pointerInput(image) {
                var activeHandle: CropHandle? = null
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val scale = size.width.toFloat() / image.width
                        activeHandle = resolveCropHandle(
                            position = startOffset,
                            screenRect = currentCropRect.scaled(scale),
                            touchRadius = handleTouchRadius,
                        )
                    },
                    onDragEnd = { activeHandle = null },
                    onDragCancel = { activeHandle = null },
                ) { change, dragAmount ->
                    val handle = activeHandle ?: return@detectDragGestures
                    change.consume()
                    val scale = size.width.toFloat() / image.width
                    currentOnCropRectChange(
                        applyCropDrag(
                            rect = currentCropRect,
                            handle = handle,
                            delta = dragAmount / scale,
                            imageWidth = image.width.toFloat(),
                            imageHeight = image.height.toFloat(),
                        )
                    )
                }
            }
    ) {
        Image(
            bitmap = imageBitmap,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scale = size.width / image.width
            val screenRect = cropRect.scaled(scale)

            drawRect(
                color = CropDimColor,
                size = androidx.compose.ui.geometry.Size(size.width, screenRect.top),
            )
            drawRect(
                color = CropDimColor,
                topLeft = Offset(0f, screenRect.bottom),
                size = androidx.compose.ui.geometry.Size(size.width, size.height - screenRect.bottom),
            )
            drawRect(
                color = CropDimColor,
                topLeft = Offset(0f, screenRect.top),
                size = androidx.compose.ui.geometry.Size(screenRect.left, screenRect.height),
            )
            drawRect(
                color = CropDimColor,
                topLeft = Offset(screenRect.right, screenRect.top),
                size = androidx.compose.ui.geometry.Size(size.width - screenRect.right, screenRect.height),
            )

            for (line in 1..7) {
                val x = screenRect.left + screenRect.width * line / 8f
                drawLine(
                    color = CropGridColor,
                    start = Offset(x, screenRect.top),
                    end = Offset(x, screenRect.bottom),
                    strokeWidth = 1.dp.toPx(),
                )
                val y = screenRect.top + screenRect.height * line / 8f
                drawLine(
                    color = CropGridColor,
                    start = Offset(screenRect.left, y),
                    end = Offset(screenRect.right, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            drawRect(
                color = TrainingAccentTeal,
                topLeft = screenRect.topLeft,
                size = screenRect.size,
                style = Stroke(width = 2.dp.toPx()),
            )
            listOf(
                screenRect.topLeft,
                Offset(screenRect.right, screenRect.top),
                Offset(screenRect.left, screenRect.bottom),
                screenRect.bottomRight,
            ).forEach { corner ->
                drawCircle(
                    color = TrainingAccentTeal,
                    radius = 6.dp.toPx(),
                    center = corner,
                )
            }
        }
    }
}

private fun defaultCropRect(imageWidth: Int, imageHeight: Int): Rect {
    val side = minOf(imageWidth, imageHeight).toFloat()
    val left = (imageWidth - side) / 2f
    val top = (imageHeight - side) / 2f
    return Rect(left, top, left + side, top + side)
}

private fun Rect.scaled(scale: Float): Rect =
    Rect(left * scale, top * scale, right * scale, bottom * scale)

private fun resolveCropHandle(
    position: Offset,
    screenRect: Rect,
    touchRadius: Float,
): CropHandle? {
    val corners = listOf(
        CropHandle.TopLeft to screenRect.topLeft,
        CropHandle.TopRight to Offset(screenRect.right, screenRect.top),
        CropHandle.BottomLeft to Offset(screenRect.left, screenRect.bottom),
        CropHandle.BottomRight to screenRect.bottomRight,
    )
    val nearestCorner = corners.minByOrNull { (_, corner) ->
        (position - corner).getDistance()
    }
    if (nearestCorner != null && (position - nearestCorner.second).getDistance() <= touchRadius) {
        return nearestCorner.first
    }
    if (screenRect.contains(position)) {
        return CropHandle.Inside
    }
    return null
}

private fun applyCropDrag(
    rect: Rect,
    handle: CropHandle,
    delta: Offset,
    imageWidth: Float,
    imageHeight: Float,
): Rect {
    val minSide = minOf(imageWidth, imageHeight) / 8f
    return when (handle) {
        CropHandle.Inside -> rect.translate(
            delta.x.coerceIn(-rect.left, imageWidth - rect.right),
            delta.y.coerceIn(-rect.top, imageHeight - rect.bottom),
        )

        CropHandle.TopLeft -> rect.copy(
            left = (rect.left + delta.x).coerceIn(0f, rect.right - minSide),
            top = (rect.top + delta.y).coerceIn(0f, rect.bottom - minSide),
        )

        CropHandle.TopRight -> rect.copy(
            right = (rect.right + delta.x).coerceIn(rect.left + minSide, imageWidth),
            top = (rect.top + delta.y).coerceIn(0f, rect.bottom - minSide),
        )

        CropHandle.BottomLeft -> rect.copy(
            left = (rect.left + delta.x).coerceIn(0f, rect.right - minSide),
            bottom = (rect.bottom + delta.y).coerceIn(rect.top + minSide, imageHeight),
        )

        CropHandle.BottomRight -> rect.copy(
            right = (rect.right + delta.x).coerceIn(rect.left + minSide, imageWidth),
            bottom = (rect.bottom + delta.y).coerceIn(rect.top + minSide, imageHeight),
        )
    }
}
