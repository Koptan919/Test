package com.noor.app.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

enum class FavoriteType { SURAH, AYAH, DUA }

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val key: String,
    val type: String,
    val refId: String,
    val titleAr: String,
    val titleEn: String,
    val preview: String?,
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        fun keyOf(type: FavoriteType, refId: String) = "${type.name}:$refId"
    }
}

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey val surah: Int,
    val ayah: Int,
    val ayahCount: Int,
    val updatedAt: Long = System.currentTimeMillis(),
)

// Derived values live outside the entities so Room never mistakes them for columns.
val FavoriteEntity.favoriteType: FavoriteType get() = FavoriteType.valueOf(type)

val ReadingProgressEntity.fraction: Float get() = if (ayahCount == 0) 0f else ayah.toFloat() / ayahCount

@Entity(tableName = "tasbih_state")
data class TasbihEntity(
    @PrimaryKey val id: Int = 1,
    val mode: String,
    val dhikrIndex: Int,
    val count: Int,
    val target: Int,
    val rounds: Int,
    val lifetimeTotal: Long,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT `key` FROM favorites")
    fun observeKeys(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE `key` = :key")
    suspend fun delete(key: String)

    @Query("SELECT COUNT(*) FROM favorites WHERE `key` = :key")
    suspend fun count(key: String): Int
}

@Dao
interface ReadingDao {
    @Query("SELECT * FROM reading_progress ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ReadingProgressEntity>>

    @Query("SELECT * FROM reading_progress ORDER BY updatedAt DESC LIMIT 1")
    fun observeLatest(): Flow<ReadingProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: ReadingProgressEntity)
}

@Dao
interface TasbihDao {
    @Query("SELECT * FROM tasbih_state WHERE id = 1")
    fun observe(): Flow<TasbihEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: TasbihEntity)
}

@Database(
    entities = [FavoriteEntity::class, ReadingProgressEntity::class, TasbihEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class NoorDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun readingDao(): ReadingDao
    abstract fun tasbihDao(): TasbihDao

    companion object {
        fun create(context: Context): NoorDatabase =
            Room.databaseBuilder(context, NoorDatabase::class.java, "noor.db").build()
    }
}
