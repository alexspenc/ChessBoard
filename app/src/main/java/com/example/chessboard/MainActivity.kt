package com.example.chessboard

import android.content.Context
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.room.PrimaryKey

import com.example.chessboard.ui.theme.ChessBoardTheme
import com.example.chessboard.ui.ChessBoardWithCoordinates
import com.example.chessboard.boardmodel.GameController
import com.example.chessboard.database.DatabaseProvider
import com.example.chessboard.database.GameEntity
import kotlin.String

class MainActivity : ComponentActivity() {

    private var gameController = GameController()
    private lateinit var dataBaseController : DatabaseProvider

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        dataBaseController = DatabaseProvider.createInstance(context = this)

        enableEdgeToEdge()
        setContent {
            ChessBoardWithCoordinates(gameController)
            MainScreen(
                gameController = gameController,
                onSaveGame = {
                    saveGame()
                }
            )
        }
    }

    private fun saveGame() {
        println("Save game clicked")

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
        dataBaseController.addGame(gameEntity)
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
            Text("Сохранить партию")
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