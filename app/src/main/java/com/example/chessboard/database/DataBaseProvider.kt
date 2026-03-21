package com.example.chessboard.database

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Database
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

// Start tables description
@Entity(
    tableName = "games",
    indices = [
        Index(value = ["white"]),
        Index(value = ["black"]),
        Index(value = ["event"]),
        Index(value = ["date"])
    ]
)
data class GameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val white: String? = null,
    val black: String? = null,
    val result: String? = null,
    val event: String? = null,
    val site: String? = null,
    val date: Long = 0,
    val round: String? = null,
    val eco: String? = null,
    val pgn: String,
    val initialFen: String
)

// We can have collision on hash
// So first select all by hash and then in application compare with fen
@Entity(
    tableName = "positions",
    indices = [
        Index(value = ["hash"], unique = true)
    ]
)
data class PositionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hash: Long,
    val fen: String
)

@Entity(
    tableName = "game_positions",
    primaryKeys = ["gameId", "ply"],
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PositionEntity::class,
            parentColumns = ["id"],
            childColumns = ["positionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["positionId"]),
        Index(value = ["gameId"])
    ]
)
data class GamePositionEntity(
    val gameId: Long,
    val positionId: Long,
    val ply: Int
)

@Dao
interface GameDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGame(game: GameEntity): Long

    @Query("DELETE FROM games")
    suspend fun deleteAllGames()

    @Query("SELECT COUNT(*) FROM games")
    suspend fun getCount(): Int
}

@Dao
interface PositionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosition(position: PositionEntity): Long

    @Query("DELETE FROM positions")
    suspend fun deleteAllPositions()

    // Possible to have different fen by same hash
    // So select list of fen
    @Query("SELECT fen FROM positions WHERE hash = :hash")
    suspend fun getFensByHash(hash: Long): List<String>

    @Query("""SELECT id FROM positions 
        WHERE hash = :hash AND fen = :fen 
        LIMIT 1""")
    suspend fun getIdByHashAndFen(hash: Long, fen: String): Long?
}

@Dao
interface GamePositionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGamePosition(gamePosition: GamePositionEntity): Long

    @Query("DELETE FROM game_positions")
    suspend fun deleteAllGamePositions()
}

@Database(
    entities = [
        GameEntity::class,
        PositionEntity::class,
        GamePositionEntity::class
    ],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun positionDao(): PositionDao
    abstract fun gamePositionDao(): GamePositionDao
}

// End tables description
// ########################################################
// ########################################################
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
     * @return The row ID of the newly inserted game.
     */
    suspend fun addGame(game: GameEntity): Long {
        return database.gameDao().insertGame(game)
    }

    suspend fun getGamesCount(): Int {
        return database.gameDao().getCount()
    }

    suspend fun clearAllData () {
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