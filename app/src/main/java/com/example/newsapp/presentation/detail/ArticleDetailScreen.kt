package com.example.newsapp.presentation.detail

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.newsapp.R
import com.example.newsapp.presentation.common.MessageState
import com.example.newsapp.presentation.common.ScreenToolbar

@Composable
fun ArticleDetailRoute(viewModel: ArticleDetailViewModel, onBack: () -> Unit) {
    val browser = remember { ArticleWebViewOwner() }
    val owner = LocalLifecycleOwner.current
    val goBack: () -> Unit = {
        val current = browser.view
        if (current != null && current.canGoBack()) current.goBack() else onBack()
    }
    BackHandler(onBack = goBack)
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> browser.view?.onResume()
                Lifecycle.Event.ON_PAUSE -> {
                    browser.saveHistory(viewModel::saveHistory)
                    browser.view?.onPause()
                }
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            browser.destroy(viewModel::saveHistory)
        }
    }
    ArticleDetailScreen(
        state = viewModel.uiState,
        onBack = goBack,
        onReload = {
            browser.view?.let {
                viewModel.onPageStarted()
                it.reload()
            }
        },
    ) {
        ArticleWebView(
            articleUrl = viewModel.articleUrl,
            restoredHistory = viewModel::restoredHistory,
            onReady = browser::attach,
            onRelease = { browser.release(it, viewModel::saveHistory) },
            onStarted = viewModel::onPageStarted,
            onProgress = viewModel::onProgress,
            onError = viewModel::onError,
        )
    }
}

@Composable
fun ArticleDetailScreen(
    state: BrowserUiState,
    onBack: () -> Unit,
    onReload: () -> Unit,
    browserContent: @Composable () -> Unit,
) {
    Scaffold(topBar = { ScreenToolbar(stringResource(R.string.article_detail_title), onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.error == null && state.progress < 100) {
                LinearProgressIndicator(
                    progress = { state.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                browserContent()
                state.error?.let { resource ->
                    val message = state.httpStatus?.let { stringResource(resource, it) }
                        ?: stringResource(resource)
                    Surface(modifier = Modifier.fillMaxSize()) {
                        MessageState(message, stringResource(R.string.action_reload), onReload)
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ArticleWebView(
    articleUrl: String,
    restoredHistory: () -> Bundle?,
    onReady: (WebView) -> Unit,
    onRelease: (WebView) -> Unit,
    onStarted: () -> Unit,
    onProgress: (Int) -> Unit,
    onError: (Int, Int?) -> Unit,
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                webViewClient = ArticleBrowserClient(onStarted, onError)
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView, newProgress: Int) {
                        onProgress(newProgress)
                    }
                }
                onReady(this)
                val restored = restoredHistory()?.let { restoreState(it) }
                if (restored == null) {
                    if (isWebArticleUrl(articleUrl)) loadUrl(articleUrl)
                    else onError(R.string.error_article_url, null)
                }
            }
        },
        onReset = null,
        onRelease = onRelease,
    )
}
