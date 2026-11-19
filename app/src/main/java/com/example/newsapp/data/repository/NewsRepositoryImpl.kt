package com.example.newsapp.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.newsapp.data.paging.NewsArticlesPagingSource
import com.example.newsapp.data.paging.NewsSourcesPagingSource
import com.example.newsapp.data.remote.NewsApiResponseHandler
import com.example.newsapp.data.remote.NewsApiService
import com.example.newsapp.domain.model.NewsArticle
import com.example.newsapp.domain.model.NewsCategory
import com.example.newsapp.domain.model.NewsSource
import com.example.newsapp.domain.repository.NewsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class NewsRepositoryImpl @Inject constructor(
    private val service: NewsApiService,
    private val responseHandler: NewsApiResponseHandler,
) : NewsRepository {
    override fun getSources(category: NewsCategory, query: String): Flow<PagingData<NewsSource>> =
        Pager(
            config = PagingConfig(
                pageSize = NewsSourcesPagingSource.PAGE_SIZE,
                initialLoadSize = NewsSourcesPagingSource.PAGE_SIZE,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = { NewsSourcesPagingSource(service, responseHandler, category, query) },
        ).flow

    override fun getArticles(sources: String?, query: String): Flow<PagingData<NewsArticle>> {
        require(!sources.isNullOrBlank() || query.isNotBlank()) {
            "Provide news sources or a search query"
        }
        return Pager(
            config = PagingConfig(
                pageSize = NewsArticlesPagingSource.PAGE_SIZE,
                initialLoadSize = NewsArticlesPagingSource.PAGE_SIZE,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = { NewsArticlesPagingSource(service, responseHandler, sources, query) },
        ).flow
    }
}
