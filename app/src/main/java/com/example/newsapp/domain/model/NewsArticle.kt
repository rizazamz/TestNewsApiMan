package com.example.newsapp.domain.model

data class NewsArticle(
    val sourceId: String?,
    val sourceName: String,
    val author: String?,
    val title: String,
    val description: String?,
    val url: String,
    val imageUrl: String?,
    val publishedAt: String,
    val content: String?,
)
