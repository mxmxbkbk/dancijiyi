package com.dancijiyi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dancijiyi.data.Word
import com.dancijiyi.data.WordRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 首页 ViewModel 骨架：订阅单词列表，并提供新增/删除入口。
 */
class WordViewModel(
    private val repository: WordRepository,
) : ViewModel() {

    val words: StateFlow<List<Word>> = repository.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun addWord(term: String, meaning: String) {
        if (term.isBlank() || meaning.isBlank()) return
        viewModelScope.launch {
            repository.add(
                Word(
                    term = term.trim(),
                    meaning = meaning.trim(),
                ),
            )
        }
    }

    fun removeWord(word: Word) {
        viewModelScope.launch { repository.remove(word) }
    }
}
