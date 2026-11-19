package com.example.newsapp.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.newsapp.data.mapper.toDomain
import com.example.newsapp.data.remote.NewsApiResponseHandler
import com.example.newsapp.data.remote.NewsApiService
import com.example.newsapp.domain.model.NewsArticle
import kotlinx.coroutines.CancellationException

class NewsArticlesPagingSource(
    private val service: NewsApiService,
    private val responseHandler: NewsApiResponseHandler,
    sources: String?,
    query: String,
) : PagingSource<Int, NewsArticle>() {
    private val sourceIds = sources?.trim()?.takeIf { it.isNotEmpty() }
    private val searchQuery = query.trim().takeIf { it.isNotEmpty() }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, NewsArticle> {
        val page = params.key ?: 1
        if (page !in 1..MAX_RESULTS / PAGE_SIZE) {
            return LoadResult.Page(emptyList(), prevKey = null, nextKey = null)
        }
        return try {
            val response = responseHandler.bodyOrThrow(
                service.getArticles(sourceIds, searchQuery, page, PAGE_SIZE),
            )
            val total = response.totalResults.coerceIn(0, MAX_RESULTS)
            val offset = (page - 1) * PAGE_SIZE
            val articles = response.articles.take(minOf(PAGE_SIZE, (total - offset).coerceAtLeast(0)))
                .map { it.toDomain() }
            LoadResult.Page(
                data = articles,
                prevKey = (page - 1).takeIf { page > 1 },
                nextKey = (page + 1).takeIf {
                    articles.size == PAGE_SIZE && offset + articles.size < total
                },
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            LoadResult.Error(error)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, NewsArticle>): Int? =
        state.anchorPosition?.let { position ->
            state.closestPageToPosition(position)?.let { page ->
                page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
            }
        }

    companion object {
        const val PAGE_SIZE = 20
        const val MAX_RESULTS = 100
    }
}
