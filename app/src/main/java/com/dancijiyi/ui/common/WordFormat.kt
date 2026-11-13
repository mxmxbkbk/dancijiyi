package com.dancijiyi.ui.common

import android.content.Context
import com.dancijiyi.R
import com.dancijiyi.data.Word
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

fun formatTimestamp(epochMillis: Long): String =
    timeFormatter.withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(epochMillis))

fun stageLabel(context: Context, stage: Int): String = context.getString(
    when (stage) {
        Word.STAGE_NEW -> R.string.stage_new
        Word.STAGE_FAMILIAR -> R.string.stage_familiar
        Word.STAGE_SHORT_TERM -> R.string.stage_short_term
        Word.STAGE_MID_TERM -> R.string.stage_mid_term
        Word.STAGE_LONG_TERM -> R.string.stage_long_term
        Word.STAGE_MASTERED -> R.string.stage_mastered
        else -> R.string.stage_unknown
    },
)

fun dueLabel(context: Context, word: Word, now: Long = System.currentTimeMillis()): String =
    context.getString(
        when {
            word.mastered -> R.string.status_mastered
            word.reviewAt <= now -> R.string.status_due
            else -> R.string.status_scheduled
        },
    )
