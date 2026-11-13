package com.dancijiyi.ui.detail

import com.dancijiyi.data.Word

data class WordDetailUiState(
    val isLoading: Boolean = true,
    val word: Word? = null,
    val errorMessage: String? = null,
)

sealed interface WordDetailEvent {
    data object Deleted : WordDetailEvent
    data class Error(val message: String) : WordDetailEvent
}
