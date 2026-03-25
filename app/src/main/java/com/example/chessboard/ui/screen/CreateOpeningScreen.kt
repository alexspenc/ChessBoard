package com.example.chessboard.ui.screen

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.repository.DatabaseProvider
import com.example.chessboard.ui.components.PrimaryButton
import com.example.chessboard.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CreateOpeningScreenContainer(
    activity: Activity,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    inDbProvider : DatabaseProvider,
) {
    val dbProvider = inDbProvider
    val gameController = remember { GameController() }

    CreateOpeningScreen(
        gameController = gameController,
        onBackClick = onBackClick,
        onSave = { name, eco ->
            val entity = GameEntity(
                event = name.ifBlank { null },
                eco = eco.ifBlank { null },
                pgn = gameController.generatePgn(),
                initialFen = "",
            )
            (activity as? LifecycleOwner)?.lifecycleScope?.launch(Dispatchers.IO) {
                dbProvider.addGame(entity, gameController.getMovesCopy())
                withContext(Dispatchers.Main) { onBackClick() }
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOpeningScreen(
    gameController: GameController,
    onBackClick: () -> Unit = {},
    onSave: (name: String, eco: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var openingName by remember { mutableStateOf("") }
    var ecoCode by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = TrainingBackgroundDark,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TrainingBackgroundDark,
                    navigationIconContentColor = TrainingTextPrimary,
                    titleContentColor = TrainingTextPrimary
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .padding(start = AppDimens.spaceSm)
                            .size(AppDimens.iconButtonSize)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TrainingTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Create Opening",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TrainingTextPrimary
                        )
                        Text(
                            text = "Build your custom opening",
                            fontSize = 12.sp,
                            color = TrainingTextSecondary
                        )
                    }
                },
                actions = {
                    PrimaryButton("Save",
                        onClick = {
                            if (openingName.isBlank()) {
                                nameError = true
                                return@PrimaryButton
                            }
                            onSave(openingName, ecoCode)
                        })
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppDimens.spaceLg)
        ) {
            Spacer(modifier = Modifier.height(AppDimens.spaceXs))

            DarkInputField(
                value = openingName,
                onValueChange = { openingName = it; nameError = false },
                placeholder = "e.g., Sicilian Defense",
                label = "Opening Name *",
                isError = nameError
            )

            DarkInputField(
                value = ecoCode,
                onValueChange = { ecoCode = it },
                placeholder = "e.g., B20",
                label = "ECO Code",
                modifier = Modifier.fillMaxWidth(0.5f)
            )

            Text(
                text = "Drag pieces to add moves",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TrainingTextSecondary
            )

            ChessBoardSection(gameController = gameController)

            Spacer(modifier = Modifier.height(AppDimens.spaceLg))
        }
    }
}
