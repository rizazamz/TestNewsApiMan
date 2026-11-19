package com.example.newsapp.data

import com.example.newsapp.data.remote.NewsApiResponseHandler
import com.example.newsapp.data.remote.dto.NewsSourcesResponseDto
import com.example.newsapp.domain.NewsApiException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class NewsApiResponseHandlerTest {
    private val handler = NewsApiResponseHandler(Json { ignoreUnknownKeys = true })

    @Test
    fun `http errors retain status code and message`() {
        for ((status, code) in listOf(401 to "apiKeyInvalid", 429 to "rateLimited", 426 to "maximumResultsReached")) {
            val response = Response.error<NewsSourcesResponseDto>(
                status,
                """{"status":"error","code":"$code","message":"API details","extra":true}"""
                    .toResponseBody("application/json".toMediaType()),
            )
            val error = assertThrows(NewsApiException::class.java) { handler.bodyOrThrow(response) }
            assertEquals(status, error.httpStatus)
            assertEquals(code, error.code)
            assertEquals("API details", error.message)
        }
    }

    @Test
    fun `status error in successful HTTP response is not treated as an empty result`() {
        val response = Response.success(NewsSourcesResponseDto("error", "apiKeyExhausted", "Quota exhausted"))
        val error = assertThrows(NewsApiException::class.java) { handler.bodyOrThrow(response) }
        assertEquals("apiKeyExhausted", error.code)
        assertEquals("Quota exhausted", error.message)
    }

    @Test
    fun `non JSON HTTP errors retain HTTP status and empty success is rejected`() {
        val response = Response.error<NewsSourcesResponseDto>(
            502, "<html>Bad gateway</html>".toResponseBody("text/html".toMediaType()),
        )
        val error = assertThrows(NewsApiException::class.java) { handler.bodyOrThrow(response) }
        assertEquals(502, error.httpStatus)
        assertNull(error.code)
        assertTrue(error.message.contains("502"))
        assertThrows(NewsApiException::class.java) {
            handler.bodyOrThrow(Response.success<NewsSourcesResponseDto>(null))
        }
    }
}
