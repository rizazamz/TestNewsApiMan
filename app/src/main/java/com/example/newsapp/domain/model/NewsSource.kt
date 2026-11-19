package com.example.newsapp.domain.model

data class NewsSource(
    val id: String,
    val name: String,
    val description: String?,
    val url: String,
    val category: NewsCategory,
    val language: String,
    val country: String,
)
