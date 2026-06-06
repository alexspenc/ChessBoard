package com.example.chessboard.repository

/*
 * File role: groups Room database wiring, migration definitions, and provider
 * factories used by the app.
 *
 * This is a legacy mixed-responsibility file: it currently defines the Room
 * database, creates the singleton provider, exposes persistence services, and
 * stores database migrations.
 *
 * Prefer keeping new table access definitions in repository/entity files and
 * only add database registration, migrations, or provider factories here.
 *
 * Validation date: 2026-05-25
 */

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.chessboard.entity.DubiousLineEntity
import com.example.chessboard.entity.GlobalTrainingStatsEntity
import com.example.chessboard.entity.LineEntity
import com.example.chessboard.entity.LinePositionEntity
import com.example.chessboard.entity.PositionEntity
import com.example.chessboard.entity.SavedSearchPositionEntity
import com.example.chessboard.entity.StatisticsTrainingFormulaSettingsEntity
import com.example.chessboard.entity.TrainingEntity
import com.example.chessboard.entity.TrainingResultEntity
import com.example.chessboard.entity.TrainingTemplateEntity
import com.example.chessboard.entity.UserProfileEntity
import com.example.chessboard.service.DubiousLineService
import com.example.chessboard.service.GlobalTrainingStatsService
import com.example.chessboard.service.LineBackupService
import com.example.chessboard.service.LineDeleter
import com.example.chessboard.service.LineListService
import com.example.chessboard.service.LineSaver
import com.example.chessboard.service.LineUpdater
import com.example.chessboard.service.PositionService
import com.example.chessboard.service.SavedSearchPositionService
import com.example.chessboard.service.SmartTrainingService
import com.example.chessboard.service.StatisticsTrainingFormulaSettingsService
import com.example.chessboard.service.StatisticsTrainingService
import com.example.chessboard.service.TrainSingleLineService
import com.example.chessboard.service.TrainingResultService
import com.example.chessboard.service.TrainingService
import com.example.chessboard.service.TrainingStatsRecordResult
import com.example.chessboard.service.TrainingTemplateService
import com.example.chessboard.service.UserProfileService
import com.github.bhlangonijr.chesslib.move.Move

