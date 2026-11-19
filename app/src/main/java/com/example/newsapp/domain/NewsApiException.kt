package com.example.newsapp.domain

class NewsApiException(
    val code: String?,
    override val message: String,
    val httpStatus: Int?,
) : Exception(message)
