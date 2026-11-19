package com.example.newsapp.presentation.articles

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale

fun formatPublishedDate(value: String, locale: Locale = Locale.getDefault()): String {
    for (pattern in listOf("yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")) {
        val parser = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
        val position = ParsePosition(0)
        val date = parser.parse(value, position)
        if (date != null && position.index == value.length) {
            return SimpleDateFormat("dd MMM yyyy, HH:mm", locale).format(date)
        }
    }
    return value
}
