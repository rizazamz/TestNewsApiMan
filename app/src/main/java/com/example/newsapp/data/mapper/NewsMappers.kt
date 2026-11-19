package com.example.newsapp.data.mapper

import com.example.newsapp.data.remote.dto.NewsArticleDto
import com.example.newsapp.data.remote.dto.NewsSourceDto
import com.example.newsapp.domain.model.NewsArticle
import com.example.newsapp.domain.model.NewsCategory
import com.example.newsapp.domain.model.NewsSource

fun NewsSourceDto.toDomain(): NewsSource = NewsSource(
    id = id,
    name = name,
    description = description,
    url = url,
    category = NewsCategory.fromApiValue(category),
    language = language,
    country = country,
)

fun NewsArticleDto.toDomain(): NewsArticle = NewsArticle(
    sourceId = source.id,
    sourceName = source.name,
    author = author,
    title = title,
    description = description,
    url = url,
    imageUrl = urlToImage,
    publishedAt = publishedAt,
    content = content,
)
