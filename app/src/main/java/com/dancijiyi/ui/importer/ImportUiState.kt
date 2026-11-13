package com.dancijiyi.ui.importer

import com.dancijiyi.data.importer.ParseResult

data class ImportUiState(
    val isReading: Boolean = false,
    val isParsing: Boolean = false,
    val isImporting: Boolean = false,
    val sourceName: String? = null,
    val preview: ParseResult? = null,
) {
    val isBusy: Boolean
        get() = isReading || isParsing || isImporting
}

sealed interface ImportEvent {
    data class Result(
        val inserted: Int,
        val skipped: Int,
        val failed: Int,
        val errorMessage: String? = null,
    ) : ImportEvent

    data class Error(val message: String) : ImportEvent
}
