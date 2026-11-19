package com.example.newsapp.presentation.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import com.example.newsapp.R
import com.example.newsapp.domain.NewsApiException
import java.io.IOException

@StringRes
fun newsErrorMessage(error: Throwable): Int = when {
    error is NewsApiException &&
        (error.httpStatus == 429 || error.code in setOf("rateLimited", "apiKeyExhausted")) ->
        R.string.error_rate_limit
    error is NewsApiException &&
        (error.httpStatus == 401 || error.code in setOf("apiKeyInvalid", "apiKeyMissing", "apiKeyDisabled")) ->
        R.string.error_api_key
    error is NewsApiException && error.httpStatus == 426 ->
        R.string.error_plan_limit
    error is IOException -> R.string.error_network
    else -> R.string.error_news_load
}

@Composable
fun LoadingState() {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun MessageState(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        if (actionLabel != null && onAction != null) {
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun PagingRefreshState(state: LoadState, emptyMessage: String, onRetry: () -> Unit) {
    when (state) {
        is LoadState.Loading -> LoadingState()
        is LoadState.Error -> MessageState(stringResource(newsErrorMessage(state.error)), stringResource(R.string.action_retry), onRetry)
        is LoadState.NotLoading -> MessageState(emptyMessage)
    }
}

@Composable
fun PagingFooter(state: LoadState, onRetry: () -> Unit) {
    when (state) {
        is LoadState.Loading -> LoadingState()
        is LoadState.Error -> MessageState(stringResource(newsErrorMessage(state.error)), stringResource(R.string.action_retry), onRetry)
        is LoadState.NotLoading -> Spacer(Modifier.height(8.dp))
    }
}
