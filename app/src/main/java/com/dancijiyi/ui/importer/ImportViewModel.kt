package com.dancijiyi.ui.importer

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dancijiyi.data.Word
import com.dancijiyi.data.WordRepository
import com.dancijiyi.data.importer.LocalFileReader
import com.dancijiyi.data.importer.ParseResult
import com.dancijiyi.data.importer.VocabularyParser
import com.dancijiyi.ui.common.toUserMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 词库导入 ViewModel：
 * - 文件读取在 IO 线程；
 * - 解析在 Default 线程；
 * - 批量写 Room 在 IO 线程；
 * - 结果通过 SharedFlow 通知页面弹窗。
 */
class ImportViewModel(
    private val repository: WordRepository,
    private val fileReader: LocalFileReader,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ImportEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ImportEvent> = _events.asSharedFlow()

    private var currentPreview: ParseResult? = null

    fun loadFile(uri: Uri) {
        viewModelScope.launch {
            currentPreview = null
            _uiState.update {
                it.copy(
                    isReading = true,
                    preview = null,
                )
            }

            try {
                val file = fileReader.read(uri)
                _uiState.update {
                    it.copy(
                        isReading = false,
                        sourceName = file.name,
                    )
                }
                parse(file.text)
            } catch (throwable: Exception) {
                _uiState.update { it.copy(isReading = false) }
                _events.emit(ImportEvent.Error(throwable.toUserMessage("读取文件失败")))
            }
        }
    }

    private suspend fun parse(text: String) {
        _uiState.update { it.copy(isParsing = true) }
        val result = withContext(Dispatchers.Default) {
            VocabularyParser.parse(text)
        }
        currentPreview = result
        _uiState.update {
            it.copy(
                isParsing = false,
                preview = result,
            )
        }

        if (result.entries.isEmpty()) {
            _events.emit(
                ImportEvent.Error(
                    "未解析到有效词条：有效 0 条，重复 ${result.duplicateCount} 条，" +
                        "错误 ${result.issues.size} 条。请检查文件格式。",
                ),
            )
        }
    }

    fun importWords() {
        val preview = currentPreview
        if (preview == null) {
            _events.tryEmit(ImportEvent.Error("请先选择并解析词库文件"))
            return
        }
        if (preview.entries.isEmpty()) {
            _events.tryEmit(ImportEvent.Error("没有可导入的有效词条"))
            return
        }
        if (_uiState.value.isImporting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true) }

            var inserted = 0
            var skipped = 0
            var failed = preview.issues.size

            try {
                preview.entries.chunked(BATCH_SIZE).forEach { chunk ->
                    val rowIds = withContext(Dispatchers.IO) {
                        repository.addAll(
                            chunk.map { parsed ->
                                Word(
                                    word = parsed.word,
                                    meaning = parsed.meaning,
                                )
                            },
                        )
                    }
                    inserted += rowIds.count { it >= 0L }
                    skipped += rowIds.count { it == -1L }
                }

                _uiState.update { it.copy(isImporting = false) }
                _events.emit(ImportEvent.Result(inserted, skipped, failed))
            } catch (throwable: Exception) {
                failed += preview.entries.size - inserted - skipped
                _uiState.update { it.copy(isImporting = false) }
                _events.emit(
                    ImportEvent.Result(
                        inserted = inserted,
                        skipped = skipped,
                        failed = failed,
                        errorMessage = throwable.toUserMessage("导入失败"),
                    ),
                )
            }
        }
    }

    private companion object {
        /** 控制单次 SQLite 变量数量，避免一次性绑定过多参数。 */
        const val BATCH_SIZE = 100
    }
}
