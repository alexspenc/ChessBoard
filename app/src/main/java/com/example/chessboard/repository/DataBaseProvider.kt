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
import com.example.chessboard.entity.GlobalTrainingStatsEntity
import com.example.chessboard.entity.PositionEntity
import com.example.chessboard.entity.SavedSearchPositionEntity
import com.example.chessboard.entity.TrainingEntity
import com.example.chessboard.entity.TrainingResultEntity
import com.example.chessboard.entity.TrainingTemplateEntity
import com.example.chessboard.entity.UserProfileEntity
import com.example.chessboard.service.TrainingResultService
import com.example.chessboard.service.TrainingGameLaunchResult
import com.example.chessboard.service.GameBackupService
import com.example.chessboard.service.GameDeleter
import com.example.chessboard.service.GameListService
import com.example.chessboard.service.GameSaver
import com.example.chessboard.service.GameUpdater
import com.example.chessboard.service.GlobalTrainingStatsService
import com.example.chessboard.service.OneGameTrainingData
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
    version = 11
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

    suspend fun addGame(game: GameEntity, moves: List<Move>): Boolean {
        val gameSaver = GameSaver(database)
        return gameSaver.trySaveGame(game, moves, game.sideMask)
    }

    suspend fun addGameAndGetId(game: GameEntity, moves: List<Move>): Long? {
        val gameSaver = GameSaver(database)
        return gameSaver.saveGame(game, moves, game.sideMask)
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

    suspend fun findPositionsByFenWithoutMoveNumber(fen: String): List<PositionEntity> {
        val positionService = PositionService(database)
        return positionService.findPositionsByFenWithoutMoveNumber(fen)
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

    suspend fun deleteTraining(trainingId: Long): Boolean {
        val trainingService = createTrainingService()
        return trainingService.deleteTraining(trainingId)
    }

    suspend fun createTrainingFromGames(
        name: String = "FullTraining",
        games: List<OneGameTrainingData>
    ): Long? {
        val trainingService = createTrainingService()
        return trainingService.createTrainingFromGames(name = name, games = games)
    }

    suspend fun updateTrainingFromGames(
        trainingId: Long,
        name: String = "FullTraining",
        games: List<OneGameTrainingData>
    ): Boolean {
        val trainingService = createTrainingService()
        return trainingService.updateTrainingFromGames(
            trainingId = trainingId,
            name = name,
            games = games
        )
    }

    suspend fun getAllTrainings(): List<TrainingEntity> {
        val trainingService = createTrainingService()
        return trainingService.getAllTrainings()
    }

    suspend fun getTrainingById(trainingId: Long): TrainingEntity? {
        val trainingService = createTrainingService()
        return trainingService.getTrainingById(trainingId)
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


    suspend fun getRecentTrainingResults(limit: Int): List<TrainingResultEntity> {
        val trainingResultService = createTrainingResultService()
        return trainingResultService.getRecentResults(limit)
    }

    suspend fun getGlobalTrainingStats(): GlobalTrainingStatsEntity {
        val globalTrainingStatsService = createGlobalTrainingStatsService()
        return globalTrainingStatsService.getStats()
    }

    suspend fun recordGlobalTrainingResult(mistakesCount: Int): GlobalTrainingStatsEntity {
        val globalTrainingStatsService = createGlobalTrainingStatsService()
        return globalTrainingStatsService.recordTrainingResult(mistakesCount)
    }

    suspend fun getTrainingResultsForGame(gameId: Long, limit: Int): List<TrainingResultEntity> {
        val trainingResultService = createTrainingResultService()
        return trainingResultService.getResultsForGame(
            gameId = gameId,
            limit = limit
        )
    }

    suspend fun getTrainingGameLaunchData(trainingId: Long): TrainingGameLaunchResult {
        val trainSingleGameService = TrainSingleGameService(database)
        return trainSingleGameService.getTrainingGameLaunchData(trainingId)
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

    suspend fun getUserProfile(): UserProfileEntity {
        val service = UserProfileService(database.userProfileDao())
        return service.getProfile()
    }

    suspend fun updateUserProfileRankTitle(tier: String, title: String) {
        val service = UserProfileService(database.userProfileDao())
        service.updateRankTitle(tier, title)
    }

    suspend fun updateUserProfileSettings(
        simpleViewEnabled: Boolean,
        dontRemoveLineIfRepIsZero: Boolean,
        hideLinesWithWeightZero: Boolean,
    ) {
        val service = UserProfileService(database.userProfileDao())
        service.updateSettings(simpleViewEnabled, dontRemoveLineIfRepIsZero, hideLinesWithWeightZero)
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

    private fun createTrainingService(): TrainingService {
        return TrainingService(
            database = database,
            gameDao = database.gameDao(),
            dao = database.trainingDao(),
            templateDao = database.trainingTemplateDao()
        )
    }

    companion object {
        private const val DB_NAME = "app_database"

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
