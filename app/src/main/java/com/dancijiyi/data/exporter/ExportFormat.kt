package com.dancijiyi.data.exporter

enum class ExportFormat(
    val extension: String,
    val mimeType: String,
) {
    CSV(extension = "csv", mimeType = "text/csv"),
    TXT(extension = "txt", mimeType = "text/plain"),
}

data class ExportedFile(
    val displayPath: String,
)
