package com.dancijiyi.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

/**
 * P1 桌面小组件骨架：展示一张示例单词卡片。
 *
 * 后续由 app 模块通过共享数据源（ContentProvider / DataStore）推送今日单词，
 * 本模块保持独立，不直接依赖 app 的 Room 实现。
 */
class WordWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_word).apply {
                setTextViewText(R.id.widget_word_text, context.getString(R.string.widget_sample_word))
                setTextViewText(R.id.widget_word_meaning, context.getString(R.string.widget_sample_meaning))
                setTextViewText(R.id.widget_caption, context.getString(R.string.widget_caption))
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
