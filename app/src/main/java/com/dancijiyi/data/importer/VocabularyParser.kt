package com.dancijiyi.data.importer

import java.util.Locale

data class ParsedWord(
    val word: String,
    val meaning: String,
)

data class ParseIssue(
    val line: Int,
    val reason: String,
)

data class ParseResult(
    val entries: List<ParsedWord>,
    val issues: List<ParseIssue>,
    val duplicateCount: Int,
    val totalLines: Int,
)

/**
 * 轻量词库解析器：
 * - 一行一条「单词,释义」；
 * - 支持 CSV 双引号字段与字段内逗号；
 * - 支持 txt 使用 Tab 分隔；
 * - 自动跳过表头、空行与 # 注释；
 * - 文件内单词按大小写不敏感去重。
 */
object VocabularyParser {

    private val HEADER_WORDS = setOf("word", "单词", "词汇")
    private val HEADER_MEANINGS = setOf("meaning", "释义", "翻译")

    fun parse(raw: String): ParseResult {
        val text = raw
            .removePrefix("\uFEFF")
            .replace("\r\n", "\n")
            .replace('\r', '\n')

        val entries = mutableListOf<ParsedWord>()
        val issues = mutableListOf<ParseIssue>()
        val seenWords = mutableSetOf<String>()

        var duplicateCount = 0
        var totalLines = 0
        var firstRecord = true

        text.lineSequence().forEachIndexed { index, rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed

            totalLines++
            val lineNumber = index + 1
            val fields = splitLine(line)
            if (fields.size < 2) {
                issues += ParseIssue(lineNumber, "缺少释义，应为「单词,释义」")
                return@forEachIndexed
            }

            val word = fields[0].trim()
            val meaning = fields.drop(1).joinToString(",").trim()

            if (firstRecord) {
                firstRecord = false
                if (isHeader(word, meaning)) return@forEachIndexed
            }

            if (word.isEmpty()) {
                issues += ParseIssue(lineNumber, "单词为空")
                return@forEachIndexed
            }
            if (meaning.isEmpty()) {
                issues += ParseIssue(lineNumber, "释义为空")
                return@forEachIndexed
            }

            val key = word.lowercase(Locale.ROOT)
            if (!seenWords.add(key)) {
                duplicateCount++
                return@forEachIndexed
            }

            entries += ParsedWord(word = word, meaning = meaning)
        }

        return ParseResult(
            entries = entries,
            issues = issues,
            duplicateCount = duplicateCount,
            totalLines = totalLines,
        )
    }

    private fun isHeader(word: String, meaning: String): Boolean =
        word.lowercase(Locale.ROOT) in HEADER_WORDS &&
            meaning.lowercase(Locale.ROOT) in HEADER_MEANINGS

    private fun splitLine(line: String): List<String> {
        if (!line.contains(',') && line.contains('\t')) {
            return line.split('\t', limit = 2)
        }

        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var index = 0

        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' && inQuotes && index + 1 < line.length && line[index + 1] == '"' -> {
                    current.append('"')
                    index++
                }

                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    fields += current.toString()
                    current.clear()
                }

                else -> current.append(char)
            }
            index++
        }
        fields += current.toString()
        return fields
    }
}
