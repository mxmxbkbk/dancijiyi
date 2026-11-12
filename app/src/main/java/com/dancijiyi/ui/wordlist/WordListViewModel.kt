package com.dancijiyi.ui.wordlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dancijiyi.ui.common.ScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 单词列表页 ViewModel。
 *
 * 当前不接数据库，只演示 Loading -> Empty 的状态流转；
 * 后续在此通过 repository.observeAll() 订阅真实单词列表。
 */
class WordListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ScreenState.LOADING)
    val uiState: StateFlow<ScreenState> = _uiState.asStateFlow()

    init {
        loadWords()
    }

    fun loadWords() {
        viewModelScope.launch {
            // TODO: repository.observeAll().collect { words -> ... }
            _uiState.value = ScreenState.EMPTY
        }
    }
}
