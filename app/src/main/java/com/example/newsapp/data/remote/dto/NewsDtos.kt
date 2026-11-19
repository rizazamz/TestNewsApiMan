package com.example.newsapp.data.remote.dto

import kotlinx.serialization.Serializable

interface NewsApiResponse {
    val status: String
    val code: String?
    val message: String?
}

@Serializable
data class NewsSourcesResponseDto(
    override val status: String,
    override val code: String? = null,
    override val message: String? = null,
    val sources: List<NewsSourceDto> = emptyList(),
) : NewsApiResponse

@Serializable
data class NewsArticlesResponseDto(
    override val status: String,
    override val code: String? = null,
    override val message: String? = null,
    val totalResults: Int = 0,
    val articles: List<NewsArticleDto> = emptyList(),
) : NewsApiResponse

@Serializable
data class NewsErrorDto(
    val status: String,
    val code: String? = null,
    val message: String? = null,
)

@Serializable
data class NewsSourceDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val url: String,
    val category: String,
    val language: String,
    val country: String,
)

@Serializable
data class ArticleSourceDto(
    val id: String? = null,
    val name: String,
)

@Serializable
data class NewsArticleDto(
    val source: ArticleSourceDto,
    val author: String? = null,
    val title: String,
    val description: String? = null,
    val url: String,
    val urlToImage: String? = null,
    val publishedAt: String,
    val content: String? = null,
)
