package com.dancijiyi.data.exporter

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.IOException

/**
 * Android 10+ 的 MediaStore 写入实现单独隔离，避免低版本设备加载到
 * 不存在的 MediaStore.Downloads 类。
 */
@RequiresApi(Build.VERSION_CODES.Q)
internal object MediaStoreExportWriter {

    fun write(
        context: Context,
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
    ): String {
        val resolver = context.contentResolver
        val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/dancijiyi"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("无法在下载目录创建文件")

        try {
            resolver.openOutputStream(uri)?.use { output ->
                output.write(bytes)
            } ?: throw IOException("无法写入下载文件")

            val updated = resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                null,
                null,
            )
            if (updated <= 0) {
                throw IOException("完成下载文件写入失败")
            }
            return "$relativePath/$fileName"
        } catch (throwable: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            throw if (throwable is IOException) {
                throwable
            } else {
                IOException("写入下载文件失败", throwable)
            }
        }
    }
}
