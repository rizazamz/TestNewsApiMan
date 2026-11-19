package com.example.newsapp.presentation.articles

import androidx.lifecycle.SavedStateHandle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import app.cash.turbine.test
import com.example.newsapp.R
import com.example.newsapp.domain.NewsApiException
import com.example.newsapp.domain.model.NewsArticle
import com.example.newsapp.domain.repository.NewsRepository
import com.example.newsapp.presentation.common.newsErrorMessage
import com.example.newsapp.support.PagingTestRule
import com.example.newsapp.support.articleDomain
import com.example.newsapp.support.awaitRefresh
import com.example.newsapp.support.pagingFlow
import com.example.newsapp.support.present
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArticlesViewModelTest {
    @get:Rule val main = PagingTestRule()
    private val repository = mockk<NewsRepository>()
    private fun handle(query: String = "") = SavedStateHandle(
        mapOf("sourceId" to "publisher", "sourceName" to "Publisher") + ("articles_query" to query),
    )
    private fun viewModel(state: SavedStateHandle = handle()) =
        main.retain(ArticlesViewModel(state, repository))

    @Test
    fun `route arguments and saved query are restored before first load`() = runTest {
        every { repository.getArticles("publisher", "saved") } returns pagingFlow { listOf(articleDomain(1)) }
        val viewModel = viewModel(handle("saved"))
        assertEquals("Publisher", viewModel.sourceName)
        assertEquals("saved", viewModel.query.value)
        assertEquals(listOf(articleDomain(1)), viewModel.articles.asSnapshot())
        verify(exactly = 1) { repository.getArticles("publisher", "saved") }
    }

    @Test
    fun `query updates immediately but repository receives only debounced distinct value`() = runTest {
        every { repository.getArticles("publisher", any()) } returns flowOf(PagingData.empty())
        val state = handle()
        val viewModel = viewModel(state)
        viewModel.articles.test {
            advanceTimeBy(399)
            runCurrent()
            verify(exactly = 0) { repository.getArticles("publisher", any()) }
            advanceTimeBy(1)
            runCurrent()
            awaitItem()
            viewModel.query.test {
                assertEquals("", awaitItem())
                viewModel.onQueryChange("a")
                assertEquals("a", awaitItem())
                runCurrent()
                advanceTimeBy(200)
                viewModel.onQueryChange(" android ")
                assertEquals(" android ", awaitItem())
                runCurrent()
                advanceTimeBy(399)
                verify(exactly = 0) { repository.getArticles("publisher", "android") }
                advanceTimeBy(1)
                runCurrent()
                verify(exactly = 1) { repository.getArticles("publisher", "android") }
                assertEquals(" android ", state.get<String>("articles_query"))
                viewModel.onQueryChange("android")
                assertEquals("android", awaitItem())
                runCurrent()
                advanceTimeBy(400)
                runCurrent()
                verify(exactly = 1) { repository.getArticles("publisher", "android") }
                viewModel.onQueryChange("")
                assertEquals("", awaitItem())
                runCurrent()
                advanceTimeBy(400)
                runCurrent()
                verify(exactly = 2) { repository.getArticles("publisher", "") }
                cancelAndIgnoreRemainingEvents()
            }
            verify(exactly = 0) { repository.getArticles("publisher", "a") }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `presented UI transitions from loading to data when repository completes`() = runTest {
        val gate = CompletableDeferred<List<NewsArticle>>()
        every { repository.getArticles("publisher", "") } returns pagingFlow { gate.await() }
        val viewModel = viewModel()
        val presenter = present(viewModel.articles)
        presenter.loadStateFlow.filterNotNull().test {
            advanceTimeBy(400)
            runCurrent()
            assertTrue(awaitRefresh { it is LoadState.Loading } is LoadState.Loading)
            assertTrue(presenter.snapshot().items.isEmpty())
            gate.complete(listOf(articleDomain(1)))
            runCurrent()
            assertTrue(awaitRefresh { it is LoadState.NotLoading } is LoadState.NotLoading)
            assertEquals(listOf(articleDomain(1)), presenter.snapshot().items)
            assertTrue(presenter.events.isNotEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search without matches transitions from data to empty successful state`() = runTest {
        every { repository.getArticles("publisher", "") } returns pagingFlow { listOf(articleDomain(1)) }
        every { repository.getArticles("publisher", "missing") } returns pagingFlow { emptyList<NewsArticle>() }
        val viewModel = viewModel()
        val presenter = present(viewModel.articles)
        advanceTimeBy(400)
        runCurrent()
        assertEquals(listOf(articleDomain(1)), presenter.snapshot().items)
        viewModel.onQueryChange("missing")
        runCurrent()
        advanceTimeBy(400)
        runCurrent()
        assertTrue(presenter.snapshot().items.isEmpty())
        assertTrue(presenter.loadStateFlow.value?.refresh is LoadState.NotLoading)
        verify(exactly = 1) { repository.getArticles("publisher", "missing") }
    }

    @Test
    fun `retry changes error to loading then data without recreating repository query`() = runTest {
        for (failure in listOf(
            IOException("offline"),
            NewsApiException("apiKeyInvalid", "Invalid key", 401),
            NewsApiException("maximumResultsReached", "Plan limit", 426),
            NewsApiException("rateLimited", "Slow down", 429),
        )) {
            var attempts = 0
            val gate = CompletableDeferred<List<NewsArticle>>()
            every { repository.getArticles("publisher", "") } returns pagingFlow {
                attempts++
                if (attempts == 1) throw failure
                gate.await()
            }
            val viewModel = viewModel()
            val presenter = present(viewModel.articles)
            presenter.loadStateFlow.filterNotNull().test {
                advanceTimeBy(400)
                runCurrent()
                awaitRefresh { it is LoadState.Loading }
                val error = awaitRefresh { it is LoadState.Error } as LoadState.Error
                assertSame(failure, error.error)
                assertTrue(newsErrorMessage(error.error) != 0)
                if (failure is NewsApiException && failure.httpStatus == 429) {
                    assertEquals(R.string.error_rate_limit, newsErrorMessage(error.error))
                }
                presenter.retry()
                runCurrent()
                awaitRefresh { it is LoadState.Loading }
                gate.complete(listOf(articleDomain(2)))
                runCurrent()
                awaitRefresh { it is LoadState.NotLoading }
                assertEquals(2, attempts)
                assertEquals(listOf(articleDomain(2)), presenter.snapshot().items)
                cancelAndIgnoreRemainingEvents()
            }
        }
        verify(exactly = 4) { repository.getArticles("publisher", "") }
    }

    @Test
    fun `cached results survive a replacement collector without another repository call`() = runTest {
        every { repository.getArticles("publisher", "") } returns pagingFlow { listOf(articleDomain(1)) }
        val viewModel = viewModel()
        assertEquals(listOf(articleDomain(1)), viewModel.articles.asSnapshot())
        assertEquals(listOf(articleDomain(1)), viewModel.articles.asSnapshot())
        verify(exactly = 1) { repository.getArticles("publisher", "") }
    }
}
