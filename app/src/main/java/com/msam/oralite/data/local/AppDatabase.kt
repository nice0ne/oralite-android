package com.msam.oralite.data.local

import android.content.Context
import androidx.room.*
import com.msam.oralite.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM connection_profiles ORDER BY id ASC")
    fun getAll(): Flow<List<ConnectionProfile>>

    @Query("SELECT COUNT(*) FROM connection_profiles")
    suspend fun count(): Int

    @Query("UPDATE connection_profiles SET host = :newIp WHERE host = :oldIp")
    suspend fun updateHostIp(oldIp: String, newIp: String)

    @Query("UPDATE connection_profiles SET restUrl = REPLACE(restUrl, :oldIp, :newIp) WHERE restUrl LIKE '%' || :oldIp || '%'")
    suspend fun updateRestUrl(oldIp: String, newIp: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ConnectionProfile): Long

    @Delete
    suspend fun delete(profile: ConnectionProfile)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM query_history ORDER BY executedAt DESC LIMIT 50")
    fun getRecent(): Flow<List<QueryHistory>>

    @Insert
    suspend fun insert(history: QueryHistory): Long

    @Query("DELETE FROM query_history")
    suspend fun clearAll()
}

@Dao
interface SavedQueryDao {
    @Query("SELECT * FROM saved_queries ORDER BY createdAt DESC")
    fun getAll(): Flow<List<SavedQuery>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(query: SavedQuery): Long

    @Delete
    suspend fun delete(query: SavedQuery)
}

@Database(
    entities = [ConnectionProfile::class, QueryHistory::class, SavedQuery::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun historyDao(): HistoryDao
    abstract fun savedQueryDao(): SavedQueryDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "oralite.db"
                ).build().also { instance = it }
            }
        }
    }
}
