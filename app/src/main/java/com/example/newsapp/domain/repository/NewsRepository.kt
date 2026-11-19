package com.example.newsapp.domain.repository

import androidx.paging.PagingData
import com.example.newsapp.domain.model.NewsArticle
import com.example.newsapp.domain.model.NewsCategory
import com.example.newsapp.domain.model.NewsSource
import kotlinx.coroutines.flow.Flow

interface NewsRepository {
    fun getSources(category: NewsCategory, query: String = ""): Flow<PagingData<NewsSource>>

    fun getArticles(sources: String? = null, query: String = ""): Flow<PagingData<NewsArticle>>
}