@Database(
    entities = [
        LineEntity::class,
        PositionEntity::class,
        LinePositionEntity::class,
        SavedSearchPositionEntity::class,
        GlobalTrainingStatsEntity::class,
        TrainingTemplateEntity::class,
        TrainingEntity::class,
        TrainingResultEntity::class,
        UserProfileEntity::class,
        StatisticsTrainingFormulaSettingsEntity::class,
        DubiousLineEntity::class,
    ],
    version = 20,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lineDao(): LineDao
    abstract fun dubiousLineDao(): DubiousLineDao
    abstract fun positionDao(): PositionDao
    abstract fun linePositionDao(): LinePositionDao
    abstract fun savedSearchPositionDao(): SavedSearchPositionDao
    abstract fun globalTrainingStatsDao(): GlobalTrainingStatsDao
    abstract fun trainingTemplateDao(): TrainingTemplateDao
    abstract fun trainingDao(): TrainingDao
    abstract fun trainingResultDao(): TrainingResultDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun statisticsTrainingFormulaSettingsDao(): StatisticsTrainingFormulaSettingsDao
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
            .addMigrations(
                MIGRATION_12_13,
                MIGRATION_13_14,
                MIGRATION_14_15,
                MIGRATION_15_16,
                MIGRATION_16_17,
                MIGRATION_17_18,
                MIGRATION_18_19,
                MIGRATION_19_20,
            )
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

    suspend fun updateLine(line: LineEntity, moves: List<Move>): Boolean {
        val lineUpdater = LineUpdater(database)
        return lineUpdater.updateLine(line, moves)
    }

    suspend fun getAllLines(): List<LineEntity> {
        return database.lineDao().getAllLines()
    }

    fun clearAllData() {
        database.clearAllTables()
    }

    fun createLineBackupService(): LineBackupService {
        return LineBackupService(database)
    }

    fun createLineSaver(): LineSaver {
        return LineSaver(database)
    }

    fun createTrainSingleLineService(): TrainSingleLineService {
        return TrainSingleLineService(database)
    }

    suspend fun findLineIdsByFenWithoutMoveNumber(fen: String): List<Long> {
        val positionService = PositionService(database)
        return positionService.findLineIdsByFenWithoutMoveNumber(fen)
    }



    fun createLineDeleter(): LineDeleter {
        return LineDeleter(database)
    }


    suspend fun recordTrainingLineStatsCheckingLevelUp(
        lineId: Long,
        mistakesCount: Int,
    ): TrainingStatsRecordResult {
        val trainSingleLineService = TrainSingleLineService(database)
        return trainSingleLineService.recordTrainingStatsCheckingLevelUp(
            lineId = lineId,
            mistakesCount = mistakesCount,
        )
    }

    suspend fun finishTrainingLine(
        trainingId: Long,
        lineId: Long,
        mistakesCount: Int,
        keepLineIfZero: Boolean = false
    ): Boolean {
        val trainSingleLineService = TrainSingleLineService(database)

        return trainSingleLineService.finishTraining(
            trainingId = trainingId,
            lineId = lineId,
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

    fun createStatisticsTrainingFormulaSettingsService(): StatisticsTrainingFormulaSettingsService {
        return StatisticsTrainingFormulaSettingsService(database.statisticsTrainingFormulaSettingsDao())
    }

    fun createLineListService(): LineListService {
        return LineListService(database.lineDao())
    }

    fun createDubiousLineService(): DubiousLineService {
        return DubiousLineService(database.dubiousLineDao())
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
            lineDao = database.lineDao(),
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

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `user_profile` ADD COLUMN `smartMaxLines` INTEGER NOT NULL DEFAULT 10")
                database.execSQL("ALTER TABLE `user_profile` ADD COLUMN `smartOnlyWithMistakes` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `user_profile` ADD COLUMN `autoNextLine` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) = Unit
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `statistics_training_formula_settings` (
                        `id` INTEGER NOT NULL,
                        `recentResultsPerLine` INTEGER NOT NULL,
                        `recencyDaysCap` INTEGER NOT NULL,
                        `lastMistakeWeight` REAL NOT NULL,
                        `maxMistakesLast` INTEGER NOT NULL,
                        `avgMistakesWeight` REAL NOT NULL,
                        `maxAvgMistakesRecent` REAL NOT NULL,
                        `recencyWeight` REAL NOT NULL,
                        `perfectRatePenaltyWeight` REAL NOT NULL,
                        `noAttemptsBoost` REAL NOT NULL,
                        `oneAttemptBoost` REAL NOT NULL,
                        `twoAttemptsBoost` REAL NOT NULL,
                        `weight5ScoreThreshold` REAL NOT NULL,
                        `weight4ScoreThreshold` REAL NOT NULL,
                        `weight3ScoreThreshold` REAL NOT NULL,
                        `weight2ScoreThreshold` REAL NOT NULL,
                        PRIMARY KEY(`id`)
                    )"""
                )
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `dubious_lines` (
                        `gameId` INTEGER NOT NULL,
                        `weight` INTEGER NOT NULL,
                        PRIMARY KEY(`gameId`),
                        FOREIGN KEY(`gameId`) REFERENCES `games`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )""",
                )
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `user_profile` ADD COLUMN `languageTag` TEXT NOT NULL DEFAULT 'en'")
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE `user_profile` ADD COLUMN `disableSimpleViewUpgradePrompt` INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE `user_profile` ADD COLUMN `simpleViewUpgradePromptInterval` INTEGER NOT NULL DEFAULT 20"
                )
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
