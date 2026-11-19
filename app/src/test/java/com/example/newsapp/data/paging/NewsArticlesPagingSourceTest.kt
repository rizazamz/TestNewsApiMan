package com.example.newsapp.data.paging

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.newsapp.data.remote.NewsApiService
import com.example.newsapp.data.remote.dto.NewsArticlesResponseDto
import com.example.newsapp.domain.NewsApiException
import com.example.newsapp.support.apiErrorBody
import com.example.newsapp.support.articleDomain
import com.example.newsapp.support.articleDto
import com.example.newsapp.support.responseHandler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class NewsArticlesPagingSourceTest {
    private val service = mockk<NewsApiService>()
    private fun paging(query: String = "") = NewsArticlesPagingSource(service, responseHandler, "publisher", query)
    private fun refresh(size: Int = 20) = PagingSource.LoadParams.Refresh<Int>(null, size, false)

    @Test
    fun `first and next server pages use twenty items and terminate at totalResults`() = runTest {
        coEvery { service.getArticles("publisher", null, 1, 20) } returns Response.success(
            NewsArticlesResponseDto("ok", totalResults = 23, articles = (0 until 20).map { articleDto(it) }),
        )
        coEvery { service.getArticles("publisher", null, 2, 20) } returns Response.success(
            NewsArticlesResponseDto("ok", totalResults = 23, articles = (20 until 23).map { articleDto(it) }),
        )
        val source = paging()
        val first = source.load(refresh(60)) as PagingSource.LoadResult.Page
        val last = source.load(PagingSource.LoadParams.Append(first.nextKey!!, 20, false)) as PagingSource.LoadResult.Page
        assertEquals((0 until 20).map { articleDomain(it) }, first.data)
        assertNull(first.prevKey)
        assertEquals(2, first.nextKey)
        assertEquals((20 until 23).map { articleDomain(it) }, last.data)
        assertEquals(1, last.prevKey)
        assertNull(last.nextKey)
        coVerify(exactly = 1) { service.getArticles("publisher", null, 1, 20) }
        coVerify(exactly = 1) { service.getArticles("publisher", null, 2, 20) }
    }

    @Test
    fun `query is trimmed and forwarded to server with selected source`() = runTest {
        coEvery { service.getArticles("publisher", "android", 1, 20) } returns Response.success(
            NewsArticlesResponseDto("ok", totalResults = 1, articles = listOf(articleDto(1))),
        )
        val page = paging(" android ").load(refresh()) as PagingSource.LoadResult.Page
        assertEquals(listOf(articleDomain(1)), page.data)
        assertNull(page.nextKey)
        coVerify(exactly = 1) { service.getArticles("publisher", "android", 1, 20) }
    }

    @Test
    fun `developer plan never fetches beyond one hundred results`() = runTest {
        coEvery { service.getArticles("publisher", null, any(), 20) } answers {
            val offset = (thirdArg<Int>() - 1) * 20
            Response.success(NewsArticlesResponseDto("ok", totalResults = 1000, articles = (offset until offset + 20).map { articleDto(it) }))
        }
        val source = paging()
        val all = (1..5).flatMap { page ->
            val result = source.load(PagingSource.LoadParams.Append(page, 20, false)) as PagingSource.LoadResult.Page
            assertEquals(if (page == 5) null else page + 1, result.nextKey)
            result.data
        }
        assertEquals(100, all.size)
        assertEquals(100, all.map { it.url }.distinct().size)
        val beyond = source.load(PagingSource.LoadParams.Append(6, 20, false)) as PagingSource.LoadResult.Page
        assertTrue(beyond.data.isEmpty())
        assertNull(beyond.nextKey)
        coVerify(exactly = 0) { service.getArticles(any(), any(), 6, any()) }
    }

    @Test
    fun `empty and short server pages stop pagination`() = runTest {
        for (count in listOf(0, 7)) {
            coEvery { service.getArticles("publisher", null, 1, 20) } returns Response.success(
                NewsArticlesResponseDto("ok", totalResults = 80, articles = (0 until count).map { articleDto(it) }),
            )
            val page = paging().load(refresh()) as PagingSource.LoadResult.Page
            assertEquals(count, page.data.size)
            assertNull(page.nextKey)
        }
    }

    @Test
    fun `network exception remains load error and retry succeeds`() = runTest {
        val error = IOException("offline")
        coEvery { service.getArticles("publisher", null, 1, 20) } throws error andThen
            Response.success(NewsArticlesResponseDto("ok", totalResults = 1, articles = listOf(articleDto(1))))
        val source = paging()
        assertSame(error, (source.load(refresh()) as PagingSource.LoadResult.Error).throwable)
        assertEquals(listOf(articleDomain(1)), (source.load(refresh()) as PagingSource.LoadResult.Page).data)
    }

    @Test
    fun `HTTP exceptions 401 426 and 429 remain load errors`() = runTest {
        for (status in listOf(401, 426, 429)) {
            val error = HttpException(Response.error<Any>(status, apiErrorBody("http")))
            coEvery { service.getArticles("publisher", null, 1, 20) } throws error
            val result = paging().load(refresh()) as PagingSource.LoadResult.Error
            assertSame(error, result.throwable)
            assertEquals(status, (result.throwable as HttpException).code())
        }
    }

    @Test
    fun `HTTP error responses preserve API details`() = runTest {
        for ((status, code) in listOf(401 to "apiKeyInvalid", 426 to "maximumResultsReached", 429 to "rateLimited")) {
            coEvery { service.getArticles("publisher", null, 1, 20) } returns Response.error(status, apiErrorBody(code))
            val error = (paging().load(refresh()) as PagingSource.LoadResult.Error).throwable as NewsApiException
            assertEquals(status, error.httpStatus)
            assertEquals(code, error.code)
            assertEquals("Request rejected", error.message)
        }
    }

    @Test
    fun `refresh key preserves current server page and handles missing anchor`() = runTest {
        coEvery { service.getArticles("publisher", null, 3, 20) } returns Response.success(
            NewsArticlesResponseDto("ok", totalResults = 100, articles = (40 until 60).map { articleDto(it) }),
        )
        val source = paging()
        val page = source.load(PagingSource.LoadParams.Refresh(3, 20, false)) as PagingSource.LoadResult.Page
        val config = PagingConfig(20)
        assertEquals(3, source.getRefreshKey(PagingState(listOf(page), 4, config, 0)))
        assertNull(source.getRefreshKey(PagingState(listOf(page), null, config, 0)))
    }

    @Test
    fun `cancellation propagates to caller`() = runTest {
        coEvery { service.getArticles("publisher", null, 1, 20) } throws CancellationException("cancelled")
        try {
            paging().load(refresh())
            fail("Expected cancellation")
        } catch (error: CancellationException) {
            assertEquals("cancelled", error.message)
        }
    }
}
