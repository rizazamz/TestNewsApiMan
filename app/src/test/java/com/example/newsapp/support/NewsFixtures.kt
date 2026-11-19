package com.example.newsapp.support

import com.example.newsapp.data.remote.NewsApiResponseHandler
import com.example.newsapp.data.remote.dto.ArticleSourceDto
import com.example.newsapp.data.remote.dto.NewsArticleDto
import com.example.newsapp.data.remote.dto.NewsSourceDto
import com.example.newsapp.domain.model.NewsArticle
import com.example.newsapp.domain.model.NewsCategory
import com.example.newsapp.domain.model.NewsSource
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody

val responseHandler = NewsApiResponseHandler(Json { ignoreUnknownKeys = true })

fun sourceDto(index: Int, name: String = "Publisher $index", description: String? = "Description $index") =
    NewsSourceDto("$index", name, description, "https://example.com/$index", "technology", "en", "us")

fun articleDto(index: Int) = NewsArticleDto(
    source = ArticleSourceDto("publisher", "Publisher"),
    author = "Author $index",
    title = "Article $index",
    description = "Description $index",
    url = "https://example.com/articles/$index",
    urlToImage = "https://example.com/images/$index.jpg",
    publishedAt = "2026-11-17T10:30:00Z",
    content = "Content $index",
)

fun sourceDomain(index: Int) = NewsSource(
    "$index", "Publisher $index", "Description $index", "https://example.com/$index",
    NewsCategory.TECHNOLOGY, "en", "us",
)

fun articleDomain(index: Int) = NewsArticle(
    "publisher", "Publisher", "Author $index", "Article $index", "Description $index",
    "https://example.com/articles/$index", "https://example.com/images/$index.jpg",
    "2026-11-17T10:30:00Z", "Content $index",
)

fun apiErrorBody(code: String) =
    """{"status":"error","code":"$code","message":"Request rejected"}"""
        .toResponseBody("application/json".toMediaType())
