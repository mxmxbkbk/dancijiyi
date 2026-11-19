package com.dancijiyi.data.exporter

import com.dancijiyi.data.Word

/**
 * 纯文本序列化，便于单元测试 CSV/TXT 格式。
 */
object WordExportFormatter {

    fun buildCsv(words: List<Word>): String = buildString {
        append('\uFEFF')
        append("word,meaning\r\n")
        words.forEach { word ->
            append(escapeCsv(word.word))
            append(',')
            append(escapeCsv(word.meaning))
            append("\r\n")
        }
    }

    fun buildTxt(words: List<Word>): String = buildString {
        words.forEach { word ->
            append(escapeTxt(word.word))
            append('\t')
            append(escapeTxt(word.meaning))
            append('\n')
        }
    }

    private fun escapeCsv(value: String): String {
        val clean = stripLineBreaks(value)
        return if (clean.any { it == ',' || it == '"' }) {
            "\"" + clean.replace("\"", "\"\"") + "\""
        } else {
            clean
        }
    }

    private fun escapeTxt(value: String): String =
        stripLineBreaks(value).replace('\t', ' ')

    private fun stripLineBreaks(value: String): String =
        value.replace("\r\n", " ")
            .replace('\n', ' ')
            .replace('\r', ' ')
            .trim()
}
