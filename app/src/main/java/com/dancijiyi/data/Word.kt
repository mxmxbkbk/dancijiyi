package com.dancijiyi.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 单词实体。term 唯一，避免重复收藏；nextReviewAt 为间隔重复预留。
 */
@Entity(
    tableName = "words",
    indices = [Index(value = ["term"], unique = true)],
)
data class Word(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val term: String,
    val meaning: String,
    val phonetic: String? = null,
    val example: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val nextReviewAt: Long? = null,
    val reviewCount: Int = 0,
)
