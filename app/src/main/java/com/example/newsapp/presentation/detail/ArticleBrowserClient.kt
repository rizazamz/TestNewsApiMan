package com.example.newsapp.presentation.detail

import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.newsapp.R
import java.net.URI

fun isWebArticleUrl(value: String): Boolean = try {
    val uri = URI(value)
    uri.scheme?.lowercase() in setOf("https", "http") && !uri.host.isNullOrBlank()
} catch (_: java.net.URISyntaxException) {
    false
}

class ArticleBrowserClient(
    private val onStarted: () -> Unit,
    private val onError: (Int, Int?) -> Unit,
) : WebViewClient() {
    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        onStarted()
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
        !isWebArticleUrl(request.url.toString())

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        if (request.isForMainFrame) {
            onError(R.string.error_web_network, null)
        }
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse,
    ) {
        if (request.isForMainFrame) {
            onError(R.string.error_web_http, errorResponse.statusCode)
        }
    }
}
