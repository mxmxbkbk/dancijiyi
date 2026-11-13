package com.dancijiyi.ui.common

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dancijiyi.data.WordRepository
import com.dancijiyi.data.importer.LocalFileReader
import com.dancijiyi.ui.detail.WordDetailViewModel
import com.dancijiyi.ui.importer.ImportViewModel
import com.dancijiyi.ui.wordlist.WordListViewModel

/**
 * 为需要 Repository / 文件读取器的 ViewModel 提供依赖。
 */
class AppViewModelFactory(
    private val repository: WordRepository,
    private val appContext: Context,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(WordListViewModel::class.java) ->
            WordListViewModel(repository) as T

        modelClass.isAssignableFrom(WordDetailViewModel::class.java) ->
            WordDetailViewModel(repository) as T

        modelClass.isAssignableFrom(ImportViewModel::class.java) ->
            ImportViewModel(
                repository = repository,
                fileReader = LocalFileReader(appContext.contentResolver),
            ) as T

        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
