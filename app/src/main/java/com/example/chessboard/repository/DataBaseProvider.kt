package com.example.chessboard.repository

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.entity.GamePositionEntity
import com.example.chessboard.entity.PositionEntity
import com.example.chessboard.entity.TrainingEntity
import com.example.chessboard.entity.TrainingTemplateEntity
import com.example.chessboard.service.FirstTrainingGameLaunchResult
import com.example.chessboard.service.GameDeleter
import com.example.chessboard.service.GameSaver
import com.example.chessboard.service.GameUpdater
import com.example.chessboard.service.OneGameTrainingData
import com.example.chessboard.service.TrainSingleGameService
import com.example.chessboard.service.TrainingService
import com.github.bhlangonijr.chesslib.move.Move

@Database(
    entities = [
        GameEntity::class,
        PositionEntity::class,
        GamePositionEntity::class,
        TrainingTemplateEntity::class,
        TrainingEntity::class
    ],
    version = 6
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun positionDao(): PositionDao
    abstract fun gamePositionDao(): GamePositionDao
    abstract fun trainingTemplateDao(): TrainingTemplateDao
    abstract fun trainingDao(): TrainingDao
}

class DatabaseProvider private constructor(
    private val context: Context
) {

    private val database: AppDatabase by lazy {
        buildDatabase()
    }

    private fun buildDatabase(): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            DB_NAME
        )
            .addCallback(databaseCallback)
            .fallbackToDestructiveMigration()
            .build()
    }

    private val databaseCallback = object : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            Log.d("DB", "Database created")
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            Log.d("DB", "Database opened")
        }
    }

    /**
     * Inserts a chess game into the database.
     * @param game The GameEntity object to insert.
     * @return true if the game was successfully saved, false otherwise.
     */
    suspend fun addGame(game: GameEntity, moves: List<Move>): Boolean {
        val gameSaver = GameSaver(database)
        return gameSaver.trySaveGame(game, moves, game.sideMask)
    }

    suspend fun updateGame(game: GameEntity, moves: List<Move>): Boolean {
        val gameUpdater = GameUpdater(database)
        return gameUpdater.updateGame(game, moves)
    }

    suspend fun getAllGames(): List<GameEntity> {
        return database.gameDao().getAllGames()
    }

    suspend fun loadTrainingGame(gameId: Long): GameEntity? {
        val trainSingleGameService = TrainSingleGameService(database)

        return trainSingleGameService.loadGame(gameId)
    }

    suspend fun deleteGame(id: Long) {
        val gameDeleter = GameDeleter(database)
        gameDeleter.deleteGame(id)
    }

    suspend fun createTrainingFromGames(
        name: String = "FullTraining",
        games: List<OneGameTrainingData>
    ): Long? {
        val trainingService = TrainingService(
            database = database,
            gameDao = database.gameDao(),
            dao = database.trainingDao(),
            templateDao = database.trainingTemplateDao()
        )

        return trainingService.createTrainingFromGames(name = name, games = games)
    }

    suspend fun finishTrainingGame(trainingId: Long, gameId: Long): Boolean {
        val trainSingleGameService = TrainSingleGameService(database)

        return trainSingleGameService.finishTraining(
            trainingId = trainingId,
            gameId = gameId
        )
    }

    suspend fun getFirstTrainingGameLaunchData(): FirstTrainingGameLaunchResult {
        val trainSingleGameService = TrainSingleGameService(database)
        return trainSingleGameService.getFirstTrainingGameLaunchData()
    }

    companion object {
        private const val DB_NAME = "app_database"

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var _instance: DatabaseProvider? = null

        fun createInstance(context: Context): DatabaseProvider {
            synchronized(this) {
                if (_instance != null) {
                    throw IllegalStateException(
                        "DatabaseProvider already was initialized." +
                                " Please use existing object.")
                }
                val newInstance = DatabaseProvider(context.applicationContext)
                _instance = newInstance
                return newInstance
            }
        }
    }
}
