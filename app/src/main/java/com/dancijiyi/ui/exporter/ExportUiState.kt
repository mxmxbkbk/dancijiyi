package com.dancijiyi.ui.exporter

data class ExportUiState(
    val isExporting: Boolean = false,
)

sealed interface ExportEvent {
    data class Success(val path: String) : ExportEvent
    data class Error(val message: String) : ExportEvent
}
