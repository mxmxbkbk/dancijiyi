package com.dancijiyi.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dancijiyi.data.WordRepository
import com.dancijiyi.ui.detail.WordDetailViewModel
import com.dancijiyi.ui.wordlist.WordListViewModel

/**
 * 为需要 Repository 的 ViewModel 提供依赖。
 */
class AppViewModelFactory(
    private val repository: WordRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(WordListViewModel::class.java) ->
            WordListViewModel(repository) as T

        modelClass.isAssignableFrom(WordDetailViewModel::class.java) ->
            WordDetailViewModel(repository) as T

        else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
