package com.dancijiyi.ui.importer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dancijiyi.ui.common.ScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 导入页 ViewModel。解析、去重与写入数据库留到后续实现，
 * 这里只保留协程调用入口和 Loading/Content 状态占位。
 */
class ImportViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ScreenState.CONTENT)
    val uiState: StateFlow<ScreenState> = _uiState.asStateFlow()

    fun importWords(rawText: String) {
        viewModelScope.launch {
            _uiState.value = ScreenState.LOADING
            // TODO: 解析 rawText，校验并调用 repository.addAll()
            _uiState.value = ScreenState.CONTENT
        }
    }
}
