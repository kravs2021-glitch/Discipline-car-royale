package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM exam_history ORDER BY epochMillis ASC")
    fun observeAllChronological(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntity)

    @Query("DELETE FROM exam_history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM exam_history")
    suspend fun clearAll()
}
