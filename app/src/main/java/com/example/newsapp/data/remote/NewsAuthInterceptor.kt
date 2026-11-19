package com.example.newsapp.data.remote

import com.example.newsapp.BuildConfig
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

class NewsAuthInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val authenticatedRequest = if (
            request.url.scheme == "https" && request.url.host == "newsapi.org" &&
            request.url.port == 443 && request.url.encodedPath.startsWith("/v2/")
        ) {
            request.newBuilder().header("X-Api-Key", BuildConfig.NEWS_API_KEY).build()
        } else {
            request
        }
        return chain.proceed(authenticatedRequest)
    }
}
