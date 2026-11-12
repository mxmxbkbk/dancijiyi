package com.dancijiyi.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {

    /** 首页列表：最近添加的排在前面。 */
    @Query("SELECT * FROM words ORDER BY addedAt DESC, id DESC")
    fun observeAll(): Flow<List<Word>>

    /** 单词详情；不存在时发射 null。 */
    @Query("SELECT * FROM words WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<Word?>

    /** 复习队列：未掌握且已到复习时间，按到期先后排序。 */
    @Query(
        """
        SELECT * FROM words
        WHERE mastered = 0 AND reviewAt <= :now
        ORDER BY reviewAt ASC, id ASC
        """,
    )
    fun observeDue(now: Long): Flow<List<Word>>

    @Query("SELECT COUNT(*) FROM words")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM words WHERE mastered = 0 AND reviewAt <= :now")
    fun observeDueCount(now: Long): Flow<Int>

    /** 返回新行 id；word 唯一冲突时返回 -1（IGNORE）。 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(word: Word): Long

    /** 批量导入，返回与入参等长的 rowId 列表，冲突项为 -1。 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(words: List<Word>): List<Long>

    @Update
    suspend fun update(word: Word): Int

    /** 复习完成后只更新调度相关字段，不改动 word/meaning/addedAt。 */
    @Query(
        """
        UPDATE words
        SET reviewAt = :reviewAt,
            memoryStage = :memoryStage,
            mastered = :mastered
        WHERE id = :id
        """,
    )
    suspend fun updateReviewState(
        id: Long,
        reviewAt: Long,
        memoryStage: Int,
        mastered: Boolean,
    ): Int

    @Delete
    suspend fun delete(word: Word): Int

    @Query("DELETE FROM words WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("SELECT COUNT(*) FROM words")
    suspend fun count(): Int
}
