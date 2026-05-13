package com.kirivsoft.commander.utils

import java.text.SimpleDateFormat
import java.util.*

object FileSizeFormatter {
    fun format(bytes: Long): String = when {
        bytes < 1024L                -> "$bytes Б"
        bytes < 1024L * 1024         -> "%.1f КБ".format(bytes / 1024f)
        bytes < 1024L * 1024 * 1024  -> "%.1f МБ".format(bytes / (1024f * 1024))
        else                         -> "%.2f ГБ".format(bytes / (1024f * 1024 * 1024))
    }
}

object FileDateFormatter {
    private val fmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("uk"))
    fun format(ms: Long): String = fmt.format(Date(ms))
}
