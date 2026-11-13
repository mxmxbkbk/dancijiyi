package com.dancijiyi.ui.wordlist

import com.dancijiyi.data.Word

/**
 * 单词列表页状态。words 是当前筛选后的结果，totalCount/dueCount 用于统计条。
 */
data class WordListUiState(
    val isLoading: Boolean = true,
    val words: List<Word> = emptyList(),
    val totalCount: Int = 0,
    val dueCount: Int = 0,
    val filter: WordFilter = WordFilter.ALL,
    val errorMessage: String? = null,
)
