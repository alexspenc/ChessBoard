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
import com.github.bhlangonijr.chesslib.move.Move


object SideMask {
    const val WHITE = 1
    const val BLACK = 2
    const val BOTH = WHITE or BLACK
}

// Start tables description
//--------------------------------------------------------------------------------------------------
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
    val initialFen: String,
    val sideMask: Int = SideMask.BOTH
)

//--------------------------------------------------------------------------------------------------
//++++++++++++++++++++++++++++
// We can have collision on hash
// So first select all by hash and then in application compare with fen
//++++++++++++++++++++++++++++
// sideMask — the side from whose perspective the position is considered

// 1 — White
// 2 — Black
// 3 — Either side = bitmask White | Black

// There may be situations where a player uses the same opening line both as White and as Black.
// Without the ability to mark a position for both sides, such a line would be treated as a duplicate and could not be stored.

// There are also positions that are favorable for one side only.
// This flag helps to filter and search for positions for a specific side.
//++++++++++++++++++++++++++++
@Entity(
    tableName = "positions",
    indices = [
        Index(value = ["hash"], unique = false),
        Index(value = ["hash", "fen"], unique = true),
        Index(value = ["hash", "sideMask"], unique = false)
    ]
)
data class PositionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hash: Long,
    val fen: String,
    val sideMask: Int,
)

//--------------------------------------------------------------------------------------------------
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

data class PositionIdWithMask(
    val id: Long,
    val sideMask: Int
)
@Dao
interface PositionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosition(position: PositionEntity): Long

    @Query("DELETE FROM positions")
    suspend fun deleteAllPositions()

    // Possible to have different fen by same hash
    // So select list of fen
    @Query("""SELECT fen FROM positions
            WHERE hash = :hash AND sideMask = :sideMask""")
    suspend fun getFensByHash(hash: Long, sideMask: Int): List<String>

    @Query("""SELECT id, sideMask FROM positions
        WHERE hash = :hash AND fen = :fen 
        LIMIT 1""")
    suspend fun getIdAndSideByHashAndFen(hash: Long, fen: String): PositionIdWithMask?

    @Query("""
        UPDATE positions 
        SET sideMask = :newSide
        WHERE id = :id""")
    suspend fun updateSideMask(id: Long, newSide: Int)

    @Query("SELECT COUNT(*) FROM positions")
    suspend fun getCount(): Int
}

@Dao
interface GamePositionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGamePosition(gamePosition: GamePositionEntity): Long

    @Query("DELETE FROM game_positions")
    suspend fun deleteAllGamePositions()

    @Query("SELECT COUNT(*) FROM game_positions")
    suspend fun getCount(): Int
}

@Database(
    entities = [
        GameEntity::class,
        PositionEntity::class,
        GamePositionEntity::class
    ],
    version = 3
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
    suspend fun addGame(game: GameEntity, moves: List<Move>): Boolean {
        val gameSaver = GameSaver(database)
        return gameSaver.trySaveGame(game, moves, game.sideMask);
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