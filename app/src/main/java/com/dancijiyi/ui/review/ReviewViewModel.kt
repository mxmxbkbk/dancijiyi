package com.dancijiyi.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dancijiyi.ui.common.ScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 复习页 ViewModel。当前只展示卡片骨架；
 * 后续由 repository.observeDue() 提供队列，并在 submitRating() 中更新调度字段。
 */
class ReviewViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ScreenState.LOADING)
    val uiState: StateFlow<ScreenState> = _uiState.asStateFlow()

    init {
        startReview()
    }

    fun startReview() {
        viewModelScope.launch {
            // TODO: repository.observeDue().collect { words -> ... }
            _uiState.value = ScreenState.CONTENT
        }
    }

    fun submitRating(rating: ReviewRating) {
        viewModelScope.launch {
            // TODO: 根据 rating 计算下次复习时间并调用 repository.updateReviewState()
        }
    }
}
