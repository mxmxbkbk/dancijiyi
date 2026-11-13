package com.dancijiyi.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dancijiyi.data.Word
import com.dancijiyi.data.WordRepository
import com.dancijiyi.ui.common.toUserMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 单词详情页 ViewModel：按 wordId 订阅单词，处理编辑与删除。
 */
class WordDetailViewModel(
    private val repository: WordRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WordDetailUiState())
    val uiState: StateFlow<WordDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<WordDetailEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<WordDetailEvent> = _events.asSharedFlow()

    private var loadedWordId: Long? = null

    fun loadWord(wordId: Long) {
        if (loadedWordId == wordId) return
        loadedWordId = wordId
        _uiState.value = WordDetailUiState(isLoading = true)

        viewModelScope.launch {
            repository.observeById(wordId)
                .catch { throwable ->
                    loadedWordId = null
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.toUserMessage("加载单词失败"),
                        )
                    }
                }
                .collect { word ->
                    _uiState.value = WordDetailUiState(
                        isLoading = false,
                        word = word,
                    )
                }
        }
    }

    fun updateWord(original: Word, word: String, meaning: String) {
        val cleanWord = word.trim()
        val cleanMeaning = meaning.trim()
        if (cleanWord.isEmpty() || cleanMeaning.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "单词和释义不能为空") }
            return
        }

        viewModelScope.launch {
            try {
                val rows = repository.update(
                    original.copy(
                        word = cleanWord,
                        meaning = cleanMeaning,
                    ),
                )
                if (rows == 0) {
                    _uiState.update { it.copy(errorMessage = "保存失败，单词可能已被删除") }
                }
            } catch (throwable: Exception) {
                _uiState.update {
                    it.copy(errorMessage = throwable.toUserMessage("保存单词失败"))
                }
            }
        }
    }

    fun deleteWord(word: Word) {
        viewModelScope.launch {
            try {
                val rows = repository.remove(word)
                if (rows > 0) {
                    _events.emit(WordDetailEvent.Deleted)
                } else {
                    _events.emit(WordDetailEvent.Error("删除失败，单词可能已被删除"))
                }
            } catch (throwable: Exception) {
                _events.emit(WordDetailEvent.Error(throwable.toUserMessage("删除单词失败")))
            }
        }
    }

    fun setMastered(word: Word, mastered: Boolean) {
        viewModelScope.launch {
            try {
                val rows = repository.update(word.copy(mastered = mastered))
                if (rows == 0) {
                    _uiState.update { it.copy(errorMessage = "更新掌握状态失败") }
                }
            } catch (throwable: Exception) {
                _uiState.update {
                    it.copy(errorMessage = throwable.toUserMessage("更新掌握状态失败"))
                }
            }
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
