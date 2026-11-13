package com.dancijiyi.data.importer

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

data class LoadedFile(
    val name: String,
    val text: String,
)

/**
 * 通过系统文件选择器返回的 content Uri 读取文本，全部在 IO 线程完成。
 */
class LocalFileReader(
    private val contentResolver: ContentResolver,
) {

    suspend fun read(uri: Uri): LoadedFile = withContext(Dispatchers.IO) {
        val name = queryDisplayName(uri)
        val bytes = readBytes(uri)
        LoadedFile(
            name = name,
            text = String(bytes, Charsets.UTF_8),
        )
    }

    private fun queryDisplayName(uri: Uri): String {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    cursor.getString(index)?.takeIf { it.isNotBlank() }?.let { return it }
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "词库文件"
    }

    private fun readBytes(uri: Uri): ByteArray {
        val output = ByteArrayOutputStream()
        contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(8 * 1024)
            var total = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                if (total > MAX_FILE_BYTES) {
                    throw IOException("文件超过 5 MB，请选择更小的词库")
                }
                output.write(buffer, 0, count)
            }
        } ?: throw IOException("无法打开文件")

        return output.toByteArray()
    }

    companion object {
        private const val MAX_FILE_BYTES = 5 * 1024 * 1024
    }
}
