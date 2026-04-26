package com.example.chessboard.repository

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.chessboard.entity.GameEntity
import com.example.chessboard.entity.GamePositionEntity
import com.example.chessboard.entity.GlobalTrainingStatsEntity
import com.example.chessboard.entity.PositionEntity
import com.example.chessboard.entity.SavedSearchPositionEntity
import com.example.chessboard.entity.TrainingEntity
import com.example.chessboard.entity.TrainingResultEntity
import com.example.chessboard.entity.TrainingTemplateEntity
import com.example.chessboard.entity.UserProfileEntity
import com.example.chessboard.service.SmartTrainingService
import com.example.chessboard.service.TrainingResultService
import com.example.chessboard.service.GameBackupService
import com.example.chessboard.service.GameDeleter
import com.example.chessboard.service.GameListService
import com.example.chessboard.service.GameSaver
import com.example.chessboard.service.GameUpdater
import com.example.chessboard.service.GlobalTrainingStatsService
import com.example.chessboard.service.PositionService
import com.example.chessboard.service.SavedSearchPositionService
import com.example.chessboard.service.StatisticsTrainingService
import com.example.chessboard.service.TrainSingleGameService
import com.example.chessboard.service.TrainingService
import com.example.chessboard.service.TrainingTemplateService
import com.example.chessboard.service.UserProfileService
import com.github.bhlangonijr.chesslib.move.Move

@Database(
    entities = [
        GameEntity::class,
        PositionEntity::class,
        GamePositionEntity::class,
        SavedSearchPositionEntity::class,
        GlobalTrainingStatsEntity::class,
        TrainingTemplateEntity::class,
        TrainingEntity::class,
        TrainingResultEntity::class,
        UserProfileEntity::class,
    ],
    version = 13
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun positionDao(): PositionDao
    abstract fun gamePositionDao(): GamePositionDao
    abstract fun savedSearchPositionDao(): SavedSearchPositionDao
    abstract fun globalTrainingStatsDao(): GlobalTrainingStatsDao
    abstract fun trainingTemplateDao(): TrainingTemplateDao
    abstract fun trainingDao(): TrainingDao
    abstract fun trainingResultDao(): TrainingResultDao
    abstract fun userProfileDao(): UserProfileDao
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
            .addMigrations(MIGRATION_12_13)
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

    suspend fun updateGame(game: GameEntity, moves: List<Move>): Boolean {
        val gameUpdater = GameUpdater(database)
        return gameUpdater.updateGame(game, moves)
    }

    suspend fun getAllGames(): List<GameEntity> {
        return database.gameDao().getAllGames()
    }

    fun clearAllData() {
        database.clearAllTables()
    }

    fun createGameBackupService(): GameBackupService {
        return GameBackupService(database)
    }

    fun createGameSaver(): GameSaver {
        return GameSaver(database)
    }

    suspend fun findGameIdsByFenWithoutMoveNumber(fen: String): List<Long> {
        val positionService = PositionService(database)
        return positionService.findGameIdsByFenWithoutMoveNumber(fen)
    }

    suspend fun loadTrainingGame(gameId: Long): GameEntity? {
        val trainSingleGameService = TrainSingleGameService(database)
        return trainSingleGameService.loadGame(gameId)
    }

    suspend fun deleteGame(id: Long) {
        val gameDeleter = GameDeleter(database)
        gameDeleter.deleteGame(id)
    }

    suspend fun recordTrainingGameStats(gameId: Long, mistakesCount: Int) {
        val trainSingleGameService = TrainSingleGameService(database)
        trainSingleGameService.recordTrainingStats(gameId = gameId, mistakesCount = mistakesCount)
    }

    suspend fun finishTrainingGame(
        trainingId: Long,
        gameId: Long,
        mistakesCount: Int,
        keepLineIfZero: Boolean = false
    ): Boolean {
        val trainSingleGameService = TrainSingleGameService(database)

        return trainSingleGameService.finishTraining(
            trainingId = trainingId,
            gameId = gameId,
            mistakesCount = mistakesCount,
            keepLineIfZero = keepLineIfZero
        )
    }

    suspend fun getGlobalTrainingStats(): GlobalTrainingStatsEntity {
        val globalTrainingStatsService = createGlobalTrainingStatsService()
        return globalTrainingStatsService.getStats()
    }

    fun createStatisticsTrainingService(): StatisticsTrainingService {
        return StatisticsTrainingService(database)
    }

    fun createGameListService(): GameListService {
        return GameListService(database.gameDao())
    }

    fun createSavedSearchPositionService(): SavedSearchPositionService {
        return SavedSearchPositionService(database.savedSearchPositionDao())
    }

    fun createSmartTrainingService(): SmartTrainingService {
        return SmartTrainingService(database.trainingDao(), database.trainingResultDao())
    }

    fun createUserProfileService(): UserProfileService {
        return UserProfileService(database.userProfileDao())
    }

    fun createTrainingTemplateService(): TrainingTemplateService {
        return TrainingTemplateService(database.trainingTemplateDao())
    }

    private fun createTrainingResultService(): TrainingResultService {
        return TrainingResultService(database)
    }

    private fun createGlobalTrainingStatsService(): GlobalTrainingStatsService {
        return GlobalTrainingStatsService(database)
    }

    fun createTrainingService(): TrainingService {
        return TrainingService(
            database = database,
            gameDao = database.gameDao(),
            dao = database.trainingDao(),
            templateDao = database.trainingTemplateDao()
        )
    }

    companion object {
        private const val DB_NAME = "app_database"

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `user_profile_new` (
                        `id` INTEGER NOT NULL,
                        `rankTier` TEXT NOT NULL,
                        `rankTitle` TEXT NOT NULL,
                        `simpleViewEnabled` INTEGER NOT NULL,
                        `removeLineIfRepIsZero` INTEGER NOT NULL,
                        `hideLinesWithWeightZero` INTEGER NOT NULL,
                        `hideSmartTrainingInfoCard` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )"""
                )
                database.execSQL(
                    """INSERT INTO `user_profile_new`
                        SELECT id, rankTier, rankTitle, simpleViewEnabled,
                               (1 - dontRemoveLineIfRepIsZero),
                               hideLinesWithWeightZero, hideSmartTrainingInfoCard
                        FROM `user_profile`"""
                )
                database.execSQL("DROP TABLE `user_profile`")
                database.execSQL("ALTER TABLE `user_profile_new` RENAME TO `user_profile`")
            }
        }

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var _instance: DatabaseProvider? = null

        fun createInstance(context: Context): DatabaseProvider {
            _instance?.let { return it }

            synchronized(this) {
                _instance?.let { return it }

                val newInstance = DatabaseProvider(context.applicationContext)
                _instance = newInstance
                return newInstance
            }
        }
    }
}
