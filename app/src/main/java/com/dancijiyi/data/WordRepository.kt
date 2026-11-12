package com.dancijiyi.data

import kotlinx.coroutines.flow.Flow

/**
 * 单词数据仓库：对 UI 层隐藏 Room 与线程切换细节。
 */
class WordRepository(private val wordDao: WordDao) {

    fun observeAll(): Flow<List<Word>> = wordDao.observeAll()

    fun observeDue(now: Long = System.currentTimeMillis()): Flow<List<Word>> =
        wordDao.observeDue(now)

    suspend fun add(word: Word): Long = wordDao.upsert(word)

    suspend fun remove(word: Word) = wordDao.delete(word)

    suspend fun count(): Int = wordDao.count()
}
