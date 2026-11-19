package com.example.newsapp.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.newsapp.data.mapper.toDomain
import com.example.newsapp.data.remote.NewsApiResponseHandler
import com.example.newsapp.data.remote.NewsApiService
import com.example.newsapp.domain.model.NewsCategory
import com.example.newsapp.domain.model.NewsSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NewsSourcesPagingSource(
    private val service: NewsApiService,
    private val responseHandler: NewsApiResponseHandler,
    private val category: NewsCategory,
    query: String,
) : PagingSource<Int, NewsSource>() {
    private val searchQuery = query.trim()
    private val mutex = Mutex()
    private var sources: List<NewsSource>? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, NewsSource> = try {
        val filteredSources = mutex.withLock {
            sources ?: responseHandler.bodyOrThrow(service.getSources(category.apiValue))
                .sources.map { it.toDomain() }
                .filter {
                    searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true) ||
                        it.description?.contains(searchQuery, ignoreCase = true) == true
                }.also { sources = it }
        }
        val boundary = (params.key ?: 0).coerceIn(0, filteredSources.size)
        val start = if (params is LoadParams.Prepend) {
            (boundary - params.loadSize).coerceAtLeast(0)
        } else {
            boundary
        }
        val end = if (params is LoadParams.Prepend) boundary else {
            (start + params.loadSize).coerceAtMost(filteredSources.size)
        }
        LoadResult.Page(
            data = filteredSources.subList(start, end),
            prevKey = start.takeIf { it > 0 },
            nextKey = end.takeIf { it < filteredSources.size },
            itemsBefore = start,
            itemsAfter = filteredSources.size - end,
        )
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        LoadResult.Error(error)
    }

    override fun getRefreshKey(state: PagingState<Int, NewsSource>): Int? {
        val anchor = state.anchorPosition ?: return null
        val item = state.closestItemToPosition(anchor) ?: return null
        val index = sources?.indexOf(item)?.takeIf { it >= 0 } ?: return null
        return (index - state.config.initialLoadSize / 2).coerceAtLeast(0)
    }

    companion object {
        const val PAGE_SIZE = 15
    }
}
