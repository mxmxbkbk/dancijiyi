package com.dancijiyi.data.exporter

import com.dancijiyi.data.Word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordExportFormatterTest {

    @Test
    fun csvContainsBomHeaderAndEscapedFields() {
        val csv = WordExportFormatter.buildCsv(
            listOf(
                Word(
                    word = "abandon",
                    meaning = "v. 放弃, \"抛弃\"",
                ),
            ),
        )

        assertTrue(csv.startsWith("\uFEFFword,meaning\r\n"))
        assertTrue(csv.contains("abandon,\"v. 放弃, \"\"抛弃\"\"\""))
        assertTrue(csv.endsWith("\r\n"))
    }

    @Test
    fun csvWithPlainMeaningDoesNotAddQuotes() {
        val csv = WordExportFormatter.buildCsv(
            listOf(Word(word = "focus", meaning = "n. 焦点")),
        )

        assertTrue(csv.contains("focus,n. 焦点\r\n"))
    }

    @Test
    fun txtUsesTabAndStripsLineBreaks() {
        val txt = WordExportFormatter.buildTxt(
            listOf(
                Word(
                    word = "challenge",
                    meaning = "n. 挑战\n第二个释义\t补充",
                ),
            ),
        )

        assertEquals("challenge\tn. 挑战 第二个释义 补充\n", txt)
    }
}
