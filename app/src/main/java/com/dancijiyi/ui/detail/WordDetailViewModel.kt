package com.dancijiyi.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dancijiyi.ui.common.ScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 单词详情页 ViewModel。当前只负责展示页面骨架，
 * 真实数据通过 wordId 在后续接入 repository.observeById()。
 */
class WordDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ScreenState.LOADING)
    val uiState: StateFlow<ScreenState> = _uiState.asStateFlow()

    fun loadWord(wordId: Long) {
        viewModelScope.launch {
            // TODO: repository.observeById(wordId).collect { word -> ... }
            _uiState.value = ScreenState.CONTENT
        }
    }
}
