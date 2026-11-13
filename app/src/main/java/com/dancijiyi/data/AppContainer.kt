package com.dancijiyi.data

import android.content.Context

/**
 * 轻量应用容器：为 ViewModel 提供进程级单例 Repository。
 */
object AppContainer {

    @Volatile
    private var repository: WordRepository? = null

    fun wordRepository(context: Context): WordRepository =
        repository ?: synchronized(this) {
            repository ?: WordRepository(
                AppDatabase.getInstance(context.applicationContext).wordDao(),
            ).also { repository = it }
        }
}
