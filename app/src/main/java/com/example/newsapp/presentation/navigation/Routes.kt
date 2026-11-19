package com.example.newsapp.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
data object Categories

@Serializable
data class Sources(val category: String)

@Serializable
data class Articles(val sourceId: String, val sourceName: String)

@Serializable
data class ArticleDetail(val articleUrl: String)
