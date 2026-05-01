package com.example.chessboard.ui.screen.training

/**
 * Shared UI helpers for training template list screens.
 *
 * Keep reusable template-card models, card rendering, and simple screen-agnostic helpers here.
 * Do not add screen container state, navigation, or database orchestration logic. Validation date: 2026-05-01.
 */
import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import com.example.chessboard.entity.TrainingTemplateEntity
import com.example.chessboard.service.GameListService
import com.example.chessboard.service.OneGameTrainingData
import com.example.chessboard.service.buildAnalysisPgnFromGames
import com.example.chessboard.ui.components.CardMetaText
import com.example.chessboard.ui.components.CardSurface
import com.example.chessboard.ui.components.DeleteIconButton
import com.example.chessboard.ui.components.IconMd
import com.example.chessboard.ui.components.ScreenTitleText
import com.example.chessboard.ui.theme.AppDimens

internal data class TrainingTemplateCardItem(
    val templateId: Long,
    val name: String,
    val gamesCount: Int,
    val gameIds: List<Long>,
)

internal data class TrainingTemplateInfoDialog(
    val title: String,
    val message: String,
)

@Composable
internal fun TrainingTemplateCard(
    template: TrainingTemplateCardItem,
    onClick: () -> Unit,
    onCopyPgnClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CardSurface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                ScreenTitleText(text = template.name)
                Spacer(modifier = Modifier.height(AppDimens.spaceXs))
                CardMetaText(text = "Template ID: ${template.templateId}")
                CardMetaText(text = "Games: ${template.gamesCount}")
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppDimens.spaceXs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onCopyPgnClick,
                ) {
                    IconMd(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy template PGN",
                    )
                }
                DeleteIconButton(onClick = onDeleteClick, contentDescription = "Delete template")
            }
        }
    }
}

internal fun TrainingTemplateEntity.toTrainingTemplateCardItem(): TrainingTemplateCardItem {
    val templateGames = OneGameTrainingData.fromJson(gamesJson)

    return TrainingTemplateCardItem(
        templateId = id,
        name = name.ifBlank { "Unnamed Template" },
        gamesCount = templateGames.size,
        gameIds = templateGames.map { game -> game.gameId },
    )
}

internal suspend fun buildTemplateAnalysisPgn(
    gameListService: GameListService,
    gameIds: List<Long>,
): String {
    val games = gameListService.getGamesByIds(gameIds)
    return buildAnalysisPgnFromGames(games)
}

internal suspend fun copyTemplatePgnToClipboard(
    gameListService: GameListService,
    clipboard: Clipboard,
    gameIds: List<Long>,
): TrainingTemplateInfoDialog {
    val templatePgn = buildTemplateAnalysisPgn(
        gameListService = gameListService,
        gameIds = gameIds,
    )
    if (templatePgn.isBlank()) {
        return TrainingTemplateInfoDialog(
            title = "PGN unavailable",
            message = "Template games could not be exported to PGN.",
        )
    }

    clipboard.setClipEntry(
        ClipEntry(
            ClipData.newPlainText(
                "Template PGN",
                templatePgn,
            )
        )
    )
    return TrainingTemplateInfoDialog(
        title = "PGN copied",
        message = "Template PGN was copied to the clipboard.",
    )
}

internal fun resolveDeleteTemplateMessage(
    templateName: String,
    templateId: Long,
): String {
    return "Delete \"$templateName\"?\nTemplate ID: $templateId"
}
