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
import com.example.chessboard.entity.TrainingTemplateEntity
import com.example.chessboard.entity.TrainingEntity
import com.example.chessboard.service.GameSaver
import com.github.bhlangonijr.chesslib.move.Move

@Database(
    entities = [
        GameEntity::class,
        PositionEntity::class,
        GamePositionEntity::class,
        TrainingTemplateEntity::class,
        TrainingEntity::class
    ],
    version = 3
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun positionDao(): PositionDao
    abstract fun gamePositionDao(): GamePositionDao
    abstract fun trainingTemplateDao(): TrainingTemplateDao
    abstract fun trainingDao(): TrainingDao
}

// ########################################################

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

    suspend fun getGamesCount(): Int {
        return database.gameDao().getCount()
    }

    suspend fun clearAllData() {
        database.clearAllTables()
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
