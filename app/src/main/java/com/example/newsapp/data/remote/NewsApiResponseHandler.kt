package com.example.newsapp.data.remote

import com.example.newsapp.data.remote.dto.NewsApiResponse
import com.example.newsapp.data.remote.dto.NewsErrorDto
import com.example.newsapp.domain.NewsApiException
import javax.inject.Inject
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.Response

class NewsApiResponseHandler @Inject constructor(private val json: Json) {
    fun <T : NewsApiResponse> bodyOrThrow(response: Response<T>): T {
        if (!response.isSuccessful) {
            val error = response.errorBody()?.use { body ->
                try {
                    json.decodeFromString<NewsErrorDto>(body.string())
                } catch (_: SerializationException) {
                    null
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
            throw NewsApiException(
                code = error?.code,
                message = error?.message?.takeIf { it.isNotBlank() }
                    ?: "NewsAPI request failed (HTTP ${response.code()})",
                httpStatus = response.code(),
            )
        }
        val body = response.body() ?: throw NewsApiException(
            code = null,
            message = "NewsAPI returned an empty response",
            httpStatus = response.code(),
        )
        if (body.status != "ok") {
            throw NewsApiException(
                code = body.code,
                message = body.message?.takeIf { it.isNotBlank() }
                    ?: "NewsAPI returned status ${body.status}",
                httpStatus = response.code(),
            )
        }
        return body
    }
}
