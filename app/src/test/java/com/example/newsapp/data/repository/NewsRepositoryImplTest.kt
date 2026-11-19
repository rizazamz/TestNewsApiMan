package com.example.newsapp.data.repository

import androidx.paging.testing.asSnapshot
import com.example.newsapp.data.remote.NewsApiService
import com.example.newsapp.data.remote.dto.NewsArticlesResponseDto
import com.example.newsapp.data.remote.dto.NewsSourcesResponseDto
import com.example.newsapp.domain.model.NewsCategory
import com.example.newsapp.support.PagingTestRule
import com.example.newsapp.support.articleDomain
import com.example.newsapp.support.articleDto
import com.example.newsapp.support.responseHandler
import com.example.newsapp.support.sourceDomain
import com.example.newsapp.support.sourceDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

class NewsRepositoryImplTest {
    @get:Rule val main = PagingTestRule()
    private val service = mockk<NewsApiService>()
    private val repository = NewsRepositoryImpl(service, responseHandler)

    @Test
    fun `sources map every DTO field through repository pager`() = runTest {
        coEvery { service.getSources("technology") } returns Response.success(
            NewsSourcesResponseDto("ok", sources = listOf(sourceDto(1))),
        )
        val snapshot = repository.getSources(NewsCategory.TECHNOLOGY).asSnapshot()
        assertEquals(listOf(sourceDomain(1)), snapshot)
    }

    @Test
    fun `articles map every DTO field through repository pager`() = runTest {
        coEvery { service.getArticles("publisher", null, 1, 20) } returns Response.success(
            NewsArticlesResponseDto("ok", totalResults = 1, articles = listOf(articleDto(1))),
        )
        assertEquals(listOf(articleDomain(1)), repository.getArticles("publisher").asSnapshot())
    }

    @Test
    fun `optional source and article fields remain null without fabricated values`() = runTest {
        coEvery { service.getSources("technology") } returns Response.success(
            NewsSourcesResponseDto("ok", sources = listOf(sourceDto(1, description = null))),
        )
        coEvery { service.getArticles("publisher", null, 1, 20) } returns Response.success(
            NewsArticlesResponseDto("ok", totalResults = 1, articles = listOf(
                articleDto(1).copy(
                    source = articleDto(1).source.copy(id = null), author = null,
                    description = null, urlToImage = null, content = null,
                ),
            )),
        )
        assertEquals(
            listOf(sourceDomain(1).copy(description = null)),
            repository.getSources(NewsCategory.TECHNOLOGY).asSnapshot(),
        )
        assertEquals(
            listOf(articleDomain(1).copy(sourceId = null, author = null, description = null, imageUrl = null, content = null)),
            repository.getArticles("publisher").asSnapshot(),
        )
    }

    @Test
    fun `repository config loads all source slices with one API request`() = runTest {
        coEvery { service.getSources("technology") } returns Response.success(
            NewsSourcesResponseDto("ok", sources = (0 until 36).map { sourceDto(it) }),
        )
        val snapshot = repository.getSources(NewsCategory.TECHNOLOGY).asSnapshot { scrollTo(35) }
        assertEquals((0 until 36).map { sourceDomain(it) }, snapshot)
        coVerify(exactly = 1) { service.getSources("technology") }
    }

    @Test
    fun `repository article paging stops at developer cap even for larger totalResults`() = runTest {
        coEvery { service.getArticles("publisher", null, any(), 20) } answers {
            val offset = (thirdArg<Int>() - 1) * 20
            Response.success(NewsArticlesResponseDto("ok", totalResults = 800, articles = (offset until offset + 20).map { articleDto(it) }))
        }
        val snapshot = repository.getArticles("publisher").asSnapshot { appendScrollWhile { true } }
        assertEquals((0 until 100).map { articleDomain(it) }, snapshot)
        coVerify(exactly = 5) { service.getArticles("publisher", null, any(), 20) }
    }

    @Test
    fun `query-only search is allowed and empty request is rejected before network`() = runTest {
        coEvery { service.getArticles(null, "android", 1, 20) } returns Response.success(
            NewsArticlesResponseDto("ok", totalResults = 0),
        )
        assertTrue(repository.getArticles(query = "android").asSnapshot().isEmpty())
        assertThrows(IllegalArgumentException::class.java) { repository.getArticles(null, "  ") }
        coVerify(exactly = 1) { service.getArticles(any(), any(), any(), any()) }
    }

    @Test
    fun `category and local query are honored by repository`() = runTest {
        coEvery { service.getSources("health") } returns Response.success(
            NewsSourcesResponseDto("ok", sources = listOf(
                sourceDto(1, name = "Medical Daily").copy(category = "health"),
                sourceDto(2).copy(category = "health"),
            )),
        )
        val snapshot = repository.getSources(NewsCategory.HEALTH, "medical").asSnapshot()
        assertEquals(listOf(sourceDomain(1).copy(name = "Medical Daily", category = NewsCategory.HEALTH)), snapshot)
    }
}
