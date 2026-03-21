package com.example.chessboard

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    private var isDatabaseBusy by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        dataBaseController = DatabaseProvider.createInstance(context = applicationContext)

        enableEdgeToEdge()
        setContent {
            MainScreen(
                gameController = gameController,
                onSaveGame = { saveGame() },
                onDatabaseClear = { clearDatabase() }
            )
        }
    }

    private fun saveGame() {
        println("Save game clicked")

        if (isDatabaseBusy) { return }

        isDatabaseBusy = true

        val localPgn = gameController.generatePgn()
        val gameEntity = GameEntity (
            white = "Biba",
            black = "Buba",
            result = null,
            event = null,
            site = null,
            date = 0,
            round = null,
            eco = null,
            pgn = localPgn,
            initialFen = "",
        )
        lifecycleScope.launch(Dispatchers.IO) {
            dataBaseController.addGame(gameEntity)
            withContext(Dispatchers.Main) {
                isDatabaseBusy = false
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val count = dataBaseController.getGamesCount()
            println("Count = $count")
        }
    }

    private fun clearDatabase() {
        lifecycleScope.launch(Dispatchers.IO) {
            dataBaseController.clearAllData()
            withContext(Dispatchers.Main) {
                isDatabaseBusy = false
            }
        }
    }
}

@Composable
fun MainScreen(
    gameController: GameController,
    onSaveGame: () -> Unit,
    onDatabaseClear: () -> Unit,
) {
    Column {
        ChessBoardWithCoordinates(gameController)

        Column {
            Row {
                Button(
                    onClick = onSaveGame,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save game")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onDatabaseClear,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear database")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Button(
                    onClick = { gameController.undoMove() },
                    enabled = gameController.canUndo,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { gameController.redoMove() },
                    enabled = gameController.canRedo,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Forward")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { gameController.resetToStartPosition() },
                    enabled = true
                ) {
                    Text("Reset")
                }
            }
        }
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