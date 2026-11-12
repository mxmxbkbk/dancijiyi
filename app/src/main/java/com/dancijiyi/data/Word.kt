package com.dancijiyi.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 单词实体。
 *
 * 字段与产品需求一一对应：
 * - [word] 单词，大小写不敏感唯一，避免 "Apple" / "apple" 重复入库；
 * - [meaning] 释义；
 * - [addedAt] 添加时间（epoch millis）；
 * - [reviewAt] 下一次复习时间（epoch millis），新词默认立即到期；
 * - [memoryStage] 记忆阶段，取值 0..5；
 * - [mastered] 是否掌握，true 时不再进入待复习队列。
 */
@Entity(
    tableName = "words",
    indices = [
        Index(value = ["word"], unique = true),
        Index(value = ["mastered", "reviewAt"]),
        Index(value = ["addedAt"]),
    ],
)
data class Word(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val word: String,

    val meaning: String,

    val addedAt: Long = System.currentTimeMillis(),

    val reviewAt: Long = System.currentTimeMillis(),

    val memoryStage: Int = STAGE_NEW,

    val mastered: Boolean = false,
) {
    companion object {
        /** 新词，尚未开始记忆。 */
        const val STAGE_NEW = 0

        /** 初识，见过并理解释义。 */
        const val STAGE_FAMILIAR = 1

        /** 短期记忆，能够主动回忆。 */
        const val STAGE_SHORT_TERM = 2

        /** 中期记忆，间隔复习后仍能回忆。 */
        const val STAGE_MID_TERM = 3

        /** 长期记忆，较长时间未复习仍能回忆。 */
        const val STAGE_LONG_TERM = 4

        /** 熟练，接近掌握。 */
        const val STAGE_MASTERED = 5

        val STAGE_RANGE = STAGE_NEW..STAGE_MASTERED
    }
}
