package com.dancijiyi.ui.wordlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dancijiyi.data.Word
import com.dancijiyi.data.WordRepository
import com.dancijiyi.ui.common.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 单词列表页 ViewModel：订阅 Room 列表、处理新增与本地筛选。
 */
class WordListViewModel(
    private val repository: WordRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WordListUiState())
    val uiState: StateFlow<WordListUiState> = _uiState.asStateFlow()

    private var allWords: List<Word> = emptyList()
    private var activeFilter: WordFilter = WordFilter.ALL

    init {
        observeWords()
    }

    private fun observeWords() {
        viewModelScope.launch {
            repository.observeAll()
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.toUserMessage("加载单词失败"),
                        )
                    }
                }
                .collect { words ->
                    allWords = words
                    publish()
                }
        }
    }

    fun setFilter(filter: WordFilter) {
        if (activeFilter == filter) return
        activeFilter = filter
        publish()
    }

    fun addWord(word: String, meaning: String) {
        val cleanWord = word.trim()
        val cleanMeaning = meaning.trim()
        if (cleanWord.isEmpty() || cleanMeaning.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "单词和释义不能为空") }
            return
        }

        viewModelScope.launch {
            try {
                val rowId = repository.add(
                    Word(word = cleanWord, meaning = cleanMeaning),
                )
                if (rowId == -1L) {
                    _uiState.update { it.copy(errorMessage = "单词已存在，请勿重复添加") }
                } else {
                    // 新词默认未掌握；回到全部，避免新增后在当前筛选下“看不见”。
                    activeFilter = WordFilter.ALL
                    publish()
                }
            } catch (throwable: Exception) {
                _uiState.update {
                    it.copy(errorMessage = throwable.toUserMessage("新增单词失败"))
                }
            }
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun publish() {
        val now = System.currentTimeMillis()
        val visibleWords = when (activeFilter) {
            WordFilter.ALL -> allWords
            WordFilter.DUE -> allWords.filter { !it.mastered && it.reviewAt <= now }
            WordFilter.MASTERED -> allWords.filter { it.mastered }
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                words = visibleWords,
                totalCount = allWords.size,
                dueCount = allWords.count { word -> !word.mastered && word.reviewAt <= now },
                filter = activeFilter,
            )
        }
    }
}
