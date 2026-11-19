package com.example.newsapp.presentation.detail

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import com.example.newsapp.presentation.navigation.ArticleDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class BrowserUiState(
    val progress: Int = 0,
    @StringRes val error: Int? = null,
    val httpStatus: Int? = null,
)

@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val articleUrl = savedStateHandle.toRoute<ArticleDetail>().articleUrl
    var uiState by mutableStateOf(BrowserUiState())
        private set

    fun restoredHistory(): Bundle? = savedStateHandle.get<Bundle>("web_history")?.let { Bundle(it) }

    fun saveHistory(bundle: Bundle) {
        savedStateHandle["web_history"] = bundle
    }

    fun onPageStarted() {
        uiState = BrowserUiState()
    }

    fun onProgress(progress: Int) {
        uiState = uiState.copy(progress = progress.coerceIn(0, 100))
    }

    fun onError(@StringRes resource: Int, httpStatus: Int?) {
        uiState = BrowserUiState(progress = 100, error = resource, httpStatus = httpStatus)
    }
}
