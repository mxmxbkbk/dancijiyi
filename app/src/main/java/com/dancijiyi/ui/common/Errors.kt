package com.dancijiyi.ui.common

/**
 * 把异常转换为可展示的中文提示，避免把堆栈直接抛给用户。
 */
fun Throwable.toUserMessage(prefix: String): String {
    val detail = message?.takeIf { it.isNotBlank() } ?: "未知错误"
    return "$prefix：$detail"
}
