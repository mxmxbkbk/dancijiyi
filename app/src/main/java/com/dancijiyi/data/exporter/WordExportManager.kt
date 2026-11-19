package com.dancijiyi.data.exporter

import android.content.Context
import android.os.Build
import android.os.Environment
import com.dancijiyi.data.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 把全部单词写入用户可访问的下载目录：
 * - Android 10+：MediaStore.Downloads，无需存储权限；
 * - Android 9 及以下：公共 Download 目录，需要 WRITE_EXTERNAL_STORAGE。
 *
 * 所有文件 IO 都在 Dispatchers.IO 执行。
 */
class WordExportManager(
    context: Context,
) {

    private val appContext = context.applicationContext

    suspend fun export(words: List<Word>, format: ExportFormat): ExportedFile =
        withContext(Dispatchers.IO) {
            if (words.isEmpty()) {
                throw IOException("没有可导出的单词")
            }

            val content = when (format) {
                ExportFormat.CSV -> WordExportFormatter.buildCsv(words)
                ExportFormat.TXT -> WordExportFormatter.buildTxt(words)
            }
            val fileName = "dancijiyi_${LocalDateTime.now().format(TIMESTAMP)}.${format.extension}"
            val bytes = content.toByteArray(Charsets.UTF_8)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ExportedFile(
                    displayPath = MediaStoreExportWriter.write(
                        context = appContext,
                        fileName = fileName,
                        mimeType = format.mimeType,
                        bytes = bytes,
                    ),
                )
            } else {
                writeToLegacyDownloads(fileName, bytes)
            }
        }

    private fun writeToLegacyDownloads(fileName: String, bytes: ByteArray): ExportedFile {
        val downloads = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS,
        )
        val directory = File(downloads, "dancijiyi")
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("无法创建下载目录")
        }

        val file = File(directory, fileName)
        try {
            file.outputStream().use { output -> output.write(bytes) }
        } catch (throwable: IOException) {
            throw IOException("写入下载文件失败", throwable)
        }
        return ExportedFile(displayPath = file.absolutePath)
    }

    private companion object {
        val TIMESTAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    }
}
