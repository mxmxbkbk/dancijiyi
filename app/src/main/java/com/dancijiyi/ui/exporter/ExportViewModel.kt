package com.dancijiyi.ui.exporter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dancijiyi.data.WordRepository
import com.dancijiyi.data.exporter.ExportFormat
import com.dancijiyi.data.exporter.WordExportManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * 导出 ViewModel：读取全部单词并异步写入下载目录。
 */
class ExportViewModel(
    private val repository: WordRepository,
    private val exportManager: WordExportManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ExportEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ExportEvent> = _events.asSharedFlow()

    fun export(format: ExportFormat) {
        if (_uiState.value.isExporting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            try {
                val words = repository.getAll()
                val file = exportManager.export(words, format)
                _events.emit(ExportEvent.Success(file.displayPath))
            } catch (throwable: IOException) {
                _events.emit(
                    ExportEvent.Error("导出失败：${throwable.message ?: "IO 异常"}"),
                )
            } catch (throwable: Exception) {
                _events.emit(
                    ExportEvent.Error("导出失败：${throwable.message ?: "未知错误"}"),
                )
            } finally {
                _uiState.update { it.copy(isExporting = false) }
            }
        }
    }
}
