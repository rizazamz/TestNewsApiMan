package com.example.newsapp.data

import com.example.newsapp.BuildConfig
import com.example.newsapp.data.remote.NewsAuthInterceptor
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class NewsAuthInterceptorTest {
    @Test
    fun `API key is sent only to HTTPS NewsAPI v2 endpoints`() {
        for (url in listOf(
            "https://newsapi.org/v2/everything",
            "https://newsapi.org/v2/top-headlines/sources",
            "https://example.com/v2/everything",
            "http://newsapi.org/v2/everything",
            "https://newsapi.org/other",
            "https://newsapi.org:8443/v2/everything",
        )) {
            val chain = mockk<Interceptor.Chain>()
            val sent = slot<Request>()
            val response = mockk<Response>()
            every { chain.request() } returns Request.Builder().url(url).build()
            every { chain.proceed(capture(sent)) } returns response
            assertSame(response, NewsAuthInterceptor().intercept(chain))
            val trusted = url.startsWith("https://newsapi.org/v2/")
            assertEquals(if (trusted) BuildConfig.NEWS_API_KEY else null, sent.captured.header("X-Api-Key"))
        }
    }
}
