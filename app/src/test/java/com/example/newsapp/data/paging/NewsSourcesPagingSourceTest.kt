package com.example.newsapp.data.paging

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.newsapp.data.remote.NewsApiService
import com.example.newsapp.data.remote.dto.NewsSourcesResponseDto
import com.example.newsapp.domain.NewsApiException
import com.example.newsapp.domain.model.NewsCategory
import com.example.newsapp.support.apiErrorBody
import com.example.newsapp.support.responseHandler
import com.example.newsapp.support.sourceDomain
import com.example.newsapp.support.sourceDto
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

class NewsSourcesPagingSourceTest {
    private val service = mockk<NewsApiService>()
    private fun paging(query: String = "") =
        NewsSourcesPagingSource(service, responseHandler, NewsCategory.TECHNOLOGY, query)
    private fun refresh(key: Int? = null, size: Int = 15) =
        PagingSource.LoadParams.Refresh(key, size, false)

    @Test
    fun `first page and local append preserve order and stop without another network call`() = runTest {
        coEvery { service.getSources("technology") } returns Response.success(
            NewsSourcesResponseDto("ok", sources = (0 until 32).map { sourceDto(it) }),
        )
        val source = paging()
        val first = source.load(refresh()) as PagingSource.LoadResult.Page
        val second = source.load(PagingSource.LoadParams.Append(first.nextKey!!, 15, false)) as PagingSource.LoadResult.Page
        val last = source.load(PagingSource.LoadParams.Append(second.nextKey!!, 15, false)) as PagingSource.LoadResult.Page
        assertEquals((0 until 15).map { sourceDomain(it) }, first.data)
        assertEquals((15 until 30).map { sourceDomain(it) }, second.data)
        assertEquals(listOf(sourceDomain(30), sourceDomain(31)), last.data)
        assertNull(first.prevKey)
        assertEquals(15, first.nextKey)
        assertNull(last.nextKey)
        assertEquals(30, last.itemsBefore)
        assertEquals(0, last.itemsAfter)
        coVerify(exactly = 1) { service.getSources("technology") }
    }

    @Test
    fun `query trims whitespace and matches name or description ignoring case`() = runTest {
        coEvery { service.getSources("technology") } returns Response.success(
            NewsSourcesResponseDto("ok", sources = listOf(
                sourceDto(1, name = "ANDROID Daily", description = null),
                sourceDto(2, description = "The Android ecosystem"),
                sourceDto(3, description = null),
            )),
        )
        val page = paging(" android ").load(refresh()) as PagingSource.LoadResult.Page
        assertEquals(listOf("1", "2"), page.data.map { it.id })
        assertNull(page.nextKey)
    }

    @Test
    fun `empty API response and unmatched query both terminate`() = runTest {
        for (sources in listOf(emptyList(), listOf(sourceDto(1)))) {
            coEvery { service.getSources("technology") } returns Response.success(
                NewsSourcesResponseDto("ok", sources = sources),
            )
            val page = paging("unmatched").load(refresh()) as PagingSource.LoadResult.Page
            assertTrue(page.data.isEmpty())
            assertNull(page.prevKey)
            assertNull(page.nextKey)
        }
    }

    @Test
    fun `network exception is exposed and retry fetches again`() = runTest {
        val failure = IOException("offline")
        coEvery { service.getSources("technology") } throws failure andThen
            Response.success(NewsSourcesResponseDto("ok", sources = listOf(sourceDto(1))))
        val source = paging()
        assertSame(failure, (source.load(refresh()) as PagingSource.LoadResult.Error).throwable)
        assertEquals(listOf(sourceDomain(1)), (source.load(refresh()) as PagingSource.LoadResult.Page).data)
        coVerify(exactly = 2) { service.getSources("technology") }
    }

    @Test
    fun `HTTP exceptions 401 426 and 429 remain load errors`() = runTest {
        for (status in listOf(401, 426, 429)) {
            val error = HttpException(Response.error<Any>(status, apiErrorBody("http")))
            coEvery { service.getSources("technology") } throws error
            val result = paging().load(refresh()) as PagingSource.LoadResult.Error
            assertSame(error, result.throwable)
            assertEquals(status, (result.throwable as HttpException).code())
        }
    }

    @Test
    fun `HTTP error responses preserve NewsAPI code status and message`() = runTest {
        for ((status, code) in listOf(401 to "apiKeyInvalid", 426 to "maximumResultsReached", 429 to "rateLimited")) {
            coEvery { service.getSources("technology") } returns Response.error(status, apiErrorBody(code))
            val result = paging().load(refresh()) as PagingSource.LoadResult.Error
            val error = result.throwable as NewsApiException
            assertEquals(status, error.httpStatus)
            assertEquals(code, error.code)
            assertEquals("Request rejected", error.message)
        }
    }

    @Test
    fun `successful HTTP response with error status is not empty success`() = runTest {
        coEvery { service.getSources("technology") } returns Response.success(
            NewsSourcesResponseDto("error", "apiKeyExhausted", "Quota exhausted"),
        )
        val error = (paging().load(refresh()) as PagingSource.LoadResult.Error).throwable as NewsApiException
        assertEquals("apiKeyExhausted", error.code)
    }

    @Test
    fun `variable refresh size prepends without gaps and refresh key follows anchor`() = runTest {
        coEvery { service.getSources("technology") } returns Response.success(
            NewsSourcesResponseDto("ok", sources = (0 until 50).map { sourceDto(it) }),
        )
        val source = paging()
        val middle = source.load(refresh(23, 20)) as PagingSource.LoadResult.Page
        val before = source.load(PagingSource.LoadParams.Prepend(middle.prevKey!!, 15, false)) as PagingSource.LoadResult.Page
        val after = source.load(PagingSource.LoadParams.Append(middle.nextKey!!, 15, false)) as PagingSource.LoadResult.Page
        assertEquals((8 until 23).map { "$it" }, before.data.map { it.id })
        assertEquals((23 until 43).map { "$it" }, middle.data.map { it.id })
        assertEquals((43 until 50).map { "$it" }, after.data.map { it.id })
        assertNull(after.nextKey)
        val config = PagingConfig(15, initialLoadSize = 15)
        assertEquals(21, source.getRefreshKey(PagingState(listOf(middle), 5, config, 0)))
        assertNull(source.getRefreshKey(PagingState(listOf(middle), null, config, 0)))
    }

    @Test
    fun `cancellation propagates instead of becoming retryable error`() = runTest {
        coEvery { service.getSources("technology") } throws CancellationException("cancelled")
        try {
            paging().load(refresh())
            fail("Expected cancellation")
        } catch (error: CancellationException) {
            assertEquals("cancelled", error.message)
        }
    }
}
