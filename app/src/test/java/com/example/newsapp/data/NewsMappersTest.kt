package com.example.newsapp.data

import com.example.newsapp.data.mapper.toDomain
import com.example.newsapp.data.remote.dto.NewsArticlesResponseDto
import com.example.newsapp.domain.model.NewsCategory
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NewsMappersTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `article decoding tolerates missing optional fields and preserves source without id`() {
        val response = json.decodeFromString<NewsArticlesResponseDto>(
            """{
                "status":"ok", "totalResults":1, "extra":true,
                "articles":[{
                    "source":{"id":null,"name":"Publisher"},
                    "title":"News", "url":"https://example.com/news",
                    "publishedAt":"2026-11-17T00:00:00Z", "urlToImage":null
                }]
            }""",
        )
        val article = response.articles.single().toDomain()
        assertNull(article.sourceId)
        assertNull(article.author)
        assertNull(article.description)
        assertNull(article.imageUrl)
        assertNull(article.content)
        assertEquals("Publisher", article.sourceName)
        assertEquals("https://example.com/news", article.url)
        assertEquals("2026-11-17T00:00:00Z", article.publishedAt)
    }

    @Test
    fun `categories cover exactly the seven API values`() {
        assertEquals(
            setOf("business", "entertainment", "general", "health", "science", "sports", "technology"),
            NewsCategory.entries.map { it.apiValue }.toSet(),
        )
        assertEquals(NewsCategory.TECHNOLOGY, NewsCategory.fromApiValue("technology"))
    }
}
