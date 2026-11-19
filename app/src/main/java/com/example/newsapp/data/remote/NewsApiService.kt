package com.example.newsapp.data.remote

import com.example.newsapp.data.remote.dto.NewsArticlesResponseDto
import com.example.newsapp.data.remote.dto.NewsSourcesResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApiService {
    @GET("top-headlines/sources")
    suspend fun getSources(@Query("category") category: String): Response<NewsSourcesResponseDto>

    @GET("everything")
    suspend fun getArticles(
        @Query("sources") sources: String?,
        @Query("q") query: String?,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): Response<NewsArticlesResponseDto>
}
