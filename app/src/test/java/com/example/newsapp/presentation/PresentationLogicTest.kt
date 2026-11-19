package com.example.newsapp.presentation

import app.cash.turbine.test
import com.example.newsapp.R
import com.example.newsapp.domain.NewsApiException
import com.example.newsapp.presentation.articles.formatPublishedDate
import com.example.newsapp.presentation.common.debouncedSearch
import com.example.newsapp.presentation.common.newsErrorMessage
import com.example.newsapp.presentation.detail.isWebArticleUrl
import com.example.newsapp.presentation.navigation.ArticleDetail
import java.io.IOException
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PresentationLogicTest {
    @Test
    fun `search waits 400ms collapses typing and ignores equivalent trimmed queries`() = runTest {
        val query = MutableStateFlow("")
        query.debouncedSearch().test {
            advanceTimeBy(400)
            runCurrent()
            assertEquals("", awaitItem())
            query.value = "a"
            runCurrent()
            advanceTimeBy(200)
            query.value = "android"
            runCurrent()
            advanceTimeBy(399)
            expectNoEvents()
            advanceTimeBy(1)
            runCurrent()
            assertEquals("android", awaitItem())
            query.value = " android "
            runCurrent()
            advanceTimeBy(400)
            runCurrent()
            expectNoEvents()
            query.value = ""
            runCurrent()
            advanceTimeBy(400)
            runCurrent()
            assertEquals("", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `rate limits and credentials select localized error resources`() {
        assertEquals(R.string.error_rate_limit, newsErrorMessage(NewsApiException("rateLimited", "raw", 429)))
        assertEquals(R.string.error_rate_limit, newsErrorMessage(NewsApiException("apiKeyExhausted", "raw", 200)))
        assertEquals(R.string.error_api_key, newsErrorMessage(NewsApiException("apiKeyInvalid", "raw", 401)))
        assertEquals(R.string.error_network, newsErrorMessage(IOException()))
    }

    @Test
    fun `publication date supports ISO seconds milliseconds and timezone offsets`() {
        val previous = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            assertEquals("17 Nov 2026, 10:30", formatPublishedDate("2026-11-17T10:30:00Z", Locale.US))
            assertEquals("17 Nov 2026, 10:30", formatPublishedDate("2026-11-17T10:30:00.123Z", Locale.US))
            assertEquals("17 Nov 2026, 03:30", formatPublishedDate("2026-11-17T10:30:00+07:00", Locale.US))
            assertEquals("invalid", formatPublishedDate("invalid", Locale.US))
        } finally {
            TimeZone.setDefault(previous)
        }
    }

    @Test
    fun `article routes retain complete URLs and browser rejects non web schemes`() {
        val route = ArticleDetail("https://example.com/read?a=1&b=two#section")
        assertEquals(route, Json.decodeFromString<ArticleDetail>(Json.encodeToString(ArticleDetail.serializer(), route)))
        assertTrue(isWebArticleUrl(route.articleUrl))
        for (url in listOf("javascript:alert(1)", "file:///private", "intent://app", "https://", "invalid url")) {
            assertFalse(isWebArticleUrl(url))
        }
    }
}
