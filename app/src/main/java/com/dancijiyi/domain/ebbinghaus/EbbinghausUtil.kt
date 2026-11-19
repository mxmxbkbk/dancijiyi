package com.dancijiyi.domain.ebbinghaus

import java.util.concurrent.TimeUnit

/**
 * 艾宾浩斯遗忘曲线复习等级工具。
 *
 * 等级与间隔：
 * 0 = 刚添加，5 分钟
 * 1 = 30 分钟
 * 2 = 12 小时
 * 3 = 1 天
 * 4 = 2 天
 * 5 = 4 天
 * 6 = 7 天（满级，不再安排下一次复习）
 *
 * 该工具只负责等级与时间计算，不依赖 Android、Room 或页面。
 */
object EbbinghausUtil {

    const val MIN_LEVEL: Int = 0
    const val MAX_LEVEL: Int = 6

    private val INTERVAL_MILLIS: LongArray = longArrayOf(
        TimeUnit.MINUTES.toMillis(5),
        TimeUnit.MINUTES.toMillis(30),
        TimeUnit.HOURS.toMillis(12),
        TimeUnit.DAYS.toMillis(1),
        TimeUnit.DAYS.toMillis(2),
        TimeUnit.DAYS.toMillis(4),
        TimeUnit.DAYS.toMillis(7),
    )

    /**
     * 返回某等级对应的复习间隔。
     * 等级 6 的 7 天间隔保留用于等级定义/展示；满级本身不再调度。
     */
    fun getIntervalMillis(level: Int): Long {
        requireLevel(level)
        return INTERVAL_MILLIS[level]
    }

    /**
     * 根据当前等级计算下次复习时间。
     *
     * @return 下次复习的 epoch millis；满级返回 null，表示不再复习。
     */
    fun calculateNextReviewTime(
        currentLevel: Int,
        baseTimeMillis: Long = System.currentTimeMillis(),
    ): Long? {
        requireLevel(currentLevel)
        if (currentLevel >= MAX_LEVEL) return null
        return baseTimeMillis + INTERVAL_MILLIS[currentLevel]
    }

    /**
     * 复习正确：等级 +1，最多到满级。
     */
    fun onReviewCorrect(currentLevel: Int): Int {
        requireLevel(currentLevel)
        return (currentLevel + 1).coerceAtMost(MAX_LEVEL)
    }

    /**
     * 复习错误：重置为刚添加等级 0。
     */
    fun onReviewWrong(): Int = MIN_LEVEL

    /**
     * 是否已经达到满级。满级不再复习。
     */
    fun isMastered(currentLevel: Int): Boolean {
        requireLevel(currentLevel)
        return currentLevel >= MAX_LEVEL
    }

    private fun requireLevel(level: Int) {
        require(level in MIN_LEVEL..MAX_LEVEL) {
            "level must be in $MIN_LEVEL..$MAX_LEVEL, but was $level"
        }
    }
}
