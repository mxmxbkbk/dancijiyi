package com.dancijiyi.data

import kotlinx.coroutines.flow.Flow

/**
 * 单词数据仓库：对 UI 层隐藏 Room 与线程切换细节。
 *
 * 本阶段只做数据访问编排，不包含业务算法（复习间隔、掌握判定）。
 */
class WordRepository(private val wordDao: WordDao) {

    fun observeAll(): Flow<List<Word>> = wordDao.observeAll()

    suspend fun getAll(): List<Word> = wordDao.getAll()

    fun observeById(id: Long): Flow<Word?> = wordDao.observeById(id)

    fun observeDue(now: Long = System.currentTimeMillis()): Flow<List<Word>> =
        wordDao.observeDue(now)

    fun observeTotalCount(): Flow<Int> = wordDao.observeTotalCount()

    fun observeDueCount(now: Long = System.currentTimeMillis()): Flow<Int> =
        wordDao.observeDueCount(now)

    suspend fun add(word: Word): Long = wordDao.insert(word)

    suspend fun addAll(words: List<Word>): List<Long> = wordDao.insertAll(words)

    suspend fun update(word: Word): Int = wordDao.update(word)

    suspend fun updateReviewState(
        id: Long,
        reviewAt: Long,
        memoryStage: Int,
        mastered: Boolean,
    ): Int = wordDao.updateReviewState(id, reviewAt, memoryStage, mastered)

    suspend fun remove(word: Word): Int = wordDao.delete(word)

    suspend fun removeById(id: Long): Int = wordDao.deleteById(id)

    suspend fun count(): Int = wordDao.count()
}
