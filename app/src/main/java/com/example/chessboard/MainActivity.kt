package com.example.chessboard

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope

import com.example.chessboard.ui.theme.ChessBoardTheme
import com.example.chessboard.ui.ChessBoardWithCoordinates
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.database.DatabaseProvider
import com.example.chessboard.database.GameEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.String

class MainActivity : ComponentActivity() {

    private var gameController = GameController()
    private lateinit var dataBaseController : DatabaseProvider

    private var isSaving by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        dataBaseController = DatabaseProvider.createInstance(context = applicationContext)

        enableEdgeToEdge()
        setContent {
            MainScreen(
                gameController = gameController,
                onSaveGame = { saveGame() }
            )
        }
    }

    private fun saveGame() {
        println("Save game clicked")

        if (isSaving) { return }

        isSaving = true

        val localPgn = gameController.generatePgn()
        val gameEntity = GameEntity (
            white = "Biba",
            black = "Buba",
            result = null,
            event = null,
            site = null,
            date = null,
            round = null,
            eco = null,
            pgn = localPgn,
            initialFen = null,
        )
        lifecycleScope.launch(Dispatchers.IO) {
            dataBaseController.addGame(gameEntity)
            withContext(Dispatchers.Main) {
                isSaving = false
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val count = dataBaseController.getGamesCount()
            println("Count = $count")
        }
    }
}

@Composable
fun MainScreen(
    gameController: GameController,
    onSaveGame: () -> Unit
) {
    Column {
        ChessBoardWithCoordinates(gameController)

        Button(onClick = onSaveGame) {
            Text("Save game")
        }

        Button(
            onClick = { gameController.undoMove() },
            enabled = gameController.canUndo()
        ) { Text("Back move") }

        Button(
            onClick = { gameController.redoMove() },
            enabled = gameController.canRedo()
        ) { Text("Forward Move") }
    }
}
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ChessBoardTheme {
        Greeting("Android")
    }
}