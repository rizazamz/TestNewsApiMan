package com.example.newsapp.presentation.detail

import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient

internal class ArticleWebViewOwner {
    var view: WebView? = null
        private set

    fun attach(webView: WebView) {
        check(view == null || view === webView)
        view = webView
    }

    fun saveHistory(onSaveHistory: (Bundle) -> Unit) {
        view?.let { current ->
            val bundle = Bundle()
            if (current.saveState(bundle) != null) onSaveHistory(bundle)
        }
    }

    fun destroy(onSaveHistory: (Bundle) -> Unit) {
        val current = view ?: return
        try {
            saveHistory(onSaveHistory)
        } finally {
            view = null
            try {
                current.stopLoading()
                current.webChromeClient = null
                current.webViewClient = WebViewClient()
                (current.parent as? ViewGroup)?.removeView(current)
                current.removeAllViews()
            } finally {
                current.destroy()
            }
        }
    }

    fun release(webView: WebView, onSaveHistory: (Bundle) -> Unit) {
        if (view === webView) destroy(onSaveHistory)
    }
}
