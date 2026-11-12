package com.dancijiyi.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {

    @Query("SELECT * FROM words ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE nextReviewAt IS NULL OR nextReviewAt <= :now ORDER BY nextReviewAt ASC")
    fun observeDue(now: Long): Flow<List<Word>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(word: Word): Long

    @Delete
    suspend fun delete(word: Word)

    @Query("SELECT COUNT(*) FROM words")
    suspend fun count(): Int
}
