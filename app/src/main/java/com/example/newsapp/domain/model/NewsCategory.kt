package com.example.newsapp.domain.model

enum class NewsCategory(val apiValue: String) {
    BUSINESS("business"),
    ENTERTAINMENT("entertainment"),
    GENERAL("general"),
    HEALTH("health"),
    SCIENCE("science"),
    SPORTS("sports"),
    TECHNOLOGY("technology");

    companion object {
        fun fromApiValue(value: String): NewsCategory =
            entries.firstOrNull { it.apiValue == value }
                ?: throw IllegalArgumentException("Unknown NewsAPI category: $value")
    }
}
